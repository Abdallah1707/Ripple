"""Draws the complete class diagram (section 2 of docs/class-diagram.md) as a Graphviz UML picture.

The Mermaid text in docs/class-diagram.md is the source of truth (DiagramConsistencyTest checks it
against the code). This script only turns it into a printable picture, so the picture can never
show a class the code does not have.

Usage (from the project root; needs Python 3 and Graphviz's `dot`):
    python docs/tools/render_class_diagram.py docs/class-diagram.md docs/images/class-diagram.dot
    dot -Tsvg docs/images/class-diagram.dot -o docs/images/class-diagram.svg
    dot -Tpng -Gdpi=96 docs/images/class-diagram.dot -o docs/images/class-diagram.png

To keep the picture readable:
- every class and every inheritance, association, aggregation and composition arrow is drawn, but
  a dashed "uses" arrow into a shared helper (see HELPERS) only once; the Mermaid text keeps them all;
- packages are laid out in fixed left-to-right columns, like MVC layers (see COLUMNS).
"""
import html
import re
import sys
from pathlib import Path

# Look of each package box: (title, background, header colour), keyed by Mermaid namespace.
CLUSTERS = {
    "view": ("VIEW  |  MVC", "#eaf2ff", "#cfe2fb"),
    "controller": ("CONTROLLER  |  MVC", "#fff1e5", "#fad9bc"),
    "service": ("SERVICE  |  MVC MODEL  ·  OBSERVER SUBJECTS", "#f3e9fa", "#e3cdf2"),
    "observer": ("OBSERVER  |  OBSERVER", "#fff8dc", "#f7e7a6"),
    "factory": ("FACTORY  |  FACTORY METHOD", "#f1ecfb", "#dcd1f4"),
    "communication": ("COMMUNICATION  |  STRATEGY", "#e6f6fa", "#bde5f0"),
    "receipt": ("RECEIPT  |  DECORATOR", "#fdebef", "#f7c6d0"),
    "extras": ("DONATION EXTRAS  |  DECORATOR", "#fdebef", "#f7c6d0"),
    "model": ("MODEL  |  ENTITIES  ·  FACTORY PRODUCTS", "#e7f7ed", "#bce6cb"),
    "repository": ("REPOSITORY  |  PERSISTENCE", "#f0f2f5", "#d5dae2"),
    "logging": ("LOGGING  |  SINGLETON", "#fff3e0", "#f9ddaa"),
}

# Left-to-right columns of packages, like MVC layers. Packages in the same column are stacked.
# Graphviz would otherwise let every arrow pull boxes to the right, giving a long diagonal staircase.
COLUMNS = [
    ["view"],
    ["controller"],
    ["service", "logging"],
    ["observer", "repository"],
    ["communication", "receipt", "extras", "factory"],
    ["model"],
]

# Small classes used by many others. Drawing every dashed "uses" arrow into them would cross the
# whole picture, so only the first one is drawn; the legend says so.
HELPERS = {"ApplicationLogger", "Result"}


def generic(text):
    """Mermaid List~T~ -> List<T> (handles nesting)."""
    while "~" in text:
        text = re.sub(r"~([^~]*)~", r"<\1>", text, count=1)
    return text


def split_top(params):
    """Splits a parameter list on the commas that are not inside <...>."""
    out, depth, cur = [], 0, ""
    for ch in params:
        depth += ch == "<"
        depth -= ch == ">"
        if ch == "," and depth == 0:
            out.append(cur)
            cur = ""
        else:
            cur += ch
    if cur.strip():
        out.append(cur)
    return [p.strip() for p in out]


def uml_param(p):
    """Java style 'int id' -> UML style 'id: int'."""
    parts = p.rsplit(" ", 1)
    if len(parts) == 1 or p == "...":
        return p
    return f"{parts[1]}: {parts[0]}"


def member(line):
    """One Mermaid member line -> (HTML text, is_method). Static is underlined, abstract in italics."""
    line = generic(line.strip())
    vis = line[0] if line[0] in "+-#~" else ""
    body = line[len(vis):]
    m = re.match(r"(\w+)\((.*)\)([*$]?)\s*(.*)$", body)
    if m:  # method or constructor
        name, params, flag, ret = m.groups()
        text = f"{vis}{name}({', '.join(uml_param(p) for p in split_top(params))})"
        if ret:
            text += f": {ret}"
        text = html.escape(text)
        if flag == "$":
            text = f"<U>{text}</U>"
        elif flag == "*":
            text = f"<I>{text}</I>"
        return text, True
    static = body.endswith("$")
    body = body.rstrip("$")
    parts = body.rsplit(" ", 1)
    text = vis + (f"{parts[1]}: {parts[0]}" if len(parts) == 2 else parts[0])  # enum constants have no type
    text = html.escape(text)
    return (f"<U>{text}</U>" if static else text), False


def parse(md):
    """Reads the classes (per namespace) and the relationships of section 2."""
    start = md.index("## 2. Complete diagram")
    block = re.search(r"(?s)```mermaid\n(.*?)```", md[start:]).group(1)

    classes = {}  # name -> dict(ns, gen, stereo, fields, methods)
    for ns_m in re.finditer(r"(?ms)^namespace\s+(\w+)\s*\{(.*?)^\}", block):
        ns, body = ns_m.groups()
        for c in re.finditer(r"(?ms)^\s*class\s+(\w+)(~\w+~)?\s*(\{(.*?)^\s*\})?", body):
            name, gen, _, members = c.groups()
            info = {"ns": ns, "gen": generic(gen or ""), "stereo": [], "fields": [], "methods": []}
            for raw in (members or "").splitlines():
                raw = raw.strip()
                if not raw:
                    continue
                if raw.startswith("<<"):
                    info["stereo"].append(raw.strip("<>"))
                    continue
                text, is_method = member(raw)
                info["methods" if is_method else "fields"].append(text)
            classes[name] = info

    rel = re.compile(r'^(\w+)\s*(?:"([^"]*)"\s*)?(--\|>|\.\.\|>|-->|o--|\*--|\.\.>|--)\s*'
                     r'(?:"([^"]*)"\s*)?(\w+)\s*(?::\s*(.*))?$')
    rels = []
    for line in block.splitlines():
        m = rel.match(line.strip())
        if m and m.group(1) in classes and m.group(5) in classes:
            rels.append(m.groups())
    return classes, rels


def declutter(rels):
    """Drops repeated dashed "uses" arrows into the shared helpers (the first one is kept)."""
    kept, seen = [], set()
    for r in rels:
        arrow, target = r[2], r[4]
        if arrow == "..>" and target in HELPERS:
            if target in seen:
                continue
            seen.add(target)
        kept.append(r)
    return kept


def node(name, info, header):
    """One class box: stereotypes and name, then fields, then methods."""
    stereo = info["stereo"]
    italic = any(s in ("interface", "abstract") for s in stereo)
    title = html.escape(name + info["gen"])
    title = f"<B><I>{title}</I></B>" if italic else f"<B>{title}</B>"
    tags = "".join(f'<FONT POINT-SIZE="9" COLOR="#55607a">«{html.escape(s)}»</FONT><BR/>' for s in stereo)
    rows = [f'<TR><TD BGCOLOR="{header}" CELLPADDING="5">{tags}{title}</TD></TR>']
    for group in (info["fields"], info["methods"]):
        if group:
            rows.append("<HR/>")
            text = "".join(f'{t}<BR ALIGN="LEFT"/>' for t in group)
            rows.append(f'<TR><TD ALIGN="LEFT" BALIGN="LEFT" CELLPADDING="5">{text}</TD></TR>')
    table = ('<TABLE BORDER="1" COLOR="#9aa6b8" STYLE="ROUNDED" CELLBORDER="0" CELLSPACING="0" BGCOLOR="white">'
             + "".join(rows) + "</TABLE>")
    return f'  "{name}" [shape=plain label=<{table}>];'


def column(ns):
    """Index of the package's column in COLUMNS."""
    return next(i for i, packages in enumerate(COLUMNS) if ns in packages)


def edge(classes, a, mult_a, arrow, mult_b, b, label):
    """One relationship as a Graphviz edge, with the UML arrowhead for its kind."""
    attrs = []
    if arrow in ("--|>", "..|>"):  # drawn from the parent, so parents sit on the left
        a, b, mult_a, mult_b = b, a, mult_b, mult_a
        attrs += ['dir=back', 'arrowtail=empty', 'color="#4f5d7a"', 'weight=4']
        if arrow == "..|>":
            attrs.append('style=dashed')
    elif arrow == "-->":
        attrs += ['arrowhead=vee', 'color="#6c7a96"']
    elif arrow == "o--":
        attrs += ['dir=both', 'arrowtail=odiamond', 'arrowhead=vee', 'color="#4f5d7a"']
    elif arrow == "*--":
        attrs += ['dir=both', 'arrowtail=diamond', 'arrowhead=vee', 'color="#4f5d7a"']
    elif arrow == "..>":
        attrs += ['arrowhead=vee', 'style=dashed', 'color="#8a93a8"', 'weight=0']
    else:  # plain link (many-to-many)
        attrs += ['arrowhead=none', 'color="#6c7a96"']
    # An arrow that does not point to a later column (backwards, or to another package in the same
    # column), and a dashed dependency inside a package, is drawn but does not decide positions,
    # so it cannot pull boxes out of their package's column or widen the package.
    ns_a, ns_b = classes[a]["ns"], classes[b]["ns"]
    if (ns_a != ns_b and column(ns_a) >= column(ns_b)) or (ns_a == ns_b and arrow == "..>"):
        attrs.append("constraint=false")
    if label:
        attrs.append(f'label="{html.escape(label.strip())}"')
    if mult_a:
        attrs.append(f'taillabel="{mult_a}"')
    if mult_b:
        attrs.append(f'headlabel="{mult_b}"')
    return f'  "{a}" -> "{b}" [{" ".join(attrs)}];'


def render(classes, rels):
    out = ['digraph Ripple {',
           '  graph [rankdir=LR splines=true nodesep=0.3 ranksep=0.9 bgcolor="#fcfbf8" fontname="Helvetica"',
           '         labelloc=t labeljust=l pad=0.5 newrank=true',
           '         label=<<FONT POINT-SIZE="30"><B>Ripple — UML Class Diagram</B></FONT><BR ALIGN="LEFT"/>'
           '<FONT POINT-SIZE="16" COLOR="#6a7489">NGO Management System  ·  Phase 1  ·  MVC  ·  Strategy  ·  '
           'Observer  ·  Decorator  ·  Factory Method  ·  Singleton</FONT><BR ALIGN="LEFT"/>'
           '<FONT POINT-SIZE="12" COLOR="#6a7489">Arrows: hollow triangle = inheritance (dashed = implements) · '
           'open arrow = association · hollow diamond = aggregation (&gt;5 methods used) · filled diamond = '
           'composition · dashed open arrow = dependency. Dashed arrows into the shared helpers '
           f'({", ".join(sorted(HELPERS))}) are drawn once; docs/class-diagram.md lists them all.'
           '</FONT><BR ALIGN="LEFT"/> >];',
           '  node [fontname="Helvetica" fontsize=11];',
           '  edge [fontname="Helvetica" fontsize=9 fontcolor="#6a7489" penwidth=1.2];']
    for ns, (title, fill, header) in CLUSTERS.items():
        members = [n for n, i in classes.items() if i["ns"] == ns]
        if not members:
            continue
        out.append(f'  subgraph cluster_{ns} {{')
        out.append(f'    label=<<B>{html.escape(title)}</B>> fontsize=15 fontcolor="#5d6570" labeljust=l')
        out.append(f'    style="rounded,filled" fillcolor="{fill}" color="{header}" penwidth=2 margin=16')
        out += ["  " + node(n, classes[n], header) for n in members]
        out.append("  }")
    out += column_order(classes)
    out += [edge(classes, *r) for r in rels]
    out.append("}")
    return "\n".join(out)


def column_order(classes):
    """Invisible gates between the COLUMNS: every class of column i is left of gate i, and every
    class of column i+1 is right of it. Only these invisible edges decide the columns."""
    out = []
    for i in range(len(COLUMNS) - 1):
        gate = f"gate{i}"
        out.append(f'  "{gate}" [shape=point style=invis width=0];')
        left = [n for n, info in classes.items() if info["ns"] in COLUMNS[i]]
        right = [n for n, info in classes.items() if info["ns"] in COLUMNS[i + 1]]
        out += [f'  "{n}" -> "{gate}" [style=invis];' for n in left]
        out += [f'  "{gate}" -> "{n}" [style=invis];' for n in right]
    return out


def main():
    classes, rels = parse(Path(sys.argv[1]).read_text(encoding="utf-8"))
    drawn = declutter(rels)
    Path(sys.argv[2]).write_text(render(classes, drawn), encoding="utf-8")
    print(f"{len(classes)} classes, {len(rels)} relationships, {len(drawn)} drawn")


if __name__ == "__main__":
    main()

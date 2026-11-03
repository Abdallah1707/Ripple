"""Turns the class diagram in docs/class-diagram.md into a Graphviz picture.

Usage (from the project root, needs Python 3 and Graphviz):
    python docs/tools/render_class_diagram.py docs/class-diagram.md docs/images/class-diagram.dot
    dot -Tsvg docs/images/class-diagram.dot -o docs/images/class-diagram.svg
    dot -Tpng -Gdpi=96 docs/images/class-diagram.dot -o docs/images/class-diagram.png
"""
import html
import re
import sys
from pathlib import Path

# package box style: (title, background, header color)
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

# package columns from left to right, so the layout stays tidy
COLUMNS = [
    ["view"],
    ["controller"],
    ["service", "logging"],
    ["observer", "repository"],
    ["communication", "receipt", "extras", "factory"],
    ["model"],
]

# used by many classes, so only draw one arrow into each
HELPERS = {"ApplicationLogger", "Result"}


def generic(text):
    """List~T~ -> List<T>"""
    while "~" in text:
        text = re.sub(r"~([^~]*)~", r"<\1>", text, count=1)
    return text


def split_top(params):
    """Splits params on commas outside <...>."""
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
    """'int id' -> 'id: int'"""
    parts = p.rsplit(" ", 1)
    if len(parts) == 1 or p == "...":
        return p
    return f"{parts[1]}: {parts[0]}"


def member(line):
    """Converts one member line. Returns (text, is_method)."""
    line = generic(line.strip())
    vis = line[0] if line[0] in "+-#~" else ""
    body = line[len(vis):]
    m = re.match(r"(\w+)\((.*)\)([*$]?)\s*(.*)$", body)
    if m:  # method
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
    """Reads the classes and relationships from section 2."""
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
    """Keeps only the first arrow into each helper."""
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
    """One class box."""
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
    """Column number of a package."""
    return next(i for i, packages in enumerate(COLUMNS) if ns in packages)


def edge(classes, a, mult_a, arrow, mult_b, b, label):
    """One relationship as a Graphviz edge."""
    attrs = []
    if arrow in ("--|>", "..|>"):  # flip so parents are on the left
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
    else:  # many-to-many
        attrs += ['arrowhead=none', 'color="#6c7a96"']
    # backward arrows and dashed arrows inside a package shouldn't move the boxes
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
    """Invisible nodes between columns that keep each package in its column."""
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

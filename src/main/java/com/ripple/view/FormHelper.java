package com.ripple.view;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.regex.Pattern;

/**
 * Small helpers that every screen uses to be easier to work with: hints inside empty fields,
 * a search box above each table, double-click to edit, Enter to save, tooltips and one
 * "Are you sure?" dialog for deletes.
 *
 * <p>Part of the <b>View</b> in MVC: it only deals with Swing components and never calls a
 * controller. Keeping these in one place means the six screens behave the same way.
 */
public final class FormHelper {

    private FormHelper() {
        // static helpers only
    }

    /**
     * Shows a grey hint inside an empty text field, e.g. the expected date format.
     *
     * @param field the field
     * @param hint  the text shown while the field is empty
     */
    public static void placeholder(JTextField field, String hint) {
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, hint);
    }

    /**
     * Creates a button with a tooltip that says what it does.
     *
     * @param text    the button's label
     * @param tooltip shown when the mouse rests on the button
     * @param action  what happens when it is clicked
     * @return the new button
     */
    public static JButton button(String text, String tooltip, Runnable action) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.addActionListener(e -> action.run());
        return button;
    }

    /**
     * Makes the table sortable (click a column header) and returns a search box that hides
     * every row which does not contain the typed text, ignoring case.
     *
     * <p>After this, the row the user sees is no longer the row in the list behind the table,
     * so screens must use {@link #selectedModelRow(JTable)} instead of
     * {@code table.getSelectedRow()}.
     *
     * @param table the table to filter
     * @return the search field to place above the table
     */
    public static JTextField searchField(JTable table) {
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);

        JTextField search = new JTextField(20);
        placeholder(search, "Search...");
        search.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        search.setToolTipText("Shows only the rows that contain this text");
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filter(sorter, search.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filter(sorter, search.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filter(sorter, search.getText());
            }
        });
        return search;
    }

    /**
     * The table in a scroll pane, with its search box above it on the left.
     *
     * @param table the table to show
     * @return a panel ready to add to the screen
     * @see #searchField(JTable)
     */
    public static JPanel tableWithSearch(JTable table) {
        JPanel searchRow = new JPanel(new BorderLayout());
        searchRow.add(searchField(table), BorderLayout.WEST);
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.add(searchRow, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private static void filter(TableRowSorter<TableModel> sorter, String text) {
        if (text.isBlank()) {
            sorter.setRowFilter(null); // show every row
        } else {
            // Pattern.quote: characters like "(" or "." are searched for as they are typed.
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text.trim())));
        }
    }

    /**
     * Returns the index of the selected row in the table's model, which is the same index as
     * in the list the screen filled the table from. This stays correct when the table is
     * sorted or filtered.
     *
     * @param table the table
     * @return the model row index, or {@code -1} if nothing is selected
     */
    public static int selectedModelRow(JTable table) {
        int viewRow = table.getSelectedRow();
        return viewRow < 0 ? -1 : table.convertRowIndexToModel(viewRow);
    }

    /**
     * Runs an action when a row of the table is double-clicked, e.g. "edit this row".
     *
     * @param table  the table
     * @param action what to do
     */
    public static void onDoubleClick(JTable table, Runnable action) {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    action.run();
                }
            }
        });
    }

    /**
     * Runs an action when Enter is pressed in any of the given fields, e.g. "save the form".
     *
     * @param action what to do
     * @param fields the fields that react to Enter
     */
    public static void onEnter(Runnable action, JTextField... fields) {
        for (JTextField field : fields) {
            field.addActionListener(e -> action.run()); // a text field fires its action on Enter
        }
    }

    /**
     * Asks "Are you sure?" before something is deleted.
     *
     * @param parent  the screen asking
     * @param message what will be deleted, e.g. "Delete 'Zakat'?"
     * @return {@code true} if the user clicked Yes
     */
    public static boolean confirmDelete(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Confirm delete",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }
}

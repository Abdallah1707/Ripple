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
 * Small UI helpers used by all screens: hints, search boxes, double-click to edit,
 * Enter to save, tooltips and delete confirmation.
 */
public final class FormHelper {

    private FormHelper() {
        // no instances
    }

    /** Shows a grey hint in an empty field. */
    public static void placeholder(JTextField field, String hint) {
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, hint);
    }

    /** Creates a button with a tooltip. */
    public static JButton button(String text, String tooltip, Runnable action) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.addActionListener(e -> action.run());
        return button;
    }

    /**
     * Makes the table sortable and returns a search box that filters its rows.
     * Use selectedModelRow after this, not getSelectedRow.
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

    /** The table with a search box above it. */
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
            sorter.setRowFilter(null); // show all rows
        } else {
            // quote so ( and . are searched as normal text
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text.trim())));
        }
    }

    /** The selected row in the model, or -1. Works with sorting and filtering. */
    public static int selectedModelRow(JTable table) {
        int viewRow = table.getSelectedRow();
        return viewRow < 0 ? -1 : table.convertRowIndexToModel(viewRow);
    }

    /** Runs the action when a row is double-clicked. */
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

    /** Runs the action when Enter is pressed in any of the fields. */
    public static void onEnter(Runnable action, JTextField... fields) {
        for (JTextField field : fields) {
            field.addActionListener(e -> action.run()); // fires on Enter
        }
    }

    /** Asks for confirmation before deleting. Returns true if Yes was clicked. */
    public static boolean confirmDelete(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Confirm delete",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }
}

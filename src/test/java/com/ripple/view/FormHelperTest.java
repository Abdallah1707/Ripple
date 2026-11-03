package com.ripple.view;

import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks the search box and the selection mapping: after filtering or sorting, the selected
 * row must still point at the right record in the screen's list.
 */
class FormHelperTest {

    private static JTable tableOf(String... names) {
        DefaultTableModel model = new DefaultTableModel(new String[]{"Name"}, 0);
        for (String name : names) {
            model.addRow(new Object[]{name});
        }
        return new JTable(model);
    }

    @Test
    void searchShowsOnlyMatchingRowsIgnoringCase() {
        JTable table = tableOf("Mona Ali", "Omar Samy", "Sara Mona");
        JTextField search = FormHelper.searchField(table);

        search.setText("mona");
        assertEquals(2, table.getRowCount());

        search.setText("");
        assertEquals(3, table.getRowCount());
    }

    @Test
    void searchTreatsSpecialCharactersAsText() {
        JTable table = tableOf("Food (dry)", "Food");
        JTextField search = FormHelper.searchField(table);

        search.setText("(dry");
        assertEquals(1, table.getRowCount()); // no regex error, and only the row that contains "(dry"
    }

    @Test
    void selectedModelRowPointsAtTheRightRecordAfterFiltering() {
        JTable table = tableOf("Mona Ali", "Omar Samy", "Sara Mona");
        JTextField search = FormHelper.searchField(table);
        search.setText("sara");

        table.setRowSelectionInterval(0, 0); // the only visible row
        assertEquals(2, FormHelper.selectedModelRow(table)); // "Sara Mona" is the 3rd record
    }

    @Test
    void selectedModelRowPointsAtTheRightRecordAfterSorting() {
        JTable table = tableOf("Zaid", "Amal", "Mona");
        FormHelper.searchField(table);
        table.getRowSorter().toggleSortOrder(0); // like clicking the column header: A to Z

        table.setRowSelectionInterval(0, 0); // "Amal" is now shown first
        assertEquals(1, FormHelper.selectedModelRow(table));
    }

    @Test
    void nothingSelectedGivesMinusOne() {
        assertEquals(-1, FormHelper.selectedModelRow(tableOf("Mona")));
    }
}

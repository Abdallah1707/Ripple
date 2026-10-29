package com.ripple.view;

import com.ripple.controller.LookupController;
import com.ripple.controller.LookupKind;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;

/**
 * The Lookup Management screen: two tabs, Donation Types and Event Categories,
 * each showing a {@link LookupTablePanel}.
 */
public class LookupPanel extends JPanel implements Refreshable {

    private final LookupTablePanel donationTypesTab;
    private final LookupTablePanel eventCategoriesTab;

    /**
     * Builds the screen.
     *
     * @param controller the lookup controller
     * @param statusBar  where to show the outcome of each action
     */
    public LookupPanel(LookupController controller, StatusBar statusBar) {
        super(new BorderLayout());
        donationTypesTab = new LookupTablePanel(controller, LookupKind.DONATION_TYPE, statusBar);
        eventCategoriesTab = new LookupTablePanel(controller, LookupKind.EVENT_CATEGORY, statusBar);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(LookupKind.DONATION_TYPE.getTitle(), donationTypesTab);
        tabs.addTab(LookupKind.EVENT_CATEGORY.getTitle(), eventCategoriesTab);
        add(tabs, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        donationTypesTab.refresh();
        eventCategoriesTab.refresh();
    }
}

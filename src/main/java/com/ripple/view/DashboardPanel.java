package com.ripple.view;

import com.ripple.controller.DashboardController;
import com.ripple.controller.DashboardSummary;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Locale;

/**
 * The Dashboard screen: six summary cards (donors, donations, total donation value,
 * volunteers, events, beneficiaries) and a list of recent activity from the application log.
 *
 * <p>A <b>View</b> in MVC: it only shows what {@link DashboardController} gives it. It is
 * {@link Refreshable}, so {@link MainFrame} reloads the numbers every time the user opens it.
 */
public class DashboardPanel extends JPanel implements Refreshable {

    private final DashboardController controller;

    private final JLabel donorsValue = newValueLabel();
    private final JLabel donationsValue = newValueLabel();
    private final JLabel totalValue = newValueLabel();
    private final JLabel volunteersValue = newValueLabel();
    private final JLabel eventsValue = newValueLabel();
    private final JLabel beneficiariesValue = newValueLabel();
    private final DefaultListModel<String> activityModel = new DefaultListModel<>();

    /**
     * Builds the screen.
     *
     * @param controller the dashboard controller that supplies the numbers
     */
    public DashboardPanel(DashboardController controller) {
        super(new BorderLayout(0, 14));
        this.controller = controller;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Dashboard");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        add(title, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.add(buildCards(), BorderLayout.NORTH);
        body.add(buildActivity(), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        DashboardSummary summary = controller.getSummary();
        donorsValue.setText(formatCount(summary.donors()));
        donationsValue.setText(formatCount(summary.donations()));
        totalValue.setText(String.format(Locale.US, "EGP %,.2f", summary.totalDonationValue()));
        volunteersValue.setText(formatCount(summary.volunteers()));
        eventsValue.setText(formatCount(summary.events()));
        beneficiariesValue.setText(formatCount(summary.beneficiaries()));

        activityModel.clear();
        if (summary.recentActivity().isEmpty()) {
            activityModel.addElement("No activity yet.");
        } else {
            activityModel.addAll(summary.recentActivity());
        }
    }

    /** Two rows of three cards. */
    private JPanel buildCards() {
        JPanel cards = new JPanel(new GridLayout(2, 3, 12, 12));
        cards.add(card("Donors", donorsValue));
        cards.add(card("Donations", donationsValue));
        cards.add(card("Total donation value", totalValue));
        cards.add(card("Volunteers", volunteersValue));
        cards.add(card("Events", eventsValue));
        cards.add(card("Beneficiaries", beneficiariesValue));
        return cards;
    }

    /** The "Recent activity" list, newest entry first. */
    private JScrollPane buildActivity() {
        JList<String> list = new JList<>(activityModel);
        list.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createTitledBorder("Recent activity (newest first)"));
        return scroll;
    }

    /** One summary card: a big number with a small caption under it. */
    private static JPanel card(String caption, JLabel value) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 205, 215)),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        JLabel captionLabel = new JLabel(caption);
        captionLabel.setForeground(Color.GRAY);
        card.add(value, BorderLayout.CENTER);
        card.add(captionLabel, BorderLayout.SOUTH);
        return card;
    }

    private static JLabel newValueLabel() {
        JLabel label = new JLabel("0");
        label.setFont(label.getFont().deriveFont(Font.BOLD, 26f));
        label.setForeground(new Color(30, 60, 90));
        return label;
    }

    private static String formatCount(int count) {
        return String.format(Locale.US, "%,d", count);
    }
}

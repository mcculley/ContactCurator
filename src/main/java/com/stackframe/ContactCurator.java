package com.stackframe;

import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.io.text.VCardReader;
import ezvcard.io.text.VCardWriter;
import ezvcard.parameter.AddressType;
import ezvcard.parameter.EmailType;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.Address;
import ezvcard.property.Birthday;
import ezvcard.property.Email;
import ezvcard.property.FormattedName;
import ezvcard.property.Organization;
import ezvcard.property.Role;
import ezvcard.property.StructuredName;
import ezvcard.property.Telephone;
import ezvcard.property.Title;
import ezvcard.property.Url;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * The default Swing GUI for ContactCurator.
 */
public class ContactCurator extends JFrame {

    private File cardFile;
    private final Preferences prefs = Preferences.userNodeForPackage(getClass());
    private final JLabel statusLabel = new JLabel();
    private final List<VCard> cards = new ArrayList<>();
    private final JMenuItem saveMenuItem = new JMenuItem("Save");
    private final JMenuItem saveAsMenuItem = new JMenuItem("Save As...");
    private final JScrollPane cardViewerPane = new JScrollPane(new JPanel(), JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

    private class CardListModel extends DefaultListModel {

        @Override
        public int getSize() {
            return cards.size();
        }

        @Override
        public Object getElementAt(int i) {
            return cards.get(i);
        }

        public void fireContentsChanged() {
            super.fireContentsChanged(this, 0, cards.size() - 1);
        }

    }

    private final CardListModel cardListModel = new CardListModel();
    private final JList<VCard> cardListView = new JList<>(cardListModel);

    private static String listDisplayName(final VCard card) {
        final StringBuilder b = new StringBuilder();
        final StructuredName n = card.getStructuredName();
        if (n != null) {
            final String family = n.getFamily();
            if (family != null && !family.isBlank()) {
                b.append(family);
                if (b.length() > 0) {
                    b.append(", ");
                }
            }

            final String given = n.getGiven();
            if (given != null && !given.isBlank()) {
                b.append(given);
            }
        }

        final FormattedName formattedName = card.getFormattedName();
        if (b.toString().trim().length() == 0 && formattedName != null) {
            b.append(formattedName.getValue());
        }

        final Organization org = card.getOrganization();
        if (b.toString().trim().length() == 0 && org != null) {
            b.append(org.getValues().get(0));
        }

        if (b.toString().isBlank()) {
            return "No Name";
        } else {
            return b.toString();
        }
    }

    public ContactCurator() {
        super("Contact Curator");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        final JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        final JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        fileMenu.setMnemonic(KeyEvent.VK_F);
        final JMenuItem openMenuItem = new JMenuItem("Open...");
        fileMenu.add(openMenuItem);
        openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.META_MASK));
        openMenuItem.setMnemonic(KeyEvent.VK_O);
        openMenuItem.addActionListener(actionEvent -> openFile());

        fileMenu.add(saveMenuItem);
        saveMenuItem
                .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.META_MASK));
        saveMenuItem.setMnemonic(KeyEvent.VK_S);
        saveMenuItem.addActionListener(actionEvent -> save());

        fileMenu.add(saveAsMenuItem);
        saveAsMenuItem
                .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.META_MASK | ActionEvent.SHIFT_MASK));
        saveAsMenuItem.setMnemonic(KeyEvent.VK_A);
        saveAsMenuItem.addActionListener(actionEvent -> saveAs());

        final JSplitPane splitPane = new JSplitPane();
        getContentPane().add(splitPane, BorderLayout.CENTER);

        splitPane.setLeftComponent(new JScrollPane(cardListView, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
        splitPane.setRightComponent(cardViewerPane);

        cardListView.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList jList, final Object o, final int index,
                                                          final boolean isSelected, final boolean hasFocus) {
                final VCard card = (VCard) o;
                final String s = listDisplayName(card);
                return super.getListCellRendererComponent(jList, s, index, isSelected, hasFocus);
            }
        });

        cardListView.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                final List<VCard> selected = cardListView.getSelectedValuesList();
                final JComponent view;
                if (selected.size() == 1) {
                    view = makeCardViewer((selected.get(0)));
                } else {
                    view = new JPanel();
                }

                cardViewerPane.setViewportView(view);
                updateGUI();
            }
        });

        getContentPane().add(statusLabel, BorderLayout.SOUTH);
        updateGUI();
    }

    private static JComponent makeCardViewer(final VCard card) {
        final JComponent view = new Box(BoxLayout.PAGE_AXIS);
        System.err.println("num names=" + card.getStructuredNames().size());
        System.err.println("names=" + card.getStructuredNames());
        final JComponent formattedNamesPanel = new Box(BoxLayout.PAGE_AXIS);
        formattedNamesPanel.setBorder(BorderFactory.createTitledBorder("Formatted Names"));
        view.add(formattedNamesPanel);
        for (final FormattedName n : card.getFormattedNames()) {
            final JPanel p = new JPanel();
            p.add(new JLabel(n.getValue()));
            p.setMaximumSize(p.getMinimumSize());
            formattedNamesPanel.add(p);
        }

        formattedNamesPanel.setMaximumSize(formattedNamesPanel.getMinimumSize());

        final JComponent structuredNamesPanel = new Box(BoxLayout.PAGE_AXIS);
        structuredNamesPanel.setBorder(BorderFactory.createTitledBorder("Structured Names"));
        view.add(structuredNamesPanel);
        for (final StructuredName n : card.getStructuredNames()) {
            final JPanel p = new JPanel();
            structuredNamesPanel.add(p);

            final List<String> prefixes = n.getPrefixes();
            if (!prefixes.isEmpty()) {
                final JLabel prefixesLabel = new JLabel("Prefixes:");
                p.add(prefixesLabel);
                prefixesLabel.setMaximumSize(prefixesLabel.getMinimumSize());
                final StringBuilder b = new StringBuilder();
                for (final String prefix : prefixes) {
                    b.append(prefix);
                    b.append(" ");
                }

                final JLabel l = new JLabel(b.toString().trim());
                prefixesLabel.setLabelFor(l);
                p.add(l);
            }

            final JLabel givenLabel = new JLabel("Given:");
            givenLabel.setMaximumSize(givenLabel.getMinimumSize());
            p.add(givenLabel);
            final JLabel given = new JLabel(n.getGiven());
            givenLabel.setLabelFor(given);
            p.add(given);

            final List<String> additionalNames = n.getAdditionalNames();
            if (!additionalNames.isEmpty()) {
                final JLabel additionalNamesLabel = new JLabel("Additional Names:");
                additionalNamesLabel.setMaximumSize(additionalNamesLabel.getMinimumSize());
                p.add(additionalNamesLabel);
                final StringBuilder b = new StringBuilder();
                for (final String a : additionalNames) {
                    b.append(a);
                    b.append(" ");
                }

                final JLabel l = new JLabel(b.toString().trim());
                l.setMaximumSize(l.getMinimumSize());
                additionalNamesLabel.setLabelFor(l);
                p.add(l);
            }

            final JLabel familyLabel = new JLabel("Family:");
            p.add(familyLabel);
            final JLabel family = new JLabel(n.getFamily());
            familyLabel.setLabelFor(family);
            p.add(family);

            final List<String> suffixes = n.getSuffixes();
            if (!suffixes.isEmpty()) {
                final JLabel suffixesLabel = new JLabel("Suffixes:");
                suffixesLabel.setMaximumSize(suffixesLabel.getMinimumSize());
                p.add(suffixesLabel);
                final StringBuilder b = new StringBuilder();
                for (final String a : suffixes) {
                    b.append(a);
                    b.append(" ");
                }

                final JLabel l = new JLabel(b.toString().trim());
                l.setMaximumSize(l.getMinimumSize());
                suffixesLabel.setLabelFor(l);
                p.add(l);
            }

        }

        structuredNamesPanel.setMaximumSize(structuredNamesPanel.getMinimumSize());

        final List<Title> titles = card.getTitles();
        if (!titles.isEmpty()) {
            final JComponent titlesPanel = new Box(BoxLayout.PAGE_AXIS);
            titlesPanel.setBorder(BorderFactory.createTitledBorder("Titles"));
            view.add(titlesPanel);

            for (final Title title : titles) {
                System.err.println("title=" + title);
                final JPanel p = new JPanel();
                final JLabel l = new JLabel("Title:");
                l.setMaximumSize(l.getMinimumSize());
                final StringBuilder text = new StringBuilder();
                final String type = title.getType();
                if (type != null) {
                    text.append('(' + type + ") ");
                }

                text.append(title.getValue());
                final JLabel t = new JLabel(text.toString());
                t.setMaximumSize(t.getMinimumSize());
                l.setLabelFor(t);
                p.add(t);
                p.setMaximumSize(p.getMinimumSize());
                titlesPanel.add(p);
            }

            titlesPanel.setMaximumSize(titlesPanel.getMinimumSize());
        }

        final List<Role> roles = card.getRoles();
        if (!roles.isEmpty()) {
            final JComponent rolesPanel = new Box(BoxLayout.PAGE_AXIS);
            rolesPanel.setBorder(BorderFactory.createTitledBorder("Roles"));
            view.add(rolesPanel);

            for (final Role role : roles) {
                System.err.println("role=" + role);
                final JPanel p = new JPanel();
                final JLabel l = new JLabel("Role:");
                l.setMaximumSize(l.getMinimumSize());
                final StringBuilder text = new StringBuilder();
                final String type = role.getType();
                if (type != null) {
                    text.append('(' + type + ") ");
                }

                text.append(role.getValue());
                final JLabel t = new JLabel(text.toString());
                t.setMaximumSize(t.getMinimumSize());
                l.setLabelFor(t);
                p.add(t);
                p.setMaximumSize(p.getMinimumSize());
                rolesPanel.add(p);
            }

            rolesPanel.setMaximumSize(rolesPanel.getMinimumSize());
        }

        final List<Organization> organizations = card.getOrganizations();
        if (!organizations.isEmpty()) {
            final JComponent sectionPanel = new Box(BoxLayout.PAGE_AXIS);
            sectionPanel.setBorder(BorderFactory.createTitledBorder("Organizations"));
            view.add(sectionPanel);

            for (final Organization o : organizations) {
                System.err.println("o=" + o);
                final JPanel p = new JPanel();
                final JLabel l = new JLabel("Organization:");
                l.setMaximumSize(l.getMinimumSize());
                final StringBuilder text = new StringBuilder();
                final String type = o.getType();
                if (type != null) {
                    text.append('(' + type + ") ");
                }

                for (final String value : o.getValues()) {
                    text.append(value);
                    text.append(" ");
                }

                final JLabel t = new JLabel(text.toString().trim());
                t.setMaximumSize(t.getMinimumSize());
                l.setLabelFor(t);
                p.add(t);
                p.setMaximumSize(p.getMinimumSize());
                sectionPanel.add(p);
            }

            sectionPanel.setMaximumSize(sectionPanel.getMinimumSize());
        }

        final List<Email> emails = card.getEmails();
        if (!emails.isEmpty()) {
            final JComponent sectionPanel = new Box(BoxLayout.PAGE_AXIS);
            sectionPanel.setBorder(BorderFactory.createTitledBorder("Emails"));
            view.add(sectionPanel);

            for (final Email e : emails) {
                System.err.println("e=" + e);
                final JPanel p = new JPanel();
                final JLabel l = new JLabel("Email:");
                l.setMaximumSize(l.getMinimumSize());
                final StringBuilder text = new StringBuilder();
                final List<EmailType> types = e.getTypes();
                if (types != null) {
                    for (final EmailType type : types) {
                        text.append('(' + type.getValue() + ") ");
                    }
                }

                text.append(e.getValue());

                final JLabel t = new JLabel(text.toString().trim());
                t.setMaximumSize(t.getMinimumSize());
                l.setLabelFor(t);
                p.add(t);
                p.setMaximumSize(p.getMinimumSize());
                sectionPanel.add(p);
            }

            sectionPanel.setMaximumSize(sectionPanel.getMinimumSize());
        }

        final List<Telephone> phones = card.getTelephoneNumbers();
        if (!phones.isEmpty()) {
            final JComponent sectionPanel = new Box(BoxLayout.PAGE_AXIS);
            sectionPanel.setBorder(BorderFactory.createTitledBorder("Telephone Numbers"));
            view.add(sectionPanel);

            for (final Telephone phone : phones) {
                System.err.println("phone=" + phone);
                final JPanel p = new JPanel();
                final JLabel l = new JLabel("Phone:");
                l.setMaximumSize(l.getMinimumSize());
                final StringBuilder text = new StringBuilder();
                final List<TelephoneType> types = phone.getTypes();
                if (types != null) {
                    for (final TelephoneType type : types) {
                        text.append('(' + type.getValue() + ") ");
                    }
                }

                text.append(phone.getText());

                final JLabel t = new JLabel(text.toString().trim());
                t.setMaximumSize(t.getMinimumSize());
                l.setLabelFor(t);
                p.add(t);
                p.setMaximumSize(p.getMinimumSize());
                sectionPanel.add(p);
            }

            sectionPanel.setMaximumSize(sectionPanel.getMinimumSize());
        }

        final List<Url> urls = card.getUrls();
        if (!urls.isEmpty()) {
            final JComponent sectionPanel = new Box(BoxLayout.PAGE_AXIS);
            sectionPanel.setBorder(BorderFactory.createTitledBorder("URLs"));
            view.add(sectionPanel);

            for (final Url u : urls) {
                System.err.println("u=" + u);
                final JPanel p = new JPanel();
                final JLabel l = new JLabel("URL:");
                l.setMaximumSize(l.getMinimumSize());
                final StringBuilder text = new StringBuilder();
                final String type = u.getType();
                if (type != null) {
                    text.append('(' + type + ") ");
                }

                text.append(u.getValue());

                final JLabel t = new JLabel(text.toString().trim());
                t.setMaximumSize(t.getMinimumSize());
                l.setLabelFor(t);
                p.add(t);
                p.setMaximumSize(p.getMinimumSize());
                sectionPanel.add(p);
            }

            sectionPanel.setMaximumSize(sectionPanel.getMinimumSize());
        }

        final List<Address> addresses = card.getAddresses();
        if (!addresses.isEmpty()) {
            final JComponent sectionPanel = new Box(BoxLayout.PAGE_AXIS);
            sectionPanel.setBorder(BorderFactory.createTitledBorder("Addresses"));
            view.add(sectionPanel);

            for (final Address a : addresses) {
                System.err.println("a=" + a);
                final JPanel p = new JPanel();
                final JLabel l = new JLabel("URL:");
                l.setMaximumSize(l.getMinimumSize());
                final StringBuilder text = new StringBuilder();
                final List<AddressType> types = a.getTypes();
                if (types != null) {
                    for (final AddressType type : types) {
                        text.append('(' + type.getValue() + ") ");
                    }
                }

                text.append(a.getStreetAddress());

                final JLabel t = new JLabel(text.toString().trim());
                t.setMaximumSize(t.getMinimumSize());
                l.setLabelFor(t);
                p.add(t);
                p.setMaximumSize(p.getMinimumSize());
                sectionPanel.add(p);
            }

            sectionPanel.setMaximumSize(sectionPanel.getMinimumSize());
        }

        final List<Birthday> birthdays = card.getBirthdays();
        if (!birthdays.isEmpty()) {
            final JComponent sectionPanel = new Box(BoxLayout.PAGE_AXIS);
            sectionPanel.setBorder(BorderFactory.createTitledBorder("Birthdays"));
            view.add(sectionPanel);

            for (final Birthday b : birthdays) {
                System.err.println("b=" + b);
                final JPanel p = new JPanel();
                final JLabel l = new JLabel("Birthday:");
                l.setMaximumSize(l.getMinimumSize());
                final StringBuilder text = new StringBuilder();
                text.append(DateFormatSymbols.getInstance().getMonths()[b.getDate().getMonth()]);
                text.append(" ");
                text.append(b.getDate().getDate());
                final String omitYearValue = b.getParameter("X-APPLE-OMIT-YEAR");
                final Calendar c = Calendar.getInstance();
                c.setTime(b.getDate());
                final boolean omitYear = omitYearValue != null;
                System.err.println("omitYear=" + omitYear);
                if (!omitYear) {
                    text.append(", " + c.get(Calendar.YEAR));
                }

                final JLabel t = new JLabel(text.toString().trim());
                t.setMaximumSize(t.getMinimumSize());
                l.setLabelFor(t);
                p.add(t);
                p.setMaximumSize(p.getMinimumSize());
                sectionPanel.add(p);
            }

            sectionPanel.setMaximumSize(sectionPanel.getMinimumSize());
        }

        return view;
    }

    private void updateGUI() {
        updateEnabled();
        updateStatus();
    }

    private void updateEnabled() {
        saveMenuItem.setEnabled(!cards.isEmpty() && cardFile != null);
        saveAsMenuItem.setEnabled(!cards.isEmpty());
    }

    private void updateStatus() {
        final StringBuilder b = new StringBuilder();
        if (cards.isEmpty()) {
            b.append("no contacts");
        } else {
            b.append(cards.size() + " contact");
            if (cards.size() > 1) {
                b.append("s");
            }
        }

        b.append(" ");
        b.append(cardListView.getSelectedIndices().length);
        b.append(" selected");

        statusLabel.setText(b.toString());
    }

    private void save(final File file, final VCardVersion version) throws IOException {
        try {
            getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            final VCardWriter writer = new VCardWriter(file, version);
            for (final VCard c : cards) {
                writer.write(c);
            }

            writer.flush();
            writer.close();
        } finally {
            getContentPane().setCursor(Cursor.getDefaultCursor());
        }
    }

    private void save() {
        try {
            save(cardFile, VCardUtil.highestVersion(cards));
        } catch (final Exception e) {
            System.err.println(e);
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "The file could not be saved:\n" + e.getLocalizedMessage(),
                    "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveAs() {
        final JFileChooser chooser = new JFileChooser();
        final JComponent versionAccessory = new JPanel();
        final JLabel versionLabel = new JLabel("vCard version");
        versionAccessory.add(versionLabel);
        final JComboBox<VCardVersion> versionPicker =
                new JComboBox<>(new VCardVersion[]{VCardVersion.V2_1, VCardVersion.V3_0, VCardVersion.V4_0});
        versionLabel.setLabelFor(versionPicker);
        versionAccessory.add(versionPicker);
        chooser.setAccessory(versionAccessory);
        final String recentDirectory = prefs.get("recentDirectory", null);
        if (recentDirectory != null) {
            final File recentDirectoryFile = new File(recentDirectory);
            if (recentDirectoryFile.exists() && recentDirectoryFile.isDirectory() && recentDirectoryFile.canRead()) {
                chooser.setCurrentDirectory(recentDirectoryFile);
            }
        }

        final int returnValue = chooser.showSaveDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            try {
                final File selectedFile = chooser.getSelectedFile();
                prefs.put("recentDirectory", selectedFile.getParent());
                System.err.println("saving to " + selectedFile);
                save(selectedFile, versionPicker.getItemAt(versionPicker.getSelectedIndex()));
            } catch (final Exception e) {
                System.err.println(e);
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "The file could not be saved:\n" + e.getLocalizedMessage(),
                        "File Error", JOptionPane.ERROR_MESSAGE);
            }
        } else if (returnValue == JFileChooser.ERROR_OPTION) {
            JOptionPane.showMessageDialog(this,
                    "There was an error saving the file.",
                    "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openFile() {
        final JFileChooser chooser = new JFileChooser();
        final String recentDirectory = prefs.get("recentDirectory", null);
        if (recentDirectory != null) {
            final File recentDirectoryFile = new File(recentDirectory);
            if (recentDirectoryFile.exists() && recentDirectoryFile.isDirectory() && recentDirectoryFile.canRead()) {
                chooser.setCurrentDirectory(recentDirectoryFile);
            }
        }

        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new FileFilter() {

            @Override
            public boolean accept(final File file) {
                return file.getName().toLowerCase().endsWith(".vcf");
            }

            @Override
            public String getDescription() {
                return "VCF (vCard) files";
            }

        });
        final int returnValue = chooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            try {
                final File selectedFile = chooser.getSelectedFile();
                prefs.put("recentDirectory", selectedFile.getParent());
                final List<VCard> newCards = open(selectedFile);
                if (newCards.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "The file had no vCards in it.",
                            "File Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    if (cards.isEmpty()) {
                        cards.addAll(newCards);
                        cardListModel.fireContentsChanged();
                        cardFile = selectedFile;
                        updateGUI();
                    } else {
                        final ContactCurator newWindow = new ContactCurator();
                        newWindow.cards.addAll(newCards);
                        newWindow.cardFile = selectedFile;
                        newWindow.updateGUI();
                        newWindow.setVisible(true);
                    }
                }
            } catch (final Exception e) {
                System.err.println(e);
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "The file could not be opened:\n" + e.getLocalizedMessage(),
                        "File Error", JOptionPane.ERROR_MESSAGE);
            }
        } else if (returnValue == JFileChooser.ERROR_OPTION) {
            JOptionPane.showMessageDialog(this,
                    "There was an error opening the file.",
                    "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<VCard> open(final File file) throws IOException {
        System.err.println("opening " + file);
        try {
            getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            final VCardReader r = new VCardReader(file);
            final List<VCard> cards = r.readAll();
            System.err.println("cards=" + cards);
            final List<VCard> cleaned = new ArrayList<>();
            for (final VCard card : cards) {
                cleaned.add(VCardUtil.cleanup(card));
            }

            return cleaned;
        } finally {
            getContentPane().setCursor(Cursor.getDefaultCursor());
        }
    }

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(() -> new ContactCurator().setVisible(true));
    }

}

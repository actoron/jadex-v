package jadex.launcher;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 * GUI application launcher for starting various Jadex applications.
 * Applications are started via reflection, invoking their main() method directly.
 */
@SuppressWarnings("serial")
public class ApplicationLauncher extends JFrame
{
    record ApplicationConfig(String name, Class<?> mainClass, String description, String source) {}
    private static final Insets COMPACT_BUTTON_MARGIN = new Insets(1, 6, 1, 6);
    
    private JTable appTable;
    private List<ApplicationConfig> applications;

    public ApplicationLauncher() {
        super("Jadex Application Launcher");
        initializeApplications();
        setupUI();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void initializeApplications() {
        applications = new ArrayList<>();
        
        // Micro Applications
        applications.add(new ApplicationConfig(
            "Micro - Breakfast",
            jadex.micro.breakfast.Main.class,
            "Shows various ways to start agents.",
            "application/micro/src/main/java/jadex/micro/breakfast/Main.java"
        ));
        applications.add(new ApplicationConfig(
            "Micro - Gobble",
            jadex.micro.gobble.GobbleAgent.class,
            "Variation of Tic Tac Toe as web app.",
            "application/micro/src/main/java/jadex/micro/gobble/GobbleAgent.java"
        ));

        // BDI Applications
    }

    private void setupUI() {
        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top panel with title (controls moved into table area)
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        JLabel titleLabel = new JLabel("Select an application to start:");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        topPanel.add(titleLabel, BorderLayout.WEST);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Middle panel with applications table
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("Available Applications"));
        
        String[] columnNames = {"Name", "Description", "Action", "Source"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // button columns are editable to host the button editors
                return column == 2 || column == 3;
            }
        };

        for (ApplicationConfig app : applications) {
            tableModel.addRow(new Object[]{
                app.name(),
                app.description(),
                "Start",
                "GitHub"
            });
        }

        appTable = new JTable(tableModel);
        appTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        appTable.setRowHeight(25);
        appTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        appTable.getColumnModel().getColumn(1).setPreferredWidth(350);
        appTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        appTable.getColumnModel().getColumn(3).setPreferredWidth(80);

        // Add per-row Start and Source buttons using custom renderer/editor
        appTable.getColumnModel().getColumn(2).setCellRenderer(new ButtonRenderer());
        appTable.getColumnModel().getColumn(2).setCellEditor(new ButtonEditor());
        appTable.getColumnModel().getColumn(3).setCellRenderer(new SourceButtonRenderer());
        appTable.getColumnModel().getColumn(3).setCellEditor(new SourceButtonEditor());

        JScrollPane scrollPane = new JScrollPane(appTable);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        // add table panel to main
        mainPanel.add(tablePanel, BorderLayout.CENTER);

        setContentPane(mainPanel);
    }

    private void startApplicationAtRow(int row) {
        if (row < 0 || row >= applications.size()) {
            JOptionPane.showMessageDialog(this, "Invalid application selection.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            ApplicationConfig app = applications.get(row);

            // Prevent double-start: check action column state
            DefaultTableModel model = (DefaultTableModel) appTable.getModel();
            Object actionVal = model.getValueAt(row, 2);
            if (!"Start".equals(actionVal)) {
                // already running or not startable
                return;
            }

            // mark running in the table (on EDT)
            final DefaultTableModel fmodel = model;
            SwingUtilities.invokeLater(() -> fmodel.setValueAt("Running...", row, 2));

            // Load the class dynamically and invoke main method
            new Thread(() -> {
                try {
                    java.lang.reflect.Method mainMethod = app.mainClass().getMethod("main", String[].class);
                    mainMethod.invoke(null, (Object) new String[]{});
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    // restore button state on EDT when main() returns
                    SwingUtilities.invokeLater(() -> fmodel.setValueAt("Start", row, 2));
                }
            }).start();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error starting application: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void openSourceAtRow(int row) {
        if (row < 0 || row >= applications.size()) {
            JOptionPane.showMessageDialog(this, "Invalid application selection.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ApplicationConfig app = applications.get(row);
        String url = "https://github.com/actoron/jadex-v/blob/main/" + app.source();

        if (!Desktop.isDesktopSupported()) {
            JOptionPane.showMessageDialog(this, "Opening the browser is not supported on this system.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (IOException | RuntimeException e) {
            JOptionPane.showMessageDialog(this, "Error opening source URL: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // Renderer for the Action column that displays a Start button
    private static class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setMargin(COMPACT_BUTTON_MARGIN);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            String text = value == null ? "Start" : value.toString();
            setText(text);
            // disable when not "Start"
            setEnabled("Start".equals(text));
            return this;
        }
    }

    // Renderer for the Source column that always stays enabled
    private static class SourceButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public SourceButtonRenderer() {
            setOpaque(true);
            setMargin(COMPACT_BUTTON_MARGIN);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value == null ? "GitHub" : value.toString());
            setEnabled(true);
            return this;
        }
    }

    // Editor for the Action column that reacts to button presses
    private class ButtonEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor, ActionListener {
        private final JButton button = new JButton("Start");
        private int currentRow = -1;

        public ButtonEditor() {
            button.addActionListener(this);
            button.setMargin(COMPACT_BUTTON_MARGIN);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentRow = row;
            String text = value == null ? "Start" : value.toString();
            button.setText(text);
            button.setEnabled("Start".equals(text));
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return button.getText();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // stop editing first to commit the change
            fireEditingStopped();
            // start the application for this row if still startable
            DefaultTableModel model = (DefaultTableModel) appTable.getModel();
            Object actionVal = model.getValueAt(currentRow, 2);
            if ("Start".equals(actionVal)) {
                startApplicationAtRow(currentRow);
            }
        }
    }

    // Editor for the Source column that opens the GitHub URL for the row
    private class SourceButtonEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor, ActionListener {
        private final JButton button = new JButton("GitHub");
        private int currentRow = -1;

        public SourceButtonEditor() {
            button.addActionListener(this);
            button.setMargin(COMPACT_BUTTON_MARGIN);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentRow = row;
            button.setText(value == null ? "GitHub" : value.toString());
            button.setEnabled(true);
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return button.getText();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            fireEditingStopped();
            openSourceAtRow(currentRow);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ApplicationLauncher launcher = new ApplicationLauncher();
            launcher.setVisible(true);
        });
    }
}

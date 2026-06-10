package jadex.launcher;

import java.awt.BorderLayout;
//import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.table.DefaultTableModel;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.jthemedetecor.OsThemeDetector;

import jadex.common.SScan;
import jadex.common.SUtil;

/**
 * GUI application launcher for starting various Jadex applications.
 * Applications are started via reflection, invoking their main() method directly.
 */
@SuppressWarnings("serial")
public class ApplicationLauncher extends JFrame
{
    record ApplicationConfig(String name, Class<?> mainClass, String description, String source, String icon) implements Comparable<ApplicationConfig>
    {	
    	@Override
    	public int compareTo(ApplicationConfig o)
		{
			return this.name.compareTo(o.name);
		}
    }
    private static final Insets COMPACT_BUTTON_MARGIN = new Insets(0, 0, 0, 0);
    private static final Icon EMPTY_ICON = new ImageIcon(new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB));
    
    private JTable appTable;
    private JTextArea consoleArea;
    private List<ApplicationConfig> applications;

    public ApplicationLauncher() {
        super("Jadex Example Application Launcher");
        this.applications	= scanForApplications();
        setupUI();
        installConsoleRedirection();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    protected static List<ApplicationConfig> scanForApplications() {
    	List<ApplicationConfig>	applications = new ArrayList<>();
        
        // Scan for README.md files in the classpath and add them as applications
        final String	README	= "README.md";
        URL[] urls = SUtil.getClasspathURLs(ApplicationLauncher.class.getClassLoader(), true).toArray(new URL[0]);
        String[]	files	= SScan.scanForFiles(urls, file -> file.toString().endsWith(README));
        Stream.of(files).forEach(readme ->
        {
        	String	package_path;
        	String	contents	= null;
        	
        	// Handle files (e.g. build in IDE)
        	File	freadme	= new File(readme);
        	if(freadme.exists())
			{
        		// Normalize Windows path
        		String	path	= freadme.getAbsolutePath().replace('\\', '/');
        		// Split between classpath location and package path
        		package_path	= path.substring(path.lastIndexOf("/jadex/")+1, path.length() - README.length() -1);
        		
        		// Fetch readme contents
        		try
        		{
        			contents = java.nio.file.Files.readString(freadme.toPath());
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}
			}
        	
			// Handle JARs
			else
			{
				// path is already classpath relative - just remove the README part
				package_path	= readme.substring(0, readme.length() - README.length() -1);
				
				// Fetch readme contents
				URL readmeUrl = ApplicationLauncher.class.getResource("/"+readme);
				try(InputStream	is	= readmeUrl.openStream())
				{
					contents = new String(is.readAllBytes());
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}
			}

//			System.out.println("Found README: "+package_path);
			String	app	= package_path.substring(package_path.lastIndexOf('/') + 1);
			String	project	= package_path.split("/")[1];
			String	src_path	= "application/"+project+"/src/main/java/"+package_path;
			String	display_name	= SUtil.toCamelCase(project, true) + " - " + SUtil.toCamelCase(app, true);
			
			// Find first non-blank, non-header line in the README as description (or fallback if not found)
			String	description	= Stream.of(contents.split("\n"))
					.map(String::trim)
					.filter(line -> !line.isEmpty()
						&& !line.startsWith("#")
						&& !line.startsWith("Main class:")
						&& !line.startsWith("![Icon]("))
					.findFirst()
					.orElse(null);
			
			// Find main class in readme
			String	main	= Stream.of(contents.split("\n"))
					.map(String::trim)
					.filter(line -> line.startsWith("Main class:"))
					.findFirst()
					.orElse(null);
			// Find link part after "Main class:" and try to load it
			Class<?> clazz = null;
			if(main!=null)
			{
				main = package_path.replace('/', '.') + "." +
					main.substring(main.indexOf("(")+1, main.indexOf(".java)")).trim();
//				System.out.println("Looking for main class: "+main);
				try
				{
					clazz = Class.forName(main);
				}
				catch(ClassNotFoundException e)
				{
					System.err.println("Could not find main class "+main+" for application "+display_name);
				}
			}
			
			// Find icon in readme
			final String	ICON_MARKER	= "![Icon](";
			String	icon	= Stream.of(contents.split("\n"))
					.map(String::trim)
					.filter(line -> line.contains(ICON_MARKER))
					.map(line -> "/"+package_path+"/"+line.substring(line.indexOf(ICON_MARKER)+ICON_MARKER.length(), line.indexOf(")")).trim())
					.findFirst()
					.orElse(null);
			
			applications.add(new ApplicationConfig(
				display_name,
				clazz,
				description,
				src_path,
				icon
			));
        });
        
        // Sort applications alphabetically by name
        applications.sort(null);
        return applications;
    }

    private void setupUI() {
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top panel with title (controls moved into table area)
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        JLabel titleLabel = new JLabel("Select an application to start or view online docs:");
        Font currentFont = titleLabel.getFont();
        titleLabel.setFont(currentFont.deriveFont(currentFont.getSize() * 1.25f).deriveFont(Font.BOLD));
        topPanel.add(titleLabel, BorderLayout.WEST);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Middle panel with applications table
        JPanel tablePanel = new JPanel(new BorderLayout());
//        tablePanel.setBorder(BorderFactory.createTitledBorder("Available Applications"));
        
        String[] columnNames = {"Icon", "Name", "Description", "Action", "Docs"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // button columns are editable to host the button editors
                return column == 3 || column == 4;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return Icon.class;
                }
                return String.class;
            }
        };

        for (ApplicationConfig app : applications) {
            Icon appIcon = EMPTY_ICON;
            if (app.icon() != null) {
                URL iconUrl = ApplicationLauncher.class.getResource(app.icon());
                if (iconUrl != null) {
                    appIcon = new ImageIcon(iconUrl);
                }
            }
            tableModel.addRow(new Object[]{
                appIcon,
                app.name(),
                app.description(),
                "Start",
                "GitHub"
            });
        }

        appTable = new JTable(tableModel);
        appTable.setTableHeader(null);
        appTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        appTable.setRowSelectionAllowed(false);
        appTable.setColumnSelectionAllowed(false);
        appTable.setCellSelectionEnabled(false);
        appTable.setFocusable(false);
        appTable.setRowHeight(45);
        appTable.setIntercellSpacing(new Dimension(24, 24));
        appTable.getColumnModel().getColumn(0).setPreferredWidth(45);
        appTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        appTable.getColumnModel().getColumn(2).setPreferredWidth(405);
        appTable.getColumnModel().getColumn(3).setPreferredWidth(75);
        appTable.getColumnModel().getColumn(4).setPreferredWidth(75);

        // Add per-row Start and Source buttons using custom renderer/editor
        appTable.getColumnModel().getColumn(0).setCellRenderer(new IconRenderer());
        appTable.getColumnModel().getColumn(3).setCellRenderer(new ButtonRenderer());
        appTable.getColumnModel().getColumn(3).setCellEditor(new ButtonEditor());
        appTable.getColumnModel().getColumn(4).setCellRenderer(new SourceButtonRenderer());
        appTable.getColumnModel().getColumn(4).setCellEditor(new SourceButtonEditor());

        JScrollPane scrollPane = new JScrollPane(appTable);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        // Bottom console area for captured stdout/stderr from started apps.
        consoleArea = new JTextArea();
        consoleArea.setEditable(false);
        consoleArea.setLineWrap(true);
        consoleArea.setWrapStyleWord(true);
        JScrollPane consoleScrollPane = new JScrollPane(consoleArea);
        consoleScrollPane.setBorder(BorderFactory.createTitledBorder("Console"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tablePanel, consoleScrollPane);
        splitPane.setResizeWeight(0.75);
        splitPane.setDividerLocation(420);

        // add split pane to main
        mainPanel.add(splitPane, BorderLayout.CENTER);

        setContentPane(mainPanel);
    }

    private void installConsoleRedirection() {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;

        PrintStream redirectedOut = new PrintStream(new ConsoleOutputStream(consoleArea, originalOut), true);
        PrintStream redirectedErr = new PrintStream(new ConsoleOutputStream(consoleArea, originalErr), true);
        System.setOut(redirectedOut);
        System.setErr(redirectedErr);
    }

    // Renderer for the Icon column that displays a fixed 20x20 icon
    private static class IconRenderer extends JLabel implements javax.swing.table.TableCellRenderer {
        public IconRenderer() {
            setOpaque(false);
            setHorizontalAlignment(CENTER);
            setVerticalAlignment(CENTER);
            setPreferredSize(new Dimension(20, 20));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof Icon) {
                setIcon((Icon) value);
            } else {
                setIcon(null);
            }
            return this;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(20, 20);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(20, 20);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(20, 20);
        }
    }

    // Renderer for the Action column that displays a Start button
    private static class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setMargin(COMPACT_BUTTON_MARGIN);
//            setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
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
//            setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
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
//            button.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
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
            Object actionVal = model.getValueAt(currentRow, 3);
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
//            button.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
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

    private static class ConsoleOutputStream extends OutputStream {
        private final JTextArea target;
        private final PrintStream fallback;

        private ConsoleOutputStream(JTextArea target, PrintStream fallback) {
            this.target = target;
            this.fallback = fallback;
        }

        @Override
        public void write(int b) throws IOException {
            fallback.write(b);
            appendToConsole(String.valueOf((char) b));
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            fallback.write(b, off, len);
            appendToConsole(new String(b, off, len));
        }

        @Override
        public void flush() throws IOException {
            fallback.flush();
        }

        private void appendToConsole(String text) {
            SwingUtilities.invokeLater(() -> {
                target.append(text);
                target.setCaretPosition(target.getDocument().getLength());
            });
        }
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
            Object actionVal = model.getValueAt(row, 3);
            if (!"Start".equals(actionVal)) {
                // already running or not startable
                return;
            }

            // mark running in the table (on EDT)
            final DefaultTableModel fmodel = model;
            SwingUtilities.invokeLater(() -> fmodel.setValueAt("Running...", row, 3));

            // Load the class dynamically and invoke main method
            new Thread(() -> {
                try {
                    java.lang.reflect.Method mainMethod = app.mainClass().getMethod("main", String[].class);
                    mainMethod.invoke(null, (Object) new String[]{});
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    // restore button state on EDT when main() returns
                    SwingUtilities.invokeLater(() -> fmodel.setValueAt("Start", row, 3));
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
        	if(OsThemeDetector.getDetector().isDark()) {
        		FlatDarkLaf.setup();
        		UIManager.put("Button.background", new ColorUIResource(60, 63, 65));
        		UIManager.put("Button.foreground", new ColorUIResource(220, 220, 220));
        	} else {
        		FlatLightLaf.setup();
        		UIManager.put("Button.background", new ColorUIResource(220, 220, 220));
        		UIManager.put("Button.foreground", new ColorUIResource(30, 30, 30));
        	}
//            UIManager.put("Button.arc", 999);
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.showVerticalLines", false);

            ApplicationLauncher launcher = new ApplicationLauncher();
            launcher.setVisible(true);
        });
    }
}

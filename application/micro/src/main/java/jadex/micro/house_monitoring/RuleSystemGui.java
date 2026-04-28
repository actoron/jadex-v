package jadex.micro.house_monitoring;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import jadex.core.ChangeEvent;
import jadex.micro.house_monitoring.IRuleSystemService.Rule;

/**
 * Panel that displays the registered rules of the rule system in a sortable table.
 */
@SuppressWarnings("serial")
public class RuleSystemGui extends JPanel
{
	// -------- inner multi-line cell renderer --------

	private static class MultiLineCellRenderer extends JTextArea implements TableCellRenderer
	{
		public MultiLineCellRenderer()
		{
			setLineWrap(true);
			setWrapStyleWord(true);
			setOpaque(true);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column)
		{
			setText(value == null ? "" : value.toString());
			setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
			setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
			setFont(table.getFont());

			// Adjust column width for wrapping, then measure preferred height
			setSize(table.getColumnModel().getColumn(column).getWidth(), Short.MAX_VALUE);
			int preferred = getPreferredSize().height;
			if(table.getRowHeight(row) != preferred)
				table.setRowHeight(row, preferred);

			return this;
		}
	}

	// -------- constants --------

	private static final String[] COLUMNS = {"ID", "Event Type", "Source", "Prompt"};

	// -------- inner table model --------

	private static class RuleTableModel extends AbstractTableModel
	{
		private final List<Rule> rules = new ArrayList<>();

		public void addRule(Rule rule)
		{
			rules.add(rule);
			int row = rules.size() - 1;
			fireTableRowsInserted(row, row);
		}

		public void removeRule(String id)
		{
			for(int i = 0; i < rules.size(); i++)
			{
				if(rules.get(i).id().equals(id))
				{
					rules.remove(i);
					fireTableRowsDeleted(i, i);
					return;
				}
			}
		}

		public Rule getRuleAt(int modelRow)
		{
			return rules.get(modelRow);
		}

		@Override
		public int getRowCount()			{ return rules.size(); }

		@Override
		public int getColumnCount()			{ return COLUMNS.length; }

		@Override
		public String getColumnName(int col)	{ return COLUMNS[col]; }

		@Override
		public Object getValueAt(int row, int col)
		{
			Rule r = rules.get(row);
			return switch(col)
			{
				case 0 -> r.id();
				case 1 -> r.type().name();
				case 2 -> r.source();
				case 3 -> r.prompt();
				default -> null;
			};
		}
	}

	// -------- attributes --------

	private final IRuleSystemService	ruleService;
	private final RuleTableModel		model;
	private final JTable				table;
	private final JLabel				statusLabel;

	// -------- constructor --------

	public RuleSystemGui(IRuleSystemService ruleService)
	{
		this.ruleService = ruleService;
		setLayout(new BorderLayout(4, 4));
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		// Title
		add(new JLabel("Rule System"), BorderLayout.NORTH);

		// Table
		model = new RuleTableModel();
		table = new JTable(model);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		table.getColumnModel().getColumn(0).setPreferredWidth(80);
		table.getColumnModel().getColumn(1).setPreferredWidth(130);
		table.getColumnModel().getColumn(2).setPreferredWidth(130);
		table.getColumnModel().getColumn(3).setPreferredWidth(300);
		table.getColumnModel().getColumn(3).setCellRenderer(new MultiLineCellRenderer());
		table.setFillsViewportHeight(true);

		// Make all columns sortable
		TableRowSorter<RuleTableModel> sorter = new TableRowSorter<>(model);
		table.setRowSorter(sorter);

		add(new JScrollPane(table), BorderLayout.CENTER);

		// Toolbar: delete button + status
		JButton deleteBtn = new JButton("Delete selected rule");
		deleteBtn.addActionListener(e -> deleteSelectedRule());

		statusLabel = new JLabel(" ");

		JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		south.add(deleteBtn);
		south.add(statusLabel);
		add(south, BorderLayout.SOUTH);

		// Subscribe to rule changes
		ruleService.subscribeToRules().next(ev -> SwingUtilities.invokeLater(() -> handleEvent(ev)));
	}

	// -------- helpers --------

	private void handleEvent(ChangeEvent ev)
	{
		if(ev.value() instanceof Rule rule)
		{
			if(ev.type() == ChangeEvent.Type.REMOVED)
				model.removeRule(rule.id());
			else // ADDED or INITIAL
				model.addRule(rule);
		}
	}

	private void deleteSelectedRule()
	{
		int viewRow = table.getSelectedRow();
		if(viewRow < 0)
		{
			setStatus("No rule selected.");
			return;
		}
		int modelRow = table.convertRowIndexToModel(viewRow);
		Rule rule = model.getRuleAt(modelRow);

		int confirm = JOptionPane.showConfirmDialog(this,
			"Delete rule \"" + rule.id() + "\"?", "Confirm delete",
			JOptionPane.YES_NO_OPTION);
		if(confirm != JOptionPane.YES_OPTION) return;

		setStatus("Deleting " + rule.id() + "...");
		ruleService.deleteRule(rule.id()).then(v ->
		{
			SwingUtilities.invokeLater(() -> setStatus("Deleted " + rule.id() + "."));
		}).catchEx(ex ->
		{
			SwingUtilities.invokeLater(() -> setStatus("Error: " + ex.getMessage()));
		});
	}

	private void setStatus(String text)
	{
		statusLabel.setText(text);
	}
}

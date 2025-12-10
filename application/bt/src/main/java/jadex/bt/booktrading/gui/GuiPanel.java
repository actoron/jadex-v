package jadex.bt.booktrading.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import jadex.bt.booktrading.domain.INegotiationAgent;
import jadex.bt.booktrading.domain.NegotiationReport;
import jadex.bt.booktrading.domain.Order;
import jadex.common.SGUI;
import jadex.core.IComponentHandle;
import jadex.execution.IExecutionFeature;

/**
 *  The gui allows to add and delete buy or sell orders and shows open and
 *  finished orders.
 */
@SuppressWarnings("serial")
public class GuiPanel extends JPanel
{
	//-------- attributes --------

	private String itemlabel;
//	private String goalname;
	private String addorderlabel;
	private IComponentHandle agent;
	private List<Order> orders = new ArrayList<>();
	private JTable table;
	private DefaultTableModel detailsdm; 
	private AbstractTableModel items = new AbstractTableModel()
	{

		public int getRowCount()
		{
			return orders.size();
		}

		public int getColumnCount()
		{
			return 7;
		}

		public String getColumnName(int column)
		{
			switch(column)
			{
				case 0:
					return "Title";
				case 1:
					return "Start Price";
				case 2:
					return "Limit";
				case 3:
					return "Deadline";
				case 4:
					return "Execution Price";
				case 5:
					return "Execution Date";
				case 6:
					return "State";
				default:
					return "";
			}
		}

		public boolean isCellEditable(int row, int column)
		{
			return false;
		}

		public Object getValueAt(int row, int column)
		{
			Object value = null;
			Order order = (Order)orders.get(row);
			if(column == 0)
			{
				value = order.getTitle();
			}
			else if(column == 1)
			{
				value = Integer.valueOf(order.getStartPrice());
			}
			else if(column == 2)
			{
				value = Integer.valueOf(order.getLimit());
			}
			else if(column == 3)
			{
				Instant deadline = order.getDeadline().toInstant();
				long left = Duration.between(Instant.now(), deadline).getSeconds();
				value= Math.max(0, left);
			}
			else if(column == 4)
			{
				value = order.getExecutionPrice();
			}
			else if(column == 5)
			{
				value = order.getExecutionDate();
			}
			else if(column == 6)
			{
				value = order.getState();
			}
			return value;
		}
	};
	private DateFormat dformat = new SimpleDateFormat("yyyy/MM/dd HH:mm");

	//-------- constructors --------

	/**
	 *  Shows the gui, and updates it when beliefs change.
	 * /
	public GuiPanel(final IComponentHandle agent)
	{
		this(agent, -1);
	}*/

	/**
	 *  Shows the gui, and updates it when beliefs change.
	 */
	public GuiPanel(final IComponentHandle agent, long closedelay)//, final boolean buy)
	{
		setLayout(new BorderLayout());
		
		this.agent = agent;
		final boolean buy = isBuyer(agent);
		
		Timer timer = new Timer(1000, e -> 
		{
			try 
			{
				refresh();
				refreshDetails();
			}
			catch (Exception ex) 
			{
				//System.out.println("gui timer stopped: "+agent.getId());
				((Timer)e.getSource()).stop();
			}
		});
		timer.start();

		if(closedelay >= 0)
		{
			Runnable dispose = () -> 
			{
				Timer t = new Timer((int)closedelay, e -> 
				{
					getFrame().dispose();
					if(timer!=null)
						timer.stop();
				});
				t.setRepeats(false);
				t.start();
			};
			agent.waitForTermination().then(found ->
			{
				dispose.run();
			}).catchEx(ex -> 
			{
				dispose.run();
			});
		}
			
		if(buy)
		{
			itemlabel = " Books to buy ";
//			goalname = "purchase_book";
			addorderlabel = "Add new purchase order";
		}
		else
		{
			itemlabel = " Books to sell ";
//			goalname = "sell_book";
			addorderlabel = "Add new sell order";
		}

		JPanel itempanel = new JPanel(new BorderLayout());
		itempanel.setBorder(new TitledBorder(new EtchedBorder(), itemlabel));

		table = new JTable(items);
		table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer()
		{
			public Component getTableCellRendererComponent(JTable table,
				Object value, boolean selected, boolean focus, int row, int column)
			{
				Component comp = super.getTableCellRendererComponent(table,
					value, selected, focus, row, column);
				setOpaque(true);
				if(column == 0)
				{
					setHorizontalAlignment(LEFT);
				}
				else
				{
					setHorizontalAlignment(RIGHT);
				}
				if(!selected)
				{
					Object state = items.getValueAt(row, 6);
					if(Order.DONE.equals(state))
					{
						comp.setBackground(new Color(211, 255, 156));
					}
					else if(Order.FAILED.equals(state))
					{
						comp.setBackground(new Color(255, 211, 156));
					}
					else
					{
						comp.setBackground(table.getBackground());
					}
				}
				if(value instanceof Date)
				{
					setValue(dformat.format(value));
				}
				return comp;
			}
		});
		table.setPreferredScrollableViewportSize(new Dimension(600, 120));
		
		JScrollPane scroll = new JScrollPane(table);
		itempanel.add(BorderLayout.CENTER, scroll);
		
		detailsdm = new DefaultTableModel(new String[]{"Negotiation Details"}, 0);
		JTable details = new JTable(detailsdm);
		details.setPreferredScrollableViewportSize(new Dimension(600, 120));
		
		JPanel dep = new JPanel(new BorderLayout());
		dep.add(BorderLayout.CENTER, new JScrollPane(details));
	
		JPanel south = new JPanel();
		// south.setBorder(new TitledBorder(new EtchedBorder(), " Control "));
		JButton add = new JButton("Add");
		final JButton remove = new JButton("Remove");
		final JButton edit = new JButton("Edit");
		add.setMinimumSize(remove.getMinimumSize());
		add.setPreferredSize(remove.getPreferredSize());
		edit.setMinimumSize(remove.getMinimumSize());
		edit.setPreferredSize(remove.getPreferredSize());
		south.add(add);
		south.add(remove);
		south.add(edit);
		remove.setEnabled(false);
		edit.setEnabled(false);
		
		JSplitPane splitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splitter.add(itempanel);
		splitter.add(dep);
		splitter.setOneTouchExpandable(true);
		//splitter.setDividerLocation();
		
		add(BorderLayout.CENTER, splitter);
		add(BorderLayout.SOUTH, south);

		/*agent.scheduleStep(ia ->
		{
			INegotiationAgent ag = (INegotiationAgent)ia.getFeature(MicroAgentFeature.class).getSelf().getPojo();
			ag.getAgent().getFeature(IBDIAgentFeature.class).addBeliefListener("orders", new IBeliefListener<Object>()
			{
				public void factRemoved(ChangeInfo<Object> info)
				{
					refresh();
				}
				
				public void factChanged(ChangeInfo<Object> info)
				{
					refresh();
				}
				
				public void factAdded(ChangeInfo<Object> info)
				{
					refresh();
				}
				
				public void beliefChanged(ChangeInfo<Object> info)
				{
					refresh();
				}
			});
		});*/
		
		/*agent.scheduleStep(ia ->
		{
			INegotiationAgent ag = (INegotiationAgent)ia.getFeature(MicroAgentFeature.class).getSelf().getPojo();
			ag.getAgent().getFeature(IBDIAgentFeature.class).addBeliefListener("reports", new IBeliefListener<Object>()
			{
				public void factRemoved(ChangeInfo<Object> info)
				{
					refreshDetails();
				}
				
				public void factChanged(ChangeInfo<Object> info)
				{
					refreshDetails();
				}
				
				public void factAdded(ChangeInfo<Object> info)
				{
					refreshDetails();
				}
				
				public void beliefChanged(ChangeInfo<Object> info)
				{
					refreshDetails();
				}
			});
		});*/
		
		/*Timer timer = new Timer();
		TimerTask tt = new TimerTask() {
			
			@Override
			public void run() 
			{
				SwingUtilities.invokeLater(new Runnable() 
				{
					@Override
					public void run() 
					{
						try
						{
							refresh();
							refreshDetails();
						}
						catch(Exception e)
						{
							//System.out.println("gui timer stopped: "+agent.getId());
							timer.cancel();
						}
					}
				});
			}
		};
		timer.schedule(tt, 1000, 1000);*/
		
		table.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				refreshDetails();
			}
		} );
		
		final InputDialog dia = new InputDialog(buy);
		
		add.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				IExecutionFeature	exe	= agent.scheduleStep(ia -> {return ia.getFeature(IExecutionFeature.class);}).get();
				while(dia.requestInput(exe.getTime()))
				{
					try
					{
						String title = dia.title.getText();
						int limit = Integer.parseInt(dia.limit.getText());
						int start = Integer.parseInt(dia.start.getText());
						Date deadline = dformat.parse(dia.deadline.getText());
						final Order order = new Order(title, exe.getTime(), start, limit, buy);
						
						agent.scheduleStep(ia ->
						{
							IExecutionFeature	iexe	= ia.getFeature(IExecutionFeature.class);
							order.setDeadline(deadline, iexe);
							INegotiationAgent ag = (INegotiationAgent)ia.getPojo();
							ag.createOrder(order);
						});
						orders.add(order);
						items.fireTableDataChanged();
						break;
					}
					catch(NumberFormatException e1)
					{
						JOptionPane.showMessageDialog(GuiPanel.this, "Price limit must be integer.", "Input error", JOptionPane.ERROR_MESSAGE);
					}
					catch(ParseException e1)
					{
						JOptionPane.showMessageDialog(GuiPanel.this, "Wrong date format, use YYYY/MM/DD hh:mm.", "Input error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});

		table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{

			public void valueChanged(ListSelectionEvent e)
			{
				boolean selected = table.getSelectedRow() >= 0;
				remove.setEnabled(selected);
				edit.setEnabled(selected);
			}
		});

		remove.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				int row = table.getSelectedRow();
				if(row >= 0 && row < orders.size())
				{
					final Order order = (Order)orders.remove(row);
					items.fireTableRowsDeleted(row, row);
					
					agent.scheduleStep(ia ->
					{
						INegotiationAgent ag = (INegotiationAgent)ia.getPojo();
						Collection<Order> orders = ag.getOrders();
						for(Order o: orders)
						{
							if(order.equals(o))
							{
								o.setState(Order.FAILED);
								//ag.getAgent().getFeature(IBDIAgentFeature.class).dropGoal(g);
								break;
							}
						}
					});
				}
			}
		});

		final InputDialog edit_dialog = new InputDialog(buy);
		edit.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				IExecutionFeature	exe	= agent.scheduleStep(ia -> {return ia.getFeature(IExecutionFeature.class);}).get();
				int row = table.getSelectedRow();
				if(row >= 0 && row < orders.size())
				{
					final Order order = (Order)orders.get(row);
					edit_dialog.title.setText(order.getTitle());
					edit_dialog.limit.setText(Integer.toString(order.getLimit()));
					edit_dialog.start.setText(Integer.toString(order.getStartPrice()));
					edit_dialog.deadline.setText(dformat.format(order.getDeadline()));

					while(edit_dialog.requestInput(exe.getTime()))
					{
						try
						{
							String title = edit_dialog.title.getText();
							int limit = Integer.parseInt(edit_dialog.limit.getText());
							int start = Integer.parseInt(edit_dialog.start.getText());
							Date deadline = dformat.parse(edit_dialog.deadline.getText());
							order.setTitle(title);
							order.setLimit(limit);
							order.setStartPrice(start);
							items.fireTableDataChanged();
							
							agent.scheduleStep(ia ->
							{
								IExecutionFeature iexe = ia.getFeature(IExecutionFeature.class);
								order.setDeadline(deadline, iexe);
								INegotiationAgent ag = (INegotiationAgent)ia.getPojo();
								Collection<Order> orders = ag.getOrders();
								
								for(Order o: orders)
								{
									if(o.equals(order))
									{
										o.setState(Order.FAILED);
										//ag.getAgent().getFeature(IBDIAgentFeature.class).dropGoal(goal);
									}
								}
								
								ag.createOrder(order);
							});
							break;
						}
						catch(NumberFormatException e1)
						{
							JOptionPane.showMessageDialog(GuiPanel.this, "Price limit must be integer.", "Input error", JOptionPane.ERROR_MESSAGE);
						}
						catch(ParseException e1)
						{
							JOptionPane.showMessageDialog(GuiPanel.this, "Wrong date format, use YYYY/MM/DD hh:mm.", "Input error", JOptionPane.ERROR_MESSAGE);
						}
					}
				}				
			}
		});
		
		refresh();
	}
	
	public static List<Integer> saveSelections(JTable table) 
	{
        List<Integer> selectedModelIndices = new ArrayList<>();
        int[] selectedRows = table.getSelectedRows();

        for (int row : selectedRows) 
        {
            int modelIndex = table.convertRowIndexToModel(row); 
            selectedModelIndices.add(modelIndex);
        }

        return selectedModelIndices;
    }
	
	public static void restoreSelections(JTable table, List<Integer> selectedModelIndices) 
	{
        table.clearSelection();
        for (int modelIndex : selectedModelIndices) 
        {
            if (modelIndex < table.getModel().getRowCount()) 
            {
                int viewIndex = table.convertRowIndexToView(modelIndex); 
                table.addRowSelectionInterval(viewIndex, viewIndex);
            }
        }
    }

//	/**
//	 *  Helper method for dropping a goal.
//	 *  @param goals
//	 *  @param num
//	 */
//	protected void dropGoal(final IEAGoal[] goals, final int num, final Order order)
//	{
//		goals[num].getParameterValue("order").addResultListener(new DefaultResultListener()
//		{
//			public void resultAvailable(Object source, Object result)
//			{
//				if(order.equals(result))
//				{
////					System.out.println("Dropping: "+goals[num]);
//					goals[num].drop();
//				}
//				else if(num+1<goals.length)
//				{
//					dropGoal(goals, num+1, order);
//				}
//			}
//		});
//	}
	
	//-------- methods --------

	/**
	 * Method to be called when goals may have changed.
	 */
	public void refresh()
	{
		agent.scheduleStep(ia ->
		{
			INegotiationAgent ag = (INegotiationAgent)ia.getPojo();

			final List<Order> aorders = ag.getOrders();

			/*System.out.println("REFRESH of agent: "+agent.getId()+" orders: "+aorders);
			for(Order o: aorders)
			{
				System.out.println("order agent: "+o+" "+o.hashCode());
			}*/

			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					List<Integer> sels = saveSelections(table);
					
					for(Order order: aorders)
					{
						if(!orders.contains(order))
						{
							orders.add(order);
						}
					}
					items.fireTableDataChanged();
					
					restoreSelections(table, sels);

					/*for(Order o: orders)
					{
						System.out.println("order gui: "+o+" "+o.hashCode());
					}*/
				}
			});

			return null;
		})
		.catchEx(ex -> 
		{
			System.out.println("Exception during GUI refresh: "+ex);
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					List<Integer> sels = saveSelections(table);
					items.fireTableDataChanged();
					restoreSelections(table, sels);
				}
			});
		});
	}
	
	/**
	 *  Refresh the details panel.
	 */
	public void refreshDetails()
	{
		//if(sel==-1)
		int	sel = table.getSelectedRow();
		if(sel!=-1)
		{
			final Order order = (Order)orders.get(sel);
			
			agent.scheduleStep(ia ->
			{
				INegotiationAgent ag = (INegotiationAgent)ia.getPojo();
				
				final List<NegotiationReport> reps = ag.getReports(order);
				
				Collections.sort(reps, new Comparator<NegotiationReport>()
				{
					public int compare(NegotiationReport o1, NegotiationReport o2)
					{
						return o1.getTime()>o2.getTime()? 1: -1;
					}
				});
				
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						while(detailsdm.getRowCount()>0)
							detailsdm.removeRow(0);
						for(NegotiationReport rep: reps)
						{
							detailsdm.addRow(new Object[]{rep});
							//System.out.println(""+i+res.get(i));
						}
					}
				});
			});
		}
	}

	/**
	 *  Get the frame.
	 */
	public Frame getFrame()
	{
		Container parent = this.getParent();
		while(parent!=null && !(parent instanceof Frame))
			parent = parent.getParent();
		return (Frame)parent;
	}
	
	//-------- inner classes --------

	/**
	 *  The input dialog.
	 */
	private class InputDialog extends JDialog
	{
		@SuppressWarnings("rawtypes")
		private JComboBox orders = new JComboBox();
		private JTextField title = new JTextField(20);
		private JTextField limit = new JTextField(20);
		private JTextField start = new JTextField(20);
		private JTextField deadline = new JTextField(20);
		private boolean aborted;
		private Exception e;

		@SuppressWarnings("unchecked")
		InputDialog(final boolean buy)
		{
			super(getFrame(), addorderlabel, true);

			// Add some default entries for easy testing of the gui.
			// These orders are not added to the agent (see manager.agent.xml).
			try
			{
				IExecutionFeature exe = agent.scheduleStep(ia -> {return ia.getFeature(IExecutionFeature.class);}).get();
				if(buy)
				{
					orders.addItem(new Order("All about agents", exe.getTime(), 100, 120, buy));
					orders.addItem(new Order("All about web services", exe.getTime(), 40, 60, buy));
					orders.addItem(new Order("Harry Potter", exe.getTime(), 5, 10, buy));
					orders.addItem(new Order("Agents in the real world", exe.getTime(), 30, 65, buy));
				}
				else
				{
					orders.addItem(new Order("All about agents", exe.getTime(), 130, 110, buy));
					orders.addItem(new Order("All about web services", exe.getTime(), 50, 30, buy));
					orders.addItem(new Order("Harry Potter", exe.getTime(), 15, 9, buy));
					orders.addItem(new Order("Agents in the real world", exe.getTime(), 100, 60, buy));
				}
			}
			catch(Exception e)
			{
				// happens when killed during startup
			}
							
			JPanel center = new JPanel(new GridBagLayout());
			center.setBorder(new EmptyBorder(5, 5, 5, 5));
			getContentPane().add(BorderLayout.CENTER, center);

			JLabel label;
			Dimension labeldim = new JLabel("Preset orders ").getPreferredSize();
			int row = 0;
			GridBagConstraints leftcons = new GridBagConstraints(0, 0, 1, 1, 0, 0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 0, 0);
			GridBagConstraints rightcons = new GridBagConstraints(1, 0, GridBagConstraints.REMAINDER, 1, 1, 0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 0, 0);

			leftcons.gridy = rightcons.gridy = row++;
			label = new JLabel("Preset orders");
			label.setMinimumSize(labeldim);
			label.setPreferredSize(labeldim);
			center.add(label, leftcons);
			center.add(orders, rightcons);

			leftcons.gridy = rightcons.gridy = row++;
			label = new JLabel("Title");
			label.setMinimumSize(labeldim);
			label.setPreferredSize(labeldim);
			center.add(label, leftcons);
			center.add(title, rightcons);

			leftcons.gridy = rightcons.gridy = row++;
			label = new JLabel("Start price");
			label.setMinimumSize(labeldim);
			label.setPreferredSize(labeldim);
			center.add(label, leftcons);
			center.add(start, rightcons);

			leftcons.gridy = rightcons.gridy = row++;
			label = new JLabel("Price limit");
			label.setMinimumSize(labeldim);
			label.setPreferredSize(labeldim);
			center.add(label, leftcons);
			center.add(limit, rightcons);

			leftcons.gridy = rightcons.gridy = row++;
			label = new JLabel("Deadline");
			label.setMinimumSize(labeldim);
			label.setPreferredSize(labeldim);
			center.add(label, leftcons);
			center.add(deadline, rightcons);


			JPanel south = new JPanel();
			// south.setBorder(new TitledBorder(new EtchedBorder(), " Control "));
			getContentPane().add(BorderLayout.SOUTH, south);

			JButton ok = new JButton("Ok");
			JButton cancel = new JButton("Cancel");
			ok.setMinimumSize(cancel.getMinimumSize());
			ok.setPreferredSize(cancel.getPreferredSize());
			south.add(ok);
			south.add(cancel);

			ok.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					aborted = false;
					setVisible(false);
				}
			});
			cancel.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					setVisible(false);
				}
			});

			orders.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					Order order = (Order)orders.getSelectedItem();
					title.setText(order.getTitle());
					limit.setText("" + order.getLimit());
					start.setText("" + order.getStartPrice());
				}
			});
		}

		public boolean requestInput(long currenttime)
		{
			if(e!=null)
			{
				throw new RuntimeException(e);
			}
			else
			{
				this.deadline.setText(dformat.format(new Date(currenttime + 300000)));
				this.aborted = true;
				this.pack();
				this.setLocation(SGUI.calculateMiddlePosition(getFrame(), this));
				this.setVisible(true);
				return !aborted;
			}
		}
	}
	
	/**
	 *  Test if agent is a buyer.
	 */
	public static boolean isBuyer(IComponentHandle agent)
	{
		return agent.scheduleStep(ia ->
		{
			return ia.getPojo().getClass().getName();
		}).get().indexOf("Buyer")!=-1;
	}
}

/**
 *  The dialog for showing details about negotiations.
 * /
class NegotiationDetailsDialog extends JDialog
{
	/**
	 *  Create a new dialog. 
	 *  @param owner The owner.
	 * /
	public NegotiationDetailsDialog(Frame owner, List details)
	{
		super(owner);
		DefaultTableModel tadata = new DefaultTableModel(new String[]{"Negotiation Details"}, 0);
		JTable table = new JTable(tadata);
		for(int i=0; i<details.size(); i++)
			tadata.addRow(new Object[]{details.get(i)});
		
		JButton ok = new JButton("ok");
		ok.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				NegotiationDetailsDialog.this.setVisible(false);
				NegotiationDetailsDialog.this.dispose();
			}
		});
		JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
		south.add(ok);

		this.getContentPane().add("Center", table);
		this.getContentPane().add("South", south);
		this.setSize(400,200);
		this.setLocation(SGUI.calculateMiddlePosition(this));
		this.setVisible(true);
	}
}*/
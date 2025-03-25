package jadex.micro.helpline;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import jadex.common.SGUI;
import jadex.core.IComponentHandle;
import jadex.future.CollectionResultListener;
import jadex.future.DefaultResultListener;
import jadex.future.IFuture;
import jadex.future.IIntermediateFuture;
import jadex.future.IResultListener;
import jadex.future.ITerminableIntermediateFuture;
import jadex.future.IntermediateFuture;
import jadex.requiredservice.IRequiredServiceFeature;

/**
 *  Helpline gui that allows searching for person info and adding new info.
 */
public class HelplinePanel extends JPanel
{
	//-------- attributes --------
	
	/** The external access of the agent. */
	protected IComponentHandle agent;
	
	//-------- constructors --------
	
	/**
	 *  Create a new gui.
	 */
	public HelplinePanel(final IComponentHandle agent)
	{
		this.agent = agent;
		this.setLayout(new BorderLayout());
		
		JPanel phelp = new JPanel(new BorderLayout());
		
		JPanel pget = new JPanel(new GridBagLayout());
		pget.setBorder(new TitledBorder(new EtchedBorder(), "Search Options"));
		final JTextField tfname = new JTextField("Lennie Lost");
		final JButton bsearchinfo = new JButton("Search");
		final JCheckBox cbremoteinfo = new JCheckBox("Remote");
		pget.add(new JLabel("Person's name"), new GridBagConstraints(0, 0, 1, 1, 0, 0, 
			GridBagConstraints.EAST, GridBagConstraints.VERTICAL, new Insets(1,2,1,2), 0, 0));
		pget.add(tfname, new GridBagConstraints(1, 0, 1, 1, 1, 0, 
			GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(1,2,1,2), 0, 0));
//		pget.add(new JLabel("Remote"), new GridBagConstraints(2, 0, 1, 1, 0, 0, 
//			GridBagConstraints.EAST, GridBagConstraints.VERTICAL, new Insets(1,2,1,2), 0, 0));
		pget.add(cbremoteinfo, new GridBagConstraints(2, 0, 1, 1, 0, 0, 
			GridBagConstraints.EAST, GridBagConstraints.VERTICAL, new Insets(1,2,1,2), 0, 0));
		pget.add(bsearchinfo, new GridBagConstraints(3, 0, 1, 1, 0, 0, 
			GridBagConstraints.EAST, GridBagConstraints.VERTICAL, new Insets(1,2,1,2), 0, 0));
		
		final List infolist = new ArrayList();
		final JTable infotable;
		final AbstractTableModel infomodel = new InfoTableModel(infolist);
		
		bsearchinfo.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				bsearchinfo.setEnabled(false);
				/*getInformation(tfname.getText(), cbremoteinfo.isSelected()).then(result ->
				{
					SwingUtilities.invokeLater(() ->
					{
						infolist.clear();
						if(result!=null)
							infolist.addAll((Collection)result);
						infomodel.fireTableDataChanged();
						bsearchinfo.setEnabled(true);
					});
				}).catchEx(ex ->
				{
					System.out.println(ex);
					bsearchinfo.setEnabled(true);
				});*/
				
				List<InformationEntry> cols = new ArrayList<InformationEntry>();
				getInformation(tfname.getText(), cbremoteinfo.isSelected()).next(res ->
				{
					cols.add(res);
				})
				.finished(Void ->
				{
					SwingUtilities.invokeLater(() ->
					{
						infolist.clear();
						if(cols!=null)
							infolist.addAll((Collection)cols);
						infomodel.fireTableDataChanged();
						bsearchinfo.setEnabled(true);
					});
				}).catchEx(ex ->
				{
					System.out.println(ex);
					bsearchinfo.setEnabled(true);
				});
			}
		});
		
		phelp.add(pget, BorderLayout.NORTH);
		
		JPanel infopanel = new JPanel(new BorderLayout());
		infopanel.setBorder(new TitledBorder(new EtchedBorder(), "Person Information"));
		infotable = new JTable(infomodel);
		infotable.setPreferredScrollableViewportSize(new Dimension(600, 120));
		infotable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		infotable.setDefaultRenderer(Date.class, new DateTimeRenderer());
		infopanel.add(BorderLayout.CENTER, new JScrollPane(infotable));
		
		phelp.add(infopanel, BorderLayout.CENTER);
		
		JPanel padd = new JPanel(new BorderLayout());
		padd.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), "Add Information Entry "));
		JPanel pss = new JPanel(new GridBagLayout());
	
		final JComboBox cbselser = new JComboBox(); 
		final JCheckBox cbremoteser = new JCheckBox("Remote");
		final JButton bsearchser = new JButton("Search");
		JLabel selsl = new JLabel("Select service");
		pss.add(selsl, new GridBagConstraints(0, 0, 1, 1, 0, 0, 
			GridBagConstraints.EAST, GridBagConstraints.VERTICAL, new Insets(1,2,1,2), 0, 0));
		pss.add(cbselser,  new GridBagConstraints(1, 0, 1, 1, 1, 0, 
			GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(1,2,1,2), 0, 0));
		pss.add(cbremoteser, new GridBagConstraints(2, 0, 1, 1, 0, 0, 
			GridBagConstraints.EAST, GridBagConstraints.VERTICAL, new Insets(1,2,1,2), 0, 0));
		pss.add(bsearchser, new GridBagConstraints(3, 0, 1, 1, 0, 0, 
			GridBagConstraints.EAST, GridBagConstraints.VERTICAL, new Insets(1,2,1,2), 0, 0));;
		
		bsearchser.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				refreshServicesCombo(cbselser, cbremoteser.isSelected());
			}
		});
		
		padd.add(pss, BorderLayout.NORTH);
		
		final JPanel pinfoentry = new JPanel(new GridBagLayout());
		JLabel lname = new JLabel("Name");
		lname.setPreferredSize(selsl.getPreferredSize());
		pinfoentry.add(lname, new GridBagConstraints(0, 0, 1, 1, 0, 0, 
			GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(1,2,1,2), 0, 0));
		final JTextField tfpname = new JTextField("Lennie Lost");
		pinfoentry.add(tfpname, new GridBagConstraints(1, 0, 1, 1, 1, 0, 
			GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(1,2,1,2), 0, 0));
		pinfoentry.add(new JLabel("Information"), new GridBagConstraints(2, 0, 1, 1, 0, 0, 
			GridBagConstraints.EAST, GridBagConstraints.VERTICAL, new Insets(1,12,1,2), 0, 0));
		final JTextField tfpinfo = new JTextField(8);
		pinfoentry.add(tfpinfo, new GridBagConstraints(3, 0, 1, 1, 3, 0, 
			GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(1,2,1,2), 0, 0));
		final JButton badd = new JButton("Add");
		badd.setPreferredSize(bsearchser.getPreferredSize());
		badd.setEnabled(false);
		pinfoentry.add(badd, new GridBagConstraints(4, 0, 1, 1, 0, 0, 
			GridBagConstraints.EAST, GridBagConstraints.VERTICAL, new Insets(1,2,1,2), 0, 0));
		
		badd.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				IHelpline hl = (IHelpline)cbselser.getSelectedItem();
				if(hl!=null)
				{
					agent.scheduleStep(ag ->
					{
						hl.addInformation(tfpname.getText(), tfpinfo.getText());
					});
				}
			}
		});
		
		cbselser.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				IHelpline hl = (IHelpline)cbselser.getSelectedItem();
				if(hl!=null)
				{
					badd.setEnabled(true);
				}
				else
				{
					badd.setEnabled(false);
				}
			}
		});
		
		padd.add(pinfoentry, BorderLayout.CENTER);
		
		phelp.add(padd, BorderLayout.SOUTH);
		
		this.add(phelp, BorderLayout.CENTER);
		
		refreshServicesCombo(cbselser, cbremoteser.isSelected());
	}
	
	/**
	 *  Refresh the service combo box.
	 */
	protected void refreshServicesCombo(final JComboBox selcb, final boolean remote)
	{
//		SServiceProvider.getServices(agent.getServiceProvider(), IHelpline.class, remote, true)
//			.addResultListener(new SwingDefaultResultListener(HelplinePanel.this)
//			(agent.getServiceProvider(), IHelpline.class, remote, true)
		agent.scheduleStep(ia ->
		{
//			ITerminableIntermediateFuture<IHelpline> fut = ia.getFeature(IRequiredServiceFeature.class).getServices(
//					remote ? "remotehelplineservices" : "localhelplineservices");
			ITerminableIntermediateFuture<IHelpline> fut = ia.getFeature(IRequiredServiceFeature.class).searchServices(IHelpline.class);
			
			List<IHelpline> col = new ArrayList<IHelpline>();
			fut.next(result ->
			{
				//System.out.println("found: "+result);
				col.add(result);
			}).finished(Void -> 
			{
				selcb.removeAllItems();
				for(Iterator it=col.iterator(); it.hasNext(); )
				{
					selcb.addItem(it.next());
				}
			})
			.catchEx(ex -> 
			{
				ex.printStackTrace();
			});
			
			/*fut.then(result ->
			{
				System.out.println("found: "+result);
				Collection newservices = (Collection)result;
				
				selcb.removeAllItems();
				if(newservices!=null)
				{
					for(Iterator it=newservices.iterator(); it.hasNext(); )
					{
						selcb.addItem(it.next());
					}
				}
			}).catchEx(ex -> 
			{
				ex.printStackTrace();
			});*/
		});
	}
	
	/**
	 *  Get all information about a person.
	 *  @param name The person's name.
	 *  @return Future that contains the information.
	 */
	public IIntermediateFuture<InformationEntry> getInformation(final String name, final boolean remote)
	{
//		SServiceProvider.getServices(agent.getServiceProvider(), IHelpline.class, remote, true)
		final IntermediateFuture<InformationEntry> ret = new IntermediateFuture<InformationEntry>();
		
		/*IIntermediateFuture<IHelpline> fut = (IIntermediateFuture<IHelpline>)agent.scheduleStep(ia ->
		{
			IIntermediateFuture<IHelpline> fut;
			if(remote)
			{
				fut	= ia.getFeature(IRequiredServiceFeature.class).getServices("remotehelplineservices");
			}
			else
			{
				fut	= ia.getFeature(IRequiredServiceFeature.class).getServices("localhelplineservices");
			}
			return fut;
		});
		
			fut.addResultListener(new IResultListener()
		{
			public void resultAvailable(Object result)
			{
				if(result!=null)
				{
					Collection coll = (Collection)result;
					CollectionResultListener crl = new CollectionResultListener(
						coll.size(), true, new DefaultResultListener()
					{
						public void resultAvailable(Object result)
						{
							if(result!=null)
							{
								Collection tmp = (Collection)result;
								Iterator it = tmp.iterator();
								List all = new ArrayList();
								for(; it.hasNext(); )
								{
									Collection part = (Collection)it.next();
									for(Iterator it2=part.iterator(); it2.hasNext(); )
									{
										Object next = it2.next();
										if(next instanceof InformationEntry && !all.contains(next))
											all.add(next);
									}
								}
								// Sorts the list by date.
								Collections.sort(all);
								
								ret.setResult(all);
							}
							else
							{
								ret.setResult(null);
							}
						}
					});
					for(Iterator it=coll.iterator(); it.hasNext(); )
					{
						IHelpline hl = (IHelpline)it.next();
						IFuture res = hl.getInformation(name);
//						res.addResultListener(new DefaultResultListener()
//						{
//							public void resultAvailable(Object result)
//							{
//								System.out.println("result"+result);
//							}
//						});
						res.addResultListener(crl);
					}
				}
			}
			
			public void exceptionOccurred(Exception exception)
			{
				ret.setException(exception);
			}
		});*/
		
		// hack! does not work 
		agent.scheduleStep(ia ->
		{
			IIntermediateFuture<IHelpline> fut;
//			if(remote)
//			{
//				fut	= ia.getFeature(IRequiredServiceFeature.class).getServices("remotehelplineservices");
//			}
//			else
			{
				fut	= ia.getFeature(IRequiredServiceFeature.class).searchServices(IHelpline.class);
			}
			
			fut.addResultListener(new IResultListener()
			{
				public void resultAvailable(Object result)
				{
					if(result!=null)
					{
						Collection coll = (Collection)result;
						CollectionResultListener crl = new CollectionResultListener(
							coll.size(), true, new DefaultResultListener()
						{
							public void resultAvailable(Object result)
							{
								if(result!=null)
								{
									Collection tmp = (Collection)result;
									Iterator it = tmp.iterator();
									List all = new ArrayList();
									for(; it.hasNext(); )
									{
										Collection part = (Collection)it.next();
										for(Iterator it2=part.iterator(); it2.hasNext(); )
										{
											Object next = it2.next();
											if(next instanceof InformationEntry && !all.contains(next))
												all.add(next);
										}
									}
									// Sorts the list by date.
									Collections.sort(all);
									
									ret.setResult(all);
								}
								else
								{
									ret.setResult(null);
								}
							}
						});
						for(Iterator it=coll.iterator(); it.hasNext(); )
						{
							IHelpline hl = (IHelpline)it.next();
							IFuture res = hl.getInformation(name);
//							res.addResultListener(new DefaultResultListener()
//							{
//								public void resultAvailable(Object result)
//								{
//									System.out.println("result"+result);
//								}
//							});
							res.addResultListener(crl);
						}
					}
				}
				
				public void exceptionOccurred(Exception exception)
				{
					ret.setException(exception);
				}
			});
		});
			
		return ret;
	}
	
	/**
	 *  Create a customer gui frame.
	 */
	public static void createHelplineGui(final IComponentHandle agent)
	{
		final JFrame f = new JFrame();
		f.add(new HelplinePanel(agent));
		f.pack();
		f.setLocation(SGUI.calculateMiddlePosition(f));
		f.setVisible(true);
		f.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				agent.terminate();
			}
		});
		
		// Dispose frame on exception.
		IResultListener<Void>	dislis	= new IResultListener<Void>()
		{
			public void exceptionOccurred(Exception exception)
			{
				f.dispose();
			}
			public void resultAvailable(Void result)
			{
			}
		};
		
		agent.waitForTermination().then(b -> f.dispose());
	}
}

class InfoTableModel extends AbstractTableModel
{
	protected List list;
	
	public InfoTableModel(List list)
	{
		this.list = list;
	}
	
	public int getRowCount()
	{
		return list.size();
	}

	public int getColumnCount()
	{
		return 2;
	}

	public String getColumnName(int column)
	{
		switch(column)
		{
			case 0:
				return "Date and Time";
			case 1:
				return "Information";
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
		InformationEntry ie = (InformationEntry)list.get(row);
		if(column == 0)
		{
			value = new Date(ie.getDate());
		}
		else if(column == 1)
		{
			value = ie.getInformation();
		}
		
		return value;
	}
	
	public Class getColumnClass(int column)
	{
		Class ret = Object.class;
		if(column == 0)
		{
			ret = Date.class;
		}
		else if(column == 1)
		{
			ret = String.class;
		}
		return ret;
	}
};

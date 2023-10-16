package jadex.micro.tutorial.a4;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import jadex.mj.core.IComponent;
import jadex.mj.core.IExternalAccess;
import jadex.mj.core.IThrowingConsumer;
import jadex.mj.feature.execution.IMjExecutionFeature;
import jadex.mj.micro.MjMicroAgent;

/**
 *  Basic chat user interface.
 */
public class ChatGui extends JFrame
{
	//-------- attributes --------
	
	/** The textfield with received messages. */
	protected JTextArea received;
	
	//-------- constructors --------
	
	/**
	 *  Create the user interface
	 */
	public ChatGui(final IExternalAccess access)
	{
		super(""+access.getId());
		this.setLayout(new BorderLayout());
		
		received = new JTextArea(10, 20);
		final JTextField message = new JTextField();
		JButton send = new JButton("send");
		
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(message, BorderLayout.CENTER);
		panel.add(send, BorderLayout.EAST);
		
		getContentPane().add(new JScrollPane(received), BorderLayout.CENTER);
		getContentPane().add(panel, BorderLayout.SOUTH);
		
		message.addActionListener(new ActionListener() 
		{
            public void actionPerformed(ActionEvent e) 
            {
                sendMessage(access, message.getText());
                message.setText("");
            }
        });

		send.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				sendMessage(access, message.getText());
				message.setText("");
			}
		});
		
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				access.scheduleStep(agent ->
				{
					agent.getFeature(IMjExecutionFeature.class).terminate();
				});
			}
		});
		
		pack();
		setVisible(true);
	}
	
	/**
	 *  Method to add a new text message.
	 *  @param text The text.
	 */
	public void addMessage(final String text)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				received.append(text+"\n");
			}
		});
	}

	public void sendMessage(IExternalAccess access, String text)
	{
		access.scheduleStep((IThrowingConsumer<IComponent>)agent ->
		{
			MjMicroAgent magent = (MjMicroAgent)agent;
			ChatAgent pojo = (ChatAgent)magent.getPojo();
			Collection<IChatService> chatservices = pojo.getChatServices();
			
			// This would search the services
			//Collection<IChatService> chatservices = agent.getFeature(IMjRequiredServiceFeature.class).getLocalServices(IChatService.class);
			
			System.out.println("found services: "+chatservices.size());
			for(Iterator<IChatService> it=chatservices.iterator(); it.hasNext(); )
			{
				IChatService cs = it.next();
				cs.message(agent.getId()+"", text);
			}
		});
	}
	
	// test cases for casts last one does not work
	/*access.scheduleStep(agent ->
	{
		agent.getFeature(IMjLifecycleFeature.class).terminate();
	});
	
	access.scheduleStep(agent ->
	{
		System.out.println("hi");
		return null;
	});
	
	access.scheduleStep(agent ->
	{
		System.out.println("hi");
		System.out.println("hi2");
		System.out.println("hi3");
	});
	
	access.scheduleStep(agent ->
	{
		System.out.println("hi");
		System.out.println("hi2");
		for(int i=0; i<10; i++)
		{
			System.out.println("cool");
		}
	});*/
}

package jadex.micro.helpline;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.SwingUtilities;

import jadex.collection.MultiCollection;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.execution.IExecutionFeature;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;

/**
 *  Helpline micro agent. 
 */
//@Description("This agent offers a helpline for getting information about missing persons.")
//@RequiredServices({
//	@RequiredService(name="remotehelplineservices", type=IHelpline.class, scope=ServiceScope.GLOBAL), //multiple=true,
//	@RequiredService(name="localhelplineservices", type=IHelpline.class, scope=ServiceScope.VM) //multiple=true,
//})
//@ProvidedServices(@ProvidedService(type=IHelpline.class, implementation=@Implementation(HelplineService.class), scope=ServiceScope.GLOBAL))
//@Agent
public class HelplineAgent
{
	//-------- attributes --------
	
	/** The agent. */
	@Inject
	protected IComponent agent;
	
	/** The provided service. */
	protected IHelpline	helpline	= new HelplineService();
	
	/** The map of information. */
	protected MultiCollection<String, InformationEntry> infos;
	
	//-------- methods --------
	
	public HelplineAgent(InformationEntry[] ini)
	{
		this.infos = new MultiCollection<String, InformationEntry>();
		for(InformationEntry ie: ini)
		{
			infos.add(ie.getName(), ie);
		}
	}
	
	public HelplineAgent(List<InformationEntry> ini)
	{
		this.infos = new MultiCollection<String, InformationEntry>();
		for(InformationEntry ie: ini)
		{
			infos.add(ie.getName(), ie);
		}
	}
	
	/**
	 *  Called once after agent creation.
	 */
	@OnStart
	public IFuture<Void> agentCreated()
	{
//		this.infos = new MultiCollection(new HashMap(), TreeSet.class);
		
//		addService(new HelplineService(this));
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				HelplinePanel.createHelplineGui((IComponentHandle)agent.getComponentHandle());
			}
		});
		return IFuture.DONE;
	}
		
	/**
	 *  Add an information about a person.
	 *  @param name The person's name.
	 *  @param info The information.
	 */
	public void addInformation(final String name, final String info)
	{
		infos.add(name, new InformationEntry(name, info, agent.getFeature(IExecutionFeature.class).getTime()));
	}
	
	/**
	 *  Get all locally stored information about a person.
	 *  @param name The person's name.
	 *  @return Future that contains the information.
	 */
	public Collection<InformationEntry> getInformation(String name)
	{
		Collection<InformationEntry> ret	= infos.get(name); 
		return ret!=null ? ret : Collections.emptyList();
	}

	/**
	 *  Get the agent.
	 *  @return The agent
	 */
	public IComponent getAgent()
	{
		return agent;
	}

}

package jadex.micro.mandelbrot_new.display;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jadex.classreader.SClassReader;
import jadex.classreader.SClassReader.ClassFileInfo;
import jadex.common.ClassInfo;
import jadex.common.FileFilter;
import jadex.common.SUtil;
import jadex.core.IComponent;
import jadex.core.impl.ComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.SubscriptionIntermediateFuture;
import jadex.future.TerminationCommand;
import jadex.micro.mandelbrot_new.model.AbstractFractalAlgorithm;
import jadex.micro.mandelbrot_new.model.AreaData;
import jadex.micro.mandelbrot_new.model.IFractalAlgorithm;
import jadex.micro.mandelbrot_new.model.PartDataChunk;
import jadex.micro.mandelbrot_new.model.ProgressData;
import jadex.providedservice.annotation.Service;
import jadex.providedservice.annotation.ServiceComponent;

/**
 *  The service allows displaying results in the frame
 *  managed by the service providing agent.
 */
@Service
public class DisplayService implements IDisplayService
{
	//-------- attributes --------
	
	/** The agent. */
	@ServiceComponent
	protected IComponent agent;
	
	/** The display subscribers. */
	protected Map<String, SubscriptionIntermediateFuture<Object>> subscribers = new HashMap<String, SubscriptionIntermediateFuture<Object>>();

	/** Store results till display subscribed */
	protected List<Object> storedresults = new ArrayList<>();
	
	//-------- IDisplayService interface --------

	/**
	 *  Display the result of a calculation.
	 */
	public IFuture<Void> displayResult(AreaData result)
	{
		internalDisplayResult(result, true);
		return IFuture.DONE;
	}
	
	/**
	 *  Display the result of a calculation.
	 */
	protected boolean internalDisplayResult(AreaData result, boolean store)
	{
		boolean consumed = false;
//		System.out.println("displayRes: "+agent.getComponentIdentifier());
//		agent.getPanel().setResults(result);
		String id = result.getDisplayId();
		if(id!=null)
		{
			SubscriptionIntermediateFuture<Object> sub = subscribers.get(id);
			if(sub==null)
			{
				if(store)
					storedresults.add(result);
			}
			else
			{
				sub.addIntermediateResult(result);
			}
		}
		else
		{			
			if(subscribers.values().isEmpty())
			{
				if(store)
					storedresults.add(result);
			}
			else
			{
				// todo: use default display
				for(Iterator<SubscriptionIntermediateFuture<Object>> it=subscribers.values().iterator(); it.hasNext(); )
				{
					SubscriptionIntermediateFuture<Object> sub = it.next();
					sub.addIntermediateResult(result);
				}
			}
		}
		return consumed;
	}

	/**
	 *  Display intermediate calculation results.
	 */
	public IFuture<Void> displayIntermediateResult(ProgressData progress)
	{
		//System.out.println("displayInRes: "+progress);
//		agent.getPanel().addProgress(progress);
		String id = progress.getDisplayId();
		if(id!=null)
		{
			SubscriptionIntermediateFuture<Object> sub = subscribers.get(id);
			sub.addIntermediateResult(progress);
		}
		else
		{
			// todo: use default display
			for(Iterator<SubscriptionIntermediateFuture<Object>> it=subscribers.values().iterator(); it.hasNext(); )
			{
				SubscriptionIntermediateFuture<Object> sub = it.next();
				sub.addIntermediateResult(progress);
			}
		}
		
		return IFuture.DONE;
	}
	
	/**
	 *  Display intermediate calculation results.
	 */
	public IFuture<Void> displayIntermediateResult(PartDataChunk data)
	{
		internalDisplayIntermediateResult(data, true);
		return IFuture.DONE;
	}
	
	/**
	 *  Display intermediate calculation results.
	 */
	protected boolean internalDisplayIntermediateResult(PartDataChunk data, boolean store)
	{
		boolean consumed = false;
		
		//System.out.println("displayInRes: "+progress);
//		agent.getPanel().addProgress(progress);
		String id = data.getDisplayId();
		if(id!=null)
		{
			SubscriptionIntermediateFuture<Object> sub = subscribers.get(id);
			if(sub==null)
			{
				if(store)
					storedresults.add(data);
			}
			else
			{
				sub.addIntermediateResult(data);
				consumed = true;
			}
		}
		else
		{
			if(subscribers.values().isEmpty())
			{
				if(store)
					storedresults.add(data);
			}
			else
			{
				// todo: use default display
				for(Iterator<SubscriptionIntermediateFuture<Object>> it=subscribers.values().iterator(); it.hasNext(); )
				{
					SubscriptionIntermediateFuture<Object> sub = it.next();
					sub.addIntermediateResult(data);
				}
				consumed = true;
			}
		}
		
		return consumed;
	}
	
	/**
	 *  Subscribe to display events.
	 */
	public ISubscriptionIntermediateFuture<Object> subscribeToDisplayUpdates(String displayid)
	{
		//System.out.println("subscribeToDisplay: "+displayid);
//		SubscriptionIntermediateFuture<Object> ret = new SubscriptionIntermediateFuture<Object>();
		//final SubscriptionIntermediateFuture<Object> ret = (SubscriptionIntermediateFuture<Object>)SFuture.getNoTimeoutFuture(SubscriptionIntermediateFuture.class, agent);
		final SubscriptionIntermediateFuture<Object> ret = new SubscriptionIntermediateFuture<Object>();
		
		ret.setTerminationCommand(new TerminationCommand()
		{
			public void terminated(Exception reason)
			{
				subscribers.remove(displayid);
				//System.out.println("removed display: "+displayid+" "+reason);
			}
		});
		subscribers.put(displayid, ret);
		
		// Send out stored results in case of first subscription
		if(subscribers.size()==1 && storedresults.size()>0)
		{
			//System.out.println("sending old results");
			List<Object> toremove = new ArrayList<>();
			storedresults.stream()
			.forEach(o -> 
			{
				if(o instanceof AreaData)
					if(internalDisplayResult((AreaData)o, false))
						toremove.add(o);
				else if(o instanceof PartDataChunk)
					if(internalDisplayIntermediateResult((PartDataChunk)o, false))
						toremove.add(o);
			});
			storedresults.removeAll(toremove);
		}
		
		return ret;
	}
	
	/**
	 *  Get info about an algorithm (for web). todo: move?!
	 *  @return The info.
	 */
	public IFuture<AreaData> getAlgorithmDefaultSettings(Class<IFractalAlgorithm> clazz)
	{
		try
		{
			return new Future<AreaData>(clazz.getDeclaredConstructor().newInstance().getDefaultSettings());
		}
		catch(Exception e)
		{
			return new Future<AreaData>(e);
		}
	}
	
	/**
	 *  Get available algorithms.
	 *  @return The algos.
	 */
	public IFuture<List<Class<IFractalAlgorithm>>> getAlgorithms()
	{
		ClassLoader cl = ComponentManager.get().getClassLoader();
		URL[] urls = SUtil.getClasspathURLs(cl, false).toArray(new URL[0]);

		Set<ClassFileInfo> algos = SClassReader.scanForClassFileInfos(urls, new FileFilter("Algorithm", true, ".class"), cfi ->
		{
			return cfi.getClassInfo().getSuperClassName().indexOf("AbstractFractalAlgorithm")!=-1;
		});
		
		// move the default algo at first position
		
		List<Class<IFractalAlgorithm>> ret = algos.stream().map(cfi -> (Class<IFractalAlgorithm>)new ClassInfo(cfi.getClassInfo().getClassName()).getType(cl)).collect(Collectors.toList());
		List<IFractalAlgorithm> algs = AbstractFractalAlgorithm.createAlgorithms(ret);
		IFractalAlgorithm def = algs.stream()
			.filter(IFractalAlgorithm::isDefault)
			.findFirst()
			.orElse(null);
		
		int idx = ret.indexOf(def.getClass());
		Class<IFractalAlgorithm> dec = ret.remove(idx);
		ret.add(0, dec);
		
		return new Future<List<Class<IFractalAlgorithm>>>(ret);
	}
}

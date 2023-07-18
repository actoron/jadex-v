package jadex.enginecore.service.component.multiinvoke;

import java.lang.reflect.Method;

import jadex.future.Future;
import jadex.future.IIntermediateResultListener;

/**
 * 
 */
public interface IMultiplexCollector extends IIntermediateResultListener<Object>
{
	/**
	 *  Init the collector.
	 */
	public void init(Future<Object> fut, Method method, Object[] args, Method muxmethod);

}

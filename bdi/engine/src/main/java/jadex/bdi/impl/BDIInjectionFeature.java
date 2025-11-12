package jadex.bdi.impl;

import jadex.bdi.impl.plan.RPlan;
import jadex.core.ChangeEvent;
import jadex.injection.impl.InjectionFeature;

/**
 *  Overridden injection feature for atomic event processing.
 */
public class BDIInjectionFeature extends InjectionFeature
{
	/**
	 *  Create the feature.
	 */
	public BDIInjectionFeature(BDIAgent self)
	{
		super(self);
	}
	
	@Override
	public void valueChanged(ChangeEvent event)
	{
		// Avoid plan self-abort inside event processing
		RPlan	rplan	= RPlan.RPLANS.get();
		if(rplan!=null && !rplan.isAtomic())
		{
			try
			{
				rplan.startAtomic();
				super.valueChanged(event);
			}
			finally
			{
				rplan.endAtomic();
			}
		}
		
		else
		{
			super.valueChanged(event);
		}
	}
}

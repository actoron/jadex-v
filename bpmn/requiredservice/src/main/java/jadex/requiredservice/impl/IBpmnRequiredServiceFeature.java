package jadex.requiredservice.impl;

import jadex.core.IComponentFeature;
import jadex.future.IFuture;

public interface IBpmnRequiredServiceFeature	extends IComponentFeature
{
	IFuture<Object> getService(String service);

	RequiredServiceInfo getServiceInfo(String fservice);
}

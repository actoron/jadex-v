package jadex.mj.publishservice.impl;

import java.util.HashMap;
import java.util.Map;

public class PublishServiceModel 
{
	protected Map<String, PublishInfo> publishinfos;
	
	public PublishServiceModel()
	{
	}

	public PublishInfo[] getPublishInfos()
	{
		return publishinfos==null? new PublishInfo[0]: publishinfos.values().toArray(new PublishInfo[publishinfos.size()]);
	}
	
	public void addPublishInfo(PublishInfo info)
	{
		if(publishinfos==null)
			publishinfos = new HashMap<String, PublishInfo>();
		publishinfos.put(info.getPublishTarget(), info);
	}
	
	public PublishInfo getPublishInfo(String name)
	{
		return publishinfos==null? null: publishinfos.get(name);
	}
	
	public boolean hasService(String name)
	{
		return publishinfos==null? false: publishinfos.containsKey(name);
	}
}

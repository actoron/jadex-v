package jadex.bdiv3.model;

import java.util.ArrayList;
import java.util.List;

import jadex.common.FieldInfo;
import jadex.common.Tuple2;
import jadex.mj.micro.MicroModel;
import jadex.mj.model.modelinfo.IModelInfo;

/**
 * 
 */
public class BDIModel extends MicroModel	implements IBDIModel
{
	/** The subcapabilities. */
	protected List<Tuple2<FieldInfo, BDIModel>> subcapabilities;

	/** The capability. */
	protected MCapability mcapa;

	/**
	 *  Create a new model.
	 */
	public BDIModel(IModelInfo modelinfo, MCapability mcapa)
	{
		super(modelinfo);
		this.mcapa = mcapa;
	}

//	/**
//	 *  Get a capability by name.
//	 *  @return The mcapa.
//	 */
//	public MCapability getCapability(String name)
//	{
//		MCapability ret = null;
//		int idx = name.indexOf(MElement.CAPABILITY_SEPARATOR);
//		if(idx!=-1)
//		{
//			String capaname = name.substring(0, idx-1);
//			String rest = name.substring(idx+1);
//			if(subcapabilities!=null)
//			{
//				BDIModel subcap = null;
//				for(Tuple2<FieldInfo, BDIModel> tup: subcapabilities)
//				{
//					subcap = tup.getSecondEntity();
//					if(subcap.getCapability().getName().equals(capaname))
//					{
//						break;
//					}
//				}
//				if(subcap==null)
//				{
//					throw new RuntimeException("Capability not found: "+capaname);
//				}
//				ret = subcap.getCapability(rest);
//			}
//		}
//		else
//		{
//			ret = mcapa;
//		}
//		return ret;
//	}
	
	/**
	 *  Get the mcapa.
	 *  @return The mcapa.
	 */
	public MCapability getCapability()
	{
		return mcapa;
	}

	/**
	 *  Set the mcapa.
	 *  @param mcapa The mcapa to set.
	 */
	public void setCapability(MCapability mcapa)
	{
		this.mcapa = mcapa;
	}
	
	/**
	 *  Add a subcapability field.
	 *  @param field The field. 
	 */
	public void addSubcapability(FieldInfo field, BDIModel model)
	{
		if(subcapabilities==null)
		{
			subcapabilities = new ArrayList<Tuple2<FieldInfo, BDIModel>>();
		}
		subcapabilities.add(new Tuple2<FieldInfo, BDIModel>(field, model));
	}
	
	/**
	 *  Get the agent injection fields.
	 *  @return The fields.
	 */
	public Tuple2<FieldInfo, BDIModel>[] getSubcapabilities()
	{
		@SuppressWarnings("unchecked")
		Tuple2<FieldInfo, BDIModel>[]	ret	= subcapabilities==null? new Tuple2[0]: (Tuple2<FieldInfo, BDIModel>[])subcapabilities.toArray(new Tuple2[subcapabilities.size()]);
		return ret;
	}
}

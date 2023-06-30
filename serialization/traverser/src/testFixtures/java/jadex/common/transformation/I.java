package jadex.common.transformation;

import jadex.common.SUtil;
import jadex.common.transformation.annotations.IncludeFields;

@IncludeFields(includePrivate = true)
public class I
{

	private String string2;

	private static final String finalStaticString = SUtil.createUniqueId();

	public I() {
		super();
	}

	public I(String string2)
	{
		this.string2 = string2;
	}

	public int hashCode()
	{
		return ((string2 == null)? 0 : string2.hashCode()) + super.hashCode();
	}

	public boolean equals(Object obj)
	{
		boolean ret = false;
		
		if(obj instanceof I)
		{
			I other = (I)obj;
			ret = string2.equals(other.string2) && getFinalString().equals(other.getFinalString());
		}
		
		return ret;
	}
	
	/**
	 *  Use instance getter to maybe find if class was loaded twice!?
	 *  Not sure why we would need to compare the static string anyways in equals()...
	 */
	String	getFinalString()
	{
		return finalStaticString;
	}
}
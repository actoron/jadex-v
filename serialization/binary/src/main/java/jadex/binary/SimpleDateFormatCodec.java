package jadex.binary;

import java.text.SimpleDateFormat;
import java.util.List;

import jadex.common.SReflect;
import jadex.common.transformation.BeanIntrospectorFactory;
import jadex.common.transformation.IStringConverter;
import jadex.common.transformation.traverser.IBeanIntrospector;
import jadex.common.transformation.traverser.ITraverseProcessor;
import jadex.common.transformation.traverser.Traverser;
import jadex.common.transformation.traverser.Traverser.MODE;

/**
 *  Codec for encoding and decoding java.text.SimpleDateFormat objects.
 *
 */
public class SimpleDateFormatCodec extends BeanCodec
{
	/** Bean introspector for inspecting beans. */
	protected IBeanIntrospector intro = BeanIntrospectorFactory.get().getBeanIntrospector(10);

	/**
	 *  Tests if the decoder can decode the class.
	 *  @param clazz The class.
	 *  @return True, if the decoder can decode this class.
	 */
	public boolean isApplicable(Class<?> clazz)
	{
		return SReflect.isSupertype(SimpleDateFormat.class, clazz);
	}
	
	/**
	 *  Add pattern property with applyPattern method.
	 */
	@Override
	public Object decode(Class<?> clazz, IDecodingContext context)
	{
		SimpleDateFormat	ret	= (SimpleDateFormat)super.decode(clazz, context);
		ret.applyPattern(context.readString());
		return ret;
	}
	
	/**
	 *  Add pattern property from toPattern method.
	 */
	@Override
	public Object encode(Object object, Class<?> clazz, List<ITraverseProcessor> preprocessors,
			List<ITraverseProcessor> processors, IStringConverter converter, MODE mode, Traverser traverser, ClassLoader targetcl,
			IEncodingContext ec)
	{
		super.encode(object, clazz, preprocessors, processors, converter, mode, traverser, targetcl, ec);
		ec.writeString(((SimpleDateFormat)object).toPattern());
		return object;
	}
}

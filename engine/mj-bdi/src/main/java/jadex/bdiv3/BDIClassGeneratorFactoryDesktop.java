package jadex.bdiv3;

/**
 * 
 */
public class BDIClassGeneratorFactoryDesktop extends BDIClassGeneratorFactory
{
	/**
	 * 
	 */
	public BDIClassReader createBDIClassReader(BDIModelLoader loader)
	{
		return new BDIClassReader(loader);
	}

	/**
	 * 
	 */
	public IBDIClassGenerator createBDIClassGenerator()
	{
		return new ASMBDIClassGenerator();
	}
}

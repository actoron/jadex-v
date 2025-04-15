package jadex.bdi;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 *  Helper for bdi tests.
 */
public class TestHelper
{
	/** Global test timeout. */
	public static final long	TIMEOUT	= 10000;

	/**
	 *  Temporarily top system out/err prints
	 */
	public static void runWithoutOutErr(Runnable runnable)
	{
		PrintStream	out	= System.out;
		PrintStream	err	= System.err;
		System.setOut(new PrintStream(new ByteArrayOutputStream()));
		System.setErr(new PrintStream(new ByteArrayOutputStream()));
		try
		{
			runnable.run();
		}
		finally
		{
			System.setOut(out);
			System.setErr(err);
		}
	}
}

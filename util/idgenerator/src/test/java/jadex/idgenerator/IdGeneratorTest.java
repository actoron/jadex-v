package jadex.idgenerator;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class IdGeneratorTest
{
	/**
	 *  Check that generated ids are unique.
	 */
	@Test
	void testIdGenerator()
	{
		IdGenerator	idgen	= new IdGenerator(false);
		Set<String>	ids	= new HashSet<>();
		for(int i=0; i<1234567; i++)
		{
			String	id	= idgen.idStringFromNumber(i);
			assertFalse(ids.contains(id), "Id "+id+" #"+ids.size());
			ids.add(id);
		}
	}
}

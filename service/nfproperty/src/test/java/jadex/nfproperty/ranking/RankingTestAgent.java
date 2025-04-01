package jadex.nfproperty.ranking;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import jadex.common.Tuple2;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;

public class RankingTestAgent 
{
	@Test
	public void waitTimes()
	{
		int n=20;
		
		for(int i=0; i<n; i++)
			IComponentManager.get().create(new ServiceSearchAgent(false)).get();
		
		IComponentHandle exta = IComponentManager.get().create(new RankingTestAgent()).get();
		
		Collection<Tuple2<ICoreDependentService, Double>> sers = ServiceSearchAgent.searchAndRank(exta).get();
	
		System.out.println("services: "+sers.size());

		assertTrue(sers.size()>0, "Must find services");
		assertTrue(areDoublesDescending(sers), "Sorted");
	}
	
	public static boolean areDoublesDescending(Collection<Tuple2<ICoreDependentService, Double>> sers) 
	{
        List<Double> doubles = sers.stream().map(Tuple2::getSecondEntity).collect(Collectors.toList());

        for (int i = 1; i < doubles.size(); i++) 
        {
            if (doubles.get(i - 1) <= doubles.get(i)) 
            {
                return false;
            }
        }
        
        return true;
    }
}

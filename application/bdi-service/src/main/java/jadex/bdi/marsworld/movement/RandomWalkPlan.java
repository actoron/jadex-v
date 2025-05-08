package jadex.bdi.marsworld.movement;

import jadex.bdi.IPlan;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.marsworld.movement.MovementCapability.Move;
import jadex.bdi.marsworld.movement.MovementCapability.WalkAround;
import jadex.injection.annotation.Inject;
import jadex.math.IVector2;
import jadex.math.Vector2Int;

/**
 *  Wander around randomly.
 */
@Plan
public class RandomWalkPlan
{
	//-------- attributes --------

	@Inject
	protected MovementCapability capa;
	
	@Inject
	protected IPlan rplan;
	
	@Inject
	protected WalkAround goal;
	
	//-------- constructors --------

	/**
	 *  Create a new plan.
	 */
	public RandomWalkPlan()
	{
		//getLogger().info("Created: "+this+" for goal "+getRootGoal());
	}

	//-------- methods --------

	/**
	 *  The plan body.
	 */
	@PlanBody
	public void body()
	{
//		System.out.println("RandomWalk: "+capa.getCapability().getAgent().getComponentIdentifier());
		IVector2 dest;
		/*if(capa.getEnvironment() instanceof Space3D)
		{
			dest	= ((Space3D)capa.getEnvironment()).getRandomPosition(Vector3Int.ZERO);			
		}
		else
		{*/
			dest	= capa.getEnvironment().getRandomPosition(Vector2Int.ZERO).get();
		//}
		Move moveto = capa.new Move(dest);
		rplan.dispatchSubgoal(moveto).get();
//		System.out.println("Reached point: "+dest);
	}
}

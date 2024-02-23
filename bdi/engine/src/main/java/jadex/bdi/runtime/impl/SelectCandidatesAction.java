package jadex.bdi.runtime.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import jadex.bdi.annotation.Plan;
import jadex.bdi.model.MCapability;
import jadex.bdi.model.MElement;
import jadex.bdi.model.MGoal;
import jadex.bdi.runtime.IParameter;
import jadex.bdi.runtime.IParameterSet;
import jadex.bdi.runtime.impl.APL.MGoalInfo;
import jadex.bdi.runtime.impl.APL.MPlanInfo;
import jadex.bdi.runtime.impl.RPlan.ResumeCommandArgs;
import jadex.bdi.runtime.impl.RPlan.Waitqueue;
import jadex.future.IResultListener;

/**
 *  Action for selecting a candidate from the APL.
 */
public class SelectCandidatesAction implements Runnable
{
	/** The element. */
	protected RProcessableElement element;
	
	/**
	 *  Create a new action.
	 */
	public SelectCandidatesAction(RProcessableElement element)
	{
		this.element = element;
	}
	
	/**
	 *  Test if the action is valid.
	 *  @return True, if action is valid.
	 */
	public boolean isValid()
	{
		boolean ret = true;
		
		if(element instanceof RGoal)
		{
			RGoal rgoal = (RGoal)element;
			ret = RGoal.GoalLifecycleState.ACTIVE.equals(rgoal.getLifecycleState())
				&& RGoal.GoalProcessingState.INPROCESS.equals(rgoal.getProcessingState());
		}
			
//		if(!ret)
//			System.out.println("not valid: "+this+" "+element);
		
		return ret;
	}
	
	/**
	 *  Execute the command.
	 *  @param args The argument(s) for the call.
	 *  @return The result of the command.
	 */
	public void	run()
	{
		if(!isValid())
		{
			return;
		}
//		if(element.toString().indexOf("cnp_make_proposal")!=-1)
//			System.out.println("select candidates: "+element);
		
//		BDIAgentInterpreter ip = (BDIAgentInterpreter)((BDIAgent)ia).getInterpreter();
		MCapability	mcapa = (MCapability)IInternalBDIAgentFeature.get().getCapability().getModelElement();

		List<ICandidateInfo> cands = element.getApplicablePlanList().selectCandidates(mcapa);
		
		if(cands!=null && !cands.isEmpty())
		{
//			if(element.toString().indexOf("go_home")!=-1)
//				System.out.println("select candidates: "+element+", "+cands);
			element.setState(RProcessableElement.State.CANDIDATESSELECTED);
			for(final ICandidateInfo ca: cands)
			{
				Object cand = ca.getRawCandidate();
				if(cand instanceof MPlanInfo)
				{
//					MPlanInfo mplaninfo = (MPlanInfo)cand;
					try
					{
						RPlan rplan = (RPlan)ca.getPlan();
//						RPlan rplan = RPlan.createRPlan(mplaninfo.getMPlan(), cand, element, ia, mplaninfo.getBinding(), null);
						rplan.executePlan();
					}
					catch(final Exception e)
					{
//						if(element.toString().indexOf("go_home")!=-1)
//							System.out.println("select candidates fail: "+e);
						StringWriter	sw	= new StringWriter();
						e.printStackTrace(new PrintWriter(sw));
						System.err.println("Plan creation of '"+cand+"' threw exception: "+sw);
						
						element.planFinished(new IInternalPlan()
						{
							public MElement getModelElement()
							{
								return null;
							}
							
							public boolean hasParameterSet(String name)
							{
								return false;
							}
							
							public boolean hasParameter(String name)
							{
								return false;
							}
							
							public String getType()
							{
								return null;
							}
							
							public IParameter[] getParameters()
							{
								return null;
							}
							
							public IParameterSet[] getParameterSets()
							{
								return null;
							}
							
							public IParameterSet getParameterSet(String name)
							{
								return null;
							}
							
							public IParameter getParameter(String name)
							{
								return null;
							}
							
							public boolean isPassed()
							{
								return false;
							}
							
							public boolean isFailed()
							{
								return true;
							}
							
							public boolean isAborted()
							{
								return true;
							}
							
							public Exception getException()
							{
								return e;
							}
							
							public ICandidateInfo getCandidate()
							{
								return ca;
							}
							
							public long getCount() 
							{
								return -1;
							}
							
							public String getId() 
							{
								return null;
							}
						});
					}
				}
				// direct subgoal for goal
				else if(cand instanceof MGoalInfo)
				{
					MGoalInfo mgoalinfo = (MGoalInfo)cand;
					
					final RProcessableElement pae = (RProcessableElement)element;
					final RGoal pagoal = pae instanceof RGoal? (RGoal)pae: null;
					final MGoal mgoal = mgoalinfo.getMGoal();
//					
//					final Object pgoal = mgoal.createPojoInstance(ia, pagoal);
//					final RGoal rgoal = new RGoal(ia, mgoal, pgoal, pagoal, mgoalinfo.getBinding(), null);
//					
					final RGoal rgoal = (RGoal)ca.getPlan();
					
					// Add candidates to meta goal
					if(mgoal.isMetagoal())
					{
						APL apl = element.getApplicablePlanList();
						List<ICandidateInfo> allcands = apl.getCandidates();
						if(allcands.size()==1)
						{
							element.planFinished(null);
							return;
						}
						
						for(ICandidateInfo c: allcands)
						{
							// avoid recursion by adding metagoal as candidate again
							if(!c.equals(cand) && !c.getModelElement().equals(mgoal))// && c instanceof MPlanInfo)
							{
//								MPlanInfo pi = (MPlanInfo)c;
//								final RPlan rplan = RPlan.createRPlan(pi.getMPlan(), c, element, ia, pi.getBinding(), null);
//								final RPlan rplan = (RPlan)ca.getPlan();
								// find by type and direction?!
								rgoal.getParameterSet("applicables").addValue(c);
//								rgoal.getParameterSet("applicables").addValue(new ICandidateInfo()
//								{
//									public IPlan getPlan()
//									{
//										return rplan;
//									}
//									
//									public IElement getElement()
//									{
//										return element;
//									}
//								});
							}
						}
					}
					
					rgoal.addListener(new IResultListener<Void>()
					{
						public void resultAvailable(Void result)
						{
							Object res = RGoal.getGoalResult(rgoal, IInternalBDIAgentFeature.get().getClassLoader());
							
							if(mgoal.isMetagoal())
							{
								// Execute selected plans if was metagoal
								// APL is automatically kept uptodate
								for(ICandidateInfo ci: (ICandidateInfo[])res)
								{
									((RPlan)ci.getPlan()).executePlan();
								}
							}
							else
							{
								pae.planFinished(rgoal);
								
								// Set goal result on parent goal
								if(pagoal!=null)
									pagoal.setGoalResult(res, IInternalBDIAgentFeature.get().getClassLoader(), null, null, rgoal);
							}
						}
						
						public void exceptionOccurred(Exception exception)
						{
							// todo: what if meta-level reasoning fails?!
							pae.planFinished(rgoal);
						}
					});
					
					RGoal.adoptGoal(rgoal);
				}
				else if(cand.getClass().isAnnotationPresent(Plan.class))
				{
					RPlan rplan = (RPlan)ca.getPlan();
//					MPlan mplan = mcapa.getPlan(cand.getClass().getName());
//					RPlan rplan = RPlan.createRPlan(mplan, cand, element, ia, null, null);
					rplan.executePlan();
				}
				else if(cand instanceof RPlan)
				{
					// dispatch to running plan
					final RPlan rplan = (RPlan)cand;
					rplan.setDispatchedElement(element);
					if(rplan.getResumeCommand()==null)
					{
						// case meta-level reasoning, plan has been created but is new
//						System.out.println("rplan no resume command: "+rplan);
						rplan.executePlan();
					}
					else
					{
						// normal case when plan was waiting
//						System.out.println("rplan resume command: "+rplan);
						rplan.getResumeCommand().execute(new ResumeCommandArgs(null, Boolean.FALSE, null));
					}
				}
				else if(cand instanceof Waitqueue)
				{
					// dispatch to waitqueue
					((Waitqueue)cand).addElement(element);
				}
//				// Unwrap candidate info coming from meta-level reasoning
//				else if(cand instanceof ICandidateInfo)
//				{
//					ICandidateInfo ci = (ICandidateInfo)cand;
//					RPlan.executePlan((RPlan)ci.getPlan(), ia);
//					ret.setResult(null);
//				}
			}
		}
		else
		{
			// todo: throw goal failed exception for goal listeners
//			System.out.println("No applicable plan found for: "+element.getId());
			element.planFinished(null);
		}
	}
	
	public String toString()
	{
		return "SelectCandidatesAction("+element+")";
	}
}

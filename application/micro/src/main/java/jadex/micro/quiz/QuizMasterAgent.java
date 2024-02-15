package jadex.micro.quiz;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.ITerminationCommand;
import jadex.future.SubscriptionIntermediateFuture;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentArgument;
import jadex.micro.quiz.model.NewQuizEvent;
import jadex.micro.quiz.model.Question;
import jadex.micro.quiz.model.QuestionEvent;
import jadex.micro.quiz.model.Quiz;
import jadex.micro.quiz.model.QuizEvent;
import jadex.micro.quiz.model.QuizResults;
import jadex.micro.quiz.model.ResultEvent;
import jadex.model.annotation.OnStart;
import jadex.providedservice.impl.service.ServiceCall;

/**
 *  The quiz master agent.
 */
@Agent
public class QuizMasterAgent implements IQuizService
{
	/** The agent. */
	@Agent
	protected IComponent agent;
	
	/** The current participants. */
	protected Map<ComponentIdentifier, SubscriptionIntermediateFuture<QuizEvent>> subscriptions = new HashMap<>();
	
	/** The quiz. */
	protected Quiz quiz;
	
	/** The results. */
	protected Map<ComponentIdentifier, QuizResults> results = new HashMap<ComponentIdentifier, QuizResults>();
	
	/** The delay between questions. */
	protected long delay;
	
	/** The current question no. */
	protected int questioncnt = 0;
	
	public QuizMasterAgent()
	{
		this(15000);
	}
	
	public QuizMasterAgent(long delay)
	{
		this.delay = delay;
	}
	
	@OnStart
	public void start()
	{
		this.quiz = createQuiz();
		//System.out.println("master working");
		
		// wait for participants 
		agent.getFeature(IExecutionFeature.class).waitForDelay(5000).get();
		
		while(questioncnt<quiz.getNumberOfQuestions())
		{
			Question question = quiz.getQuestion(questioncnt);
			publishQuestion(question, questioncnt);
			questioncnt++;
			agent.getFeature(IExecutionFeature.class).waitForDelay(delay).get();
		}
		
		publishResults();
		
		System.out.println("quiz ended: "+quiz);
	}

	/**
	 *  Create a quiz.
	 */
	protected Quiz createQuiz()
	{
		Quiz quiz = new Quiz("Software Engineering");
		quiz.addQuestion(new Question("Which software pattern can be used to exchange algorithms?", Arrays.asList(new String[]{"Visitor", "Observer", "Memento", "Strategy"}), 3));
		quiz.addQuestion(new Question("What does the L in SOLID stands for?", Arrays.asList(new String[]{"Lavrow", "Liskov", "Low", "Lightweight"}), 1));
		quiz.addQuestion(new Question("In the SOA triangle there are the entities service provider, user and ...?", Arrays.asList(new String[]{"ESB", "Bus", "Registry", "Mesh"}), 2));
		quiz.addQuestion(new Question("An agent often is reactive and ...?", Arrays.asList(new String[]{"emotional", "fast", "proactive", "scalable"}), 2));
		return quiz;
	}
	
	/**
	 *  Method to participate in the quiz.
	 *  @return The subscription for receiving quiz events.
	 */
	public ISubscriptionIntermediateFuture<QuizEvent> participate()
	{
		SubscriptionIntermediateFuture<QuizEvent> ret = new SubscriptionIntermediateFuture<QuizEvent>();
		ComponentIdentifier caller = ServiceCall.getCurrentInvocation().getCaller();
		subscriptions.put(caller, ret);
		
		QuizResults res = results.get(caller);
		if(res==null)
		{
			res = new QuizResults();
			results.put(caller, res);
		}
		
		ret.addIntermediateResult(new NewQuizEvent(quiz.getName(), quiz.getNumberOfQuestions(), delay, quiz.getStart()));
		
		ret.setTerminationCommand(new ITerminationCommand()
		{
			public void terminated(Exception reason)
			{
				subscriptions.remove(caller);
			}
			
			public boolean checkTermination(Exception reason)
			{
				return true;
			}
		});
		return ret;
	}

	/**
	 *  Send an answer.
	 *  @param answer The answer.
	 */
	public IFuture<Void> sendAnswer(int answer, int questioncnt)
	{
		ComponentIdentifier caller = ServiceCall.getCurrentInvocation().getCaller();
		
		if(questioncnt!=this.questioncnt && questioncnt+1!=this.questioncnt)
			return new Future<Void>(new RuntimeException("Answer only to current questions allowed: "+questioncnt+" "+this.questioncnt));
		
		QuizResults res = results.get(caller);
		
		System.out.println("answer: "+answer+" "+questioncnt+" "+(quiz.getQuestion(questioncnt).getSolution()==answer));
		//System.out.println("antwort: "+(quiz.getQuestion(questioncnt).getSolution()==answer)+" "+answer);
		res.addResult(questioncnt, quiz.getQuestion(questioncnt).getSolution()==answer);
		//System.out.println("res: "+res.size()+" "+quiz.getNumberOfQuestions());
		
		/*if(res.size()==quiz.getNumberOfQuestions())
		{
			SubscriptionIntermediateFuture<QuizEvent> s = subscriptions.get(caller);
			if(s!=null)
			{
				s.addIntermediateResult(new ResultEvent(res));
				s.setFinished();
				subscriptions.remove(caller);
			}
			else
			{
				System.out.println("not found: "+caller+" "+results+" "+subscriptions);
			}
		}*/
		
		return IFuture.DONE;
	}
	
	/**
	 *  Publish a question to all subscribers.
	 *  @param question The question.
	 */
	public void publishQuestion(Question question, int questioncnt)
	{
		publishEvent(new QuestionEvent(question, questioncnt));
	}
	
	/**
	 *  Publish the results.
	 */
	public void publishResults()
	{
		for(ComponentIdentifier cid: results.keySet())
		{
			QuizResults res = results.get(cid);
			SubscriptionIntermediateFuture<QuizEvent> s = subscriptions.get(cid);
			s.addIntermediateResult(new ResultEvent(res));
			s.setFinished();
			subscriptions.remove(cid);
		}
	}
	
	/**
	 *  Publish a quiz event.
	 *  @param event The event.
	 */
	public void publishEvent(QuizEvent event)
	{
		for(SubscriptionIntermediateFuture<QuizEvent> subscription: subscriptions.values())
		{
			subscription.addIntermediateResult(event);
		}
	}
	
}

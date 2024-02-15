package jadex.micro.quiz;

import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.micro.quiz.model.QuizEvent;
import jadex.providedservice.annotation.Service;

/**
 *  Interface for a quiz service.
 */
@Service
public interface IQuizService
{
	/**
	 *  Method to participate in the quiz.
	 *  @return The subscription for receiving quiz events.
	 */
	public ISubscriptionIntermediateFuture<QuizEvent> participate();

	/**
	 *  Send an answer.
	 *  @param answer The answer.
	 */
	public IFuture<Void> sendAnswer(int answer, int questioncnt);
}

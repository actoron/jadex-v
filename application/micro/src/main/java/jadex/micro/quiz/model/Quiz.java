package jadex.micro.quiz.model;

import java.util.ArrayList;
import java.util.List;

/**
 *  The quiz data class.
 */
public class Quiz
{
	/** The questions. */
	protected List<Question> questions;
	
	/** The quiz name. */
	protected String name;
	
	/** The start time. */
	protected long start;
	
	/**
	 *  Create a new quiz.
	 */
	public Quiz()
	{
		this(null);
	}
	
	/**
	 *  Create a new quiz.
	 */
	public Quiz(String name)
	{
		this(name, null);
	}
	
	/**
	 *  Create a new quiz.
	 */
	public Quiz(String name, List<Question> questions)
	{
		this.name = name;
		this.start = System.currentTimeMillis();
		this.questions = questions!=null? questions: new ArrayList<Question>();;
	}

	/**
	 * @return the questions
	 */
	public List<Question> getQuestions()
	{
		return questions;
	}

	/**
	 * @param questions the questions to set
	 */
	public void setQuestions(List<Question> questions)
	{
		this.questions = questions;
	}
	
	/**
	 *  Get a question per index.
	 */
	public Question getQuestion(int no)
	{
		return questions.get(no);
	}
	
	/**
	 *  Get the number of questions.
	 */
	public int getNumberOfQuestions()
	{
		return questions.size();
	}
	
	/**
	 *  Add a question.
	 */
	public void addQuestion(Question q)
	{
		this.questions.add(q);
	}

	/**
	 * @return the name
	 */
	public String getName() 
	{
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) 
	{
		this.name = name;
	}

	/**
	 * @return the start
	 */
	public long getStart() 
	{
		return start;
	}

	/**
	 * @param start the start to set
	 */
	public void setStart(long start) 
	{
		this.start = start;
	}
	
	
}

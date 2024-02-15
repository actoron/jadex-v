package jadex.micro.quiz.model;

/**
 *  Event 
 */
public class NewQuizEvent extends QuizEvent
{
	/** The quiz name. */
	protected String name;
	
	/** The number of questions. */
	protected int size;

	/** The delay bewteen questions. */
	protected long delay;
	
	/** The start time. */
	protected long start;
	
	/**
	 *  Create a new question event.
	 */
	public NewQuizEvent()
	{
	}
	
	/**
	 *  Create a new question event.
	 */
	public NewQuizEvent(String name, int size, long delay, long start)
	{
		this.name = name;
		this.size = size;
		this.delay = delay;
		this.start = start;
	}

	/**
	 *  Get the name.
	 *  @return the name
	 */
	public String getName() 
	{
		return name;
	}

	/**
	 *  Set the name.
	 *  @param name the name to set
	 */
	public void setName(String name) 
	{
		this.name = name;
	}

	/**
	 *  Get the size.
	 *  @return the size
	 */
	public int getSize() 
	{
		return size;
	}

	/**
	 *  Set the size.
	 *  @param size the size to set
	 */
	public void setSize(int size) 
	{
		this.size = size;
	}

	/**
	 *  Get the delay.
	 *  @return the delay
	 */
	public long getDelay() 
	{
		return delay;
	}

	/**
	 *  Set the delay.
	 *  @param delay the delay to set
	 */
	public void setDelay(long delay) 
	{
		this.delay = delay;
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

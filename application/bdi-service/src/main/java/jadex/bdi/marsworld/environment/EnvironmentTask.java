package jadex.bdi.marsworld.environment;

import java.util.Objects;
import java.util.function.Function;

import jadex.future.TerminableFuture;

public class EnvironmentTask 
{
	protected String id;
	
	protected TerminableFuture<Void> future;
	
	protected Function<Long, Boolean> task;
	
	protected Environment env;
	
	public EnvironmentTask(Environment env, TerminableFuture<Void> future, Function<Long, Boolean> task) 
	{
		this.env = env;
		this.future = future;
		this.task = task;
		
		if(future!=null)
		{
			future.setTerminationCommand(ex ->
			{
				env.removeTask(this);
			});
		}
	}
	
	public String getId() 
	{
		return id;
	}

	public void setId(String id) 
	{
		this.id = id;
	}

	public TerminableFuture<Void> getFuture() 
	{
		return future;
	}

	public Function<Long, Boolean> getTask() 
	{
		return task;
	}

	@Override
	public int hashCode() 
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + getEnclosingInstance().hashCode();
		result = prime * result + Objects.hash(id);
		return result;
	}

	@Override
	public boolean equals(Object obj) 
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EnvironmentTask other = (EnvironmentTask)obj;
		if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
			return false;
		return Objects.equals(id, other.id);
	}

	private Environment getEnclosingInstance() 
	{
		return env;
	}
}
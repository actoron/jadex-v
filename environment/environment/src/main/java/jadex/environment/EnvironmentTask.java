package jadex.environment;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import jadex.future.TerminableFuture;

public class EnvironmentTask 
{
	protected String id;
	
	protected SpaceObject owner;
	
	protected String type;
	
	protected TerminableFuture<Void> future;
	
	protected Function<TaskData, TaskData> task;
	
	protected TaskData taskdata;
	
	protected Environment env;
	
	protected Map<String, Object> infos;
	
	
	public record TaskData(boolean finsihed, long delta, Map<String, Object> data, Set<SpaceObject> changed) 
	{
		public static TaskData TRUE = new TaskData(true); 
		public static TaskData FALSE = new TaskData(false); 
		
		public TaskData(boolean finished)
		{
			this(finished, 0, null, null);
		}
		
		public TaskData(boolean finished, Set<SpaceObject> changed)
		{
			this(finished, 0, null, changed);
		}
		
		public TaskData(long delta)
		{
			this(false, delta, null, null);
		}
		
		public TaskData(boolean finished, Map<String, Object> data)
		{
			this(finished, 0, data, null);
		}
		
		public TaskData(boolean finished, Map<String, Object> data, Set<SpaceObject> changed)
		{
			this(finished, 0, data, changed);
		}
		
		public TaskData(long delta, Map<String, Object> data)
		{
			this(false, delta, data, null);
		}
	}
	
	public EnvironmentTask(SpaceObject owner, String type, Environment env, TerminableFuture<Void> future, Function<TaskData, TaskData> task) 
	{
		this.owner = owner;
		this.type = type;
		this.env = env;
		this.future = future;
		this.task = task;
		
		if(future!=null)
		{
			future.setTerminationCommand(ex ->
			{
				//System.out.println("env removing task: "+this+" "+ex);
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
	
	public SpaceObject getOwner() 
	{
		return owner;
	}
	
	public String getType() 
	{
		return type;
	}

	public void setType(String type) 
	{
		this.type = type;
	}

	public TerminableFuture<Void> getFuture() 
	{
		return future;
	}

	public Function<TaskData, TaskData> getTask() 
	{
		return task;
	}
	
	public TaskData getTaskData() 
	{
		return taskdata;
	}

	public void setTaskData(TaskData taskdata) 
	{
		this.taskdata = taskdata;
	}
	
	public Map<String, Object> getInfos() 
	{
		return infos;
	}

	public void setInfos(Map<String, Object> infos) 
	{
		this.infos = infos;
	}
	
	public void addInfo(String name, Object val)
	{
		if(infos==null)
			infos = new HashMap<String, Object>();
		infos.put(name, val);
	}
	
	public Object getInfo(String name)
	{
		return infos==null? null: infos.get(name);
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
	
	@Override
	public String toString() 
	{
		return "EnvironmentTask [id=" + id + "]";
	}

	private Environment getEnclosingInstance() 
	{
		return env;
	}
}
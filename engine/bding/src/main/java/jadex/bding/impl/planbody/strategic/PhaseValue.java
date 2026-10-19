package jadex.bding.impl.planbody.strategic;


public class PhaseValue<T>
{
    public enum PlanPhase
    {
        PHASE1,
        PHASE2,
        PHASE3
    }

    protected Object[] values = new Object[3];

    public T get()
    {
        T ret = null;
        for(int i=2; i>=0 && ret==null; i--)
        {
            ret = (T)values[i];
        }
        return ret;
    }

    public T get(PlanPhase phase)
    {
        return (T)values[phase.ordinal()];
    }

    public void set(PlanPhase phase, T value)
    {
        values[phase.ordinal()] = value;
    }

    public Object[] getValues() 
    {
        return values;
    }

    public void setValues(Object[] values)
    {
        this.values = values;
    }

}
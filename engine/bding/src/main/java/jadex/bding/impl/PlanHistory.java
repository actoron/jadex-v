package jadex.bding.impl;

import java.util.ArrayList;
import java.util.List;

public class PlanHistory 
{
    public static class PlanHistoryEntry
    {
        protected RPlan plan;

        protected ResultState result;

        public PlanHistoryEntry(RPlan plan)
        {
            this.plan = plan;
        }

        public RPlan getPlan()
        {
            return plan;
        }
    }

    protected List<PlanHistoryEntry> entries = new ArrayList<>();

    public void addEntry(PlanHistoryEntry entry)
    {
        entries.add(entry);
    }

    public List<PlanHistoryEntry> getEntries()
    {
        return entries;
    }

}

package jadex.bding.impl;

import java.util.ArrayList;
import java.util.List;

import jadex.bding.Intention;

public class IntentionHistory 
{
    public static class IntentionHistoryEntry
    {
        protected RIntention intention;

        //protected ResultState result;

        public IntentionHistoryEntry(RIntention intention)
        {
            this.intention = intention;
        }

        public RIntention getIntention()
        {
            return intention;
        }
    }

    protected List<IntentionHistoryEntry> entries = new ArrayList<>();

    public void addEntry(IntentionHistoryEntry entry)
    {
        entries.add(entry);
    }

    public List<IntentionHistoryEntry> getEntries()
    {
        return entries;
    }

    public boolean isKnown(Intention intention)
    {
        boolean ret = false;
        for(IntentionHistoryEntry entry: entries)
        {
            if(entry.getIntention().getIntention().getDescription().equals(intention.getDescription()))
            {
                System.out.println("Found similar intention: "+intention+" "+entry.getIntention().getIntention());
                ret = true;
                break;
            }
        }
        return ret;
    }

}

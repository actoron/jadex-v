package jadex.micro.gobble;

import java.util.ArrayList;
import java.util.List;

public class Inventory 
{
    private int size;
    private List<Integer> inv;

    public Inventory(int size) 
    {
        this.size = size;
        this.inv = new ArrayList<>();
        for (int i = 0; i < 3; i++) 
            inv.add(size);
    }

    public int getContent(int i) 
    {
        return inv.get(i);
    }
    
    public int getSize() 
    {
		return size;
	}

	public int getInventoryCount() 
    {
        int sum = 0;
        for(int val : inv) 
            sum += val;
        return sum;
    }

    public boolean hasGhost(int size) 
    {
        return inv.get(size - 1) > 0;
    }

    public void removeGhost(int size) 
    {
        if (hasGhost(size)) 
        {
            inv.set(size - 1, inv.get(size - 1) - 1);
        } 
        else 
        {
            System.out.println("No ghost of that size in inventory");
        }
    }

    public void addGhost(int size) 
    {
        inv.set(size - 1, inv.get(size - 1) + 1);
    }

    public int getMinGhostSize(int start) 
    {
        int ret = -1;
        start = (start != 0) ? start - 1 : 0;
        for(int i = start; i < inv.size(); i++) 
        {
            if(inv.get(i) > 0) 
            {
                ret = i + 1;
                break;
            }
        }
        return ret;
    }
}
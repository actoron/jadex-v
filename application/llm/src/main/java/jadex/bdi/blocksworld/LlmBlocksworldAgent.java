package jadex.bdi.blocksworld;

import java.awt.Color;

import jadex.future.Future;
import jadex.future.IFuture;

/**
 *  Interact with a blocksworld via natural language prompts.
 *  Text-based world state representation, but can include an image of the world state
 *  in the prompt if desired and supported by the model.
 */
public class LlmBlocksworldAgent	extends LlmBlocksworldBaseAgent	implements IBlocksworldTextService
{
	@Override
	public IFuture<String> getWorldState()
	{
		StringBuilder sb = new StringBuilder();
		for(Block block : blocks)
		{
			sb
				.append("'")
				.append(block)
				.append("'")
				.append(
					block.getLower()==table ? " is on the "
					: block.getLower()==bucket ? " is in the "
					: " is on ")
				.append("'")
				.append(block.getLower())
				.append("'")
				.append("\n");
		}
		if(table.getAllBlocks().length==0)
		{
			sb.append("The '"+table+"' is empty.\n");
		}
		if(bucket.getAllBlocks().length==0)
		{
			sb.append("The '"+bucket+"' is empty.\n");
		}
		
		for(Block block : blocks)
		{
			sb
				.append("'")
				.append(block)
				.append("'")
				.append(" has color ")
				.append(approximateColorName(block.getColor()))
				.append("\n");
		}
		return new Future<>(sb.toString());
	}
	
	/**
	 *  Approximate a color name from a Color object.
	 */
	protected static String	approximateColorName(Color color)
	{
	    float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
	    float hue = hsb[0] * 360f;
	    float sat = hsb[1];
	    float bri = hsb[2];

	    if(bri < 0.12f) return "black";
	    if(bri < 0.22f) return "dark gray";
	    if(sat < 0.10f)
	    {
	        if(bri > 0.90f) return "white";
	        if(bri > 0.70f) return "light gray";
	        if(bri > 0.35f) return "gray";
	        return "dark gray";
	    }

	    String base;
	    if(hue < 15f || hue >= 345f) base = "red";
	    else if(hue < 45f) base = "orange";
	    else if(hue < 70f) base = "yellow";
	    else if(hue < 160f) base = "green";
	    else if(hue < 200f) base = "cyan";
	    else if(hue < 255f) base = "blue";
	    else if(hue < 290f) base = "purple";
	    else if(hue < 345f) base = "magenta";
	    else base = "red";

	    String tone = bri < 0.35f ? "dark " : bri > 0.80f ? "light " : "";
	    String vivid = sat > 0.85f && bri > 0.45f ? "vivid " : sat < 0.35f ? "muted " : "";

	    return (tone + vivid + base).trim();
	}
	
	/**
	 *  Run the agent with a GUI.
	 */
	public static void main(String[] args)
	{
		runGui(new LlmBlocksworldAgent());
	}
}

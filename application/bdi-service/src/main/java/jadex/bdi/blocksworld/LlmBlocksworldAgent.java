package jadex.bdi.blocksworld;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.INoCopyStep;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.micro.llmcall2.LlmHelper;
import jadex.micro.llmcall2.LlmResultAgent;

public class LlmBlocksworldAgent	extends BlocksworldAgent	implements IBlocksworldService
{
	@Override
	public IFuture<String> getWorldState()
	{
		StringBuilder sb = new StringBuilder();
		for(Block block : blocks)
		{
			sb.append(block)
				.append(
					block.getLower()==table ? " is on the "
					: block.getLower()==bucket ? " is in the "
					: " is on ")
				.append(block.getLower())
				.append("\n");
		}
		for(Block block : blocks)
		{
			sb.append(block)
				.append(" has color ")
				.append(approximateColorName(block.getColor()))
				.append("\n");
		}
		return new Future<>(sb.toString());
	}
	
	@Override
	public IFuture<Void> move(String block1, String block2)
	{
		Block b1 = getBlock(block1);
		Block b2 = getBlock(block2);
		if(b1.getUpper()!=null)
			throw new IllegalStateException("Block '"+block1+"' is not clear.");
		if(b2.getUpper()!=null && b2!=table && b2!=bucket)
			throw new IllegalStateException("Block '"+block2+"' is not clear.");
		if(b1.getLower()==null)	// table and bucket cannot be moved
			throw new IllegalArgumentException("'"+block1+"' can not be moved.");
		if(b2.getLower()==bucket)	// Cannot move on block in bucket
			throw new IllegalArgumentException("'"+block2+"' is in the "+bucket);
		if(b1==b2)
			throw new IllegalArgumentException("Cannot move block '"+block1+"' on itself.");
		b1.getLower().removeBlock(b1);
		b1.setLower(b2);
		b2.addBlock(b1);
		
		return IFuture.DONE;
	}
	
	protected Block	getBlock(String name)
	{
		if(name.equals(table.toString()))
			return table;
		if(name.equals(bucket.toString()))
			return bucket;
		for(Block block : blocks)
		{
			if(block.toString().equals(name))
				return block;
		}
		throw new IllegalArgumentException("Block '"+name+"' not found.");
	}
	
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

	public static void main(String[] args)
	{
		IComponentHandle	bwagent	= IComponentManager.get().create(new LlmBlocksworldAgent()).get();
		BlocksworldGui	gui	= bwagent.scheduleAsyncStep(
			(INoCopyStep<IFuture<BlocksworldGui>>) comp -> ((BlocksworldAgent)comp.getPojo()).gui).get();
		
		// Create simple gui to send a prompt
		SwingUtilities.invokeLater(() ->
		{
			JTextArea	prompt	= new JTextArea("Move the red block on the green block.");
			JButton		send	= new JButton("Send");
			
			JPanel	panel	= new JPanel(new BorderLayout());
			panel.add(prompt, BorderLayout.CENTER);
			panel.add(send, BorderLayout.SOUTH);
			panel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), "LLM Requests"));
			
			send.addActionListener(e ->
			{
				prompt.setEnabled(false);
				send.setEnabled(false);
				IComponentHandle llmagent = IComponentManager.get().create(
					new LlmResultAgent(LlmHelper.createChatModel(), prompt.getText())).get();
				LlmResultAgent.printResults(llmagent);
				llmagent.waitForTermination()
					.then(v -> {prompt.setEnabled(true); send.setEnabled(true);})
					.catchEx(v -> {prompt.setEnabled(true); send.setEnabled(true);})
					.printOnEx();
			});
			
			Container	worlds	= (Container)gui.getContentPane().getComponent(0);
			worlds.remove(worlds.getComponent(0));
			worlds.add(panel, 0);
			worlds.validate();
			worlds.repaint();
		});
		
		bwagent.waitForTermination().get();
	}
}

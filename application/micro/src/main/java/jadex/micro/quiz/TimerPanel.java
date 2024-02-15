package jadex.micro.quiz;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class TimerPanel extends JPanel
{
    protected JLabel timerLabel;
    protected Timer stimer;
    protected int duration;
    //protected int yellow;
    //protected int red;
    protected Runnable due;
    protected Consumer<TimerPanel> ticker;
    protected Color oldback;
    
    public TimerPanel() 
    {
    	this((Runnable)null);
    }
    
    public TimerPanel(Runnable due) 
    {
    	this(due, new Colorizer());
    }
    
    public TimerPanel(Consumer<TimerPanel> ticker) 
    {
    	this(null, ticker);
    }
    
    public TimerPanel(Runnable due, Consumer<TimerPanel> ticker) 
    {
        setLayout(new BorderLayout());

		//this.yellow = yellow;
    	//this.red = red;
    	this.due = due;
    	this.ticker = ticker;
 
        timerLabel = new JLabel();
        timerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 20));
        updateTimerLabel();

        add(timerLabel, BorderLayout.CENTER);

        this.oldback = getBackground();
    }

    public void start(int dur) 
    {
    	this.duration = dur;
    	
    	updateTimerLabel();
    	
    	if(stimer!=null)
    		stimer.stop();
    	
    	setBackground(oldback);
    	
    	stimer = new Timer(1000, new ActionListener() 
    	{
    		@Override
    		public void actionPerformed(ActionEvent e) 
    		{
    			duration--;
    			updateTimerLabel();
    			if(ticker!=null)
    				ticker.accept(TimerPanel.this);
    			
    			if(duration <= 0) 
    			{
    				stimer.stop();
    				if(due!=null)
    					due.run();
    			}
    		}
    	});
    	stimer.start();
    }

    public void stop()
    {
    	this.duration = 0;
    	updateTimerLabel();
    	if(stimer!=null)
    		stimer.stop();
    	setBackground(oldback);
    }
    
    private void updateTimerLabel() 
    {
        int minutes = duration / 60;
        int seconds = duration % 60;
        String time = String.format("%02d:%02d", minutes, seconds);
        timerLabel.setText(time);
    }
    
    public void setDuration(int dur)
    {
    	this.duration = dur;
    }
    
    /**
	 * @return the duration
	 */
	public int getDuration() 
	{
		return duration;
	}

	public static class Colorizer implements Consumer<TimerPanel>
    {
    	@Override
    	public void accept(TimerPanel t) 
    	{
    		if(t.getDuration() <= 5) 
				t.setBackground(Color.RED);
			else if(t.getDuration() <= 3) 
				t.setBackground(Color.YELLOW);
    	}
    }
    
    public static void main(String[] args) 
    {
        SwingUtilities.invokeLater(new Runnable() 
        {
            @Override
            public void run() 
            {
            	JFrame f = new JFrame("Timer");
            	TimerPanel timer = new TimerPanel();
                //TimerPanel timer = new TimerPanel(() -> { f.setVisible(false); f.dispose(); });
                timer.start(10);
                
                f.add(timer);
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                f.pack();
                f.setLocationRelativeTo(null);
                f.setVisible(true);
            }
        });
    }
}
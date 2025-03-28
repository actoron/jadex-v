package jadex.micro.quiz;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import jadex.common.SGUI;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnEnd;
import jadex.injection.annotation.OnStart;
import jadex.micro.mandelbrot.ui.PropertiesPanel;
import jadex.micro.quiz.TimerPanel.Colorizer;
import jadex.micro.quiz.model.NewQuizEvent;
import jadex.micro.quiz.model.Question;
import jadex.micro.quiz.model.QuestionEvent;
import jadex.micro.quiz.model.ResultEvent;

/**
 *  The quiz client agent.
 */
public class QuizClientAgent
{
	@Inject
	protected IComponent agent;
	
	/** The quiz service. */
	protected IQuizService quizservice;
	
	/** The gui. */
	protected QuizGui gui;
	
	/** The question count. */
	protected int questioncnt;
	protected int quizsize;
	
	@OnStart
	protected void onStart()
	{
		gui = new QuizGui();
	}
	
	@Inject
	protected void subscribeAtService(IQuizService qs)
	{
		System.out.println("Client found quiz service: "+qs);
		if(quizservice==null)
		{
			quizservice = qs;
			gui.setTitle(quizservice);
			qs.participate().next(event ->
			{
				if(event instanceof NewQuizEvent)
				{
					NewQuizEvent nqe = (NewQuizEvent)event;
					quizsize = nqe.getSize();
					gui.setQuiz(nqe.getName(), nqe.getDelay(), nqe.getSize(), nqe.getStart());
				}
				else if(event instanceof QuestionEvent)
				{
					questioncnt = ((QuestionEvent)event).getCount();
					gui.setQuestion(((QuestionEvent)event).getQuestion());
				}
				else if(event instanceof ResultEvent)
				{
					gui.setResult("Quiz result is: "+((ResultEvent)event).getResults().toString());
				}
				
			})
			.finished(x ->
			{
				quizservice = null;
				gui.setTitle(null);
				gui.setQuestion(null);
				//gui.setResult(null);
			})
			.catchEx(ex -> 
			{
				ex.printStackTrace();
				quizservice = null;
				gui.setTitle(null);
				gui.setQuestion(null);
				gui.setResult(null);
			});
		}
	}
	
	@OnEnd
	public void end()
	{
		gui.dispose();
	}
	
	/**
	 *  The quiz gui.
	 */
	public class QuizGui
	{
		protected JFrame f;
		protected JLabel qnamel;
		protected JLabel qsizel;
		protected JLabel qstartl;
		protected JLabel qdelayl;
		protected JLabel conl;
		protected JTextArea ta;
		protected TimerPanel timerp;
		protected JLabel la;
		protected ButtonGroup bg;
		
		protected long delay;
		protected boolean answered;
		
		/**
		 *  Create a new quiz gui.
		 */
		public QuizGui()
		{
			SwingUtilities.invokeLater(()-> 
			{
				f = new JFrame();
				PropertiesPanel pp = new PropertiesPanel();
				qnamel = new JLabel();
				qsizel = new JLabel();
				qstartl = new JLabel();
				qdelayl = new JLabel();
				pp.addComponent("Quiz Name", qnamel);
				pp.addComponent("Size", qsizel);
				pp.addComponent("Start", qstartl);
				pp.addComponent("Delay [s]", qdelayl);
				JPanel p = new JPanel(new BorderLayout());
				conl = new JLabel(" ");
				ta = new JTextArea();
				JPanel n = new JPanel(new BorderLayout());
				n.add(pp, BorderLayout.NORTH);
				n.add(conl, BorderLayout.CENTER);
				n.add(ta, BorderLayout.SOUTH);
				p.add(BorderLayout.NORTH, n);
				
				JPanel bp = new JPanel(new GridLayout(0, 1));
				bg = new ButtonGroup();
				for(int i=0; i<4; i++)
				{
					final int fi = i;
					JRadioButton b = new JRadioButton();
					b.addActionListener(a -> 
					{
						answered = true;
						agent.getFeature(IExecutionFeature.class).scheduleStep(Void ->
						{
							if(quizservice!=null)
								quizservice.sendAnswer(fi, questioncnt);
						});
					});
					bg.add(b);
					bp.add(b);
				}
				
				p.add(BorderLayout.CENTER, bp);
				la = new JLabel(" ");
				JPanel s = new JPanel(new BorderLayout());
				timerp = new TimerPanel(new Colorizer()
				{
					@Override
			    	public void accept(TimerPanel t) 
			    	{
						if(answered)
							t.setBackground(Color.GREEN);
						else
							super.accept(t);
			    	}
				});
				s.add(BorderLayout.CENTER, timerp);
				s.add(BorderLayout.SOUTH, la);
				p.add(BorderLayout.SOUTH, s);
				
				f.setTitle("Quiz UI of "+agent.getId());
				f.setLayout(new BorderLayout());		
				f.getContentPane().add(p, BorderLayout.CENTER);
				f.setSize(600, 300);
				//f.pack();
				f.setVisible(true);
				f.setLocation(SGUI.calculateMiddlePosition(f));
				
				f.addWindowListener(new WindowAdapter()
				{
					public void windowClosing(WindowEvent e)
					{
						agent.terminate();
					}
				});
				
				setQuestion(null);
			});
		}
		
		/**
		 *  Set a question
		 */
		public void setQuiz(String name, long delay, int size, long start)
		{
			this.delay = delay;
			
			SwingUtilities.invokeLater(()-> 
			{
				qnamel.setText(name);
				qsizel.setText(""+size);
				qdelayl.setText(""+delay/1000);
				SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
				qstartl.setText(sdf.format(new Date(start)));
				//update();
			});
		}
		
		/**
		 *  Set a question
		 */
		public void setQuestion(Question question)
		{
			answered = false;
			SwingUtilities.invokeLater(()-> 
			{
				ta.setText(question!=null? "Question "+(questioncnt+1)+"/"+quizsize+": "+question.getQuestion(): "No current question");
				int i=0;
				for(Enumeration<AbstractButton> buttons = bg.getElements(); buttons.hasMoreElements();)
				{
					AbstractButton button = buttons.nextElement();
					button.setText(question!=null? question.getAnswers().get(i++): "No answer "+(++i));
				}
				bg.clearSelection();
				
				if(question!=null)
					timerp.start((int)(delay/1000));
				//update();
			});
		}
	
		/**
		 *  Set result.
		 */
		public void setResult(String result)
		{
			SwingUtilities.invokeLater(()-> 
			{
				timerp.stop();
				la.setText(result);
			});
		}
		
		/**
		 *  Set the title.
		 */
		public void setTitle(IQuizService quizser)
		{
			SwingUtilities.invokeLater(()-> 
			{
				conl.setText(quizser!=null? "Connected with "+quizser: "");
			});
		}
		
		/**
		 *  Close the gui.
		 */
		public void dispose()
		{
			f.dispose();
		}
		
		/**
		 *  Pack the component.
		 */
		protected void update()
		{
			f.pack();
	        f.revalidate(); 
	        f.repaint(); 
		}
	}
}

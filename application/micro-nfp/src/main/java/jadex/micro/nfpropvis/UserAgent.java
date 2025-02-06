package jadex.micro.nfpropvis;

import java.awt.BorderLayout;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Day;
import org.jfree.data.time.Hour;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.Month;
import org.jfree.data.time.Quarter;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.Week;
import org.jfree.data.time.Year;
import org.jfree.data.xy.XYDataset;

import jadex.common.MethodInfo;
import jadex.common.Tuple2;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.execution.future.ComponentResultListener;
import jadex.future.IFuture;
import jadex.future.IResultListener;
import jadex.future.ITerminableIntermediateFuture;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Configuration;
import jadex.micro.annotation.Configurations;
import jadex.model.annotation.OnEnd;
import jadex.model.annotation.OnStart;
import jadex.nfproperty.INFPropertyFeature;
import jadex.nfproperty.impl.search.ComposedEvaluator;
import jadex.nfproperty.sensor.service.AverageEvaluator;
import jadex.nfproperty.sensor.service.WaitqueueEvaluator;
import jadex.providedservice.IService;
import jadex.providedservice.ServiceScope;
import jadex.providedservice.annotation.Service;
import jadex.requiredservice.IRequiredServiceFeature;
import jadex.requiredservice.annotation.RequiredService;
import jadex.requiredservice.annotation.RequiredServices;

/**
 *  Ranking of a requires services via an waitqueue ranker.
 */
@RequiredServices(@RequiredService(name="aser", type=ICryptoService.class, scope=ServiceScope.VM)) // multiple=true,
//ranker="new AverageEvaluator(new WaitqueueEvaluator(new MethodInfo(ICryttoService.class.getMethod(\"encrypt\", new Class[]{String.class}))))"

@Agent
@Service
@Configurations({@Configuration(name="default"), @Configuration(name="with gui")})

//@RequiredServices(@RequiredService(name="cryptoser", type=ICryptoService.class, multiple=true, 
//	binding=@Binding(scope=ServiceScope.PLATFORM, dynamic=true),
//	nfreqs=@NFRequirement(description="select service with smallest call waitqueue", 
//	value=WaitqueueProperty.class, methodname="encrypt", ranker=WaitqueueEvaluator.class)))
public class UserAgent
{
	@Agent
	protected IComponent agent;
	
	/** The evaluator. */
	protected ComposedEvaluator<ICryptoService> ranker;
	
	protected boolean gui;
		
	public UserAgent()
	{
		this(false);
	}
	
	public UserAgent(boolean gui)
	{
		this.gui = gui;
	}
	
	/**
	 *  The agent body.
	 * /
	@OnStart
	public void body()
	{
		// todo: make ITerminable in DefaultServiceFetcher
		
		try
		{
			while(true)
			{
				ComposedEvaluator<IAService> ranker = new ComposedEvaluator<IAService>();
				ranker.addEvaluator(new WaitqueueEvaluator(new MethodInfo(IAService.class.getMethod("test", new Class[0]))));
				ITerminableIntermediateFuture<IAService> sfut = agent.getComponentFeature(IRequiredServicesFeature.class).getServices("aser");
				Collection<Tuple2<IAService, Double>> res = SServiceProvider.rankServicesWithScores(sfut, ranker, null).get();
				System.out.println("Found: "+res);
				if(agent.getConfiguration().equals("with gui"))
					addData(res);
				IAService aser = res.iterator().next().getFirstEntity();
				aser.test().get();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}*/
	
	/**
	 *  The agent body.
	 */
	@OnStart
	public void body() throws Exception
	{
		// todo: make ITerminable in DefaultServiceFetcher
		
		// problem with execution time evaluator scheduling:
		// all use the best service until that service time drops below the level of the second
		// then all use the second and the second level drops hardly and cannot recover because
		// no one will ever use it again
		
		ranker = new ComposedEvaluator<ICryptoService>();
		AverageEvaluator eva = new AverageEvaluator(new WaitqueueEvaluator(agent.getComponentHandle(), new MethodInfo(ICryptoService.class.getMethod("encrypt", new Class[]{String.class}))));
		ranker.addEvaluator(eva);
//		ranker.addEvaluator(new ExecutionTimeEvaluator(new MethodInfo(IAService.class.getMethod("test", new Class[0]))));
		
		invoke();
	}
	
	//@AgentKilled
	@OnEnd
	public void cleanup()
	{
		if(frame!=null)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					frame.dispose();
				}
			});
		}
	}
	
	/**
	 * 
	 */
	public void invoke()
	{
		ITerminableIntermediateFuture<ICryptoService> sfut = agent.getFeature(IRequiredServiceFeature.class).getServices("aser");
		ITerminableIntermediateFuture<Tuple2<ICryptoService, Double>> fut = agent.getFeature(INFPropertyFeature.class).rankServicesWithScores(sfut, ranker, null);
		fut.addResultListener(new IResultListener<Collection<Tuple2<ICryptoService,Double>>>() {
			
			@Override
			public void resultAvailable(Collection<Tuple2<ICryptoService, Double>> res) 
			{
				System.out.println("next: "+res);
				
				if(gui)
					addData(res);
				if(res.size()>0)
				{
					ICryptoService aser = res.iterator().next().getFirstEntity();
					aser.encrypt("bla").then(enc -> 
					{
						System.out.println("next invoke");
						invoke();
					}).catchEx(ex ->
					{
						agent.getFeature(IExecutionFeature.class).waitForDelay(2000).get();
						agent.getFeature(IExecutionFeature.class).scheduleStep(() -> invoke());
					});
				}
				else
				{
					agent.getFeature(IExecutionFeature.class).waitForDelay(2000).get();
					agent.getFeature(IExecutionFeature.class).scheduleStep(() -> invoke());
				}
			}
			
			@Override
			public void exceptionOccurred(Exception exception) 
			{
				System.out.println("ex: "+exception);
			}
		});
		
		/*.then(res ->
		{
			System.out.println("next: "+res);
			if(gui)
				addData(res);
			ICryptoService aser = res.iterator().next().getFirstEntity();
			aser.encrypt("bla").then(enc -> 
			{
				invoke();
			}).catchEx(ex ->
			{
				agent.getFeature(IExecutionFeature.class).waitForDelay(2000).get();
				agent.getFeature(IExecutionFeature.class).scheduleStep(() -> invoke());
			});
		}).catchEx(ex ->
		{
//			if(wgui)
//				exception.printStackTrace();
			agent.getFeature(IExecutionFeature.class).waitForDelay(2000).get();
			agent.getFeature(IExecutionFeature.class).scheduleStep(() -> invoke());
		});	*/
	}
	
	/**
	 * 
	 */
	public void addData(Collection<Tuple2<ICryptoService, Double>> res)
	{
		for(Tuple2<ICryptoService, Double> tup: res)
		{
			ICryptoService ser = tup.getFirstEntity();
			Double val = tup.getSecondEntity();
			addValue(((IService)ser).getServiceId().toString(), System.currentTimeMillis(), val);
		}
	}
	
	
	/** The frame. */
	protected JFrame frame;
	
	/** The chart. */
	protected JFreeChart chart;

	/** The seriesmap. */
	protected Map<Comparable, Integer> seriesmap = new HashMap<Comparable, Integer>();
	
	/**
	 *  Get the chart panel.
	 *  @return The chart panel.
	 */
	public JPanel getChartPanel()
	{
		// Todo: should be swing thread?
//		assert SwingUtilities.isEventDispatchThread();
		ChartPanel panel = new ChartPanel(getChart(), false, false, false, false, false);
        panel.setFillZoomRectangle(true);
        return panel;
	}
	
	/**
	 *  Get the chart.
	 *  @return The chart.
	 */
	public JFreeChart getChart()
	{
		// Todo: should be swing thread?
//		assert SwingUtilities.isEventDispatchThread();
		if(chart==null)
			chart = createChart();
		return this.chart;
	}
	
	/**
	 *  Create a chart with the underlying dataset.
	 *  @return The chart.
	 */
	protected JFreeChart createChart()
	{
		XYDataset dataset = new TimeSeriesCollection();
		JFreeChart chart = ChartFactory.createTimeSeriesChart("Service Quality", "ms", "score", dataset, true, true, true);
		chart.setNotify(true);
		
		ChartPanel panel = new ChartPanel(chart);
		panel.setFillZoomRectangle(true);
		frame = new JFrame();
		JPanel content = new JPanel(new BorderLayout());
		content.add(panel, BorderLayout.CENTER);
		frame.setContentPane(panel);
		frame.pack();
		frame.setVisible(true);
		
		return chart;
	}
	
	/**
	 *  Add a value to a specific series of the chart.
	 *  @param valx The x value.
	 *  @param valy The y value.
	 *  @param data The data table.
	 *  @param row The current data row. 
	 */
	protected void addValue(Comparable<?> seriesname, Object valx, Object valy)
	{
		// Determine series number for adding the new data.
		
		int seriesnum = 0;
		TimeSeriesCollection dataset = (TimeSeriesCollection)((XYPlot)getChart().getPlot()).getDataset();
		int sercnt = dataset.getSeriesCount();
		Integer sernum = (Integer)seriesmap.get(seriesname);
		if(sernum!=null)
			seriesnum = sernum.intValue();
		else
			seriesnum = sercnt;
		
		Class<?> time = Millisecond.class;
		for(int j=sercnt; j<=seriesnum; j++)
		{
			Integer maxitemcnt = 10000;
			TimeSeries series;
			if(seriesname!=null)
			{
				series = new TimeSeries(seriesname); //, time
				if(maxitemcnt!=null)
					series.setMaximumItemCount(maxitemcnt.intValue());
				seriesmap.put(seriesname, Integer.valueOf(j));
			}
			else
			{
				series = new TimeSeries(Integer.valueOf(j)); // , time
				if(maxitemcnt!=null)
					series.setMaximumItemCount(maxitemcnt.intValue());
				seriesmap.put(Integer.valueOf(j), Integer.valueOf(j));
			}
			dataset.addSeries(series);
//			System.out.println("Created series: "+seriesname+" "+j);
		}	
		TimeSeries ser = dataset.getSeries(seriesnum);
		
		// Add the value.
		
		RegularTimePeriod t = null;
		if(Millisecond.class.equals(time) || time==null)
		{
			t= new Millisecond(new Date(((Number)valx).longValue()));
		}
		else if(Second.class.equals(time))
		{
			t= new Second(new Date(((Number)valx).longValue()));
		}
		else if(Hour.class.equals(time))
		{
			t= new Hour(new Date(((Number)valx).longValue()));
		}
		else if(Day.class.equals(time))
		{
			t= new Day(new Date(((Number)valx).longValue()));
		}
		else if(Week.class.equals(time))
		{
			t= new Week(new Date(((Number)valx).longValue()));
		}
		else if(Month.class.equals(time))
		{
			t= new Month(new Date(((Number)valx).longValue()));
		}
		else if(Quarter.class.equals(time))
		{
			t= new Quarter(new Date(((Number)valx).longValue()));
		}
		else if(Year.class.equals(time))
		{
			t= new Year(new Date(((Number)valx).longValue()));
		}
		
		// When the same time period is used twice the value will be overridden.
		ser.addOrUpdate(t, ((Number)valy).doubleValue());
	}
}

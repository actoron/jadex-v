package jadex.micro.house_monitoring;

import java.util.ArrayList;
import java.util.List;

import com.cronutils.model.Cron;

import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.micro.house_monitoring.IAlarmService.AlarmState;
import jadex.micro.house_monitoring.IRuleSystemService.Rule;
import jadex.micro.llmcall2.LlmBenchmark;

public class LlmSmartHomeBenchmark
{
	static List<IComponentHandle>	COMPONENTS	= new ArrayList<>();
	
	public static void main(String[] args)
	{
		String	prompt	= "Immer wenn Bewegungsmelder A auslöst, analysiere das aktuelle Bild von Kamera 1 "
			+ "und löse Alarm aus, wenn du eine verdächtige Situation bemerkst.";
		String	benchmark_name	= LlmSmartHomeBenchmark.class.getSimpleName();
		
		LlmBenchmark.runBenchmarks(benchmark_name, prompt,
			() -> {
				COMPONENTS.add(IComponentManager.get().create(new RuleSystem()
				{
//					protected void	scheduleCronRule(CronExpression cron, Rule rule)
					protected void	scheduleCronRule(Cron cron, Rule rule)
					{
						// NOP -> is executed manually in the benchmark response phase
					}
				}, "Rule System").get());
				COMPONENTS.add(IComponentManager.get().create(new Camera(), "Kamera 1").get());
				COMPONENTS.add(IComponentManager.get().create(new Camera(), "Kamera 2").get());
				COMPONENTS.add(IComponentManager.get().create(new Camera(), "Kamera 3").get());
				COMPONENTS.add(IComponentManager.get().create(new MotionSensor(), "Bewegungsmelder A").get());
				COMPONENTS.add(IComponentManager.get().create(new MotionSensor(), "Bewegungsmelder B").get());
				COMPONENTS.add(IComponentManager.get().create(new Alarm(), "Alarm").get());
			},
			response -> {
				
				// Check that there is exactly one rule created (the motion sensor rule)
				// And that the alarm is not triggered without motion
				IRuleSystemService	rule_system = LlmBenchmark.getService(IRuleSystemService.class, "Rule System");
				List<Rule>	rules	= rule_system.listRules().get();
				IAlarmService alarm = LlmBenchmark.getService(IAlarmService.class, "Alarm");
				if(rules.size()!=1
					|| rule_system.listRules().get().get(0).type()!=IRuleSystemService.EventType.MOTION_DETECTED
					|| !rule_system.listRules().get().get(0).source().equals("Bewegungsmelder A")
					|| alarm.getAlarmState().get()==AlarmState.ON)
				{
					return false;
				}
				
				// Check first use case: trigger motion sensor, check if alarm is (not) triggered
				IMotionSensorService sensor = LlmBenchmark.getService(IMotionSensorService.class, "Bewegungsmelder A");
				sensor.motionDetected().get(300000);
				if(alarm.getAlarmState().get()==AlarmState.ON)
				{
					return false;
				}
				
				ICameraService camera = LlmBenchmark.getService(ICameraService.class, "Kamera 1");
				camera.setCurrentImage("a burglar breaking into a house at night").get();
				sensor.motionDetected().get(300000);
				if(alarm.getAlarmState().get()!=AlarmState.ON)
				{
					return false;
				}
				
				// Check second use case: scheduled rule, check if alarm is triggered at the right time
				else
				{
					alarm.setAlarmState(AlarmState.OFF).get();
					
					String	prompt2	= 
						"Überprüfe alle 30 Sekunden die aktuellen Bilder von Kamera 2 und 3 "
						+ "und löse Alarm aus, wenn du eine verdächtige Situation bemerkst.";
					rule_system.executePrompt(prompt2).get(300000);
					rules	= rule_system.listRules().get();
					
					// Check if motion sensor rule is still present
					// And at least one cron rule is created
					// And the alarm is not triggered immediately (before the first 30 seconds are over)
					if(!rules.stream().filter(r -> r.type()==IRuleSystemService.EventType.MOTION_DETECTED).findAny().isPresent()
						|| !rules.stream().filter(r -> r.cron_expression()!=null).findAny().isPresent()
						|| alarm.getAlarmState().get()==AlarmState.ON)
					{
						return false;
					}
					
					// Check and execute the cron rules manually (maybe one for each camera)
					for(Rule rule: rules.stream().filter(r -> r.cron_expression()!=null).toList())
					{
//						try
//						{
							// Check if the rule is scheduled to run within the next 30 seconds
//							CronExpression cron	= new CronExpression(rule.cron_expression());
							Cron cron	= RuleSystem.CRON_PARSER.parse(rule.cron_expression());
							long	current_time	= System.currentTimeMillis();
//							long	next_time	= cron.getNextValidTimeAfter(new java.util.Date(current_time)).getTime();
							long	next_time	= RuleSystem.getNextExecutionTime(cron, current_time, rule);
							long	delay	= next_time - current_time;
							if(delay>30000)
							{
								return false;
							}
							
							// Check if delta is 30 seconds
							long next_time2	= RuleSystem.getNextExecutionTime(cron, next_time+1, rule);
							if(next_time2 - next_time!=30000)
							{
								return false;
							}
							
							String	fprompt	= "The rule "+rule.rule_id()+" has been triggered. Thus you as the LLM should perform the following action(s):\n"
									+ rule.prompt();
							rule_system.executePrompt(fprompt).get(300000);
//						}
//						catch (ParseException e)
//						{
//							// Should not happen, because the rule system validates the cron expression.
//							return false;
//						}
					}
					
					if(alarm.getAlarmState().get()==AlarmState.ON)
					{
						return false;
					}
					
					camera = LlmBenchmark.getService(ICameraService.class, "Kamera 2");
					camera.setCurrentImage("a burglar breaking into a house at night").get();
					
					// Check and execute the cron rules manually (maybe one for each camera)
					for(Rule rule: rules.stream().filter(r -> r.cron_expression()!=null).toList())
					{
						String	fprompt	= "The rule "+rule.rule_id()+" has been triggered. Thus you as the LLM should perform the following action(s):\n"
							+ rule.prompt();
						rule_system.executePrompt(fprompt).get(300000);
					}
					return alarm.getAlarmState().get()==AlarmState.ON;
				}
			},
			() -> {
				for(IComponentHandle comp : COMPONENTS)
				{
					comp.terminate().get();
				}
				COMPONENTS.clear();
			});

	}
}

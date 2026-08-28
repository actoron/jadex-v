package jadex.bding;

import java.util.HashMap;
import java.util.Map;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.StreamingChatModel;
import jadex.bding.annotation.BDINGAgent;
import jadex.bding.annotation.Belief;
import jadex.bding.impl.RGoal;
import jadex.bding.tool.BDIViewer;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.impl.ComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;
import jadex.micro.llmcall2.LlmChatAgent2;
import jadex.micro.llmcall2.LlmHelper;
import jadex.providedservice.annotation.Service;

/** 

    Goal      = desired world state ("what")
    Intention = strategic approach ("how at a conceptual level")
    Plan      = concrete operationalization ("how exactly")
    PlanBody  = executable realization

    Goal:
        Deliver package

    Intentions:
        Use drone

        Use truck

        Ask another agent

    Plans:
        Fly with drone XYZ from A to B
        
        Request drone service from provider X
        
        Use autonomous drone fleet

    Goal:      Wach werden
   │
   ├── Intention: Kaffee trinken
   │       │
   │       ├── Plan: Kaffeeautomat benutzen
   │       │
   │       └── Plan: Kaffee im Café kaufen
   │
   └── Intention: kalt duschen
           │
           └── Plan: Dusche einschalten

    Goal
    |
    | "Was soll erreicht werden?"
    v
    Intention
    |
    | "Welche Strategie verfolge ich?"
    v
    Plan
    |
    | "Wie setze ich diese Strategie konkret um?"
    v
    PlanBody

    Goal
    |
    RGoal
    |
    adopt()
    |
    Brain.generateIntentions()
    |
    Brain.selectIntention()
    |
    RIntention
    |
    adopt()
    |
    Brain.generatePlans()
    |
    Brain.selectPlan()
    |
    RPlan
    |
    execute()

*/    
public class Main 
{
    @Service
    public interface IAppTools
    {
        @Tool("Get all available train connections between two locations, including departure time, arrival time, duration, and price.")
        public IFuture<TripInfo> getTrainConnectionInfo(String from, String to);

        @Tool("Get all available bus connections between two locations, including departure time, arrival time, duration, and price.")
        public IFuture<TripInfo> getBusConnectionInfo(String from, String to);

        @Tool("Buy a bus ticket for the specified connection using the specified account.")
        public IFuture<Ticket> buyBusTicket(TripInfo connection, String account);

        @Tool("Buy a train ticket for the specified connection using the specified account.")
        public IFuture<Ticket> buyTrainTicket(TripInfo connection, String account);

        @Tool("Travel by train using the specified purchased train ticket.")
        public IFuture<String> travelByTrain(Ticket ticket);

        @Tool("Travel by bus using the specified purchased bus ticket.")
        public IFuture<String> travelByBus(Ticket ticket);

        @Tool("Walk from one location to another location.")
        public IFuture<String> walk(String from, String to);

        @Tool("Get the current balance of the specified account.")
        public IFuture<String> getAccountValue(String account);
    }

    public record TripInfo(String from, String to, String starttime, String endtime, String duration, double price) 
    {
    }

    public record Ticket(String from, String to, int no)
    {
    }

    @BDINGAgent
    public static class UniversityAgent
    {
        @Inject
        protected IComponent agent;

        //@Model
        //protected AgentModel model;

        //@Reasoner
        //protected IReasoner reasoner;

        @Belief(description = "Describes the current weather.")
        protected String weather;

        @Belief(description = "Bank number of account.") 
        protected String account;

        @Belief(description = "The current location of the agent.")
        protected String location;

        public UniversityAgent()
        {
            this("Hamburg", "account1");//, null);
        }

        public UniversityAgent(String location, String account)//, AgentModel model)
        {
            this.location = location;
            this.account = account;
            //this.model = model;
        }
        
        /*OnStart
        protected void onStart()
        {
            //System.out.println("Hello from agent " + agent.getId()+" "+agent.getClass().getName());

            RGoal goal = agent.getFeature(IBDINGAgentFeature.class).dispatchTopLevelGoal(
                "Go now to City University of Applied Sciences in Bremen.").get();
            
            goal.getFinished().then(Void ->
            {
                System.out.println("goal finished: "+goal.getState());
                agent.terminate();
            }).catchEx(ex ->
            {
                System.out.println("goal finished with ex: "+ex.getMessage());
                ex.printStackTrace();
                agent.terminate();
            });
        }*/

        @OnStart
        protected void onStart()
        {
            agent.getFeature(IBDINGAgentFeature.class).dispatchTopLevelGoal(
                "Go now to City University of Applied Sciences in Bremen.")
            .then(goal ->
            {
                goal.getFinished().then(Void ->
                {
                    System.out.println("goal finished: " + goal.getState());
                    agent.terminate();
                }).catchEx(ex ->
                {
                    System.out.println("goal finished with ex: " + ex.getMessage());
                    ex.printStackTrace();
                    agent.terminate();
                });
            })
            .catchEx(ex ->
            {
                System.out.println("dispatch failed: " + ex.getMessage());
                ex.printStackTrace();
                agent.terminate();
            });
        }
    }

    @Service
    public static class ToolAgent implements IAppTools
    {
        protected Map<String, Double> accounts = new HashMap<>();
        protected int nextTicketNo = 1;

        public ToolAgent()
        {
            accounts.put("account1", 100.0);
            accounts.put("account2", 20.0);
        }

        @Override
        public IFuture<Ticket> buyBusTicket(TripInfo connection, String account)
        {
            Future<Ticket> ret = new Future<>();

            double price = getBusPrice(connection.from(), connection.to());

            Double balance = accounts.get(account);

            if(balance == null)
            {
                ret.setException(new RuntimeException("Unknown account: " + account));
                return ret;
            }

            if(balance < price)
            {
                ret.setException(new RuntimeException("Insufficient funds. Required: " + price + ", available: " + balance));
                return ret;
            }

            accounts.put(account, balance - price);

            Ticket ticket = new Ticket(connection.from(), connection.to(), nextTicketNo++);

            System.out.println("Bought bus ticket: " + ticket + " for " + price + " EUR using " + account);

            ret.setResult(ticket);
            return ret;
        }

        @Override
        public IFuture<Ticket> buyTrainTicket(TripInfo connection, String account)
        {
            Future<Ticket> ret = new Future<>();

            double price = getTrainPrice(connection.from(), connection.to());

            Double balance = accounts.get(account);

            if(balance == null)
            {
                ret.setException(new RuntimeException("Unknown account: " + account));
                return ret;
            }

            if(balance < price)
            {
                ret.setException(new RuntimeException("Insufficient funds. Required: " + price + ", available: " + balance));
                return ret;
            }

            accounts.put(account, balance - price);

            Ticket ticket = new Ticket(connection.from(), connection.to(), nextTicketNo++);

            System.out.println("Bought train ticket: " + ticket + " for " + price + " EUR using " + account);

            ret.setResult(ticket);
            return ret;
        }

        @Override
        public IFuture<String> getAccountValue(String account)
        {
            Future<String> ret = new Future<>();

            Double balance = accounts.get(account);

            if(balance == null)
            {
                ret.setException(new RuntimeException("Unknown account: " + account));
                return ret;
            }

            ret.setResult(String.format("%.2f", balance));
            return ret;
        }

        @Override
        public IFuture<TripInfo> getBusConnectionInfo(String from, String to)
        {
            Future<TripInfo> ret = new Future<>();

            double price = getBusPrice(from, to);

            TripInfo info = new TripInfo(from, to, "10:00", "13:00", "3h", price);

            ret.setResult(info);
            return ret;
        }

        @Override
        public IFuture<TripInfo> getTrainConnectionInfo(String from, String to)
        {
            Future<TripInfo> ret = new Future<>();

            double price = getTrainPrice(from, to);

            TripInfo info = new TripInfo(from, to, "10:15", "11:35", "1h 20min", price);

            ret.setResult(info);
            return ret;
        }

        @Override
        public IFuture<String> travelByBus(Ticket ticket)
        {
            Future<String> ret = new Future<>();

            System.out.println("Traveling by bus from " + ticket.from() + " to " + ticket.to() + " using ticket " + ticket.no());

            ret.setResult("Arrived at " + ticket.to() + " from " + ticket.from() + " by bus.");

            return ret;
        }

        @Override
        public IFuture<String> travelByTrain(Ticket ticket)
        {
            Future<String> ret = new Future<>();

            System.out.println("Traveling by train from " + ticket.from() + " to " + ticket.to() + " using ticket " + ticket.no());

            ret.setResult("Arrived at " + ticket.to() + " from " + ticket.from() + " by train.");

            return ret;
        }

        @Override
        public IFuture<String> walk(String from, String to)
        {
            Future<String> ret = new Future<>();

            System.out.println("Walking from " + from + " to " + to);

            ret.setResult("Arrived at " + to + " from " + from + " on foot.");

            return ret;
        }

        protected double getBusPrice(String from, String to)
        {
            if(from.equalsIgnoreCase("Hamburg") && to.equalsIgnoreCase("Bremen"))
            {
                return 12.0;
            }

            return 15.0;
        }

        protected double getTrainPrice(String from, String to)
        {
            if(from.equalsIgnoreCase("Hamburg") && to.equalsIgnoreCase("Bremen"))
            {
                return 25.0;
            }

            return 30.0;
        }
    }

    public static void main(String[] args) 
    {
        ComponentManager.get().create(new ToolAgent()).get();

<<<<<<< HEAD
        //StreamingChatModel llm = LlmHelper.createChatModel(LlmHelper.Provider.OLLAMA_REMOTE, "gemma4:31b", false);
        StreamingChatModel llm = LlmHelper.createChatModel(LlmHelper.Provider.OPENAI_HCI, "api-programming-preloaded-1", 
            false, true);
        
=======
        StreamingChatModel llm = LlmHelper.createChatModel(LlmHelper.Provider.OLLAMA, "gemma4:31b", false);

>>>>>>> 0bdf2a148e3ff265d421d9915f5d53ccbaca87e0
        ComponentManager.get().create(new LlmChatAgent2(llm)).get();

        IComponentHandle ua = ComponentManager.get().create(new UniversityAgent()).get();

        BDIViewer viewer = new BDIViewer(ua);
        viewer.setVisible(true);

        ComponentManager.get().waitForLastComponentTerminated();
    }
}

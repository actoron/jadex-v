package jadex.publishservice;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import jadex.common.Tuple2;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.IIntermediateResultListener;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.SubscriptionIntermediateFuture;
import jadex.providedservice.annotation.Service;
import jadex.publishservice.publish.annotation.ParametersMapper;
import jadex.publishservice.publish.annotation.ResultMapper;
import jadex.publishservice.publish.mapper.IParameterMapper2;
import jadex.publishservice.publish.mapper.IValueMapper;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;

@Service
public interface ITestService 
{
    public IComponent getComponent();

    @GET
    public default IFuture<String> getString()
    {
        return new Future<>("hello");
    }

    @GET
    public default ISubscriptionIntermediateFuture<String> getStrings()
    {
        SubscriptionIntermediateFuture<String> ret = new SubscriptionIntermediateFuture<>();

        int[] i = new int[1];
        Runnable step = new Runnable()
        {
            @Override
            public void run() 
            {
                ret.addIntermediateResult(""+i[0]++);
                getComponent().getFeature(IExecutionFeature.class).waitForDelay(500).get();
                if(i[0]<3)
                    getComponent().getFeature(IExecutionFeature.class).scheduleStep(this);
                else
                    ret.setFinished();
            }
        };

        getComponent().getFeature(IExecutionFeature.class).scheduleStep(step);

        return ret;
    }

    @GET
    public default ISubscriptionIntermediateFuture<String> getStringsInfinite()
    {
        SubscriptionIntermediateFuture<String> ret = new SubscriptionIntermediateFuture<>()
        {
            @Override
            public void terminate(Exception reason) 
            {
                System.out.println("terminate called: "+reason);
                super.terminate(reason);
            }
        };

        int[] i = new int[1];
        Runnable step = new Runnable()
        {
            @Override
            public void run() 
            {
                if(ret.addIntermediateResultIfUndone(""+i[0]++))
                {
                    getComponent().getFeature(IExecutionFeature.class).waitForDelay(500).get();
                    getComponent().getFeature(IExecutionFeature.class).scheduleStep(this);
                }
                else
                {
                    System.out.println("Call terminated: "+ret);
                }
            }
        };

        getComponent().getFeature(IExecutionFeature.class).scheduleStep(step);

        /*ret.addResultListener(new IIntermediateResultListener<String>() 
        {
            @Override
            public void intermediateResultAvailable(String result) 
            {
                System.out.println("intermediateResAva: "+result);
            }

            @Override
            public void exceptionOccurred(Exception exception) 
            {
                System.out.println("ex: "+exception);
            }

            @Override
            public void resultAvailable(Collection<String> result) 
            {
                System.out.println("resAva: "+result);
            }

            @Override
            public void finished() 
            {
                System.out.println("finished");
            }

            @Override
            public void maxResultCountAvailable(int max) 
            {
            }
        });*/

        return ret;
    }

    // is automapped when automapping is enabled in @Publish 
    public default IFuture<String> getString2(String in)
    {
        return new Future<>(in+"!");
    }

    // is automapped when automapping is enabled in @Publish 
    public default IFuture<Void> setString(String in)
    {
        System.out.println("setString called with: " + in);
        return IFuture.DONE;
    }

    public default IFuture<Void> removeString(String in)
    {
        System.out.println("removeString called with: " + in);
        return IFuture.DONE;
    }

    @POST
    public default IFuture<Customer> paramMapping(Customer c)
    {
        return new Future<>(c);
    }

    @POST
    @ParametersMapper(CustomerMapper.class)
    public default IFuture<Void> paramMapping2(String name, int age)
    {
        if(name == null)
            throw new IllegalArgumentException("name is null");
        if(age == 0)
            throw new IllegalArgumentException("age is 0");

        System.out.println("paramMapping2 called with: name=" + name + ", age=" + age);
        return IFuture.DONE;
    }

    @POST
    @ResultMapper(ResMapper.class)
    public default IFuture<Tuple2<String, Integer>> resultMapping(String name, int age)
    {
        if(name == null)
            throw new IllegalArgumentException("name is null");
        if(age == 0)
            throw new IllegalArgumentException("age is 0");

        System.out.println("resultMapping called with: name=" + name + ", age=" + age);

        return new Future<>(new Tuple2<String, Integer>(name, age));
    }

    /*@POST
    public default Customer2 paramMapping2(Customer2 c)
    {
        return c;
    }*/

    public static class ResMapper implements IValueMapper
    {
        @Override
        public Object convertValue(Object value) throws Exception
        {
            Object ret = value;
            if(value instanceof Tuple2)
            {
                Tuple2<String, Integer> tup = (Tuple2<String, Integer>)value;
                ret = new Customer(tup.getFirstEntity(), tup.getSecondEntity(), null);
            }
            return ret;
        }
    }

    public static class CustomerMapper implements IParameterMapper2
    {
        @Override
        public Object[] convertParameters(Map<String, Object> values, Tuple2<List<Tuple2<String, String>>, Map<String, Class<?>>> pinfos, Object request) throws Exception
        {
            String name = (String)values.get("name");
            Integer age;
            Object ageo = values.get("age");
            if(ageo instanceof Integer)
                age = (Integer)ageo;
            else if(ageo instanceof String)
                age = Integer.parseInt((String)ageo);
            else
                age = 0;
            return new Object[] { name, age };
        }
    }
}
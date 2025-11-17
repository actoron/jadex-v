package jadex.publishservice;

import java.util.List;
import java.util.Map;

import jadex.common.Tuple2;
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
    @GET
    public String getString();

    // is automapped when automapping is enabled in @Publish 
    public String getString2(String in);

    // is automapped when automapping is enabled in @Publish 
    public void setString(String in);

    public default void removeString(String in)
    {
        System.out.println("removeString called with: " + in);
    }

    @POST
    public default Customer paramMapping(Customer c)
    {
        return c;
    }

    @POST
    @ParametersMapper(CustomerMapper.class)
    public default void paramMapping2(String name, int age)
    {
        if(name == null)
            throw new IllegalArgumentException("name is null");
        if(age == 0)
            throw new IllegalArgumentException("age is 0");

        System.out.println("paramMapping2 called with: name=" + name + ", age=" + age);
    }

    @POST
    @ResultMapper(ResMapper.class)
    public default Tuple2<String, Integer> resultMapping(String name, int age)
    {
        if(name == null)
            throw new IllegalArgumentException("name is null");
        if(age == 0)
            throw new IllegalArgumentException("age is 0");

        System.out.println("resultMapping called with: name=" + name + ", age=" + age);

        return new Tuple2<String, Integer>(name, age);
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
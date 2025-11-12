
package jadex.publishservice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.impl.ComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.injection.annotation.OnStart;
import jadex.publishservice.publish.annotation.Publish;
import jakarta.ws.rs.GET;

import static io.restassured.RestAssured.*;
import static io.restassured.matcher.RestAssuredMatchers.*;
import static org.hamcrest.Matchers.*;

//@EnabledIf("PublishTests#isEnabled")
public class PublishTestsBase 
{
    @Publish(publishid="http://${host}:${port}/${cid}/api", publishtarget = ITestService.class)
    //@Publish(publishid="http://${host}:${port}/api", publishtarget = ITestService.class)
    class Provider implements ITestService
    {
        /*@OnStart
        public void onStart(IComponent agent)
        {
            System.out.println("=== JVM Classpath ===");
            System.out.println(System.getProperty("java.class.path"));
            System.out.println("Agent: "+agent.getId().getLocalName());
            try 
            {
                Class<?> cl = Class.forName("jadex.publishservicejetty.impl.PublishServiceJettyFeature");
                System.out.println("Found class: " + cl.getName());
            } 
            catch (Exception e) 
            {
                e.printStackTrace();
            }
        }*/

        @Override
        public String getString() 
        {
            return "hello";
        }

        @Override
        public String getString2(String in) 
        {
            return in+"!";
        }

        @Override
        public void setString(String in) 
        {
            System.out.println("setString called with: " + in);
        }
    }

    @BeforeAll
    public static void setup() 
    {
        System.setProperty("host", "localhost");
        System.setProperty("port", "8080");
    }

    @Test
    public void testGetString() 
    {
        IComponentHandle provider = ComponentManager.get().create(new Provider()).get();

        String baseUrl = "http://"+System.getProperty("host") +":"+ System.getProperty("port") +"/" + provider.getId().getLocalName() + "/api";
        //baseUrl = "http://"+System.getProperty("host") +":"+ System.getProperty("port") + "/api";

        System.out.println("Base URL: " + baseUrl);

        get(baseUrl + "/getString").then().assertThat()
            .statusCode(200);
       
        provider.terminate();
    }

    @Test
    public void testGetString2() 
    {
        IComponentHandle provider = ComponentManager.get().create(new Provider()).get();

        String baseUrl = "http://"+System.getProperty("host") +":"+ System.getProperty("port") +"/" + provider.getId().getLocalName() + "/api";
        //baseUrl = "http://"+System.getProperty("host") +":"+ System.getProperty("port") + "/api";

        System.out.println("Base URL: " + baseUrl);

        get(baseUrl + "/getString2?id=123").then().assertThat()
            .statusCode(200)
            .body(equalTo("123!"));
            //.body(equalTo("\"123!\""));*/

        provider.terminate();
    }

    @Test
    public void testSetStringWrong() 
    {
        IComponentHandle provider = ComponentManager.get().create(new Provider()).get();

        String baseUrl = "http://"+System.getProperty("host") +":"+ System.getProperty("port") +"/" + provider.getId().getLocalName() + "/api";
        //baseUrl = "http://"+System.getProperty("host") +":"+ System.getProperty("port") + "/api";

        System.out.println("Base URL: " + baseUrl);

        get(baseUrl + "/setString?in=abc").then().assertThat()
            .statusCode(400);

        //int status = get(baseUrl + "/setString?in=abc")
        //    .getStatusCode();

        //System.out.println("Status: " + status);
        //System.out.println("Test finished.");
       
        provider.terminate();
    }

    @Test
    public void testSetStringOk() 
    {
        IComponentHandle provider = ComponentManager.get().create(new Provider()).get();

        String baseUrl = "http://"+System.getProperty("host") +":"+ System.getProperty("port") +"/" + provider.getId().getLocalName() + "/api";
        //baseUrl = "http://"+System.getProperty("host") +":"+ System.getProperty("port") + "/api";

        System.out.println("Base URL: " + baseUrl);

        put(baseUrl + "/setString?in=abc").then().assertThat()
            .statusCode(200);
       
        provider.terminate();
    }

    @Test
    public void testRemoveString() 
    {
        IComponentHandle provider = ComponentManager.get().create(new Provider()).get();

        String baseUrl = "http://"+System.getProperty("host") +":"+ System.getProperty("port") +"/" + provider.getId().getLocalName() + "/api";
        //baseUrl = "http://"+System.getProperty("host") +":"+ System.getProperty("port") + "/api";

        System.out.println("Base URL: " + baseUrl);

        delete(baseUrl + "/removeString?in=abc").then().assertThat()
            .statusCode(200);
       
        provider.terminate();
    }

    // todo: test parameter mapping

    @Test
    public void testParameterMapping() 
    {
        IComponentHandle provider = ComponentManager.get().create(new Provider()).get();

        String baseUrl = "http://"+System.getProperty("host") +":"+ System.getProperty("port") +"/" + provider.getId().getLocalName() + "/api";
        //baseUrl = "http://"+System.getProperty("host") +":"+ System.getProperty("port") + "/api";

        System.out.println("Base URL: " + baseUrl);

        Customer c = new Customer("John Doe", 30, "123 Main St");
        
        /*given()
            .body(c)
            .contentType("application/json")
        .when()
            .post(baseUrl + "/paramMapping")
        .then()
            .statusCode(200)
            .body(equalTo(c));*/

        Customer resc =
        given()
            .body(c)
            .contentType("application/json")
        .when()
            .post(baseUrl + "/paramMapping")
        .then()
            .statusCode(200)
            .extract()
            .as(Customer.class);

        assertEquals(c, resc);
       
        provider.terminate();
    }

    @Test
    public void testParameterMapping2() 
    {
        IComponentHandle provider = ComponentManager.get().create(new Provider()).get();

        String baseUrl = "http://"+System.getProperty("host") +":"+ System.getProperty("port") +"/" + provider.getId().getLocalName() + "/api";
        //baseUrl = "http://"+System.getProperty("host") +":"+ System.getProperty("port") + "/api";

        System.out.println("Base URL: " + baseUrl);

        Customer c = new Customer("John Doe", 30, "123 Main St");
        
        given()
            .body(c)
            .contentType("application/json")
        .when()
            .post(baseUrl + "/paramMapping2")
        .then()
            .statusCode(200);
       
        provider.terminate();
    }

    @Test
    public void testParameterMapping3() 
    {
        IComponentHandle provider = ComponentManager.get().create(new Provider()).get();

        String baseUrl = "http://"+System.getProperty("host") +":"+ System.getProperty("port") +"/" + provider.getId().getLocalName() + "/api";
        //baseUrl = "http://"+System.getProperty("host") +":"+ System.getProperty("port") + "/api";

        System.out.println("Base URL: " + baseUrl);

        given()
            .contentType("application/json")
        .when()
            .post(baseUrl + "/paramMapping2/?name=John%20Doe&age=30")
        .then()
            .statusCode(200);
       
        provider.terminate();
    }

    @Test
    public void testResultMapping() 
    {
        IComponentHandle provider = ComponentManager.get().create(new Provider()).get();

        String baseUrl = "http://"+System.getProperty("host") +":"+ System.getProperty("port") +"/" + provider.getId().getLocalName() + "/api";
        //baseUrl = "http://"+System.getProperty("host") +":"+ System.getProperty("port") + "/api";

        System.out.println("Base URL: " + baseUrl);

        /*given()
            .contentType("application/json")
        .when()
            .post(baseUrl + "/resultMapping/?name=John%20Doe&age=30")
        .then()
            .statusCode(200);*/
       

        Customer c = new Customer("John Doe", 30, null);
        
        Customer resc =
        given()
            .body(c)
            .contentType("application/json")
        .when()
            .post(baseUrl + "/resultMapping/?name=John%20Doe&age=30")
        .then()
            .statusCode(200)
            .extract()
            .as(Customer.class);

        assertEquals(c, resc);

        provider.terminate();
    }

    /*@Test
    public void testParameterMapping2() 
    {
        IComponentHandle provider = ComponentManager.get().create(new Provider()).get();

        String baseUrl = "http://"+System.getProperty("host") +":"+ System.getProperty("port") +"/" + provider.getId().getLocalName() + "/api";
        //baseUrl = "http://"+System.getProperty("host") +":"+ System.getProperty("port") + "/api";

        System.out.println("Base URL: " + baseUrl);
Customer c = new Customer("John Doe", 30, "123 Main St");
        Customer2 c = new Customer2("John Doe");
        c.setAge(30);
        c.setAddress("123 Main St");
        
        /*given()
            .body(c)
            .contentType("application/json")
        .when()
            .post(baseUrl + "/paramMapping2")
        .then()
            .statusCode(200)
            .body(equalTo(c));* /

        Customer2 resc =
        given()
            .body(c)
            .contentType("application/json")
        .when()
            .post(baseUrl + "/paramMapping")
        .then()
            .statusCode(200)
            .extract()
            .as(Customer2.class);

        assertEquals(c, resc);

        provider.terminate();
    }*/

    // todo: return info site 

    // todo: test error handling


    /*@Test
    public void testGet() 
    {
        IComponentHandle provider = ComponentManager.get().create(new Provider()).get();

        String baseUrl = "http://"+System.getProperty("host") +":"+ System.getProperty("port") +"/" + provider.getId().getLocalName() + "/api";
        //baseUrl = "http://"+System.getProperty("host") +":"+ System.getProperty("port") + "/api";

        System.out.println("Base URL: " + baseUrl);

        /*String res = get(baseUrl + "/getString2?id=123")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString();
        System.out.println("Response: " + res);
        System.out.println("[" + res + "]");
        System.out.println("Length: " + res.length());*/

        /*get(baseUrl + "/getString").then().assertThat()
            .statusCode(200);*/

        /*get(baseUrl + "/getString2?id=123").then().assertThat()
            .statusCode(200)
            .body(equalTo("123!"));
            //.body(equalTo("\"123!\""));* /

       
        get(baseUrl + "/setString?in=abc").then().assertThat()
            .statusCode(200);
       
        System.out.println("Test finished.");

        //ComponentManager.get().waitForLastComponentTerminated();
    }*/
}

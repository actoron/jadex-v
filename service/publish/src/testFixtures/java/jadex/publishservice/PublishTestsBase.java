
package jadex.publishservice;

import static io.restassured.RestAssured.delete;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.put;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import jadex.common.SUtil;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.impl.ComponentManager;
import jadex.future.Future;
import jadex.future.IIntermediateFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.injection.annotation.Inject;
import jadex.publishservice.impl.RequestManagerFactory;
import jadex.publishservice.impl.v2.ws.WsServiceProxy;
import jadex.publishservice.publish.annotation.Publish;
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

//@EnabledIf("PublishTests#isEnabled")
public class PublishTestsBase 
{
    @Publish(publishid="http://${host}:${port}/${cid}/api", publishinterface = ITestService.class)
    //@Publish(publishid="http://${host}:${port}/api", publishtarget = ITestService.class)
    class Provider implements ITestService
    {
        @Inject
        protected IComponent component;

        @Override
        public IComponent getComponent() 
        {
            return component;
        }
    }

    // todo: allow omitting interface when agent only has one service
    //@Publish(publishid="http://${host}:${port}/${cid}/api", publishinterface = ITestService.class, 
    @Publish(publishid="http://${host}:${port}/mytestservice", publishinterface = ITestService.class, 
        publishtype = "ws", publishname = "mytestservice")
    public static class Provider2 implements ITestService
    {
        @Inject
        protected IComponent component;

        @Override
        public IComponent getComponent() 
        {
            return component;
        }
    }

    @BeforeAll
    public static void setup() 
    {
        System.setProperty("host", "localhost");
        System.setProperty("port", "8080");
    }

    public void runAll()
    {
        testGetString();
        testSetStringOk();
        //testSetStringWrong();
        testRemoveString();
        testParameterMapping();
        testParameterMapping2();
        testParameterMapping3();
        testResultMapping();
        
        testWebSocketService();
        testWebSocketService2();
        testWebSocketService3();
        testWebSocketTerminate();
    }

    public String getWsUrl()
    {
        String host = System.getProperty("host", "localhost");
        String port = System.getProperty("port", "8080");
        String path = System.getProperty("path", "/ws");

        if (!path.startsWith("/")) 
            path = "/" + path;

        String ret = "ws://" + host + ":" + port + path;
        System.out.println("Using ws url: "+ret);
        return ret;
    }

    public String getRestUrl(IComponentHandle provider)
    {
        String ret = "http://"+System.getProperty("host") +":"+ System.getProperty("port") +"/" + provider.getId().getLocalName() + "/api";
        System.out.println("Using rest URL: " + ret);
        return ret;
    }

    @Test
    public void testWebSocketService() 
    {
        Assumptions.assumeTrue(RequestManagerFactory.getInstance(true).isSupported(PublishType.WS),
            "WebSocket not supported"
        );

        IComponentHandle provider = null;
        Session session = null;

        try 
        {
            provider = ComponentManager.get().create(new Provider2()).get();
            Future<String> ret = new Future<>();

            WebSocketContainer container = ContainerProvider.getWebSocketContainer();

            session = container.connectToServer(new Endpoint() 
            {
                @Override
                public void onOpen(Session session, EndpointConfig config) 
                {
                    session.addMessageHandler(String.class, res ->
                    {
                        System.out.println("Received response: "+res);
                        ret.setResult(res);
                    });

                    JsonObject json = new JsonObject();
                    //json.add("serviceName", "Provider2");
                    json.add("callid", "call_1");
                    json.add("serviceType", ITestService.class.getName());
                    json.add("method", "getString");

                    //JsonArray params = new JsonArray();
                    //json.add("params", params);
                    /*String msg = json.toString();
                    send(session, msg);*/

                    //json = new JsonObject();
                    ////json.add("serviceName", "Provider2");
                    /*json.add("callid", "call_2");
                    json.add("serviceType", ITestService.class.getName());
                    json.add("method", "getStrings");*/

                    String msg = json.toString();
                    send(session, msg);
                }

                public void send(Session session, String msg)
                {
                    System.out.println("Sending: " + msg);
                    try 
                    {
                        session.getBasicRemote().sendText(msg);
                    } 
                    catch (IOException e) 
                    {
                        ret.setException(e);
                    }
                }

                @Override
                public void onError(Session session, Throwable thr) 
                {
                    ret.setException(new RuntimeException(thr));
                }

                @Override
                public void onClose(Session session, CloseReason closeReason) 
                {
                    System.out.println("WebSocket closed: " + closeReason);
                }
            }, URI.create(getWsUrl()));

            String result = ret.get(5000);

            JsonObject obj = Json.parse(result).asObject();

            System.out.println("WS Response: " + obj);

            assertEquals("hello", obj.get("result").asString());

        } catch (Exception e) 
        {
            e.printStackTrace();
            //throw new RuntimeException(e); 
        } 
        finally 
        {
            if (session != null && session.isOpen()) 
            {
                try 
                {
                    session.close();
                } 
                catch (Exception e) 
                {
                    e.printStackTrace();
                }
            }
            if(provider!=null)
                provider.terminate();
        }
    }
    
    @Test
    public void testWebSocketService2() 
    {
        Assumptions.assumeTrue(RequestManagerFactory.getInstance(true).isSupported(PublishType.WS),
            "WebSocket not supported"
        );

        IComponentHandle provider = null;
        try
        {
            provider = ComponentManager.get().create(new Provider2()).get();

            ITestService ts = WsServiceProxy.create(getWsUrl(), ITestService.class);
        
            String hello = ts.getString().get();

            System.out.println("Received: "+hello);
            
            assertEquals("hello", hello);
        }
        finally
        {
            if(provider!=null)
                provider.terminate();
        }
    }

    @Test
    public void testWebSocketService3() 
    {
        Assumptions.assumeTrue(RequestManagerFactory.getInstance(true).isSupported(PublishType.WS),
            "WebSocket not supported"
        );

        IComponentHandle provider = null;
        try
        {
            provider = ComponentManager.get().create(new Provider2()).get();

            ITestService ts = WsServiceProxy.create(getWsUrl(), ITestService.class);
        
            Collection<String> res = ts.getStrings().get();
            List<String> exp = List.of("0", "1", "2");

            System.out.println("Received: "+res+" "+res.getClass());
            
            assertEquals(exp, res);
        }
        finally
        {
            if(provider!=null)
                provider.terminate();
        }
    }

    @Test
    public void testWebSocketTerminate() 
    {
        Assumptions.assumeTrue(RequestManagerFactory.getInstance(true).isSupported(PublishType.WS),
            "WebSocket not supported"
        );

        IComponentHandle provider = null;
        try
        {
            provider = ComponentManager.get().create(new Provider2(), "Provider2").get();

            ITestService ts = WsServiceProxy.create(getWsUrl(), ITestService.class);
        
            ISubscriptionIntermediateFuture<String> fut = ts.getStringsInfinite();
            fut.next(res -> System.out.println())
                .catchEx(ex -> ex.printStackTrace());   
            
            //SUtil.sleep(1000);

            fut.terminate();
            //fut.terminate(new RuntimeException("User terminated ex"));

            try 
            {
                fut.get();
            } 
            catch (Exception e) 
            {
                e.printStackTrace();
            }
            System.out.println("terminate finished");
            
            //List<String> exp = List.of("0", "1", "2");

            //System.out.println("Received: "+res+" "+res.getClass());
            
            //assertEquals(exp, res);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if(provider!=null)
                provider.terminate();

            //SUtil.sleep(1000);

            //System.out.println("remaining: "+ComponentManager.get().getNumberOfComponents()+" "+ComponentManager.get().getAllComponents());
        }
    }

    @Test
    public void testGetString() 
    {
        IComponentHandle provider = null;
        try
        {
            provider = ComponentManager.get().create(new Provider()).get();

            get(getRestUrl(provider) + "/getString").then().assertThat().statusCode(200);
        }
        finally
        {
            if(provider!=null)
                provider.terminate();
        }
    }

    @Test
    public void testGetString2() 
    {
        IComponentHandle provider = null;

        try
        {
            provider = ComponentManager.get().create(new Provider()).get();

            /*get(getRestUrl(provider) + "/getString2?id=123").then().assertThat()
                .statusCode(200)
                .body(equalTo("123!"));*/
                //.body(equalTo("\"123!\""));*/

            String body = get(getRestUrl(provider) + "/getString2?id=123")
                .then()
                .statusCode(200)
                .extract()
                .asString();

            System.out.println("BODY=[" + body + "]");
            System.out.println("LENGTH=" + body.length());
        }
        finally
        {
            if(provider!=null)
                provider.terminate();
        }
    }

    @Test
    public void testSetStringWrong() 
    {
        IComponentHandle provider = null;

        try
        {
            provider = ComponentManager.get().create(new Provider()).get();

            get(getRestUrl(provider) + "/setString?in=abc").then().assertThat().statusCode(400);

            //int status = get(baseUrl + "/setString?in=abc")
            //    .getStatusCode();

            //System.out.println("Status: " + status);
            //System.out.println("Test finished.");
        }
        finally
        {
            if(provider!=null)
                provider.terminate();
        }
    }

    @Test
    public void testSetStringOk() 
    {
        IComponentHandle provider = null;

        try
        {
            provider = ComponentManager.get().create(new Provider()).get();

            put(getRestUrl(provider) + "/setString?in=abc").then().assertThat().statusCode(200);
        }
        finally
        {
            if(provider!=null)
                provider.terminate();
        }
    }

    @Test
    public void testRemoveString() 
    {
        IComponentHandle provider = null;

        try
        {
            provider = ComponentManager.get().create(new Provider()).get();

            delete(getRestUrl(provider) + "/removeString?in=abc").then().assertThat().statusCode(200);
        }
        finally
        {
            if(provider!=null)
                provider.terminate();
        }
    }

    // todo: test parameter mapping

    @Test
    public void testParameterMapping() 
    {
        IComponentHandle provider = null;

        try
        {
            provider = ComponentManager.get().create(new Provider()).get();

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
                .post(getRestUrl(provider) + "/paramMapping")
            .then()
                .statusCode(200)
                .extract()
                .as(Customer.class);

            assertEquals(c, resc);
        }
        finally
        {
            if(provider!=null)
                provider.terminate();
        }
    }

    @Test
    public void testParameterMapping2() 
    {
        IComponentHandle provider = null;

        try
        {
            provider = ComponentManager.get().create(new Provider()).get();

            Customer c = new Customer("John Doe", 30, "123 Main St");
            
            given()
                .body(c)
                .contentType("application/json")
            .when()
                .post(getRestUrl(provider) + "/paramMapping2")
            .then()
                .statusCode(200);
        }
        finally
        {
            if(provider!=null)
                provider.terminate();
        }
    }

    @Test
    public void testParameterMapping3() 
    {
        IComponentHandle provider = null;

        try
        {
            provider = ComponentManager.get().create(new Provider()).get();

            given().contentType("application/json")
                .when().post(getRestUrl(provider) + "/paramMapping2/?name=John%20Doe&age=30")
                .then().statusCode(200);
        }
        finally
        {
            if(provider!=null)
                provider.terminate();
        }
    }

    @Test
    public void testResultMapping() 
    {
        IComponentHandle provider = null;

        try
        {
            provider = ComponentManager.get().create(new Provider()).get();

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
                .post(getRestUrl(provider) + "/resultMapping/?name=John%20Doe&age=30")
            .then()
                .statusCode(200)
                .extract()
                .as(Customer.class);

            assertEquals(c, resc);
        }
        finally
        {
            if(provider!=null)
                provider.terminate();
        }
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

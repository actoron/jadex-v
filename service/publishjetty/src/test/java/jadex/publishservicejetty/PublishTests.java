package jadex.publishservicejetty;


import jadex.publishservice.PublishTestsBase;
import jadex.publishservice.impl.RequestManagerFactory;
import jadex.publishservice.impl.v2.RequestManager2;

public class PublishTests extends PublishTestsBase
{
    /*@Test
    void dummy() 
    {
        System.out.println("Dummy test");
    }*/

    public static void main(String[] args) 
    {
        RequestManagerFactory.setRequestManagerClass(RequestManager2.class);

        System.setProperty("host", "localhost");
        System.setProperty("port", "8080");
        
        //RestAssured.defaultParser = Parser.JSON;
        PublishTests tests = new PublishTests();

        //tests.testParameterMapping();
        //tests.runAll();

        tests.testSetStringWrong();
        
        /*tests.testWebSocketService();
        tests.testWebSocketService2();
        tests.testWebSocketService3();
        tests.testWebSocketTerminate();*/

        //IComponentHandle provider = ComponentManager.get().create(new Provider2()).get();
        //IComponentManager.get().waitForLastComponentTerminated();

        //tests.testWebSocketTerminate();

        /*Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
        for (Thread t : threads.keySet()) {
            System.out.println(t.getName() + " [" + t.getState() + "] daemon=" + t.isDaemon());
            for (StackTraceElement s : threads.get(t)) {
                System.out.println("    " + s);
            }
            System.out.println();
        }*/
    }

    
}

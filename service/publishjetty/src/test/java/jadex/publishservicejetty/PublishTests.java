package jadex.publishservicejetty;

import org.junit.jupiter.api.Test;

import jadex.publishservice.PublishTestsBase;

public class PublishTests extends PublishTestsBase
{
    /*@Test
    void dummy() 
    {
        System.out.println("Dummy test");
        // leer – sorgt dafür, dass Runner die Klasse erkennt
    }*/

    public static void main(String[] args) 
    {
        System.setProperty("host", "localhost");
        System.setProperty("port", "8080");
        
        PublishTests tests = new PublishTests();
        
        //tests.testGetString();
        //tests.testSetStringWrong();
        //tests.testParameterMapping3();
        tests.testResultMapping();
    }
}

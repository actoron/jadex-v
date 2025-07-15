package jadex.llm.workflow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;
import jadex.publishservice.IPublishServiceFeature;
import jadex.publishservice.publish.annotation.Publish;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Publish(publishid="http://localhost:9002/", publishtarget = ISensorAgent.class)
public class SensorAgent implements ISensorAgent {
    @Inject
    protected IComponent agent;

    @OnStart
    public void onStart() {
        System.out.println("agent started: " + agent.getId().getLocalName());

        IPublishServiceFeature ps = agent.getFeature(IPublishServiceFeature.class);
        //ps.publishResources("http://localhost:5000/", "jadex/bdi-llm/workflow");
    }


    public static void main(String[] args) {
        IComponentManager.get().create(new SensorAgent()).get();
        IComponentManager.get().waitForLastComponentTerminated();
    }

    @Override
    public IFuture<Void> deploy(HashMap<String, Object> info)
    {
        Map<String, Object> projectmap = (Map<String, Object>) info.get("project");
        System.out.println("projectmap: " + projectmap);

        info.entrySet().forEach(e -> System.out.println(e.getKey() + " = " + e.getValue()));
        System.out.println("agent: " + agent.getId().getLocalName());
        Object projectRaw = info.get("project");
        System.out.println("Project: " + projectRaw);

        String nameSpace = (String) info.get("namespace");
        System.out.println("Namespace: " + nameSpace);
//        printMapRecursive(info, "");
        return IFuture.DONE;
    }

//    public void printMapRecursive(HashMap<String, Object> map, String s)
//    {
//        for (HashMap.Entry<String, Object> entry : map.entrySet()) {
//            String key = s.isEmpty() ? entry.getKey() : s + "." + entry.getKey();
//            Object value = entry.getValue();
//
//            if (value instanceof HashMap<?, ?> nestedMap) {
//                printMapRecursive((HashMap<String, Object>) nestedMap, key);
//            } else if (value instanceof List<?> list) {
//                printListRecursive((List<Object>) list, key);
//            } else {
//                System.out.println(key + " = " + value + " (" + (value != null ? value.getClass().getSimpleName() : "null") + ")");
//            }
//        }
//    }
//
//    public void printListRecursive(List<Object> list, String s)
//    {
//        for (int i = 0; i < list.size(); i++) {
//            Object item = list.get(i);
//            String key = s + "[" + i + "]";
//
//            if (item instanceof HashMap<?, ?> nestedMap) {
//                printMapRecursive((HashMap<String, Object>) nestedMap, key);
//            } else if (item instanceof List<?> sublist) {
//                printListRecursive((List<Object>) sublist, key);
//            } else {
//                System.out.println(key + " = " + item);
//            }
//        }
//    }
}
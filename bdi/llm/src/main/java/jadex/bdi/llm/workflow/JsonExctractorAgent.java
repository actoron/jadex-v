package jadex.bdi.llm.workflow;

import jadex.core.IComponent;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;
import jadex.micro.annotation.Agent;
import jadex.providedservice.impl.search.ServiceQuery;
import jadex.requiredservice.IRequiredServiceFeature;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class JsonExctractorAgent<T> {

    @Inject
    private IComponent component;

    private String sensortag;

    private Class<?> resultclass;

    private List<JsonMapping> mappings;

    public JsonExctractorAgent(String sensortag, Class<?> resultclass, List<JsonMapping> mappings) {
        this.sensortag = sensortag;
        this.resultclass = resultclass;
        this.mappings = mappings;
    }

    @OnStart
    public void onStart() {
        IRequiredServiceFeature reqfeat = component.getFeature(IRequiredServiceFeature.class);
        ServiceQuery<IJsonEventProvider> query = new ServiceQuery<>(IJsonEventProvider.class);
        query.setServiceTags(new String[] { sensortag }, component).setMultiplicity(ServiceQuery.Multiplicity.ONE);
        reqfeat.addQuery(query).next((sensoragent) -> {
            System.out.println("sensoragent: " + sensoragent);
            sensoragent.subscribe().next(this::processPushEvent);
        });
    }

    public void processPushEvent(Map<String, Object> event)
    {
        System.out.println("event: " + event);
        Object result = null;
        try
        {
            Constructor<?> c = resultclass.getConstructor();
            result = c.newInstance();

            for (JsonMapping mapping : mappings)
            {
                String[] jsonpath = mapping.jsonsource().split("\\.");
                String[] objpath = mapping.javatarget().split("\\.");

                Object targetobject = result;
                for (int i = 0; i < objpath.length - 1; i++)
                {
                    String objname = objpath[i];
                    objname = objname.substring(0, 1).toUpperCase() + objname.substring(1);
                    Method subobjgetter = targetobject.getClass().getMethod("get" + objname);
                    Object subobj = subobjgetter.invoke(targetobject);
                    if (subobj == null)
                    {
                        Class<?> subobjclass = subobjgetter.getReturnType();
                        Constructor<?> subobjconstructor = subobjclass.getConstructor();
                        subobj = subobjconstructor.newInstance();
                        targetobject.getClass().getMethod("set" + objname, subobjclass).invoke(targetobject, subobj);
                    }
                    targetobject = subobj;
                }

                Object jsonvalue = event;
                for (int i = 0; i < jsonpath.length; i++)
                {
                    String jsonname = jsonpath[i];
                    jsonvalue = ((Map<String, Object>) jsonvalue).get(jsonname);
                }

                targetobject.getClass().getMethod("set" + objpath[objpath.length - 1], jsonvalue.getClass()).invoke(targetobject, jsonvalue);
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        System.out.println("result: " + result);
    }

    public record JsonMapping(String javatarget, String jsonsource) {};
}

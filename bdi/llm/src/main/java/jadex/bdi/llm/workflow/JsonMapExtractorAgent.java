package jadex.bdi.llm.workflow;

import jadex.common.SUtil;
import jadex.core.IComponent;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;
import jadex.providedservice.impl.search.ServiceQuery;
import jadex.requiredservice.IRequiredServiceFeature;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonMapExtractorAgent<T> {

    @Inject
    private IComponent component;

    private String sensortag;

    private List<JsonMapping> mappings;

    public JsonMapExtractorAgent(String sensortag, List<JsonMapping> mappings) {
        this.sensortag = sensortag;
        this.mappings = mappings;
    }

    @OnStart
    public void onStart() {
        IRequiredServiceFeature reqfeat = component.getFeature(IRequiredServiceFeature.class);
        ServiceQuery<IJsonEventProvider> query = new ServiceQuery<>(IJsonEventProvider.class);
        query.setServiceTags(new String[] { sensortag }, component).setMultiplicity(ServiceQuery.Multiplicity.ONE);
        reqfeat.addQuery(query).next((sensoragent) -> {
            System.out.println("extractoragent result: " + sensoragent);
            sensoragent.subscribe().next(this::processPushEvent);
        });
    }

    public void processPushEvent(Map<String, Object> event)
    {
        //System.out.println("event: " + event);
        Map<String, Object> result = new HashMap<>();
        try
        {
            for (JsonMapping mapping : mappings)
            {
                String[] jsonpath = mapping.jsonsource().split("\\.");
                String[] objpath = mapping.javatarget().split("\\.");

                Map<String, Object> targetobject = result;
                for (int i = 0; i < objpath.length - 1; i++)
                {
                    String objname = objpath[i];
                    Map<String, Object> subobj = (Map<String, Object>) targetobject.get(objname);
                    if (subobj == null)
                    {
                        subobj = new HashMap<>();
                        targetobject.put(objname, subobj);
                    }
                    targetobject = subobj;
                }

                Object jsonvalue = event;
                for (int i = 0; i < jsonpath.length; i++)
                {
                    String jsonname = jsonpath[i];
                    jsonvalue = ((Map<String, Object>) jsonvalue).get(jsonname);
                }

                String objname = objpath[objpath.length - 1];
                targetobject.put(objname, jsonvalue);
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        System.out.println("Map Result: " + result);
    }



    public record JsonMapping(String javatarget, String jsonsource) {};
}

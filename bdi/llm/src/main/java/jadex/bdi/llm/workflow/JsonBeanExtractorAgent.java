package jadex.bdi.llm.workflow;

import jadex.common.transformation.BeanIntrospectorFactory;
import jadex.common.transformation.traverser.BeanProperty;
import jadex.common.transformation.traverser.IBeanIntrospector;
import jadex.core.IComponent;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;
import jadex.providedservice.impl.search.ServiceQuery;
import jadex.requiredservice.IRequiredServiceFeature;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class JsonBeanExtractorAgent<T> {

    @Inject
    private IComponent component;

    private String sensortag;

    private Class<?> resultclass;

    private List<JsonMapping> mappings;

    public JsonBeanExtractorAgent(String sensortag, Class<?> resultclass, List<JsonMapping> mappings) {
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
            System.out.println("extractoragent result: " + sensoragent);
            sensoragent.subscribe().next(this::processPushEvent);
        });
    }

    public void processPushEvent(Map<String, Object> event)
    {
        System.out.println("event: " + event);
        Object result = null;
        try
        {
            IBeanIntrospector bi = BeanIntrospectorFactory.get().getBeanIntrospector();

            MethodHandle constructor = bi.getBeanConstructor(resultclass, true, false);
            Map<String, BeanProperty> props = bi.getBeanProperties(resultclass, true, false);

            result = constructor.invoke();

            for (JsonMapping mapping : mappings)
            {
                String[] jsonpath = mapping.jsonsource().split("\\.");
                String[] objpath = mapping.javatarget().split("\\.");

                Object targetobject = result;
                Map<String, BeanProperty> subprops = props;
                for (int i = 0; i < objpath.length - 1; i++)
                {
                    String objname = objpath[i];
                    BeanProperty prop = subprops.get(objname);
                    Method subobjgetter = prop.getGetter();
                    Object subobj = subobjgetter.invoke(targetobject);
                    if (subobj == null)
                    {
                        Class<?> subobjclass = subobjgetter.getReturnType();
                        MethodHandle subobjconstructor = bi.getBeanConstructor(subobjclass, true, false);
                        subobj = subobjconstructor.invoke();
                        prop.getSetter().invoke(targetobject, subobj);
                    }
                    targetobject = subobj;
                    subprops = bi.getBeanProperties(targetobject.getClass(), true, false);
                }

                Object jsonvalue = event;
                for (int i = 0; i < jsonpath.length; i++)
                {
                    String jsonname = jsonpath[i];
                    jsonvalue = ((Map<String, Object>) jsonvalue).get(jsonname);
                }

                String objname = objpath[objpath.length - 1];
                subprops.get(objname).getSetter().invoke(targetobject, jsonvalue);
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        System.out.println("Bean Result: " + result);
    }

    public record JsonMapping(String javatarget, String jsonsource) {};
}

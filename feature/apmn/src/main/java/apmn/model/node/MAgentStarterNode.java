package apmn.model.node;

import jadex.common.SUtil;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;

import java.lang.reflect.Constructor;

public class MAgentStarterNode extends MActorNode
{
    private Class<?> agentclass;

    public MAgentStarterNode(Class<?> agentclass)
    {
        this.agentclass=agentclass;
    }

    @Override
    public void execute() {
        try {
            System.out.println(agentclass.getClassLoader());
            System.out.println(Class.forName("Example",true, agentclass.getClassLoader()));
            Constructor<?> con = agentclass.getConstructor();
            System.out.println(con);
            Object pojo = con.newInstance();
            System.out.println("pojo: " + pojo.getClass());
            System.out.println(agentclass.getResource("/Example.class"));
            System.out.println(agentclass.getProtectionDomain().getCodeSource());
            System.out.println(System.getProperty("java.class.path"));
            IComponentHandle agent = IComponentManager.get().create(pojo).get();
            System.out.printf("agent" + agent);
            agent.waitForTermination().get();
        } catch (Exception e) {
            throw SUtil.throwUnchecked(e);
        }
    }
}

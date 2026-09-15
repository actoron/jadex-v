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
            Constructor<?> con = agentclass.getConstructor();
            Object pojo = con.newInstance();
            IComponentHandle agent = IComponentManager.get().create(pojo).get();
            agent.waitForTermination().get();
        } catch (Exception e) {
            throw SUtil.throwUnchecked(e);
        }
    }
}

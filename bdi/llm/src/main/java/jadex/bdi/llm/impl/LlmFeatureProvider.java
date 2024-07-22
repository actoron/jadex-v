package jadex.bdi.llm.impl;

import jadex.bdi.llm.ILlmFeature;
import jadex.bdi.runtime.impl.BDIAgent;
import jadex.core.impl.Component;
import jadex.core.impl.FeatureProvider;

public class LlmFeatureProvider extends FeatureProvider<ILlmFeature>
{
    public LlmFeatureProvider()
    {
        super();
        System.out.println("LlmFeatureProvider constructor");
    }

    @Override
    public Class<ILlmFeature> getFeatureType()
    {
        return ILlmFeature.class;
    }

    @Override
    public ILlmFeature createFeatureInstance(Component self) {
        return null;
    }

    public ILlmFeature createFeatureInstance(String chatgpt_url, String chatgpt_key, Class<?> agent_class, Class<?> feature_class)
    {
        return new LlmFeature(chatgpt_url, chatgpt_key, agent_class, feature_class);
    }

    @Override
    public Class <? extends Component> getRequiredComponentType()
    {
        return BDIAgent.class;
    }
}

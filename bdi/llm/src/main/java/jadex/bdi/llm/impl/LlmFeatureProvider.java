package jadex.bdi.llm.impl;

import jadex.bdi.llm.ILlmFeature;
import jadex.bdi.impl.BDIAgent;
import jadex.core.impl.Component;
import jadex.core.impl.FeatureProvider;

import java.net.URI;

public class LlmFeatureProvider extends FeatureProvider<ILlmFeature>
{
    public LlmFeatureProvider()
    {
        super();
    }

    @Override
    public Class<ILlmFeature> getFeatureType()
    {
        return ILlmFeature.class;
    }

    public ILlmFeature createFeatureInstance(Component self) {
        return null;
    }

    public ILlmFeature createFeatureInstance(URI apiAddress, String beliefType, String model)
    {
        return new LlmFeature(apiAddress, beliefType, model);
    }

    public Class <? extends Component> getRequiredComponentType()
    {
        return BDIAgent.class;
    }
}

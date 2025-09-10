package jadex.bdi.llm.workflow;

import jadex.model.modelinfo.ModelInfo;

public class MWorkflowModel
{
    //-------- constants --------

    public static final String SENSOR = "Sensor";

    public static final String TRANSFORMER = "Transformer";

    public static final String PROCESSOR = "Processor";

    public static final String QUERY = "Query";

    //-------- attributes --------

    //-------- init structures --------

    //-------- added structures --------

    //-------- model management --------

    /** The model info. */
    protected ModelInfo modelinfo;

    //-------- methods --------

    public MWorkflowModel()
    {
        this.modelinfo = new ModelInfo();
        modelinfo.internalSetRawModel(this);
    }
}

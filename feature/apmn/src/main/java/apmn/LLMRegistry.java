package apmn;

public class LLMRegistry
{
    public static LLMClient createJadexClient()
    {
        return new JadexClient("gemma4:12b-it-qat");
    }
}

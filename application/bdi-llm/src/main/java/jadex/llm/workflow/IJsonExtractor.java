package jadex.llm.workflow;

public interface IJsonExtractor<T> {

    public T extract(String json);
}

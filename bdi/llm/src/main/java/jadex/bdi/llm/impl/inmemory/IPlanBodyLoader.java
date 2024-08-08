package jadex.bdi.llm.impl.inmemory;

import static java.util.Objects.requireNonNull;

import java.util.Map;

public class IPlanBodyLoader extends ClassLoader {

    private final IPlanBodyFileManager manager;

    public IPlanBodyLoader(ClassLoader parent, IPlanBodyFileManager manager) {
        super(parent);
        this.manager = requireNonNull(manager, "manager must not be null");
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {

        Map<String, JavaClassAsBytes> compiledClasses = manager
                .getBytesMap();

        if (compiledClasses.containsKey(name)) {
            byte[] bytes = compiledClasses.get(name)
                    .getBytes();
            return defineClass(name, bytes, 0, bytes.length);
        } else {
            throw new ClassNotFoundException();
        }
    }
}
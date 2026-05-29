package jadex.publishservice.impl.v2.invocation;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.publishservice.impl.PublishServiceFeature;
import jadex.publishservice.impl.v2.Request;

public class ResourceInvocation extends Invocation
{
    protected String resource;
    
	protected List<String> roots = new ArrayList<>();
    
    public ResourceInvocation(Request request, String resource) 
    {
        super(request, null);
        this.resource = resource;
		this.roots.add(getPath());
    }

    public IFuture<InvocationResult> invoke()
    {
	    return loadResource(resource);
    }

    /**
	 *  Get the path to my package as path
	 *  @return The path.
	 */
	public static String getPath()
	{
		String ret = SReflect.getPackageName(PublishServiceFeature.class);
		ret = ret.replace('.', '/');
		if(!ret.endsWith("/"))
			ret = ret+"/";
		return ret;
	}

    protected IFuture<InvocationResult> loadResource(String name)
    {
        if(name.contains(".."))
            return new Future<>(new InvocationResult(new SecurityException("Invalid path")));

        for(String root : roots)
        {
            String full = normalizeRoot(root) + name;

            try(InputStream is = getClassLoader().getResourceAsStream(full))
            {
                if(is != null)
                {
                    byte[] data = SUtil.readStream(is);

                    String mime = URLConnection.guessContentTypeFromName(name);
                    if(mime == null)
                        mime = "application/octet-stream";

                    return new Future<>(new InvocationResult(data).addMetaInfo("content-type", mime));
                }
            }
            catch(Exception e)
            {
                return new Future<>(new InvocationResult(e));
            }
        }

        return new Future<>(new InvocationResult(new FileNotFoundException(name)));
    }

    protected String normalizeRoot(String root)
    {
        if(root.startsWith("/"))
            root = root.substring(1);
        if(!root.endsWith("/"))
            root += "/";
        return root;
    }
    
}

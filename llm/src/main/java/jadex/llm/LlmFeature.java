package jadex.llm;

import java.util.Collection;

import jadex.future.IFuture;
import jadex.requiredservice.IRequiredServiceFeature;
import jadex.execution.LambdaAgent;
import jadex.future.Future;

public class LlmFeature implements ILlmFeature
{
    public IFuture<String> handle(String input)
    {
        Future<String> ret = new Future<>();

        LambdaAgent.create(agent -> 
        {
            IMcpHostService mcp = agent.getFeature(IRequiredServiceFeature.class).getLocalService(IMcpHostService.class);
            if(mcp==null)
            {
                ret.setException(new RuntimeException("No MCP Host service available"));
                return;
            }

            IFuture<String> res = mcp.handle(input);
            res.then(answer -> 
            {
                agent.terminate();
            }).catchEx(e -> 
            {
                agent.terminate();
            });

            res.delegateTo(ret);
        });

        /*LambdaAgent.run(agent -> 
        {
            IMcpHostService mcp = agent.getFeature(IRequiredServiceFeature.class).getLocalService(IMcpHostService.class);
            if(mcp==null)
            {
                ret.setException(new RuntimeException("No MCP Host service available"));
                return;
            }

            mcp.handle(input).delegateTo(ret);
        });*/

        return ret;
    }

    @Override
    public IFuture<Collection<ToolSchema>> listTools()
    {
        Future<Collection<ToolSchema>> ret = new Future<>();

        LambdaAgent.create(agent -> 
        {
            IMcpClientService mcp = agent.getFeature(IRequiredServiceFeature.class).getLocalService(IMcpClientService.class);
            if(mcp==null)
            {
                ret.setException(new RuntimeException("No MCP Client service available"));
                return;
            }

            IFuture<Collection<ToolSchema>> res = mcp.listTools();
            res.then(answer -> 
            {
                agent.terminate();
            }).catchEx(e -> 
            {
                agent.terminate();
            });

            res.delegateTo(ret);
        });


        /*LambdaAgent.run(agent -> 
        {
            IMcpClientService mcp = agent.getFeature(IRequiredServiceFeature.class).getLocalService(IMcpClientService.class);
            if(mcp==null)
            {
                ret.setException(new RuntimeException("No MCP Client service available"));
                return;
            }

            mcp.listTools().delegateTo(ret);
        });*/

        return ret;
    }

    @Override
    public IFuture<Void> addTool(ToolSchema tool)
    {
        Future<Void> ret = new Future<>();

        LambdaAgent.create(agent -> 
        {
            IMcpClientService mcp = agent.getFeature(IRequiredServiceFeature.class).getLocalService(IMcpClientService.class);
            if(mcp==null)
            {
                ret.setException(new RuntimeException("No MCP Client service available"));
                return;
            }

            IFuture<Void> res = mcp.addTool(tool);
            res.then(answer -> 
            {
                agent.terminate();
            }).catchEx(e -> 
            {
                agent.terminate();
            });

            res.delegateTo(ret);
        });

        /*LambdaAgent.run(agent -> 
        {
            IMcpClientService mcp = agent.getFeature(IRequiredServiceFeature.class).getLocalService(IMcpClientService.class);
            if(mcp==null)
            {
                ret.setException(new RuntimeException("No MCP Client service available"));
                return;
            }

            mcp.addTool(tool).delegateTo(ret);
        });*/

        return ret;
    }

    @Override
    public IFuture<Boolean> removeTool(String toolname)
    {
        Future<Boolean> ret = new Future<>();

        LambdaAgent.create(agent -> 
        {
            IMcpClientService mcp = agent.getFeature(IRequiredServiceFeature.class).getLocalService(IMcpClientService.class);
            if(mcp==null)
            {
                ret.setException(new RuntimeException("No MCP Client service available"));
                return;
            }

            IFuture<Boolean> res = mcp.removeTool(toolname);
            res.then(answer -> 
            {
                agent.terminate();
            }).catchEx(e -> 
            {
                agent.terminate();
            });

            res.delegateTo(ret);
        });

        /*LambdaAgent.run(agent -> 
        {
            IMcpClientService mcp = agent.getFeature(IRequiredServiceFeature.class).getLocalService(IMcpClientService.class);
            if(mcp==null)
            {
                ret.setException(new RuntimeException("No MCP Client service available"));
                return;
            }

            mcp.removeTool(toolname).delegateTo(ret);
        });*/

        return ret;
    }

    @Override
    public IFuture<ToolSchema> getTool(String name)
    {
        Future<ToolSchema> ret = new Future<>();

         LambdaAgent.create(agent -> 
        {
            IMcpClientService mcp = agent.getFeature(IRequiredServiceFeature.class).getLocalService(IMcpClientService.class);
            if(mcp==null)
            {
                ret.setException(new RuntimeException("No MCP Client service available"));
                return;
            }

            IFuture<ToolSchema> res = mcp.getTool(name);
            res.then(answer -> 
            {
                agent.terminate();
            }).catchEx(e -> 
            {
                agent.terminate();
            });

            res.delegateTo(ret);
        });

        /*LambdaAgent.run(agent -> 
        {
            IMcpClientService mcp = agent.getFeature(IRequiredServiceFeature.class).getLocalService(IMcpClientService.class);
            if(mcp==null)
            {
                ret.setException(new RuntimeException("No MCP Client service available"));
                return;
            }

            mcp.getTool(name).delegateTo(ret);
        });*/

        return ret;
    }

    @Override
    public IFuture<String> invokeTool(String toolname, String argsstr) 
    {
        Future<String> ret = new Future<>();

        LambdaAgent.create(agent -> 
        {
            IMcpClientService mcp = agent.getFeature(IRequiredServiceFeature.class).getLocalService(IMcpClientService.class);
            if(mcp==null)
            {
                ret.setException(new RuntimeException("No MCP Client service available"));
                return;
            }

            IFuture<String> res = mcp.invokeTool(toolname, argsstr);
            res.then(answer -> 
            {
                agent.terminate();
            }).catchEx(e -> 
            {
                agent.terminate();
            });

            res.delegateTo(ret);
        });

        /*LambdaAgent.run(agent -> 
        {
            IMcpClientService mcp = agent.getFeature(IRequiredServiceFeature.class).getLocalService(IMcpClientService.class);
            if(mcp==null)
            {
                ret.setException(new RuntimeException("No MCP Client service available"));
                return;
            }

            mcp.invokeTool(toolname, argsstr).delegateTo(ret);
        });*/

        return ret;
    }
}

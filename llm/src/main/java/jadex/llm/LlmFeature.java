package jadex.llm;

import java.util.Collection;
import java.util.Map;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.core.IThrowingConsumer;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.requiredservice.IRequiredServiceFeature;

public class LlmFeature implements ILlmFeature
{
    public IFuture<String> handleInput(String input)
    {
        // todo!!!

        Future<String> ret = new Future<>();

        IComponentManager.get().create((IThrowingConsumer<IComponent>)agent -> 
        {
            Collection<ILlmService> services = agent.getFeature(IRequiredServiceFeature.class).getLocalServices(ILlmService.class);
            if(services.isEmpty())
            {
                ret.setException(new RuntimeException("No LLM services available"));
                agent.terminate();
                return;
            }

            // select small llm 

            // ask llm if it is a question or a command

            // delegate to question or command handler

            agent.terminate();
        });

        return ret;
    }

    public IFuture<String> handleChatQuestion(String input)
    {
        Future<String> ret = new Future<>();

        IComponentManager.get().create((IThrowingConsumer<IComponent>)agent -> 
        {
            ILlmService llm = agent.getFeature(IRequiredServiceFeature.class)
                .getLocalService(ILlmService.class);
            if (llm == null) 
            {
                ret.setException(new RuntimeException("No LLM service available"));
                return;
            }

            IFuture<String> res = llm.callLlm(input);
            res.then(answer -> 
            {
                agent.terminate();
            }).catchEx(e -> 
            {
                agent.terminate();
            });
            res.delegateTo(ret);
        });

        return ret;
    }

    public IFuture<String> handleToolCall(String input)
    {
        Future<String> ret = new Future<>();

        IComponentManager.get().create((IThrowingConsumer<IComponent>)agent -> 
        {
            IMcpHostService mcp = agent.getFeature(IRequiredServiceFeature.class).getLocalService(IMcpHostService.class);
            if(mcp==null)
            {
                ret.setException(new RuntimeException("No MCP Host service available"));
                return;
            }

            IFuture<String> res = mcp.handleToolCall(input);
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
    public IFuture<Collection<McpToolSchema>> listTools()
    {
        Future<Collection<McpToolSchema>> ret = new Future<>();

        IComponentManager.get().create((IThrowingConsumer<IComponent>)agent -> 
        {
            IMcpClientService mcp = agent.getFeature(IRequiredServiceFeature.class).getLocalService(IMcpClientService.class);
            if(mcp==null)
            {
                ret.setException(new RuntimeException("No MCP Client service available"));
                return;
            }

            IFuture<Collection<McpToolSchema>> res = mcp.listTools();
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
    public IFuture<Void> addTool(McpToolSchema tool)
    {
        Future<Void> ret = new Future<>();

        IComponentManager.get().create((IThrowingConsumer<IComponent>)agent -> 
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

        IComponentManager.get().create((IThrowingConsumer<IComponent>)agent -> 
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
    public IFuture<McpToolSchema> getTool(String name)
    {
        Future<McpToolSchema> ret = new Future<>();

        IComponentManager.get().create((IThrowingConsumer<IComponent>)agent -> 
        {
            IMcpClientService mcp = agent.getFeature(IRequiredServiceFeature.class).getLocalService(IMcpClientService.class);
            if(mcp==null)
            {
                ret.setException(new RuntimeException("No MCP Client service available"));
                return;
            }

            IFuture<McpToolSchema> res = mcp.getTool(name);
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
    public IFuture<McpToolResult> invokeTool(String toolname, Map<String, Object> args) 
    {
        Future<McpToolResult> ret = new Future<>();

        IComponentManager.get().create((IThrowingConsumer<IComponent>)agent -> 
        {
            IMcpClientService mcp = agent.getFeature(IRequiredServiceFeature.class).getLocalService(IMcpClientService.class);
            if(mcp==null)
            {
                ret.setException(new RuntimeException("No MCP Client service available"));
                return;
            }

            IFuture<McpToolResult> res = mcp.invokeTool(toolname, args);
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

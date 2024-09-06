package jadex.rules.eca;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 *  The matcher node is the base class for event based rule matching.
 *  
 *  The task is to deliver the set of rules that needs to be checked
 *  against the event.
 */
public class MatcherNode 
{
    private final TrieNode root = new TrieNode("");

    public List<IRule<?>> getRules(String type) 
    {
        return root.collectRules(new EventType(type).getTypes());
    }
    
    public List<IRule<?>> getRules(EventType type) 
    {
    	//if("goaloption : jadex.bdi.plan.PlanTriggerTest$PlanTriggerTestAgent$MyGoal".equals(type.toString()))
    	//	System.out.println("check");
    	
    	List<IRule<?>> ret = root.collectRules(type.getTypes());
    	//System.out.println("found rules: "+type+" :"+ret);
    	return ret;
    }
    
    public void addRule(IRule<?> rule) 
    {
        for(EventType type : rule.getEvents()) 
            root.addRule(type, rule);
    }
    
    public void removeRule(IRule<?> rule)
    {
        for(EventType type : rule.getEvents()) 
            root.removeRule(type, rule);
    }
    
    public String toString()
    {
        return "MatcherNode(root=" + root.toString() + ")";
    }

    public class TrieNode 
    {
        private Map<String, TrieNode> children = new HashMap<>();
        
        private List<IRule<?>> rules = new ArrayList<>();
        
        private String type;
        
        public TrieNode(String type)
        {
        	this.type = type;
        }
        
        public Map<String, TrieNode> getChildren() 
        {
			return children;
		}

		public List<IRule<?>> getRules() 
		{
			return rules;
		}

		public String getType() 
		{
			return type;
		}

		public void addRule(EventType eventType, IRule<?> rule) 
        {
            TrieNode node = this;
            String[] types = eventType.getTypes();
            
            for(int i = 0; i < types.length; i++) 
            {
                String type = types[i];
                node = node.getOrCreateChild(type);
            }
            
            node.addRule(rule);
        }
        
        public void removeRule(EventType eventType, IRule<?> rule) 
        {
            TrieNode node = this;
            String[] types = eventType.getTypes();
            
            for(int i = 0; i < types.length; i++) 
            {
                String type = types[i];
                node = node.getChild(type);
                
                if(node == null) 
                    return; // Rule not found
            }
            
            // Remove the rule from the final node that represents the full event type
            node.removeRuleFromCurrentLevel(rule);
        }
        
        public List<IRule<?>> collectRules(String[] types) 
        {
            Set<IRule<?>> ret = new LinkedHashSet<>();
            TrieNode node = this;
            String wildcard = null;
            
            // Traverse the trie according to the event types
            for(int i = 0; i < types.length && node!=null; i++) 
            {
                String type = types[i];
                
                // Check and collect wildcard rules at each level
                if(node.getChild("*") != null) 
                {
                    ret.addAll(node.getChild("*").getRules());
                }
                
                if(type.equals("*") || type.equals("**")) 
                {
                    wildcard = type;
                    break; // Stop at wildcard, collect rules from current node
                }
                
                node = node.getChild(type);
            }
            
            if("*".equals(wildcard))
            {
            	// collect rules of this node
            	ret.addAll(node.getRules());
            	
            	// collect all direct children
            	for(TrieNode child: node.getChildren().values())
            		ret.addAll(child.getRules());
            }
            else if("**".equals(wildcard)) 
            {
                collectWildcardRules(node, ret);
            }
            else if(node != null) // add sibling rules
            {
                ret.addAll(node.rules);
            }
            
            return new ArrayList<>(ret);
        }


        private void collectWildcardRules(TrieNode node, Set<IRule<?>> rules) 
        {
            //TrieNode curnode = node;
            
            // Collect rules for the current node
            //rules.addAll(curnode.rules);
            for(TrieNode child: getChildren().values())
        		rules.addAll(child.getRules());
            
            // Recursively collect from child nodes
            for(TrieNode child : node.getChildren().values()) 
                collectWildcardRules(child, rules);
        }

        private void addRule(IRule<?> rule)
        {
        	rules.add(rule);
        }
        
        private void removeRuleFromCurrentLevel(IRule<?> rule) 
        {
            rules.remove(rule);
        }
        
        private TrieNode getOrCreateChild(String type) 
        {
            return children.computeIfAbsent(type, k -> new TrieNode(type));
        }
        
        private TrieNode getChild(String type) 
        {
            return children.get(type);
        }
        
        @Override
        public String toString() 
        {
            StringBuilder sb = new StringBuilder();
            toString(sb, "", true);
            return sb.toString();
        }
        
        private void toString(StringBuilder sb, String prefix, boolean isTail) 
        {
            sb.append(prefix).append(isTail ? "`- " : "|- ")
              .append("Node(type="+type+" rules=").append(rules).append(")\n");
            List<String> keys = new ArrayList<>(children.keySet());
            for(int i = 0; i < keys.size(); i++) 
            {
                String key = keys.get(i);
                TrieNode child = children.get(key);
                child.toString(sb, prefix + (isTail ? "   " : "|  "), i == keys.size() - 1);
            }
        }
    }
    
    public static void main(String[] args) 
    {
        MatcherNode node = new MatcherNode();
        Rule<?> r1 = new Rule<Object>("a:b:c:d", null, null, new EventType[]{new EventType("a:b:c:d")});
		node.addRule(r1);
		node.addRule(new Rule<Object>("a:b", null, null, new EventType[]{new EventType("a:b")}));
		node.addRule(new Rule<Object>("a:b2", null, null, new EventType[]{new EventType("a:b2")}));
		node.addRule(new Rule<Object>("a:b:c2", null, null, new EventType[]{new EventType("a:b:c2")}));
		node.addRule(new Rule<Object>("a:b2", null, null, new EventType[]{new EventType("a:b2")}));
		node.addRule(new Rule<Object>("a:b2:c", null, null, new EventType[]{new EventType("a:b2:c")}));
		node.addRule(new Rule<Object>("a:b:c:d2", null, null, new EventType[]{new EventType("a:b:c:d")}));
		node.addRule(new Rule<Object>("a2", null, null, new EventType[]{new EventType("a2")}));
		node.addRule(new Rule<Object>("*", null, null, new EventType[]{new EventType("*")}));
		node.addRule(new Rule<Object>("a:b:*", null, null, new EventType[]{new EventType("a:b:*")}));
		node.addRule(new Rule<Object>("a:*", null, null, new EventType[]{new EventType("a:*")}));

        System.out.println(node);

        System.out.println("a:*: " + node.getRules("a:*"));
        System.out.println("a:b:*: " + node.getRules("a:b:*"));
        System.out.println("a:b:c:* " + node.getRules("a:b:c:*"));
        
        node.removeRule(r1);

        System.out.println("After removing rule 'a:b:c:d':");
        System.out.println("a:*: " + node.getRules("a:*"));
        System.out.println("a:b:*: " + node.getRules("a:b:*"));
        System.out.println("a:b:c:* " + node.getRules("a:b:c:*"));
    }
}

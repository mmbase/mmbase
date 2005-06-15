package org.mmbase.util.functions;

import java.util.*;
import java.util.regex.*;

import org.mmbase.bridge.*;
import org.mmbase.bridge.util.*;
import org.mmbase.util.transformers.RomanTransformer;
import org.mmbase.module.core.*;
import org.mmbase.storage.search.*;
import org.mmbase.util.logging.*;

/**
 * The index node functions can be assigned to nodes which are connected by an 'index' relation. An
 * index relation is an extension of 'posrel', but also has an 'index' and a 'root' field. These are
 * used to calcaluate the 'number' of the connected nodes. The 'pos' field only serves to fix the order.
 *
 * The index field can be empty in which case the node with the lowest pos gets number 1, the
 * following number 2 and so on. But on any one of the index relations the index can be states
 * explicitely. If e.g. the index is specified 'a' then the following node will be 'b'. You can also
 * arrange for 'i', 'ii', 'iii', 'iv' and so on.
 *
 * The root field (a node-type fiulds) specifies to which tree the relations belong. So principaly
 * the same 'chapter' can exists with several differnent chapter numbers. Also, it is used to define
 * where counting must starts, because the complete number of a chapter consists of a chain of
 * numbers (like 2.3.4.iii).
 *
 *
 * @author Michiel Meeuwissen
 * @version $Id: IndexFunction.java,v 1.1 2005-06-15 06:42:53 michiel Exp $
 * @since MMBase-1.8
 */
public class IndexFunction extends FunctionProvider {
    private static final Logger log = Logging.getLoggerInstance(IndexFunction.class);
    


    /** 
     * Returns the 'successor' or a string. Which means that e.g. after 'zzz' follows 'aaaa'.
     */
    public static String successor(String index) {
        StringBuffer buf = new StringBuffer(index);
        boolean lowercase = true;
        for (int i = index.length() - 1 ; i >= 0; i--) {
            char c = buf.charAt(i);
            if (c >= 'a' && c <= 'y') {
                buf.setCharAt(i, (char) (c + 1));
                return buf.toString();
            } else if (c == 'z') {
                buf.setCharAt(i, 'a');
                continue;
            } else if (c >= 'A' && c <= 'Y') {
                buf.setCharAt(i, (char) (c + 1));
                return buf.toString();
            } else if (c == 'Z') {
                lowercase = false;
                buf.setCharAt(i, 'A');
                continue;
            } else if ((int) c < 128) {
                buf.setCharAt(i, (char) (c + 1));
                return buf.toString();
            } else {
                buf.setCharAt(i, (char) 65);
                continue;
            }
        }
        
        if (lowercase) {
            buf.insert(0, 'a');
        } else {
            buf.insert(0, 'A');
        }
        return buf.toString();
    }


    /**
     * Calculates the 'successor' of a roman number, preserving uppercase/lowercase.
     */
    protected static String romanSuccessor(String index) {
        boolean uppercase = index.length() > 0 && Character.isUpperCase(index.charAt(0));
        String res = RomanTransformer.decimalToRoman(RomanTransformer.romanToDecimal(index) + 1);
        return uppercase ? res.toUpperCase() : res;
        
    }
    /**
     * Calculates the 'successor' of an index String. Like '7.4.iii' of which the successor is
     * '7.4.iv'.
     *
     * @param index The string to succeed
     * @param seperator Regular expression to split up the string first (e.g. "\\.")
     * @param joiner    String to rejoin it again (e.g. ".")
     * @param roman     Whether to consider roman numbers
     */
    protected static String successor(String index, String separator, String joiner, boolean roman) {
        String[] split = index.split(separator);
        String postfix = split[split.length - 1];
        if (RomanTransformer.NUMERIC.matcher(postfix).matches()) {
            postfix = "" + (Integer.parseInt(postfix) + 1);
        } else {
            if (! roman || ! RomanTransformer.ROMAN.matcher(postfix).matches()) {
                postfix = successor(postfix);
            } else {
                postfix = romanSuccessor(postfix);
            }
        }
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < split.length - 1; i++) {
            buf.append(split[i]);
            buf.append(joiner);
        }
        buf.append(postfix);
        return buf.toString();
    }

    private static Parameter[] ARGS = new Parameter[] {
        Parameter.CLOUD,
        new Parameter("root", Node.class, false),
        new Parameter("separator", String.class, "\\."),
        new Parameter("joiner", String.class, "."),
        new Parameter("roman", Boolean.class, Boolean.TRUE),
        new Parameter("role", String.class, "index")
    };


    protected static MMObjectBuilder.NodeFunction index = new MMObjectBuilder.NodeFunction("index", ARGS, ReturnType.STRING) {
            
            {
                setDescription("Calculates the index of a node, using the surrounding 'indexrels'");
            }
            
            protected Object getFunctionValue(final MMObjectNode coreNode, final Parameters parameters) {
                final Cloud cloud   = (Cloud)  parameters.get(Parameter.CLOUD);
                final Node node     = cloud.getNode(coreNode.getNumber());
                return getFunctionValue(node, parameters);
                
            }
            /**
             * complete bridge version of {@link #getFunctionValue}
             */
            public Object getFunctionValue(final Node node, final Parameters parameters) {
                Node root     = (Node)   parameters.get("root");        
                final String role   = (String) parameters.get("role");
                final String join   = (String) parameters.get("joiner");
                final String separator   = (String) parameters.get("separator");
                final Pattern indexPattern = Pattern.compile("(.+)" + separator + "(.+)");
                final boolean roman   = ((Boolean) parameters.get("roman")).booleanValue();
                final NodeManager nm = node.getNodeManager();
                
                
                // now we have to determine the path from node to root.        
                GrowingTreeList tree = new GrowingTreeList(Queries.createNodeQuery(node), 10, nm, role, "source");
                NodeQuery template = tree.getTemplate();
                if (root != null) {
                    StepField sf = template.addField(role + ".root");
                    template.setConstraint(template.createConstraint(sf, root));
                    
                }
                
                List stack = new ArrayList();
                TreeIterator it = tree.treeIterator();
                int depth = it.currentDepth();
                while (it.hasNext()) {
                    Node n = it.nextNode();
                    if (it.currentDepth() > depth) {
                        stack.add(0, n);
                        depth = it.currentDepth();
                    }
                    if (root == null) root = n.getNodeValue(role + ".root");
                    if (root != null && n.getNumber() == root.getNumber()) break;
                }
                
                if (stack.isEmpty()) return "???";
                StringBuffer buf = new StringBuffer();
                Node n = (Node) stack.remove(0);
                String j = "";
                OUTER:
                while(! stack.isEmpty()) {
                    Node search = (Node) stack.remove(0);
                    NodeQuery q = Queries.createRelatedNodesQuery(n, nm, role, "destination");
                    StepField sf = q.addField(role + ".pos");
                    q.addSortOrder(sf, SortOrder.ORDER_ASCENDING);
                    q.addField(role + ".index");
                    String index = null;
                    NodeIterator ni = q.getCloud().getList(q).nodeIterator();
                    boolean doRoman = roman;
                    while(ni.hasNext()) {
                        Node clusterFound = ni.nextNode();
                        Node found = clusterFound.getNodeValue(q.getNodeStep().getAlias());
                        String i = clusterFound.getStringValue(role + ".index");
                        if (i == null || i.equals("")) i = index;
                        if (i == null) i = "1";
                        Matcher matcher = indexPattern.matcher(i);
                        if (matcher.matches()) {
                            buf = new StringBuffer(matcher.group(1));
                            i = matcher.group(2);
                            doRoman = roman && RomanTransformer.ROMAN.matcher(i).matches();
                        }
                        
                        if (found.getNumber() == search.getNumber()) {
                            // found!
                            buf.append(j).append(i);
                            j = join;
                            n = found;
                            continue OUTER;
                        }               
                        index = successor(i, separator, join, doRoman);
                    }
                    // not found
                    buf.append(j).append("???");
                    break;
                }
                
                return buf.toString();
            }
        };
    {
        addFunction(index);
    }


    public static void main(String argv[]) {

        CloudContext cc = ContextProvider.getDefaultCloudContext();
        Cloud cloud = cc.getCloud("mmbase", "class", null);
        Node node = cloud.getNode(argv[0]);
        Node root = null;
        if (argv.length > 1) root = cloud.getNode(argv[1]);
        Parameters params = index.createParameters();
        params.set("root", root);
        params.set("roman", Boolean.TRUE);
        System.out.println("" + index.getFunctionValue(node, params));
        
        
        
    }    

}

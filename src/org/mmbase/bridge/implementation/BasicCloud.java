/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge.implementation;
import org.mmbase.bridge.*;
import org.mmbase.security.*;
import org.mmbase.module.core.*;
import org.mmbase.module.corebuilders.TypeDef;
import org.mmbase.util.StringTagger;
import org.mmbase.util.logging.*;
import java.util.*;

/**
 *
 * @author Rob Vermeulen
 * @author Pierre van Rooden
 */
public class BasicCloud implements Cloud, Cloneable {
    private static Logger log = Logging.getLoggerInstance(BasicCloud.class.getName());

    // link to cloud context
    private BasicCloudContext cloudContext = null;

    // link to typedef object for retrieving type info (builders, etc)
    private TypeDef typedef = null;

    // name of the cloud
    protected String name = null;

    // account of the current user (unique)
    // This is a unique number, unrelated to the user context
    // It is meant to uniquely identify this session to MMBase
    // It is NOT used for authorisation!
    protected String account = null;

    // language
    protected String language = null;

    // description
    // note: in future, this is dependend on language settings!
    protected String description = null;

    // transactions
    protected HashMap transactions = new HashMap();

    // node managers cache
    protected HashMap nodeManagerCache = new HashMap();

    // relation manager cache
    protected HashMap relationManagerCache = new HashMap();

    // parent Cloud, if appropriate
    protected BasicCloud parentCloud=null;

    // MMBaseCop
    MMBaseCop mmbaseCop = null;

    // User context
    protected BasicUser userContext = null;

    /**
     *  basic constructor for descendant clouds (i.e. Transaction)
     */
    BasicCloud(String cloudName, BasicCloud cloud) {
        cloudContext=cloud.cloudContext;
        parentCloud=cloud;
        typedef = cloud.typedef;
        language=cloud.language;
        if (cloudName==null) {
            name = cloud.name;
        } else {
            name = cloud.name+"."+cloudName;
        }
        description = cloud.description;
        mmbaseCop = cloud.mmbaseCop;

        userContext = cloud.userContext;
        account= cloud.account;
    }

    /**
     */
    BasicCloud(String name, String application, HashMap loginInfo, CloudContext cloudContext) {
        // get the cloudcontext and mmbase root...
        this.cloudContext=(BasicCloudContext)cloudContext;
        MMBase mmb = this.cloudContext.mmb;

        // do authentication.....
        mmbaseCop = mmb.getMMBaseCop();

        if (mmbaseCop == null) {
            String message;
            message = "Couldn't find the MMBaseCop.";
            log.error(message);
            throw new BridgeException(message);
        }
	org.mmbase.security.UserContext uc = mmbaseCop.getAuthentication().login(application, loginInfo, null);
        if (uc == null) {
            String message;
            message = "Login invalid.";
            log.error(message);
            throw new BridgeException(message);
        }
	userContext = new BasicUser(mmbaseCop, uc);
        // end authentication...

        // other settings of the cloud...
        typedef = mmb.getTypeDef();
        language = mmb.getLanguage();

        // normally, we want the cloud to read it's context from an xml file.
        // the current system does not support multiple clouds yet,
        // so as a temporary hack we set default values
        this.name = name;
        description = name;

        // generate an unique id for this instance...
        account="U"+uniqueId();
    }

    public Node getNode(int nodenumber) {
        MMObjectNode node;
        try {
            node = BasicCloudContext.tmpObjectManager.getNode(account,""+nodenumber);
        // Catch the exception in case of a negative nodenumber.
        } catch (StringIndexOutOfBoundsException e) {
            String message;
            message = "Node with number " + nodenumber + " does not exist.";
            log.error(message);
            throw new BridgeException(message);
        // Catch the exception in case of a positive nodenumber wich is not
        // found.
        } catch (RuntimeException e) {
            String message;
            message = "Node with number " + nodenumber + " does not exist.";
            log.error(message);
            throw new BridgeException(message);
        }
        if (node==null) {
            String message;
            message = "Node with number " + nodenumber + " does not exist.";
            log.error(message);
            throw new BridgeException(message);
        } else {
            assert(Operation.READ,nodenumber);
            if (node.getNumber()==-1) {
                return new BasicNode(node, getNodeManager(node.parent.getTableName()), nodenumber);
            } else {
                return new BasicNode(node, getNodeManager(node.parent.getTableName()));
            }
        }
    }

    public Node getNode(String nodenumber) {
        MMObjectNode node = BasicCloudContext.tmpObjectManager.getNode(account,""+nodenumber);
        if (node==null) {
            String message;
            message = "Node with number " + nodenumber + " does not exist.";
            log.error(message);
            throw new BridgeException(message);
        } else {
            assert(Operation.READ,node.getNumber());
            if (node.getIntValue("number")==-1) {
                return new BasicNode(node, getNodeManager(node.parent.getTableName()), Integer.parseInt(nodenumber));
            } else {
                return new BasicNode(node, getNodeManager(node.parent.getTableName()));
            }
        }
    }

    public Node getNodeByAlias(String aliasname) {
        MMObjectNode node = BasicCloudContext.tmpObjectManager.getNode(account,aliasname);
        if ((node==null) || (node.getNumber()==-1)) {
            String message;
            message = "Node with alias " + aliasname + " does not exist.";
            log.error(message);
            throw new BridgeException(message);
        } else {
            assert(Operation.READ,node.getNumber());
            return new BasicNode(node, getNodeManager(node.parent.getTableName()));
        }
    }

    public NodeManagerList getNodeManagers() {
        Vector nodeManagers = new Vector();
        for(Enumeration builders = cloudContext.mmb.getMMObjects(); builders.hasMoreElements();) {
            MMObjectBuilder bul=(MMObjectBuilder)builders.nextElement();
            if(!bul.isVirtual()) {
                nodeManagers.add(bul.getTableName());
            }
        }
        return new BasicNodeManagerList(nodeManagers,this);
    }

    public NodeManager getNodeManager(String nodeManagerName) {
        // cache quicker, and you don't get 2000 nodetypes when you do a search....
        NodeManager nodeManager=(NodeManager)nodeManagerCache.get(nodeManagerName);
        if (nodeManager==null) {
            MMObjectBuilder bul=cloudContext.mmb.getMMObject(nodeManagerName);
            if (bul==null) {
                String message;
                message = "Node manager with name " + nodeManagerName
                          + " does not exist.";
                log.error(message);
                throw new BridgeException(message);
            }
            nodeManager=new BasicNodeManager(bul, this);
            nodeManagerCache.put(nodeManagerName,nodeManager);
        }
        return nodeManager;
    }

    /**
     * Retrieves a node manager (aka builder)
     * @param nodeManagerId ID of the NodeManager to retrieve
     * @return the requested <code>NodeManager</code> if the manager exists, <code>null</code> otherwise
     */
    NodeManager getNodeManager(int nodeManagerId) {
        return getNodeManager(typedef.getValue(nodeManagerId));
    }

    /**
     * Retrieves a RelationManager.
     * Note that you can retrieve a manager with source and destination reversed.
     * @param sourceManagerID number of the NodeManager of the source node
     * @param destinationManagerID number of the NodeManager of the destination node
     * @param roleID number of the role
     * @return the requested RelationManager
     */
    RelationManager getRelationManager(int sourceManagerId, int destinationManagerId, int roleId) {
        // cache. pretty ugly but at least you don't get 1000+ instances of a relationmanager
        RelationManager relManager=(RelationManager)relationManagerCache.get(""+sourceManagerId+"/"+destinationManagerId+"/"+roleId);
        if (relManager==null) {
            // XXX adapt for other dir too!
            Enumeration e =cloudContext.mmb.getTypeRel().search(
                  "WHERE snumber="+sourceManagerId+" AND dnumber="+destinationManagerId+" AND rnumber="+roleId);
            if (!e.hasMoreElements()) {
              e =cloudContext.mmb.getTypeRel().search(
                  "WHERE dnumber="+sourceManagerId+" AND snumber="+destinationManagerId+" AND rnumber="+roleId);
            }
            if (e.hasMoreElements()) {
                MMObjectNode node=(MMObjectNode)e.nextElement();
                relManager = new BasicRelationManager(node,this);
                relationManagerCache.put(""+sourceManagerId+"/"+destinationManagerId+"/"+roleId,relManager);
            }
        }
        return relManager;
    }

    /**
     * Retrieves a flexible (type-independent) RelationManager.
     * Note that the relation manager retrieved with this method does not contain
     * type info, which means that some data, such as source- and destination type,
     * cannot be retrieved.
     * @param roleID number of the role
     * @return the requested RelationManager
     */
    RelationManager getRelationManager(int roleId) {
        // cache. pretty ugly but at least you don't get 1000+ instances of a relationmanager
        RelationManager relManager=(RelationManager)relationManagerCache.get(""+roleId);
        if (relManager==null) {
            MMObjectNode n =cloudContext.mmb.getRelDef().getNode(roleId);
            if (n!=null) {
                relManager = new BasicRelationManager(n,this);
                relationManagerCache.put(""+roleId,relManager);
            }
        }
        return relManager;
    }

    public RelationManagerList getRelationManagers() {
        Vector v= new Vector();
        for(Enumeration e =cloudContext.mmb.getTypeRel().search("");e.hasMoreElements();) {
            v.add((MMObjectNode)e.nextElement());
        }
        return new BasicRelationManagerList(v,this);
    }

    public RelationManager getRelationManager(String sourceManagerName,
        String destinationManagerName, String roleName) {
        // uses getguesed number, maybe have to fix this later
        int r=cloudContext.mmb.getRelDef().getNumberByName(roleName);
        if (r==-1) {
            String message;
            message = "Role " + roleName + " does not exist.";
            log.error(message);
            throw new BridgeException(message);
        }
        int n1=typedef.getIntValue(sourceManagerName);
        if (n1==-1) {
            String message;
            message = "Source type " + sourceManagerName + " does not exist.";
            log.error(message);
            throw new BridgeException(message);
        }
        int n2=typedef.getIntValue(destinationManagerName);
        if (n2==-1) {
            String message;
            message = "Destination type " + destinationManagerName
                      + " does not exist.";
            log.error(message);
            throw new BridgeException(message);
        }
        RelationManager rm=getRelationManager(n1,n2,r);
        if (rm==null) {
            String message;
            message = "Relation manager from " + sourceManagerName + " to "
                      + destinationManagerName + " as " + roleName
                      + " does not exist.";
            log.error(message);
            throw new BridgeException(message);
        } else {
            return rm;
        }
    }

    public RelationManager getRelationManager(String roleName) {
        int r=cloudContext.mmb.getRelDef().getNumberByName(roleName);
        if (r==-1) {
            String message;
            message = "Role " + roleName + " does not exist.";
            log.error(message);
            throw new BridgeException(message);
        }
        RelationManager rm=getRelationManager(r);
        if (rm==null) {
            String message;
            message = "Relation manager for " + roleName + " does not exist.";
            log.error(message);
            throw new BridgeException(message);
        } else {
            return rm;
        }
    }

    /**
     * Create unique number
     */
    static synchronized int uniqueId() {
        try {
            Thread.sleep(1); // A bit paranoid, but just to be sure that not two threads steal the same millisecond.
        } catch (Exception e) {}
        return (int)(java.lang.System.currentTimeMillis() % Integer.MAX_VALUE);
    }

    public Transaction createTransaction() {
        return createTransaction(null,false);
    }

    public Transaction createTransaction(String name){
        return createTransaction(name,false);
    }

    public Transaction createTransaction(String name, boolean overwrite) {
        if (name==null) {
            name="Tran"+uniqueId();
          } else {
              Transaction oldtransaction=(Transaction)transactions.get(name);
              if (oldtransaction!=null) {
                  if (overwrite) {
                      oldtransaction.cancel();
                  } else {
                      String message;
                      message = "Transaction with name " + name
                                + "already exists.";
                      log.error(message);
                      throw new BridgeException(message);
                  }
              }
        }
        Transaction transaction = new BasicTransaction(name,this);
        transactions.put(name,transaction);
        return transaction;
    }

    public Transaction getTransaction(String name) {
        Transaction tran=(Transaction)transactions.get(name);
        if (tran==null) {
            tran = createTransaction(name,false);
        }
        return tran;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public String getName() {
        return name;
    }

    public String getDescription(){
        return description;
    }

    public User getUser() {
        return userContext;
    }

    /**
     * Retrieves the current user accountname (unique)
     * @return the account name
     */
    String getAccount() {
        return account;
    }

    /**
    * Checks access rights.
    * @param operation the operation to check (READ, WRITE, CREATE, LINK, OWN)
    * @param nodeID the node on which to check the operation
    * @return <code>true</code> if acces sis granted, <code>false</code> otherwise
    */
    boolean check(Operation operation, int nodeID) {
        return mmbaseCop.getAuthorization().check(userContext.getUserContext(),nodeID,operation);
    }

    /**
    * Asserts access rights. throws an exception if an operation is not allowed.
    * @param operation the operation to check (READ, WRITE, CREATE, LINK, OWN)
    * @param nodeID the node on which to check the operation
    */
    void assert(Operation operation, int nodeID) {
        mmbaseCop.getAuthorization().assert(userContext.getUserContext(),nodeID,operation);
    }

    /**
    * initializes access rights for a newly created node
    * @param nodeID the node to init
    */
    void createSecurityInfo(int nodeID) {
        mmbaseCop.getAuthorization().create(userContext.getUserContext(),nodeID);
    }

    /**
    * removes access rights for a removed node
    * @param nodeID the node to init
    */
    void removeSecurityInfo(int nodeID) {
        mmbaseCop.getAuthorization().remove(userContext.getUserContext(),nodeID);
    }

    /**
    * updates access rights for a changed node
    * @param nodeID the node to init
    */
    void updateSecurityInfo(int nodeID) {
        mmbaseCop.getAuthorization().update(userContext.getUserContext(),nodeID);
    }

    public NodeList getList(String startNodes, String nodePath, String fields,
            String constraints, String orderby, String directions,
            String searchDir, boolean distinct) {

        String sdistinct="";
        int search = ClusterBuilder.SEARCH_BOTH;
        String pars ="";

        if (startNodes!=null) {
            pars+=" NODES='"+startNodes+"'";
        }
        if (nodePath != null && (!nodePath.trim().equals(""))) {
            pars+=" TYPES='"+nodePath+"'";
        } else {
            String message;
            message = "No nodePath specified.";
            log.error(message);
            throw new BridgeException(message);
        }

        if (fields == null) fields = "";
        pars += " FIELDS='"+fields+"'";

        if (orderby!=null) {
            pars+=" SORTED='"+orderby+"'";
        }
        if (directions!=null) {
          pars+=" DIR='"+directions+"'";
        }
        StringTagger tagger= new StringTagger(pars,' ','=',',','\'');
        if (searchDir!=null) {
            searchDir = searchDir.toUpperCase();
            if ("DESTINATION".equals(searchDir)) {
                search = ClusterBuilder.SEARCH_DESTINATION;
            } else if ("SOURCE".equals(searchDir)) {
                search = ClusterBuilder.SEARCH_SOURCE;
            } else if ("BOTH".equals(searchDir)) {
                search = ClusterBuilder.SEARCH_BOTH;
            } else if ("ALL".equals(searchDir)) {
                search = ClusterBuilder.SEARCH_ALL;
            } else if ("EITHER".equals(searchDir)) {
                search = ClusterBuilder.SEARCH_EITHER;
            }
        }

        if (distinct) sdistinct="YES";
        Vector snodes = tagger.Values("NODES");
        Vector sfields = tagger.Values("FIELDS");
        Vector tables = tagger.Values("TYPES");
        Vector orderVec = tagger.Values("SORTED");
        Vector sdirection =tagger.Values("DIR"); // minstens een : UP
        if (sdirection==null) {
            sdirection=new Vector();
            sdirection.addElement("UP"); // UP == ASC , DOWN =DESC
        }
        ClusterBuilder clusters = cloudContext.mmb.getClusterBuilder();
        if (constraints!=null) {
            if (constraints.trim().equals("")) {
                constraints = null;
            } else {
                constraints="WHERE "+constraints;
            }
        }
        Vector v = clusters.searchMultiLevelVector(snodes,sfields,sdistinct,tables,constraints,
                                                   orderVec,sdirection,search);
        if (v!=null) {
            // get authorization for this call only
            Authorization auth=mmbaseCop.getAuthorization();
            for (int i=v.size()-1; i>=0; i--) {
                boolean check=true;
                MMObjectNode node=(MMObjectNode)v.get(i);
                for (int j=0; check && (j<tables.size()); j++) {
                    check=auth.check(userContext.getUserContext(),
                                     node.getIntValue(tables.get(j)+".number"),
                                     Operation.READ);
                }
                if (!check) v.remove(i);
            }
            NodeManager tempNodeManager = null;
            if (v.size()>0) {
                tempNodeManager = new VirtualNodeManager((MMObjectNode)v.get(0),this);
            }
            return new BasicNodeList(v,this,tempNodeManager);
        } else {
            String message;
            message = "Parameters are invalid :" + pars + " - " + constraints;
            log.error(message);
            throw new BridgeException(message);
        }
    }

    /**
     * set the Context of the current Node
     */
    void setContext(int nodeNumber, String context) {
    	mmbaseCop.getAuthorization().setContext(userContext.getUserContext(), nodeNumber, context);
    }

    /**
     * get the Context of the current Node
     */
    String getContext(int nodeNumber) {
    	return mmbaseCop.getAuthorization().getContext(userContext.getUserContext(), nodeNumber);
    }

    /**
     * get the Contextes which can be set to this specific node
     */
    StringList getPossibleContexts(int nodeNumber) {
    	return new BasicStringList(mmbaseCop.getAuthorization().getPossibleContexts(userContext.getUserContext(), nodeNumber));
    }
}

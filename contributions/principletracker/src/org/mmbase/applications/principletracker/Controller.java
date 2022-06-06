/*
 *  This software is OSI Certified Open Source Software.
 *  OSI Certified is a certification mark of the Open Source Initiative.
 *  The license (Mozilla version 1.0) can be read at the MMBase site.
 *  See http://www.MMBase.org/license
 */
package org.mmbase.applications.principletracker;

import java.io.*;
import java.net.URL;
import java.text.DateFormat;
import java.util.*;
import java.util.jar.*;

import org.mmbase.bridge.*;
import org.mmbase.bridge.implementation.*;
import org.mmbase.util.logging.*;
import org.mmbase.util.xml.*;
import org.mmbase.util.*;
import org.mmbase.module.core.*;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.implementation.*;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

/**
 * @author     Daniel Ockeloen
 * @created    April 12, 2006
 */
public class Controller {

    private static Logger log = Logging.getLoggerInstance(Controller.class);
    private static Cloud cloud;


    /** DTD resource filename of the principleset DTD version 1.0 */
    public static final String DTD_PRINCIPLESET_1_0 = "principleset_1_0.dtd";

    /** Public ID of the principleset DTD version 1.0 */
    public static final String PUBLIC_ID_PRINCIPLESET_1_0 = "-//MMBase//DTD principleset 1.0//EN";

    /**
     * Register the Public Ids for DTDs used by DatabaseReader
     * This method is called by XMLEntityResolver.
     */
    public static void registerPublicIDs() {
        XMLEntityResolver.registerPublicID(PUBLIC_ID_PRINCIPLESET_1_0, DTD_PRINCIPLESET_1_0, Controller.class);
    }

    /**
     *Constructor for the Controller object
     */
    public Controller() {
           cloud = ContextProvider.getDefaultCloudContext().getCloud("mmbase", "class", null);
    }

    public String getNextPatchLevel(String version) {
	StringTokenizer tok = new StringTokenizer(version,".\n\r");
	if (tok.hasMoreTokens()) {
	    String mayor = tok.nextToken();
	    if (tok.hasMoreTokens()) {
		String minor = tok.nextToken();
	    	if (tok.hasMoreTokens()) {
			String patchlevel = tok.nextToken();
			try {
				int i = Integer.parseInt(patchlevel);
				return mayor+"."+minor+"."+(i+1);
			} catch(Exception e) {
			  // illegal patchlevel, set to .1
			     return (version+".1");
			}
		} else {
			return (version+".1");
		}
            } else {
		return (version+".0.1");
	    }
	} else {
		return ("1.0");
	}
    }

    public String isOlderVersion(String currentversion, String checkversion) {
	int cu = getVersionValue(currentversion);
	int ch = getVersionValue(checkversion);
	if (cu==-1 || ch==-1) return "unknown";
	if (ch==cu) return "same";
	if (cu<ch) {
		return "newer";
	} else {
		return "older";
	}
    }


    public String isOlderPatchLevel(String currentversion, String checkversion) {
	try {
	StringTokenizer tok = new StringTokenizer(currentversion,".\n\r");
	StringTokenizer tok2 = new StringTokenizer(checkversion,".\n\r");
	if (tok.hasMoreTokens() && tok2.hasMoreTokens()) {
	    if (tok.nextToken().equals(tok2.nextToken())) {
		if (tok.hasMoreTokens() && tok2.hasMoreTokens()) {
	    	    if (tok.nextToken().equals(tok2.nextToken())) {
			if (tok.hasMoreTokens() && tok2.hasMoreTokens()) {
				int cu = Integer.parseInt(tok.nextToken());
				int ch = Integer.parseInt(tok2.nextToken());
				if (ch==cu) return "same";
				if (cu<ch) {
					return "newer";
				} else {
					return "older";
				}
			}
		    }
		}
	    }
	}
      } catch(Exception e) {
	// something went wrong return false;
      }
      return "invalid";
    }

    public int getVersionValue(String version) {
      // use a stupid trick that works until we get more than 9999 version
      // per type
      try {
	int value=0;
	StringTokenizer tok = new StringTokenizer(version,".\n\r");
	if (tok.hasMoreTokens()) {
	    value += Integer.parseInt(tok.nextToken())*100000000;
	    if (tok.hasMoreTokens()) {
	    	value += Integer.parseInt(tok.nextToken())*10000;
	    	if (tok.hasMoreTokens()) {
	    		value += Integer.parseInt(tok.nextToken());
		}
            }
	}
	return value;
      } catch(Exception e) {
	// something went wrong return  -1
      }
      return -1;
    }


    public String getNextPrincipleNumber(String principleset) {
	    // since i didn't want to enforce int only numbers
	    // but to want to help and and guess the next number
	    // we need this kinda kludge
	    // (seems i did demand Integer afteral, im leaving this in
	    //  since i might change it)
	    int current = 0;
            NodeManager principlesetmanager = cloud.getNodeManager("principlesets");
            NodeManager principlemanager = cloud.getNodeManager("principle");
            Query query = cloud.createQuery();
            Step step1 = query.addStep(principlesetmanager);
            RelationStep step2 = query.addRelationStep(principlemanager);
            StepField f1 = query.addField(step1, principlesetmanager.getField("number"));
            query.addField(step2.getNext(), principlemanager.getField("principlenumber"));

            query.setConstraint(query.createConstraint(f1, new Integer(principleset)));

            NodeIterator i = cloud.getList(query).nodeIterator();
            while (i.hasNext()) {
                org.mmbase.bridge.Node node = i.nextNode();
		String value = node.getStringValue("principle.principlenumber");
		try {
			int parsed = Integer.parseInt(value);
			if (parsed > current) current = parsed;
		} catch(Exception e) {
		}
	    }
	return ""+(current+1);
    }

    public String exportPrincipleSet(String setid,String filepath,String wantedstate) {
	org.mmbase.bridge.Node node=cloud.getNode(setid);
	if (node!=null) {
	    String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
	    body += "<!DOCTYPE principleset PUBLIC \"//MMBase - principleset//\" \"https://www.mmbase.org/dtd/principleset_1_0.dtd\">\n";
	    body += "<principleset name=\""+node.getStringValue("name")+"\" description=\""+node.getStringValue("description")+"\">\n";
            RelationIterator i = node.getRelations("principlerel", "principle").relationIterator();
            while (i.hasNext()) {
                Relation principlerel = i.nextRelation();
		org.mmbase.bridge.Node principleset = principlerel.getSource();
		org.mmbase.bridge.Node principle = null;
		if (principleset.getNumber()==node.getNumber()) {
			principle = principlerel.getDestination();
		} else {
			principle = principleset;
			principleset = principlerel.getDestination();
		}
		String setname = principleset.getStringValue("name");
		String state = principlerel.getStringValue("state");
		String name = principle.getStringValue("name");
		if (wantedstate.equals("all") || state.equals("active")) {
	            body += "  <principle number=\""+principle.getStringValue("principlenumber")+"\">\n";
	            body += "    <state>"+principlerel.getStringValue("state")+"</state>\n";
	            body += "    <version>"+principle.getStringValue("version")+"</version>\n";
	            body += "    <theme>"+principle.getStringValue("theme")+"</theme>\n";
	            body += "    <name><![CDATA["+name+"]]></name>\n";
	            body += "    <qualification><![CDATA["+principle.getStringValue("qualification")+"]]></qualification>\n";
	            body += "    <explanation><![CDATA["+principle.getStringValue("explanation")+"]]></explanation>\n";
	            body += "    <argumentation><![CDATA["+principle.getStringValue("argumentation")+"]]></argumentation>\n";
	            body += "    <consequence><![CDATA["+principle.getStringValue("consequence")+"]]></consequence>\n";
	            body += "    <allowedimplementation><![CDATA["+principle.getStringValue("allowedimpl")+"]]></allowedimplementation>\n";
	            body += "    <source><![CDATA["+principle.getStringValue("source")+"]]></source>\n";
	            body += "  </principle>\n";
		}
	    }
	    body += "</principleset>\n";
            File sfile = new File(filepath);
            try {
                DataOutputStream scan = new DataOutputStream(new FileOutputStream(sfile));
                scan.writeBytes(body);
                scan.flush();
                scan.close();
            } catch(Exception e) {
                log.error(Logging.stackTrace(e));
            }
	} else {
		log.error("Set node found");
		return "set not found";
	}
	return "saved";
    }


    public String exportToDocbook(String setid,String filepath) {
	org.mmbase.bridge.Node node=cloud.getNode(setid);
	if (node!=null) {
	    String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
	    body += "<!DOCTYPE article PUBLIC \"-//OASIS//DTS DocBook XML V4.1.2//EN\" \"http://www.oasis-open.org/docbook/xml/4.0/docbookx.dtd\">\n";
	    body += "<article>\n";
	    body += "  <articleinfo>\n";
	    body += "    <title>MMBase princinples (demo version)</title>\n";
	    body += "    <date>"+(new Date()).toString()+"</date>\n";
	    body += "    <edition>$Id$</edition>\n";
	    body += "    <authorgroup>\n";
	    body += "      <author>\n";
	    body += "        <firstname>MMBase</firstname><surname>Community</surname>\n";
	    body += "        <affiliation><orgname>MMBase.org</orgname></affiliation>\n";
	    body += "      </author>\n";
	    body += "    </authorgroup>\n";
	    body += "    <legalnotice>\n";
	    body += "      <para>This software is OSI Certified Open Source Software. OSI Certified is a certification mark of the Open Source Initiative.</para>\n";
	    body += "      <para>The license (Mozilla version 1.0) can be read at the MMBase site. See <ulink url=\"http://www.mmbase.org/license\">http://www.mmbase.org/license</ulink></para>\n";
	    body += "    </legalnotice>\n";
	    body += "  </articleinfo>\n";
	    //
            NodeManager principlesetsmanager = cloud.getNodeManager("principlesets");
            NodeManager principlerelmanager = cloud.getNodeManager("principlerel");
            NodeManager principlemanager = cloud.getNodeManager("principle");
            Query query = cloud.createQuery();
            Step step1 = query.addStep(principlesetsmanager);
            RelationStep step2 = query.addRelationStep(principlemanager,"principlerel","BOTH");
            Step step3 = step2.getNext();

            StepField f1 = query.addField(step1, principlesetsmanager.getField("number"));
            StepField f3 = query.addField(step2,principlerelmanager.getField("state"));
            StepField f4 = query.addField(step3, principlemanager.getField("principlenumber"));
            query.addField(step3, principlemanager.getField("name"));

            query.addSortOrder(f4, SortOrder.ORDER_ASCENDING);

            query.setConstraint(query.createConstraint(f1, new Integer(setid)));
            NodeIterator i2 = cloud.getList(query).nodeIterator();
	    int newcount = 0;
	    int newthreadcount = 0;
            while (i2.hasNext()) {
                org.mmbase.bridge.Node n2 = i2.nextNode();
		String setname = n2.getStringValue("principlesets.name");
		String state =n2.getStringValue("principlerel.state");
		String name = n2.getStringValue("principle.name");
		String principlenumber = n2.getStringValue("principle.principlenumber");
		String qualification = n2.getStringValue("principle.qualification");
		String themes = n2.getStringValue("principle.theme");
		String version = n2.getStringValue("principle.version");
		String explanation = n2.getStringValue("principle.explanation");
		String argumentation = n2.getStringValue("principle.argumentation");
		String consequence = n2.getStringValue("principle.consequence");
		String allowedimpl = n2.getStringValue("principle.allowedimpl");
		String source = n2.getStringValue("principle.source");
		if (state.equals("active")) {
	            body += "  <section>\n";
	            body += "    <title>"+principlenumber+") "+name+"</title>\n";
		    body += "    <para>\n";
		    body += "    <itemizedlist>\n";
	            body += "    <listitem><para><![CDATA[State : "+state+"\n\n]]></para></listitem>\n";
	            body += "    <listitem><para><![CDATA[Version : "+version+"\n\n]]></para></listitem>\n";
	            body += "    <listitem><para><![CDATA[Themes : "+themes+"\n\n]]></para></listitem>\n";
	            body += "    <listitem><para><![CDATA[Argumentation : "+argumentation+"\n\n]]></para></listitem>\n";
	            body += "    <listitem><para><![CDATA[Explenation : "+explanation+"\n\n]]></para></listitem>\n";
	            body += "    <listitem><para><![CDATA[Consequence : "+consequence+"\n\n]]></para></listitem>\n";
	            body += "    <listitem><para><![CDATA[Allowed Implementation : "+allowedimpl+"\n\n]]></para></listitem>\n";
	            body += "    <listitem><para><![CDATA[Source : "+source+"\n\n]]></para></listitem>\n";
		    body += "    </itemizedlist>\n";
		    body += "    </para>\n";
	            body += "  </section>\n";
		}
	    }
	    body += "</article>\n";
            File sfile = new File(filepath);
            try {
                DataOutputStream scan = new DataOutputStream(new FileOutputStream(sfile));
                scan.writeBytes(body);
                scan.flush();
                scan.close();
            } catch(Exception e) {
                log.error(Logging.stackTrace(e));
            }
	} else {
		log.error("Set node found");
		return "set not found";
	}
	return "saved";
    }

    public String importPrincipleSet(String setname,String filepath) {

        NodeManager principlesetmanager = cloud.getNodeManager("principlesets");
        RelationManager principlerelmanager = cloud.getRelationManager("principlesets", "principle", "principlerel");
        NodeManager principlemanager = cloud.getNodeManager("principle");

        DocumentReader reader = null;
	if (filepath.startsWith("config/")) {
		try {
                InputSource is = ResourceLoader.getConfigurationRoot().getInputSource(filepath.substring(7));
                reader = new DocumentReader(is, Controller.class);
		} catch (Exception e) {}
	} else if (filepath.startsWith("html/")) {
		try {
                InputSource is = ResourceLoader.getWebRoot().getInputSource(filepath.substring(5));
                reader = new DocumentReader(is, Controller.class);
		} catch (Exception e) {}
	} else {
        	File file = new File(filepath);
        	if (file.exists()) {
            	   reader = new DocumentReader(new InputSource(filepath), Controller.class);
	       }
	}

        if (reader != null) {

            	Element mainnode = reader.getElementByPath("principleset");
		if (mainnode!=null) {
  	         NamedNodeMap nm=mainnode.getAttributes();
                 org.w3c.dom.Node n4=nm.getNamedItem("name");
		if (n4!=null) {
			if (setname.equals(""))  setname = n4.getNodeValue();
		}
                n4=nm.getNamedItem("description");
		String description = "";
		if (n4!=null) {
			description = n4.getNodeValue();
		}

		// create the new set
        	org.mmbase.bridge.Node psn = principlesetmanager.createNode();
        	psn.setStringValue("name", setname);
        	psn.setStringValue("description", description);
		psn.commit();

            	for(Iterator ns=reader.getChildElements("principleset","principle").iterator();ns.hasNext(); ) {
               		Element n=(Element)ns.next();
  	               	nm=n.getAttributes();
                 	if (nm!=null) {
				int principlenumber=-1;
				String name="";
				String state="active";
				String version="";
				String theme="";
				String qualification="";
				String explanation="";
				String argumentation="";
				String consequence="";
				String allowedimplementation="";
				String source="";
                        	org.w3c.dom.Node n2=nm.getNamedItem("number");
                        	if (n2!=null) {
					try {
					    principlenumber=Integer.parseInt(n2.getNodeValue());
					} catch(Exception e) {
						log.error("illegal principlenumber");
					}
				}
                            	org.w3c.dom.Node n3 = n.getFirstChild();
                            	while (n3!=null) {
					String key = n3.getNodeName();
					if (key.equals("name")) {
						if (n3.getFirstChild()!=null) name = n3.getFirstChild().getNodeValue();
					} else if (key.equals("qualification")) {
						if (n3.getFirstChild()!=null) qualification = n3.getFirstChild().getNodeValue();
					} else if (key.equals("version")) {
						if (n3.getFirstChild()!=null) version = n3.getFirstChild().getNodeValue();
					} else if (key.equals("theme")) {
						if (n3.getFirstChild()!=null) theme = n3.getFirstChild().getNodeValue();
					} else if (key.equals("explanation")) {
						if (n3.getFirstChild()!=null) explanation = n3.getFirstChild().getNodeValue();
					} else if (key.equals("argumentation")) {
						if (n3.getFirstChild()!=null) argumentation = n3.getFirstChild().getNodeValue();
					} else if (key.equals("allowedimplementation")) {
						if (n3.getFirstChild()!=null) allowedimplementation = n3.getFirstChild().getNodeValue();
					} else if (key.equals("consequence")) {
						if (n3.getFirstChild()!=null) consequence = n3.getFirstChild().getNodeValue();
					} else if (key.equals("source")) {
						if (n3.getFirstChild()!=null) source = n3.getFirstChild().getNodeValue();
					} else if (key.equals("state")) {
						if (n3.getFirstChild()!=null) state = n3.getFirstChild().getNodeValue();
					}
					n3 = n3.getNextSibling();
				}
        			org.mmbase.bridge.Node pn = principlemanager.createNode();
        			pn.setIntValue("principlenumber", principlenumber);
        			pn.setStringValue("name", name);
        			pn.setStringValue("version", version);
        			pn.setStringValue("theme", theme);
        			pn.setStringValue("qualification", qualification);
        			pn.setStringValue("explanation", explanation);
        			pn.setStringValue("argumentation", argumentation);
        			pn.setStringValue("consequence", consequence);
        			pn.setStringValue("allowedimpl", allowedimplementation);
				pn.commit();
            			org.mmbase.bridge.Node prn = principlerelmanager.createRelation(psn, pn);
				prn.setStringValue("state",state);
				prn.commit();
			}
	        }
		}
	    }
        return "imported";
    }

}


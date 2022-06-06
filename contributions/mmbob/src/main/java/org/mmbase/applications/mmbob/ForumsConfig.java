/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

 */

package org.mmbase.applications.mmbob;

import org.w3c.dom.*;

import java.io.*;
import java.util.*;

import org.mmbase.util.*;
import org.mmbase.util.xml.*;
import org.mmbase.module.*;
import org.mmbase.module.core.*;
import org.mmbase.module.corebuilders.*;
import org.mmbase.util.logging.*;


/**
 * forumManager
 * ToDo: Write docs!
 *
 * @todo This class looks remarkably like {@link ForumConfig}. Something's odd.
 * @author Daniel Ockeloen (MMBased)
 * @verion $Id$
 */
public class ForumsConfig {
    private static final Logger log = Logging.getLoggerInstance(ForumsConfig.class);
    private final Map fieldaliases = new HashMap();
    private final Map subs = new HashMap();
    private String defaultaccount, defaultpassword;
    private String accountcreationtype="open";
    private String accountremovaltype="open";
    private String loginsystemtype="http";
    private String loginmodetype="open";
    private String logoutmodetype="open";
    private String guestreadmodetype="open";
    private String guestwritemodetype="open";
    private String threadstartlevel="all";
    private String navigationmethod="list";
    private boolean replyoneachpage = false;
    private int preloadchangedthreadstime = 0;
    private int swapoutunusedthreadstime = 0;
    private int speedposttime=60;
    private int postingsoverflowpostarea=4;
    private int postingsoverflowthreadpage=15;
    private HashMap emailtexts =  new HashMap();

    private String avatarsUploadEnabled = "true";
    private String avatarsGalleryEnabled = "true";

    private String xsltpostingsodd = "xslt/posting2xhtmlDark.xslt";
    private String xsltpostingseven = "xslt/posting2xhtmlLight.xslt";

    private String contactInfoEnabled = "true";
    private String smileysEnabled = "true";
    private String privateMessagesEnabled = "true";
    private int postingsPerPage = 10;
    private String fromEmailAddress = "";
    private String rooturl = "";
    private String externalrooturl = "";
    private String htmlHeaderPath = "header.jsp";
    private String htmlFooterPath = "footer.jsp";

    private static HashMap filterwords;
    private int quotamax = 100;
    private int quotasoftwarning = 60;
    private int quotawarning = 80;
    private boolean firstrun = true;

    private final DocumentReader reader;
    private final Element element;

    public ForumsConfig (DocumentReader reader, Element n) {
        element = n;
        this.reader = reader;
        log.debug("subhasmap cleared");
	if (firstrun) checkCloudModel();
	firstrun = false;
	decodeConfig(reader, n);
    }
    public ForumsConfig() {
        element = null;
        reader = null;
    }
    public Element getElement() {
        return element;
    }
    public DocumentReader getReader() {
        return reader;
    }

    /**
     * Only for compile-time compliance with  both MMBase 1.8 and MMBase 1.8, because the
     * return-type of {@link DocumentReader#getChildElements} changed, and we want to compile agains
     * MMBase-1.8
     */
    protected static Iterable<Element> list(final Object o) {
        if (o instanceof Iterator) {
            return new Iterable<Element>() {
                public Iterator<Element> iterator() {
                    return (Iterator<Element>) o;
                }
            };
        } else {
            return (Iterable<Element>) o;
        }
    }

    private boolean decodeConfig(DocumentReader reader,Element n) {
        NamedNodeMap nm = n.getAttributes();
        if (nm != null) {
            String id = "default";
            String account = "admin";
            String password = "admin2k";
            // decode name
            org.w3c.dom.Node n3 = nm.getNamedItem("id");
            if (n3 != null) {
                id = n3.getNodeValue();
            }
            // decode account
            n3 = nm.getNamedItem("account");
            if (n3 != null) {
                account = n3.getNodeValue();
            }
            // decode password
            n3 = nm.getNamedItem("password");
            if (n3 != null) {
                password = n3.getNodeValue();
            }
            if (id.equals("default")) {
                defaultaccount = account;
                defaultpassword = password;
            }

            //get xslt configuration
            Element xsltElement = reader.getElementByPath("mmbobconfig.forums.xslts.postings");
            if (xsltElement!=null) {
                xsltpostingsodd = xsltElement.getAttribute("odd");
                xsltpostingseven = xsltElement.getAttribute("even");
            }

            //get avatar configuration
            Element avatarsElement = reader.getElementByPath("mmbobconfig.forums.avatars");
            if (avatarsElement != null) {
                Element avatarsUploadElement = reader.getElementByPath(avatarsElement, "avatars.upload");
                avatarsUploadEnabled = avatarsUploadElement.getAttribute("enable");
                Element avatarsGalleryElement = reader.getElementByPath(avatarsElement,"avatars.gallery");
                avatarsGalleryEnabled = avatarsGalleryElement.getAttribute("enable");
            }

            for (Element n2 : list(reader.getChildElements(n, "generatedata"))) {
                nm = n2.getAttributes();
                if (nm != null) {
                    String role = null;
                    String dfile = null;
                    String tokenizer = null;
                    n3 = nm.getNamedItem("role");
                    if (n3 != null) {
                        role = n3.getNodeValue();
                    }
                    n3 = nm.getNamedItem("file");
                    if (n3 != null) {
                        dfile = n3.getNodeValue();
                    }
                    n3 = nm.getNamedItem("tokenizer");
                    if (n3 != null) {
                        tokenizer = n3.getNodeValue();
                    }
                    org.mmbase.applications.mmbob.generate.Handler.setGenerateFile(role, dfile, tokenizer);
                }
            }

            for (Element n2 : list(reader.getChildElements(n, "quota"))) {
                nm = n2.getAttributes();
                if (nm != null) {
                    n3 = nm.getNamedItem("max");
                    if (n3 != null) {
                        setQuotaMax(n3.getNodeValue());
                    }
                    n3 = nm.getNamedItem("softwarning");
                    if (n3 != null) {
                        setQuotaSoftWarning(n3.getNodeValue());
                    }
                    n3 = nm.getNamedItem("warning");
                    if (n3 != null) {
                        setQuotaWarning(n3.getNodeValue());
                    }
                }
            }

            // get time
            preloadchangedthreadstime = 0;
            String tmp = getAttributeValue(reader,n,"preloadchangedthreads","minutes");
            try {
                if (tmp!=null) preloadchangedthreadstime+=Integer.parseInt(tmp)*60;
            } catch (Exception e) {}
            tmp = getAttributeValue(reader,n,"preloadchangedthreads","hours");
            try {
                if (tmp!=null) preloadchangedthreadstime+=Integer.parseInt(tmp)*3600;
            } catch (Exception e) {}
            tmp = getAttributeValue(reader,n,"preloadchangedthreads","days");
            try {
                if (tmp!=null) preloadchangedthreadstime+=Integer.parseInt(tmp)*(3600*24);
            } catch (Exception e) {}


            // get time
            swapoutunusedthreadstime = 0;
            tmp = getAttributeValue(reader,n,"swapoutunusedthreads","minutes");
            try {
                if (tmp!=null) swapoutunusedthreadstime+=Integer.parseInt(tmp)*60;
            } catch (Exception e) {}
            tmp = getAttributeValue(reader,n,"swapoutunusedthreads","hours");
            try {
                if (tmp!=null) swapoutunusedthreadstime+=Integer.parseInt(tmp)*3600;
            } catch (Exception e) {}
            tmp = getAttributeValue(reader,n,"swapoutunusedthreads","days");
            try {
                if (tmp!=null) swapoutunusedthreadstime+=Integer.parseInt(tmp)*(3600*24);
            } catch (Exception e) {}

            accountcreationtype = getAttributeValue(reader,n,"accountcreation","type");
            accountremovaltype = getAttributeValue(reader,n,"accountremoval","type");
            loginsystemtype = getAttributeValue(reader,n,"loginsystem","type");
            loginmodetype = getAttributeValue(reader,n,"loginmode","type");
            logoutmodetype = getAttributeValue(reader,n,"logoutmode","type");
            guestreadmodetype = getAttributeValue(reader,n,"guestreadmode","type");
            guestwritemodetype = getAttributeValue(reader,n,"guestwritemode","type");
            threadstartlevel = getAttributeValue(reader,n,"threadstart","level");

            contactInfoEnabled = getAttributeValue(reader,n,"contactinfo","enable");
            smileysEnabled = getAttributeValue(reader,n,"smileys","enable");
            privateMessagesEnabled = getAttributeValue(reader,n,"privatemessages","enable");
            String inttemp = getAttributeValue(reader,n,"postingsperpage","value");
            if (inttemp != null) {
                postingsPerPage = (Integer.valueOf(inttemp)).intValue();
            }

            inttemp = getAttributeValue(reader,n,"speedposttime","value");
            if (inttemp != null) {
                speedposttime = (Integer.valueOf(inttemp)).intValue();
            }

            inttemp = getAttributeValue(reader,n,"postingsoverflowpostarea","value");
            if (inttemp != null) {
                postingsoverflowpostarea = (Integer.valueOf(inttemp)).intValue();
            }

            inttemp = getAttributeValue(reader,n,"postingsoverflowthreadpage","value");
            if (inttemp != null) {
                postingsoverflowthreadpage = (Integer.valueOf(inttemp)).intValue();
            }

            rooturl = getAttributeValue(reader,n,"urls","root");
            externalrooturl = getAttributeValue(reader,n,"urls","externalroot");
            fromEmailAddress = getAttributeValue(reader,n,"email","from");

            if ("true".equals(getAttributeValue(reader,n,"replyoneachpage","value"))) {
                replyoneachpage = true;
            }

            for(Element n2 : list(reader.getChildElements(n,"layout"))) {
                org.w3c.dom.NodeList layoutList = n2.getElementsByTagName("footer");
                if (layoutList.getLength() > 0) {
                    Element footerNode = (Element)layoutList.item(0);
                    htmlFooterPath = footerNode.getAttribute("path");
                }
                layoutList = n2.getElementsByTagName("header");
                if (layoutList.getLength() > 0) {
                    Element headerNode = (Element)layoutList.item(0);
                    htmlHeaderPath = headerNode.getAttribute("path");
                }
            }
            for (Element n2 : list(reader.getChildElements(n,"filters"))) {
                org.w3c.dom.Node n4 = n2.getFirstChild();
                while (n4!=null) {
                    nm=n4.getAttributes();
                    if (nm!=null) {
                        String type=null;
                        String replace=null;
                        String with=null;
                        n3=nm.getNamedItem("type");
                        if (n3!=null) {
                            type=n3.getNodeValue();
                        }
                        n3=nm.getNamedItem("replace");
                        if (n3!=null) {
                            replace=n3.getNodeValue();
                        }
                        n3=nm.getNamedItem("with");
                        if (n3!=null) {
                            with=n3.getNodeValue();
                        }
                        if (type.equals("content")) {
                            if (filterwords==null) filterwords = new HashMap();
                            filterwords.put(replace,with);
                        }
                    }
                    n4 = n4.getNextSibling();
                }
            }
            for (Element n2 : list(reader.getChildElements(n,"emailtexts"))) {
                org.w3c.dom.Node n4 = n2.getFirstChild();
                while (n4!=null) {
                    nm=n4.getAttributes();
                    if (nm!=null) {
                        String role=null;
                        String text=null;
                        n3=nm.getNamedItem("role");
                        if (n3!=null) {
                            role=n3.getNodeValue();
                        }
                        text=n4.getFirstChild().getNodeValue();
                        if (role!=null && text!=null) emailtexts.put(role,text);
                    }
                    n4=n4.getNextSibling();
                }
            }
            for(Element n2 : list(reader.getChildElements(n,"alias"))) {
                nm=n2.getAttributes();
                if (nm!=null) {
                    String object=null;
                    String extern=null;
                    String field=null;
                    String externfield=null;
                    String key=null;
                    String externkey=null;
                    n3=nm.getNamedItem("object");
                    if (n3!=null) {
                        object=n3.getNodeValue();                                        		}
                    n3=nm.getNamedItem("extern");
                    if (n3!=null) {
                        extern=n3.getNodeValue();
                    }
                    n3=nm.getNamedItem("field");
                    if (n3!=null) {
                        field=n3.getNodeValue();
                    }
                    n3=nm.getNamedItem("externfield");
                    if (n3!=null) {
                        externfield=n3.getNodeValue();
                    }
                    n3=nm.getNamedItem("key");
                    if (n3!=null) {
                        key=n3.getNodeValue();
                    }
                    n3=nm.getNamedItem("externkey");
                    if (n3!=null) {
                        externkey=n3.getNodeValue();
                    }
                    id="default."+object+"."+field;
                    FieldAlias fa = new FieldAlias(id);
                    fa.setObject(object);
                    fa.setExtern(extern);
                    fa.setField(field);
                    fa.setExternField(externfield);
                    fa.setKey(key);
                    fa.setExternKey(externkey);
                    fieldaliases.put(id, fa);
                }
            }

            for(Element n2 : list(reader.getChildElements(n,"forum"))) {
                ForumConfig config = new ForumConfig(reader, n2);
                subs.put(config.getId(), config);
            }

        }
        return true;
    }

    public Map getFieldaliases() {
        return fieldaliases;
    }


    public String getDefaultPassword() {
        return defaultpassword;
    }

    public String getDefaultAccount() {
        return defaultaccount;
    }

    /**
     * ToDo: Write docs!
     * @param id
     */
    protected Map getNamePassword(String id) {
        Map user = new HashMap();
        if (id.equals("default")) {
            user.put("username", defaultaccount);
            user.put("password", defaultpassword);
        }
        return user;
    }


    public void setQuotaMax(String maxs) {
	try {
            quotamax=Integer.parseInt(maxs);
	} catch (Exception e) {
            log.error("illegal (non number) value set for quota max");
	}
    }


    public void setQuotaMax(int max) {
	quotamax=max;
    }

    public void setQuotaSoftWarning(String sws) {
	try {
            quotasoftwarning=Integer.parseInt(sws);
	} catch (Exception e) {
            log.error("illegal (non number) value set for quota softwarning");
	}
    }


    public void setQuotaWarning(String ws) {
	try {
            quotawarning=Integer.parseInt(ws);
	} catch (Exception e) {
            log.error("illegal (non number) value set for quota warning");
	}
    }

    public int getQuotaMax() {
	return quotamax;
    }

    public int getQuotaSoftWarning() {
	return quotasoftwarning;
    }

    public int getQuotaWarning() {
	return quotawarning;
    }

    public ForumConfig getForumConfig(String id) {
	Object o = subs.get(id);
	if (o != null) {
            return (ForumConfig)o;
	}
	return null;
    }

    private String getAttributeValue(DocumentReader reader,Element n,String itemname,String attribute) {
        for (Element n2 : list(reader.getChildElements(n, itemname))) {
            NamedNodeMap nm = n2.getAttributes();
            if (nm != null) {
                org.w3c.dom.Node n3 = nm.getNamedItem(attribute);
                if (n3 != null) {
                    return n3.getNodeValue();
                }
            }
        }
        return null;
    }

    public String getAccountCreationType() {
        return accountcreationtype;
    }

    public String getAccountRemovalType() {
        return accountremovaltype;
    }

    public String getLoginSystemType() {
        return loginsystemtype;
    }

    public String getLoginModeType() {
        return loginmodetype;
    }

    public void setLoginModeType(String mode) {
	loginmodetype = mode;
    }

    public void setLoginSystemType(String system) {
	loginsystemtype = system;
    }

    public String getLogoutModeType() {
        return logoutmodetype;
    }

    public void setLogoutModeType(String mode) {
	logoutmodetype = mode;
    }

    public String getGuestReadModeType() {
        return guestreadmodetype;
    }

    public String getThreadStartLevel() {
        return threadstartlevel;
    }

    public void setGuestReadModeType(String mode) {
	guestreadmodetype = mode;
    }

    public String getGuestWriteModeType() {
        return guestwritemodetype;
    }

    public void setGuestWriteModeType(String mode) {
	guestwritemodetype = mode;
    }

    public String getAvatarsUploadEnabled() {
        return avatarsUploadEnabled;
    }

    public void setAvatarsUploadEnabled(String mode) {
	avatarsUploadEnabled = mode;
    }

    public void setPostingsPerPage(String number) {
	try {
            postingsPerPage = Integer.parseInt(number);
	} catch(Exception e) {
            log.info("Illegal number for postings per page");
	}
    }

    public void setContactInfoEnabled(String mode) {
	contactInfoEnabled = mode;
    }

    public void setSmileysEnabled(String mode) {
	smileysEnabled = mode;
    }

    public void setPrivateMessagesEnabled(String mode) {
	privateMessagesEnabled = mode;
    }

    public String getAvatarsGalleryEnabled() {
        return avatarsGalleryEnabled;
    }

    public void setAvatarsGalleryEnabled(String mode) {
	avatarsGalleryEnabled = mode;
    }

    public int getPreloadChangedThreadsTime() {
	return preloadchangedthreadstime;
    }

    public int getSwapoutUnusedThreadsTime() {
	return swapoutunusedthreadstime;
    }

    public String getXSLTPostingsOdd() {
        return xsltpostingsodd;
    }

    public String getXSLTPostingsEven() {
        return xsltpostingseven;
    }

    public String getContactInfoEnabled() {
        return contactInfoEnabled;
    }

    public String getSmileysEnabled() {
        return smileysEnabled;
    }

    public String getPrivateMessagesEnabled() {
        return privateMessagesEnabled;
    }

    public String getLanguage() {
        return "en";
    }

    public int getPostingsPerPage() {
        return postingsPerPage;
    }

    public String getFromEmailAddress() {
        return fromEmailAddress;
    }

    public String getHeaderPath() {
        return htmlHeaderPath;
    }

    public String getFooterPath() {
        return htmlFooterPath;
    }

    public void save() {

        /// OOOH NOOOO!
	String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
	body += "<!DOCTYPE mmbobconfig PUBLIC \"-//MMBase/DTD mmbob config 1.0//EN\" \"https://www.mmbase.org/dtd/mmbobconfig_1_0.dtd\">\n";
	body += "<mmbobconfig>\n";
	body += "\t<forums account=\""+getDefaultAccount()+"\" password=\""+getDefaultPassword()+"\" language=\""+getLanguage()+"\">\n";
	body += "\t\t<generatedata role=\"body\" file=\"generate/body.txt\" tokenizer=\",!? \" />\n";
	body += "\t\t<generatedata role=\"smileys\" file=\"generate/smileys.txt\" tokenizer=\" \\n\\r\" />\n";
	body += "\t\t<generatedata role=\"punctuation\" file=\"generate/punctuation.txt\" tokenizer=\" \\n\\r\" />\n\n";
	body += "\t\t<urls externalroot=\""+getExternalRootUrl()+"\" root=\""+getRootUrl()+"\" />\n";
	body += "\t\t<preloadchangedthreads  minutes=\""+(preloadchangedthreadstime/60)+"\" />\n";
	body += "\t\t<swapoutunusedthreads  minutes=\""+(swapoutunusedthreadstime/60)+"\" />\n";
	body += "\t\t<accountcreation type=\""+getAccountCreationType()+"\" />\n";
	body += "\t\t<accountremoval type=\""+getAccountRemovalType()+"\" />\n\n";
	body += "\t\t<loginsystem type=\""+getLoginSystemType()+"\" />\n";
	body += "\t\t<loginmode type=\""+getLoginModeType()+"\" />\n";
	body += "\t\t<logoutmode type=\""+getLogoutModeType()+"\" />\n";
	body += "\t\t<guestreadmode type=\""+getGuestReadModeType()+"\" />\n";
	body += "\t\t<guestwritemode type=\""+getGuestWriteModeType()+"\" />\n\n";
	body += "\t\t<avatars>\n";
	body += "\t\t\t<upload enable=\""+getAvatarsUploadEnabled()+"\"/>\n";
	body += "\t\t\t<gallery enable=\""+getAvatarsGalleryEnabled()+"\"/>\n";
	body += "\t\t</avatars>\n\n";

	body += "\t\t<email from=\""+getFromEmailAddress()+"\"/>\n\n";
	body += "\t\t<emailtexts>\n";
	Iterator i=emailtexts.keySet().iterator();
        while (i.hasNext()) {
            String role=(String)i.next();
            String text=(String)emailtexts.get(role);
            body += "\t\t\t<emailtext role=\""+role+"\" >"+text+"</emailtext>\n";
	}
	body += "\t\t</emailtexts>\n";
	body += "\t\t <layout>\n";
	body += "\t\t\t<header path=\""+getHeaderPath()+"\"/>\n";
	body += "\t\t\t<footer path=\""+getFooterPath()+"\"/>\n";
	body += "\t\t</layout>\n\n";
	body += "\t\t<filters>\n";
	i=filterwords.keySet().iterator();
        while (i.hasNext()) {
            String replace=(String)i.next();
            String with=(String)filterwords.get(replace);
            body += "\t\t\t<filter type=\"content\" replace=\""+replace+"\" with=\""+with+"\" />\n";
	}
	body += "\t\t</filters>\n";

	body += "\t\t<xslts>\n";
	body += "\t\t\t<postings odd=\""+getXSLTPostingsOdd()+"\" even=\""+getXSLTPostingsEven()+"\" />\n";
	body += "\t\t</xslts>\n\n";

	body += "\t\t<contactinfo enable=\""+getContactInfoEnabled()+"\"/>\n";
	body += "\t\t<smileys enable=\""+getSmileysEnabled()+"\"/>\n";
	body += "\t\t<privatemessages enable=\""+getPrivateMessagesEnabled()+"\"/>\n";
	body += "\t\t<postingsperpage value=\""+getPostingsPerPage()+"\"/>\n\n";
	body += "\t\t<postingsoverflowpostarea value=\""+getPostingsOverflowPostArea()+"\"/>\n\n";
	body += "\t\t<postingsoverflowthreadpage value=\""+getPostingsOverflowThreadPage()+"\"/>\n\n";
	body += "\t\t<speedposttime value=\""+getSpeedPostTime()+"\"/>\n\n";
	body += "\t\t<replyoneachpage value=\""+getReplyOnEachPage()+"\"/>\n\n";
	body += "\t\t <navigation method=\""+getNavigationMethod()+"\"/>\n\n";

	// now loop all the forums
        for (Enumeration forums = ForumManager.getForums(); forums.hasMoreElements();) {
            Forum forum = (Forum) forums.nextElement();
            if (forum.getAlias()!=null) {
		if (forum.getCloneMaster()) {
                    body += "\t\t<forum id=\""+forum.getName()+"\" language=\""+forum.getLanguage()+"\" alias=\""+forum.getAlias()+"\" clonemaster=\"true\" >\n";
		} else {
                    body += "\t\t<forum id=\""+forum.getName()+"\" language=\""+forum.getLanguage()+"\" alias=\""+forum.getAlias()+"\">\n";
		}
            } else {
	     	body += "\t\t<forum id=\""+forum.getName()+"\" language=\""+forum.getLanguage()+"\">\n";
            }
            if (forum.getGuiEdit("loginsystem")==null || forum.getGuiEdit("loginsystem").equals("true")) {
                body += "\t\t\t<loginsystem type=\""+forum.getLoginSystemType()+"\" />\n";
	    } else {
                body += "\t\t\t<loginsystem type=\""+forum.getLoginSystemType()+"\" guiedit=\""+forum.getGuiEdit("loginsystem")+"\" />\n";
	    }
            if (forum.getGuiEdit("loginmode")==null || forum.getGuiEdit("loginmode").equals("true")) {
	     	body += "\t\t\t<loginmode type=\""+forum.getLoginModeType()+"\" />\n";
            } else {
	     	body += "\t\t\t<loginmode type=\""+forum.getLoginModeType()+"\" guiedit=\""+forum.getGuiEdit("loginmode")+"\" />\n";
            }

            if (forum.getGuiEdit("logoutmode")==null || forum.getGuiEdit("logoutmode").equals("true")) {
	     	body += "\t\t\t<logoutmode type=\""+forum.getLogoutModeType()+"\" />\n";
            } else {
	     	body += "\t\t\t<logoutmode type=\""+forum.getLogoutModeType()+"\" guiedit=\""+forum.getGuiEdit("logoutmode")+"\" />\n";
            }

            if (forum.getGuiEdit("guestreadmode")==null || forum.getGuiEdit("guestreadmode").equals("true")) {
    	        body += "\t\t\t<guestreadmode type=\""+forum.getGuestReadModeType()+"\" />\n";
            } else {
    	        body += "\t\t\t<guestreadmode type=\""+forum.getGuestReadModeType()+"\" guiedit=\""+forum.getGuiEdit("guestreadmode")+"\" />\n";
            }

            if (forum.getGuiEdit("guestwritemode")==null || forum.getGuiEdit("guestwritemode").equals("true")) {
	     	body += "\t\t\t<guestwritemode type=\""+forum.getGuestWriteModeType()+"\" />\n\n";
            } else {
	     	body += "\t\t\t<guestwritemode type=\""+forum.getGuestWriteModeType()+"\" guiedit=\""+forum.getGuiEdit("guestwritemode")+"\" />\n";
            }

            Iterator pi=forum.getProfileDefs();
            if (pi!=null) {
                while (pi.hasNext()) {
                    ProfileEntryDef pd = (ProfileEntryDef)pi.next();
                    body += "\t\t\t<profileentry name=\""+pd.getName()+"\" guiname=\""+pd.getGuiName()+"\" guipos=\""+pd.getGuiPos()+"\" edit=\""+pd.getEdit()+"\" type=\""+pd.getType()+"\" size=\""+pd.getSize()+"\" external=\""+pd.getExternalString()+"\" externalname=\""+pd.getExternalNameString()+"\" />\n";
                }
            }
            body += "\t\t\t<avatars>\n\n";
            if (forum.getGuiEdit("avatarsupload")==null || forum.getGuiEdit("avatarsupload").equals("true")) {
	     	body += "\t\t\t\t<upload enable=\""+forum.getAvatarsUploadEnabled()+"\"/>\n";
            } else {
	     	body += "\t\t\t\t<upload enable=\""+forum.getAvatarsUploadEnabled()+"\" guiedit=\""+forum.getGuiEdit("avatarsupload")+"\" />\n";
            }
            if (forum.getGuiEdit("avatarsgallery")==null || forum.getGuiEdit("avatarsgallery").equals("true")) {
	     	body += "\t\t\t\t<gallery enable=\""+forum.getAvatarsGalleryEnabled()+"\"/>\n";
            } else {
	     	body += "\t\t\t\t<gallery enable=\""+forum.getAvatarsGalleryEnabled()+"\" guiedit=\""+forum.getGuiEdit("avatarsgallery")+"\" />\n";
            }
            body += "\t\t\t</avatars>\n\n";
            body += "\t\t\t<postingsperpage value=\""+forum.getPostingsPerPage()+"\"/>\n\n";
            body += "\t\t\t<postingsoverflowpostarea value=\""+forum.getPostingsOverflowPostArea()+"\"/>\n\n";
            body += "\t\t\t<postingsoverflowthreadpage value=\""+forum.getPostingsOverflowThreadPage()+"\"/>\n\n";
            body += "\t\t\t<speedposttime value=\""+forum.getSpeedPostTime()+"\"/>\n\n";
            body += "\t\t\t<replyoneachpage value=\""+forum.getReplyOnEachPage()+"\"/>\n\n";
            if (forum.getGuiEdit("navigationmethod")==null || forum.getGuiEdit("navigationmethod").equals("true")) {
	     	body += "\t\t\t <navigation method=\""+forum.getNavigationMethod()+"\"/>\n\n";
            } else {
	     	body += "\t\t\t <navigation method=\""+forum.getNavigationMethod()+"\" guiedit=\""+forum.getGuiEdit("navigationmethod")+"\" />\n\n";
            }
            for (Enumeration postareas = forum.getPostAreas(); postareas.hasMoreElements();) {
             	PostArea postarea = (PostArea) postareas.nextElement();
	        body += "\t\t\t<postarea id=\""+postarea.getName()+"\" language=\"nl\" pos=\""+postarea.getPos()+"\">\n";
    	        body += "\t\t\t\t<guestreadmode type=\""+postarea.getGuestReadModeType()+"\" />\n";
	        body += "\t\t\t\t<guestwritemode type=\""+postarea.getGuestWriteModeType()+"\" />\n\n";
		if (postarea.getThreadStartLevel()!=null) {
                    body += "\t\t\t\t<threadstart level=\""+postarea.getThreadStartLevel()+"\" />\n\n";
		}
		body += "\t\t\t</postarea>\n";
            }
            body += "\t\t</forum>\n\n";
	}
	body += "\t</forums>\n";
	body += "</mmbobconfig>\n";
	try {
            Writer wr = ResourceLoader.getConfigurationRoot().getWriter("mmbob/mmbob.xml");
            wr.write(body);
            wr.flush();
            wr.close();
	} catch(Exception e) {
            e.printStackTrace();
	}
    }


    private void checkCloudModel() {
        checkRelDef("related", "related", 2, "Related", "Relared", "insrel");
        checkRelDef("posrel", "posrel", 2, "Posrel", "Posrel", "posrel");
        checkRelDef("rolerel", "rolerel", 2, "RoleRel", "RoleRel", "rolerel");
        checkRelDef("posmboxrel", "posmboxrel", 2, "PosMBoxRel", "PosMBoxRel", "posmboxrel");
        checkRelDef("forposrel", "forposrel", 2, "ForPosRel", "ForPosRel", "forposrel");
        checkRelDef("forarearel", "forerearel", 2, "ForAreaRel", "ForAreaRel", "forarearel");
        checkRelDef("areathreadrel", "areathreadrel", 2, "AreaThreadRel", "AreaThreadRel", "areathreadrel");
    	checkTypeRel("forummessagebox", "forumprivatemessage", "related",-1);
    	checkTypeRel("postareas", "posters", "rolerel",-1);
    	checkTypeRel("images", "posters", "rolerel",-1);
    	checkTypeRel("postthreads", "postings", "related",-1);
    	checkTypeRel("posters", "avatarsets", "related",-1);
    	checkTypeRel("avatarsets", "images", "posrel",-1);
    	checkTypeRel("posters", "forummessagebox", "posmboxrel",-1);
    	checkTypeRel("forums", "posters", "forposrel",-1);
    	checkTypeRel("forums", "posters", "rolerel",-1);
    	checkTypeRel("forums", "postareas", "forarearel",-1);
    	checkTypeRel("postareas", "postthreads", "areathreadrel",-1);
    	checkTypeRel("posters", "signatures", "related",-1);
    	checkTypeRel("forums", "forumrules", "related",-1);
    	checkTypeRel("postareas", "forumrules", "related",-1);
    	checkTypeRel("posters", "remotehosts", "related",-1);
    	checkTypeRel("postthreads", "threadobservers", "related",-1);
    	checkTypeRel("posters", "profileinfo", "related",-1);
    }


    /**
     * Checks and if required installs an allowed type relation (typerel object).
     *
     * @param  sname  source type name of the type relation
     * @param  dname  destination type name of the type relation
     * @param  rname  role name of the type relation
     * @param  count  cardinality of the type relation
     * @return        <code>true</code> if succesfull, <code>false</code> if an error occurred
     */
    private boolean checkTypeRel(String sname, String dname, String rname, int count) {
        MMBase mmb = MMBase.getMMBase();
        TypeRel typerel = mmb.getTypeRel();
        if (typerel != null) {
            TypeDef typedef = mmb.getTypeDef();
            if (typedef == null) {
                //return result.error("Can't get typedef builder");
                return false;
            }
            RelDef reldef = mmb.getRelDef();
            if (reldef == null) {
                //return result.error("Can't get reldef builder");
                return false;
            }

            // figure out rnumber
            int rnumber = reldef.getNumberByName(rname);
            if (rnumber == -1) {
                //return result.error("No reldef with role '"+rname+"' defined");
                return false;
            }

            // figure out snumber
            int snumber = typedef.getIntValue(sname);
            if (snumber == -1) {
                //return result.error("No builder with name '"+sname+"' defined");
                return false;
            }

            // figure out dnumber
            int dnumber = typedef.getIntValue(dname);
            if (dnumber == -1) {
                //return result.error("No builder with name '"+dname+"' defined");
                return false;
            }

            if (!typerel.contains(snumber, dnumber, rnumber, TypeRel.STRICT)) {
                MMObjectNode node = typerel.getNewNode("system");
                node.setValue("snumber", snumber);
                node.setValue("dnumber", dnumber);
                node.setValue("rnumber", rnumber);
                node.setValue("max", count);
                int id = typerel.insert("system", node);
                if (id != -1) {
                    log.info("TypeRel (" + sname + "," + dname + "," + rname + ") installed");
                } else {
                    log.info("TypeRel (" + sname + "," + dname + "," + rname + ") not installed");
                    return false;
                }
            }
            return true;
        } else {
	    log.info("Can't get the typerel builder");
            return false;
        }
    }


    /**
     * Checks whether a given relation definition exists, and if not, creates that definition.
     *
     * @param  sname     source name of the relation definition
     * @param  dname     destination name of the relation definition
     * @param  dir       directionality (uni or bi)
     * @param  sguiname  source GUI name of the relation definition
     * @param  dguiname  destination GUI name of the relation definition
     * @param  builder   references the builder to use (only in new format)
     * @param  step      Description of the Parameter
     * @return           <code>true</code> if succesfull, <code>false</code> if an error occurred
     */
    private boolean checkRelDef(String sname, String dname, int dir, String sguiname, String dguiname, String buildername) {
        MMBase mmb = MMBase.getMMBase();
        int builder = mmb.getTypeDef().getIntValue(buildername);
        RelDef reldef = mmb.getRelDef();
        if (reldef != null) {
            log.debug("checking reldef " + sname + "/" + dname + " ..");
            if (reldef.getNumberByName(sname + "/" + dname) == -1) {
                MMObjectNode node = reldef.getNewNode("system");
                node.setValue("sname", sname);
                node.setValue("dname", dname);
                node.setValue("dir", dir);
                node.setValue("sguiname", sguiname);
                node.setValue("dguiname", dguiname);
                if (reldef.usesbuilder) {
                    // if builder is unknown (falsely specified), use the InsRel builder
                    if (builder <= 0) {
                        builder = mmb.getInsRel().getObjectType();
                        //			log.info("UNFIXED 1.8 port problem ");
                    }
                    node.setValue("builder", builder);
                }
                int id = reldef.insert("system", node);
                if (id != -1) {
                    log.info("checking reldef " + sname + "/" + dname + " ..installed");
                } else {
                    log.info("checking reldef " + sname + "/" + dname + " .. not installed");
                    return false;
                }
            } else {
                log.debug("checking reldef " + sname + "/" + dname + " .. allready installed");
            }
        } else {
	    log.info("Can't use reldef !");
            return false;
        }
        return true;
    }

    public HashMap getFilterWords() {
	return filterwords;
    }

    public void addWordFilter(String name,String value) {
	filterwords.put(name,value);
    }

    public void removeWordFilter(String name) {
	filterwords.remove(name);
    }

    public int getSpeedPostTime() {
	return speedposttime;
    }

    public int getPostingsOverflowPostArea() {
	return postingsoverflowpostarea;
    }

    public int getPostingsOverflowThreadPage() {
	return postingsoverflowthreadpage;
    }

    public String getNavigationMethod() {
	return navigationmethod;
    }

    public String getEmailtext(String role) {
	Object o = emailtexts.get(role);
	if (o!=null) return (String)o;
	return null;
    }

    public String getExternalRootUrl() {
	return externalrooturl;
    }

    public String getRootUrl() {
	return rooturl;
    }

    public boolean getReplyOnEachPage() {
	return replyoneachpage;
    }
}

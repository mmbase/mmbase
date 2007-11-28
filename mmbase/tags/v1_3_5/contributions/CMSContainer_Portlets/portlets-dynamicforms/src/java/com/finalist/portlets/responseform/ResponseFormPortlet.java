/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package com.finalist.portlets.responseform;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import net.sf.mmapps.commons.bridge.RelationUtil;
import net.sf.mmapps.commons.util.StringUtil;
import net.sf.mmapps.modules.cloudprovider.CloudProvider;
import net.sf.mmapps.modules.cloudprovider.CloudProviderFactory;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.portlet.*;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;

import org.mmbase.bridge.Cloud;
import org.mmbase.bridge.Node;
import org.mmbase.bridge.NodeIterator;
import org.mmbase.bridge.NodeList;
import org.mmbase.bridge.NodeManager;
import org.mmbase.bridge.util.SearchUtil;
import org.mmbase.remotepublishing.PublishManager;
import org.mmbase.remotepublishing.util.PublishUtil;

import com.finalist.cmsc.mmbase.PropertiesUtil;
import com.finalist.cmsc.navigation.ServerUtil;
import com.finalist.cmsc.portlets.ContentPortlet;
import com.finalist.pluto.portalImpl.core.CmscPortletMode;


public class ResponseFormPortlet extends ContentPortlet {

    protected static final String PARAMETER_MAP = "parameterMap";
    protected static final String ERRORMESSAGES = "errormessages";

    private static final int DEFAULT_MAXFILESIZE = 2;  // default file size in Meg
    private static final long MEGABYTE = 1024 * 1024;  // 1 Meg
    private static final String FIELD_PREFIX = "field_";
    private static final int TYPE_TEXTBOX = 1;
    private static final int TYPE_TEXTAREA = 2;   
    private static final int TYPE_RADIO = 4;  
    private static final int TYPE_CHECKBOX = 6;
    private static final int TYPE_ATTACHEMENT = 7;
    private static final String CHECKBOX_NO = "nee";
    private static final String CHECKBOX_YES = "ja";
    private static final String RADIO_EMPTY = "[niets gekozen]";
    private static final String TEXTBOX_EMPTY = "[niet ingevuld]";
    private static final int TEXTAREA_MAXLENGTH = 1024;   
    private static final String DEFAULT_EMAILREGEX = "^([a-zA-Z0-9_.-])+@(([a-zA-Z0-9-])+.)+([a-zA-Z0-9]{2,4})+$";
      
    public void processView(ActionRequest request, ActionResponse response) throws PortletException, IOException {
    	Map<String, String> errorMessages = new Hashtable<String, String>();  
    	Map<String, String> parameterMap = new HashMap<String, String>();
    	DataSource  attachment = processUserRequest(request, errorMessages, parameterMap); 
    	String action = request.getParameter(ACTION_PARAM);
        Map<String, String> formfields = new HashMap<String, String>();        
        StringBuffer data = new StringBuffer();
        
        if (action == null) {
            response.setPortletMode(CmscPortletMode.EDIT_DEFAULTS);
        } else if (action.equals("edit")) {
            PortletPreferences preferences = request.getPreferences();
            String contentelement = preferences.getValue(CONTENTELEMENT, null);
            
            if (contentelement != null) {            	
            	if (parameterMap.size() > 0) {
	                CloudProvider cloudProvider = CloudProviderFactory.getCloudProvider();
	                Cloud cloud = cloudProvider.getCloud();
	                Node responseForm = cloud.getNode(contentelement); 	               
	                NodeList formfieldList = SearchUtil.findRelatedOrderedNodeList(responseForm, "formfield", "posrel", "posrel.pos");	                
	                NodeIterator formfieldIterator = formfieldList.nodeIterator(); 
	                String userEmailAddress = null;    
	                
	                while (formfieldIterator.hasNext()) {                	
	                	Node formfield = formfieldIterator.nextNode();
	                	String number = formfield.getStringValue("number");                	     
	                    String label = formfield.getStringValue("label");	                   
	                    int type = formfield.getIntValue("type");
	                    String regex = formfield.getStringValue("regex");
	                    boolean isMandatory = formfield.getBooleanValue("mandatory");                                    
	                    boolean sendEmail = formfield.getBooleanValue("sendemail");
	                    String fieldIdentifier = FIELD_PREFIX + number;
	                    String value = parameterMap.get(fieldIdentifier);   
	                    String textValue = null;  
	                    if (sendEmail) {
	                    	userEmailAddress = value;
	                    }                      
	                    if (type == TYPE_TEXTAREA && value != null && value.length() >= TEXTAREA_MAXLENGTH) {
	                    	value = value.substring(0, TEXTAREA_MAXLENGTH - 1);
	                    }                    
	                    if (isMandatory && (((type == TYPE_RADIO || type == TYPE_CHECKBOX) && (value == null)) 
	                    		|| (((type == TYPE_TEXTBOX) || (type == TYPE_TEXTAREA) || (type == TYPE_ATTACHEMENT)) && value.equals("")))) {  
	                       errorMessages.put(fieldIdentifier, "view.formfield.empty");
	                    }                    
	                    if (!regex.equals("") && (((type == TYPE_TEXTBOX) || (type == TYPE_TEXTAREA)) && !value.matches(regex))) {
	                    	errorMessages.put(fieldIdentifier, "view.formfield.invalid");
	                    }                    
	                    if (type == TYPE_CHECKBOX) {                       
	                    	textValue = (value == null) ? CHECKBOX_NO : CHECKBOX_YES; 	                    	
	                    }
	                    else if (type == TYPE_RADIO) {
	                    	textValue = (value == null) ? RADIO_EMPTY : value; 
	                    }                    
	                    else {                       
	                    	textValue = (StringUtil.isEmptyOrWhitespace(value)) ? TEXTBOX_EMPTY : value;
	                    }  	                    
	                    addFormFieldsData(data, label, textValue);
	                    formfields.put(number, textValue);
	                }      
	                
	                if (errorMessages.size() == 0) {      
	                	boolean saveAnswer = responseForm.getBooleanValue("saveanswer"); 
	                	if (saveAnswer) { 
	                		saveResponseForm(cloud, formfields, responseForm);
	                	}
	                	String emailData = data.toString();
	                	boolean sent = sendResponseFormEmail(responseForm, userEmailAddress, emailData, attachment); 
	                    if (!sent) {
	                		errorMessages.put("sendemail", "view.error.sendemail");
	                	}        
	                    else {
	                    	// if the responseform email has been sent, send also the email to the user
	                    	sendUserEmail(responseForm, userEmailAddress, emailData, parameterMap);                      	
	                    }                    	
	                } 
            	}
                if (errorMessages.size() > 0) {
                	request.getPortletSession().setAttribute(ERRORMESSAGES, errorMessages);
                	request.getPortletSession().setAttribute(PARAMETER_MAP, parameterMap);  
                }
                else {                	
                	request.getPortletSession().setAttribute("confirm", "confirm");
                }                 
            } 
            else {
                getLogger().error("No contentelement");
            }            
            // switch to View mode
            response.setPortletMode(PortletMode.VIEW);            
        } else {
            getLogger().error("Unknown action: '" + action + "'");
        }
    }
    
    private void addFormFieldsData(StringBuffer data, String label, String textValue) {
    	 data.append(label);
         data.append(": ");
         data.append(textValue);
         data.append(System.getProperty("line.separator"));		
	}	

	private void saveResponseForm(Cloud cloud, Map<String, String> formfields, Node responseForm) {		
				
		NodeManager savedFormMgr = cloud.getNodeManager("savedform");     	
    	Node savedResponse = savedFormMgr.createNode();
    	savedResponse.commit();    
    	
    	RelationUtil.createRelation(responseForm, savedResponse, "posrel");
    	if (ServerUtil.isLive()) {
    		PublishUtil.publishOrUpdateNode(savedResponse);
    	}
        
    	NodeManager savedFieldMgr = cloud.getNodeManager("savedfieldvalue"); 
        Iterator<String> keyIterator = formfields.keySet().iterator();	
        
    	while(keyIterator.hasNext()) {
    		String key = keyIterator.next();
    		String formFieldNumber = key; // key represents the node number of the form field from staging
    		if (ServerUtil.isLive()) {
    			Node liveFormFieldNode = cloud.getNode(key);
    			Node stagingFormFieldNode = PublishManager.getSourceNode(liveFormFieldNode); 
    			formFieldNumber = String.valueOf(stagingFormFieldNode.getNumber());
    		}    		
	        Node savedFieldValue = savedFieldMgr.createNode();
	        savedFieldValue.setStringValue("field", formFieldNumber);
	        savedFieldValue.setStringValue("value", formfields.get(key));
	        savedFieldValue.commit();	
	        
	        RelationUtil.createRelation(savedResponse, savedFieldValue, "posrel");
	        if (ServerUtil.isLive()) {
	        	PublishUtil.publishOrUpdateNode(savedFieldValue);
	        }
	       
    	}		
	}

	protected void sendUserEmail(Node responseform, String userEmailAddress,String responseformData, Map<String,String> parameterMap) { 			
    	String userEmailSubject = responseform.getStringValue("useremailsubject");
        String userEmailSender = responseform.getStringValue("useremailsender");
        String userEmailSenderName = responseform.getStringValue("useremailsendername");
        String userEmailTextBefore = responseform.getStringValue("useremailbody");
        String userEmailTextAfter = responseform.getStringValue("useremailbodyafter");
        boolean includedata = responseform.getBooleanValue("includedata");
        StringBuffer userEmailText = new StringBuffer();	
        
    	userEmailTextBefore = userEmailTextBefore.trim();    
    	userEmailText.append(userEmailTextBefore);
    	if (includedata) {
    		userEmailText.append(System.getProperty("line.separator"));	
    		userEmailText.append(responseformData);    		
    	}
    	if (userEmailTextAfter != null) {
    		userEmailTextAfter = userEmailTextAfter.trim();
    		userEmailText.append(System.getProperty("line.separator"));	
    		userEmailText.append(userEmailTextAfter);
    	}
    	
        String emailRegex = getEmailRegex();
        if (!StringUtil.isEmptyOrWhitespace(userEmailSender) 
        		&& !StringUtil.isEmptyOrWhitespace(userEmailText.toString()) 
        		&& !StringUtil.isEmptyOrWhitespace(userEmailSenderName) 
        		&& !StringUtil.isEmptyOrWhitespace(userEmailAddress)        		
        		&& userEmailAddress.matches(emailRegex)) {
	        try {
	        	EmailSender.getInstance().sendEmail(userEmailSender, userEmailSenderName, userEmailAddress, userEmailSubject, userEmailText.toString());	        	
			} catch (UnsupportedEncodingException e) {
				getLogger().error("error sending email", e);
			} catch (MessagingException e) {
				getLogger().error("error sending email", e);
			}    
        }	
	}

	private boolean sendResponseFormEmail(Node responseform, String userEmailAddress, String responseformData, DataSource attachment) {
		boolean sent = false;		
		StringBuffer emailText = new StringBuffer();
		
        String emailTextBefore = responseform.getStringValue("emailbody"); 
        String emailTextAfter = responseform.getStringValue("emailbodyafter"); 

        String senderEmail = userEmailAddress;
        String senderName = userEmailAddress;     
        if (StringUtil.isEmptyOrWhitespace(userEmailAddress)) {
            senderEmail = responseform.getStringValue("useremailsender");
            senderName = responseform.getStringValue("useremailsendername");
        }
        	             
        emailTextBefore = emailTextBefore.trim();    
    	emailText.append(emailTextBefore);
    	emailText.append(System.getProperty("line.separator"));	
    	emailText.append(responseformData);
    	if (emailTextAfter != null) {
    		emailTextAfter = emailTextAfter.trim();
    		emailText.append(System.getProperty("line.separator"));	
    		emailText.append(emailTextAfter);
    	}
      
        String emailAddressesValue = responseform.getStringValue("emailaddresses");                                         
        String emailSubject = responseform.getStringValue("emailsubject");    
        List<String> emailList = Arrays.asList(emailAddressesValue.split(";"));
        try {
			EmailSender.getInstance().sendEmail(senderEmail, senderName, emailList, emailSubject, emailText.toString(), attachment);
			sent = true;
		} catch (UnsupportedEncodingException e) {
			getLogger().error("error sending email", e);
		} catch (MessagingException e) {
			getLogger().error("error sending email", e);
		}    
		return sent;		
	}	

	@Override
    protected void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        PortletPreferences preferences = request.getPreferences();
        String contentelement = preferences.getValue(CONTENTELEMENT, null);
        PortletSession portletSession = request.getPortletSession();

        if (contentelement != null) {        	
        	if (portletSession.getAttribute("confirm") != null) {
        		String confirm = (String)portletSession.getAttribute("confirm");
        		portletSession.removeAttribute("confirm");
            	request.setAttribute("confirm", confirm);
        	}        	
        	if (portletSession.getAttribute(ERRORMESSAGES) != null) {
        		Map<String,String> errormessages = (Map<String,String>) portletSession.getAttribute(ERRORMESSAGES);
        		portletSession.removeAttribute(ERRORMESSAGES);
            	request.setAttribute(ERRORMESSAGES, errormessages);
        	}        	
        	if (portletSession.getAttribute(PARAMETER_MAP) != null) {
        		Map<String,String> parameterMap = (Map<String,String>) portletSession.getAttribute(PARAMETER_MAP);
        		portletSession.removeAttribute(PARAMETER_MAP);
            	Iterator<String> keyIterator = parameterMap.keySet().iterator();
	        	while (keyIterator.hasNext()) {
	    			String keyValue = keyIterator.next(); 
	    			String entryValue = parameterMap.get(keyValue);	    			
	    			request.setAttribute(keyValue, entryValue);
	    		}    	            	
        	}

        } else {
            getLogger().error("No contentelement");
        }
        super.doView(request, response);
    }

	public void processEdit(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        super.processEdit(request, response);
        
        String action = request.getParameter(ACTION_PARAM);
        if (action == null) {
            response.setPortletMode(PortletMode.EDIT);
        } else if (action.equals("delete")) {
            String deleteNumber = request.getParameter("deleteNumber");
            CloudProvider cloudProvider = CloudProviderFactory.getCloudProvider();
            Cloud cloud = cloudProvider.getCloud();
            Node element = cloud.getNode(deleteNumber);
            element.delete(true);
        }
    }    
     
    private DataSource processUserRequest(ActionRequest request, Map<String, String> errorMessages, Map<String, String> parameterMap) {
    	List<FileItem> fileItems = null;
    	DataSource attachment = null;
        String encoding = "UTF-8";
        try {
        	DiskFileItemFactory factory = new DiskFileItemFactory();            
        	PortletFileUpload upload = new PortletFileUpload(factory);            
        	upload.setHeaderEncoding(encoding);
            fileItems = upload.parseRequest(request);
        } catch (FileUploadException e) {
        	getLogger().error("error parsing request", e);
        	errorMessages.put("sendemail", "view.error.sendemail");
        }
        if (fileItems != null ) { 
	        Iterator<FileItem> itFileItems = fileItems.iterator();
	        while (itFileItems.hasNext()) {
	           FileItem fileItem = itFileItems.next();           
	           if(fileItem.isFormField()) {          	   
                  try {
	                 parameterMap.put(fileItem.getFieldName(), fileItem.getString(encoding));
                  }
                  catch (UnsupportedEncodingException e) {
                     getLogger().error("UnsupportedEncoding " + encoding);
                  }
	           }
	           else { 	
	        	   if (!StringUtil.isEmptyOrWhitespace(fileItem.getName())) {	        		   
	        		   if (fileItem.getSize() <= getMaxAttachmentSize()) {
	        			   attachment = new WrappedFileItem(fileItem);
	        		   }
	        		   else {
	        			   errorMessages.put("sendemail", "view.error.filesize");
	        		   }
	        	   }
	        	   parameterMap.put(fileItem.getFieldName(), fileItem.getName());				
	           }	           
	        } 
        }
	    return attachment;
        
     } 

	private long getMaxAttachmentSize() {
		long maxFileSize = DEFAULT_MAXFILESIZE;
		String maxFileSizeValue = PropertiesUtil.getProperty("email.maxattachmentsize");
		if (!StringUtil.isEmptyOrWhitespace(maxFileSizeValue)) {
			try {
				maxFileSize = Integer.parseInt(maxFileSizeValue);				
			} catch (NumberFormatException e) {
				getLogger().info("incorrect value for email.maxattachmentsize=" + maxFileSizeValue + ", default value=" + DEFAULT_MAXFILESIZE + " is used");
			}
		}
		return maxFileSize * MEGABYTE;
	}  
	
	private String getEmailRegex() {
		String emailRegex = PropertiesUtil.getProperty("email.regex");
		if (!StringUtil.isEmptyOrWhitespace(emailRegex)) {
			return emailRegex;
		}
		else {
			return DEFAULT_EMAILREGEX;
		}		
	}  
    
}

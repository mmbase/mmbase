package com.finalist.newsletter.forms;

import com.finalist.cmsc.services.community.ApplicationContextFactory;
import com.finalist.cmsc.services.community.person.Person;
import com.finalist.cmsc.services.community.person.PersonService;
import com.finalist.cmsc.services.community.security.AuthenticationService;
import com.finalist.newsletter.domain.Subscription;
import com.finalist.newsletter.domain.Term;
import com.finalist.newsletter.services.NewsletterSubscriptionServices;
import com.finalist.newsletter.services.NewsletterService;
import com.finalist.cmsc.services.community.security.Authentication;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.*;
import org.apache.struts.upload.FormFile;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.struts.DispatchActionSupport;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class SubscriptionImportExportAction extends DispatchActionSupport {
   private static Log log = LogFactory.getLog(SubscriptionImportExportAction.class);
   private static final String PARAM_NEWSLETTERID = "newsletterId";

   private NewsletterSubscriptionServices subscriptionServices;
   private NewsletterService newsletterService;

   protected void onInit() {
      super.onInit();
      subscriptionServices = (NewsletterSubscriptionServices) getWebApplicationContext().getBean(
               "subscriptionServices");
      newsletterService = (NewsletterService) getWebApplicationContext().getBean("newsletterServices");
   }

   public ActionForward export(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                               HttpServletResponse response) throws IOException {

      log.debug("Export Susbscriptions");

      List<Subscription> subscriptions = new ArrayList<Subscription>();

      String type = request.getParameter("type");

      if (StringUtils.isNotBlank(type)) {
         String[] ids = request.getParameterValues("ids");
         for (String id : ids) {
            subscriptions.addAll(getSubscriptions(type, id));
         }
      } else {
         subscriptions = subscriptionServices.getAllSubscription();
      }

      String xml = getXStream().toXML(subscriptions);
      byte[] bytes = xml.getBytes();

      response.setContentType("text/xml");
      response.setContentLength(bytes.length);
      response.setHeader("Content-Disposition", "attachment; filename=subscriptions.xml");
      response.setHeader("Cache-Control", "no-store");

      response.flushBuffer();
      OutputStream outStream = response.getOutputStream();

      outStream.write(bytes);
      outStream.flush();

      return mapping.findForward(null);
   }

   public ActionForward importsubscription(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                           HttpServletResponse response) throws IOException {
      SubscriptionImportUploadForm myForm = (SubscriptionImportUploadForm) form;

      FormFile myFile = myForm.getDatafile();
      byte[] fileData = myFile.getFileData();
      String contentType = myFile.getContentType();
      String fileName = myFile.getFileName();

      boolean isXML = "text/xml".equals(contentType);
      boolean isPlain = "text/plain".equals(contentType);
      boolean isCSV = ".csv".endsWith(fileName.toLowerCase());

      ActionMessages messages = new ActionMessages();

      if (!isXML && !(isPlain & isCSV)) {
         messages.add("file", new ActionMessage("datafile.unsupport"));
         saveMessages(request, messages);
         return mapping.findForward("failed");
      }

      try {
         importFromFile(fileData);
      } catch (Exception e) {
         log.error(e);
         messages.add("file", new ActionMessage("datafile.invalid"));
         saveMessages(request, messages);
         return mapping.findForward("failed");
      }

      return mapping.findForward("success");
   }

   public ActionForward importUserSubScription(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                               HttpServletResponse response) throws FileNotFoundException, IOException {

      SubscriptionImportUploadForm myForm = (SubscriptionImportUploadForm) form;
      ActionMessages messages = new ActionMessages();
      FormFile myFile = myForm.getDatafile();
      boolean isCSV = myFile.getFileName().toLowerCase().endsWith(".csv");
      int newsletterId = Integer.parseInt((String) request.getParameter(PARAM_NEWSLETTERID));
      if (isCSV) {
         byte[] fileData = myFile.getFileData();
         String fileString = new String(fileData);
         BufferedReader br = new BufferedReader(new StringReader(fileString));
         String tmpLinsStr = br.readLine();
         PersonService personService = (PersonService) ApplicationContextFactory.getBean("personService");
         NewsletterSubscriptionServices subscriptionServices = (NewsletterSubscriptionServices) ApplicationContextFactory.getBean(
                  "subscriptionServices");

         while (tmpLinsStr != null) {
            String tmpUserInfo = tmpLinsStr.replaceAll("\"", "");
            String tmpUserName = tmpUserInfo.substring(0, tmpUserInfo.indexOf(","));
            String tmpUserEmail = tmpUserInfo.substring(tmpUserInfo.indexOf(",") + 1, tmpUserInfo.length());
            Person tmpPerson = personService.getPersonByEmail(tmpUserEmail);
            if (tmpPerson == null) {
               AuthenticationService as = getAuthenticationService();
               Authentication authentication = as.createAuthentication(tmpUserEmail, tmpUserEmail);
               Person person = personService.createPerson(tmpUserName, "", "", authentication.getId());
               person.setEmail(tmpUserEmail);
               personService.updatePerson(person);
               tmpPerson = personService.getPersonByEmail(tmpUserEmail);
            }
            int authId = tmpPerson.getAuthenticationId().intValue();
            Subscription subRet = subscriptionServices.getSubscription(authId, newsletterId);
            if (subRet == null) {
               subscriptionServices.createSubscription(authId, newsletterId);
            }
            tmpLinsStr = br.readLine();
         }
         return mapping.findForward("success");
      } else {
         request.setAttribute("importType", "importCSV");
         request.setAttribute(PARAM_NEWSLETTERID, newsletterId);
         messages.add("file", new ActionMessage("datafile.unsupport"));
         saveMessages(request, messages);
         return mapping.findForward("failed");
      }
   }

   private List<Subscription> getSubscriptions(String type, String id) {
      if ("person".equals(type)) {
         return subscriptionServices.getSubscriptionBySubscriber(id);
      }
      if ("newsletter".equals(type)) {
         return subscriptionServices.getSubscriptionsByNewsletterId(id);
      }
      if ("subscription".equals(type)) {
         return Collections.singletonList(subscriptionServices.getSubscription(id));
      }
      return Collections.emptyList();
   }

   private void importFromFile(byte[] fileData) throws Exception {
      String xml = new String(fileData);
      List<Subscription> subscriptionList;
      try {
         subscriptionList = (List<Subscription>) getXStream().fromXML(xml);
      } catch (Exception e) {
         throw new Exception(e);
      }

      for (Subscription subscription : subscriptionList) {
         Person subscrier = subscription.getSubscriber();
         int sbId = subscrier.getId().intValue();
         int nId = subscription.getNewsletter().getId();

         if (null == subscriptionServices.getSubscription(sbId, nId)) {

            log.debug(String.format("try to import user %s's subscription of %s which is not exist", sbId, nId));

            subscriptionServices.addNewRecord(sbId, nId);
         }
      }
   }

   private XStream getXStream() {
      XStream xstream = new XStream(new DomDriver());
      xstream.alias("person", Person.class);
      xstream.alias("subscription", Subscription.class);
      xstream.alias("term", Term.class);
      return xstream;
   }

   protected AuthenticationService getAuthenticationService() {
      WebApplicationContext ctx = getWebApplicationContext();
      return (AuthenticationService) ctx.getBean("authenticationService");
   }
}

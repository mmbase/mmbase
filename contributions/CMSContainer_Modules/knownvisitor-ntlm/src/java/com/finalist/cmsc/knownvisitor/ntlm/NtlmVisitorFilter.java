package com.finalist.cmsc.knownvisitor.ntlm;

import jcifs.http.NtlmSsp;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbSession;
import jcifs.smb.NtlmChallenge;
import jcifs.smb.SmbAuthException;
import jcifs.UniAddress;
import jcifs.Config;
import jcifs.util.Base64;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.FilterChain;
import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;
import javax.naming.NamingException;
import java.util.Map;
import java.io.IOException;
import java.security.Principal;

import org.mmbase.util.ApplicationContextReader;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

import com.finalist.cmsc.mmbase.PropertiesUtil;
import com.finalist.cmsc.portalImpl.PortalServlet;

/**
 * @author Freek Punt, Finalist IT Group
 * @author Jeoffrey Bakker, Finalist IT Group
 */
public class NtlmVisitorFilter implements Filter {
   private static final String PROPERTY_ENABLED = "knownvisitor-ntlm.enabled";
   private static final String PROPERTY_DOMAIN_CONTROLLER = "knownvisitor-ntlm.domaincontroller";

   private static final String realm = "jCIFS";


   private static Logger log = Logging.getLoggerInstance(NtlmVisitorFilter.class);


   public void init(FilterConfig filterConfig) throws ServletException {


      /* Set jcifs properties we know we want; soTimeout and cachePolicy to 10min.
      */
      Config.setProperty("jcifs.smb.client.soTimeout", "300000");
      Config.setProperty("jcifs.netbios.cachePolicy", "1200");

      try {
         Map<String, String> contextMap;
         contextMap = ApplicationContextReader.getProperties("cmsc/security");
         for (String name : contextMap.keySet()) {
            if (name.startsWith("jcifs.")) {
               Config.setProperty(name, contextMap.get(name));
            }
         }
      } catch (NamingException e) {
         log.warn("Can't obtain properties from application context: ", e);
      }
   }

   public void destroy() {
   }

   /**
    * This method simply calls <tt>negotiate( req, resp, false )</tt> and then <tt>chain.doFilter</tt>. You can override
    * and call negotiate manually to achive a variety of different behavior.
    */
   public void doFilter(ServletRequest request,
                        ServletResponse response,
                        FilterChain chain) throws IOException, ServletException {
      final HttpServletRequest req = (HttpServletRequest) request;
      final HttpServletResponse resp = (HttpServletResponse) response;
      final NtlmPasswordAuthentication ntlm;

      
      if (isEnabled() && PortalServlet.isNavigation(req, resp) && !negotiate(req, resp, false)) {
         return;
      }

      ntlm = (NtlmPasswordAuthentication) req.getSession().getAttribute("NtlmHttpAuth");
      
      chain.doFilter(req, resp);
      // LOGGED IN
/*      chain.doFilter(new HttpServletRequestWrapper(req) {

         public String getRemoteUser() {
            return (ntlm == null) ? null : ntlm.getName();
         }

         public Principal getUserPrincipal() {
            return ntlm;
         }

         public String getAuthType() {
            return ntlm == null ? req.getAuthType() : "NTLM";
         }
      }, resp);*/
   }

   /**
    * Negotiate password hashes with MSIE clients using NTLM SSP
    *
    * @param req The servlet request
    * @param resp The servlet response
    * @param skipAuthentication If true the negotiation is only done if it is initiated by the client (MSIE post
    * requests after successful NTLM SSP authentication). If false and the user has not been authenticated yet the
    * client will be forced to send an authentication (server sends HttpServletResponse.SC_UNAUTHORIZED).
    * @return True if the negotiation is complete, otherwise false
    */
   protected boolean negotiate(HttpServletRequest req,
                                                  HttpServletResponse resp,
                                                  boolean skipAuthentication) throws IOException, ServletException {
      UniAddress dc;
      String msg;
      NtlmPasswordAuthentication ntlm = null;
      msg = req.getHeader("Authorization");
      boolean offerBasic = req.isSecure();

      if (msg != null && (msg.startsWith("NTLM ") ||
              (offerBasic && msg.startsWith("Basic ")))) {
         if (msg.startsWith("NTLM ")) {
            HttpSession ssn = req.getSession();
            byte[] challenge;

            dc = UniAddress.getByName(getDomainController(), true);
            challenge = SmbSession.getChallenge(dc);

            if ((ntlm = NtlmSsp.authenticate(req, resp, challenge)) == null) {
               return false;
            }
            /* negotiation complete, remove the challenge object */
            ssn.removeAttribute("NtlmHttpChal");
         } else {
            String auth = new String(Base64.decode(msg.substring(6)),
                    "US-ASCII");
            int index = auth.indexOf(':');
            String user = (index != -1) ? auth.substring(0, index) : auth;
            String password = (index != -1) ? auth.substring(index + 1) :
                    "";
            index = user.indexOf('\\');
            if (index == -1) index = user.indexOf('/');
            String domain = user.substring(0, index);
            user = (index != -1) ? user.substring(index + 1) : user;
            ntlm = new NtlmPasswordAuthentication(domain, user, password);
            dc = UniAddress.getByName(getDomainController(), true);
         }
         try {

            SmbSession.logon(dc, ntlm);

            if (log.isDebugEnabled()) {
               log.debug("NtlmHttpFilter: " + ntlm +
                       " successfully authenticated against " + dc);
            }
         } catch (SmbAuthException sae) {
            if (log.isServiceEnabled()) {
               log.service("NtlmHttpFilter: " + ntlm.getName() +
                       ": 0x" + jcifs.util.Hexdump.toHexString(sae.getNtStatus(), 8) +
                       ": " + sae);
            }
            if (sae.getNtStatus() == sae.NT_STATUS_ACCESS_VIOLATION) {
               /* Server challenge no longer valid for
               * externally supplied password hashes.
               */
               HttpSession ssn = req.getSession(false);
               if (ssn != null) {
                  ssn.removeAttribute("NtlmHttpAuth");
               }
            }
            // lets try mmbase authentication now
            return true;
         }
         req.getSession().setAttribute("NtlmHttpAuth", ntlm);
      } else {
         if (!skipAuthentication) {
            HttpSession ssn = req.getSession(false);
            if (ssn == null || (ntlm = (NtlmPasswordAuthentication)
                    ssn.getAttribute("NtlmHttpAuth")) == null) {
               resp.setHeader("WWW-Authenticate", "NTLM");
               if (offerBasic) {
                  resp.addHeader("WWW-Authenticate", "Basic realm=\"" +
                          realm + "\"");
               }
               resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
               resp.setContentLength(0);
               resp.flushBuffer();
               return false;
            }
         }
      }

      return ntlm != null;
   }


   private boolean isEnabled() {
      return PropertiesUtil.getProperty(PROPERTY_ENABLED).equals("true");
   }
   
   private String getDomainController() {
      return PropertiesUtil.getProperty(PROPERTY_DOMAIN_CONTROLLER);
   }

}

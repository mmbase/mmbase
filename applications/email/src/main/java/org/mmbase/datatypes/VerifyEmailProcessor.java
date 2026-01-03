/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license
*/
package org.mmbase.datatypes;

import java.text.MessageFormat;
import java.util.*;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.mail.internet.InternetAddress;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.*;
import javax.servlet.jsp.PageContext;

import org.mmbase.bridge.*;
import org.mmbase.bridge.util.Queries;
import org.mmbase.core.event.EventManager;
import org.mmbase.datatypes.processors.CommitProcessor;
import org.mmbase.datatypes.processors.Processor;
import org.mmbase.storage.search.FieldCompareConstraint;
import org.mmbase.util.Casting;
import org.mmbase.util.GenericResponseWrapper;
import org.mmbase.util.HttpServletRequestUtils;
import org.mmbase.util.functions.Function;
import org.mmbase.util.functions.Parameters;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.transformers.Base64;
import org.mmbase.util.transformers.*;

/**
 * Using this class a Processor and as CommitProcessor on a certain field, adds 'email verification'
 * functionality for another field.
 *
 * If this other field is changed, then this change will be ignored but in stead be stored on this
 * 'verify email' field, together with a verification key.
 *
 * The verification can then be done by setting this key in this verify field (so, over the
 * implicitely stored value). If the key is then the same as already stored, then the new email
 * value is stored in the actual email field, and the verify field is emptied.
 *
 *
 * @author Michiel Meeuwissen
 * @version $Id$

 */

public class VerifyEmailProcessor implements CommitProcessor, Processor, java.io.Serializable {
    private static final Logger log = Logging.getLoggerInstance(VerifyEmailProcessor.class);
    private static final long serialVersionUID = 1L;

    private static final String SEP = ":";

    private static CharTransformer paramEscaper = new Url(Url.PARAM_ESCAPE);

    private static final String ENCRYPT_IMPL = "PBEWithMD5AndDES";
    private static final PBEParameterSpec PBE_PARAM_SPEC;
    private static SecretKey pbeKey = null;
    static {
        byte[] salt = {
            (byte)0xc7, (byte)0x73, (byte)0x21, (byte)0x8c,
            (byte)0x7e, (byte)0xc8, (byte)0xee, (byte)0x99
        };

        int iterationCount = 20;


        // Create PBE parameter set
        PBE_PARAM_SPEC = new PBEParameterSpec(salt, iterationCount);
    }

    private static long startTime = System.currentTimeMillis();

    private static int key = 0;

    protected static Set<String> verificationReceivers = new HashSet<String>();

    public static Set<String> getVerificationReceivers() {
        return Collections.unmodifiableSet(verificationReceivers);
    }

    private String emailField   = "email";
    private String emailTextBundle = "org.mmbase.datatypes.resources.verifyemailtemplate";
    private String successProcessor;
    private String url = "/mmbase/email/verify/";
    private String includeUrl = null; //"/core/mail.jsp";

    private boolean onlyForSelf = false;


    public void setOnlyForSelf(boolean o) {
        onlyForSelf = o;
    }


    public void setEmailField(String ef) {
        emailField = ef;
    }

    public void setTextBundle(String b) {
        if ("".equals(b)) b = null;
        emailTextBundle = b;
        String from = getResourceBundle(Locale.US).getString("from");
        if (from != null && ! "".equals(from)) {
            try {
                InternetAddress ia  = InternetAddress.parse(from)[0];
                verificationReceivers.add(ia.getAddress());
            } catch (Exception e) {
                log.error(e);
            }
        }

    }


    protected ResourceBundle getResourceBundle(Locale locale) {
        if (emailTextBundle != null) {
            return ResourceBundle.getBundle(emailTextBundle, locale);
        } else {
            return  new ListResourceBundle() {
                protected Object[][] getContents() {
                    return new Object[][] {
                        {"body", "{2}"},
                        {"subject", "{0}"},
                        {"from", "nobody@nowhere.org"}
                    };
                }
            };
        }


    }
    public void setSuccessProcessor(String p) {
        successProcessor = p;
    }
    public void setUrl(String u) {
        url = u;
    }

    public void setIncludeUrl(String u) {
        includeUrl = u;
        log.service("Will include " + includeUrl);
    }


    public static void setupEncryption() {
        if (pbeKey == null) {
            String password = null;
            try {
                password = ContextProvider.getDefaultCloudContext().getModule("sendmail").getProperty("verify_email.password");
            } catch (Exception e) {
                log.error(e);
            }
            if (password == null) password = "secret";
            try {
                PBEKeySpec pbeKeySpec   = new PBEKeySpec(password.toCharArray());
                SecretKeyFactory keyFac = SecretKeyFactory.getInstance(ENCRYPT_IMPL);
                pbeKey = keyFac.generateSecret(pbeKeySpec);
            } catch (Exception e) {
                log.error(e);
            }
        }
    }
    public static String encrypt(String string) {
        setupEncryption();
        try {
            Cipher pbeCipher = Cipher.getInstance(ENCRYPT_IMPL);
            pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey, PBE_PARAM_SPEC);
            Base64 base64 = new Base64();
            return base64.transform(pbeCipher.doFinal(string.getBytes()));
        } catch (Exception e) {
            log.error(e);
            return string;
        }
    }
    public static String decrypt(String string) {
        setupEncryption();
        try {
            Cipher pbeCipher = Cipher.getInstance(ENCRYPT_IMPL);
            pbeCipher.init(Cipher.DECRYPT_MODE, pbeKey, PBE_PARAM_SPEC);
            Base64 base64 = new Base64();
            return new String(pbeCipher.doFinal(base64.transformBack(string)));
        } catch (Exception e) {
            log.error(e);
            return string;
        }
    }


    protected String generateKey() {
        //todo:  perhaps something less obvious
        return "" + (startTime + key++);
    }
    public Object process(Node node, Field field, Object value) {
        String checkValue = node.getStringValue(field.getName());
        if (checkValue != null) {
            int pos = checkValue.indexOf(SEP);
            if (pos > 0) {
                String key   = checkValue.substring(0, pos);
                String email = checkValue.substring(pos + 1);
                if (value.equals(key)) {
                    node.setStringValue(emailField, email);
                    log.service("Verification of " + email + " succeeded");
                    if (successProcessor != null) {
                        try {
                            Processor success = (Processor) Class.forName(successProcessor).newInstance();
                            success.process(node, field, email);
                        } catch (Exception e) {
                            log.error(e);
                        }
                    }
                    return null;
                } else {
                    log.service("Verification of " + email + " failed (" + value + "!=" + key + ")");
                }
            }
        }
        return checkValue;
    }

    protected String encryptKey(Node node, Field field, String key) {
        return encrypt(node.getNodeManager().getName() + SEP + field.getName() + SEP + key);
    }


    /**
     * Call this method with the key received from the client to perform the actual validation. This
     * method decrypts and parses this key, and then sets the value.
     */
    public static Node validate(Cloud cloud, String encryptedKey) {
        String keyChain = decrypt(encryptedKey.replaceAll(" ", "+"));
        log.debug("Found keyChain " + keyChain + " (from " + encryptedKey + " ). User " + cloud.getUser());
        int pos1 = keyChain.indexOf(SEP);
        String nodeManager = keyChain.substring(0, pos1);
        int pos2 = keyChain.indexOf(SEP, pos1 + 1);
        String field = keyChain.substring(pos1 + 1, pos2);
        String fieldValue = keyChain.substring(pos2 + 1);
        String key = keyChain.substring(pos2 + 1);
        NodeManager nm = cloud.getNodeManager(nodeManager);
        NodeQuery query = nm.createQuery();

        Queries.addConstraint(query, Queries.createConstraint(query, field, FieldCompareConstraint.LIKE, key + ":%"));
        NodeList nl = nm.getList(query);
        if (nl.size() == 1) {
            Node node = nl.getNode(0);
            node.setStringValue(field, key);
            node.commit();
            EventManager.getInstance().propagateEvent(new EmailValidated(node.getNumber()));
            return node;
        }
        log.service("Not Found " + query.toSql());
        return null;
    }

    private static CharTransformer stripper;
    static {
        TagStripperFactory fac = new TagStripperFactory();
        stripper = (CharTransformer) fac.createTransformer(fac.createParameters());
    }
    public void commit(Node node, Field field) {
        if (log.isDebugEnabled()) {
            log.debug("Commit for " + node + " " + emailField + " " + node.getChanged());
        }
        if (onlyForSelf) {
            try {
                int myNode = node.getCloud().getCloudContext().getAuthentication().getNode(node.getCloud().getUser());
                log.debug("Comparing " + node.getNumber() + " with " + myNode);
                if (myNode != node.getNumber()) {
                    log.debug("Logged in as someone different. Ignoring");
                    return;
                }
            } catch (UnsupportedOperationException uoe) {
                log.debug(uoe);
                return;
            }
        }
        if ((node.getChanged().contains(emailField) && ! node.getChanged().contains(field.getName())) || node.isNew()) {
            String email = node.getStringValue(emailField);
            if ("".equals(email)) {
                log.debug("Email field is empty, cannot confirm that");
            } else {
                log.service("Sending confirmation email to '" + email + "'");
                String key = generateKey();
                String encryptedKey = encryptKey(node, field, key);

                node.setValueWithoutProcess(field.getName(), key + SEP + email);
                Object originalValue = node.isNew() ? null :
                    org.mmbase.module.core.MMBase.getMMBase().getBuilder("object").getNode(node.getNumber()).getValues().get(emailField);
                log.debug("Found original value " + originalValue);
                if (originalValue != null) {
                    node.setValueWithoutProcess(emailField, originalValue);
                }
                ;
                log.debug("Setting " + key + SEP + email + " in " + field);


                Cloud cloud = node.getCloud().getNonTransactionalCloud();

                // Send an email.
                Locale locale = cloud.getLocale();

                ResourceBundle emailTemplate = getResourceBundle(locale);

                if (log.isDebugEnabled()) {
                    log.debug("Found email template " + emailTemplate + " " + emailTemplate.getLocale());
                    for (String k : Collections.list(emailTemplate.getKeys())) {
                        log.debug(k + "=" + emailTemplate.getString(k));
                    }
                }

                org.mmbase.bridge.Module emailModule   = cloud.getCloudContext().getModule("sendmail");


                NodeManager emailBuilder = cloud.getNodeManager(emailModule.getProperty("emailbuilder"));

                Node emailNode = emailBuilder.createNode();

                String toField       = emailModule.getProperty("emailbuilder.tofield");
                String subjectField  = emailModule.getProperty("emailbuilder.subjectfield");
                String bodyField     = emailModule.getProperty("emailbuilder.bodyfield");
                String fromField     = emailModule.getProperty("emailbuilder.fromfield");

                HttpServletRequest req = (HttpServletRequest) cloud.getProperty("request");
                StringBuilder u = new StringBuilder();


                if (req != null) {
                    String scheme = HttpServletRequestUtils.getScheme(req);
                    u.append(scheme).append("://");
                    u.append(HttpServletRequestUtils.getServerName(req));
                    int port = HttpServletRequestUtils.getServerPort(req);
                    u.append((port == 80 && "http".equals(scheme)) ||
                             (port == 443 && "https".equals(scheme))
                             ? "" : ":" + port);
                    u.append(req.getContextPath());


                }
                u.append(url);
                String sep = url.indexOf("?") > 0 ? "&amp;" : "?";
                u.append(sep);
                u.append("signature=" + paramEscaper.transform(encryptedKey));

                emailNode.setStringValue(toField, email);

                emailNode.setStringValue(subjectField, MessageFormat.format(emailTemplate.getString("subject"), encryptedKey));


                String from = emailTemplate.getString("from");

                if (! "".equals(from)) {
                    emailNode.setStringValue(fromField, from);
                }


                StringBuilder include = new StringBuilder();
                if (req != null) {
                    log.debug("Including " + includeUrl);
                    CharTransformer escaper = (CharTransformer) req.getAttribute("org.mmbase.bridge.jsp.taglib.escaper");
                    req.setAttribute("_node", Casting.wrap(emailNode, escaper));
                    req.setAttribute("fieldNode", Casting.wrap(node, escaper));
                    req.setAttribute("signature", encryptedKey);
                    req.setAttribute("url", u.toString());
                    if (includeUrl != null && ! "".equals(includeUrl)) {
                        try {
                            PageContext pageContext = (PageContext) (Class.forName("org.mmbase.bridge.jsp.taglib.ContextReferrerTag").
                                                                     getMethod("getThreadPageContext").invoke(null));
                            HttpServletRequestWrapper requestWrapper   = new HttpServletRequestWrapper(req);
                            RequestDispatcher requestDispatcher = req.getRequestDispatcher(includeUrl);
                            HttpServletResponse response = new GenericResponseWrapper((HttpServletResponse) pageContext.getResponse()) {
                                    // don't wrap status to including request.
                                    public void setStatus(int status) {
                                    }
                                    public void sendError(int sc, String mes) {
                                    }
                                    public void sendError(int sc) {
                                    }
                                };
                            requestDispatcher.include(requestWrapper, response);
                            include.append(response.toString());
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        }
                    }

                }
                String bodyTemplate = emailTemplate.getString("body");
                String bodyHtml = MessageFormat.format(emailTemplate.getString("body"), encryptedKey, u.toString(), include.toString());

                String body = "<multipart id=\"plaintext\" type=\"text/plain\" encoding=\"UTF-8\">\n" +
                    stripper.transform(bodyHtml) +
                    "\n</multipart>\n" +
                    "<multipart alt=\"plaintext\" type=\"text/html\" encoding=\"UTF-8\">\n" +
                    bodyHtml +
                    "\n</multipart>\n";
                emailNode.setStringValue(bodyField, body);



                try {
                    emailNode.commit();
                } catch (Exception e) {
                    log.error(e);
                    emailNode.delete();
                }

                log.debug("Using cloud " + cloud);
                emailNode = cloud.getNode(emailNode.getNumber());
                try {
                    Function mailFunction = emailNode.getFunction("startmail");
                    mailFunction.getFunctionValue((Parameters) null);
                } catch (NotFoundException nfe) {
                    log.debug("No function 'startmail', assuming that the mail builder mailed on commit");
                }
            }
        }

    }


    public static void main(String[] argv) throws Exception {

        String encrypted = encrypt(argv[0]);
        System.out.println(encrypted);
        System.out.println(decrypt(encrypted));

    }

}

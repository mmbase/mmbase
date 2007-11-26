/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.sms.cmtelecom;

import org.mmbase.sms.*;
import org.mmbase.bridge.*;
import org.mmbase.util.xml.*;
import java.util.*;
import java.io.*;
import java.net.*;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.SAXException;
import java.util.concurrent.*;
import org.xml.sax.helpers.AttributesImpl;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * A realization of {@link Sender}, which communicates to a server implemented by
 * 'cmtelecom'. (http://www.clubmessage.biz/).
 *
 *
 * @author Michiel Meeuwissen
 * @version $Id: CMTelecomSender.java,v 1.6 2007-11-26 15:50:38 michiel Exp $
 **/
public  class CMTelecomSender extends Sender {
    private static final Logger log = Logging.getLoggerInstance(CMTelecomSender.class);

    private Queue<SMS> queue = new LinkedBlockingQueue<SMS>();

    public static final Map<String, String> configuration = new UtilReader("cmtelecom.xml").getProperties();

    protected interface Appender {
        void append(XmlWriter w) throws SAXException;
    }

    public void add(SMS s, XmlWriter w) throws SAXException {
        w.startElement("MSG");
        w.startElement("FROM");
        String from = configuration.get("from");
        if (from == null) from = "MMBase notifications";
        w.characters(from);
        w.endElement("FROM");
        {
            AttributesImpl a = new AttributesImpl();
            a.addAttribute("", "TYPE", "", "CDATA", "TEXT");
            w.startElement("", "BODY", "", a);
            w.characters(s.getMessage());
            w.endElement("BODY");
        }
        {
            AttributesImpl a = new AttributesImpl();
            int op = s.getOperator();
            a.addAttribute("", "OPERATOR", "", "CDATA", "" + (op != -1 ? "" + op : configuration.get("operator")));
            w.startElement("", "TO", "", a);
            w.characters(s.getMobile());
            w.endElement("TO");
        }
        w.endElement("MSG");
    }

    protected void send(Appender body)  throws SAXException, IOException {
        String u = configuration.get("url");
        log.service("Connecting to '" + u + "' " + configuration);
        URL url = new URL(configuration.get("url"));
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setConnectTimeout(10000);
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        OutputStream out = con.getOutputStream();
        send(out, body);

        try {
            final InputStream in = con.getInputStream();
            BufferedReader is = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = is.readLine()) != null) {
                log.info(line);
            }
            is.close();
        } catch (Exception e) {
            log.error(e);
        }

        {
            final InputStream error = con.getErrorStream();
            if (error != null) {
                BufferedReader es = new BufferedReader(new InputStreamReader(error));
                String line;
                while ((line = es.readLine()) != null) {
                    log.error(line);
                }
                es.close();
            }
        }
        out.close();

    }


    protected void send(OutputStream out, Appender body) throws SAXException, IOException {
        Writer writer = new OutputStreamWriter(out);
        XmlWriter w = new XmlWriter(writer);
        w.setSystemId("http://www.clubmessage.biz/DTD/bundles/messages.dtd");
        w.startDocument();
        {
            AttributesImpl a = new AttributesImpl();
            String productId = configuration.get("productId");
            if (productId == null || "".equals(productId)) {
                productId = "25";
            }
            a.addAttribute("", "PID", "", "CDATA", productId);
            w.startElement("", "MESSAGES", "", a);
        }
        {
            AttributesImpl a = new AttributesImpl();
            a.addAttribute("", "ID", "", "CDATA", configuration.get("customerId"));
            w.emptyElement("", "CUSTOMER",  "", a);
        }
        {
            AttributesImpl a = new AttributesImpl();
            a.addAttribute("", "LOGIN", "", "CDATA", configuration.get("userLogin"));
            a.addAttribute("", "PASSWORD", "", "CDATA", configuration.get("userPassword"));
            w.emptyElement("", "USER",  "", a);
        }
        String adminEmail = configuration.get("adminEmail");
        if (adminEmail != null && ! adminEmail.equals("")) {
            w.startElement("ADMIN_EMAIL");
            w.characters(adminEmail);
            w.endElement("ADMIN_EMAIL");
        }
        w.startElement("TARIFF");
        w.characters("0");
        w.endElement("TARIFF");
        w.startElement("REFERENCE");
        w.characters("mmbase reference " + System.currentTimeMillis());
        w.endElement("REFERENCE");

        if (body != null) {
            body.append(w);
        }

        w.endElement("MESSAGES");
        w.endDocument();
        w.flush();

    }

    protected void send(OutputStream out, final SMS sms) throws SAXException, IOException {
        send(out, new Appender() {
                public void append(XmlWriter w) throws SAXException {
                    add(sms, w);
                }
            });
    }

    void trigger() throws SAXException, IOException {
        final int drain = queue.size();
        if (drain > 0) {
            log.service("Sending " + drain + " SMS messages");
            send(new Appender() {
                    public void append(XmlWriter w) throws SAXException {
                        for (int i = 0; i < drain; i++) {
                            SMS sms = queue.poll();
                            add(sms, w);
                        }
                    }
                });
        } else  {
            log.service("Nothing queued, nothing to be sent");
        }
    }
    public boolean send(final SMS sms) {
        try {
            send(new Appender() {
                    public void append(XmlWriter w) throws SAXException {
                        add(sms, w);
                    }
                });
            return true;
        }  catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }

    }
    public boolean offer(SMS sms) {
        return queue.offer(sms);
    }

    public Collection<SMS> getQueue() {
        return Collections.unmodifiableCollection(queue);
    }


    /**
     * Main for testing only
     */
    public static void main(final String[] argv) throws Exception {
        CMTelecomSender sender = new CMTelecomSender();
        if (argv.length == 0) {
            System.out.println("Use tel-number as argument");
            sender.send(System.out, (Appender) null);
        } else {
            SMS sms = new BasicSMS(argv[0], 20416, "Test test " + new Date());
            sender.send(sms);
            sender.send(System.out, sms);
        }
    }

}

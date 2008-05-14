package com.finalist.newsletter.publisher;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.bridge.NodeIterator;
import org.apache.commons.lang.StringUtils;
import org.htmlparser.Parser;
import org.htmlparser.visitors.HtmlPage;
import org.htmlparser.util.ParserException;

import javax.mail.MessagingException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.finalist.newsletter.util.NewsletterUtil;

public class NewsletterGenerator {

   private static Logger log = Logging.getLoggerInstance(NewsletterGenerator.class.getName());


   public static String generate(String urlPath, String mimeType) throws MessagingException {

      log.debug("generate newsletter from url:" + urlPath);

      String inputString = "";
      try {

         log.debug("Try to get content from URL:" + urlPath);

         URL url = new URL(urlPath);
         HttpURLConnection connection = (HttpURLConnection) url.openConnection();

         connection.setRequestMethod("GET");
         connection.setDoInput(true);
         connection.setRequestProperty("Content-Type", mimeType);

         Reader reader = new InputStreamReader(connection.getInputStream());

         StringBuffer buffer = new StringBuffer();

         int c;
         while ((c = reader.read()) != -1) {
            char character = (char) c;
            buffer.append("").append(character);
         }

         reader.close();

         inputString = buffer.toString().trim();

         if ("text/plain".equals(mimeType)) {
            inputString = getContentFromPage(inputString);
         }
         inputString = calibrateRelativeURL(inputString);

         return (inputString);
      } catch (Exception e) {
         log.debug("Error when try to get content from" + urlPath, e);
      }

      return inputString;
   }

   public static String getContentFromPage(String inputString) {
      Parser myParser;
      myParser = Parser.createParser(inputString, "utf-8");

      HtmlPage visitor = new HtmlPage(myParser);

      try {
         myParser.visitAllNodesWith(visitor);
      } catch (ParserException e) {
         e.printStackTrace();
      }

      inputString =  visitor.getBody().asHtml().trim();
      inputString = inputString.replaceAll("(?m)^\\s*\r\n+", "").replaceAll("(?m)^\\s*\r+", "").replaceAll("(?m)^\\s*\n+", "");
      inputString = inputString.replaceAll("(?m)\r\n+", "").replaceAll("(?m)\r+", "").replaceAll("(?m)\n+", "");
      inputString = inputString.replaceAll("<br/>", "\r\n");


      return inputString;
   }

   private static String calibrateRelativeURL(String inputString) {

      String host = NewsletterUtil.getServerURL();
      inputString = StringUtils.replace(inputString, "href=\"/", "href=\"" + host);
      inputString = StringUtils.replace(inputString, "src=\"/", "src=\"" + host);
      inputString = StringUtils.replace(inputString, "src=\"/", "src=\"" + host);
      return inputString;
   }
}
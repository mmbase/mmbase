/*
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Initial Developer of the Original Code is 'Media Competence'
 *
 * See license.txt in the root of the LeoCMS directory for the full license.
 */
package nl.leocms.connectors.UISconnector;

/* This class contains settings specific for the UISConnector
*/

public class UISconfig {


   public UISconfig() {
   }

   public static String getProductUrl(){
      return "http://212.123.241.76/mmdemo/api/getProducts.jsp"; // production
      //return "file:///Z:/in.xml";
   }

   public static String postOrderUrl(){
        // return "http://212.123.241.76/mmdemo/api/postOrders.jsp"; // production
      return "http://192.168.120.47:8080/mmdemo_test/api/postOrders.jsp"; // development
      // return "http://mc018/mmdemo/api/postOrders.jsp"; // erwin
   }

   public static boolean isUISconnected() {
      // return false;
      return true;
   }


   public static String getProductPropertiesURL(){
//      return "file:///Z:/in2.xml";
      return "http://212.123.241.76/mmdemo/api/getProductPropertyList.jsp";
   }
   public static String getCustomerPropertiesURL(){
//      return "file:///Z:/in2.xml";
      return "http://212.123.241.76/mmdemo/api/getCustomerPropertyList.jsp";
   }


   public static String getCustomersURL(String sUserName, String sPassword){
//      return "http://212.123.241.76/demo/getCustomerInformation.jsp?username=" + sUserName + "&pwd=" + sPassword;
//      return "file:///Z:/test.xml";
      return "http://212.123.241.76/mmdemo/api/getCustomerInformation.jsp?username=test2&pwd=test2";
   }
}

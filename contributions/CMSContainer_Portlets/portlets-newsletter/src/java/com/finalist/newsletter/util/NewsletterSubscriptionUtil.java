package com.finalist.newsletter.util;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.finalist.cmsc.services.community.NewsletterCommunication;
import com.finalist.newsletter.generator.NewsletterGeneratorFactory;

public abstract class NewsletterSubscriptionUtil {

   private static final ResourceBundle rb = ResourceBundle.getBundle("portlets-newslettersubscription");

   public static final String NEWSLETTER = "newsletter";
   public static final String NEWSLETTER_THEME = "newslettertheme";
   public static final String PREFERRED_MIMETYPE = "preferredmimetype";

   public static final String MIMETYPE_HTML = "text/html";
   public static final String MIMETYPE_PLAIN = "text/plain";
   public static final String MIMETIPE_DEFAULT = MIMETYPE_HTML;
   public static final String USERNAME = "username";

   public static final String SUBSCRIPTION_STATUS_KEY = "subscriptionstatus";
   public static final String SUBSCRIPTION_STATUS_ACTIVE = rb.getString("status.active");
   public static final String SUBSCRIPTION_STATUS_INACTIVE = rb.getString("status.inactive");
   public static final String SUBSCRIPTION_STATUS_DEFAULT = SUBSCRIPTION_STATUS_ACTIVE;
   public static final String STATUS_OPTIONS = "statusoptions";

   private static List<String> statusOptions = new ArrayList<String>();

   static {
      statusOptions.add(SUBSCRIPTION_STATUS_ACTIVE);
      statusOptions.add(SUBSCRIPTION_STATUS_INACTIVE);
   }

   // public static final List AVAILABLE_MIMETYPES = new ArrayList<String>()
   // {MIMETYPE_HTML, MIMETYPE_PLAIN;;

   public static List<Integer> compareToUserSubscribedThemes(List<Integer> compareWithThemes, String userName, int newsletterNumber) {
      if (compareWithThemes == null || userName == null || newsletterNumber <= 0) {
         return (null);
      }
      List<Integer> userThemes = getUserSubscribedThemes(userName, newsletterNumber);
      List<Integer> themes = new ArrayList<Integer>();
      for (int i = 0; i < compareWithThemes.size(); i++) {
         int theme = compareWithThemes.get(i);
         if (userThemes.contains(theme)) {
            themes.add(theme);
         }
      }
      return (themes);
   }

   public static int countSubscriptions() {
      int number = NewsletterCommunication.countByKey(NEWSLETTER);
      return (number);
   }

   public static int countSubscriptions(int newsletterNumber) {
      int number = NewsletterCommunication.countK(NEWSLETTER, String.valueOf(newsletterNumber));
      return (number);
   }

   public static List<String> getAllUsersWithSubscription() {
      List<String> users = null;
      return (users);
   }

   public static int getNumberOfSubscribedNewsletters(String userName) {
      int amount = 0;
      if (userName != null) {
         amount = NewsletterCommunication.count(userName, NEWSLETTER);
      }
      return (amount);
   }

   public static int getNumberOfSubscribedThemes(String userName, int newsletterNumber) {
      int amount = 0;
      if (userName != null && newsletterNumber > 0) {
         List<Integer> themesList = NewsletterUtil.getAllThemes(newsletterNumber);
         if (themesList != null) {
            List<String> subscribedThemes = NewsletterCommunication.getUserPreferences(userName, NewsletterSubscriptionUtil.NEWSLETTER_THEME);
            for (int t = 0; t < themesList.size(); t++) {
               int theme = Integer.valueOf(themesList.get(t));
               if (subscribedThemes.contains(theme)) {
                  amount++;
               }
            }
         }
      }
      return (amount);
   }

   public static String getPreferredMimeType(String userName) {
      if (userName != null) {
         String preferredMimeType = NewsletterCommunication.getUserPreference(userName, PREFERRED_MIMETYPE);
         return (preferredMimeType);
      }
      return (null);
   }

   public static List<String> getStatusOptions() {
      return (statusOptions);
   }

   public static List<String> getSubscribersForNewsletter(int newsletterNumber) {
      List<String> subscribers = NewsletterCommunication.getUsersWithPreferences(NewsletterUtil.NEWSLETTER, String.valueOf(newsletterNumber));
      return (subscribers);
   }

   public static String getSubscriptionStatus(String userName) {
      return (NewsletterCommunication.getUserPreference(userName, SUBSCRIPTION_STATUS_KEY));
   }

   public static List<Integer> getUserSubscribedNewsletters(String userName) {
      if (userName != null) {
         List<String> newsletterList = NewsletterCommunication.getUserPreferences(userName, NEWSLETTER);
         List<Integer> newsletters = new ArrayList<Integer>();
         if (newsletterList != null) {
            for (int i = 0; i < newsletterList.size(); i++) {
               newsletters.add(Integer.valueOf(newsletterList.get(i)));
            }
         }
         return (newsletters);
      }
      return (null);
   }

   public static List<Integer> getUserSubscribedThemes(String userName) {
      if (userName != null) {
         List<String> themeList = NewsletterCommunication.getUserPreferences(userName, NEWSLETTER_THEME);
         List<Integer> themes = new ArrayList<Integer>();
         if (themeList != null) {
            for (int i = 0; i < themeList.size(); i++) {
               themes.add(Integer.valueOf(themeList.get(i)));
            }
         }

         return (themes);
      }
      return (null);
   }

   public static List<Integer> getUserSubscribedThemes(String userName, int newsletterNumber) {
      if (userName != null && newsletterNumber > 0) {
         List<String> themeList = NewsletterCommunication.getUserPreferences(userName, "newslettertheme");
         List<Integer> themes = new ArrayList<Integer>();
         if (themeList != null) {
            for (int i = 0; i < themeList.size(); i++) {
               themes.add(Integer.valueOf(themeList.get(i)));
            }
         }

         return (themes);
      }
      return (null);
   }

   public static void pauseSubscription(String userName) {
      setSubscriptionStatus(userName, SUBSCRIPTION_STATUS_INACTIVE);
   }

   public static void resumeSubscription(String userName) {
      setSubscriptionStatus(userName, SUBSCRIPTION_STATUS_ACTIVE);
   }

   public static void setPreferredMimeType(String userName, String mimeType) {
      if (userName != null) {
         if (mimeType == null) {
            mimeType = NewsletterGeneratorFactory.MIMETYPE_DEFAULT;
         }
         NewsletterCommunication.removeUserPreference(userName, PREFERRED_MIMETYPE);
         NewsletterCommunication.setUserPreference(userName, PREFERRED_MIMETYPE, mimeType);
      }
   }

   public static void setSubscriptionStatus(String userName, String status) {
      if (status == null) {
         status = SUBSCRIPTION_STATUS_DEFAULT;
      }
      if (userName != null && status != null) {
         if (statusOptions.contains(status)) {
            NewsletterCommunication.removeUserPreference(userName, SUBSCRIPTION_STATUS_KEY);
            NewsletterCommunication.setUserPreference(userName, SUBSCRIPTION_STATUS_KEY, status);
            return;
         }
      }
   }

   private static void subscribe(String userName, List<Integer> objects, String prefType) {
      if (userName != null && objects != null) {
         for (int i = 0; i < objects.size(); i++) {
            int objectNumber = objects.get(i);
            NewsletterCommunication.setUserPreference(userName, prefType, String.valueOf(objectNumber));
         }
      }
   }

   public static void subscribeToNewsletters(String userName, List<Integer> newsletters) {
      subscribe(userName, newsletters, NEWSLETTER);
      NewsletterCommunication.setUserPreference(userName, "subscribtiondate", String.valueOf(System.currentTimeMillis()));
   }

   public static void subscribeToTheme(String userName, int theme) {
      if (userName != null && theme > 0) {
         NewsletterCommunication.setUserPreference(userName, NEWSLETTER_THEME, String.valueOf(theme));
      }
   }

   public static void subscribeToThemes(String userName, List<Integer> themes) {
      subscribe(userName, themes, NEWSLETTER_THEME);
   }

   public static void terminateUserSubscription(String userName) {
      if (userName != null) {
         NewsletterCommunication.removeNewsPrefByUser(userName);
      }
   }

   public static void unsubscribeFromAllNewsletters(String userName) {
      if (userName != null) {
         NewsletterCommunication.removeUserPreference(userName, NEWSLETTER);
         NewsletterCommunication.removeUserPreference(userName, NEWSLETTER_THEME);
      }
   }

   public static void unsubscribeFromTheme(String userName, int theme) {
      if (userName != null && theme > 0) {
         NewsletterCommunication.removeUserPreference(userName, NEWSLETTER_THEME, String.valueOf(theme));
      }
   }

   public static void unsubscribeFromThemes(String userName, List<Integer> themes) {
      if (userName != null && themes != null) {
         for (int i = 0; i < themes.size(); i++) {
            int theme = themes.get(i);
            NewsletterCommunication.removeUserPreference(userName, NEWSLETTER_THEME, String.valueOf(theme));
         }
      }
   }

}
/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.util;

import org.mmbase.module.Module;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * Module for the automatic generation of passwords.
 * Based on the code of Arnold G. Reinhold, Cambridge, MA, USA.
 * This code is under the GNU License as specified by the original author.
 * <br />
 * The passwords generated by this class are based on a template.
 * A template can exists of a number of characters, which are replaced by
 * the generator as follows:<br />
 * A : is replaced by a random letter (a - z).<br />
 * C : is replaced by a random alphanumeric character (0-9, a-z)<br />
 * H : is replaced by a random hex character (0-9,A-F)<br />
 * S : is replaced by a random syllable. This, alterating, an element from a set of
 *     consonants or an element form a set of vowels. A syllable can be more than
 *     one character (i.e. "qu").<br />
 * 6 : is replaced by a random dice-roll (1-6)<br />
 * 9 : is replaced by a random digit (0-9)<br />
 * <br />
 *
 * @license uses the GNU license, should be moved external
 * @author Rico Jansen
 * @author Pierre van Rooden (javadocs)
 * @version $Id: PasswordGenerator.java,v 1.9 2005-01-30 16:46:35 nico Exp $
 */

public class PasswordGenerator extends Module implements PasswordGeneratorInterface {

    // logger
    private static Logger log = Logging.getLoggerInstance(PasswordGenerator.class.getName());

    /**
     * For testing.
     */
    private static PasswordGenerator PG;

    /**
     * List of consonants that can be used in a password.
     */
    private static String consonants[] = {
            "b","c","d","f","g","h","j","k","l","m",
            "n","p","qu","r","s","t","v","w","x","z",
            "ch","cr","fr","nd","ng","nk","nt","ph","pr","rd",
            "sh","sl","sp","st","th","tr"
        };

    /**
     * List of vowels that can be used in a password.
     */
    private static String vowels[] = { "a","e","i","o","u","y" };

    /**
     * Pool of random numbers.
     */
    RandomPool ranPool;

    /**
     * Default template to use when generating passwords.
     */
    String defaulttemplate=new String("SSSSSS");

    /**
     * Creates the generator
     */
    public PasswordGenerator() {
        ranPool=new RandomPool();
    }

    /**
     * Called when the module is loaded.
     * Not used.
     */
    public void onload() {
    }

    /**
     * Called when the module is reloaded.
     * Tries to retrieve a default template for a password from the
     * template property from the module configuration file.
     * Not used.
     */
    public void reload() {
        defaulttemplate=getInitParameter("template");
        if (defaulttemplate==null) defaulttemplate=new String("SSSSSS");
    }

    /**
     * Initializes the module.
     * Tries to retrieve a default template for a password from the
     * template property from the module configuration file.
     */
    public void init() {
        defaulttemplate=getInitParameter("template");
        if (defaulttemplate==null) defaulttemplate=new String("SSSSSS");
    }

    /**
     * Called when the module is unloaded.
     * Not used.
     */
    public void unload() {
    }

    /**
     * Called when the module is shut down (removed).
     * Not used.
     */
    public void shutdown() {
    }

    /**
     * Entry point when calling from teh command line.
     * Used for testing.
     */
    public static void main(String args[]) {
        PG=new PasswordGenerator();
        log.info("Password "+PG.getPassword());
        log.info("Password "+PG.getPassword("SSS 9 SSS"));
        log.info("Password "+PG.getPassword("SSSS"));
        log.info("Password "+PG.getPassword("SSSSS"));
        log.info("Password "+PG.getPassword("SSSSSS"));
        log.info("Password "+PG.getPassword("CCC CCC CCC"));
        log.info("Password "+PG.getPassword("HHHH HHHH HHHH"));
        log.info("Password "+PG.getPassword("AAAAA AAAAA AAAAA"));
        log.info("Password "+PG.getPassword("99999 99999 99999"));
        log.info("Password "+PG.getPassword("66666 66666 66666"));
    }

    /**
     * Returns the positive modulus of the arguments.
     * @param x the argument to modulate
     * @param y the argument to modulate with
     * @return the positive modulus of x modulus y.
     */
    private int mod(long x, long y) {
        if (x<0) x=-x;
        if (y<0) y=-y;
        return (int) (x % y);
    }

    /**
     * Generate a password, based on the default template for this module.
     * @return the generated password.
     */
    public String getPassword() {
        return(getPassword(defaulttemplate));
    }

    /**
     * Generate a password, based on the given template.
     * @param template the template the password should be based on.
     * @return the generated password.
     */
    public String getPassword(String template) {
        int len;
        boolean next=true;
        StringBuffer pwd=new StringBuffer();

        len=template.length();
        for (int i=0;i<len;i++) {
            ranPool.stir(i*93762+49104); // Increasing value
            next=addChar(pwd,template,i,next);
        }
        return(pwd.toString());
    }

    /**
     * Add a random character to the password as specified by the template.
     * @param password the <code>StringBuffer</code> containing the password to add a new character to
     * @param template template to use when generating the password
     * @param ntmpl index in the template indicating the template character to use
     * @param consonantNext if <code>true</code>, the 'S' template character will
     *                      return a consonant. otherwise it will return a vowel.
     * @return <code>false</code> if the value returned was a conmsonant generated by the 'S'
     *         template character, otherwise <code>true</code>.
     */
    private boolean addChar(StringBuffer password,String template, int ntmpl,boolean consonantNext) {
        int ch = 0;
        char tmplChar;
        String charsOut=null;

        if (ntmpl >= template.length()) {
            consonantNext = true;
            return consonantNext;
        }
        tmplChar = template.charAt(ntmpl);

        if (tmplChar == ' ') {
            ch = ' ';

        } else if (tmplChar == 'A') {        //random letter
            ch = mod(ranPool.value(), 26) + (int) 'a';

        } else if (tmplChar == 'C') {        //random alphanumeric [0-9,A-Z]
            ch =  mod(ranPool.value(), 36);
            if (ch <10) ch = ch + (int) '0';
            else ch = ch + (int) 'a' - 10;

        } else if (tmplChar == 'H') {        //random hex digit
            ch =  mod(ranPool.value(), 16);
            if (ch <10) ch = ch + (int) '0';
            else ch = ch + (int) 'a' - 10;

        } else if (tmplChar == 'S') {        //random syllable
            if (consonantNext) {
                charsOut = consonants[mod(ranPool.value(), consonants.length)];
                if (!"qu".equals(charsOut)) consonantNext = false;
            } else {
                charsOut = vowels[mod(ranPool.value(), vowels.length)];
                consonantNext = true;
            }

        } else if (tmplChar == '6') {        //random dice throw
            ch = mod(ranPool.value(), 6) + (int) '1';

        } else if (tmplChar == '9') {        //random digit
            ch = mod(ranPool.value(), 10) + (int) '0';

        } else {
            return consonantNext;
        }

        if (charsOut==null) {
            charsOut = String.valueOf((char)ch);
            consonantNext = true;
        }
        password.append(charsOut);
        return consonantNext;
    }

    /**
     * Returns a description of the module.
     */
    public String getModuleInfo() {
        return("Password Generator module, based on the code of Arnold G. Reinhold, Cambridge, MA, USA. Author Rico Jansen");
    }
}


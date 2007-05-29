package nl.leocms.vastgoed;

import org.apache.struts.action.ActionForm;

/**
 * @author
 * @version $Id: BestelForm.java,v 1.1 2007-05-29 11:52:28 ieozden Exp $
 *
 * @struts:form name="BestelForm"
 */

public class BestelForm extends ActionForm{

	 private String naam;
	 private String email;
	 private String eendheid;
	 private String bezorgadres;
	 
	 
	public String getEendheid() {
		return eendheid;
	}
	public void setEendheid(String eendheid) {
		this.eendheid = eendheid;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getNaam() {
		return naam;
	}
	public void setNaam(String naam) {
		this.naam = naam;
	}
	public String getBezorgadres() {
		return bezorgadres;
	}
	public void setBezorgadres(String bezorgadres) {
		this.bezorgadres = bezorgadres;
	}
	 
	 
}

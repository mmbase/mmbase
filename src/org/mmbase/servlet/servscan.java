/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.servlet;

import java.lang.*;
import java.net.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.Image;
import javax.servlet.*;
import javax.servlet.http.*;


import org.mmbase.util.*;
import org.mmbase.module.*;
import org.mmbase.module.core.*;
import org.mmbase.module.gui.html.*;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * servscan is the 'extened html language' (parsing *.shtml) provided for MMbase its a system like
 * php3, asp or jsp but has its roots providing a simpler toolset for Interaction
 * designers and gfx designers its provides as a option but not demanded you can
 * also use the provides jsp for a more traditional parser system.
 * 
 * @version $Id: servscan.java,v 1.23 2001-11-25 16:21:36 vpro Exp $
 * @author Daniel Ockeloen
 * @author Rico Jansen
 * @author Jan van Oosterom
 * @author Rob Vermeulen
 */
public class servscan extends JamesServlet {
	private static Logger log;

	// modules used in servscan
	private static ProcessorModule grab=null;
	private static sessionsInterface sessions=null;
	// private static idInterface id=null;
	// private static StatisticsInterface stats=null;

	private static String htmlroot;

	//int lastlistitem=0;
	private static String  fileroot;
	private scanparser parser;


	// Davzev added on 27-10-1999: Defining constants    
	public static final String SHTML_CONTENTTYPE = "text/html";
 	public static final String DEFAULT_CHARSET = "iso-8859-1"; 

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        // Initializing log here because log4j has to be initialized first.
        log = Logging.getLoggerInstance(servscan.class.getName());
        log.info("Init of servlet " + config.getServletName() + ".");
        MMBaseContext.initHtmlRoot();
        parser=(scanparser)getModule("SCANPARSER");
        sessions=(sessionsInterface)getModule("SESSION");
    }
	
	/**
	 * Adds DEFAULT_CHARSET to mimetype given by SHTML_CONTENTTYPE for handling
	 * of the charset used by the database
	 */
	private String addCharSet( String mimetype ) {
		if (mimetype.equals(SHTML_CONTENTTYPE)) 
			return mimetype+"; "+DEFAULT_CHARSET; 
		return mimetype;
	}
	
	/**
	 * handle_line is called by service to parse the SHTML in body.
	 * It can be used by children to do their own parsing. The default
	 * implementation calls parser.handle_line (from module scanparser)
	 * to do the parsing.
	 */
	protected String handle_line(String body, sessionInfo session, scanpage sp) throws ParseException {
		return parser.handle_line(body, session, sp);
	}

	/**
	 * Servlet request service.
	 * This processes the the page and sends it back
	 */


	public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		incRefCount(req);
		try {
			
			log.service("Scan page: " + req.getRequestURI());

			scanpage sp=new scanpage(this, req, res, sessions);
	
			// POST
			if (req.getMethod().equals("POST")) {
				handlePost(sp,res);
			}
	
			// Generate page
			PrintWriter out=res.getWriter();

			do {
				sp.rstatus=0;
				sp.body=parser.getfile(sp.req_line);
				log.trace("body :" + sp.body);
				if (!doCrcCheck(sp,res)) {
					throw new PageCRCException("invalid crc");
				}
				doSecure(sp,res); // name=doSecure(sp,res); but name not used here
				long stime=handleTime(sp);

				try {	
					if (handleCache(sp,res,out)) return;
				} catch (Exception e) {
					log.error("servscan - something is wrong with scancache");	
				}
	
				log.trace("body " + sp.body);
				if (sp.body != null && !sp.body.equals("")) {
					long oldtime = System.currentTimeMillis();
					// Process HTML
					sp.body = handle_line(sp.body, sp.session, sp);
					// Send data back
					if (sp.body!=null) {
						if (sp.rstatus==0) {
							sp.mimetype = addCharSet(sp.mimetype);
							res.setContentType(sp.mimetype); 
							out.print(sp.body);
							handleCacheSave(sp,res);
						} else if (sp.rstatus==1) {
							res.setStatus(302,"OK");
							res.setContentType( addCharSet( sp.mimetype ) ); 
							res.setHeader("Location",sp.body);
						} else if (sp.rstatus==2) {
							sp.req_line=sp.body;	
							if (sp.req_line.indexOf('\n')!=-1) {
								sp.req_line=sp.req_line.substring(0,sp.req_line.indexOf('\n'));
							}
						} else if (sp.rstatus==4) {
							String tmp = req.getHeader("If-Modified-Since:");
							if (tmp!=null && sp.processor!=null) {
								res.setStatus(304,"Not Modified");
								res.setContentType( addCharSet(sp.mimetype) ); 
							} else {
								setHeaders(sp,res,sp.body.length());
								out.print(sp.body);
							}
						}
					} else {
						sp.body="<TITLE>Servscan</TITLE>handle_line returned null<BR>";
						setHeaders(sp,res,sp.body.length());
						out.print(sp.body);
					}
				} else {
					break;
				}
	
				if (stime!=-1) {
					stime=System.currentTimeMillis()-stime;
					if(log.isDebugEnabled()) {
						log.debug("service("+getRequestURL(req)+"): STIME "+stime+"ms "+(stime/1000)+"sec");
					}
				}
			} while (sp.rstatus==2);	
			// End of page parser
			log.debug("page parsed");
			out.flush();
			out.close();
		} catch(NotLoggedInException e) {
			log.debug( "service(): page("+getRequestURL(req)+"): NotLoggedInException: ");
		} catch(PageCRCException e) {
			log.warn( "service(): page("+getRequestURL(req)+"): Invalid CRC");
		} catch(Exception a) {
			log.debug( "service(): exception on page: "+getRequestURL(req));
			a.printStackTrace();
		} finally { decRefCount(req); }
		
	}// service

	private final void setHeaders(scanpage sp,HttpServletResponse res,int len) {
		res.setContentType( addCharSet(sp.mimetype) );
		// res.setContentLength(len);
		// org.mmbase String ac=sp.req.getAcceptor();
		String ac="";
		if (true || ac.equals("film")) {
			Date d=new Date(); // Note this one IS correct
			Date d2=new Date(System.currentTimeMillis()-7200000);
			String dt=RFC1123.makeDate(d);
			String dt2=RFC1123.makeDate(d2);
//			debug("setHeaders(): "+ac+" time="+dt);
			res.setHeader("Expires",dt2);
			res.setHeader("Last-Modified",dt);
			res.setHeader("Date",dt);
			res.setHeader("Cache-Control","no-cache");
			res.setHeader("Pragma","no-cache");
		} else {
			Date d=new Date(0);
//			Date d=new Date(DateSupport.currentTimeMillis()-(1000*24*3600)); // probably this one
//			debug("setHeaders(): "+ac+" time="+dt);
			String dt=RFC1123.makeDate(d);
//			res.setHeader("Expires",dt);
//			res.setHeader("Last-Modified",dt);
			res.setHeader("Date",dt);
			res.setHeader("Cache-Control","no-cache");
			res.setHeader("Pragma","no-cache");
		}
//		res.setHeader("Cache-Control","no-cache");
//		res.setHeader("Pragma","no-cache");
	}
		


	/* org.mmbase
	private final String do_reload(scanpage sp,String part)
	{
		sp.rstatus=4;
		body2=part;
		return(""); 
	}
	*/


	/**
	*/
	private final String do_cache(String part)
	{
		return(null);
	}


	public String getServletInfo() {
		return("extended html parser that adds extra html commands and a interface to modules.");
	}

	 void handlePost(scanpage  sp,HttpServletResponse res) throws Exception {
		String rtn,part,part2,finals,tokje,header;
		Hashtable proc_cmd = new Hashtable();
		Hashtable proc_var = new Hashtable();
		Object obj;
		HttpServletRequest req=sp.req;
		HttpPost poster=new HttpPost(req);
		sp.poster=poster;
		String name=null;
	
			
		// first check if it has a secure tag.
			String sec = poster.getPostParameter("SECURE");

				if (sec!=null) {
					if( log.isDebugEnabled()) log.debug("handlePost("+sp.getUrl()+"): Secure tag found, displaying username/password window at client-side.");
					name=getAuthorization(req,res);
					if( log.isDebugEnabled()) log.debug("handlePost("+sp.getUrl()+"): getting cookie to check name");
					String sname=getCookie(req,res);
					if (name==null) { 
						log.debug("handlePost(): Warning Username is null");
						return;
					}
				}
	
				// Process method=post information 
				for (Enumeration t=poster.getPostParameters().keys();t.hasMoreElements();) {
					obj=t.nextElement();
					part=(String)obj;
					if (part.indexOf("SESSION-")==0) {
						if (sp.session!=null) {
							if (poster.checkPostMultiParameter((String)obj)) {
								// MULTIPLE SUPPORT
								sessions.addSetValues(sp.session,part.substring(8),poster.getPostMultiParameter((String)obj));
							} else {
								sessions.setValue(sp.session,part.substring(8),poster.getPostParameter((String)obj));
							}
						} else {
						}
					// Personal objects
					} else if (part.indexOf("ID-")==0) {
						//SESSION HACK getAuthorization(req.getAcceptor(),"Basic");
						//aaaa name=getRemoteUser();
						//name check
						if (name==null) { 
							log.debug("handlePost(): Warning Username is null");
							return;
						}
						if (name!=null && name.length()>1) {
							// setUserServletProperty(part.substring(3),poster.getPostParameter((String)obj),0);

							if (poster.checkPostMultiParameter((String)obj)) {
								// MULTIPLE SUPPORT
								// id.setValue(name,part.substring(3),req.getPostMultiParameter((String)obj));
							} else {
								// id.setValue(name,part.substring(3),poster.getPostParameter((String)obj));
							}
						}
					// PRC-CMD- commands
					} else	if (part.indexOf("PRC-CMD-")==0) {
						if (poster.checkPostMultiParameter((String)obj)) {
							proc_cmd.put(part.substring(8),poster.getPostMultiParameter((String)obj));
						} else {
							proc_cmd.put(part.substring(8),poster.getPostParameter((String)obj));
						}
					// PRC-VAR- vars
					} else	if (part.indexOf("PRC-VAR-")==0) {
						if (poster.checkPostMultiParameter((String)obj)) {
							proc_var.put(part.substring(8),poster.getPostMultiParameter((String)obj));
						} else {
							proc_var.put(part.substring(8),poster.getPostParameter((String)obj));
						}
					}
				}	
				// If there are cmds process them
				if (!proc_cmd.isEmpty()) parser.do_proc_input(sp.req_line,poster,proc_var,proc_cmd,sp);
	 }


	boolean handleCacheSave(scanpage sp,HttpServletResponse res) {
		if (sp.wantCache!=null) {
			 String req_line=sp.req_line;
			 if (sp.querystring!=null) req_line+="?"+sp.querystring;
			 try {
			 	parser.scancache.newput(sp.wantCache,res,req_line,sp.body, sp.mimetype);
			 } catch (Exception e) {
				log.error("servscan - something is wrong with scancache");	
			 }
		}
		return(true);
	}

	boolean handleCache(scanpage sp,HttpServletResponse res,PrintWriter out) {
			String req_line=sp.req_line;
			if (sp.querystring!=null) req_line+="?"+sp.querystring;

			// new new new scancache setup, needs to be moved
			// ----------------------------------------------

			// This depends on the PRAGMA: no-cache header
			// Which Internet Explorer does not send.

			if (sp.body!=null) {
				int start = sp.body.indexOf("<CACHE HENK");
				if (start>=0) {
					start+=11;
					int end = sp.body.indexOf(">", start);
					sp.wantCache="HENK";
//					debug("handleCache(): CACHE="+parser.scancache);
					String rst=parser.scancache.get(sp.wantCache,req_line, sp.body.substring(start,end+1),sp);
					if (log.isDebugEnabled()) log.debug("handleCache: sp.reload: "+sp.reload);
					if (rst!=null && !sp.reload) {
						setHeaders(sp,res,rst.length());
						// org.mmbase res.writeHeaders();
						out.print(rst);
						out.flush();
						out.close();
						if (log.isDebugEnabled()) log.debug("handleCache(): cache.hit("+req_line+")");
						return(true);
					} else {
						log.debug("handleCache(): cache.miss("+req_line+")");
					}
				}
			}

			if (sp.body!=null && sp.body.indexOf("<CACHE PAGE>")!=-1) {
				sp.wantCache="PAGE";
				String rst=parser.scancache.get(sp.wantCache,req_line,sp);
				if (log.isDebugEnabled()) log.debug("handleCache: sp.reload: "+sp.reload);
				if (rst!=null && !sp.reload) {
					setHeaders(sp,res,rst.length());
					// org.mmbase res.writeHeaders();
					out.print(rst);
					out.flush();
					out.close();
					if (log.isDebugEnabled()) log.debug("handleCache(): cache.hit("+req_line+")");
					return(true);
				} else {
					log.debug("handleCache(): cache.miss("+req_line+")");
				}
			}

			return (false);
		}


	private long handleTime(scanpage sp) {
		if (sp.body!=null && sp.body.indexOf("<TIME>")!=-1) {
			return(System.currentTimeMillis());
		}
		return(-1);
	}


	private boolean doCrcCheck(scanpage sp,HttpServletResponse res) {
		if (sp.body!=null && sp.body.indexOf("<CRC>")!=-1) {
			Vector p=sp.getParamsVector();
			String value=null;
			String checker=null;
			for (Enumeration t=p.elements();t.hasMoreElements();) {
				String part=(String)t.nextElement();
				if (!((String)p.lastElement()).equals(part)) {
					if (value==null) {
						value=part;
					} else {
						value+="+"+part;
					}
				} else {
					checker=part;
				}
			}
			value=sp.req.getRequestURI()+"?"+value;
			int crc=scanparser.calccrc32(value);
			String thiscrc="CRC"+crc;
			System.out.println("CRC = "+crc);
			if (checker!=null && checker.equals(thiscrc)) {
				return(true);
			}	
			return(false);
		}
		return(true);
	}

	private String doSecure(scanpage sp,HttpServletResponse res) throws Exception {
		String name=null;
		if (sp.body!=null && sp.body.indexOf("<SECURE>")!=-1) {

			if( log.isDebugEnabled()) log.debug("doSecure("+sp.getUrl()+"): Secure tag found, calling getAuthorization()...");
			name=getAuthorization(sp.req,res);
			if( log.isDebugEnabled()) log.debug("doSecure("+sp.getUrl()+"): getting cookie from user...");
			String sname=getCookie(sp.req,res);

			// check name
			// ----------

			if (name==null) { 
				log.warn("doSecure("+sp.getUrl()+"): WARNING: no username found!");
				return(null);
			}
		}
		return(name);
	}

}

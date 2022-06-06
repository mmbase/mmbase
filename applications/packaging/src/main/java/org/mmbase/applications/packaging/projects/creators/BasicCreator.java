/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

 */

package org.mmbase.applications.packaging.projects.creators;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.mmbase.applications.packaging.PackageManager;
import org.mmbase.applications.packaging.Person;
import org.mmbase.applications.packaging.packagehandlers.PackageInterface;
import org.mmbase.applications.packaging.projects.*;
import org.mmbase.applications.packaging.util.ExtendedDocumentReader;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.w3c.dom.NamedNodeMap;

/**
 * BasicCreator, base class for creators
 *
 * @author Daniel Ockeloen (MMBased)
 */
public class BasicCreator implements CreatorInterface,Runnable {
    private static Logger log = Logging.getLoggerInstance(BasicCreator.class);
    Class cl;
    String prefix;

    private Target threadtarget;
    private int threadversion;
    private String state = "done";

    private String type="unknown/unknown";
    private String description="";


    private ArrayList<packageStep> packagesteps;
    private packageStep projectstep;
    ArrayList<Target> relatedtargetcreate =  new  ArrayList<Target>();
    private float progressbar = 0;
    private float progressstep = 1;
    private float subprogressbar = 0;
    private float subprogressstep = 1;


    public BasicCreator() {
    }

    public String getDescription() {
	return description;
    }

    public String getType() {
	if (type==null) return "";
	return type;
    }

    public void setType(String type) {
	this.type=type;
    }

   public boolean setDescription(Target target,String newdescription) {
	target.setItem("description",newdescription);
	target.save();
	return true;
   }


   public boolean setName(Target target,String newname) {
	target.setItem("name",newname);
	target.save();
	return true;
   }


   public boolean setMaintainer(Target target,String newmaintainer) {
	target.setItem("maintainer",newmaintainer);
	target.save();
	return true;
   }

   public boolean setLicenseVersion(Target target,String newlicenseversion) {
	target.setItem("license.version",newlicenseversion);
	target.save();
	return true;
   }

   public boolean setLicenseType(Target target,String newlicensetype) {
	target.setItem("license.type",newlicensetype);
	target.save();
	return true;
   }

   public boolean setLicenseName(Target target,String newlicensename) {
	target.setItem("license.name",newlicensename);
	target.save();
	return true;
   }

   public int getLastVersion(Target target) {
	return getItemIntValue(target,"lastversion");
   }

   public int getNextVersion(Target target) {
        int i=getLastVersion(target);
        if (i==-1) return 1;
        return i+1;
   }

   public String getLastDate(Target target) {
	return getItemStringValue(target,"lastdate");
   }

   public boolean createPackage(Target target,int newversion) {
	log.error("createPackage not implemented by creator : "+this);
	return false;
   }


   public void createPackageThreaded(Target target,int newversion) {
       threadtarget = target;
       threadversion =  newversion;
       state="creating";
       Thread kicker = new Thread(this, "packagethread");
       kicker.start();
   }

   public void run() {
	createPackage(threadtarget,threadversion);
	state="done";
   }

   public boolean addPackageDepends(Target target,String newpackage,String version) {
        ArrayList<PackageDepend> packagedepends=(ArrayList<PackageDepend>)target.getItem("packagedepends");
        PackageInterface p=PackageManager.getPackage(newpackage);
        if (p!=null) {
             PackageDepend pd=new PackageDepend();
             pd.setName(p.getName());
             pd.setMaintainer(p.getMaintainer());
             pd.setVersion(p.getVersion());
             pd.setType(p.getType());
             packagedepends.add(pd);
             target.save();
             return true;
	}
	return false;
   }

   public boolean delPackageDepends(Target target,String packageid,String version,String versionmode) {
        ArrayList<PackageDepend> packagedepends=(ArrayList<PackageDepend>)target.getItem("packagedepends");
        for (PackageDepend pd : packagedepends) {
                if (pd.getId().equals(packageid) && pd.getVersion().equals(version) && pd.getVersionMode().equals(versionmode)) {
                        packagedepends.remove(pd);
                        break;
                }
        }
        target.save();
	return true;
   }


   public boolean setPackageDepends(Target target,String packageid,String oldversion,String oldversionmode,String newversion,String newversionmode) {
        ArrayList<PackageDepend> packagedepends=(ArrayList<PackageDepend>)target.getItem("packagedepends");
        for (PackageDepend pd : packagedepends) {
                if (pd.getId().equals(packageid) && pd.getVersion().equals(oldversion) && pd.getVersionMode().equals(oldversionmode)) {
			pd.setVersion(newversion);
			pd.setVersionMode(newversionmode);
                        break;
                }
        }
        target.save();
	return true;
   }

   public ArrayList<PackageDepend> getPackageDepends(Target target) {
	Object o=target.getItem("packagedepends");
	if (o!=null) return (ArrayList<PackageDepend>)o;
	return new ArrayList<PackageDepend>();
   }

   public boolean addPackageInitiator(Target target,String newname,String newcompany) {
   	ArrayList<Person> list=getRelatedPeople("initiators",target);
	if (list==null) {
		list=new ArrayList<Person>();
		target.setItem("initiators",list);
	}
	Person pr=new Person();
	pr.setName(newname);
	pr.setCompany(newcompany);
	list.add(pr);
	target.save();
	return true;
   }

   public boolean setPackageInitiator(Target target,String oldname,String newname,String oldcompany,String newcompany) {
   	ArrayList<Person> people=getRelatedPeople("initiators",target);
       	for (Person pr : people) {
		if (pr.getName().equals(oldname) && pr.getCompany().equals(oldcompany)) {
			pr.setName(newname);
			pr.setCompany(newcompany);
			target.save();
			break;
		}
	}
	return true;
   }


   public boolean delPackageInitiator(Target target,String oldname,String oldcompany) {
   	ArrayList<Person> people=getRelatedPeople("initiators",target);
       	for (Person pr : people) {
		if (pr.getName().equals(oldname) && pr.getCompany().equals(oldcompany)) {
			people.remove(pr);
			target.save();
			break;
		}
	}
	return true;
   }


   public boolean addPackageDeveloper(Target target,String newname,String newcompany) {
   	ArrayList<Person> list=getRelatedPeople("developers",target);
        if (list==null) {
                list=new ArrayList<Person>();
                target.setItem("developers",list);
        }
	Person pr=new Person();
	pr.setName(newname);
	pr.setCompany(newcompany);
	list.add(pr);
	target.save();
	return true;
   }

   public boolean setPackageDeveloper(Target target,String oldname,String newname,String oldcompany,String newcompany) {
   	ArrayList<Person> people=getRelatedPeople("developers",target);
       	for (Person pr : people) {
		if (pr.getName().equals(oldname) && pr.getCompany().equals(oldcompany)) {
			pr.setName(newname);
			pr.setCompany(newcompany);
			target.save();
			break;
		}
	}
	return true;
   }


   public boolean delPackageDeveloper(Target target,String oldname,String oldcompany) {
   	ArrayList<Person> people=getRelatedPeople("developers",target);
       	for (Person pr : people) {
		if (pr.getName().equals(oldname) && pr.getCompany().equals(oldcompany)) {
			people.remove(pr);
			target.save();
			break;
		}
	}
	return true;
   }


   public boolean addPackageContact(Target target,String newreason,String newname,String newemail) {
   	ArrayList<Person> list=getRelatedPeople("contacts",target);
        if (list==null) {
                list=new ArrayList<Person>();
                target.setItem("contacts",list);
        }
	Person pr=new Person();
	pr.setName(newname);
	pr.setMailto(newemail);
	pr.setReason(newreason);
	list.add(pr);
	target.save();
	return true;
   }

   public boolean setPackageContact(Target target,String oldreason,String newreason,String oldname,String newname,String oldemail,String newemail) {
   	ArrayList<Person> people=getRelatedPeople("contacts",target);
       	for (Person pr : people) {
		if (pr.getName().equals(oldname) && pr.getReason().equals(oldreason) && pr.getMailto().equals(oldemail)) {
			pr.setName(newname);
			pr.setMailto(newemail);
			pr.setReason(newreason);
			target.save();
			break;
		}
	}
	return true;
   }


   public boolean delPackageContact(Target target,String oldreason,String oldname,String oldemail) {
   	ArrayList<Person> people=getRelatedPeople("contacts",target);
       	for (Person pr : people) {
		if (pr.getName().equals(oldname) && pr.getMailto().equals(oldemail) && pr.getReason().equals(oldreason)) {
			people.remove(pr);
			target.save();
			break;
		}
	}
	return true;
   }


   public boolean addPackageSupporter(Target target,String newcompany) {
   	ArrayList<Person> list=getRelatedPeople("supporters",target);
        if (list==null) {
                list=new ArrayList<Person>();
                target.setItem("supporters",list);
        }
	Person pr=new Person();
	pr.setCompany(newcompany);
	list.add(pr);
	target.save();
	return true;
   }

   public boolean setPackageSupporter(Target target,String oldcompany,String newcompany) {
   	ArrayList<Person> people=getRelatedPeople("supporters",target);
       	for (Person pr : people) {
		if (pr.getCompany().equals(oldcompany)) {
			pr.setCompany(newcompany);
			target.save();
			break;
		}
	}
	return true;
   }


   public boolean delPackageSupporter(Target target,String oldcompany) {
   	ArrayList<Person> people=getRelatedPeople("supporters",target);
       	for (Person pr : people) {
		if (pr.getCompany().equals(oldcompany)) {
			people.remove(pr);
			target.save();
			break;
		}
	}
	return true;
   }


   public boolean decodeIntItem(Target target,String epath) {
	ExtendedDocumentReader reader=target.getReader();
        String value=reader.getElementValue(prefix+"."+epath);
	try {
		target.setItem(epath,new Integer(Integer.parseInt(value)));
	} catch(Exception e) {
		return false;
	}
	return true;
   }


   public boolean decodeStringItem(Target target,String epath) {
	ExtendedDocumentReader reader=target.getReader();
       	String value=reader.getElementValue(prefix+"."+epath);
	if (value!=null) {
		target.setItem(epath,value);
	} else {
		target.setItem(epath,"");
	}
	return true;
   }


   public boolean decodeStringAttributeItem(Target target,String epath,String attribute) {
	ExtendedDocumentReader reader=target.getReader();
       	org.w3c.dom.Node n=reader.getElementByPath(prefix+"."+epath);
        NamedNodeMap nm=n.getAttributes();
        if (nm!=null) {
               String value=null;
                org.w3c.dom.Node n2=nm.getNamedItem(attribute);
                if (n2!=null) {
                       value=n2.getNodeValue();
			target.setItem(epath+"."+attribute,value);
                }
		return true;
	}
 	return false;
   }


    public Iterator<packageStep> getPackageSteps() {
	if (packagesteps!=null) {
		return packagesteps.iterator();
	} else {
		return null;
	}
    }

    public int getErrorCount() {
	int count=0;
    	Iterator<packageStep> e=getPackageSteps();
	while (e.hasNext()) {
		packageStep step=e.next();
		count+=step.getErrorCount();
	}
	return count;
    }


    public int getWarningCount() {
	int count=0;
    	Iterator<packageStep> e=getPackageSteps();
	while (e.hasNext()) {
		packageStep step=e.next();
		count+=step.getWarningCount();
	}
	return count;
    }

    public Iterator<packageStep> getPackageSteps(int logid) {
	// well maybe its one of my subs ?
    	Iterator<packageStep> e=getPackageSteps();
	while (e.hasNext()) {
		packageStep step=e.next();
		Object o=step.getPackageSteps(logid);
		if (o!=null) {
			return (Iterator<packageStep>)o;
		}
	}
	return null;
    }

    public packageStep getNextPackageStep() {
        packageStep step=null;
        if (projectstep!=null) {
                step=projectstep.getNextPackageStep();
        } else {
                // create new step
                step=new packageStep();
        }
        if (packagesteps==null) {
                packagesteps=new ArrayList<packageStep>();
                packagesteps.add(step);
                return step;
        } else {
                packagesteps.add(step);
                return step;
        }
    }

    public void clearPackageSteps() {
	packagesteps=null;
    }


   public ArrayList<String> getFileNames(ArrayList<String> foundfiles,String basedir,String include,String exclude) {
	// wrapper because we only want to filter it 1 time and not recursive
	ArrayList<String> files=getFileNames_r(foundfiles,basedir,include,exclude);
	ArrayList<String> filtered=new ArrayList<String>();

	// tricky : a filter all unused dirs
	Iterator<String> e=files.iterator();
	while (e.hasNext()) {
		String fn=e.next();
		if (fn.endsWith(File.separator)) {
			Iterator<String> f=files.iterator();
			while (f.hasNext()) {
				String fn2=f.next();
				if (!fn2.endsWith(File.separator) && fn2.indexOf(fn)!=-1) {
					filtered.add(fn);
					break;
				}
			}
		} else {
			filtered.add(fn);
		}
	}
	return filtered;
   }

   public ArrayList<String> getFileNames_r(ArrayList<String> foundfiles,String basedir,String include,String exclude) {
        File currDir = new File(basedir);
	if (currDir!=null && currDir.isDirectory()) {
        String files[] = currDir.list();
        for (String element : files) {
		File tfile=new File(basedir+File.separator+element);
		if (tfile.isDirectory()) {
			foundfiles.add(basedir+element+"/");
			getFileNames_r(foundfiles,basedir+element+File.separator,include,exclude);
		} else {

			String fn=basedir+element;
			if (include.equals("*") || fn.indexOf(include)!=-1) {
			if (exclude.equals("") || !excludeFile(tfile,fn,exclude)) {
				foundfiles.add(fn);
			}
			}
		}
        }
	}
	return foundfiles;
   }

   private boolean excludeFile(File tfile,String fullname,String excludestring) {
	excludestring = excludestring.replace('/',File.separatorChar);
	excludestring = excludestring.replace('\\',File.separatorChar);

    long length=tfile.length();
	long lastmodified=tfile.lastModified();
	long fileage=(System.currentTimeMillis()-lastmodified)/1000;
	StringTokenizer tok =  new StringTokenizer(excludestring,",\n\r");
	while (tok.hasMoreTokens()) {
		String ex=tok.nextToken();
		if (ex.startsWith("t(")) {
		} else if (ex.startsWith("s(")) {
			try {
				String tmp=ex.substring(2);
				int maxfsize=Integer.parseInt(tmp.substring(0,tmp.indexOf(")")));
				if (length>maxfsize) return true;
			} catch(Exception e) {
			}
		} else if (ex.startsWith("a(")) {
			try {
				String tmp=ex.substring(2);
				int age=3600*Integer.parseInt(tmp.substring(0,tmp.indexOf(")")));
				if (fileage>age) return true;
			} catch(Exception e) {
			}
		} else if (fullname.indexOf(ex)!=-1) {
			return true;
		}
	}
	return false;
   }

   public String getXMLFile(Target target) {
	log.error("getXMLFile not implemented in creator : "+this);
	return null;
   }

   public void createPackageMetaFile(JarOutputStream jarfile,Target target,int newversion) {
	Date d=new Date();
	String body="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
	body+="<!DOCTYPE package PUBLIC \"-//MMBase/DTD package config 1.0//EN\" \"https://www.mmbase.org/dtd/package_1_0.dtd\">\n";
	body+="<package name=\""+getName(target)+"\" type=\""+type+"\" maintainer=\""+getMaintainer(target)+"\" version=\""+newversion+"\" creation-date=\""+d.toString()+"\" >\n";
	body+="\t<description>"+getDescription(target)+"</description>\n";
	body+="\t<license type=\""+getLicenseType(target)+"\" version=\""+getLicenseVersion(target)+"\" name=\""+getLicenseName(target)+"\" />\n";
	body+="\t<releasenotes>\n"+getReleaseNotes(target)+"\n</releasenotes>\n";
	body+="\t<installationnotes>\n"+getInstallationNotes(target)+"\n</installationnotes>\n";


  	body+=getRelatedPeopleXML("initiators","initiator",target);
  	body+=getRelatedPeopleXML("supporters","supporter",target);
  	body+=getRelatedPeopleXML("developers","developer",target);
  	body+=getRelatedPeopleXML("contacts","contact",target);
	body+="</package>\n";
	try {
       		JarEntry entry = new JarEntry("package.xml");
           	jarfile.putNextEntry(entry);
       		jarfile.write(body.getBytes("UTF-8"));
	} catch (Exception e) {
		e.printStackTrace();
	}
   }


   public String getPackageDependsXML(Target target) {
	String body="\t<packagedepends>\n";
	ArrayList<PackageDepend> packagedepends=getPackageDepends(target);
	if (packagedepends!=null) {
        	for (PackageDepend pd : packagedepends) {
			body+="\t\t<package";
			body+=" name=\""+pd.getName()+"\"";
			body+=" maintainer=\""+pd.getMaintainer()+"\"";
			body+=" type=\""+pd.getType()+"\"";
			body+=" version=\""+pd.getVersion()+"\"";
			body+=" versionmode=\""+pd.getVersionMode()+"\"";
			body+=" />\n";
		}
	}
	body+="\t</packagedepends>\n";
	return body;
   }


   public void createDependsMetaFile(JarOutputStream jarfile,Target target) {
	String body="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
	body+="<!DOCTYPE packagedepends PUBLIC \"-//MMBase/DTD packagedepends config 1.0//EN\" \"https://www.mmbase.org/dtd/packagedepends_1_0.dtd\">\n";
	body+="\t<packagedepends>\n";

	ArrayList<PackageDepend> packagedepends=getPackageDepends(target);
	if (packagedepends!=null) {
        	for (PackageDepend pd : packagedepends) {
			body+="\t\t<package";
			body+=" name=\""+pd.getName()+"\"";
			body+=" maintainer=\""+pd.getMaintainer()+"\"";
			body+=" type=\""+pd.getType()+"\"";
			body+=" version=\""+pd.getVersion()+"\"";
			body+=" versionmode=\""+pd.getVersionMode()+"\"";
			body+=" />\n";
		}
	}
	body+="\t</packagedepends>\n";
	try {
       		JarEntry entry = new JarEntry("depends.xml");
           	jarfile.putNextEntry(entry);
       		jarfile.write(body.getBytes("UTF-8"));
	} catch (Exception e) {
		e.printStackTrace();
	}
   }

   public int addFiles(JarOutputStream jarfile,String basedir,String include,String exclude,String guiname,String dirprefix) {
        packageStep step=getNextPackageStep();
        step.setUserFeedBack("adding "+guiname+" files...");

	if (!dirprefix.equals("") && !dirprefix.endsWith(File.separator)) dirprefix+=File.separator;

	int filesadded=0;

	        byte[] buffer = new byte[1024];
   		int bytesRead;
		int baselen=basedir.length();
     		try {
			Iterator<String> files=getFileNames(new ArrayList<String>(),basedir,include,exclude).iterator();
			while (files.hasNext()) {
				String fn=files.next();
                                if (!fn.endsWith("/")) {
      				FileInputStream file = new FileInputStream(fn);
        			packageStep substep=step.getNextPackageStep();
	       	 		substep.setUserFeedBack("adding "+guiname+" file : "+fn);
      				try {
       					JarEntry entry = new JarEntry(dirprefix+fn.substring(baselen));
                  			jarfile.putNextEntry(entry);
       					while ((bytesRead = file.read(buffer)) != -1) {
        					jarfile.write(buffer, 0, bytesRead);
       					}
	        			substep.setUserFeedBack("added "+guiname+" file : "+fn.substring(baselen));
					filesadded++;
		         	} catch (Exception e) {
	        			substep.setUserFeedBack("failed added "+guiname+" file : "+fn.substring(baselen));
      				} finally {
       					file.close();
      				}
				} else {
       					JarEntry entry = new JarEntry(dirprefix+fn.substring(baselen));
                  			jarfile.putNextEntry(entry);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
        step.setUserFeedBack("added "+filesadded+" "+guiname+" files... done");
	return filesadded;
   }

   public String getDescription(Target target) {
        return(getItemStringValue(target,"description"));
   }

   public String getReleaseNotes(Target target) {
        return(getItemStringValue(target,"releasenotes"));
   }

   public String getInstallReset(Target target) {
        String tmp=getItemStringValue(target,"installreset");
	if (tmp!=null && !tmp.equals("")) {
		return tmp;
	}
	return "false";
   }

   public String getInstallationNotes(Target target) {
        return(getItemStringValue(target,"installationnotes"));
   }

   public String getMaintainer(Target target) {
        return(getItemStringValue(target,"maintainer"));
   }

   public String getName(Target target) {
        return(getItemStringValue(target,"name"));
   }

   public String getLicenseType(Target target) {
        return(getItemStringValue(target,"license.type"));
   }

   public String getLicenseName(Target target) {
        return(getItemStringValue(target,"license.name"));
   }

   public String getLicenseVersion(Target target) {
        return(getItemStringValue(target,"license.version"));
   }

   public ArrayList<Person> getRelatedPeople(String type,Target target) {
	Object o=target.getItem(type);
	if (o!=null) return (ArrayList<Person>)o;
	return null;
   }


   public void addFile(JarOutputStream jarfile,String fn,String dfn,String guiname,String dirprefix) {

        packageStep step=getNextPackageStep();
        if (!dirprefix.equals("")) dfn=dirprefix+File.separator+dfn;
        step.setUserFeedBack("added "+guiname+" file : "+dfn+" ... done");
        byte[] buffer = new byte[1024];
  	int bytesRead;
     	try {
		if (!fn.endsWith("/")) {
      			FileInputStream file = new FileInputStream(fn);
      			try {
       				JarEntry entry = new JarEntry(dfn);
       		         	jarfile.putNextEntry(entry);
       				while ((bytesRead = file.read(buffer)) != -1) {
       		 			jarfile.write(buffer, 0, bytesRead);
       				}
		        } catch (Exception e) {
      			} finally {
       			file.close();
      			}
		} else {
       			JarEntry entry = new JarEntry(dfn);
               		jarfile.putNextEntry(entry);
		}
	} catch (Exception e) {
        	step.setUserFeedBack("added "+guiname+" file : "+dfn+" ... error file not found");
                step.setType(packageStep.TYPE_ERROR);
		return;
	}
        step.setUserFeedBack("added "+guiname+" file : "+dfn+" ... done");
   }


   public void addScreenshotFiles(JarOutputStream jarfile,Target target) {
       try {
            JarEntry entry = new JarEntry("screenshots" + File.separator);
            jarfile.putNextEntry(entry);
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }

        for (Iterator<String> i = target.getScreenshots().iterator(); i.hasNext();) {
            String name = i.next();
            String filen = i.next(); // not used here
            String description = i.next(); // not used here
            String fn = target.getBaseDir() + "packaging" + File.separator + filen;

            String dfn = filen;
            packageStep step = getNextPackageStep();
            step.setUserFeedBack("added screenshot file : " + dfn + " ... done");
            byte[] buffer = new byte[1024];
            int bytesRead;
            try {
                FileInputStream file = new FileInputStream(fn);
                try {
                    JarEntry entry = new JarEntry(dfn);
                    jarfile.putNextEntry(entry);
                    while ((bytesRead = file.read(buffer)) != -1) {
                        jarfile.write(buffer, 0, bytesRead);
                    }
                }
                catch (Exception e) {
                }
                finally {
                    file.close();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                step
                        .setUserFeedBack("added screenshot file : " + dfn
                                + " ... error file not found");
                step.setType(packageStep.TYPE_ERROR);
                return;
            }
            step.setUserFeedBack("added screenshot file : " + dfn + " ... done");
        }
    }

  public String getRelatedPeopleXML(String type,String subtype,Target target) {
	String result="\t<"+type+">\n";
	List<Person> people=getRelatedPeople(type,target);
	if (people!=null) {
        	for (Person pr : people) {
			result+="\t\t<"+subtype;
			if (pr.getName()!=null) result+=" name=\""+pr.getName()+"\"";
			if (pr.getCompany()!=null) result+=" company=\""+pr.getCompany()+"\"";
			if (pr.getReason()!=null) result+=" reason=\""+pr.getReason()+"\"";
			if (pr.getMailto()!=null) result+=" mailto=\""+pr.getMailto()+"\"";

			result+=" />\n";
		}
	}
	result+="\t</"+type+">\n";
	return result;
  }


  public String getScreenshotsXML(Target target) {
	String result="\t<screenshots>\n";
	ArrayList<String> l=target.getScreenshots();
	if (l!=null) {
        	for (Iterator<String> i = l.iterator(); i.hasNext();) {
			String name=i.next();
			String file=i.next();
			String description=i.next();
			result+="\t\t<screenshot name=\""+name+"\" file=\""+file+"\">"+description+"</screenshot>\n";
		}
	}
	result+="\t</screenshots>\n";
	return result;
  }


  public String getStarturlsXML(Target target) {
	String result="\t<starturls>\n";
	ArrayList<String> l=target.getStarturls();
	if (l!=null) {
        	for (Iterator<String> i = l.iterator(); i.hasNext();) {
			String name=i.next();
			String link=i.next();
			String description=i.next();
			result+="\t\t<url name=\""+name+"\" link=\""+link+"\">"+description+"</url>\n";
		}
	}
	result+="\t</starturls>\n";
	return result;
  }

  public ArrayList<IncludedPackage> getIncludedPackages(Target target) {
	return null;
  }

  public boolean setIncludedVersion(Target target,String id,String newversion) {
	return false;
  }

  public boolean delIncludedPackage(Target target,String id) {
	return false;
  }

  public boolean addPackage(Target target,String newpackage) {
	return false;
  }


  public boolean decodeItems(Target target) {
	decodeIntItem(target,"lastversion");
	decodeStringItem(target,"lastdate");
	decodeStringItem(target,"maintainer");
	decodeStringItem(target,"name");
	decodeStringItem(target,"description");
	decodeStringItem(target,"releasenotes");
	decodeStringItem(target,"installationnotes");
	decodeStringItem(target,"installreset");
	decodeStringAttributeItem(target,"license","type");
	decodeStringAttributeItem(target,"license","version");
	decodeStringAttributeItem(target,"license","name");
  	decodeRelatedPeople(target,"initiators","initiator");
  	decodeRelatedPeople(target,"supporters","supporter");
  	decodeRelatedPeople(target,"developers","developer");
  	decodeRelatedPeople(target,"contacts","contact");
  	decodeScreenshots(target);
  	decodeStarturls(target);
  	decodePackageDepends(target,"packagedepends");
  	decodePublishProvider(target);
	return true;
  }

  public boolean decodeRelatedPeople(Target target,String type,String subtype) {
	ExtendedDocumentReader reader=target.getReader();
	ArrayList<Person> list=new ArrayList<Person>();
        org.w3c.dom.Node n=reader.getElementByPath(prefix+"."+type);
       	org.w3c.dom.Node n2=n.getFirstChild();
      	while (n2!=null) {
       		if (n2.getNodeName().equals(subtype)) {
			Person p=new Person();
        		NamedNodeMap nm=n2.getAttributes();
              		if (nm!=null) {
                       		org.w3c.dom.Node n3=nm.getNamedItem("name");
                       		if (n3!=null) p.setName(n3.getNodeValue());
                       		n3=nm.getNamedItem("company");
                       		if (n3!=null) p.setCompany(n3.getNodeValue());
                       		n3=nm.getNamedItem("reason");
                       		if (n3!=null) p.setReason(n3.getNodeValue());
                       		n3=nm.getNamedItem("mailto");
                       		if (n3!=null) p.setMailto(n3.getNodeValue());
			}
			list.add(p);
		}
        	n2=n2.getNextSibling();
	}
	target.setItem(type,list);
	return true;
  }


  public boolean decodePackageDepends(Target target,String itemname) {
	ExtendedDocumentReader reader=target.getReader();
	ArrayList<PackageDepend> list=new ArrayList<PackageDepend>();
        org.w3c.dom.Node n=reader.getElementByPath(prefix+".packagedepends");
	if (n!=null) {
       	org.w3c.dom.Node n2=n.getFirstChild();
      	while (n2!=null) {
       		if (n2.getNodeName().equals("package")) {
			PackageDepend pd=new PackageDepend();
        		NamedNodeMap nm=n2.getAttributes();
              		if (nm!=null) {
                       		org.w3c.dom.Node n3=nm.getNamedItem("name");
                       		if (n3!=null) pd.setName(n3.getNodeValue());
                       		n3=nm.getNamedItem("type");
                       		if (n3!=null) pd.setType(n3.getNodeValue());
                       		n3=nm.getNamedItem("maintainer");
                       		if (n3!=null) pd.setMaintainer(n3.getNodeValue());
                       		n3=nm.getNamedItem("version");
                       		if (n3!=null) pd.setVersion(n3.getNodeValue());
                       		n3=nm.getNamedItem("versionmode");
                       		if (n3!=null) pd.setVersionMode(n3.getNodeValue());
				list.add(pd);
			}
		}
        	n2=n2.getNextSibling();
		}
	}
	target.setItem(itemname,list);
	return true;
  }


  public boolean decodeScreenshots(Target target) {
	ExtendedDocumentReader reader=target.getReader();
    org.w3c.dom.Node n=reader.getElementByPath(prefix+".screenshots");
	if (n!=null) {
       	org.w3c.dom.Node n2=n.getFirstChild();
      	while (n2!=null) {
       		if (n2.getNodeName().equals("screenshot")) {
        		NamedNodeMap nm=n2.getAttributes();
              		if (nm!=null) {
				String name="";
				String file="";
				String description="";
                       		org.w3c.dom.Node n3=nm.getNamedItem("name");
                       		if (n3!=null) name=n3.getNodeValue();
                       		n3=nm.getNamedItem("file");
                       		if (n3!=null) file=n3.getNodeValue();
				n3=n2.getFirstChild();
                       		if (n3!=null) description=n3.getNodeValue();
				target.addScreenshot(name,file,description);

			}
		}
        	n2=n2.getNextSibling();
		}
	}
	return true;
  }


  public boolean decodeStarturls(Target target) {
	ExtendedDocumentReader reader=target.getReader();
    org.w3c.dom.Node n=reader.getElementByPath(prefix+".starturls");
	if (n!=null) {
       	org.w3c.dom.Node n2=n.getFirstChild();
      	while (n2!=null) {
       		if (n2.getNodeName().equals("url")) {
        		NamedNodeMap nm=n2.getAttributes();
              		if (nm!=null) {
				String name="";
				String link="";
				String description="";
                       		org.w3c.dom.Node n3=nm.getNamedItem("name");
                       		if (n3!=null) name=n3.getNodeValue();
                       		n3=nm.getNamedItem("link");
                       		if (n3!=null) link=n3.getNodeValue();
				n3=n2.getFirstChild();
                       		if (n3!=null) description=n3.getNodeValue();
				target.addStarturl(name,link,description);

			}
		}
        	n2=n2.getNextSibling();
		}
	}
	return true;
  }

   public String getItemStringValue(Target target,String name) {
	Object o=target.getItem(name);
	if (o!=null) return (String)o;
	return "";
   }

   public int getItemIntValue(Target target,String name) {
	Object o=target.getItem(name);
	if (o!=null) return ((Integer)o).intValue();
	return -1;
   }

   public String getDefaultXMLHeader(Target target) {
       String body="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";        body+="<!DOCTYPE "+prefix+" PUBLIC \"-//MMBase/DTD "+prefix+" config 1.0//EN\" \"https://www.mmbase.org/dtd/"+prefix+"_1_0.dtd\">\n";
        body+="<"+prefix+">\n";
	return body;
   }

   public String getDefaultXMLMetaInfo(Target target) {
	int lastversion=getLastVersion(target);
	if (lastversion==-1) lastversion=0;
        String body="\t<lastversion>"+lastversion+"</lastversion>\n";
        body+="\t<lastdate>"+getLastDate(target)+"</lastdate>\n";
        body+="\t<name>"+getName(target)+"</name>\n";
        body+="\t<maintainer>"+getMaintainer(target)+"</maintainer>\n";
        body+="\t<description>"+getDescription(target)+"</description>\n";
	body+="\t<license type=\""+getLicenseType(target)+"\" version=\""+getLicenseVersion(target)+"\" name=\""+getLicenseName(target)+"\" />\n";
	body+="\t<releasenotes>"+getReleaseNotes(target)+"</releasenotes>\n";
	body+="\t<installationnotes>"+getInstallationNotes(target)+"</installationnotes>\n";
	body+="\t<installreset>"+getInstallReset(target)+"</installreset>\n";
	return body;
   }

   public String getDefaultXMLFooter(Target target) {
        String body="</"+prefix+">\n";
	return body;
   }

   public boolean updatePackageTime(Target target,Date d,int newversion) {
	target.setItem("lastdate",d.toString());
	target.setItem("lastversion",new Integer(newversion));
	return true;
   }

   public void setDefaults(Target target) {
   }

   public String getBuildPath() {
	String path=PackageManager.getConfigPath()+File.separator+"packaging"+File.separator+"build"+File.separator;
	File dir=new File(path);
	if (!dir.exists()) {
		dir.mkdir();
	}
	return path;
   }



  public boolean decodePublishProvider(Target target) {
	ExtendedDocumentReader reader=target.getReader();
    org.w3c.dom.Node n=reader.getElementByPath(prefix+".publishprovider");
	if (n!=null) {
        	NamedNodeMap nm=n.getAttributes();
        	if (nm!=null) {
                	org.w3c.dom.Node n2=nm.getNamedItem("name");
                	if (n2!=null) {
                       		target.setPublishProvider(n2.getNodeValue());
			}
                	n2=nm.getNamedItem("state");
                	if (n2!=null) {
                       		if (n2.getNodeValue().equals("active")) {
					target.setPublishState(true);
				}
			}
                	n2=nm.getNamedItem("sharepassword");
                	if (n2!=null) {
                       		target.setPublishSharePassword(n2.getNodeValue());
			}
                }

	}
	return true;
  }


  public boolean renameTempFile(String filename) {
            File f1 = new File(filename + ".tmp");
            File f2 = new File(filename + ".mmp");
	    if (f2.exists()) {
		f2.delete();
	    }
            f2 = new File(filename + ".mmp");
            if (f1.renameTo(f2)) {
		return true;
            } else {
		log.error("rename failed from : "+filename+".tmp  to : "+filename + ".mmp");
		return false;
	    }
  }

  public String getDefaultTargetName() {
	log.error("Creator doesn't supply a default target name, method should be overriden");
	return "unknown";
  }

  public String getState() {
	return state;
  }

  public void addRelatedTargetsCreate(Target t) {
      relatedtargetcreate.add(t);
  }

    /**
     *  Sets the progressBar attribute of the BasicPackage object
     *
     * @param  stepcount  The new progressBar value
     */
    public void setProgressBar(int stepcount) {
        progressbar = 1;
        progressstep = 100 / (float) stepcount;
    }


    /**
     *  Sets the progressBar attribute of the BasicPackage object
     *
     * @param  stepcount  The new progressBar value
     */
    public void setSubProgressBar(int substepcount) {
        subprogressbar = 1;
        subprogressstep = 100 / (float) substepcount;
    }


    /**
     *  Description of the Method
     */
    public void increaseProgressBar() {
        increaseProgressBar(1);
    }


    /**
     *  Description of the Method
     */
    public void increaseSubProgressBar() {
        increaseSubProgressBar(1);
    }


    /**
     *  Description of the Method
     *
     * @param  stepcount  Description of the Parameter
     */
    public void increaseProgressBar(int stepcount) {
        progressbar += (stepcount * progressstep);
    }


    /**
     *  Description of the Method
     *
     * @param  stepcount  Description of the Parameter
     */
    public void increaseSubProgressBar(int substepcount) {
        subprogressbar += (substepcount * subprogressstep);
    }


    /**
     *  Gets the progressBarValue attribute of the BasicPackage object
     *
     * @return    The progressBarValue value
     */
    public int getProgressBarValue() {
        return (int) progressbar;
    }


    /**
     *  set the progressBarValue attribute of the BasicPackage object
     */
    public void setProgressBarValue(int value) {
	progressbar=value;
    }


    /**
     *  set the subprogressBarValue attribute of the BasicPackage object
     */
    public void setSubProgressBarValue(int value) {
	subprogressbar=value;
    }


    /**
     *  Gets the progressBarValue attribute of the BasicPackage object
     *
     * @return    The progressBarValue value
     */
    public int getSubProgressBarValue() {
        return (int) subprogressbar;
    }



}

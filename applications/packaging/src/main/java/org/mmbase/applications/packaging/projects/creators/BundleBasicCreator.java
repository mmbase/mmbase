/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

 */

package org.mmbase.applications.packaging.projects.creators;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.jar.*;

import org.mmbase.applications.packaging.PackageManager;
import org.mmbase.applications.packaging.ProviderManager;
import org.mmbase.applications.packaging.packagehandlers.PackageInterface;
import org.mmbase.applications.packaging.projects.IncludedPackage;
import org.mmbase.applications.packaging.projects.Target;
import org.mmbase.applications.packaging.projects.packageStep;
import org.mmbase.applications.packaging.util.ExtendedDocumentReader;
import org.mmbase.util.xml.EntityResolver;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.w3c.dom.NamedNodeMap;

/**
 * BundleBasicCreator, Handler for basic bundles
 *
 * @author Daniel Ockeloen (MMBased)
 */
public class BundleBasicCreator extends BasicCreator implements CreatorInterface {

    private static Logger log = Logging.getLoggerInstance(BundleBasicCreator.class);

    public static final String DTD_PACKAGING_BUNDLE_BASIC_1_0 = "packaging_bundle_basic_1_0.dtd";
    public static final String PUBLIC_ID_PACKAGING_BUNDLE_BASIC_1_0 = "-//MMBase//DTD packaging_bundle_basic config 1.0//EN";

    public static void registerPublicIDs() {
        EntityResolver.registerPublicID(PUBLIC_ID_PACKAGING_BUNDLE_BASIC_1_0, "DTD_PACKAGING_BUNDLE_BASIC_1_0", BundleBasicCreator.class);    }

    public BundleBasicCreator() {
    	cl=BundleBasicCreator.class;
   	prefix="packaging_bundle_basic";
    }


   public boolean createPackage(Target target,int newversion) {

   	clearPackageSteps();
	// set the size of both bars on 1000 steps
        setProgressBar(1000);

        // step1
        packageStep step=getNextPackageStep();
        step.setUserFeedBack("bundle/basic packager started");

	// set to 5%
        increaseProgressBar(50);

	// lets first see if we need to create related targets
        setSubProgressBar(relatedtargetcreate.size());
       	for (Target rt : relatedtargetcreate) {
		// tricky should be make last version based on local number
		// or remote number, both ways now until i decide.
                int nv=rt.getNextVersion();
		PackageInterface p = PackageManager.getPackage(rt.getId());
		if (p != null) {
			try {
				int t=Integer.parseInt(p.getVersion());
				if (t >= nv) nv = t+1;
			} catch(Exception e) {}
		}

	        step=getNextPackageStep();
        	step.setUserFeedBack("related create : "+rt.getId()+" version "+nv+"..");
                rt.createPackage(nv);
		ProviderManager.discoverPackages();
                target.setIncludedVersion(rt.getId(),""+nv);
                ProviderManager.resetSleepCounter();
        	step.setUserFeedBack("related create : "+rt.getId()+" version "+nv+"...done");
        	increaseSubProgressBar(1);
	}
	// set to 25%
        increaseProgressBar(200);

	relatedtargetcreate = new ArrayList<Target>();


	String newfilename=getBuildPath()+getName(target).replace(' ','_')+"@"+getMaintainer(target)+"_bundle_basic_"+newversion;

	try {
  		JarOutputStream jarfile = new JarOutputStream(new FileOutputStream(newfilename+".tmb"),new Manifest());

	        step=getNextPackageStep();
       	 	step.setUserFeedBack("creating bundle.xml file...");
		createBundleMetaFile(jarfile,target,newversion);
        	step.setUserFeedBack("creating bundle.xml file...done");

		// add screenshots
		addScreenshotFiles(jarfile,target);

		// set to 35%
        	increaseProgressBar(100);

		// loop the included packages to put them in the bundle jar
		ArrayList<IncludedPackage> packages=(ArrayList<IncludedPackage>)target.getItem("includedpackages");

        	setSubProgressBar(packages.size());
        	for (IncludedPackage ip : packages) {
               		if (ip.getIncluded()) {
				// sometimes it seems to take a while before
				// the bundle to show up so wait a while for it
				PackageInterface p=PackageManager.getPackage(ip.getId(),ip.getVersion());
				int d=0;
				while (p == null && d<10) {
					try {
						Thread.sleep(1000);
					} catch(Exception e) {}
					p=PackageManager.getPackage(ip.getId(),ip.getVersion());
					d = d +1;
				}
				JarFile jf=p.getJarFile();
				if (jf!=null) {
					String includename=ip.getId()+"_"+ip.getVersion()+".mmp";
					String buildname=jf.getName();
	                		addFile(jarfile,buildname,includename,"packagefile","");
				}
			}

		}
       		increaseSubProgressBar(1);
		// set to 55%
        	increaseProgressBar(200);

		jarfile.close();
	} catch(Exception e) {
		e.printStackTrace();
	}


	// update the build file to reflect the last build, should only be done if no errors
	if (getErrorCount()==0) {
    		File f1 = new File(newfilename+".tmb");
    		File f2 = new File(newfilename+".mmb");
                if (f2.exists()) {
                   f2.delete();
                }
    		f2 = new File(newfilename+".mmb");
    		if (f1.renameTo(f2)) {
			updatePackageTime(target,new Date(),newversion);
			target.save();
                } else {
                    log.error("rename failed from : "+newfilename+".tmp  to : "+newfilename + ".mmb");
                    return false;
                }
        	step=getNextPackageStep();
        	step.setUserFeedBack("Saving new version : "+newversion);
	}
	// set to 75%
       	increaseProgressBar(200);

	// do we need to send this to a publish provider ?
	if (target.getPublishState()) {
		ProviderManager.discoverPackages();
        	step=getNextPackageStep();
        	step.setUserFeedBack("publishing to provider : "+target.getPublishProvider());
        	step=getNextPackageStep();
        	step.setUserFeedBack("sending file (version "+newversion+") : "+target.getId()+" ...");
		if (target.publish(newversion,step)) {
        		step.setUserFeedBack("sending file (version "+newversion+") : "+target.getId()+" ... done");
		} else {
        		step.setUserFeedBack("sending file (version "+newversion+") : "+target.getId()+" ... failed");
		}
	}
	// set to 95%
       	increaseProgressBar(200);

        step=getNextPackageStep();
        step.setUserFeedBack("bundle/basic packager ended : "+getErrorCount()+" errors and "+getWarningCount()+" warnings");
	// set to 100%
       	increaseProgressBar(50);
        return true;
   }


   public void createBundleMetaFile(JarOutputStream jarfile,Target target,int newversion) {
	Date d=new Date();
	String body="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
	body+="<!DOCTYPE bundle PUBLIC \"-//MMBase/DTD bundle config 1.0//EN\" \"https://www.mmbase.org/dtd/bundle_1_0.dtd\">\n";
	body+="<bundle name=\""+getName(target)+"\" type=\""+getType()+"\" maintainer=\""+getMaintainer(target)+"\" version=\""+newversion+"\" creation-date=\""+d.toString()+"\" >\n";
	body+="\t<description>"+getDescription(target)+"</description>\n";
	body+="\t<license type=\""+getLicenseType(target)+"\" version=\""+getLicenseVersion(target)+"\" name=\""+getLicenseName(target)+"\" />\n";
	body+="\t<releasenotes>\n"+getReleaseNotes(target)+"\n</releasenotes>\n";
	body+="\t<installationnotes>\n"+getInstallationNotes(target)+"\n</installationnotes>\n";
        body+="\t<installreset>"+getInstallReset(target)+"</installreset>\n";
  	body+=getScreenshotsXML(target);
  	body+=getStarturlsXML(target);
  	body+=getRelatedPeopleXML("initiators","initiator",target);
  	body+=getRelatedPeopleXML("supporters","supporter",target);
  	body+=getRelatedPeopleXML("developers","developer",target);
  	body+=getRelatedPeopleXML("contacts","contact",target);

	body+=getNeededPackagesXML(target);

	body+="</bundle>\n";
	try {
       		JarEntry entry = new JarEntry("bundle.xml");
           	jarfile.putNextEntry(entry);
       		jarfile.write(body.getBytes("UTF-8"));
	} catch (Exception e) {
		e.printStackTrace();
	}
   }


   public String getNeededPackagesXML(Target target) {
	ArrayList<IncludedPackage> packages=(ArrayList<IncludedPackage>)target.getItem("includedpackages");
	String body="\t<neededpackages>\n";
	if (packages!=null) {
        	for (IncludedPackage ip : packages) {
               		body+="\t\t<package name=\""+ip.getName()+"\" type=\""+ip.getType()+"\" maintainer=\""+ip.getMaintainer()+"\" version=\""+ip.getVersion()+"\" included=\""+ip.getIncluded()+"\" />\n";
		}
	}
	body+="\t</neededpackages>\n";
	return body;
     }

  public ArrayList<IncludedPackage> getIncludedPackages(Target target) {
	Object o=target.getItem("includedpackages");
	if (o!=null) {
		return (ArrayList<IncludedPackage>)o;
	}
	return null;
  }

  public boolean decodeItems(Target target) {
			super.decodeItems(target);
			// decode the needed packages
			ArrayList<IncludedPackage> includedpackages=new ArrayList<IncludedPackage>();
			ExtendedDocumentReader reader=target.getReader();
                       	org.w3c.dom.Node n=reader.getElementByPath(prefix+".neededpackages");
        		org.w3c.dom.Node n2=n.getFirstChild();
     		   	while (n2!=null) {
         		if (n2.getNodeName().equals("package")) {
              			NamedNodeMap nm=n2.getAttributes();
				String name=null;
				String type=null;
				String maintainer=null;
				String version=null;
				String included=null;
              			if (nm!=null) {
                       			org.w3c.dom.Node n3=nm.getNamedItem("name");
                       			name=n3.getNodeValue();
                       			n3=nm.getNamedItem("type");
                       			type=n3.getNodeValue();
                       			n3=nm.getNamedItem("maintainer");
                       			maintainer=n3.getNodeValue();
                       			n3=nm.getNamedItem("version");
                       			version=n3.getNodeValue();
                       			n3=nm.getNamedItem("included");
                       			included=n3.getNodeValue();
					IncludedPackage ip=new IncludedPackage();
					ip.setName(name);
					ip.setMaintainer(maintainer);
					ip.setVersion(version);
					ip.setType(type);
					if (included.equals("true")) {
						ip.setIncluded(true);
					} else {
						ip.setIncluded(false);
					}
					includedpackages.add(ip);
				}
			}
        		n2=n2.getNextSibling();
		}
		target.setItem("includedpackages",includedpackages);
        return false;
  }

  public boolean setIncludedVersion(Target target,String id,String newversion) {
	ArrayList<IncludedPackage> packages=(ArrayList<IncludedPackage>)target.getItem("includedpackages");
       	for (IncludedPackage ip : packages) {
              	if (ip.getId().equals(id)) {
			ip.setVersion(newversion);
		}
	}
	target.save();
        return true;
  }


  public boolean delIncludedPackage(Target target,String id) {
	ArrayList<IncludedPackage> packages=(ArrayList<IncludedPackage>)target.getItem("includedpackages");
       	for (IncludedPackage ip : packages) {
              	if (ip.getId().equals(id)) {
			packages.remove(ip);
			break;
		}
	}
	target.save();
        return true;
  }

   public String getXMLFile(Target target) {
        String body=getDefaultXMLHeader(target);
	body+=getDefaultXMLMetaInfo(target);
        body+=getScreenshotsXML(target);
        body+=getStarturlsXML(target);
        body+=getRelatedPeopleXML("initiators","initiator",target);
        body+=getRelatedPeopleXML("supporters","supporter",target);
        body+=getRelatedPeopleXML("developers","developer",target);
        body+=getRelatedPeopleXML("contacts","contact",target);
	body+=getNeededPackagesXML(target);
	if (target.getPublishProvider()!=null) {
		if (target.getPublishState()) {
			body+="\t<publishprovider name=\""+target.getPublishProvider()+"\" state=\"active\" sharepassword=\""+target.getPublishSharePassword()+"\" />\n";
		} else {
			body+="\t<publishprovider name=\""+target.getPublishProvider()+"\" state=\"inactive\" sharepassword=\""+target.getPublishSharePassword()+"\" />\n";
		}
	}
	body+=getDefaultXMLFooter(target);
        return body;
   }

  public boolean addPackage(Target target,String newpackage) {
	ArrayList<IncludedPackage> packages=(ArrayList<IncludedPackage>)target.getItem("includedpackages");
	PackageInterface p=PackageManager.getPackage(newpackage);
	if (p!=null) {
             IncludedPackage ip=new IncludedPackage();
             ip.setName(p.getName());
             ip.setMaintainer(p.getMaintainer());
             ip.setVersion(p.getVersion());
             ip.setType(p.getType());
             ip.setIncluded(true);
	     if (packages==null) {
		packages=new ArrayList<IncludedPackage>();
		target.setItem("includedpackages",packages);
	     }
	     packages.add(ip);
	     target.save();
             return true;
	}
        return false;
  }


}

/*
 
This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.
 
The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license
 
 */

package org.mmbase.bridge.remote.generator;
import java.util.*;
import java.io.*;

/**
 * @author Kees Jongenburger <keesj@framfab.nl>
 **/
public class RemoteGenerator{
    
    private MMCI mmci = null;
    private String targetDir= null;
    
    /*
     * main method to generate the Remote MMCI files
     * this method wil create 2 directories
     * <UL>
     *    <LI>org/mmbase/bridge/remote/rmi</LI>
     *    <LI>org/mmbase/bridge/remote/implementation</LI>
     * </UL>
     * @param targetDir the root directory of the mmbase source where the generated sources
     * should be (e.q) /home/mmbase/src/
     * @param mmciFile the location of the MMCI.xml file generated by org.mmbase.bridge.generator.MMCI
     */
    public RemoteGenerator(String targetDir,String mmciFile) throws Exception{
        //check if the org/mmbase/bridge/remote dir exists
        File file = new File(targetDir + "/org/mmbase/bridge/remote");
        if (! file.exists() || !file.isDirectory()){
            throw new Exception("directory {} does not contain a sub directory org/mmbase/bridge/remote. this is required for RemoteGenerator to work");
        }
        file = new File(targetDir + "/org/mmbase/bridge/remote/rmi");
        if (!file.exists()){
            file.mkdirs();
        }
        file = new File(targetDir + "/org/mmbase/bridge/remote/implementation");
        if (!file.exists()){
            file.mkdirs();
        }
        mmci =  MMCI.getDefaultMMCI(mmciFile);
        this.targetDir = targetDir;
        Enumeration enum = mmci.getClasses().elements();
        while(enum.hasMoreElements()){
            
            XMLClass xmlClass = (XMLClass)enum.nextElement();
            String name = xmlClass.getName();
            
            generateInterface(xmlClass);
            generateRmi(xmlClass);
            generateImplementation(xmlClass);
        }
    }
    
    /**
     * This method generates an (RMI)remote interface based on
     * an XMLClass
     */
    public void generateInterface(XMLClass xmlClass){
        String shortName = xmlClass.getShortName();
        String className = "Remote" + shortName ;
        StringBuffer sb = new StringBuffer();
        
        //create the default imports for the interface
        sb.append("package org.mmbase.bridge.remote;\n");
        sb.append("\n");
        
        sb.append("import java.util.*;\n");
        sb.append("import java.rmi.*;\n");
        sb.append("\n");
        
        sb.append("/**\n");
        sb.append(" * " +className + " is a generated interface based on "+ xmlClass.getName() +"<BR>\n");
        sb.append(" * This interface has almoost the same methods names as the "+ xmlClass.getName() + " interface.\n");
        sb.append(" * The interface is created in such way that it can implement java.rmi.Remote.\n");
        sb.append(" * Where needed other return values or parameters are used.\n");
        sb.append(" * @Author Kees Jongenburger <keesj@framfab.nl>\n");
        sb.append(" */\n");
        sb.append(" //DO NOT EDIT THIS FILE, IT IS GENERATED by org.mmbase.bridge.remote.remoteGenerator\n");
        sb.append("public interface "+ className +" extends  Remote {\n");
        System.err.println("generate interface " + className);
        
        //for every method in the XMLClass create an alternate method fot the
        //remote interface
        
        Enumeration methodsEnum = xmlClass.getMethods().elements();
        while(methodsEnum.hasMoreElements()){
            XMLMethod xmlMethod = (XMLMethod)methodsEnum.nextElement();
            if (!  xmlMethod.getName().equals("equals") && !  xmlMethod.getName().equals("hashCode") && ! xmlMethod.getName().equals("toString")){
                XMLClass returnType = xmlMethod.getReturnType();
                String retTypeName = xmlMethod.getReturnType().getShortName();
                
                //if the return type is in the MMBase bridge we need to
                //create a wrapper
                if ( xmlMethod.getReturnType().getOriginalName().indexOf("mmbase") != -1){
                    retTypeName = "Remote" + retTypeName;
                }
                
                if (returnType.isArray){
                    sb.append("   public "+ xmlMethod.getReturnType().getName() + "[] " + xmlMethod.getName() +"(");
                } else {
                    sb.append("   public "+ retTypeName + " " + xmlMethod.getName() +"(");
                }
                
                Iterator iter = xmlMethod.getParameterList().iterator();
                int counter =0;
                while(iter.hasNext()){
                    counter ++;
                    XMLClass parameter = (XMLClass)iter.next();
                    if (parameter != null){
                        if (parameter.isArray){
                            sb.append(parameter.getOriginalName() + "[] param"  + counter);
                        } else {
                            if (parameter.getOriginalName().indexOf("mmbase") != -1){
                                sb.append("Remote" + parameter.getShortName() + " param"  + counter);
                            } else {
                                sb.append(parameter.getOriginalName() + " param"  + counter);
                            }
                        }
                    } else {
                        System.err.println("Class " + xmlMethod.getName() +" Parameter == null");
                    }
                    if (iter.hasNext()){
                        sb.append(" ,");
                    }
                }
                sb.append(") throws RemoteException;\n");
            }
        }
        sb.append("\n");
        sb.append("   public String getMapperCode() throws RemoteException;\n");
        sb.append("}\n");
        try {
            File file = new File(targetDir +"/org/mmbase/bridge/remote/" + className+  ".java");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(sb.toString().getBytes());
            fos.flush();
            fos.close();
        } catch (Exception e){
            System.err.println("writeFile" + e.getMessage());
        }
    }
    /**
     * This method generates an (RMI)remote implementation based on
     * an XMLClass
     */
    public void generateRmi(XMLClass xmlClass){
        String shortName = xmlClass.getShortName();
        String className = "Remote" + shortName +"_Rmi";
        StringBuffer sb = new StringBuffer();
        sb.append("package org.mmbase.bridge.remote.rmi;\n");
        sb.append("\n");
        sb.append("import org.mmbase.bridge.*;\n");
        sb.append("import org.mmbase.util.logging.*;\n");
        sb.append("import java.util.*;\n");
        sb.append("import java.rmi.*;\n");
        sb.append("import java.rmi.server.*;\n");
        sb.append("import org.mmbase.bridge.remote.*;\n\n");
        sb.append("import org.mmbase.bridge.remote.util.StubToLocalMapper;\n\n");
        
        sb.append("/**\n");
        sb.append(" * " +className + " in a generated implementation of Remote"+ xmlClass.getShortName() +"<BR>\n");
        sb.append(" * This implementation is used by rmic to create a stub and skelton for communication between remote and server.\n");
        sb.append(" * @Author Kees Jongenburger <keesj@framfab.nl>\n");
        sb.append(" */\n");
        sb.append(" //DO NOT EDIT THIS FILE, IT IS GENERATED by remote.remote.remoteGenerator\n");
        sb.append("public class "+ className +" extends  UnicastRemoteObject implements Unreferenced,Remote"+ shortName +"  {\n");
        System.err.println("generate implementation " + className);
        
        sb.append("   //original object\n");
        sb.append("   " + xmlClass.getShortName() + " originalObject;\n\n");
        sb.append("   //mapper code\n");
        sb.append("   String mapperCode = null;\n\n");
        
        sb.append("   //logger object\n");
        sb.append("   private static Logger log = Logging.getLoggerInstance(" + className+ ".class.getName());");
        
        //constructor
        sb.append("   public "+ className +"("+ xmlClass.getShortName() + " originalObject) throws RemoteException{\n");
        sb.append("      super();\n");
        sb.append("      log.debug(\"new "+className +"\");\n");
        sb.append("      this.originalObject = originalObject;\n");
        sb.append("      mapperCode = StubToLocalMapper.add(this.originalObject);\n");
        sb.append("   }\n");
        
        Enumeration methodsEnum = xmlClass.getMethods().elements();
        while(methodsEnum.hasMoreElements()){
            XMLMethod xmlMethod = (XMLMethod)methodsEnum.nextElement();
            if (!  xmlMethod.getName().equals("equals") && !  xmlMethod.getName().equals("hashCode") && ! xmlMethod.getName().equals("toString")){
                XMLClass returnType = xmlMethod.getReturnType();
                String retTypeName = xmlMethod.getReturnType().getName();
                
                //if the return type is in the MMBase bridge we need to
                //create a wrapper
                if ( xmlMethod.getReturnType().getOriginalName().indexOf("mmbase") != -1){
                    retTypeName = "Remote" + xmlMethod.getReturnType().getShortName();
                }
                
                if (returnType.isArray){
                    sb.append("   public "+ retTypeName + "[] " + xmlMethod.getName() +"(");
                } else {
                    sb.append("   public "+ retTypeName + " " + xmlMethod.getName() +"(");
                }
                
                Iterator iter = xmlMethod.getParameterList().iterator();
                int counter =0;
                while(iter.hasNext()){
                    counter ++;
                    XMLClass parameter = (XMLClass)iter.next();
                    if (parameter.isArray){
                        sb.append(parameter.getOriginalName() + "[] param"  + counter);
                    } else {
                        if (parameter.getOriginalName().indexOf("mmbase") != -1){
                            sb.append("Remote" + parameter.getShortName() + " param"  + counter);
                        } else {
                            sb.append(parameter.getOriginalName() + " param"  + counter);
                        }
                    }
                    if (iter.hasNext()){
                        sb.append(" ,");
                        
                    }
                }
                sb.append(") throws RemoteException{\n");
                if (xmlMethod.getReturnType().getName().indexOf("void") == -1){
                    sb.append("         return ");
                } else {
                    sb.append("                ");
                }
                if (! xmlMethod.getReturnType().getOriginalName().equals(xmlMethod.getReturnType().getName())){
                    sb.append(" new " +  xmlMethod.getReturnType().getName() + "_Rmi(");
                }
                //if (
                if ( xmlMethod.getReturnType().getOriginalName().indexOf("mmbase") != -1){
                    sb.append("new Remote" +returnType.getShortName() + "_Rmi(originalObject." + xmlMethod.getName() + "(");
                } else {
                    sb.append("originalObject." + xmlMethod.getName() + "(");
                }
                
                int paramCounter =0;
                Iterator paramIter = xmlMethod.getParameterList().iterator();
                while(paramIter.hasNext()){
                    XMLClass parameter = (XMLClass)paramIter.next();
                    
                    paramCounter ++;
                    if (parameter.getOriginalName().indexOf("mmbase") != -1){
                        sb.append("("+parameter.getShortName() +")StubToLocalMapper.get(param"  + paramCounter +".getMapperCode())");
                    } else {
                        sb.append(" param"  + paramCounter);
                    }
                    if (paramIter.hasNext()){
                        sb.append(" ,");
                    }
                }
                if ( xmlMethod.getReturnType().getOriginalName().indexOf("mmbase") != -1){
                    sb.append(")");
                }
                if (! xmlMethod.getReturnType().getOriginalName().equals(xmlMethod.getReturnType().getName())){
                    sb.append(")");
                }
                sb.append(");\n");
                
                
                sb.append("   }\n");
                sb.append("\n");
            }
        }
        sb.append("\n");
        sb.append("   public String getMapperCode() throws RemoteException{\n");
        sb.append("      return mapperCode;\n");
        sb.append("   }\n");
        sb.append("\n");
        sb.append("   //clean up StubToLocalMapper when the class is unreferenced\n");
        sb.append("   public void unreferenced() {\n");
        sb.append("      if (StubToLocalMapper.remove(mapperCode)){\n");
        sb.append("         mapperCode = null;\n");
        sb.append("      }\n");
        sb.append("   }\n");
        sb.append("}\n");
        try {
            File file = new File(targetDir + "/org/mmbase/bridge/remote/rmi/" + className+  ".java");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(sb.toString().getBytes());
            fos.flush();
            fos.close();
        } catch (Exception e){
            System.err.println(e.getMessage());
        }
    }
    
    /**
     * This method generates an (Remote)bridge implementation
     */
    public void generateImplementation(XMLClass xmlClass){
        String shortName = xmlClass.getShortName();
        String className = "Remote" + shortName +"_Impl";
        StringBuffer sb = new StringBuffer();
        sb.append("package org.mmbase.bridge.remote.implementation;\n");
        sb.append("\n");
        sb.append("import java.util.*;\n");
        sb.append("import org.mmbase.bridge.*;\n");
        sb.append("import org.mmbase.bridge.remote.*;\n\n");
        sb.append("/**\n");
        sb.append(" * " +className + " in a generated implementation of "+ xmlClass.getShortName() +"<BR>\n");
        sb.append(" * This implementation is used by a local class when the MMCI is called remotely\n");
        sb.append(" * @Author Kees Jongenburger <keesj@framfab.nl>\n");
        sb.append(" */\n");
        sb.append(" //DO NOT EDIT THIS FILE. IT IS GENERATED by remote.remote.remoteGenerator\n");
        String impl = xmlClass.getShortName();
        //impl += ",java.io.Serializable";
        if (!xmlClass.getImplements().equals("")){
            impl += ",";
        }
        sb.append("public class "+ className +" implements "+ impl + xmlClass.getImplements()  +"  {\n");
        System.err.println("generate implementation " + className);
        
        sb.append("   //original object\n");
        sb.append("   " + "Remote" + xmlClass.getShortName() + " originalObject;\n\n");
        
        //constructor
        sb.append("   public "+ className +"(Remote"+ xmlClass.getShortName() + " originalObject) {\n");
        sb.append("      super();\n");
        sb.append("      this.originalObject = originalObject;\n");
        sb.append("   }\n");
        
        Enumeration methodsEnum = xmlClass.getMethods().elements();
        while(methodsEnum.hasMoreElements()){
            XMLMethod xmlMethod = (XMLMethod)methodsEnum.nextElement();
            if (!  xmlMethod.getName().equals("equals") && !  xmlMethod.getName().equals("hashCode") && ! xmlMethod.getName().equals("toString")){
                XMLClass returnType = xmlMethod.getReturnType();
                String retTypeName = xmlMethod.getReturnType().getName();
                
                //if the return type is in the MMBase bridge we need to
                //create a wrapper
                if ( xmlMethod.getReturnType().getOriginalName().indexOf("mmbase") != -1){
                    retTypeName =  xmlMethod.getReturnType().getShortName();
                }
                
                if (returnType.isArray){
                    sb.append("   public "+ retTypeName + "[] " + xmlMethod.getName() +"(");
                } else {
                    sb.append("   public "+ retTypeName + " " + xmlMethod.getName() +"(");
                }
                
                Iterator iter = xmlMethod.getParameterList().iterator();
                int counter =0;
                while(iter.hasNext()){
                    counter ++;
                    XMLClass parameter = (XMLClass)iter.next();
                    if (parameter.isArray){
                        sb.append(parameter.getOriginalName() + "[] param"  + counter);
                    } else {
                        sb.append(parameter.getOriginalName() + " param"  + counter);
                    }
                    if (iter.hasNext()){
                        sb.append(" ,");
                        
                    }
                }
                sb.append(") {\n");
                sb.append("      try {\n");
                if (xmlMethod.getReturnType().getName().indexOf("void") == -1){
                    sb.append("         return ");
                } else {
                    sb.append("                ");
                }
                if (! xmlMethod.getReturnType().getOriginalName().equals(xmlMethod.getReturnType().getName())){
                    sb.append(" new Remote" +xmlMethod.getReturnType().getName() + "_Impl(");
                }
                //if (
                if ( xmlMethod.getReturnType().getOriginalName().indexOf("mmbase") != -1){
                    sb.append("new Remote" +returnType.getShortName() + "_Impl(originalObject." + xmlMethod.getName() + "(");
                } else {
                    sb.append("originalObject." + xmlMethod.getName() + "(");
                }
                
                int paramCounter =0;
                Iterator paramIter = xmlMethod.getParameterList().iterator();
                while(paramIter.hasNext()){
                    XMLClass parameter =(XMLClass) paramIter.next();
                    paramCounter ++;
                    if (parameter.getOriginalName().indexOf("mmbase") != -1){
                        sb.append("((Remote" + parameter.getShortName() + "_Impl)param"+paramCounter +").originalObject");
                    } else {
                        sb.append("param" + paramCounter);
                    }
                    if (paramIter.hasNext()){
                        sb.append(" ,");
                    }
                }
                if ( xmlMethod.getReturnType().getOriginalName().indexOf("mmbase") != -1){
                    sb.append(")");
                }
                if (! xmlMethod.getReturnType().getOriginalName().equals(xmlMethod.getReturnType().getName())){
                    sb.append(")");
                }
                sb.append(");\n");
                
                //FIXME
                sb.append("      } catch (Exception e){ throw new BridgeException(e.getMessage());}\n");
                
                sb.append("   }\n");
                sb.append("\n");
            }
        }
        sb.append("}\n");
        try {
            File file = new File(targetDir + "/org/mmbase/bridge/remote/implementation/" + className+  ".java");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(sb.toString().getBytes());
            fos.flush();
            fos.close();
        } catch (Exception e){
            System.err.println(e.getMessage());
        }
    }
    
    /*
     * main method
     * parameters required are targetdirectory and MMCI.xml file location
     */
    public static void main(String[] argv) throws Exception{
        if (argv.length !=2 ){
            System.err.println("Usage: java org.mmbase.bridge.remote.generator.RemoteGenerator <targetdir> <mmci-xml-file>");
            System.exit(1);
        }
        new RemoteGenerator(argv[0],argv[1]);
    }
}

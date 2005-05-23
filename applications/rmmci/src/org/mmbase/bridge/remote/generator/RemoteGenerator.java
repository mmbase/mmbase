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
 * @author Kees Jongenburger <keesj@dds.nl>
 **/
public class RemoteGenerator {

    private MMCI mmci = null;
    private String targetDir = null;

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
    public RemoteGenerator(String targetDir, String mmciFile) throws Exception {
        //check if the org/mmbase/bridge/remote dir exists
        File file = new File(targetDir + "/org/mmbase/bridge/remote");
        if (!file.exists() || !file.isDirectory()) {
            throw new Exception("directory {" + file.getName() + "} does not contain a sub directory org/mmbase/bridge/remote. this is required for RemoteGenerator to work");
        }
        file = new File(targetDir + "/org/mmbase/bridge/remote/rmi");
        if (!file.exists()) {
            file.mkdirs();
        }
        file = new File(targetDir + "/org/mmbase/bridge/remote/implementation");
        if (!file.exists()) {
            file.mkdirs();
        }
        mmci = MMCI.getDefaultMMCI(mmciFile);
        this.targetDir = targetDir;
        Iterator i = mmci.getClasses().iterator();
        while (i.hasNext()) {

            XMLClass xmlClass = (XMLClass) i.next();
            String name = xmlClass.getName();

            if (needsRemote(xmlClass)) {
                generateInterface(xmlClass);
                generateRmi(xmlClass);
                generateImplementation(xmlClass);
            }
        }
        generateObjectWrappers(mmci);
    }

    public boolean needsRemote(XMLClass xmlClass) {
        return xmlClass != null && xmlClass.getOriginalName().indexOf("org.mmbase") != -1 && xmlClass.isInterface;
    }

    /**
     * This method generates an (RMI)remote interface based on
     * an XMLClass
     */
    public void generateInterface(XMLClass xmlClass) {
        String shortName = xmlClass.getShortName();
        String className = "Remote" + shortName;
        StringBuffer sb = new StringBuffer();

        //create the default imports for the interface
        sb.append("package org.mmbase.bridge.remote;\n");
        sb.append("\n");

        sb.append("import java.util.*;\n");
        sb.append("import java.rmi.*;\n");
        sb.append("import org.mmbase.security.*;\n");
        sb.append("import org.mmbase.cache.*;\n");
        sb.append("\n");

        sb.append("/**\n");
        sb.append(" * " + className + " is a generated interface based on " + xmlClass.getName() + "<br />\n");
        sb.append(" * This interface has almost the same methods names as the " + xmlClass.getName() + " interface.\n");
        sb.append(" * The interface is created in such way that it can implement java.rmi.Remote.\n");
        sb.append(" * Where needed other return values or parameters are used.\n");
        sb.append(" * @Author Kees Jongenburger <keesj@dds.nl>\n");
        sb.append(" */\n");
        sb.append(" //DO NOT EDIT THIS FILE, IT IS GENERATED by org.mmbase.bridge.remote.remoteGenerator\n");

        String impl = " ServerMappedObject";
        //impl += ",java.io.Serializable";

        String m = xmlClass.getImplements();
        StringTokenizer st = new StringTokenizer(m, ",");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            XMLClass xmlc = mmci.getClass(token);
            if (needsRemote(xmlc)) {
                impl += ", Remote" + xmlc.getShortName();
            } else {
                //impl += ", " + token;
            }
        }

        if (xmlClass.isInterface) {
            System.err.println("generate interface " + className);
            sb.append("public interface " + className + " extends  " + impl + "{\n");
        } else {
            System.err.println("No need to generated class " + className);
            return;
        }

        //for every method in the XMLClass create an alternate method fot the
        //remote interface

        Iterator methodsIt = xmlClass.getMethods().iterator();
        while (methodsIt.hasNext()) {
            XMLMethod xmlMethod = (XMLMethod)methodsIt.next();
            String methodName = xmlMethod.getName();
            if (methodName.equals("equals") || methodName.equals("hashCode") || methodName.equals("toString") || methodName.equals("clone")) {
                methodName = "wrapped_" + methodName;
            }
            XMLClass returnType = xmlMethod.getReturnType();
            if (returnType == null) {
                System.err.println("Return type of " + xmlMethod + " is null");
            }
            String retTypeName = xmlMethod.getReturnType().getShortName();

            //if the return type is in the MMBase bridge we need to
            //create a wrapper
            if (needsRemote(xmlMethod.getReturnType())) {
                retTypeName = "Remote" + retTypeName;
            }

            if (returnType.isArray) {
                sb.append("   public " + retTypeName + "[] " + methodName + "(");
            } else {
                sb.append("   public " + retTypeName + " " + methodName + "(");
            }

            Iterator iter = xmlMethod.getParameterList().iterator();
            int counter = 0;
            while (iter.hasNext()) {
                counter++;
                XMLClass parameter = (XMLClass)iter.next();
                if (parameter != null) {
                    if (parameter.isArray) {
                        if (needsRemote(parameter)) {
                            sb.append("Remote" + parameter.getShortName() + "[] param" + counter);
                        } else {
                            sb.append(parameter.getOriginalName() + "[] param" + counter);
                        }
                    } else {
                        if (needsRemote(parameter)) {
                            sb.append("Remote" + parameter.getShortName() + " param" + counter);
                        } else {
                            sb.append(parameter.getOriginalName() + " param" + counter);
                        }
                    }
                } else {
                    System.err.println("Class " + xmlMethod.getName() + " Parameter == null");
                }
                if (iter.hasNext()) {
                    sb.append(" ,");
                }
            }
            sb.append(") throws RemoteException;\n");
        }
        sb.append("}\n");
        try {
            File file = new File(targetDir + "/org/mmbase/bridge/remote/" + className + ".java");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(sb.toString().getBytes());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            System.err.println("writeFile" + e.getMessage());
        }
    }
    /**
     * This method generates an (RMI)remote implementation based on
     * an XMLClass
     */
    public void generateRmi(XMLClass xmlClass) {
        String shortName = xmlClass.getShortName();
        String className = "Remote" + shortName + "_Rmi";
        StringBuffer sb = new StringBuffer();
        sb.append("package org.mmbase.bridge.remote.rmi;\n");
        sb.append("\n");
        sb.append("import org.mmbase.bridge.*;\n");
        sb.append("import org.mmbase.core.FieldType;\n");
        sb.append("import org.mmbase.security.*;\n");
        sb.append("import org.mmbase.storage.search.*;\n");
        sb.append("import org.mmbase.util.functions.*;\n");
        sb.append("import org.mmbase.util.logging.*;\n");
        sb.append("import java.util.*;\n");
        sb.append("import java.rmi.*;\n");
        sb.append("import java.rmi.server.*;\n");
        sb.append("import org.mmbase.bridge.remote.*;\n\n");
        sb.append("import org.mmbase.bridge.remote.util.*;\n\n");

        sb.append("/**\n");
        sb.append(" * " + className + " in a generated implementation of Remote" + xmlClass.getShortName() + "<BR>\n");
        sb.append(" * This implementation is used by rmci to create a stub and skeleton for communication between remote and server.\n");
        sb.append(" * @Author Kees Jongenburger <keesj@dds.nl>\n");
        sb.append(" */\n");
        sb.append(" //DO NOT EDIT THIS FILE, IT IS GENERATED by remote.remote.remoteGenerator\n");
        String impl = "";

        if (needsRemote(xmlClass)) {
            String m = xmlClass.getImplements();
            StringTokenizer st = new StringTokenizer(m, ",");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                //XMLClass xmlc = mmci.getClass(xmlClass.getImplements());
                XMLClass xmlc = mmci.getClass(token);
                if (xmlc != null && needsRemote(xmlc)) {
                    impl += ", Remote" + xmlc.getShortName();
                } else {
                    //impl = ", " + token;
                }
            }
        }

        sb.append("public class " + className + " extends  UnicastRemoteObject implements Unreferenced,Remote" + shortName + impl + "  {\n");
        System.err.println("generate implementation " + className);

        sb.append("   //original object\n");
        sb.append("   " + xmlClass.getShortName() + " originalObject;\n\n");
        sb.append("   //mapper code\n");
        sb.append("   String mapperCode = null;\n\n");

        sb.append("   private static Logger log = Logging.getLoggerInstance(" + className + ".class);\n");

        //constructor
        sb.append("   public " + className + "(" + xmlClass.getShortName() + " originalObject) throws RemoteException{\n");
        sb.append("      super();\n");
        sb.append("      log.debug(\"new " + className + "\");\n");
        sb.append("      this.originalObject = originalObject;\n");
        sb.append("      mapperCode = StubToLocalMapper.add(this.originalObject);\n");
        sb.append("   }\n");

        Iterator methodsIt = xmlClass.getMethods().iterator();
        while (methodsIt.hasNext()) {
            XMLMethod xmlMethod = (XMLMethod)methodsIt.next();
            String methodName = xmlMethod.getName();
            if (methodName.equals("equals") || methodName.equals("hashCode") || methodName.equals("toString") || methodName.equals("clone")) {
                methodName = "wrapped_" + methodName;
            }
            XMLClass returnType = xmlMethod.getReturnType();
            String retTypeName = xmlMethod.getReturnType().getName();

            //if the return type is in the MMBase bridge we need to
            //create a wrapper
            if (needsRemote(xmlMethod.getReturnType())) {
                retTypeName = "Remote" + xmlMethod.getReturnType().getShortName();
            }

            if (returnType.isArray) {
                sb.append("   public " + retTypeName + "[] " + methodName + "(");
            } else {
                sb.append("   public " + retTypeName + " " + methodName + "(");
            }

            Iterator iter = xmlMethod.getParameterList().iterator();
            int counter = 0;
            while (iter.hasNext()) {
                counter++;
                XMLClass parameter = (XMLClass)iter.next();
                if (parameter.isArray) {
                    if (needsRemote(parameter)) {
                        sb.append("Remote" + parameter.getShortName() + "[] param" + counter);
                    } else {
                        sb.append(parameter.getOriginalName() + "[] param" + counter);
                    }
                } else {
                    if (needsRemote(parameter)) {
                        sb.append("Remote" + parameter.getShortName() + " param" + counter);
                    } else {
                        sb.append(parameter.getOriginalName() + " param" + counter);
                    }
                }
                if (iter.hasNext()) {
                    sb.append(" ,");
                }
            }
            sb.append(") throws RemoteException{\n");

            int paramCounter = 0;
            Iterator paramIter = xmlMethod.getParameterList().iterator();
            while (paramIter.hasNext()) {
                XMLClass parameter = (XMLClass)paramIter.next();
                paramCounter++;
                if (needsRemote(parameter)) {
                    if (parameter.isArray) {
                        sb.append("         " + parameter.getShortName() + "[] localparam" + paramCounter + " = new " + parameter.getShortName() + "[param" + paramCounter + ".length];\n");
                        sb.append("         for(int i = 0; i <param" + paramCounter + ".length; i++ ) {\n");
                        sb.append("             localparam" + paramCounter + "[i] = "+
                          "(" + parameter.getShortName() + ")StubToLocalMapper.get(param" + paramCounter + "[i] == null ? \"\" + null : param" + paramCounter + "[i].getMapperCode());");
                        sb.append("         }\n");
                    } else {
                        sb.append(parameter.getShortName() +" localparam" + paramCounter + " = "+
                          "(" + parameter.getShortName() + ")StubToLocalMapper.get(param" + paramCounter + " == null ? \"\" + null : param" + paramCounter + ".getMapperCode());");
                    }
                }
            }

            if (xmlMethod.getReturnType().getName().indexOf("void") == -1) {
                if (needsRemote(xmlMethod.getReturnType())) {
                    if (!xmlMethod.getReturnType().isArray) {
                        sb.append("         Remote" + xmlMethod.getReturnType().getShortName() + " retval =(Remote" + xmlMethod.getReturnType().getShortName() + ")");
                    } else {
                        sb.append("         Remote" + xmlMethod.getReturnType().getShortName() + "[] retval =(Remote" + xmlMethod.getReturnType().getShortName() + "[])");
                    }
                } else {
                    if (!xmlMethod.getReturnType().isArray) {
                        sb.append("         " + xmlMethod.getReturnType().getName() + " retval =(" + xmlMethod.getReturnType().getName() + ")");
                    } else {
                        sb.append("         " + xmlMethod.getReturnType().getName() + "[] retval =(" + xmlMethod.getReturnType().getName() + "[])");
                    }

                }
            }

            String typeName = xmlMethod.getReturnType().getOriginalName();
            if (needsRemote(xmlMethod.getReturnType()) || typeName.equals("java.lang.Object") || typeName.equals("java.util.List") || typeName.equals("java.util.SortedSet")) {
                sb.append("ObjectWrapper.localToRMIObject(originalObject." + xmlMethod.getName() + "(");
            } else {
                sb.append("originalObject." + xmlMethod.getName() + "(");
            }

            paramCounter = 0;
            paramIter = xmlMethod.getParameterList().iterator();
            while (paramIter.hasNext()) {
                XMLClass parameter = (XMLClass)paramIter.next();

                paramCounter++;
                if (needsRemote(parameter)) {
                    sb.append(" localparam" + paramCounter);
                } else if ((parameter.getOriginalName().equals("java.lang.Object") || parameter.getOriginalName().equals("java.util.List")|| parameter.getOriginalName().equals("java.util.SortedSet")) && !parameter.isArray) {
                    sb.append("(" + parameter.getName() + ")ObjectWrapper.rmiObjectToLocal(param" + paramCounter + ")");
                } else {
                    sb.append(" param" + paramCounter);
                }
                if (paramIter.hasNext()) {
                    sb.append(" ,");
                }
            }
            if (needsRemote(xmlMethod.getReturnType()) || typeName.equals("java.lang.Object") || typeName.equals("java.util.List") || typeName.equals("java.util.SortedSet")) {
                sb.append(")");
            }
            if (!xmlMethod.getReturnType().getOriginalName().equals(xmlMethod.getReturnType().getName())) {
                sb.append(")");
            }
            sb.append(");\n");

            if (xmlMethod.getReturnType().getName().indexOf("void") == -1) {
                sb.append("return retval;\n");
            }
            sb.append("   }\n");
            sb.append("\n");
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
            File file = new File(targetDir + "/org/mmbase/bridge/remote/rmi/" + className + ".java");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(sb.toString().getBytes());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * This method generates an (Remote)bridge implementation
     */
    public void generateImplementation(XMLClass xmlClass) {
        String shortName = xmlClass.getShortName();
        String className = "Remote" + shortName + "_Impl";
        StringBuffer sb = new StringBuffer();
        sb.append("package org.mmbase.bridge.remote.implementation;\n");
        sb.append("\n");
        sb.append("import java.util.*;\n");
        sb.append("import org.mmbase.bridge.*;\n");
        sb.append("import org.mmbase.core.FieldType;\n");
        sb.append("import org.mmbase.storage.search.*;\n");
        sb.append("import org.mmbase.util.functions.*;\n");
        sb.append("import org.mmbase.bridge.remote.*;\n");
        sb.append("import org.mmbase.security.*;\n\n");
        sb.append("import org.mmbase.bridge.remote.util.*;\n\n");
        sb.append("/**\n");
        sb.append(" * " + className + " in a generated implementation of " + xmlClass.getShortName() + "<BR>\n");
        sb.append(" * This implementation is used by a local class when the MMCI is called remotely\n");
        sb.append(" * @Author Kees Jongenburger <keesj@dds.nl>\n");
        sb.append(" */\n");
        sb.append(" //DO NOT EDIT THIS FILE. IT IS GENERATED by remote.remote.remoteGenerator\n");
        String impl = xmlClass.getShortName() + ",MappedObject";
        //impl += ",java.io.Serializable";
        if (!xmlClass.getImplements().equals("")) {
            impl += ",";
        }
        String extendsString = "";
        if (xmlClass.getImplements().indexOf("List") != -1 && xmlClass.getImplements().indexOf("Iterator") == -1) {
            extendsString = " extends AbstractList ";
        }
        sb.append("public class " + className + extendsString + " implements " + impl + xmlClass.getImplements() + "  {\n");
        System.err.println("generate implementation " + className);

        sb.append("   //original object\n");
        sb.append("   " + "Remote" + xmlClass.getShortName() + " originalObject;\n\n");

        //constructor
        sb.append("   public " + className + "(Remote" + xmlClass.getShortName() + " originalObject) {\n");
        sb.append("      super();\n");
        sb.append("      this.originalObject = originalObject;\n");
        sb.append("   }\n");

        Iterator methodsIt = xmlClass.getMethods().iterator();
        while (methodsIt.hasNext()) {
            XMLMethod xmlMethod = (XMLMethod)methodsIt.next();
            String methodName = xmlMethod.getName();

            boolean wrapped = false;
            if (!methodName.equals("toArray") && !methodName.equals("iterator") && !methodName.equals("listIterator")) {

                if (methodName.equals("equals") || methodName.equals("hashCode") || methodName.equals("toString") || methodName.equals("clone")) {
                    wrapped = true;
                }
                XMLClass returnType = xmlMethod.getReturnType();
                String retTypeName = xmlMethod.getReturnType().getName();

                //if the return type is in the MMBase bridge we need to
                //create a wrapper
                if (needsRemote(xmlMethod.getReturnType())) {
                    retTypeName = xmlMethod.getReturnType().getShortName();
                }

                if (returnType.isArray) {
                    sb.append("   public " + retTypeName + "[] " + xmlMethod.getName() + "(");
                } else {
                    sb.append("   public " + retTypeName + " " + xmlMethod.getName() + "(");
                }

                Iterator iter = xmlMethod.getParameterList().iterator();
                int counter = 0;
                while (iter.hasNext()) {
                    counter++;
                    XMLClass parameter = (XMLClass)iter.next();
                    if (parameter.isArray) {
                        sb.append(parameter.getOriginalName() + "[] param" + counter);
                    } else {
                        sb.append(parameter.getOriginalName() + " param" + counter);
                    }
                    if (iter.hasNext()) {
                        sb.append(" ,");

                    }
                }
                sb.append(") {\n");
                sb.append("      try {\n");

                int paramCounter = 0;
                Iterator paramIter = xmlMethod.getParameterList().iterator();
                while (paramIter.hasNext()) {
                    XMLClass parameter = (XMLClass)paramIter.next();
                    paramCounter++;
                    if (needsRemote(parameter)) {
                        if (parameter.isArray) {
                            sb.append("         Remote" + parameter.getShortName() + "[] remoteparam" + paramCounter + " = new Remote" + parameter.getShortName() + "[param" + paramCounter + ".length];\n");
                            sb.append("         for(int i = 0; i <param" + paramCounter + ".length; i++ ) {\n");
                            sb.append("             remoteparam" + paramCounter + "[i] = (Remote" + parameter.getShortName() + ")( param" + paramCounter + "[i] == null ? null : ((MappedObject) param" + paramCounter + "[i]).getWrappedObject());\n");
                            sb.append("         }\n");
                        } else {
                            sb.append("         Remote" + parameter.getShortName() + " remoteparam" + paramCounter + " = (Remote" + parameter.getShortName() + ")( param" + paramCounter + " == null ? null : ((MappedObject) param" + paramCounter + ").getWrappedObject());\n");
                        }
                    }
                }

                //**
                if (xmlMethod.getReturnType().getName().indexOf("void") == -1) {
                    if (!xmlMethod.getReturnType().isArray) {
                        sb.append("         " + xmlMethod.getReturnType().getName() + " retval =(" + xmlMethod.getReturnType().getName() + ")");
                    } else {
                        sb.append("         " + xmlMethod.getReturnType().getName() + "[] retval =(" + xmlMethod.getReturnType().getName() + "[])");
                    }
                }

                String typeName = xmlMethod.getReturnType().getOriginalName();
                if (needsRemote(xmlMethod.getReturnType())  || typeName.equals("java.lang.Object") || typeName.equals("java.util.List") ||  typeName.equals("java.util.SortedSet")) {
                    sb.append("ObjectWrapper.rmiObjectToRemoteImplementation(originalObject." + (wrapped ? "wrapped_" : "") + xmlMethod.getName() + "(");
                } else {
                    sb.append("originalObject." + (wrapped ? "wrapped_" : "") + xmlMethod.getName() + "(");
                }
                //sb.append("originalObject." + (wrapped ? "wrapped_" : "") + xmlMethod.getName() + "(");

                paramCounter = 0;
                paramIter = xmlMethod.getParameterList().iterator();
                while (paramIter.hasNext()) {
                    XMLClass parameter = (XMLClass)paramIter.next();
                    paramCounter++;
                    if (needsRemote(parameter)) {
                        sb.append("remoteparam" + paramCounter);
                    } else {
                        if (parameter.getOriginalName().equals("java.lang.Object") || parameter.getOriginalName().equals("java.util.List") || parameter.getOriginalName().equals("java.util.SortedSet")) {
                            String sss = className.substring(6, className.length() - 9);
                            if (sss.equals("String")) {
                                sb.append("param" + paramCounter);
                            } else {
                                sb.append("(" + parameter.getName() + ")ObjectWrapper.remoteImplementationToRMIObject(param" + paramCounter + ")");
                            }
                        } else {
                            sb.append("param" + paramCounter);
                        }
                    }
                    if (paramIter.hasNext()) {
                        sb.append(" ,");
                    }
                }
                if (needsRemote(xmlMethod.getReturnType())  || typeName.equals("java.lang.Object") || typeName.equals("java.util.List")||  typeName.equals("java.util.SortedSet")) {
                    sb.append(")");
                }
                //sb.append(")");
                sb.append(");\n");
                if (typeName.indexOf("void") == -1) {

                    sb.append("    return retval;\n");
                }

                sb.append("      } catch (Exception e){ if (e instanceof BridgeException){ throw (BridgeException)e ;} else {throw new BridgeException(e.getMessage(),e);}}\n");

                sb.append("   }\n");
                sb.append("\n");
            }
        }
        sb.append(" public String getMapperCode(){ String code =null; try {code = originalObject.getMapperCode();} catch (Exception e){} return code ;}\n");
        sb.append(" public Object getWrappedObject(){  return originalObject ;}\n");
        sb.append("}\n");
        try {
            File file = new File(targetDir + "/org/mmbase/bridge/remote/implementation/" + className + ".java");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(sb.toString().getBytes());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * @javadoc
     */
    void generateObjectWrappers(MMCI mmci) {
        System.out.println("Creating ObjectWrapperHelper");
        StringBuffer helper = new StringBuffer();
        helper.append("package org.mmbase.bridge.remote;");
        helper.append("import java.util.*;\n");
        helper.append("import java.rmi.*;\n");
        helper.append("import java.util.Vector;\n");

        helper.append("import org.mmbase.bridge.*;\n");
        helper.append("import org.mmbase.core.FieldType;\n");
        helper.append("import org.mmbase.security.*;\n");
        helper.append("import org.mmbase.bridge.remote.*;\n");
        helper.append("import org.mmbase.bridge.remote.rmi.*;\n");
        helper.append("import org.mmbase.bridge.remote.implementation.*;\n");

        helper.append("import org.mmbase.storage.search.*;\n");
        helper.append("import org.mmbase.storage.search.Step;\n");
        helper.append("import org.mmbase.util.functions.*;\n");
        helper.append("import org.mmbase.util.logging.*;\n");

        helper.append("public abstract class ObjectWrapperHelper {\n");

        StringBuffer sb = new StringBuffer();
        StringBuffer sb2 = new StringBuffer();
        List v = new ArrayList(mmci.getClasses());
        List w = new ArrayList();
        System.out.println("Sorting " + v);

        // now handle more specific classes
        int specificity = 0;
        int currentSize = w.size() - 1;
        while (v.size() > 0) {
            specificity++;
            System.out.println("specificity:" + specificity);
            if (w.size() == currentSize) {
                System.err.println("ERROR: Could not resolve order in ObjectWrapperHelper");
                w.add(0, v);
            }
            currentSize = w.size();
            for (Iterator i = v.iterator(); i.hasNext();) {
                XMLClass xml = (XMLClass) i.next();
                if (w.containsAll(getSuperClasses(xml))) {
                    w.add(0, xml);
                    i.remove();
                    break;
                }
            }
        }


        //System.out.println("Result " + v);
        Iterator i = w.iterator();

        sb.append("public static Object localToRMIObject(Object o) throws RemoteException {\n");
        sb.append("		Object retval = null;\n");
        sb2.append("public static Object rmiObjectToRemoteImplementation(Object o) throws RemoteException {\n");
        sb2.append("		Object retval = null;\n");

        boolean isFirst = true;
        while (i.hasNext()) {

            XMLClass xmlClass = (XMLClass) i.next ();
            String name = xmlClass.getName();

            if (needsRemote(xmlClass)) {
                if (!isFirst) {
                    sb.append("} else");
                    sb2.append("} else");
                }
                sb.append(" if (o instanceof " + xmlClass.getShortName() + ") {\n");
                sb.append("retval = new Remote" + xmlClass.getShortName() + "_Rmi((" + xmlClass.getShortName() + ")o);\n");

                sb2.append(" if (o instanceof Remote" + xmlClass.getShortName() + ") {\n");
                sb2.append("retval = new Remote" + xmlClass.getShortName() + "_Impl((Remote" + xmlClass.getShortName() + ")o);\n");
                isFirst = false;
            }
        }
        sb.append("		}\n;return retval ;\n}\n");
        sb2.append("	}\n;	return retval ;\n}\n");
        helper.append(sb);
        helper.append(sb2);
        helper.append("}\n");
        //System.out.println(helper.toString());
        try {
            File file = new File(targetDir + "/org/mmbase/bridge/remote/ObjectWrapperHelper.java");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(helper.toString().getBytes());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    static List getSubClasses(XMLClass xmlClass) {
        List retval = new ArrayList();
        MMCI mmci = null;
        try {
            mmci = MMCI.getDefaultMMCI();
        } catch (Exception e) {
            System.err.println("can not get MMCI");
        }
        List v = mmci.getClasses();
        Iterator iter = v.iterator();
        while (iter.hasNext()) {
            XMLClass f = (XMLClass)iter.next();
            List list = getSuperClasses(f);
            for (int x = 0; x < list.size(); x++) {
                XMLClass listItem = (XMLClass)list.get(x);
                if (listItem.getName().equals(xmlClass.getName())) {
                    retval.add(f);
                    retval.addAll(getSubClasses(f));
                    //System.err.println(xmlClass.getName() + " has subclass " + f.getName());
                }
            }
        }
        return retval;
    }

    static List getSuperClasses(XMLClass xmlClass) {
        List retval = new ArrayList();
        try {
            MMCI.getDefaultMMCI();
        } catch (Exception e) {
            System.err.println("can not get MMCI");
        }

        if (xmlClass.getImplements() != null && xmlClass.getImplements().trim().length() > 0) {
            StringTokenizer st = new StringTokenizer(xmlClass.getImplements(), ",");
            while (st.hasMoreTokens()) {
                String newClass = st.nextToken();
                //System.err.println(newClass);
                if (newClass.indexOf("mmbase") != -1) {
                    try {
                        XMLClass f = MMCI.getDefaultMMCI().getClass(newClass);
                        retval.add(f);
                        retval.addAll(getSuperClasses(f));
                    } catch (NotInMMCIException e) {
                        System.err.println(e.getMessage());
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    }
                }
            }
        }
        return retval;
    }

    /*
     * main method
     * parameters required are targetdirectory and MMCI.xml file location
     */
    public static void main(String[] argv) throws Exception {
        if (argv.length != 2) {
            System.err.println("Usage: java org.mmbase.bridge.remote.generator.RemoteGenerator <targetdir> <mmci-xml-file>");
            System.exit(1);
        }
        new RemoteGenerator(argv[0], argv[1]);
    }
}

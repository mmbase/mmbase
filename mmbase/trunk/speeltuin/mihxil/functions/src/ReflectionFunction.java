/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.functions;


import org.mmbase.util.logging.*;

/**
 * MMBase Function
 *
 * @javadoc
 * @author Daniel Ockeloen
 * @author Michiel Meeuwissen
 * @version $Id: MMFunction.java
 */
public class ReflectionFunction extends Function {

    private static final Logger log = Logging.getLoggerInstance(ReflectionFunction.class);

    private Class  implementor;
    private String methodName; // Method?



    public ReflectionFunction(String name, Parameter[] def, ReturnType returnType, Class implementor, String methodName) {
        super(name, def, returnType);
        this.implementor = implementor;
        this.methodName  = methodName;
    }

    public Class getImplementor() {
        return implementor;
    }

    public String getMethodName() {
        return methodName;
    }

    public Object getFunctionValue(Parameters arguments) {
        return null;
        /*
        if (functionMethod==null) {
            if (!initFunction()) {
                return null;
            }
        }
		

        Object arglist[] = new Object[arguments.size()];
        // test call
        try {
            
            Enumeration e=params.elements();
            int i=0;
            while (e.hasMoreElements()) {
                MMFunctionParam p=(MMFunctionParam)e.nextElement();
                String key=p.getName();
                String type=p.getType();
                String value=(String)atr.get(key);
                if (value==null) {
                    value=(String)p.getDefaultValue();
                }
                if (type.equals("String")) {
                    arglist[i]=value;
                }
                if (type.equals("int")) {
                    try {
                        arglist[i]=new Integer(Integer.parseInt(value));
                    } catch(Exception f) {}
                }
                i++;
            }

            Object retobj=functionMethod.invoke(functionInstance,arglist);
            return(retobj);

			
        } catch(Exception e) {
            log.error("function call : "+name);
            log.error("functionMethod="+functionMethod);
            log.error("functionInstance="+functionInstance);
            log.error("arglist="+arglist.toString());
            e.printStackTrace();
        }

        return(null);
    }



    private  boolean initFunction() {
        if (classname!=null) {
            try {
                functionClass = Class.forName(classname);
            } catch(Exception e) {
                log.error("can't create an application function class : "+classname);
                e.printStackTrace();
            }

            try {
                functionInstance = functionClass.newInstance();
            } catch(Exception e) {
                log.error("can't create an function instance : "+classname);
                e.printStackTrace();
            }
	
            Class paramtypes[]= new Class[params.size()];
            Enumeration e=params.elements();
            int i=0;
            while (e.hasMoreElements()) {
                MMFunctionParam p=(MMFunctionParam)e.nextElement();
                String type=p.getType();
                if (type.equals("String")) {
                    paramtypes[i]=String.class;
                }
                if (type.equals("int")) {
                    paramtypes[i]=int.class;
                }
                i++;
            }
            try {
                functionMethod=functionClass.getMethod(methodname,paramtypes);
            } catch(NoSuchMethodException f) {
                String paramstring="";
                e=params.elements();
                while (e.hasMoreElements()) {
                    if (!paramstring.equals("")) paramstring+=",";
                    MMFunctionParam p=(MMFunctionParam)e.nextElement();
                    paramstring+=p.getType();
                    paramstring+=" "+p.getName();
					
                }
                log.error("MMFunction method  not found : "+classname+"."+methodname+"("+paramstring+")");
                //f.printStackTrace();
            }
        }
        return(true);
        */
	}

}

package com.finalist.portlets.banner;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class StripQueryStringFilter implements Filter {
    
    static Set parameters = new HashSet();
    
    public void destroy() {
        // empty
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        EmptyQueryStringRequestWrapper wrapper = new EmptyQueryStringRequestWrapper((HttpServletRequest) request, parameters);
        chain.doFilter(wrapper, response);
    }

    public void init(FilterConfig config) throws ServletException {
        // TODO make the to be stripped out parameters configurable
        // empty
        parameters.add("clickTAG");
    }

}

class EmptyQueryStringRequestWrapper extends HttpServletRequestWrapper {
    
    private Set parameters;

    public EmptyQueryStringRequestWrapper(HttpServletRequest request, Set parameters) {
        super(request);
        this.parameters = parameters;
    }

    @Override
    public String getParameter(String name) {
        return null;
    }

    @Override
    public Map getParameterMap() {
        //return Collections.EMPTY_MAP;
        return null;
    }

    @Override
    public Enumeration getParameterNames() {
        //return EMPTY_ENUMERATION;
        return null;
    }

    @Override
    public String[] getParameterValues(String name) {
        //return EMPTY_STRING_ARRAY;
        return null;
    }

    @Override
    public String getQueryString() {
        return null;
    }

    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final Enumeration EMPTY_ENUMERATION = new EmptyEnumeration();
    static final class EmptyEnumeration implements Enumeration {

        public boolean hasMoreElements() {
            return false;
        }

        public Object nextElement() {
            return null;
        }
        
    }
    
}

package org.mmbase.bridge.mock.jsp;

import java.util.Enumeration;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class MockServletConfig implements ServletConfig {
    public MockServletConfig(ServletContext servletContext) {
    }

    @Override
    public String getServletName() {
        return "";
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public String getInitParameter(String name) {
        return "";
    }

    @Override
    public Enumeration getInitParameterNames() {
        return null;
    }
}

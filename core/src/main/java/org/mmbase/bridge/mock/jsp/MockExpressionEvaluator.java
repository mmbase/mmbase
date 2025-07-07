package org.mmbase.bridge.mock.jsp;

import javax.servlet.jsp.el.*;

public class MockExpressionEvaluator extends ExpressionEvaluator {
    public MockExpressionEvaluator(MockPageContext mockPageContext) {
    }

    @Override
    public Expression parseExpression(String s, Class aClass, FunctionMapper functionMapper) throws ELException {
        return null;
    }

    @Override
    public Object evaluate(String s, Class aClass, VariableResolver variableResolver, FunctionMapper functionMapper) throws ELException {
        return null;
    }
}

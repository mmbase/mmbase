//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.5-b16-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2006.07.17 at 08:40:55 PM MSD 
//


package nl.leocms.connectors.UISconnector.input.customers.model.impl;

public class PropertyTypeImpl implements nl.leocms.connectors.UISconnector.input.customers.model.PropertyType, com.sun.xml.bind.JAXBObject, nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.UnmarshallableObject, nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.XMLSerializable, nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.ValidatableObject
{

    protected java.lang.String _PropertyDescription;
    protected com.sun.xml.bind.util.ListImpl _PropertyValue;
    protected java.lang.String _PropertyId;
    public final static java.lang.Class version = (nl.leocms.connectors.UISconnector.input.customers.model.impl.JAXBVersion.class);
    private static com.sun.msv.grammar.Grammar schemaFragment;

    private final static java.lang.Class PRIMARY_INTERFACE_CLASS() {
        return (nl.leocms.connectors.UISconnector.input.customers.model.PropertyType.class);
    }

    public java.lang.String getPropertyDescription() {
        return _PropertyDescription;
    }

    public void setPropertyDescription(java.lang.String value) {
        _PropertyDescription = value;
    }

    protected com.sun.xml.bind.util.ListImpl _getPropertyValue() {
        if (_PropertyValue == null) {
            _PropertyValue = new com.sun.xml.bind.util.ListImpl(new java.util.ArrayList());
        }
        return _PropertyValue;
    }

    public java.util.List getPropertyValue() {
        return _getPropertyValue();
    }

    public java.lang.String getPropertyId() {
        return _PropertyId;
    }

    public void setPropertyId(java.lang.String value) {
        _PropertyId = value;
    }

    public nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.UnmarshallingEventHandler createUnmarshaller(nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.UnmarshallingContext context) {
        return new nl.leocms.connectors.UISconnector.input.customers.model.impl.PropertyTypeImpl.Unmarshaller(context);
    }

    public void serializeBody(nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        int idx2 = 0;
        final int len2 = ((_PropertyValue == null)? 0 :_PropertyValue.size());
        context.startElement("", "propertyId");
        context.endNamespaceDecls();
        context.endAttributes();
        try {
            context.text(((java.lang.String) _PropertyId), "PropertyId");
        } catch (java.lang.Exception e) {
            nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.Util.handlePrintConversionException(this, e, context);
        }
        context.endElement();
        context.startElement("", "propertyDescription");
        context.endNamespaceDecls();
        context.endAttributes();
        try {
            context.text(((java.lang.String) _PropertyDescription), "PropertyDescription");
        } catch (java.lang.Exception e) {
            nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.Util.handlePrintConversionException(this, e, context);
        }
        context.endElement();
        while (idx2 != len2) {
            if (_PropertyValue.get(idx2) instanceof javax.xml.bind.Element) {
                context.childAsBody(((com.sun.xml.bind.JAXBObject) _PropertyValue.get(idx2 ++)), "PropertyValue");
            } else {
                context.startElement("", "propertyValue");
                int idx_4 = idx2;
                context.childAsURIs(((com.sun.xml.bind.JAXBObject) _PropertyValue.get(idx_4 ++)), "PropertyValue");
                context.endNamespaceDecls();
                int idx_5 = idx2;
                context.childAsAttributes(((com.sun.xml.bind.JAXBObject) _PropertyValue.get(idx_5 ++)), "PropertyValue");
                context.endAttributes();
                context.childAsBody(((com.sun.xml.bind.JAXBObject) _PropertyValue.get(idx2 ++)), "PropertyValue");
                context.endElement();
            }
        }
    }

    public void serializeAttributes(nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        int idx2 = 0;
        final int len2 = ((_PropertyValue == null)? 0 :_PropertyValue.size());
        while (idx2 != len2) {
            if (_PropertyValue.get(idx2) instanceof javax.xml.bind.Element) {
                context.childAsAttributes(((com.sun.xml.bind.JAXBObject) _PropertyValue.get(idx2 ++)), "PropertyValue");
            } else {
                idx2 += 1;
            }
        }
    }

    public void serializeURIs(nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        int idx2 = 0;
        final int len2 = ((_PropertyValue == null)? 0 :_PropertyValue.size());
        while (idx2 != len2) {
            if (_PropertyValue.get(idx2) instanceof javax.xml.bind.Element) {
                context.childAsURIs(((com.sun.xml.bind.JAXBObject) _PropertyValue.get(idx2 ++)), "PropertyValue");
            } else {
                idx2 += 1;
            }
        }
    }

    public java.lang.Class getPrimaryInterface() {
        return (nl.leocms.connectors.UISconnector.input.customers.model.PropertyType.class);
    }

    public com.sun.msv.verifier.DocumentDeclaration createRawValidator() {
        if (schemaFragment == null) {
            schemaFragment = com.sun.xml.bind.validator.SchemaDeserializer.deserialize((
 "\u00ac\u00ed\u0000\u0005sr\u0000\u001fcom.sun.msv.grammar.SequenceExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000\u001dcom.su"
+"n.msv.grammar.BinaryExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002L\u0000\u0004exp1t\u0000 Lcom/sun/msv/gra"
+"mmar/Expression;L\u0000\u0004exp2q\u0000~\u0000\u0002xr\u0000\u001ecom.sun.msv.grammar.Expressi"
+"on\u00f8\u0018\u0082\u00e8N5~O\u0002\u0000\u0002L\u0000\u0013epsilonReducibilityt\u0000\u0013Ljava/lang/Boolean;L\u0000\u000b"
+"expandedExpq\u0000~\u0000\u0002xpppsq\u0000~\u0000\u0000ppsr\u0000\'com.sun.msv.grammar.trex.Ele"
+"mentPattern\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001L\u0000\tnameClasst\u0000\u001fLcom/sun/msv/grammar/Na"
+"meClass;xr\u0000\u001ecom.sun.msv.grammar.ElementExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002Z\u0000\u001aigno"
+"reUndeclaredAttributesL\u0000\fcontentModelq\u0000~\u0000\u0002xq\u0000~\u0000\u0003pp\u0000sq\u0000~\u0000\u0000pps"
+"r\u0000\u001bcom.sun.msv.grammar.DataExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0003L\u0000\u0002dtt\u0000\u001fLorg/relaxn"
+"g/datatype/Datatype;L\u0000\u0006exceptq\u0000~\u0000\u0002L\u0000\u0004namet\u0000\u001dLcom/sun/msv/uti"
+"l/StringPair;xq\u0000~\u0000\u0003ppsr\u0000#com.sun.msv.datatype.xsd.StringType"
+"\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001Z\u0000\risAlwaysValidxr\u0000*com.sun.msv.datatype.xsd.Buil"
+"tinAtomicType\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000%com.sun.msv.datatype.xsd.Concret"
+"eType\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000\'com.sun.msv.datatype.xsd.XSDatatypeImpl\u0000"
+"\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0003L\u0000\fnamespaceUrit\u0000\u0012Ljava/lang/String;L\u0000\btypeNameq\u0000~"
+"\u0000\u0014L\u0000\nwhiteSpacet\u0000.Lcom/sun/msv/datatype/xsd/WhiteSpaceProces"
+"sor;xpt\u0000 http://www.w3.org/2001/XMLSchemat\u0000\u0006stringsr\u00005com.su"
+"n.msv.datatype.xsd.WhiteSpaceProcessor$Preserve\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr"
+"\u0000,com.sun.msv.datatype.xsd.WhiteSpaceProcessor\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xp\u0001"
+"sr\u00000com.sun.msv.grammar.Expression$NullSetExpression\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001"
+"\u0002\u0000\u0000xq\u0000~\u0000\u0003ppsr\u0000\u001bcom.sun.msv.util.StringPair\u00d0t\u001ejB\u008f\u008d\u00a0\u0002\u0000\u0002L\u0000\tloca"
+"lNameq\u0000~\u0000\u0014L\u0000\fnamespaceURIq\u0000~\u0000\u0014xpq\u0000~\u0000\u0018q\u0000~\u0000\u0017sr\u0000\u001dcom.sun.msv.gr"
+"ammar.ChoiceExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0001ppsr\u0000 com.sun.msv.grammar.At"
+"tributeExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002L\u0000\u0003expq\u0000~\u0000\u0002L\u0000\tnameClassq\u0000~\u0000\bxq\u0000~\u0000\u0003sr\u0000\u0011j"
+"ava.lang.Boolean\u00cd r\u0080\u00d5\u009c\u00fa\u00ee\u0002\u0000\u0001Z\u0000\u0005valuexp\u0000psq\u0000~\u0000\fppsr\u0000\"com.sun.m"
+"sv.datatype.xsd.QnameType\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0011q\u0000~\u0000\u0017t\u0000\u0005QNamesr\u00005c"
+"om.sun.msv.datatype.xsd.WhiteSpaceProcessor$Collapse\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001"
+"\u0002\u0000\u0000xq\u0000~\u0000\u001aq\u0000~\u0000\u001dsq\u0000~\u0000\u001eq\u0000~\u0000)q\u0000~\u0000\u0017sr\u0000#com.sun.msv.grammar.Simple"
+"NameClass\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002L\u0000\tlocalNameq\u0000~\u0000\u0014L\u0000\fnamespaceURIq\u0000~\u0000\u0014xr\u0000"
+"\u001dcom.sun.msv.grammar.NameClass\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xpt\u0000\u0004typet\u0000)http://"
+"www.w3.org/2001/XMLSchema-instancesr\u00000com.sun.msv.grammar.Ex"
+"pression$EpsilonExpression\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0003sq\u0000~\u0000$\u0001psq\u0000~\u0000-t\u0000\n"
+"propertyIdt\u0000\u0000sq\u0000~\u0000\u0007pp\u0000sq\u0000~\u0000\u0000ppq\u0000~\u0000\u000fsq\u0000~\u0000 ppsq\u0000~\u0000\"q\u0000~\u0000%pq\u0000~\u0000&"
+"q\u0000~\u0000/q\u0000~\u00003sq\u0000~\u0000-t\u0000\u0013propertyDescriptionq\u0000~\u00007sr\u0000 com.sun.msv.g"
+"rammar.OneOrMoreExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000\u001ccom.sun.msv.grammar.UnaryE"
+"xp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001L\u0000\u0003expq\u0000~\u0000\u0002xq\u0000~\u0000\u0003ppsq\u0000~\u0000 ppsq\u0000~\u0000\u0007pp\u0000sq\u0000~\u0000 ppsq\u0000"
+"~\u0000>q\u0000~\u0000%psq\u0000~\u0000\"q\u0000~\u0000%psr\u00002com.sun.msv.grammar.Expression$AnyS"
+"tringExpression\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0003q\u0000~\u00004psr\u0000 com.sun.msv.gramma"
+"r.AnyNameClass\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000.q\u0000~\u00003sq\u0000~\u0000-t\u0000Enl.leocms.conne"
+"ctors.UISconnector.input.customers.model.PropertyValuet\u0000+htt"
+"p://java.sun.com/jaxb/xjc/dummy-elementssq\u0000~\u0000\u0007pp\u0000sq\u0000~\u0000\u0000ppsq\u0000"
+"~\u0000\u0007pp\u0000sq\u0000~\u0000 ppsq\u0000~\u0000>q\u0000~\u0000%psq\u0000~\u0000\"q\u0000~\u0000%pq\u0000~\u0000Gq\u0000~\u0000Iq\u0000~\u00003sq\u0000~\u0000-t"
+"\u0000Inl.leocms.connectors.UISconnector.input.customers.model.Pr"
+"opertyValueTypeq\u0000~\u0000Lsq\u0000~\u0000 ppsq\u0000~\u0000\"q\u0000~\u0000%pq\u0000~\u0000&q\u0000~\u0000/q\u0000~\u00003sq\u0000~\u0000"
+"-t\u0000\rpropertyValueq\u0000~\u00007sr\u0000\"com.sun.msv.grammar.ExpressionPool"
+"\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001L\u0000\bexpTablet\u0000/Lcom/sun/msv/grammar/ExpressionPool"
+"$ClosedHash;xpsr\u0000-com.sun.msv.grammar.ExpressionPool$ClosedH"
+"ash\u00d7j\u00d0N\u00ef\u00e8\u00ed\u001c\u0003\u0000\u0003I\u0000\u0005countB\u0000\rstreamVersionL\u0000\u0006parentt\u0000$Lcom/sun/m"
+"sv/grammar/ExpressionPool;xp\u0000\u0000\u0000\u000e\u0001pq\u0000~\u0000\u0005q\u0000~\u0000Dq\u0000~\u0000Qq\u0000~\u0000Cq\u0000~\u0000Pq"
+"\u0000~\u0000Nq\u0000~\u0000\u0006q\u0000~\u0000@q\u0000~\u0000!q\u0000~\u0000:q\u0000~\u0000Uq\u0000~\u0000Aq\u0000~\u0000\u000bq\u0000~\u00009x"));
        }
        return new com.sun.msv.verifier.regexp.REDocumentDeclaration(schemaFragment);
    }

    public class Unmarshaller
        extends nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.AbstractUnmarshallingEventHandlerImpl
    {


        public Unmarshaller(nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.UnmarshallingContext context) {
            super(context, "----------");
        }

        protected Unmarshaller(nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.UnmarshallingContext context, int startState) {
            this(context);
            state = startState;
        }

        public java.lang.Object owner() {
            return nl.leocms.connectors.UISconnector.input.customers.model.impl.PropertyTypeImpl.this;
        }

        public void enterElement(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname, org.xml.sax.Attributes __atts)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case  0 :
                        if (("propertyId" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, true);
                            state = 1;
                            return ;
                        }
                        break;
                    case  8 :
                        if (("propertyValueDescription" == ___local)&&("" == ___uri)) {
                            _getPropertyValue().add(((nl.leocms.connectors.UISconnector.input.customers.model.impl.PropertyValueTypeImpl) spawnChildFromEnterElement((nl.leocms.connectors.UISconnector.input.customers.model.impl.PropertyValueTypeImpl.class), 9, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("propertyValueId" == ___local)&&("" == ___uri)) {
                            _getPropertyValue().add(((nl.leocms.connectors.UISconnector.input.customers.model.impl.PropertyValueTypeImpl) spawnChildFromEnterElement((nl.leocms.connectors.UISconnector.input.customers.model.impl.PropertyValueTypeImpl.class), 9, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        _getPropertyValue().add(((nl.leocms.connectors.UISconnector.input.customers.model.impl.PropertyValueTypeImpl) spawnChildFromEnterElement((nl.leocms.connectors.UISconnector.input.customers.model.impl.PropertyValueTypeImpl.class), 9, ___uri, ___local, ___qname, __atts)));
                        return ;
                    case  6 :
                        if (("propertyValue" == ___local)&&("" == ___uri)) {
                            _getPropertyValue().add(((nl.leocms.connectors.UISconnector.input.customers.model.impl.PropertyValueImpl) spawnChildFromEnterElement((nl.leocms.connectors.UISconnector.input.customers.model.impl.PropertyValueImpl.class), 7, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("propertyValue" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, false);
                            state = 8;
                            return ;
                        }
                        break;
                    case  7 :
                        if (("propertyValue" == ___local)&&("" == ___uri)) {
                            _getPropertyValue().add(((nl.leocms.connectors.UISconnector.input.customers.model.impl.PropertyValueImpl) spawnChildFromEnterElement((nl.leocms.connectors.UISconnector.input.customers.model.impl.PropertyValueImpl.class), 7, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("propertyValue" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, false);
                            state = 8;
                            return ;
                        }
                        revertToParentFromEnterElement(___uri, ___local, ___qname, __atts);
                        return ;
                    case  3 :
                        if (("propertyDescription" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, true);
                            state = 4;
                            return ;
                        }
                        break;
                }
                super.enterElement(___uri, ___local, ___qname, __atts);
                break;
            }
        }

        public void leaveElement(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case  8 :
                        _getPropertyValue().add(((nl.leocms.connectors.UISconnector.input.customers.model.impl.PropertyValueTypeImpl) spawnChildFromLeaveElement((nl.leocms.connectors.UISconnector.input.customers.model.impl.PropertyValueTypeImpl.class), 9, ___uri, ___local, ___qname)));
                        return ;
                    case  5 :
                        if (("propertyDescription" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 6;
                            return ;
                        }
                        break;
                    case  9 :
                        if (("propertyValue" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 7;
                            return ;
                        }
                        break;
                    case  2 :
                        if (("propertyId" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 3;
                            return ;
                        }
                        break;
                    case  7 :
                        revertToParentFromLeaveElement(___uri, ___local, ___qname);
                        return ;
                }
                super.leaveElement(___uri, ___local, ___qname);
                break;
            }
        }

        public void enterAttribute(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case  8 :
                        _getPropertyValue().add(((nl.leocms.connectors.UISconnector.input.customers.model.impl.PropertyValueTypeImpl) spawnChildFromEnterAttribute((nl.leocms.connectors.UISconnector.input.customers.model.impl.PropertyValueTypeImpl.class), 9, ___uri, ___local, ___qname)));
                        return ;
                    case  7 :
                        revertToParentFromEnterAttribute(___uri, ___local, ___qname);
                        return ;
                }
                super.enterAttribute(___uri, ___local, ___qname);
                break;
            }
        }

        public void leaveAttribute(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case  8 :
                        _getPropertyValue().add(((nl.leocms.connectors.UISconnector.input.customers.model.impl.PropertyValueTypeImpl) spawnChildFromLeaveAttribute((nl.leocms.connectors.UISconnector.input.customers.model.impl.PropertyValueTypeImpl.class), 9, ___uri, ___local, ___qname)));
                        return ;
                    case  7 :
                        revertToParentFromLeaveAttribute(___uri, ___local, ___qname);
                        return ;
                }
                super.leaveAttribute(___uri, ___local, ___qname);
                break;
            }
        }

        public void handleText(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                try {
                    switch (state) {
                        case  8 :
                            _getPropertyValue().add(((nl.leocms.connectors.UISconnector.input.customers.model.impl.PropertyValueTypeImpl) spawnChildFromText((nl.leocms.connectors.UISconnector.input.customers.model.impl.PropertyValueTypeImpl.class), 9, value)));
                            return ;
                        case  4 :
                            state = 5;
                            eatText1(value);
                            return ;
                        case  1 :
                            state = 2;
                            eatText2(value);
                            return ;
                        case  7 :
                            revertToParentFromText(value);
                            return ;
                    }
                } catch (java.lang.RuntimeException e) {
                    handleUnexpectedTextException(value, e);
                }
                break;
            }
        }

        private void eatText1(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _PropertyDescription = value;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

        private void eatText2(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _PropertyId = value;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

    }

}

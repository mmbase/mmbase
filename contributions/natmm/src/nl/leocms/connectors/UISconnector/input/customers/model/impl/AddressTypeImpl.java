//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.5-b16-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2006.07.17 at 08:40:55 PM MSD 
//


package nl.leocms.connectors.UISconnector.input.customers.model.impl;

public class AddressTypeImpl implements nl.leocms.connectors.UISconnector.input.customers.model.AddressType, com.sun.xml.bind.JAXBObject, nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.UnmarshallableObject, nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.XMLSerializable, nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.ValidatableObject
{

    protected java.lang.String _ExtraInfo;
    protected java.lang.String _HouseNumberExtension;
    protected java.lang.String _ZipCode;
    protected java.lang.String _CountryID;
    protected java.lang.String _HouseNumber;
    protected java.lang.String _AddressType;
    protected java.lang.String _City;
    protected java.lang.String _StreetName;
    public final static java.lang.Class version = (nl.leocms.connectors.UISconnector.input.customers.model.impl.JAXBVersion.class);
    private static com.sun.msv.grammar.Grammar schemaFragment;

    private final static java.lang.Class PRIMARY_INTERFACE_CLASS() {
        return (nl.leocms.connectors.UISconnector.input.customers.model.AddressType.class);
    }

    public java.lang.String getExtraInfo() {
        return _ExtraInfo;
    }

    public void setExtraInfo(java.lang.String value) {
        _ExtraInfo = value;
    }

    public java.lang.String getHouseNumberExtension() {
        return _HouseNumberExtension;
    }

    public void setHouseNumberExtension(java.lang.String value) {
        _HouseNumberExtension = value;
    }

    public java.lang.String getZipCode() {
        return _ZipCode;
    }

    public void setZipCode(java.lang.String value) {
        _ZipCode = value;
    }

    public java.lang.String getCountryID() {
        return _CountryID;
    }

    public void setCountryID(java.lang.String value) {
        _CountryID = value;
    }

    public java.lang.String getHouseNumber() {
        return _HouseNumber;
    }

    public void setHouseNumber(java.lang.String value) {
        _HouseNumber = value;
    }

    public java.lang.String getAddressType() {
        return _AddressType;
    }

    public void setAddressType(java.lang.String value) {
        _AddressType = value;
    }

    public java.lang.String getCity() {
        return _City;
    }

    public void setCity(java.lang.String value) {
        _City = value;
    }

    public java.lang.String getStreetName() {
        return _StreetName;
    }

    public void setStreetName(java.lang.String value) {
        _StreetName = value;
    }

    public nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.UnmarshallingEventHandler createUnmarshaller(nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.UnmarshallingContext context) {
        return new nl.leocms.connectors.UISconnector.input.customers.model.impl.AddressTypeImpl.Unmarshaller(context);
    }

    public void serializeBody(nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        context.startElement("", "countryID");
        context.endNamespaceDecls();
        context.endAttributes();
        try {
            context.text(((java.lang.String) _CountryID), "CountryID");
        } catch (java.lang.Exception e) {
            nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.Util.handlePrintConversionException(this, e, context);
        }
        context.endElement();
        context.startElement("", "addressType");
        context.endNamespaceDecls();
        context.endAttributes();
        try {
            context.text(((java.lang.String) _AddressType), "AddressType");
        } catch (java.lang.Exception e) {
            nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.Util.handlePrintConversionException(this, e, context);
        }
        context.endElement();
        context.startElement("", "houseNumber");
        context.endNamespaceDecls();
        context.endAttributes();
        try {
            context.text(((java.lang.String) _HouseNumber), "HouseNumber");
        } catch (java.lang.Exception e) {
            nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.Util.handlePrintConversionException(this, e, context);
        }
        context.endElement();
        context.startElement("", "houseNumberExtension");
        context.endNamespaceDecls();
        context.endAttributes();
        try {
            context.text(((java.lang.String) _HouseNumberExtension), "HouseNumberExtension");
        } catch (java.lang.Exception e) {
            nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.Util.handlePrintConversionException(this, e, context);
        }
        context.endElement();
        context.startElement("", "streetName");
        context.endNamespaceDecls();
        context.endAttributes();
        try {
            context.text(((java.lang.String) _StreetName), "StreetName");
        } catch (java.lang.Exception e) {
            nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.Util.handlePrintConversionException(this, e, context);
        }
        context.endElement();
        context.startElement("", "extraInfo");
        context.endNamespaceDecls();
        context.endAttributes();
        try {
            context.text(((java.lang.String) _ExtraInfo), "ExtraInfo");
        } catch (java.lang.Exception e) {
            nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.Util.handlePrintConversionException(this, e, context);
        }
        context.endElement();
        context.startElement("", "zipCode");
        context.endNamespaceDecls();
        context.endAttributes();
        try {
            context.text(((java.lang.String) _ZipCode), "ZipCode");
        } catch (java.lang.Exception e) {
            nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.Util.handlePrintConversionException(this, e, context);
        }
        context.endElement();
        context.startElement("", "city");
        context.endNamespaceDecls();
        context.endAttributes();
        try {
            context.text(((java.lang.String) _City), "City");
        } catch (java.lang.Exception e) {
            nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.Util.handlePrintConversionException(this, e, context);
        }
        context.endElement();
    }

    public void serializeAttributes(nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
    }

    public void serializeURIs(nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
    }

    public java.lang.Class getPrimaryInterface() {
        return (nl.leocms.connectors.UISconnector.input.customers.model.AddressType.class);
    }

    public com.sun.msv.verifier.DocumentDeclaration createRawValidator() {
        if (schemaFragment == null) {
            schemaFragment = com.sun.xml.bind.validator.SchemaDeserializer.deserialize((
 "\u00ac\u00ed\u0000\u0005sr\u0000!com.sun.msv.grammar.InterleaveExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000\u001dcom."
+"sun.msv.grammar.BinaryExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002L\u0000\u0004exp1t\u0000 Lcom/sun/msv/g"
+"rammar/Expression;L\u0000\u0004exp2q\u0000~\u0000\u0002xr\u0000\u001ecom.sun.msv.grammar.Expres"
+"sion\u00f8\u0018\u0082\u00e8N5~O\u0002\u0000\u0002L\u0000\u0013epsilonReducibilityt\u0000\u0013Ljava/lang/Boolean;L"
+"\u0000\u000bexpandedExpq\u0000~\u0000\u0002xpppsq\u0000~\u0000\u0000ppsq\u0000~\u0000\u0000ppsq\u0000~\u0000\u0000ppsq\u0000~\u0000\u0000ppsq\u0000~\u0000\u0000"
+"ppsq\u0000~\u0000\u0000ppsr\u0000\'com.sun.msv.grammar.trex.ElementPattern\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
+"\u0001\u0002\u0000\u0001L\u0000\tnameClasst\u0000\u001fLcom/sun/msv/grammar/NameClass;xr\u0000\u001ecom.su"
+"n.msv.grammar.ElementExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002Z\u0000\u001aignoreUndeclaredAttrib"
+"utesL\u0000\fcontentModelq\u0000~\u0000\u0002xq\u0000~\u0000\u0003pp\u0000sr\u0000\u001fcom.sun.msv.grammar.Seq"
+"uenceExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0001ppsr\u0000\u001bcom.sun.msv.grammar.DataExp\u0000\u0000"
+"\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0003L\u0000\u0002dtt\u0000\u001fLorg/relaxng/datatype/Datatype;L\u0000\u0006exceptq\u0000~"
+"\u0000\u0002L\u0000\u0004namet\u0000\u001dLcom/sun/msv/util/StringPair;xq\u0000~\u0000\u0003ppsr\u0000#com.sun"
+".msv.datatype.xsd.StringType\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001Z\u0000\risAlwaysValidxr\u0000*c"
+"om.sun.msv.datatype.xsd.BuiltinAtomicType\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000%com."
+"sun.msv.datatype.xsd.ConcreteType\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000\'com.sun.msv."
+"datatype.xsd.XSDatatypeImpl\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0003L\u0000\fnamespaceUrit\u0000\u0012Ljav"
+"a/lang/String;L\u0000\btypeNameq\u0000~\u0000\u001aL\u0000\nwhiteSpacet\u0000.Lcom/sun/msv/d"
+"atatype/xsd/WhiteSpaceProcessor;xpt\u0000 http://www.w3.org/2001/"
+"XMLSchemat\u0000\u0006stringsr\u00005com.sun.msv.datatype.xsd.WhiteSpacePro"
+"cessor$Preserve\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xr\u0000,com.sun.msv.datatype.xsd.White"
+"SpaceProcessor\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xp\u0001sr\u00000com.sun.msv.grammar.Expressi"
+"on$NullSetExpression\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0003ppsr\u0000\u001bcom.sun.msv.util."
+"StringPair\u00d0t\u001ejB\u008f\u008d\u00a0\u0002\u0000\u0002L\u0000\tlocalNameq\u0000~\u0000\u001aL\u0000\fnamespaceURIq\u0000~\u0000\u001axp"
+"q\u0000~\u0000\u001eq\u0000~\u0000\u001dsr\u0000\u001dcom.sun.msv.grammar.ChoiceExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0001"
+"ppsr\u0000 com.sun.msv.grammar.AttributeExp\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002L\u0000\u0003expq\u0000~\u0000\u0002"
+"L\u0000\tnameClassq\u0000~\u0000\rxq\u0000~\u0000\u0003sr\u0000\u0011java.lang.Boolean\u00cd r\u0080\u00d5\u009c\u00fa\u00ee\u0002\u0000\u0001Z\u0000\u0005va"
+"luexp\u0000psq\u0000~\u0000\u0012ppsr\u0000\"com.sun.msv.datatype.xsd.QnameType\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
+"\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0017q\u0000~\u0000\u001dt\u0000\u0005QNamesr\u00005com.sun.msv.datatype.xsd.WhiteSpa"
+"ceProcessor$Collapse\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000 q\u0000~\u0000#sq\u0000~\u0000$q\u0000~\u0000/q\u0000~\u0000\u001dsr"
+"\u0000#com.sun.msv.grammar.SimpleNameClass\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0002L\u0000\tlocalName"
+"q\u0000~\u0000\u001aL\u0000\fnamespaceURIq\u0000~\u0000\u001axr\u0000\u001dcom.sun.msv.grammar.NameClass\u0000\u0000"
+"\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0000xpt\u0000\u0004typet\u0000)http://www.w3.org/2001/XMLSchema-instan"
+"cesr\u00000com.sun.msv.grammar.Expression$EpsilonExpression\u0000\u0000\u0000\u0000\u0000\u0000"
+"\u0000\u0001\u0002\u0000\u0000xq\u0000~\u0000\u0003sq\u0000~\u0000*\u0001psq\u0000~\u00003t\u0000\tcountryIDt\u0000\u0000sq\u0000~\u0000\fpp\u0000sq\u0000~\u0000\u0010ppq\u0000~"
+"\u0000\u0015sq\u0000~\u0000&ppsq\u0000~\u0000(q\u0000~\u0000+pq\u0000~\u0000,q\u0000~\u00005q\u0000~\u00009sq\u0000~\u00003t\u0000\u000baddressTypeq\u0000~"
+"\u0000=sq\u0000~\u0000\fpp\u0000sq\u0000~\u0000\u0010ppq\u0000~\u0000\u0015sq\u0000~\u0000&ppsq\u0000~\u0000(q\u0000~\u0000+pq\u0000~\u0000,q\u0000~\u00005q\u0000~\u00009s"
+"q\u0000~\u00003t\u0000\u000bhouseNumberq\u0000~\u0000=sq\u0000~\u0000\fpp\u0000sq\u0000~\u0000\u0010ppq\u0000~\u0000\u0015sq\u0000~\u0000&ppsq\u0000~\u0000("
+"q\u0000~\u0000+pq\u0000~\u0000,q\u0000~\u00005q\u0000~\u00009sq\u0000~\u00003t\u0000\u0014houseNumberExtensionq\u0000~\u0000=sq\u0000~\u0000"
+"\fpp\u0000sq\u0000~\u0000\u0010ppq\u0000~\u0000\u0015sq\u0000~\u0000&ppsq\u0000~\u0000(q\u0000~\u0000+pq\u0000~\u0000,q\u0000~\u00005q\u0000~\u00009sq\u0000~\u00003t\u0000"
+"\nstreetNameq\u0000~\u0000=sq\u0000~\u0000\fpp\u0000sq\u0000~\u0000\u0010ppq\u0000~\u0000\u0015sq\u0000~\u0000&ppsq\u0000~\u0000(q\u0000~\u0000+pq\u0000"
+"~\u0000,q\u0000~\u00005q\u0000~\u00009sq\u0000~\u00003t\u0000\textraInfoq\u0000~\u0000=sq\u0000~\u0000\fpp\u0000sq\u0000~\u0000\u0010ppq\u0000~\u0000\u0015sq"
+"\u0000~\u0000&ppsq\u0000~\u0000(q\u0000~\u0000+pq\u0000~\u0000,q\u0000~\u00005q\u0000~\u00009sq\u0000~\u00003t\u0000\u0007zipCodeq\u0000~\u0000=sq\u0000~\u0000\f"
+"pp\u0000sq\u0000~\u0000\u0010ppq\u0000~\u0000\u0015sq\u0000~\u0000&ppsq\u0000~\u0000(q\u0000~\u0000+pq\u0000~\u0000,q\u0000~\u00005q\u0000~\u00009sq\u0000~\u00003t\u0000\u0004"
+"cityq\u0000~\u0000=sr\u0000\"com.sun.msv.grammar.ExpressionPool\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0002\u0000\u0001L\u0000"
+"\bexpTablet\u0000/Lcom/sun/msv/grammar/ExpressionPool$ClosedHash;x"
+"psr\u0000-com.sun.msv.grammar.ExpressionPool$ClosedHash\u00d7j\u00d0N\u00ef\u00e8\u00ed\u001c\u0003\u0000"
+"\u0003I\u0000\u0005countB\u0000\rstreamVersionL\u0000\u0006parentt\u0000$Lcom/sun/msv/grammar/Ex"
+"pressionPool;xp\u0000\u0000\u0000\u0017\u0001pq\u0000~\u0000\nq\u0000~\u0000\'q\u0000~\u0000@q\u0000~\u0000Fq\u0000~\u0000Lq\u0000~\u0000Rq\u0000~\u0000Xq\u0000~\u0000"
+"^q\u0000~\u0000dq\u0000~\u0000\u000bq\u0000~\u0000\tq\u0000~\u0000\bq\u0000~\u0000\u0005q\u0000~\u0000\u0007q\u0000~\u0000\u0011q\u0000~\u0000?q\u0000~\u0000Eq\u0000~\u0000Kq\u0000~\u0000Qq\u0000~\u0000"
+"Wq\u0000~\u0000]q\u0000~\u0000\u0006q\u0000~\u0000cx"));
        }
        return new com.sun.msv.verifier.regexp.REDocumentDeclaration(schemaFragment);
    }

    public class Unmarshaller
        extends nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.AbstractUnmarshallingEventHandlerImpl
    {


        public Unmarshaller(nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.UnmarshallingContext context) {
            super(context, "-----------------");
        }

        protected Unmarshaller(nl.leocms.connectors.UISconnector.input.customers.model.impl.runtime.UnmarshallingContext context, int startState) {
            this(context);
            state = startState;
        }

        public java.lang.Object owner() {
            return nl.leocms.connectors.UISconnector.input.customers.model.impl.AddressTypeImpl.this;
        }

        public void enterElement(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname, org.xml.sax.Attributes __atts)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case  0 :
                        if (("city" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, true);
                            state = 15;
                            return ;
                        }
                        if (("zipCode" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, true);
                            state = 1;
                            return ;
                        }
                        if (("extraInfo" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, true);
                            state = 11;
                            return ;
                        }
                        if (("streetName" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, true);
                            state = 3;
                            return ;
                        }
                        if (("houseNumberExtension" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, true);
                            state = 9;
                            return ;
                        }
                        if (("houseNumber" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, true);
                            state = 7;
                            return ;
                        }
                        if (("addressType" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, true);
                            state = 5;
                            return ;
                        }
                        if (("countryID" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, true);
                            state = 13;
                            return ;
                        }
                        revertToParentFromEnterElement(___uri, ___local, ___qname, __atts);
                        return ;
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
                    case  0 :
                        revertToParentFromLeaveElement(___uri, ___local, ___qname);
                        return ;
                    case  8 :
                        if (("houseNumber" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 0;
                            return ;
                        }
                        break;
                    case  4 :
                        if (("streetName" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 0;
                            return ;
                        }
                        break;
                    case  16 :
                        if (("city" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 0;
                            return ;
                        }
                        break;
                    case  2 :
                        if (("zipCode" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 0;
                            return ;
                        }
                        break;
                    case  6 :
                        if (("addressType" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 0;
                            return ;
                        }
                        break;
                    case  12 :
                        if (("extraInfo" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 0;
                            return ;
                        }
                        break;
                    case  10 :
                        if (("houseNumberExtension" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 0;
                            return ;
                        }
                        break;
                    case  14 :
                        if (("countryID" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 0;
                            return ;
                        }
                        break;
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
                    case  0 :
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
                    case  0 :
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
                        case  0 :
                            revertToParentFromText(value);
                            return ;
                        case  15 :
                            state = 16;
                            eatText1(value);
                            return ;
                        case  13 :
                            state = 14;
                            eatText2(value);
                            return ;
                        case  1 :
                            state = 2;
                            eatText3(value);
                            return ;
                        case  5 :
                            state = 6;
                            eatText4(value);
                            return ;
                        case  3 :
                            state = 4;
                            eatText5(value);
                            return ;
                        case  9 :
                            state = 10;
                            eatText6(value);
                            return ;
                        case  7 :
                            state = 8;
                            eatText7(value);
                            return ;
                        case  11 :
                            state = 12;
                            eatText8(value);
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
                _City = value;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

        private void eatText2(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _CountryID = value;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

        private void eatText3(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _ZipCode = value;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

        private void eatText4(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _AddressType = value;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

        private void eatText5(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _StreetName = value;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

        private void eatText6(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _HouseNumberExtension = value;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

        private void eatText7(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _HouseNumber = value;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

        private void eatText8(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _ExtraInfo = value;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

    }

}

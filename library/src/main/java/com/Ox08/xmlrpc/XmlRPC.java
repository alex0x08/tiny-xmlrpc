package com.Ox08.xmlrpc;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.*; import java.util.*; import java.util.logging.*;
import org.xml.sax.*; import org.xml.sax.helpers.DefaultHandler;import javax.xml.parsers.*;
/**
 * Core XML-RPC
 *
 * @author alex0x08
 * @since 1.0
 */
public abstract class XmlRPC {
    private final static Logger LOG = Logger.getLogger("0x08-xml-rpc");
    /**
     * See <a href="https://en.wikipedia.org/wiki/XML-RPC">...</a>
     */
    enum XmlRpcTypes { String,Integer,Boolean,Double,Date,Base64,Struct,Array,Nil }
    private static final SimpleDateFormat XMLRPC_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");
    private final SAXParserFactory sf = SAXParserFactory.newInstance();

    String parse(InputStream is) throws Exception {
        final SAXParser parser = sf.newSAXParser(); final XMLRPCSAXParser rp = new XMLRPCSAXParser();
        parser.parse(new InputSource(is), rp); return rp.methodName;
    }
    /**
     * Here we craft XML from provided object value
     * @param what
     *          some object
     * @param writer
     *          XML writer instance
     */
    void writeObject(Object what, XmlWriter writer) {
        writer.startEl("value"); // write <value>
        if (what == null)  writer.emptyEl("nil"); // write <nil/>
        else if (what instanceof String) writer.write(what.toString(), true);
        else if (what instanceof Integer) writer.writeEl("int", what.toString()); // write <int>what</int>
        else if (what instanceof Boolean b) writer.writeEl("boolean", (b ? "1" : "0"));
        else if (what instanceof Double || what instanceof Float) writer.writeEl("double", what.toString());
        else if (what instanceof Date d) writer.writeEl("dateTime.iso8601", XMLRPC_DATE_FORMAT.format(d));
        else if (what instanceof byte[] b) writer.writeEl("base64", Base64.getEncoder().encodeToString(b));
        else if (what instanceof List<?> v) {
            writer.startEl("array").startEl("data"); for (Object o : v) writeObject(o, writer);
            writer.endEl("data").endEl("array");
        } else if (what instanceof Map<?, ?> h) { writer.startEl("struct");
            for (Map.Entry<?, ?> e : h.entrySet()) {
                if (!(e.getKey() instanceof String nk)) continue; final Object nv = e.getValue();
                writer.startEl("member").startEl("name").write(nk, false).endEl("name");
                writeObject(nv, writer); writer.endEl("member"); } writer.endEl("struct");
        } else throw new RuntimeException("unknown type: %s".formatted(what.getClass()));
        writer.endEl("value"); // write </value>
    }
    public abstract void objectParsed(Object paramObject);
    /**
     * SAX handler for XML-RPC
     */
    class XMLRPCSAXParser extends DefaultHandler {
        //detected method name
        protected String methodName;
        // values stack
        private final Stack<Value> values = new Stack<>();
        private Value cvalue; // current processing value
        private final StringBuilder cdata = new StringBuilder(128);  // stores CDATA block
        protected boolean readCdata, fault;
        /**
         * Used for CDATA only!
         *
         * @param ch     The characters.
         * @param start  The start position in the character array.
         * @param length The number of characters to use from the
         *               character array.
         */
        @Override
        public void characters(char[] ch, int start, int length) {
            if (!this.readCdata) return; this.cdata.append(ch, start, length);
        }
        @Override
        public void endElement(String uri, String localName, String qName) {
            if (LOG.isLoggable(Level.FINE)) LOG.fine("endElement: %s".formatted(qName));
            if (this.cvalue != null && this.readCdata) {
                this.cvalue.characterData(this.cdata.toString()); this.cdata.setLength(0); this.readCdata = false;
            }
            switch (qName) { case "value" -> { int depth = this.values.size();
                    // this check means ordinary value, not struct or array
                    if (depth < 2 || this.values.get(depth - 2).type != XmlRpcTypes.Struct) {
                        final Value v = this.cvalue; this.values.pop();
                        if (depth < 2) { objectParsed(v.getValue()); this.cvalue = null; } else {
                            this.cvalue = this.values.peek(); this.cvalue.onEndElement(v); }  } }
                case "member" -> { final Value v = this.cvalue; this.values.pop();
                    this.cvalue = this.values.peek(); this.cvalue.onEndElement(v); }
                case "methodName" -> {
                    this.methodName = this.cdata.toString(); this.cdata.setLength(0); this.readCdata = false; }
            }
        }
        @Override
        public void startElement(String uri, String localName,
                                 String qName, Attributes attributes) {
            if (LOG.isLoggable(Level.FINE)) LOG.fine("startElement: %s".formatted(qName));
            switch (qName) { case "fault" -> this.fault = true;
                case "value" -> { final Value v = new Value(); this.values.push(v);
                    this.cvalue = v; this.cdata.setLength(0); this.readCdata = true; }
                case "methodName", "name", "string" -> { this.cdata.setLength(0); this.readCdata = true; }
                case "i4", "int" -> { this.cvalue.setType(XmlRpcTypes.Integer);
                    this.cdata.setLength(0); this.readCdata = true; }
                case "boolean" -> { this.cvalue.setType(XmlRpcTypes.Boolean);
                    this.cdata.setLength(0); this.readCdata = true; }
                case "double" -> { this.cvalue.setType(XmlRpcTypes.Double);
                    this.cdata.setLength(0); this.readCdata = true; }
                case "dateTime.iso8601" -> {
                    this.cvalue.setType(XmlRpcTypes.Date); this.cdata.setLength(0); this.readCdata = true; }
                case "base64" -> {
                    this.cvalue.setType(XmlRpcTypes.Base64); this.cdata.setLength(0); this.readCdata = true; }
                case "struct" -> this.cvalue.setType(XmlRpcTypes.Struct);
                case "array" -> this.cvalue.setType(XmlRpcTypes.Array);
                case "nil" -> this.cvalue.setType(XmlRpcTypes.Nil);
            }
        }
    }
    /**
     * DTO represents parsed XML value
     */
    static class Value {
        // value type
        private XmlRpcTypes type = XmlRpcTypes.String;
        // value data
        private Object value;
        // used if value data is an object
        private final Map<String, Object> struct = new LinkedHashMap<>();
        // used if value is array
        private final List<Object> array = new ArrayList<>();
        private String nextMemberName;
        /**
         * Return actual value, uses different fields based on value type
         * @return
         *      actual value
         */
        public Object getValue() { return switch (type) {
                case Array -> this.array; case Struct -> this.struct; default -> value; }; }

        void onEndElement(Value child) { switch (this.type) { case Array -> this.array.add(child.getValue());
                case Struct -> this.struct.put(this.nextMemberName, child.getValue()); } }
        public void setType(XmlRpcTypes type) {  this.type = type; }
        /**
         * Parse input value according to its types
         * @param cdata
         *          raw string, extracted from XML
         */
        public void characterData(String cdata) { switch (this.type) {
                case Integer -> this.value = Integer.valueOf(cdata.trim());
                case Boolean -> this.value = "1".equals(cdata.trim());
                case Double -> this.value = Double.valueOf(cdata.trim());
                case Date -> { try { this.value = XMLRPC_DATE_FORMAT.parse(cdata.trim());
                    } catch (ParseException p) { throw new RuntimeException(p.getMessage()); } }
                case Base64 -> this.value = Base64.getDecoder().decode(cdata.getBytes());
                case String -> this.value = cdata; case Struct -> nextMemberName = cdata;
                default -> throw new IllegalStateException("Unexpected value: %s".formatted(this.type));  }  }
        public String toString() { return "%s element %s".formatted(this.type, this.value); }
    }
    /**
     * An XML writer, used to generate 'hand-made' XMLs
     */
    public static class XmlWriter {
        private final StringBuilder buf = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        /**
         * resets writer, internal buffer will store just XML header line
         */
        public void reset() {
            this.buf.setLength("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".length());
        }
        /**
         * append single XML tag with data
         *
         * @param elem tag name
         * @param data tag data
         */
        public void writeEl(String elem, String data) {
          this.buf.append("<").append(elem).append(">").append(data).append("</").append(elem).append(">");
        }
        /**
         * Append XML tag start
         *
         * @param elem tag name
         */
        public XmlWriter startEl(String elem) { this.buf.append("<").append(elem).append(">"); return this; }
        public XmlWriter endEl(String elem) { this.buf.append("</").append(elem).append(">"); return this; }
        public void emptyEl(String elem) { this.buf.append("<").append(elem).append("/>"); }
        /**
         * Append string with/without escaping
         *
         * @param text   text data
         * @param escape if true - escape special characters
         */
        public XmlWriter write(String text, boolean escape) {
            // escape special chars
            if (escape) for (char c : text.toCharArray()) switch (c) {
                        case '<' -> this.buf.append("&lt;"); case '&' -> this.buf.append("&amp;");
                        default -> this.buf.append(c);  }
            else this.buf.append(text); // or append 'as-is'
            return this;
        }
        public String toString() {
            return this.buf.toString();
        }
        public byte[] getBytes() {
            return this.buf.toString().getBytes(StandardCharsets.UTF_8);
        }
    }
    public static class XmlRpcException extends Exception { private final int code;
        public XmlRpcException(int code, String message) { super(message); this.code = code; }
        public int getCode() {
            return code;
        }  }
    /**
     * An interface to define XML-RPC handler
     */
    public interface XmlRpcHandler {
        /**
         * Execute call on server
         * @param methodName
         *          method name
         * @param params
         *          method parameters
         * @return
         *      call results
         * @throws Exception
         *          everything can happen during a call
         */
        Object execute(String methodName, List<Object> params) throws Exception;
    }
    public interface AuthenticatedXmlRpcHandler {
        /**
         * Execute XML-RPC call on server but with authentication -with provided login&password
         * @param methodName
         *          method name
         * @param params
         *          method parameters
         * @param username
         *          provided principal
         * @param password
         *          provided password (BASIC authentication)
         * @return
         *      call results
         * @throws Exception
         *      on any errors
         */
        Object execute(String methodName, List<Object> params, String username, String password) throws Exception;
    }
}

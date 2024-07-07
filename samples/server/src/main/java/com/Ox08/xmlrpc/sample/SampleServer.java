package com.Ox08.xmlrpc.sample;
import com.Ox08.xmlrpc.XmlRPC;
import com.Ox08.xmlrpc.XmlRpcServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * This is standalone implementation of my XML-RPC server, based on 'internal' sun.net.httpserver
 * @author alex0x08
 * @since 1.0
 */
public class SampleServer {
    private final static Logger LOG = Logger.getLogger("0x08-xml-rpc"); // note on logger name
    /**
     * Famous 'void main()' function
     * @param args
     *          array with passed arguments
     * @throws IOException
     *          on I/O errors
     */
    public static void main(String[] args) throws IOException {
        //check for 'appDebug' parameter
        boolean debugMessages = Boolean.parseBoolean(System.getProperty("appDebug", "false"));
        // adjust logging levels to show more messages, if appDebug was set
        if (debugMessages) { LOG.setUseParentHandlers(false);
            final Handler systemOut = new ConsoleHandler();systemOut.setLevel(Level.FINE);
            LOG.addHandler(systemOut); LOG.setLevel(Level.FINE);
            LOG.fine("debug messages enabled");
        }
        // create HTTP-server
        final HttpServer server = HttpServer.create(new InetSocketAddress(8000), 50);
        // initialize default handler
        final DefaultServerHttpHandler dsh = new DefaultServerHttpHandler();
        // add some demo handlers
        dsh.addHandler("example", new DemoXmlRpcHandler());
        // one with authentication enabled
        dsh.addHandler("auth", new SampleAuthenticatedXmlRpcHandler());
        // setup default XML-RPC handler
        dsh.addHandler("$default", new DefaultXmlRpcHandler());
        server.createContext("/", dsh);
        server.setExecutor(null); // creates a default executor
        LOG.info("Started  XML-RPC server on http://%s:%d".formatted(server.getAddress().getHostName(),
                server.getAddress().getPort()));
        server.start(); //finally that the server
    }
    static class SampleAuthenticatedXmlRpcHandler
            implements XmlRPC.AuthenticatedXmlRpcHandler {
        public Object execute(String method, List<Object> v, String user, String password) throws Exception {
            System.out.println("user:"+user+" pass: "+password);
            if ("admin".equals(user) && "admin1".equals(password))
                                return "Hello %s".formatted(user);
            throw new XmlRPC.XmlRpcException(5, "Access denied");
        }
    }
    static class DefaultXmlRpcHandler implements XmlRPC.XmlRpcHandler {
        @Override
        public Object execute(String methodName, List<Object> params) {
            return "0x08 XML-RPC Server 1.0";
        }
    }
    /**
     * A demo handler, note on not using XmlRpcHandler interface here.
     * Also, all methods parameters should be non-primitive!
     */
    public static class DemoXmlRpcHandler {
        /**
         * Sample method, to call from XML-RPC
         * @param x
         *          some integer
         * @param y
         *          some another integer
         * @return
         *      a map with 2 properties: sum - would contain sum of two provided integers
         *                               difference - would be x - y result
         */
        public Map<String, Object> sumAndDifference(Integer x, Integer y) {
            final Map<String, Object> result = new HashMap<>();
            result.put("sum", x + y);
            result.put("difference", x - y);
            return result;
        }
    }
    /**
     * This is default handler for Java's http server, used to handle XML-RPC requests
     */
    public static class DefaultServerHttpHandler implements HttpHandler {
        private final XmlRpcServer xrs = new XmlRpcServer(); // an instance of XmlRpcServer
        /**
         * Binds provided handler to XML-RPC server instance
         * @param handlerName
         *              a handler's unique name
         * @param h
         *          handler instance
         */
        public void addHandler(String handlerName, Object h) { this.xrs.addHandler(handlerName, h); }
        /**
         * Handles input HTTP request
         * @param t the exchange containing the request from the
         *                 client and used to send the response
         * @throws IOException
         *          on I/O errors
         */
        public void handle(HttpExchange t) throws IOException {
            // ignore all non POST requests
            if (!"POST".equals(t.getRequestMethod())) {
                t.sendResponseHeaders(400, 0); t.close(); return;
            }
            if (LOG.isLoggable(Level.FINE))
                LOG.fine("got http request: %s".formatted(t.getRequestURI()));
            // process request
            try (OutputStream so = t.getResponseBody()) {
                String[] creds = null; // check for Basic Auth
                if (t.getRequestHeaders().containsKey("Authorization"))
                    creds = this.xrs.extractCredentials(t.getRequestHeaders().get("Authorization").get(0));
                // execute call and get result (there would be XML encoded in byte array)
                final byte[] result = creds!=null? this.xrs.execute(t.getRequestBody(),creds[0],creds[1]) :
                            this.xrs.execute(t.getRequestBody());
                // set response 'content-type' header
                t.getResponseHeaders().add("Content-type", "text/xml");
               // send headers
                t.sendResponseHeaders(200, result.length);
                // send body
                so.write(result); so.flush();
            } catch (Exception e) {
                LOG.warning(e.getMessage());
            }
        }
    }
}

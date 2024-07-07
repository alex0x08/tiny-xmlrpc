
package com.Ox08.xmlrpc;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
/**
 * XML-RPC client implementation
 * @since 1.0
 * @author alex0x08
 */
public class XmlRpcClient
        implements XmlRPC.XmlRpcHandler {
    private final static int MAX_WORKERS = 50; // number of allowed parallel workers
    private final URL url; //target server url
    private String auth; //stores login/password pair for BASIC auth
    private final Deque<ClientWorker> pool = new ArrayDeque<>(); // stores all pending requests
    /**
     * Build XML-RPC client for specified server url
     * @param url
     *          server url
     */
    public XmlRpcClient(URL url) { this.url = url; }
    /**
     * Specify authentication, if required
     * @param user
     *          princpipal
     * @param password
     *          password
     */
    public void setBasicAuthentication(String user, String password) {
        if (user == null || password == null) this.auth = null; else {
            this.auth = (new String(Base64.getEncoder().encode((user + ":" + password).getBytes()))).trim(); }
    }
    /**
     * Executes remote XML-RPC call
     * @param method
     *          method name
     * @param params
     *          method parameters
     * @return
     * @throws XmlRPC.XmlRpcException
     *              on remote errors
     * @throws IOException
     *              on I/O errors
     */
    public Object execute(String method, List<Object> params) throws XmlRPC.XmlRpcException, IOException {
        // get a worker
        final ClientWorker clientWorker = getWorker();
        // execute single call to server
        try { return clientWorker.execute(method, params); } finally {
            // if there were no errors - push worker back to pool
            if (!clientWorker.fault) this.pool.push(clientWorker); }
    }
    /**
     * Get a worker for call processing
     * @return
     *      a worker instance
     * @throws IOException
     *          on I/O errors
     */
    private ClientWorker getWorker() throws IOException {
        // if there is no any workers - create one
        if (this.pool.isEmpty()) return new ClientWorker(this);
        // if pool overloaded - throw exception
        if (this.pool.size() > MAX_WORKERS) throw new IOException("Too many requests!");
        // remove one worker from pool and return it
        return this.pool.pop();
    }
    /**
     * Dedicated worker for client side
     */
    static class ClientWorker extends XmlRPC {
        private boolean fault; // sign that response has error
        private Object result; // result object
        private final XmlRpcClient client; // link to client instance
        public ClientWorker(XmlRpcClient client) { this.client = client; }
        public Object execute(String method, List<Object> params) throws XmlRpcException, IOException {
            this.fault = false; // reset fault flag
            try {
                // build XML request
                final byte[] request = buildRequest(method, params);
                // open connection to XML-RPC server
                // length argument is used to pass 'Content-Length' header
                final URLConnection con = openConnection(request.length);
                // send our XML-RPC request to server
                try (OutputStream out = con.getOutputStream()) { out.write(request); out.flush(); }
                // receive and parse response
                try (InputStream in = con.getInputStream()) { parse(in); }
            } catch (Exception x) {
                // at this step here could be only I/O errors
                throw new IOException(x);
            }
            // process XML-RPC fault
            if (this.fault && this.result != null && this.result instanceof Map<?, ?> f) {
                    try { throw new XmlRpcException( Integer.parseInt(f.get("faultCode").toString()),
                                f.get("faultString").toString().trim());
                    } catch (Exception x) { throw new XmlRpcException(0, "Invalid fault response"); }
            }
            return this.result;
        }
        /**
         * This is called from XmlRPC internally
         * @param what
         *          parsed object
         */
        @Override
        public void objectParsed(Object what) { this.result = what; }
        /**
         * Opens connection to remote XML-RPC server
         * @param len
         *          request length (required for header)
         * @return
         *          an URLConnection instance
         * @throws IOException
         *          on I/O errors
         */
        protected URLConnection openConnection(int len) throws IOException {
            final URLConnection con = this.client.url.openConnection();
            con.setDoInput(true); con.setDoOutput(true);
            con.setUseCaches(false); con.setAllowUserInteraction(false);
            con.setRequestProperty("Content-Length", Integer.toString(len));
            con.setRequestProperty("Content-Type", "text/xml");
            // add 'Basic Auth' header
            if (this.client.auth != null)
                con.setRequestProperty("Authorization", "Basic " + this.client.auth);
            return con;
        }
        /**
         * Create call response as XML serialized to byte array
         *
         * @param method
         *          method name
         * @param params
         *          method parameters
         * @return
         *          call response
         */
        protected byte[] buildRequest(String method, List<Object> params) {
            final XmlRPC.XmlWriter writer = new XmlWriter();
            writer.startEl("methodCall").startEl("methodName").write(method, false)
                    .endEl("methodName").startEl("params");
            for (Object param : params) {
                writer.startEl("param"); writeObject(param, writer); writer.endEl("param"); }
            writer.endEl("params").endEl("methodCall"); return writer.getBytes();
        }
    }
}


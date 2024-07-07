package com.Ox08.xmlrpc;
import java.io.InputStream;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * This is my tiny XML-RPC server engine
 */
public class XmlRpcServer {
    private static final int MAX_WORKERS = 500;
    private final static Logger LOG = Logger.getLogger("0x08-xml-rpc");
    // stores all registered handlers
    private final Map<String, Object> handlers = new LinkedHashMap<>();
    // this pool stores all current requests
    private final Deque<ServerWorker> pool = new ArrayDeque<>();
    /**
     * Register handler
     *
     * @param handlername handlers unique name, use $default to set default handler
     * @param handler     handler's object
     */
    public void addHandler(String handlername, Object handler) {
        if (handlername == null || handlername.isEmpty())
            throw new IllegalStateException("Cannot add handler: name is null or empty.");
        if (handler == null) throw new IllegalStateException("Cannot add handler: object is null or empty.");
        // put handler to handlers map, not on 'Invoker' wrapper, used for POJOs
        this.handlers.put(handlername, handler instanceof XmlRPC.XmlRpcHandler
                || handler instanceof XmlRPC.AuthenticatedXmlRpcHandler ? handler : new Invoker(handler));
    }
    /**
     * Execute XML-RPC call
     *
     * @param is input request data as stream
     * @return
     */
    public byte[] execute(InputStream is) {
        return execute(is, null, null);
    }
    /**
     * Executes XML-RPC call
     *
     * @param is       input request data as stream
     * @param user     username (for auth mode)
     * @param password password (for auth)
     * @return
     */
    public byte[] execute(InputStream is, String user, String password) {
        final ServerWorker serverWorker = getWorker();
        // execute call
        try { return serverWorker.execute(is, user, password); } finally {
            this.pool.push(serverWorker);  // push worker back to pool
        }
    }
    public String[] extractCredentials(String  a) {
        if (a == null || !a.toLowerCase().startsWith("basic")) return null;
        return new String(Base64.getDecoder().decode(a.substring("Basic ".length()).trim()), StandardCharsets.UTF_8)
                .split(":", 2);
    }
    /**
     * Get first available worker
     *
     * @return
     */
    private ServerWorker getWorker() {
        // create new worker if pool is empty
        if (this.pool.isEmpty()) return new ServerWorker(this);
        // prevent overload
        if (this.pool.size() > MAX_WORKERS) throw new RuntimeException("Too many workers!"); return this.pool.pop();
    }
    /**
     * Dedicated class to invoke method via Reflection API
     */
    static class Invoker
            implements XmlRPC.XmlRpcHandler {
        private final Object invokeTarget; // target handler instance (could be any class)
        private final Class<?> targetClass; // target handler class
        /**
         * Construct with provided handler
         *
         * @param target handler instance (could be any class, that does not implement XmlRpcHandler)
         */
        public Invoker(Object target) {
            this.invokeTarget = target;
            this.targetClass = (this.invokeTarget instanceof Class) ?
                    (Class<?>) this.invokeTarget : this.invokeTarget.getClass();
            if (LOG.isLoggable(Level.FINE)) LOG.fine("Target object is :%s".formatted(this.targetClass));
        }
        /**
         * Execute method in handler via Reflection API
         *
         * @param methodName unique method name
         * @param params     list with params
         * @return call result
         * @throws Exception
         *              on I/O errors
         */
        public Object execute(String methodName, List<Object> params) throws Exception {
            final List<Class<?>> argClasses = new ArrayList<>(); final List<Object> argValues = new ArrayList<>();
            if (params != null && !params.isEmpty()) {
                // here we check provided params and try to unwrap basic types
                for (final Object v : params) { argValues.add(v);
                    if (LOG.isLoggable(Level.FINE))
                            LOG.fine("param class: %s value=%s".formatted(v.getClass().getName(), v));
                    argClasses.add(v.getClass().isPrimitive()
                            ? MethodType.methodType(v.getClass()).unwrap().returnType() : v.getClass());
                }
            }
            final Method method; // method to call
            if (LOG.isLoggable(Level.FINE)) {  LOG.fine("Calling method: %s".formatted(methodName));
                for (int c = 0; c < argClasses.size(); c++)
                    LOG.fine("Parameter %d: %s = %s".formatted(c, argClasses.get(c), argValues.get(c)));
            }
            // get method via 'Reflection API'
            method = this.targetClass.getMethod(methodName, argClasses.toArray(new Class[0]));
            // and try to invoke
            return method.invoke(this.invokeTarget, argValues.toArray(new Object[0]));
        }
    }
    /**
     * This is server worker class, that responsible for single XML-RPC request processing
     */
    static class ServerWorker extends XmlRPC {
        private final List<Object> inParams = new ArrayList<>(); // call parameters
        private final XmlRpcServer rpcServer; // link to server instance
        ServerWorker(XmlRpcServer rpcServer) {
            this.rpcServer = rpcServer;
        }
        /**
         * Execute a call
         *
         * @param is       raw request data
         * @param user
         *          principal (used only if authentication enabled)
         * @param password
         *             password
         * @return
         *      call result (XML as byte array)
         */
        public byte[] execute(InputStream is, String user, String password) {
            //reset variables
            this.inParams.clear();  final XmlRPC.XmlWriter writer = new XmlWriter();
            try {
                // extract method name from XML-RPC request
                String methodName = parse(is),handlerName;
                Object handler;
                // try to detect handler name
                final int dot = methodName.indexOf(".");
                // if method name contains '.'
                if (dot > -1) {
                    // handler is a first part, methodName - second
                    // ex.: 'example.sumAndDifference'  where 'example' is handler name
                    // and 'sumAndDifference' - method name
                    handlerName = methodName.substring(0, dot); handler = this.rpcServer.handlers.get(handlerName);
                    if (handler != null) methodName = methodName.substring(dot + 1);
                } else {
                    // otherwise, assume that method name = handler name, and use 'execute' as default method name
                    handlerName = methodName; handler = this.rpcServer.handlers.get(handlerName); methodName ="execute";
                }
                if (LOG.isLoggable(Level.FINE)) LOG.fine("calling method name: %s ,inparams: %s".formatted(methodName, this.inParams));
                // try to use default handler, if none found in request
                if (handler == null) handler = this.rpcServer.handlers.get("$default");
                if (handler == null)
                    throw new RuntimeException("RPC handler object \"%s\" not found and no default handler registered."
                            .formatted(dot > -1 ? handlerName : methodName));
                final Object outParam;
                // execute method call
                if (handler instanceof AuthenticatedXmlRpcHandler a)
                    outParam =  a.execute(methodName, this.inParams, user, password);
                else if (handler instanceof XmlRpcHandler r)
                    outParam = r.execute(methodName,this.inParams);
                else throw new IllegalStateException("Incorrect handler class :%s".formatted(handler.getClass().getName()));
                if (LOG.isLoggable(Level.FINE)) LOG.fine("outparam: %s".formatted(outParam));
                // build response and output data
                buildResponse(outParam, writer); return writer.getBytes();
            } catch (Exception x) {
                LOG.warning(x.getMessage());
                writer.reset();// must be called to reset response buffer!
                // respond as 'fault'
                writeError(x instanceof XmlRpcException rpcException ? rpcException.getCode():0, x.getMessage(), writer);
                return writer.getBytes();
            }
        }
        /**
         * Append parsed parameter from XML-RPC request
         * @param what
         *        parsed parameter value
         */
        @Override
        public void objectParsed(Object what) { this.inParams.add(what); }
        /**
         * Build XML response
         * @param callResponse
         *      call result
         * @param writer
         *      XML writer instance
         */
        void buildResponse(Object callResponse, XmlRPC.XmlWriter writer) {
            writer.startEl("methodResponse").startEl("params").startEl("param");
            writeObject(callResponse, writer);
            writer.endEl("param").endEl("params").endEl("methodResponse");
        }
        /**
         * Build XML-RPC fault
         * @param code
         *          fault code
         * @param message
         *          fault message
         * @param writer
         *          XML writer instance
         */
        void writeError(int code, String message, XmlRPC.XmlWriter writer) {
            final Map<String, Object> h = new LinkedHashMap<>();
            h.put("faultCode", code); h.put("faultString", message);
            writer.startEl("methodResponse").startEl("fault"); writeObject(h, writer);
            writer.endEl("fault").endEl("methodResponse");
        }
    }
}

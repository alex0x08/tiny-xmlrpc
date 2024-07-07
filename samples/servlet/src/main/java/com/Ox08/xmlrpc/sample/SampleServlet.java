package com.Ox08.xmlrpc.sample;
import com.Ox08.xmlrpc.XmlRPC;
import com.Ox08.xmlrpc.XmlRpcServer;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
/**
 * Sample servlet implementation
 * @author alex0x08
 */
public class SampleServlet extends HttpServlet {
    protected XmlRpcServer xmlrpc = new XmlRpcServer();
    @Override
    public void init(ServletConfig config) {
        //register our sample handler
        this.xmlrpc.addHandler("hello", new DemoXmlRpcHandler());
    }
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        // execute XML-RPC call and get response as byte array
        final byte[] result = this.xmlrpc.execute(req.getInputStream());
        // set response content type and length
        res.setContentType("text/xml"); res.setContentLength(result.length);
        // respond to client
        try (ServletOutputStream so = res.getOutputStream()) { so.write(result); so.flush(); }
    }
    /**
     * Sample handler
     */
    static class DemoXmlRpcHandler implements XmlRPC.XmlRpcHandler {
        /**
         * This sample method just responds list of request parameters as string
         * @param methodname
         *          method name
         * @param params
         *          method parameters
         * @return
         *          list with request parameters
         */
        public Object execute(String methodname, List<Object> params) {
            final StringBuilder out = new StringBuilder("Request was:\n");
            for (Object p :params)
                out.append("param: ").append(p).append('\n');
            return out.toString();
        }
    }
}

package com.Ox08.xmlrpc.sample;
import com.Ox08.xmlrpc.XmlRpcClient;
import java.net.URL;
import java.util.List;
/**
 * Sample standalone XML-RPC client
 * @author alex0x08
 */
public class SampleClient {
    public static void main(String[] args) throws Exception {
        // a test call for foreign remote XML-RPC server
        //XmlRpcClient client = new XmlRpcClient(new URL("http://betty.userland.com/RPC2"));
        //System.out.println(client.execute("examples.getStateName", List.of(15)));
        //should return 'Iowa'

        // test call to local standalone XML-RPC server
        // see 'tiny-xmlrpc-library-sample-server' subproject
        XmlRpcClient client2 = new XmlRpcClient(new URL("http://localhost:8000"));
        System.out.println(client2.execute("example.sumAndDifference", List.of(15,55)));

        XmlRpcClient clientAuth = new XmlRpcClient(new URL("http://localhost:8000"));
        clientAuth.setBasicAuthentication("admin","admin1");
        System.out.println(clientAuth.execute("auth.execute", List.of(1,2)));


        // test call to local servlet-based XML-RPC server
       // see 'tiny-xmlrpc-library-sample-servlet' subproject
       // XmlRpcClient client3 = new XmlRpcClient(new URL("http://localhost:8080/api/xmlrpc"));
       //System.out.println(client3.execute("hello", List.of(15)));
    }
}

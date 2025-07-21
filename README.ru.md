[![ru](https://img.shields.io/badge/lang-ru-red.svg)](https://github.com/alex0x08/teleporta/blob/main/README.ru.md)

# Tiny XMLRPC
My tiny XML-RPC protocol implementation in less than 500 lines of Java code.
[Client](https://github.com/alex0x08/tiny-xmlrpc/blob/main/library/src/main/java/com/Ox08/xmlrpc/XmlRpcClient.java) and 
[server](https://github.com/alex0x08/tiny-xmlrpc/blob/main/library/src/main/java/com/Ox08/xmlrpc/XmlRpcServer.java).

[Here](https://github.com/alex0x08/tiny-xmlrpc/tree/main/samples) you can find sample projects for [client](https://github.com/alex0x08/tiny-xmlrpc/tree/main/samples/client), 
[server](https://github.com/alex0x08/tiny-xmlrpc/tree/main/samples/server) and [servlet](https://github.com/alex0x08/tiny-xmlrpc/tree/main/samples/servlet)

# Articles

This library has been initially created for my [article](https://blog.0x08.ru/tiny-xmlrpc-call-them-all) (in russian), which was also [published on Habr](https://habr.com/ru/articles/837942/).


# In action
Below a screenshot of both client and server running locally:
![In action](https://github.com/alex0x08/tiny-xmlrpc/blob/main/images/in-action.jpg?raw=true)


# Sample clients in other languages
As proof of correct implementation, I created some samples in other languages:  [Tcl](https://github.com/alex0x08/tiny-xmlrpc/tree/main/clients/test.tcl),
[Common Lisp](https://github.com/alex0x08/tiny-xmlrpc/tree/main/clients/test-clisp-cbcl), [C++](https://github.com/alex0x08/tiny-xmlrpc/tree/main/clients/test-xml-rpc.cpp),
[Python](https://github.com/alex0x08/tiny-xmlrpc/tree/main/clients/test.py),[Perl](https://github.com/alex0x08/tiny-xmlrpc/tree/main/clients/xml-rpc.pl).

Sample client in pure C interact with my XML-RPC server:

![In C](https://github.com/alex0x08/tiny-xmlrpc/blob/main/images/c-sample.png?raw=true)

Sample client in Haskell:

![Haskell](https://github.com/alex0x08/tiny-xmlrpc/blob/main/images/haskell-sample.png?raw=true)

FreePascal/Lazarus:

![Lazarus](https://github.com/alex0x08/tiny-xmlrpc/blob/main/images/lazarus-sample.png?raw=true)

# How to build
Tiny XML-RPC has no external dependencies and can be easily built on JDK 17+ with Apache Maven, just run from project parent folder:

```
mvn clean package
```

That will build library and all samples.

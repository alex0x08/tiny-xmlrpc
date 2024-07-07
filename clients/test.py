import xmlrpc.client; 

with xmlrpc.client.ServerProxy("http://localhost:8000") as proxy:
    print("3 is even: %s" % str(proxy.example.execute("opopopomnknjopo oлол лоло",7999)))

use XML::RPC;
 
my $xmlrpc = XML::RPC->new('http://localhost:8000');
my $result = $xmlrpc->call( 'example.execute', { state1 => 12, state2 => 28 } );

print $result;
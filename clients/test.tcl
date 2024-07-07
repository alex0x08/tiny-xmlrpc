
package require xmlrpc

if {[catch {set res [xmlrpc::call "http://127.0.0.1:8000" "" "example.sumAndDifference" { {int 221} {int 22} }]} e]} {
	puts "xmlrpc call failed: $e"
} else {
	puts "res: $res."
}

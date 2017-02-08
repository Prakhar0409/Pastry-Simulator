package dht1;

public class Message {
	Node srcNode;
	Node destNode;
	int srcId;
	int destId;
	
	long key;
	String type; 	//"join","forward"
}

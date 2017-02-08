package dht1;

public class Message {
	Node srcNode;
	Node destNode;
	int srcId;
	int destId;
	
	long key;
	String str_key;
	String type; 	//"join"->join request,"normal"->normal msg, "info"->join reply with state info;
					//"lastinfo" -> last node sends info
	int level;		//used in case of "info" and "join" msg
	
	public Message(){
		
	}
	
	public Message(String type,Node src){
		this.type = type;
		this.srcNode = src;
		this.level = 0;
		if(this.type=="join"){
			this.key = this.srcNode.node_id;
			this.str_key = this.srcNode.str_node_id;
		}
	}
	
	public Message(String type,int level,Node src){
		this.type = type;
		this.srcNode = src;
		this.level = level;
	}
}

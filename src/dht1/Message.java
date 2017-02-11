package dht1;

public class Message {
	Node srcNode;
	int srcId;
	long key;
	String str_key;
	String type; 	//"join"->join request,"lookup"->normal msg, "info"->join reply with state info;
					//"lastinfo" -> last node sends info; "newnode_ll"->msg from a fully initialised new node which should be in my left leafset
					//"newnode_rl"->msg from a fully initialised new node which should be in my right leafset
					//"newnode_rt" -> msg from initialised new node that can be in routing table
					//"newnode_n" -> msg from initialised new node that can be in neighbouring set
	
	int hops;		//used in case of "info" and "join" msg; counts hops
	
	public Message(String type,Node src){
		this.type = type;
		this.srcNode = src;
		this.hops = 0;
		if(this.type=="join"){
			this.key = this.srcNode.node_id;
			this.str_key = this.srcNode.str_node_id;
		}
	}
	
	public Message(String type,Node src,long key){
		this.type = type;
		this.srcNode = src;
		this.hops = 0;
		this.key = key;
		this.str_key = Long.toString(key,Node.base);
	}
	
	/*
	 * Constructor for message when `key` is not necessary: Put key as -1;
	 * */
	public Message(String type,int hops,Node src){
		this.type = type;
		this.srcNode = src;
		this.hops = hops;
		this.key = -1;
		this.str_key = this.srcNode.str_node_id;
	}
	
	public Message(String type,int level,Node src,long key){
		this.type = type;
		this.srcNode = src;
		this.hops = level;
		this.key = key;
		this.str_key = Long.toString(key,Node.base);
	}
}

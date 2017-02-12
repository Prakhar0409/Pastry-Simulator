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
	String value;	//used in case of a lookup reply
	
	/**
	 * Constructor for message when `key` is not necessary: Put key as -1;
	 * */
	public Message(String type,int hops,Node src){
		this.type = type;
		this.srcNode = src;
		this.hops = hops;
		this.key = -1;
		this.str_key = this.srcNode.str_node_id;
	}
	
	/**
	 * Constructor when message carries the key:value tuples
	 * */
	public Message(String type,int hops,Node src,long key,String val){
		this.type = type;
		this.hops = hops;
		this.srcNode = src;
		this.key = key;
		this.str_key = correctSize(Long.toString(key,Node.base));
		this.value = val;
	}
	
	public String correctSize(String a){
		while(a.length()<Node.key_size/Node.b){
			a = "0"+a;
		}
		return a;
	}
	
	
	/**
	 * Constructor when no value but message to be routed by key
	 * */
	public Message(String type,int level,Node src,long key){
		this.type = type;
		this.srcNode = src;
		this.hops = level;
		this.key = key;
		this.str_key = correctSize(Long.toString(key,Node.base));
	}
}

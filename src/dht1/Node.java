package dht1;

import java.util.Vector;


public class Node implements Runnable{
    /**
     * Each node has its own message queue. Create thread for each node
     */
	//static global node info
	static int b=4;		//base 2^b
	static int base=16;	// cal as - 2^b
	static int L=32;	//size of leaf set	2*2^b
	static int key_size = 16;		//in bits or logN;	hence 2^16 nodes 
	static long maxNodes =  (long) Math.pow(2,key_size);
	static int rows = key_size/b;
	
	//State information
	
	long node_id;		// Unique node id for each node
	String public_addr;	//ip or  other public name also the thread name
	Node[][] r_table = new Node[rows][base];		//routing table
	Vector<Node> l_lset = new Vector<Node>();			//leaf set
	Vector<Node> r_lset = new Vector<Node>();			//leaf set
	Vector<Node> n_set;			//neighbourhood set
	Thread th;
	
    private static Vector<Node> nodeList = new Vector<Node>();
    private MessageQueue<Message> msgQ= new MessageQueue<Message>(100);
    

    public Node(long nodeid){
        this.node_id=nodeid;
         // add the necessary arguments for a node in the constructor
    }
    
    public void start(){
    	th = Thread.currentThread();
    	public_addr = th.getName();
    	node_id = this.hashCode()%maxNodes;
    }

    /**
     *Each thread will perform its function inside run
     */
    public void run() {
    	
    	System.out.println("Hey fron new node: "+ Thread.currentThread().getName());
    	this.start();		//Node join
    	System.out.println("Node "+ this.node_id+" initialized");
    	while(true){
    		Message msg = getNextMessage();
    		if(msg==null){
    			continue;
    		}
    		
    			
    		
    	}
    }

    public Node getNode(int index) {
        return nodeList.get(index);
    }

    public int getNodeListLength(){
        return nodeList.size();
    }
   
    public void addMessage(Message msg) {
        msgQ.add(msg);
    }
   
    public void addNode(Node node)
    {
         // add this node to the list
    }

    /* Blocking Version*/
    private Message getNextMessage(){
        return (Message) msgQ.getMessage();
    }

    /* Non blocking version */
    private Message pollMessage(){
        return (Message) msgQ.poll();
    }
}


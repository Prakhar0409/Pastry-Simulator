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
    String str_node_id;
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
        str_node_id = Long.toString(node_id,base);
        System.out.println("Node id: "+ node_id);
        System.out.println("Node id in base: "+ base + " : "+str_node_id);
//    	node_id = public_addr.hashCode()%maxNodes;
        this.l_lset.add(this);
        this.r_lset.add(this);
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
            switch (msg.type) {
                case "join":
                        System.out.println("Request to join from node: "+ msg.srcNode.node_id);
                    break;
                case "forward":
                        System.out.println("Message to route key: "+ msg.key);
                        route(msg);
                    break;
                default:

            }
    	}
    }

    public long dist_key(long x,long y){
    	return (x>y)?x-y:y-x;
    }

    public int sharedPrefix(long a,long b){
        int len = 0;
        String x=Long.toString(a,base),y = Long.toString(b,base);
        for(int i=0;i<x.length() && i<y.length();i++){
            if(x.charAt(i) != y.charAt(i)){
                break;
            }
            len++;
        }
        return len;
    }
    
    public void route(Message msg){
        //check leaf set
        if(l_lset.lastElement().node_id >= msg.key && msg.key<= r_lset.lastElement().node_id){
            boolean left = true;
            long min_dist=9999999,tmp_dist;
            int min_idx=0;
            for(int i=0;i<l_lset.size();i++){
                tmp_dist = dist_key(l_lset.get(i).node_id,msg.key);
                if(tmp_dist<min_dist){
                    min_dist = tmp_dist;
                    min_idx = i;
                }
            }
            for(int i=0;i<r_lset.size();i++){
                tmp_dist = dist_key(r_lset.get(i).node_id,msg.key);
                if(tmp_dist<min_dist){
                    left = false;
                    min_dist = tmp_dist;
                    min_idx = i;
                }
            }
            if((left && l_lset.get(min_idx)==this) || (!left && r_lset.get(min_idx)==this)){
                // msg for you now return the answer
                // handleMsg();
                System.out.println("Notification:- Node: "+str_node_id+" received a msg of type: "+msg.type +" from: "+msg.srcNode.str_node_id);                
            }else if(left){
                l_lset.get(min_idx).addMessage(msg);
            }else{
                r_lset.get(min_idx).addMessage(msg);
            }
            return;
        }
        // if not in leaf set now go to routing table
        int l = sharedPrefix(this.node_id,msg.key);
        String key_str = Long.toString(msg.key);
        int dl = key_str.charAt(l);
        if(r_table[l][dl] != null){
            r_table[l][dl].addMessage(msg);    // entry found in routing table
            return;
        }

        //if not found in routing table or leaf set
        int where = -4;   //-1->neighbour, -2->left, -3->right; +ve->routing table; -4->nothing
        int min_idx=-1;
        long min_dist = 99999999,dist;
        for(int i=0;i<n_set.size();i++){
            int l1 = sharedPrefix(n_set.get(i).node_id,msg.key);
            if(l1>=l){
                dist = dist_key(n_set.get(i).node_id,msg.key);
                if(dist<min_dist){
                    where = -1;
                    min_dist = dist;
                    min_idx = i;
                }
            }
        }
        for(int i=0;i<l_lset.size();i++){
            int l1 = sharedPrefix(l_lset.get(i).node_id,msg.key);
            if(l1>=l){
                dist = dist_key(l_lset.get(i).node_id,msg.key);
                if(dist<min_dist){
                    where = -2;
                    min_dist = dist;
                    min_idx = i;
                }
            }
        }
        for(int i=0;i<r_lset.size();i++){
            int l1 = sharedPrefix(r_lset.get(i).node_id,msg.key);
            if(l1>=l){
                dist = dist_key(r_lset.get(i).node_id,msg.key);
                if(dist<min_dist){
                    where = -3;
                    min_dist = dist;
                    min_idx = i;
                }
            }
        }
        for(int i=0;i<rows;i++){
            for(int j=0;i<base;j++){
                if(r_table[i][j]!=null){
                    int l1 = sharedPrefix(r_table[i][j].node_id,msg.key);
                    if(l1>=l){
                        dist = dist_key(r_table[i][j].node_id,msg.key);
                        if(dist<min_dist){
                            where = i*base + j;
                            min_dist = dist;
                            min_idx = i;
                        }
                    }
                }
            }
        }
        if(where==-1){
            n_set.get(min_idx).addMessage(msg);
            return;
        }else if(where ==-2){
            l_lset.get(min_idx).addMessage(msg);
            return;
        }else if(where==-3){
            r_lset.get(min_idx).addMessage(msg);
            return;
        }else if(where>0){
            r_table[where/rows][where%rows].addMessage(msg);
        }else{
            //should never reach here;
            System.out.println("Panic: Routing table incorrect at node: "+this.node_id);
            // correctTables();
            return;
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


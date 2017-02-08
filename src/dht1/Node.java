package dht1;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;
import java.util.Vector;


public class Node implements Runnable{
    /**
     * Each node has its own message queue. Create thread for each node
     */
	//static global node info
	static int b=4;					//base 2^b
	static int base=16;				// cal as - 2^b
	static int L=2;					//size of leaf set	ideally 2*2^b since node added in leafsets
	static int M=2;					//size of neighbourhood set
	static int key_size = 16;		//in bits or logN;	hence 2^16 nodes To change also need to change convertbytestoInt
	static long maxNodes = (long) Math.pow(2,key_size);
	static int rows = (int) Math.ceil(key_size/b);
	
	
	//State information
	long node_id;		// Unique node id for each node
    String str_node_id;
	String public_addr;	//ip or  other public name also the thread name
	Node[][] r_table = new Node[rows][base];		//routing table
	Vector<Node> l_lset = new Vector<Node>();			//leaf set
	Vector<Node> r_lset = new Vector<Node>();			//leaf set
	Vector<Node> n_set;			//neighbourhood set
	Thread th;
	public Node known;
	
    private static Vector<Node> nodeList = new Vector<Node>();
    private MessageQueue<Message> msgQ= new MessageQueue<Message>(100);
    
    public Node(){
    	byte[] id;
        id = generateRandomID(key_size/8);  //length in bytes
        this.node_id = convertBytesToInt(id);
        System.out.println("Node id is: "+node_id);
    }

    public Node(long nodeid){
        this.node_id=nodeid;
    }
    
    public void start(){
    	th = Thread.currentThread();
    	public_addr = th.getName();
    	node_id = this.hashCode()%maxNodes;
        str_node_id = Long.toString(node_id,base);
        System.out.println("Node id: "+ node_id);
        System.out.println("Node id in base: "+ base + " : "+str_node_id);
//    	node_id = public_addr.hashCode()%maxNodes;
//        this.l_lset.add(this);
//        this.r_lset.add(this);
        for(int i=0;i<rows;i++){
        	for(int j=0;j<base;j++){
        		int col = 0;
        		if(this.str_node_id.charAt(i)>='0' && this.str_node_id.charAt(i)<='9'){
        			col = this.str_node_id.charAt(i) - '0';
        		}else{
        			col = this.str_node_id.charAt(i) - 'a'+10;
        		}
        		r_table[i][col] = this;
        	}
        }
        if(known!=null){
        	Message m = new Message("join",this);
        	known.addMessage(m);					//X sent a join message to A
        	updateNeighbours(known);
        }
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
                	boolean f = route(msg);
                	Message m;
                	if(f){
                		m = new Message("lastinfo",msg.level+1,this);		//send state info
                	}else{
                		m = new Message("info",msg.level+1,this);		//send state info
                	}
                    msg.srcNode.addMessage(m);					//send state info directly to x
                    msg.level += 1;
                    
                    break;
                case "forward":
                    System.out.println("Message to route key: "+ msg.key);
                    route(msg);
                    break;
                case "info":
                	System.out.println("Message of type info route key: "+ msg.key);
                	updateTables(msg.srcNode);
                case "lastinfo":
                	System.out.println("Message of type info route key: "+ msg.key);
                	updateTables(msg.srcNode);
                	updateLeafSet(msg.srcNode);
                	//send the new node update to all the known nodes
                	Message m1 = new Message("newnode",this);
                	for(int i=0;i<this.l_lset.size();i++){
                		if(this.l_lset.get(i) != null){
                			this.l_lset.get(i).addMessage(m1);
                		}
                	}
                	for(int i=0;i<this.r_lset.size();i++){
                		if(this.r_lset.get(i) != null){
                			this.r_lset.get(i).addMessage(m1);
                		}
                	}
                	for(int i=0;i<this.n_set.size();i++){
                		if(this.n_set.get(i) != null){
                			this.n_set.get(i).addMessage(m1);
                		}
                	}
                	for(int i=0;i<rows;i++){
                		for(int j=0;j<base;j++){
                			if(this.r_table[i][j]!=null){
                				this.r_table[i][j].addMessage(m1);
                			}
                		}
                	}
                	//all initialisations for this node done.
                default:

            }
    	}
    }
    
    public void updateLeafSet(Node n){
    	if(node_id<n.node_id){
    		this.l_lset.add(n);		//add Z to leaf set
    	}else{
    		this.r_lset.add(n);		//add Z to leaf set
    	}
		for(int i=0;i<n.l_lset.size();i++){
			if(this.l_lset.size()>=L/2){
				break;
			}
			this.l_lset.add(n.l_lset.get(i));	//update X left leaf set
		}
		for(int i=0;i<n.r_lset.size();i++){
			if(this.r_lset.size()>=L/2){
				break;
			}
			this.r_lset.add(n.r_lset.get(i));	//update X right leaf set
		}
    }

    public double dist_phy(Node a,Node b){
    	// todo
    	return 0;
    }
    
    public void updateTables(Node n){
    	int l = sharedPrefix(this.node_id,n.node_id);
    	for(int i=0;i<l;i++){
    		for(int j=0;j<base;j++){
    			if(this.r_table[i][j]==null){
    				this.r_table[i][j] = n.r_table[i][j];
    			}else if(n.r_table[i][j]!=null && dist_phy(this,this.r_table[i][j])>dist_phy(this,n.r_table[i][j])){
    				this.r_table[i][j] = n.r_table[i][j];
    			}
    		}
    	}
    	for(int j=0;j<base;j++){
    		this.r_table[l][j] = n.r_table[l][j];
    	}
    	int dl = 0;
    	if(this.str_node_id.charAt(l)>='0' && this.str_node_id.charAt(l)<='9'){
    		dl = this.str_node_id.charAt(l) - '0'; 
    	}else{
    		dl = this.str_node_id.charAt(l) - 'a' + 10;
    	}
    	this.r_table[l][dl] = this;
    }
    
    public void updateNeighbours(Node n){
    	this.n_set.add(n);
    	for(int i=0;i<n.n_set.size();i++){
    		if(this.n_set.size()>=M){
    			break;
    		}
			this.n_set.add(n.n_set.get(i));
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
    
    // returns true if this is the last node else false
    public boolean route(Message msg){
        //check leaf set
    	long least=0,greatest;
    	if(l_lset.isEmpty()){
    		least = this.node_id;
    	}else{
    		least = l_lset.lastElement().node_id;
    	}
    	if(r_lset.isEmpty()){
    		greatest = this.node_id;
    	}else{
    		greatest = r_lset.lastElement().node_id;
    	} 	
        if(least >= msg.key && msg.key<= greatest){
            int where = 0;		//-1=left; 1=right
            long min_dist= dist_key(this.node_id, msg.key),tmp_dist;
            int min_idx=0;
            for(int i=0;i<l_lset.size();i++){
                tmp_dist = dist_key(l_lset.get(i).node_id,msg.key);
                if(tmp_dist<min_dist){
                	where = -1;
                    min_dist = tmp_dist;
                    min_idx = i;
                }
            }
            for(int i=0;i<r_lset.size();i++){
                tmp_dist = dist_key(r_lset.get(i).node_id,msg.key);
                if(tmp_dist<min_dist){
                    where = 1;
                    min_dist = tmp_dist;
                    min_idx = i;
                }
            }
            if(where == 0){
                // msg for you now return the answer
                // handleMsg();
                System.out.println("Notification:- Node: "+str_node_id+" received a msg of type: "+msg.type +" from: "+msg.srcNode.str_node_id);
                return true;
            }else if(where<0){
                l_lset.get(min_idx).addMessage(msg);
            }else{
                r_lset.get(min_idx).addMessage(msg);
            }
            return false;
        }
        // if not in leaf set now go to routing table
        int l = sharedPrefix(this.node_id,msg.key);
        String key_str = Long.toString(msg.key);
         char x = key_str.charAt(l);
         int dl=0;
         if(x>='0' && x<='9'){
        	 dl = x-'0';
         }else{
        	 dl = x-'a'+10;
         }
         
        if(r_table[l][dl] != null){
            r_table[l][dl].addMessage(msg);    // entry found in routing table
            return false;
        }

        //if not found in routing table or leaf set
        int where = -4;   //-1->neighbour, -2->left, -3->right; +ve->routing table; -4->self; -5->nothing
        int min_idx=-1;
        long min_dist = dist_key(this.node_id,msg.key),dist;
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
                            min_idx = i*base+j;
                        }
                    }
                }
            }
        }
        if(where==-1){
            n_set.get(min_idx).addMessage(msg);
            return false;
        }else if(where ==-2){
            l_lset.get(min_idx).addMessage(msg);
            if(l_lset.get(min_idx) == this){return true;}
            return false;
        }else if(where==-3){
            r_lset.get(min_idx).addMessage(msg);
            if(r_lset.get(min_idx) == this){return true;}
            return false;
        }else if(where>0){
            r_table[min_idx/rows][min_idx%rows].addMessage(msg);
            return false;
        }else if(where==-4){
        	 System.out.println("Notification:- Node: "+str_node_id+" received a msg of type: "+msg.type +" from: "+msg.srcNode.str_node_id);
             return true;
        }else{
            //should never reach here;
            System.out.println("Panic: Routing table incorrect at node: "+this.node_id);
            // correctTables();
            return false;
        }
    }

    private static byte[] generateRandomID(int length) {
        Random random = new Random();
        byte[] bytes = new byte[length];
        for(int i=0; i<bytes.length; i++) {
            bytes[i] = (byte) (random.nextInt() % 256);     //256 -> 2^8
        }
        return bytes;
    }

    private long convertBytesToInt(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int result = buffer.getShort() & 0xffff; 		//only key size 16; bitmask for only +ve range        
        return (long)result;
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


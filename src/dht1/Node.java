package dht1;

import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;

public class Node implements Runnable{
    /**
     * Implements a node of Pastry - a scalable, robust and resilient P2P protocol 
     */
	//Configuration info for each Node in Pastry;
	public static int b = 4;									//base of all keys is 2^b
	public static int base = (int) Math.pow(2, b);				
	public static int L = 2;									//size of leaf set
	public static int M = 2;									//size of neighbourhood set
	public static int key_size = 16;							//in bits;
	public static long max_nodes = (long) Math.pow(2,key_size);	//maximum Nodes possible in network
	public static int rows = (int) Math.ceil(key_size/Node.b);	//# of rows in routing table
	public static int addr_range = 100;							//size of 2-D sq. array where each entry serves as public addr
	
	// Node State, maintained at each node
	public Pair<Integer,Integer> public_addr = null;		//public_id (ip) of the node
	public long node_id;									// Unique node id for each node
    public String str_node_id;								// node_id expressed in base `base` (2^b)	
	public Node[][] r_table = new Node[rows][base];			//routing table
	public Vector<Node> l_lset = new Vector<Node>();		//leaf set smaller - left leaf set has smaller ids
	public Vector<Node> r_lset = new Vector<Node>();		//leaf set bigger - right leaf set has bigger ids
	public Vector<Node> n_set = new Vector<Node>();			//neighbourhood set
	public Thread th;										//thread running node logic
	public Node known;										//initial node known to this node, null if this inits the network

	//for deletion commands from simulator
	public boolean self_delete = false;						//to simulate deletes

	//for lookups from simulator - unsed as for now. Used messaging system directly - todo
	public boolean lookup = false;
	public long lookup_key; 
	
    private MessageQueue<Message> msgQ= new MessageQueue<Message>(100);		//to store msgs coming to a node
    private Storage<String> store = new Storage<String>();					//local data storage at the node
    
    /**
     * Constructor1 : when node_id not given 
     * */
    public Node(){
    	byte[] id;
        id = KeyGenerator.generateRandomID(key_size/8);  //length in bytes
        this.node_id = KeyGenerator.convertBytesToInt(id);
        if(this.public_addr == null){
        	Random random = new Random();
        	this.public_addr = new Pair<Integer,Integer>(random.nextInt(Node.addr_range+1),random.nextInt(Node.addr_range+1));
        }
    }

    /**
     * Constructor2 : when node_id given 
     * */
    public Node(long nodeid){
        this.node_id=nodeid;
        if(this.public_addr == null){
        	Random random = new Random();
        	this.public_addr = new Pair<Integer,Integer>(random.nextInt(Node.addr_range+1),random.nextInt(Node.addr_range+1));
        }
    }
    
    /**
     * Initializing node before it starts to function
     * Sets the current thread, str_node_id, self entries in routing table etc.
     * */
    public void start(){
    	this.th = Thread.currentThread();
        this.str_node_id = Long.toString(this.node_id,Node.base);
        while(this.str_node_id.length()<(key_size/b)){
        	this.str_node_id = '0'+this.str_node_id;
        }
        System.out.println(this.node_id+": Starting Node : "+ this.node_id+" ("+this.str_node_id+")");
        for(int i=0;i<Node.rows;i++){
    		int col = 0;
    		if(this.str_node_id.charAt(i)>='0' && this.str_node_id.charAt(i)<='9'){
    			col = this.str_node_id.charAt(i) - '0';
    		}else{
    			col = this.str_node_id.charAt(i) - 'a'+10;
    		}
    		this.r_table[i][col] = this;
        }

        // if not the 1st Node to join the network
        if(known!=null){
        	Message m = new Message("join",0,this);		// key doesn't matter Message(type,hops,srcNode);
        	this.known.addMessage(m);					//X sent a join message to A (X,A as referenced in paper)
        	this.updateNeighbourSet(known);
        }
    }

    /**
     *Each thread will perform its function inside run.
     *This is the main loop that handles calling of all other functions a node performs
     */
    public void run() {
    	this.start();											//basic node-thread inits
    	System.out.println(this.node_id + ": Node-Thread initialized");
    	Message m;
    	boolean f;
    	while(true){
    		if(this.self_delete){
    			deleteNode();
    			return;
    		}
    		Message msg = getNextMessage();
    		if(msg==null){
    			continue;
    		}
    		
			System.out.println(this.node_id +": Received message-> type:"+ msg.type +" src:"+msg.srcNode.node_id+" key:"+msg.key+" ("+msg.str_key+")");
    		
    		msg.hops+=1;							    			//increase hops
            switch (msg.type) {
                case "join":
                	f = route(msg);
                	if(f){
                		m = new Message("lastinfo",msg.hops,this);	//direct message, so key doesn't matter
                	}else{
                		m = new Message("info",msg.hops,this);		//direct message, so key doesn't matter
                	}
                	
                	//send message to node if it did not leave the network
                    if(msg.srcNode != null){
                    	msg.srcNode.addMessage(m);					//send state info directly to x
                    }
                    break;
                case "lookup":
                	f = route(msg);
                	if(f){
                		if(msg.srcNode == this){
                			if(store.get(msg.key)!=null){
                				//subtract 1 hop because lookup started by sending a msg from main thread to node.
                				System.out.println(this.node_id+": Lookup reply for msg: "+msg.key+
                									". I had the key. Value:"+store.get(msg.key)+" Hops: "+(msg.hops-1));
                			}else{
                				//subtract 1 hop because lookup started by sending a msg from main thread to node.
                				System.out.println(this.node_id+": Lookup reply for msg: "+msg.key+
                									". I had the key. No value. Hops: "+(msg.hops-1));
                			}
                		}else{
                			String val = store.get(msg.key);
                			//Message(type,hops,src,key_to_which_this_is_reply,value of the key);		direct msg to the src
                			//subtract a hop since the first lookup msg was from thread to node to simulate
                			m = new Message("lookup_reply",msg.hops-1,this,msg.key,val);		
                			msg.srcNode.addMessage(m);
                		}
                	}else{
                		//chill, you already forwarded it in route :-P
                	}
                	break;
                case "lookup_reply":
                	//subtraction of a hope done in the sending of reply.
                	System.out.println(this.node_id+": Lookup reply for msg: "+msg.key+ " value: "+msg.value+
                						" from node:"+msg.srcNode.node_id+" in hops: "+msg.hops);
                	break;
                case "add_key":
                	f = route(msg);
                	if(f){
                		store.add(msg.key, msg.value);
                		System.out.println(this.node_id+": Added key: "+msg.key+" value: "+msg.value+
                							" as requested by node: "+msg.srcNode.node_id);
                	}
                	break;
                case "send_key_val":
                	//direct msg arrived from a node to request me to store the key-value since it is leaving
                	store.add(msg.key,msg.value);
                	System.out.println(this.node_id+": Added key: "+msg.key+" value: "+msg.value+
							" as requested by node: "+msg.srcNode.node_id+" since it is leaving the network.");
                	break;
                case "remove_key":
                	f = route(msg);
                	if(f){
                		String val = store.remove(msg.key);
                		System.out.println(this.node_id+": Removed key: "+msg.key+" value: "+val +
    							" as requested by node: "+msg.srcNode.node_id);
                	}
                	break;
                case "delete":
                	deleteNodeFromState(msg);
                	break;
                case "forward":
                    route(msg);
                    break;
                case "info":
                	//received an info msg to update Routing tables
                	updateTables(msg.srcNode);
                	break;
                case "lastinfo":
                	//all initializations for this node 
                	updateTables(msg.srcNode);
                	System.out.println(this.node_id+": Updated routing tables");
                	updateLeafSet(msg.srcNode);
                	System.out.println(this.node_id+": Updated Leaf sets");
                	//send the new node update to all the known nodes
                	System.out.println(this.node_id+": Sending new-node-update message to others nodes I know");
                	for(int i=0;i<this.l_lset.size();i++){
                		if(this.l_lset.get(i) != null){
                			Message m1 = new Message("newnode_rl",this);
                			this.l_lset.get(i).addMessage(m1);
                		}
                	}
                	for(int i=0;i<this.r_lset.size();i++){
                		if(this.r_lset.get(i) != null){
                			Message m1 = new Message("newnode_ll",this);
                			this.r_lset.get(i).addMessage(m1);
                		}
                	}
                	for(int i=0;i<this.n_set.size();i++){
                		if(this.n_set.get(i) != null){
                			Message m1 = new Message("newnode_n",this);
                			this.n_set.get(i).addMessage(m1);
                		}
                	}
                	for(int i=0;i<rows;i++){
                		for(int j=0;j<base;j++){
                			if(this.r_table[i][j]!=null && this.r_table[i][j].node_id!=this.node_id){	//preventing from nulls and self sends
                				Message m1 = new Message("newnode_rt",this);
                				this.r_table[i][j].addMessage(m1);
                			}
                		}
                	}
                	break;
                case "newnode_ll":
                	this.updateLeafFromNewNode(msg.srcNode,msg);
                	this.updateTables(msg.srcNode);
                	this.updateNeighbourSet(msg.srcNode);
                	break;
                case "newnode_rl":
                	this.updateLeafFromNewNode(msg.srcNode,msg);
                	this.updateTables(msg.srcNode);
                	this.updateNeighbourSet(msg.srcNode);
                	break;
                case "newnode_rt":
                	this.updateTables(msg.srcNode);
//                	this.updateNeighbourSet(msg.srcNode);
                	break;
                case "newnode_n":
                	this.updateNeighbourSet(msg.srcNode);
                	break;
                default:

            }
    	}
    }
    
    
    /**
     * Returns true if key->a > key->b
     * */
    public boolean greaterThanForKeys(long a,long b){
    	long tmp1 = a-b;
    	if(tmp1<0){tmp1 += Node.max_nodes;}
    	long tmp2 = b-a;
    	if(tmp2<0){tmp2 += Node.max_nodes;}
    	if(tmp1<tmp2){return true;}		//a is bigger than b draw a circle with 8 nodes and check:P
    	return false;
    }
    
    /**
     * Returns true if key->a < key->b on node id space;
     * */
    public boolean smallerThanForKeys(long a,long b){
    	return this.greaterThanForKeys(b, a);
    }
    
    /**
     * Update the leaf sets of nodes that received a msg from the newly added node
     * */
    public void updateLeafFromNewNode(Node n,Message m){
    	if(this.greaterThanForKeys(this.node_id,n.node_id)){	//if my.id > n.id then add n to lesser leaf set
    		//update to left leaf set
    		int i=0;
    		while(i<this.l_lset.size() && this.greaterThanForKeys(this.l_lset.get(i).node_id,n.node_id)){
    			i++;
    		}
    		this.l_lset.add(i,n);
    		while(this.l_lset.size()>L/2){
    			this.l_lset.remove(this.l_lset.size()-1);
    		}
    	}else{
    		//update to right leaf set
    		int i=0;
    		while(i<this.r_lset.size() && this.smallerThanForKeys(this.r_lset.get(i).node_id,n.node_id)){
    			i++;
    		}    		
    		this.r_lset.add(i,n);    		
    		while(this.r_lset.size()>L/2){
    			this.r_lset.remove(this.r_lset.size()-1);
    		}
    	}
    	
    	//this cases is required only while starting network on the first node
    	if(this.l_lset.isEmpty()){
			this.l_lset.add(n);
		}
		if(this.r_lset.isEmpty()){
			this.r_lset.add(n);
		}
    }
    
    /**
     * initializes leaf set of new node based on the nearest neighbour in the nodeId space
     * @param n - nearest neighbour in the nodeId space
     */
    public void updateLeafSet(Node n){
    	if(this.smallerThanForKeys(this.node_id, n.node_id)){		//if my key < n.key
    		this.r_lset.add(n);		//add Z or n to right/bigger leaf set
    	}else{
    		this.l_lset.add(n);		//add Z to leaf set
    	}
		for(int i=0;i<n.l_lset.size();i++){
			if(this.l_lset.size()>=L/2){
				break;
			}
			this.l_lset.add(n.l_lset.get(i));	//update X left leaf set
			System.out.println(this.node_id+": Added to left leaf set, node: "+n.node_id);
		}
		for(int i=0;i<n.r_lset.size();i++){
			if(this.r_lset.size()>=L/2){
				break;
			}
			this.r_lset.add(n.r_lset.get(i));	//update X right leaf set
			System.out.println(this.node_id+": Added to right leaf set, node: "+n.node_id);
		}
		if(this.l_lset.isEmpty()){
			this.l_lset.add(n);
		}
		if(this.r_lset.isEmpty()){
			this.r_lset.add(n);
		}
    }

    
    /**
     * Given information from node n, updates my routing table;
     * */
    public void updateTables(Node n){
    	int l = sharedPrefix(this.node_id,n.node_id);
    	System.out.println(this.node_id+": ("+this.str_node_id+") updating tables with node:"
    						+n.node_id+" ("+n.str_node_id+") prefix:"+l);
    	for(int i=0;i<l+1 && i<rows;i++){
    		for(int j=0;j<base;j++){
    			if(this.r_table[i][j]==null){
    				this.r_table[i][j] = n.r_table[i][j];
    			}else if(n.r_table[i][j]!=null && dist_phy(this,this.r_table[i][j])>dist_phy(this,n.r_table[i][j])){
    				this.r_table[i][j] = n.r_table[i][j];
    			}
    		}
    	}

    	if(l<this.str_node_id.length()){
	    	int dl = 0;
	    	if(this.str_node_id.charAt(l)>='0' && this.str_node_id.charAt(l)<='9'){
	    		dl = this.str_node_id.charAt(l) - '0'; 
	    	}else{
	    		dl = this.str_node_id.charAt(l) - 'a' + 10;
	    	}
	    	this.r_table[l][dl] = this;
    	}
    }
    
    /**
     * Node 'n' is close to the current node. Use this to update my neigbourSet
     * NeigbourSet is arranged in the order of distances
     * */
    public void updateNeighbourSet(Node n){
//    	this.n_set.add(n);
//    	for(int i=0;i<n.n_set.size();i++){
//    		if(this.n_set.size()>=M){
//    			break;
//    		}
//			this.n_set.add(n.n_set.get(i));
//    	}
    	//add at the correct index - only if not present
    	if(!this.n_set.contains(n)){
    		double dist = dist_phy(this,n);
    		int i=0;
    		for(;i<this.n_set.size();i++){
    			if(dist < dist_phy(this,this.n_set.get(i))){
    				break;
    			}
    		}
    		this.n_set.add(i,n);
    	}
    	for(int i=0;i<n.n_set.size();i++){
    		if(this.n_set.contains(n.n_set.get(i))){continue;}
    		double dist = dist_phy(this,n.n_set.get(i));
    		int j=0;
    		for(;j<this.n_set.size();j++){
    			if(this.n_set.get(j)!=null && dist < dist_phy(this,this.n_set.get(j))){
    				break;
    			}
    		}
    		this.n_set.add(j,n.n_set.get(i));
    	}
    	while(this.n_set.size()>M){
    		this.n_set.remove(this.n_set.size()-1);
		}
    	return;
    }
    
    /**
     * returns distance between two keys on the circle
     * */
    public long dist_key(long x,long y){
    	return (x>y)?x-y:y-x;
    }
    
    /**
     * returns 'Manhattan' distance between public addresses of two nodes
     * */
    public double dist_phy(Node a,Node b){
    	//Manhattan distance
    	double d = Math.abs(a.public_addr.getLeft() - b.public_addr.getLeft());
    	d += Math.abs(a.public_addr.getRight() - b.public_addr.getRight());
    	return d;
    }

    /**
     * returns longest shared prefix between two keys or ids on the hash circle
     * */
    public int sharedPrefix(long a,long b){
        int len = 0;
        String x=Long.toString(a,base),y = Long.toString(b,base);
        while(x.length()<key_size/Node.b){
        	x = '0'+x;
        }
        while(y.length()<key_size/Node.b){
        	y = '0'+y;
        }
        for(int i=0;i<x.length() && i<y.length();i++){
            if(x.charAt(i) != y.charAt(i)){
                break;
            }
            len++;
        }
        return len;
    }
    
    /**
     * This is the main routing logic. Takes a message and routes it to node with id closest 
     * to the key accessible from the current node position. 
     * Returns true if already at the closest node.
     * */
    public boolean route(Message msg){
        //check leaf set
    	long smallest,greatest;
    	if(l_lset.isEmpty()){
    		smallest = this.node_id;
    	}else{
    		smallest = l_lset.lastElement().node_id;
    	}
    	if(r_lset.isEmpty()){
    		greatest = this.node_id;
    	}else{
    		greatest = r_lset.lastElement().node_id;
    	}
    	System.out.println(this.node_id+ ": left_neigh:"+smallest+" right_neigh:"+greatest);
    	// if smallest == greatest => only 2 nodes in system and every keys falls in our range
        if( smallest==greatest || (this.greaterThanForKeys(msg.key,smallest)  && this.greaterThanForKeys(greatest,msg.key))){
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
                System.out.println(this.node_id+ ": Notification1:- Node closest to msg: "+msg.key+" of type: "+msg.type +" from: "+msg.srcNode.node_id);
                return true;
            }else if(where<0){
            	System.out.println(this.node_id+": msg:+"+msg.key +" Forwarded to:"+this.l_lset.get(min_idx).node_id);
            	l_lset.get(min_idx).addMessage(msg);
            }else{
            	System.out.println(this.node_id+": msg:+"+msg.key +" Forwarded to:"+this.r_lset.get(min_idx).node_id);
                r_lset.get(min_idx).addMessage(msg);
            }
            return false;
        }
        // if not in leaf set now go to routing table
	    int l = sharedPrefix(this.node_id,msg.key);
	    System.out.println(this.node_id+": str_node_id:"+this.str_node_id+" msg_id:"+msg.key+" ("+msg.str_key+") sharedPrefix:"+l);
	    if(key_size/Node.b <= l){return true;}
	    String key_str = msg.str_key;
	    char x = key_str.charAt(l);
	    int dl=0;
	    if(x>='0' && x<='9'){
	    	dl = x-'0';
	    }else{
        	dl = x-'a'+10;
        }
         
         if(this.r_table[l][dl]!=null && this.r_table[l][dl]==this){
        	 return true;
         }
         if(r_table[l][dl] != null){
        	 System.out.println(this.node_id+": msg:+"+msg.key +" Forwarded to:"+this.r_table[l][dl].node_id);
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
            for(int j=0;j<base;j++){
                if(this.r_table[i][j]!=null){
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
        	System.out.println(this.node_id+": msg:+"+msg.key +" Forwarded to:"+this.n_set.get(min_idx).node_id);
            n_set.get(min_idx).addMessage(msg);
            return false;
        }else if(where ==-2){
        	System.out.println(this.node_id+": msg:+"+msg.key +" Forwarded to:"+this.l_lset.get(min_idx).node_id);
            l_lset.get(min_idx).addMessage(msg);
            if(l_lset.get(min_idx) == this){System.out.println("Panic::::::");return true;}
            return false;
        }else if(where==-3){
        	System.out.println(this.node_id+": msg:+"+msg.key +" Forwarded to:"+this.r_lset.get(min_idx).node_id);
            r_lset.get(min_idx).addMessage(msg);
            if(r_lset.get(min_idx) == this){System.out.println("Panic::::::");return true;}
            return false;
        }else if(where>0){
        	if(this.r_table[min_idx/base][min_idx%base] != null){
        		System.out.println(this.node_id+": msg:+"+msg.key +" Forwarded to:"+this.r_table[min_idx/base][min_idx%base].node_id);
        		r_table[min_idx/base][min_idx%base].addMessage(msg);
        		return false;
        	}else{
        		//that node suddenly left
        		//add message for self to recheck
        		this.addMessage(msg);
        		return false;
        	}
        }else if(where==-4){
        	System.out.println("Notification2:- Node: "+str_node_id+" received a msg of type: "+msg.type +" from: "+msg.srcNode.node_id);
            return true;
        }else{
            //should never reach here;
            System.out.println("Panic: Routing table incorrect at node: "+this.node_id);
            // correctTables();
            return false;
        }
    }
    
    public void printNodeState(Node n){
    	System.out.println("######################################################################");
    	System.out.println("Node id: "+n.node_id+" ("+n.str_node_id+")"+" - public_addr:"+n.public_addr.toString());
    	System.out.println("Leaf Set");
    	for(int i=0;i<n.l_lset.size();i++){
    		if(n.l_lset.get(i)!=null){
    			System.out.print(n.l_lset.get(i).node_id+", ");
//    			System.out.print(n.l_lset.get(i).node_id+" ("+n.l_lset.get(i).str_node_id+"), ");
    		}else{
    			System.out.print("null, ");
    		}
    	}
    	System.out.print(" || ");
    	for(int i=0;i<n.r_lset.size();i++){
    		if(n.r_lset.get(i)!=null){
    			System.out.print(n.r_lset.get(i).node_id+", ");
//    			System.out.print(n.r_lset.get(i).node_id+" ("+n.r_lset.get(i).str_node_id+"), ");
    		}else{
    			System.out.print("null, ");
    		}
    	}
    	System.out.println();
    	
    	System.out.println("Neigbourhood Set");
    	for(int i=0;i<n.n_set.size();i++){
    		if(n.n_set.get(i)!=null){
    			System.out.print(n.n_set.get(i).node_id+", ");
//    			System.out.print(n.n_set.get(i).node_id+" ("+n.n_set.get(i).str_node_id+"), ");
    		}else{
    			System.out.print("null, ");
    		}
    	}
    	System.out.println();
    	System.out.println("Routing table");
    	for(int j=0;j<base;j++){
    		System.out.print(j+" \t\t");
    	}
    	System.out.println();
    	for(int i=0;i<rows;i++){
    		for(int j=0;j<base;j++){
    			if(n.r_table[i][j]!=null){
    				System.out.print(n.r_table[i][j].node_id+", \t");
    			}else{
        			System.out.print("null, \t");
        		}
    		}
    		System.out.println();
    	}
    	System.out.println("**********************************************************************");
    }

    /**
     * Others deleting a node from their state tables, leaf sets and neighbour sets
     * 1. delete from leaf sets
     * 2. delete from neighbourhood set
     * 3. delete from the routing tables;
     * */
    public void deleteNodeFromState(Message msg){
    	for(int i=0;i<this.n_set.size();i++){
    		if(this.n_set.get(i)!=null && this.n_set.get(i).node_id == msg.srcNode.node_id){
    			this.n_set.remove(i);
    		}
    	}
    	for(int i=0;i<this.l_lset.size();i++){
    		if(this.l_lset.get(i)!=null && this.l_lset.get(i).node_id == msg.srcNode.node_id){
    			this.l_lset.remove(i);
    		}
    	}
    	for(int i=0;i<this.r_lset.size();i++){
    		if(this.r_lset.get(i)!=null && this.r_lset.get(i).node_id == msg.srcNode.node_id){
    			this.r_lset.remove(i);
    		}
    	}
    	for(int i=0;i<rows;i++){
    		for(int j=0;j<base;j++){
    			if(this.r_table[i][j]!=null && this.r_table[i][j].node_id == msg.srcNode.node_id){
        			this.r_table[i][j] = null;
        		}
    		}
    	}
    	return;
    }
    
    /**
     * Deleting a node.
     * 1. Migrates its data store to others appropriately
     * 2. Notify others about its leaving
     * */
    public void deleteNode(){
    	//Migrating the data store;
    	Message m;
    	ArrayList<Long> key_set = store.getKeys();
    	for(int i=0;i<key_set.size();i++){
    		if(greaterThanForKeys(key_set.get(i),this.node_id)){	//key > node_id
    			m = new Message("send_key_val",0,this,key_set.get(i),store.get(key_set.get(i)));
    			if(r_lset.size()>0){
    				this.r_lset.get(0).addMessage(m);
    			}
    		}else{				// key < node_id
    			m = new Message("send_key_val",0,this,key_set.get(i),store.get(key_set.get(i)));
    			if(l_lset.size()>0){
    				this.l_lset.get(0).addMessage(m);
    			}
    		}
    	}
    	// notifying others about leaving the network
    	Message msg = new Message("delete",0,this,this.node_id);	//msg key doesn't matter since you will directly send it to whoever knows you
    	for(int i=0;i<this.n_set.size();i++){
    		if(this.n_set.get(i)!=null){
    			this.n_set.get(i).addMessage(msg);
    		}
    	}
    	for(int i=0;i<this.l_lset.size();i++){
    		if(this.l_lset.get(i)!=null){
    			this.l_lset.get(i).addMessage(msg);
    		}
    	}
    	for(int i=0;i<this.r_lset.size();i++){
    		if(this.r_lset.get(i)!=null){
    			this.r_lset.get(i).addMessage(msg);
    		}
    	}
    	for(int i=0;i<rows;i++){
    		for(int j=0;j<base;j++){
    			if(this.r_table[i][j] != null){
    				this.r_table[i][j].addMessage(msg);
    			}
    		}
    	}
    	return;
    }

    /**
     * Add message to the message queue
     * */
    public void addMessage(Message msg) {
        msgQ.add(msg);
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


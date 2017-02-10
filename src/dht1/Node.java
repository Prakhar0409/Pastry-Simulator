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
	static int rows = (int) Math.ceil(key_size/Node.b);
	
	
	//State information
	long node_id;		// Unique node id for each node
    String str_node_id;
	public String public_addr;	//ip or  other public name also the thread name
	Node[][] r_table = new Node[rows][base];		//routing table
	Vector<Node> l_lset = new Vector<Node>();			//leaf set smaller- left leaf set has smaller ids
	Vector<Node> r_lset = new Vector<Node>();			//leaf set bigger
	Vector<Node> n_set = new Vector<Node>();			//neighbourhood set
	Thread th;
	public Node known;
	
    private static Vector<Node> nodeList = new Vector<Node>();
    private MessageQueue<Message> msgQ= new MessageQueue<Message>(100);
    
    public Node(){
    	byte[] id;
        id = generateRandomID(key_size/8);  //length in bytes
        this.node_id = convertBytesToInt(id);
//        System.out.println("Node id is: "+node_id);
    }

    public Node(long nodeid){
        this.node_id=nodeid;
    }
    
    public void start(){
    	th = Thread.currentThread();
    	public_addr = th.getName();
//    	node_id = this.hashCode()%maxNodes;
        str_node_id = Long.toString(node_id,base);
        while(this.str_node_id.length()<4){
        	this.str_node_id = '0'+this.str_node_id;
        }
        System.out.println("Node id: "+ node_id+" str: "+this.str_node_id+"  rows:"+rows);
//        System.out.println("Node id in base: "+ base + " : "+str_node_id);
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
        	updateNeighbourSet(known);
        }
    }

    /**
     *Each thread will perform its function inside run
     */
    public void run() {
    	
//    	System.out.println("Hey fron new node: "+ Thread.currentThread().getName());
    	this.start();		//Node join
    	System.out.println(this.node_id + ": Node initialized");
    	while(true){
    		Message msg = getNextMessage();
    		if(msg==null){
    			continue;
    		}
    		System.out.println(this.node_id +": Received message-> type:"+ msg.type+" src:"+msg.srcNode.node_id+" key:"+msg.key);
    		msg.level+=1;
            switch (msg.type) {
                case "join":
                	boolean f = route(msg);
                	Message m;
                	if(f){
                		m = new Message("lastinfo",msg.level,this,msg.srcNode.node_id);		//send state info
                	}else{
                		m = new Message("info",msg.level,this,msg.srcNode.node_id);		//send state info
                	}
                    msg.srcNode.addMessage(m);					//send state info directly to x
//                    msg.level += 1;
                    
                    break;
                case "forward":
                    route(msg);
                    break;
                case "info":
                	updateTables(msg.srcNode);
                	break;
                case "lastinfo":
                	//all initialisations for this node 
                	updateTables(msg.srcNode);
                	System.out.println(this.node_id+": Updated routing tables");
                	updateLeafSet(msg.srcNode);
                	System.out.println(this.node_id+": Updated Leaf sets");
                	//send the new node update to all the known nodes
                	System.out.println(this.node_id+": Sending new-node-update message to others nodes i know");
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
                	updateLeafFromNewNode(msg.srcNode,msg);
                	updateTables(msg.srcNode);
                	break;
                case "newnode_rl":
                	updateLeafFromNewNode(msg.srcNode,msg);
                	updateTables(msg.srcNode);
                	break;
                case "newnode_rt":
                	updateTables(msg.srcNode);
                	break;
                case "newnode_n":
                	break;
                default:

            }
    	}
    }
    
    /*
     * Returns true if key->a > key->b
     * */
    public boolean greaterThanForKeys(long a,long b){
    	long tmp1 = a-b;
    	if(tmp1<0){tmp1 += maxNodes;}
    	long tmp2 = b-a;
    	if(tmp2<0){tmp2 += maxNodes;}
    	if(tmp1<tmp2){return true;}		//a is bigger than b draw a circle with 8 nodes and check:P
    	return false;
    }
    
    public boolean smallerThanForKeys(long a,long b){
    	return this.greaterThanForKeys(b, a);
    }
    
    public void updateLeafFromNewNode(Node n,Message m){
    	if(this.greaterThanForKeys(this.node_id,n.node_id)){	//if my.id > n.id then add n to lesser leaf set
    		//update to left leaf set
//    		System.out.println(this.node_id+": inside update Leaf Set if");
    		int i=0;
    		while(i<this.l_lset.size() && this.greaterThanForKeys(this.l_lset.get(i).node_id,n.node_id)){
    			i++;
    		}
//    		if(!this.l_lset.isEmpty() && this.l_lset.get(i).node_id == this.node_id){
//    			return;
//    		}
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
//    		if(!this.r_lset.isEmpty() && this.r_lset.get(i).node_id == this.node_id){
//    			return;
//    		}    		
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
//    	for(int i=0;i<this.l_lset.size();i++){
//    		if(this.l_lset.get(i)!=null && this.l_lset.get(i).node_id!=n.node_id){
////    			System.out.println(this.node_id+": Send update leafset:");
//    			this.l_lset.get(i).addMessage(m);
//    		}
//    	}
//    	for(int i=0;i<this.r_lset.size();i++){
//    		if(this.r_lset.get(i)!=null && this.r_lset.get(i).node_id!=n.node_id){
//    			this.r_lset.get(i).addMessage(m);
//    		}
//    	}
    }
    
    //initialises leafset of new node based on the nearest neighbour in the nodeId space
    public void updateLeafSet(Node n){
    	if(this.smallerThanForKeys(this.node_id, n.node_id)){		//if my key less than the other node
    		System.out.println(this.node_id+": inside update Leaf Set my key smaller than: "+n.node_id);
    		this.r_lset.add(n);		//add Z or n to right/bigger leaf set
    	}else{
    		System.out.println(this.node_id+": inside update Leaf Set my key bigger than "+n.node_id);
    		this.l_lset.add(n);		//add Z to leaf set
    	}
		for(int i=0;i<n.l_lset.size();i++){
			if(this.l_lset.size()>=L/2){
				break;
			}
			this.l_lset.add(n.l_lset.get(i));	//update X left leaf set
			System.out.println(this.node_id+": added to left leaf set: "+n.node_id);
		}
		for(int i=0;i<n.r_lset.size();i++){
			if(this.r_lset.size()>=L/2){
				break;
			}
			this.r_lset.add(n.r_lset.get(i));	//update X right leaf set
			System.out.println(this.node_id+": added to right leaf set: "+n.node_id);
		}
		if(this.l_lset.isEmpty()){
			this.l_lset.add(n);
		}
		if(this.r_lset.isEmpty()){
			this.r_lset.add(n);
		}
//		if(this.l_lset.isEmpty() && !this.r_lset.isEmpty()){
//			Node biggerN = this.r_lset.lastElement();
//			
//			Node bigBiggerN = biggerN.
//		}
    }

    public double dist_phy(Node a,Node b){
    	// todo
    	return 0;
    }
    
    public void updateTables(Node n){
    	int l = sharedPrefix(this.node_id,n.node_id);
    	System.out.println(this.node_id+": updating tables with node:"+n.node_id+" prefix:"+l);
    	for(int i=0;i<l+1 && i<rows;i++){
    		for(int j=0;j<base;j++){
    			if(this.r_table[i][j]==null){
    				this.r_table[i][j] = n.r_table[i][j];
    			}else if(n.r_table[i][j]!=null && dist_phy(this,this.r_table[i][j])>dist_phy(this,n.r_table[i][j])){
    				this.r_table[i][j] = n.r_table[i][j];
    			}
    		}
    	}
//    	for(int j=0;j<base;j++){
//    		this.r_table[l][j] = n.r_table[l][j];
//    	}
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
    
    public void updateNeighbourSet(Node n){
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
//        System.out.println("key/b: "+key_size/Node.b);
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
//        System.out.println("x:"+x+"  y:"+y+"  l:"+len);
        return len;
    }
    
    // returns true if this is the last node else false
    public boolean route(Message msg){
        //check leaf set
    	long smallest=0,greatest;
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
    	System.out.println(this.node_id+ ": left neigh:"+smallest+" right_neigh:"+greatest);
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
                // handleMsg();
                System.out.println("Notification1:- Node: "+str_node_id+" received a msg of type: "+msg.type +" from: "+msg.srcNode.node_id);
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
	    System.out.println(this.node_id+": str_node_id:"+this.str_node_id+" msg_id:"+msg.str_key+" prefix:"+l);
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
        	System.out.println(this.node_id+": msg:+"+msg.key +" Forwarded to:"+this.r_table[min_idx/rows][min_idx%rows].node_id);
            r_table[min_idx/rows][min_idx%rows].addMessage(msg);
            return false;
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
    	System.out.println("Node id: "+n.node_id+" ("+n.str_node_id+")");
    	System.out.println("Leaf Set");
    	for(int i=0;i<n.l_lset.size();i++){
    		if(n.l_lset.get(i)!=null){
    			System.out.print(n.l_lset.get(i).node_id+" ("+n.l_lset.get(i).str_node_id+"), ");
    		}else{
    			System.out.print("null, ");
    		}
    	}
    	System.out.print(" || ");
    	for(int i=0;i<n.r_lset.size();i++){
    		if(n.r_lset.get(i)!=null){
    			System.out.print(n.r_lset.get(i).node_id+" ("+n.r_lset.get(i).str_node_id+"), ");
    		}else{
    			System.out.print("null, ");
    		}
    	}
    	System.out.println();
    	
    	System.out.println("Neigbourhood Set");
    	for(int i=0;i<n.n_set.size();i++){
    		if(n.n_set.get(i)!=null){
    			System.out.print(n.n_set.get(i).node_id+" ("+n.n_set.get(i).str_node_id+"), ");
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
    				System.out.print(n.r_table[i][j].node_id+" ("+n.r_table[i][j].str_node_id+"), \t");
    			}else{
        			System.out.print("null value, \t");
        		}
    		}
    		System.out.println();
    	}
    	System.out.println("**********************************************************************");
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


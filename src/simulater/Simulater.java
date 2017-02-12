package simulater;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Random;
import java.util.Vector;
import java.util.UUID;
import dht1.KeyGenerator;
import dht1.Message;
import dht1.Node;
import dht1.Pair;

public class Simulater {
	Vector <Node> n_list = new Vector<Node>();
	boolean override = false;		//to decide and override the commands
	int max_nodes=64;				//max allowed nodes in the network
	int min_nodes=2;				//min nodes in the network to prevent deleting
	boolean auto=false;				// todo  add functions like auto csimulate vs user input
	
	public void simulate() throws Exception{
		Thread curr_thread = Thread.currentThread();
		Random random = new Random();
		float r,r1,frac1,frac2;

		String line=null;
		byte[] inp_bytes = new byte[2048];
		int num_bytes;
		File file = new File("node_vs_hops.txt");
        if (!file.exists()) {
             file.createNewFile();
        }
        FileWriter fw = new FileWriter(file.getAbsoluteFile());//, true);
        BufferedWriter bw = new BufferedWriter(fw);
        Node.bw = bw;
		
		while (true){
			if(System.in.available()>0){
				num_bytes = System.in.read(inp_bytes, 0, 1024);
				if (num_bytes < 0){
					System.out.println("Simulator: Panic input reading. Numbytes: "+num_bytes);
				}else{
					if(line==null){
						line = new String(inp_bytes, 0, num_bytes);
					}else{
						line += new String(inp_bytes, 0, num_bytes);
					}
					if (line != null && line.length() > 0 && line.charAt(line.length()-1)=='\n') {
						line = line.substring(0, line.length()-1);
						System.out.println("Simulator: User entered: "+line);
						if("end".equals(line)){
				    		System.out.println("Simulator: Ending Simulations");
				    		bw.close();
				    		System.exit(0);
				    	}
						line = null;
					}
			     }
			}else{								//otherwise randomly do something
				r = random.nextFloat();
				//at #nodes = max/2 => frac1 = frac2-frac1 = 1/3;
				frac1 = (float)(max_nodes-n_list.size())/(float)(3*max_nodes/2);
				frac2 = (float)(n_list.size())/(float)(3*max_nodes/2);
//				System.out.println("Simulator: addNode:"+frac1);
//				System.out.println("Simulator: deleteNode:"+frac2);
//				System.out.println("Simulator: lookupKey:"+frac2);
				if(r<frac1){
					//add a new node
					addNode();
					Node.incrementNodes();
					curr_thread.sleep(500);
					for(int i=0;i<n_list.size();i++){
						if(n_list.get(i) == null){continue;}
						n_list.get(i).printNodeState(n_list.get(i));
					}
				}else if(r<frac2+frac1){
					//delete a node
					deleteNode();
					Node.decrementNodes();
				}else{
//				}else if(r<2*frac2+ frac1){
					//lookup add or delete a key
					r1 = random.nextFloat();
					if(r1<=0.7){
						lookUp();
					}else if(r1<=0.95){
						addKey();
					}else{
						deleteKey();
					}
				}
			}
		}

	}
	
	/**
	 * 1. Starting point.
	 * 2. Starts a simulator instance
	 * */
	public static void main(String[] args) throws Exception{		
		Simulater s = new Simulater();
		System.out.println("Starting Simulator");
		s.simulate();
	}

	/**
	 * 1. Generates a random public addr (a 2-tuple)
	 * 2. Adds node to the network
	 * */
	public void addNode(){
		if(n_list.size()>=max_nodes){
			System.out.println("Simulater: AddNode Panic. Size over: "+max_nodes+".");
			return;
		}
		Random random = new Random();
		Pair<Integer,Integer> public_addr = new Pair<Integer,Integer>(random.nextInt(Node.addr_range+1),random.nextInt(Node.addr_range+1));
		Node tmp_node = new Node();
		
		//set node public ip addr; set a known node. 
		tmp_node.public_addr = public_addr;
		if(n_list.size()<=0){
			//first node
			tmp_node.known = null;
		}else{
			//set a random known node
			tmp_node.known = n_list.get(random.nextInt(n_list.size()));
		}
		System.out.println("Simulater: Adding a node : "+tmp_node.node_id+" "+public_addr.toString());
		n_list.add(tmp_node);
		Thread t = new Thread(tmp_node,public_addr.toString());
		t.start();
		return;
	}

	/**
	 * 1. Selects a random node
	 * 2. Asks that node to delete itself
	 * */
	public void deleteNode(){
		if(n_list.size()<=min_nodes){
			System.out.println("Simulater: DeleteNode Panic. Size already: "+min_nodes+".");
			return;
		}
		Random random = new Random();
		int idx = random.nextInt(n_list.size());
		Node tmp_node = n_list.get(idx);
		System.out.println("Simulater: Deleting node: "+tmp_node.node_id+" "+tmp_node.public_addr.toString());
		tmp_node.self_delete = true;
		n_list.remove(idx);
	}
	
	/**
	 * 1. Generates a random key to lookup. 
	 * 2. Selects a random node that wants to lookup the key
	 * 3. Asks node to lookup the key.
	 * */
	public void lookUp(){
		if(n_list.size()<=0){
			System.out.println("Simulator: Lookup Panic. Size of network 0");
			return;
		}
		Random random = new Random();
		//generating random key to lookup
		byte[] key_bytes = KeyGenerator.generateRandomID(Node.key_size/8);		//size in bytes
		long key = KeyGenerator.convertBytesToInt(key_bytes);
		
		//selecting random node that wishes to lookup
		Node tmp_node = n_list.get(random.nextInt(n_list.size()));
		System.out.println("Simulator: Node: "+tmp_node.node_id+" "+tmp_node.public_addr.toString()+" looking up key: "+key);
		Message m = new Message("lookup",-1,tmp_node,key);
		tmp_node.addMessage(m);
//		tmp_node.lookup_key = key;
//		tmp_node.lookup = true;
	}
	
	/**
	 * 1.Selects a random node that wants to add a key:value pair
	 * 2.Selects a random key,value pair to be added
	 * 3.Simulates the adding of the key:value pair in the network 
	 * */
	public void addKey(){
		if(n_list.size()<=0){
			System.out.println("Simulator: AddKey Panic. Size of network 0");
			return;
		}
		Random random = new Random();
		//generating random key and value to addkey
		byte[] key_bytes = KeyGenerator.generateRandomID(Node.key_size/8);		//size in bytes
		long key = KeyGenerator.convertBytesToInt(key_bytes);
		String value = UUID.randomUUID().toString();

		//selecting random node that wishes to addkey
		Node tmp_node = n_list.get(random.nextInt(n_list.size()));
		System.out.println("Simulator: Node: "+tmp_node.node_id+" "+tmp_node.public_addr.toString()+" adding key,value: ("+key+","+value+")");
		Message m = new Message("add_key",-1,tmp_node,key,value);
		tmp_node.addMessage(m);
		return;
	}
	
	/**
	 * 1. select a random key to remove mapping of
	 * 2. select a random node that decides to remove that mapping
	 * 3. simulate the removing of the key mapping if present.
	 * */
	public void deleteKey(){
		if(n_list.size()<=0){
			System.out.println("Simulator: AddKey Panic. Size of network 0");
			return;
		}
		Random random = new Random();
		//generating random key and value to remove
		byte[] key_bytes = KeyGenerator.generateRandomID(Node.key_size/8);		//size in bytes
		long key = KeyGenerator.convertBytesToInt(key_bytes);
//		String value = UUID.randomUUID().toString();

		//selecting random node that wishes to remove the mapping
		Node tmp_node = n_list.get(random.nextInt(n_list.size()));
		System.out.println("Simulator: Node: "+tmp_node.node_id+" "+tmp_node.public_addr.toString()+" removing key: "+key);
		Message m = new Message("remove_key",-1,tmp_node,key);
		tmp_node.addMessage(m);
		return;
	}
}

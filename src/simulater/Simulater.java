package simulater;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.Scanner;
import java.util.Vector;

import dht1.KeyGenerator;
import dht1.Node;
import dht1.Pair;

public class Simulater {
	Vector <Node> n_list = new Vector<Node>();
	int addr_range = 100;			//public addr 2-D sq. array dimensions
	boolean override = false;		//to decide and override the commands
	int max_nodes=16;				//max allowed nodes in the network
	int min_nodes=2;				//min nodes in the network to prevent deleting
	boolean auto=false;				// todo  add functions like auto csimulate vs user input
	
	public void simulate(){
		Thread t = Thread.currentThread();
		Random random = new Random();
		float r,frac1,frac2;
//		Scanner input = new Scanner(System.in);
		BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
		String s = null,line;
		int tp;
		while (true){
//			System.in.read;
			// handling if user wants something
			System.out.println("yo man");
//			s = input.nextLine();
			try {
				while(true){
					if((tp = br.read()) != -1 ){
						System.out.println("typed: "+tp);
					}else{
						System.out.println("food");
					}
					System.out.println("food");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if( !s.equals("\\n") ){
				System.out.println("Simulater: User entered: "+s);
				if("end".equals(s)){
					System.out.println("Simulater: Ending Simulations");
//					input.close();
					break;
				}
			}else{	//otherwise randomly do something
				System.out.println("heere");
				r = random.nextFloat();
				//at #nodes = max/2 => frac1 = frac2-frac1 = 1/3;
				frac1 = (float)(max_nodes-n_list.size())/(float)(3*max_nodes/2);
				frac2 = (float)(n_list.size())/(float)(3*max_nodes/2) + frac1;
				if(r<frac1){
					//add a new node
					addNode();
				}else if(r<frac2){
					//delete a node
					deleteNode();
				}else{
					//lookup something
					lookUp();
				}
			}
		}

	}
	
	/*
	 * 1. Starting point.
	 * 2. Starts a simulator instance
	 * */
	public static void main(String[] args) throws Exception{		
		Simulater s = new Simulater();
		System.out.println("Starting Simulator");
		s.simulate();
	}

	/*
	 * 1. Generates a random public addr (a 2-tuple)
	 * 2. Adds node to the network
	 * */
	public void addNode(){
		if(n_list.size()>=max_nodes){
			System.out.println("Simulater: AddNode Panic. Size over: "+max_nodes+".");
			return;
		}
		Random random = new Random();
		Pair<Integer,Integer> public_addr = new Pair<Integer,Integer>(random.nextInt(addr_range+1),random.nextInt(addr_range+1));;
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
		System.out.println("Simulater: Adding a node : "+public_addr.toString());
		n_list.add(tmp_node);
		Thread t = new Thread(tmp_node,public_addr.toString());
		t.start();
		return;
	}

	/*
	 * 1. Selects a random node
	 * 2. Asks that node to delete itself
	 * */
	public void deleteNode(){
		if(n_list.size()<=min_nodes){
			System.out.println("Simulater: DeleteNode Panic. Size already: "+min_nodes+".");
			return;
		}
		Random random = new Random();
		Node tmp_node = n_list.get(random.nextInt(n_list.size()));
		System.out.println("Simulater: Deleting node: "+tmp_node.public_addr.toString());
		tmp_node.self_delete = true;
	}
	
	/*
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
		System.out.println("Simulator: Node: "+tmp_node.public_addr.toString()+" looking up key: "+key);
		tmp_node.lookup_key = key;
		tmp_node.lookup = true;
	}
}

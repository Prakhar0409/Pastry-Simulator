package simulater;

import java.util.Random;
import java.util.Scanner;
import java.util.Vector;

import dht1.Node;
import dht1.Pair;

public class Simulater {
	Vector <Node> n_list = new Vector<Node>();
	int addr_range = 100;			//public addr 2-D sq. array dimensions
	boolean override = false;		//to decide and override the commands
	int max_nodes=16;				//max allowed nodes in the network
	int min_nodes=2;				//min nodes in the network to prevent deleting
	
	public void simulate(){
		Thread t = Thread.currentThread();
		Random random = new Random();
		Float r;
		Scanner input = new Scanner(System.in);
		String s = null;
		while (true){
			// handling if user wants something
			s = input.nextLine();
			if( !s.equals("\\n") ){
				System.out.println("Simulater: User entered: "+s);
				if("end".equals(s)){
					System.out.println("Simulater: Ending Simulations");
					input.close();
					break;
				}
			}else{	//otherwise randomly do something
				r = random.nextFloat();
				if(r<0.33){
					//add a new node
					addNode();
					
				}else if(r<0.66){
					//delete a node
					deleteNode();
				}else{
					//lookup something
				}
			}
		}

	}
	
	public static void main(String[] args) throws Exception{		
		Simulater s = new Simulater();
		s.simulate();
	}

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

	public void deleteNode(){
		if(n_list.size()<=min_nodes){
			System.out.println("Simulater: DeleteNode Panic. Size already: "+min_nodes+".");
			return;
		}
		Random random = new Random();
//		Node tmp_node = n_list.get()
	}
}

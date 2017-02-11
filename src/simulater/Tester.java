package simulater;

import java.util.Vector;

import dht1.*;

public class Tester {
	public static void main(String[] args) throws Exception{
		System.out.println("Started Tester");
		Tester test = new Tester();
		Thread t = Thread.currentThread();
		
		Vector<Node> nList = new Vector<Node>();
		Node n = new Node(3329);
		test.init(n);
		t.sleep(500);
		n.printNodeState(n);
				
		int iter=2;
		t.sleep(1000);
		System.out.println("\n\n\n\n");
		System.out.println("Nodes: 1");
		Node n1 = new Node(38798);
		n1.known = n;
//		n1.public_addr = "10.208.1.99";
		test.addNode(n1);
		t.sleep(500);
		n.printNodeState(n);
//		t.sleep(500);
		n.printNodeState(n1);
		
		t.sleep(1000);
		n1.self_delete = true;
		n1 = new Node();
		t.sleep(1000);
		System.out.println("Deleted");
		n.printNodeState(n);
		
//		t.sleep(1000);
//		System.out.println("\n\n\n\n");
//		System.out.println("Nodes: 2");
//		Node n2 = new Node(3330);
//		n2.known = n;
////		n2.public_addr = "3.72.93.2";
//		test.addNode(n2);
//		t.sleep(500);
//		n.printNodeState(n);
//		n.printNodeState(n1);
//		n.printNodeState(n2);
		while(true);
	}
	
	public void addNode(Node n){
		Thread t = new Thread(n);
		t.start();
	}
	
	public void init(Node n){
		n.known=null;
		Thread t = new Thread(n,"192.168.2.0");
		t.start();
	}
}
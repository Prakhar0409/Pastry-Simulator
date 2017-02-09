package simulater;

import dht1.*;

public class Tester {
	public static void main(String[] args) throws Exception{
		System.out.println("Started Tester");
		Tester test = new Tester();
		Thread t = Thread.currentThread();
		
		Node n = new Node();
		test.init(n);
				
		int iter=2;
		t.sleep(5000);
		System.out.println("Nodes: 1");
		Node n1 = new Node();
		n1.known = n;
		n1.public_addr = "10.208.1.99";
		test.addNode(n1);
		
		while(true);
	}
	
	public void addNode(Node n){
		Thread t = new Thread(n,n.public_addr);
		t.start();
	}
	
	public void init(Node n){
		n.known=null;
		Thread t = new Thread(n,"192.168.2.0");
		t.start();
	}
}
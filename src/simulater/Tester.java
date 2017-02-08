package simulater;

import dht1.*;

public class Tester {
	public static void main(String[] args) throws Exception{
		System.out.println("Started Tester");
		Tester test = new Tester();
		test.init();
//		while(true);
		Thread t = Thread.currentThread();
		t.sleep(10000);	//sleep for 10 secs
		
		Node n = new Node();
		
		
	}
	
	public void addNode(){
		Node n = new Node(1);
	}
	
	public void init(){
		Node n = new Node();
		n.known=null;
		Thread t = new Thread(n,"192.168.2.0");
		t.start();
	}
}

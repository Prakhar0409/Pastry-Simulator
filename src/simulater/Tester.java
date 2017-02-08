package simulater;

import dht1.*;

public class Tester {
	public static void main(String[] args){
		System.out.println("Started Tester");
		Tester test = new Tester();
		test.init();
		while(true);
		
	}
	
	public void addNode(){
		Node n = new Node(1);
	}
	
	public void init(){
		Node n = new Node(1);
		Thread t = new Thread(n,"192.168.2.0");
		t.start();
	}
}

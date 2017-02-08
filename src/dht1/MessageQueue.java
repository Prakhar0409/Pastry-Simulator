package dht1;

import java.util.Vector;

public class MessageQueue<T> {
    
    private int capacity;//Number of messages that can be stored in the queue.

    private Vector<T> queue = new Vector<T>();//The queue for receiving all incoming messages.

    /**
     * Constructor, initializes the queue.
     * 
     * @param capacity The number of messages allowed in the queue.
     */
    public MessageQueue(int capacity) {
        this.capacity = capacity;
    }

    
    public synchronized void send(T message) {
        //TODO check
    }
	
    public synchronized T receive() {
        //TODO check
        
//    	return 0;
    	return null;
    }
 
    private boolean isEmpty() {
        return this.queue.size() == 0;
    }
    
    public synchronized void add(T msg){
    	if(this.queue.size()<this.capacity){
    		this.queue.addElement(msg);
    	}
    }
    
    public synchronized T getMessage(){
    	T msg = null;
    	if(!this.isEmpty()){
    		msg = this.queue.firstElement();
    		this.queue.remove(0);
    	}
    	return msg;
    }
    
    public synchronized T poll(){
    	if(this.queue.isEmpty()){
    		return null;
    	}
    	T msg = queue.firstElement();
    	this.queue.remove(0);
    	return msg;
    }
}

package dht1;

import java.util.*;

/**
* Implementation of a local storage
*/

public class Storage<E>{
	 
	 /**
	 * The actual storage of values that this node is responsible for.
	 */
	 
	 private HashMap<Long, E> storage ;
	 
	 /**
	 * Constructor to initiate a Storage on  node.
	 */
	 
	 public Storage(){
		 storage = new HashMap<>();
	 }
	 
	 /**
	 * Local addition of key
	 * @param key
	 * @param value
	 */
	 
	 public void add(long key, E value){
		 storage.put(key, value);
	 }
	 
	 /**
	 * Local search for key
	 * @param key
	 * @return the item paired with the given key.
	 */
	 
	 public  E get(long key){
		 return storage.get(key);
	 }
	 
	 /**
	 * Local remove the key
	 * @param key
	 */
	 
	 public E remove(long key){
		return storage.remove(key);
	}
	
	public void modify(long key, E value){
		//
	}
	/**
	 * Getting all values contained in this node's storage.
	 * @return  a list of all values 
	 */
	
	public List<E> getValues(){
		ArrayList<E> values = new ArrayList<>(storage.values());
		return values;
	}
	
	public ArrayList<Long> getKeys(){
		ArrayList<Long> keys = new ArrayList<Long>(storage.keySet());
		return keys;
	}
	
	public HashMap<Long,E> getMap(){
		return storage;
	}
}
	

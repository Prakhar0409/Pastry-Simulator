package dht1;

public class Pair<L,R> {

	  public L left;
	  public R right;

	  public Pair() {
		    this.left = null;
		    this.right = null;
	  }
	  
	  public Pair(L left, R right) {
	    this.left = left;
	    this.right = right;
	  }

	  public L getLeft() { return left; }
	  public R getRight() { return right; }

	  @Override
	  public int hashCode() { return left.hashCode() ^ right.hashCode(); }

	  @Override
	  public boolean equals(Object o) {
	    if (!(o instanceof Pair)) return false;
	    Pair pairo = (Pair) o;
	    return this.left.equals(pairo.getLeft()) &&
	           this.right.equals(pairo.getRight());
	  }
	  
	  public String toString(Pair<L,R> p){
		  String s="(";
		  s = p.getLeft().toString();
		  s+=",";
		  s += p.getRight().toString();
		  s+=")";
		  return s;
	  }

}
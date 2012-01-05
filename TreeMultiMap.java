import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;


public class TreeMultiMap<K, V>
{
	
	private SortedMap<K,HashSet<V>> tree;
	private int numElements = 0;
	private boolean bMaximum;
	
	public static class Entry<K,V> implements Comparable<Entry<K,V>> 
	{
		K myKey;
		V myValue;
		
		public Entry(K key, V value) {
			myKey = key;
			myValue = value;
		}
		
		public K getKey() {
			return myKey;
		}
		public V getValue() {
			return myValue;
		}
		private static boolean equals(Object x, Object y) {
		    return (x == null && y == null) || (x != null && x.equals(y));
		  }
		@SuppressWarnings("unchecked")
		public boolean equals(Object other) {
		     return
		      other instanceof TreeMultiMap.Entry &&
		      equals(myKey, ((TreeMultiMap.Entry<K,V>)other).getKey()) &&
		      equals(myValue, ((TreeMultiMap.Entry<K,V>)other).getValue());
		  }

		@SuppressWarnings("unchecked")
		@Override
		public int compareTo(Entry<K, V> o) {
			// TODO Auto-generated method stub
			return ((Comparable<K>)myKey).compareTo(o.getKey());
		}
	}

	public TreeMultiMap(boolean bMaximum){
		this.bMaximum = bMaximum;
		
		if(bMaximum) {
			tree = new TreeMap<K,HashSet<V>>(Collections.reverseOrder());
		}
		else {
			tree = new TreeMap<K,HashSet<V>>();
		}
	}

	public TreeMultiMap(){
		this(false);
	}

	public void clear() {
		numElements = 0;
		tree.clear();
		
	}

	public boolean containsKey(Object key) {
		return tree.containsKey(key);
	}

	public boolean containsValue(Object value) {
		// TODO Auto-generated method stub
		for(HashSet<V> l: tree.values()){
			if(l.contains(value)){
				return true;
			}
		}
		return false;
	}

	public List<TreeMultiMap.Entry<K, V>> entryList() {
		// TODO Auto-generated method stub
		List<TreeMultiMap.Entry<K, V>> s = new LinkedList<TreeMultiMap.Entry<K, V>>();
		
		for(Map.Entry<K,HashSet<V>> tmp: tree.entrySet()){
			for(V l:tmp.getValue()){
				s.add(new TreeMultiMap.Entry<K,V>(tmp.getKey(), l));
			}
		}
		
		return s;
	}

	public K firstKey() {
		if(!tree.isEmpty()){
			return tree.firstKey();
		}
		return null;
	}

	public K lastKey() {
		if(!tree.isEmpty()){
			return tree.lastKey();
		}
		return null;
	}

	public Set<V> get(Object key) {
		if(tree.containsKey(key)){
			return tree.get(key);
		}
		return null;
	}

	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return numElements == 0;
	}

	public Set<K> keySet() {
		// TODO Auto-generated method stub
		return tree.keySet();
	}

	public V put(K key, V value) {
		// TODO Auto-generated method stub
		
		if(!tree.containsKey(key)){
			tree.put(key, new HashSet<V>());
		}
		if(!tree.get(key).contains(value)){
			numElements++;
		}
		tree.get(key).add(value);
		
		return value;
	}

	public void putAll(Map<? extends K, ? extends V> m) {
		// TODO Auto-generated method stub
		for(Map.Entry<? extends K, ? extends V> e:m.entrySet()){
			this.put(e.getKey(), e.getValue());
			numElements++;
		}
	}

	public Set<V> removeKey(Object key) {
		// TODO Auto-generated method stub
		if(tree.containsKey(key)){
			numElements = numElements - tree.get(key).size();
		}
		return tree.remove(key);
	}
	
	public void removeKeyValue(K key, V value){
		if(tree.containsKey(key)){
			Set<V> out = tree.get(key);
			if(out.contains(value)){
				out.remove(value);
				numElements--;
			}
		}
	}
	
	public void removeValue(V value){
		// LINEAR TIME :(
		for(Set<V> s:tree.values()){
			if(s.contains(value)){
				s.remove(value);
				numElements--;
			}
		}
	}

	public int size() {
		// TODO Auto-generated method stub
		return numElements;
	}

	public Collection<V> values() {
		// TODO Auto-generated method stub
		Set<V> out = new HashSet<V>();
		for(HashSet<V> l:tree.values()){
			for(V v: l){
				out.add(v);
			}
		}
		return out;
	}

	/**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
    	String out = "";

		for(Map.Entry<K,HashSet<V>> tmp: tree.entrySet()){
			out += tmp.getKey().toString() + ": ";
			for(V l:tmp.getValue()){
				out += l.toString() + " | ";
			}
			out += "\n";
		}

		
		return out;
    }
	
	public static void main(String[] args) throws IOException {
		TreeMultiMap<Integer,Integer> testTreeMap = new TreeMultiMap<Integer,Integer>();

		for(int i = 999; i>=0; i--){
			testTreeMap.put(Integer.toString(i).length(), i);
			if(testTreeMap.size() != 1000-i){
				System.out.println("ERROR, NUMBER OF ENTRIES SHOULD BE " + (1000-i) + " BUT IT SAYS " + testTreeMap.size());
			}
		}

		testTreeMap.removeKey(1);
		if(testTreeMap.size() != 990){
			System.out.println("ERROR, NUMBER OF ENTRIES SHOULD BE 990");
		}
		int getMe = Integer.parseInt(testTreeMap.get(2).toArray()[0].toString());

		if(getMe != 10){
			System.out.println("ERROR First 2 digit number is 10!");
		}
		testTreeMap.put(1,5);

		if(testTreeMap.size() != 991){
			System.out.println("ERROR SIZE WRONG AFTER ADDING");
		}
		if(testTreeMap.get(1).size() != 1){
			System.out.println("ERROR SET SIZE WRONG AFTER ADDING");
		}
		testTreeMap.put(1,5);
		if(testTreeMap.size() != 991){
			System.out.println("Tried to add 5 twice, it shouldn't have allowed me " + testTreeMap.size());
		}
		if(testTreeMap.get(1).size() != 1){
			System.out.println("Tried to add 5 twice, it shouldn't have allowed me");
		}
		if(testTreeMap.get(2).size() != 90){
			System.out.println("Wrong number of 2 digit guys... it gave back " + testTreeMap.get(2).size());
		}

		testTreeMap.removeValue(765);
		if(testTreeMap.get(3).size() != 899){
			System.out.println("Wrong number of 3 digit guys... it gave back " + testTreeMap.get(3).size());
		}
		testTreeMap.removeValue(765);
		if(testTreeMap.get(3).size() != 899){
			System.out.println("Wrong number of 3 digit guys after duplicate delete... it gave back " + testTreeMap.get(3).size());
		}
		testTreeMap.removeKeyValue(3, 453);
		if(testTreeMap.get(3).size() != 898){
			System.out.println("Wrong number of 3 digit guys after duplicate delete... it gave back " + testTreeMap.get(3).size());
		}

		testTreeMap.removeKeyValue(3, 453);
		if(testTreeMap.get(3).size() != 898){
			System.out.println("Wrong number of 3 digit guys after duplicate delete... it gave back " + testTreeMap.get(3).size());
		}

		if(testTreeMap.size() != 989){
			System.out.println("WRONG SIZE... should be 989, it says " + testTreeMap.size());
		}

		int count = 0;
		for(Integer i:testTreeMap.values()){
			System.out.print(i + " ");
			count++;
		}
		if(count != 989){
			System.out.println("LOOPING THROUGH VALUES WRONG... " + count + " instead of 989");
		}
		count = 0;

		for(TreeMultiMap.Entry<Integer, Integer> e: testTreeMap.entryList()){
			count++;
		}

		if(count != 989){
			System.out.println("LOOPING THROUGH ENTRYSET WRONG... " + count + " instead of 989");
		}

		count = 0;
		for(Iterator<TreeMultiMap.Entry<Integer, Integer>> i = testTreeMap.entryList().iterator(); i.hasNext();){
			count++;

			TreeMultiMap.Entry<Integer, Integer> next = i.next();
			if(next.getValue() == 342){
				testTreeMap.removeKeyValue(next.getKey(), next.getValue());
			}

		}
		if(count != 989){
			System.out.println("LOOPING THROUGH Iterator WRONG... " + count + " instead of 989");
		}

		if(testTreeMap.size() != 988){

			System.out.println("Size wrong at end check, should be " + 998 + " but returned " + testTreeMap.size());
		}

		count = 0;
		for(TreeMultiMap.Entry<Integer, Integer> e: testTreeMap.entryList()){
			count++;
		}
		if(testTreeMap.size() != 988){

			System.out.println("Count wrong in manual end check check, should be " + 988 + " but returned " + testTreeMap.size());
		}

		testTreeMap.clear();

		if(testTreeMap.size() != 0){
			System.out.println("clearing didn't work");
		}
	}   
}

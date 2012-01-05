public class Pair<A, B> {
 
  public A first;
  public B second;
 
  public Pair(A first, B second) {
    this.first = first;
    this.second = second;
  }
 
  public A getFirst() { return first; }
  public B getSecond() { return second; }
 
  public void setFirst(A v) { first = v; }
  public void setSecond(B v) { second = v; }
 
  public String toString() {
    return "Pair[" + first + "," + second + "]";
  }
 
  private static boolean equals(Object x, Object y) {
    return (x == null && y == null) || (x != null && x.equals(y));
  }
 
  public boolean equals(Object other) {
     return
      other instanceof Pair &&
      equals(first, ((Pair)other).first) &&
      equals(second, ((Pair)other).second);
  }
 
  public int hashCode() {
    if (first == null) return (second == null) ? 0 : second.hashCode() + 1;
    else if (second == null) return first.hashCode() + 2;
    else return first.hashCode() * 17 + second.hashCode();
  }
 
  public static <A,B> Pair<A,B> of(A a, B b) {
    return new Pair<A,B>(a,b);
  }
}

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a direction in which to move an ant.
 */
public enum Aim 
{
    /** North direction, or up. */
    NORTH(-1, 0, 'n'),
    
    /** East direction or right. */
    EAST(0, 1, 'e'),
    
    /** South direction or down. */
    SOUTH(1, 0, 's'),
    
    /** West direction or left. */
    WEST(0, -1, 'w');
    
    private static final Map<Character, Aim> symbolLookup = new HashMap<Character, Aim>();
    
    public static final Aim getOpposite(Aim a){
    	
    	// REALLY HACKY BUT I'M TIRED
    	if(a == Aim.NORTH){
    		return Aim.SOUTH;
    	} else if(a == Aim.SOUTH){
    		return Aim.NORTH;
    	} else if(a == Aim.WEST){
    		return Aim.EAST;
    	} else if(a == Aim.EAST){
    		return Aim.WEST;
    	}
    	return Aim.SOUTH;
    }

    public static final List<Aim> getPerpendiculars(Aim a){
    	List<Aim> aimList = new ArrayList<Aim>();
    	// REALLY HACKY BUT I'M TIRED
    	if(a == Aim.NORTH){
    		aimList.add(WEST);
    		aimList.add(EAST);
    	} else if(a == Aim.SOUTH){
    		aimList.add(WEST);
    		aimList.add(EAST);
    	} else if(a == Aim.WEST){
    		aimList.add(NORTH);
    		aimList.add(SOUTH);
    	} else if(a == Aim.EAST){
    		aimList.add(NORTH);
    		aimList.add(SOUTH);
    	}
    	return aimList;
    }
    
    static {
        symbolLookup.put('n', NORTH);
        symbolLookup.put('e', EAST);
        symbolLookup.put('s', SOUTH);
        symbolLookup.put('w', WEST);
    }
    
    private final int rowDelta;
    
    private final int colDelta;
    
    private final char symbol;
    
    Aim(int rowDelta, int colDelta, char symbol) {
        this.rowDelta = rowDelta;
        this.colDelta = colDelta;
        this.symbol = symbol;
    }
    
    /**
     * Returns rows delta.
     * 
     * @return rows delta.
     */
    public int getRowDelta() {
        return rowDelta;
    }
    
    /**
     * Returns columns delta.
     * 
     * @return columns delta.
     */
    public int getColDelta() {
        return colDelta;
    }
    
    /**
     * Returns symbol associated with this direction.
     * 
     * @return symbol associated with this direction.
     */
    public char getSymbol() {
        return symbol;
    }
    
    /**
     * Returns direction associated with specified symbol.
     * 
     * @param symbol <code>n</code>, <code>e</code>, <code>s</code> or <code>w</code> character
     * 
     * @return direction associated with specified symbol
     */
    public static Aim fromSymbol(char symbol) {
        return symbolLookup.get(symbol);
    }
}

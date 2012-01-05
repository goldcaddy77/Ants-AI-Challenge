import java.util.EnumMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a direction in which to move an ant.
 */
public enum Aim 
{
    NORTH(-1, 0, 'n'),
    EAST(0, 1, 'e'),
    SOUTH(1, 0, 's'),
    WEST(0, -1, 'w');
    
	private static final Map<Aim, Aim> rightLookup = new EnumMap<Aim, Aim>(Aim.class);
	private static final Map<Aim, Aim> leftLookup = new EnumMap<Aim, Aim>(Aim.class);
	private static final Map<Aim, Aim> behindLookup = new EnumMap<Aim, Aim>(Aim.class);
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

    public final BattleResolution.direction getBRDistroIndex(){
    	if(this == Aim.NORTH){
    		return BattleResolution.direction.N;
    	} else if(this == Aim.SOUTH){
    		return BattleResolution.direction.S;
    	} else if(this == Aim.EAST){
    		return BattleResolution.direction.E;
    	} else if(this == Aim.WEST){
    		return BattleResolution.direction.W;
    	}
    	// We should never get here, but seemed like a decent default this late
    	return BattleResolution.direction.X;
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
		rightLookup.put(NORTH, EAST);
		rightLookup.put(EAST, SOUTH);
		rightLookup.put(SOUTH, WEST);
		rightLookup.put(WEST, NORTH);
		leftLookup.put(NORTH, WEST);
		leftLookup.put(WEST, SOUTH);
		leftLookup.put(SOUTH, EAST);
		leftLookup.put(EAST, NORTH);
		behindLookup.put(NORTH, SOUTH);
		behindLookup.put(SOUTH, NORTH);
		behindLookup.put(EAST, WEST);
		behindLookup.put(WEST, EAST);
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
    
	public Aim left() {
		return leftLookup.get(this);
	}

	public Aim right() {
		return rightLookup.get(this);
	}

	public Aim behind() {
		return behindLookup.get(this);
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Represents an ant on the game board
 */
public class Ant implements Comparable<Ant> {
    private static int AntCounter = 0;
 
    public enum AntType {
    	Gatherer,
    	Hunter,
    	Defender
    }
    
    private Tile tileCurrent;
    private int antID;
    private AntType myType;
    private Path path;
    private int age;
    private Map<Integer, LinkedList<String>> orderLog = new HashMap<Integer, LinkedList<String>>();
           
    /**
     * Creates new {@link Ant} object.
     * 
     * @param tile current tile
     */
    public Ant(int row, int col) {
    	this.setId();
        this.tileCurrent = new Tile(row, col);
    }
    
    public Ant(Tile tile) {
    	this.setId();
        this.tileCurrent = tile;
    }
    
    public int getAge()
    {
    	return age;
    }
    
    public void setPath(Path p) {
    	this.path = p;
    }

    public void clearPath() {
    	this.path = null;
    }

    private void setId() {
        this.antID = Ant.AntCounter;
        Ant.AntCounter+=1;
        // System.out.println("Created ant with id " + antID);
    }
    
    public Tile getCurrentTile() {
        return this.tileCurrent;
    }

    public Path getPath() {
        return this.path;
    }
    
    public boolean hasPath() {
        return !(this.path == null);
    }

    public Tile getNextMove() {
    	if(this.path == null) {
    		return null;
    	}
    	return this.path.start();
    }

    public Tile getDestination() {
    	if(this.path == null) {
    		return null;
    	}
    	else {
    		return this.path.end();
    	}
    }

    public void move(Tile to) {
    	age++;
    	setCurrentTile(to);
    	// if we're currently at the start of our path and we're moving to the 2nd item in our
    	// path, remove the first item (ie take the step)
    	if(this.path != null) {
    		// If this is the last item in the path, set the path to null
    		if(this.path.getSize() == 1) {
    			this.clearPath();
    		}
    		else {
    			this.path.step();
    		}
    	}
    }
    
    public void setCurrentTile(Tile tile) {
    	this.tileCurrent = tile;
    }
    
    /**
	 * @return the myType
	 */
	public AntType getAntType() {
		return myType;
	}

	/**
	 * @param myType the myType to set
	 */
	public void setAntType(AntType myType) {
		this.myType = myType;
	}

	/**
	 * @return the orderLog
	 */
	public LinkedList<String> getOrderLog(int iTurn) {
		return orderLog.get(iTurn);
	}

	/**
	 * @param orderLog the orderLog to set
	 */
	public void addLog(Integer turn, String orderText) {
		if(!orderLog.containsKey(turn)) {
			orderLog.put(turn, new LinkedList<String>());
		}
		orderLog.get(turn).add(" Ant " + this + " with age " + this.age + " was told to do: " + orderText);
	}
	
	/** 
     * {@inheritDoc}
     */
    @	Override
    public int compareTo(Ant o) {
        return hashCode() - o.hashCode();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return antID;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        boolean result = false;
        if (o instanceof Ant) {
            Ant ant = (Ant)o;
            result = antID == ant.hashCode();
        }
        return result;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "#" + antID + " " + this.getCurrentTile();
    }
    
}
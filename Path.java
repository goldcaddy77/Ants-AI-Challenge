import java.util.List;

/**
 * Represents a path from one tile to another.
 */
public class Path {
    private List<Tile> tiles;
    private boolean bComplete;
    private boolean bDiscovered;
    private boolean bObstructed;
    
    public static final int MAX_MAP_SIZE_2 = Game.MAX_MAP_SIZE * Game.MAX_MAP_SIZE;

    public Path(List<Tile> inTiles, boolean bComplete) {
    	this.tiles = inTiles;
    	this.bComplete = bComplete;
    }

    public boolean complete() {
        return bComplete;
    }
    
    public boolean obstructed() {
        return bObstructed;
    }
    
    // Refreshes the path with currently discovered tiles
    public boolean refresh(Game g)
    {
    	if(bComplete) {
    		return true;
    	}
    		
    	boolean bDisc = true;
    	boolean bObst = false;
        for (Tile t : this.tiles) {
        	bDisc = bDisc && g.isDiscovered(t); // complete = all tiles discovered
        	bObst = bObst || !g.getIlk(t).isPassable(); // obstructed = any tile unpassable (water)
        }
        bDiscovered = bDisc;
        bObstructed = bObst;
        bComplete = bDiscovered && !bObstructed;
        return bComplete;
    } 
    
    public void setCompleted() {
        this.bComplete = true;
    }

    public Tile getTile(int index) {
    	if (this.tiles == null) {
    		return null;
    	}
    	
        return this.tiles.get(index);
    }

    public List<Tile> getTiles() {
        return this.tiles;
    }
    
    public int getSize() {
    	if (this.tiles == null) {
    		return 0;
    	}
        return this.tiles.size();
    }

    public Tile start() {
    	if (this.tiles == null) {
    		return null;
    	}
    	return this.tiles.get(0);
    }

    public Tile step() {
    	if (this.tiles == null) {
    		return null;
    	}
    	return this.tiles.remove(0);
    }

    public Tile end() {
    	if (this.tiles == null) {
    		return null;
    	}
        return this.tiles.get(this.tiles.size() - 1);
    }

    @Override
    public boolean equals(Object o) {
        boolean result = false;
        if (o instanceof Path) {
            Path route = (Path)o;
            result = this.start() == route.start() && this.end() == route.end();
        }
        return result;
    }

    @Override
    public String toString() {
        return this.start().toString() + " -> " + this.end().toString();
    }    
}
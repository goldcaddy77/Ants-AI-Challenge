import java.util.HashSet;
import java.util.Set;

/**
 * Represents a tile of the game map.
 */
public class Tile implements Comparable<Tile> {
    private final int row;
    private final int col;
    
    /**
     * Creates new {@link Tile} object.
     * 
     * @param row row index
     * @param col column index
     */
    public Tile(int row, int col) {
        this.row = row;
        this.col = col;
    }
    

	public boolean isNeighbor(Tile tile, AntMap map) {
		Set<Tile> myNeighbors = this.getNeighbors(map);
		for(Tile t : myNeighbors) {
			if(t == tile) {
				return true;
			}
		}
		return false;
	}    

	public Set<Tile> getNeighborsWithDiags(AntMap map) {
		Set<Tile> tiles = new HashSet<Tile>();
		for(Aim aim: Aim.values()) {
			tiles.add(map.getTile(this, aim));
		}
		return tiles;
	}    

	public boolean isNeighborWithDiags(Tile tile, AntMap map) {
		Set<Tile> myNeighbors = this.getNeighbors(map);
		for(Tile t : myNeighbors) {
			if(t == tile) {
				return true;
			}
		}
		return false;
	}    

	public Set<Tile> getNeighbors(AntMap map) {
		Set<Tile> tiles = new HashSet<Tile>();
		for(Aim aim: Aim.values()) {
			tiles.add(map.getTile(this, aim));
		}
		return tiles;
	}    

	public Set<Tile> getMovableNeighbors(AntMap map) {
		Set<Tile> tiles = new HashSet<Tile>();
		for(Aim aim: Aim.values()) {
			Tile t = map.getTile(this, aim);
			if(map.getIlk(t).isMovable()) {
				tiles.add(t);
			}
		}
		return tiles;
	}

	public Set<Aim> getMovableDirections(AntMap map) {
		Set<Aim> directions = new HashSet<Aim>();
		for(Aim aim: Aim.values()) {
			Tile t = map.getTile(this, aim);
			if(map.getIlk(t).isMovable()) {
				directions.add(aim);
			}
		}
		return directions;
	}
	
	public Set<Tile> getPossibleDestinationsNextTurn(AntMap map) {
		Set<Tile> tiles = this.getMovableNeighbors(map);
		tiles.add(this);
		return tiles;
	}    
	
	public Set<Pair<Tile, Aim>> getNeighborsWithDirections(AntMap map) {
		Set<Pair<Tile, Aim>> tiles = new HashSet<Pair<Tile, Aim>>();
		for(Aim aim: Aim.values()) {
			tiles.add(new Pair<Tile, Aim>(map.getTile(this, aim), aim));
		}
		return tiles;
	}    
	
	
    /**
     * Returns row index.
     * 
     * @return row index
     */
    public int getRow() {
        return row;
    }
    
    /**
     * Returns column index.
     * 
     * @return column index
     */
    public int getCol() {
        return col;
    }
    
    /** 
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Tile o) {
        return hashCode() - o.hashCode();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return row * Game.MAX_MAP_SIZE + col;
    }
    
	// TODO: convert this to be row * ROW_LENGTH + column
	public int id() {
		return this.row * 65536 + this.col;
	}
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        boolean result = false;
        if (o instanceof Tile) {
            Tile tile = (Tile)o;
            result = row == tile.row && col == tile.col;
        }
        return result;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
		return "(" + this.row + "," + this.col + ")";
    }
}

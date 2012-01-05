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

/**
 * Represents type of tile on the game map.
 */
public enum Ilk {
    /** Water tile. */
    WATER,

/*    
    // Useless tile.  Meaning it is an inlet one deep that searches should not look at
    USELESS,
*/
    /** Tile is in a deadend.  Should only be searched for if we specifically want to go here. Same as useless? */
    DEADEND,
    
    /** Food tile. */
    FOOD,
    
    /** Land tile. */
    LAND,
    
    /** Dead ant tile. */
    DEAD,
    
    /** My ant tile. */
    MY_ANT,
    
    /** Enemy ant tile. */
    ENEMY_ANT,
    
    // My Hill tile
    MY_HILL,
    
    // Enemy Hill tile
    ENEMY_HILL;
    
    public boolean isMovable() {
        return ordinal() > WATER.ordinal();
    }

    public boolean isPassable() {
        return ordinal() > DEADEND.ordinal();
    }

    public boolean isNavigable() {
        return ordinal() > FOOD.ordinal();
    }

    public boolean isClearable() {
        return this == FOOD || this == DEAD || this == MY_ANT || this == ENEMY_ANT;
    }
    
    
    /**
     * Checks if this type of tile is unoccupied, which means it is a land tile or a dead ant tile.
     * 
     * @return <code>true</code> if this is a land tile or a dead ant tile, <code>false</code>
     *         otherwise
     */
    public boolean isUnoccupied() {
        return this == LAND || this == DEAD;
    }
}

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Holds all game data and current game state.
 */
public class Game {
    /** Maximum map size. */
    public static final int MAX_MAP_SIZE = 256 * 2;

    private final int loadTime;

    private final int turnTime;

    private final int rows;

    private final int cols;

    private final int turns;

    private final int viewRadius2;

    private final int attackRadius2;

    private final int spawnRadius2;

    private final boolean visible[][];
    private final boolean discovered[][];

    private final Set<Tile> visionOffsets;

    private long turnStartTime;
    private int currentTurn;

    private final Ilk map[][];

    private final Set<Tile> myAntTiles = new HashSet<Tile>();
    private final Set<Tile> enemyAntTiles = new HashSet<Tile>();
    private final Set<Tile> myHillTiles = new HashSet<Tile>();
    private final Set<Tile> enemyHillTiles = new HashSet<Tile>();
    private final Set<Tile> foodTiles = new HashSet<Tile>();
    private final Set<Tile> waterTiles = new HashSet<Tile>();

    private final Set<Order> orders = new HashSet<Order>();

    // Items used in logging
    private FileWriter fstream;
    private BufferedWriter out;
    private int iCrashTurn;

    /**
     * Creates new {@link Game} object.
     * 
     * @param loadTime timeout for initializing and setting up the bot on turn 0
     * @param turnTime timeout for a single game turn, starting with turn 1
     * @param rows game map height
     * @param cols game map width
     * @param turns maximum number of turns the game will be played
     * @param viewRadius2 squared view radius of each ant
     * @param attackRadius2 squared attack radius of each ant
     * @param spawnRadius2 squared spawn radius of each ant
     */
    public Game(int loadTime, int turnTime, int rows, int cols, int turns, int viewRadius2, int attackRadius2, int spawnRadius2) {
    	this.currentTurn = 0;
    	this.loadTime = loadTime;
        this.turnTime = turnTime;
        this.rows = rows;
        this.cols = cols;
        this.turns = turns;
        this.viewRadius2 = viewRadius2;
        this.attackRadius2 = attackRadius2;
        this.spawnRadius2 = spawnRadius2;

        // Set up logging
        try {
			fstream = new FileWriter("out.txt");
		    out = new BufferedWriter(fstream);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        map = new Ilk[rows][cols];
        for (Ilk[] row : map) {
            Arrays.fill(row, Ilk.LAND);
        }
        visible = new boolean[rows][cols];
        for (boolean[] row : visible) {
            Arrays.fill(row, false);
        }
        // Put this into the new data structure (Range, Offset - not sure what I'm calling it yet)
        visionOffsets = new HashSet<Tile>();
        int mx = (int)Math.sqrt(viewRadius2);
        for (int row = -mx; row <= mx; ++row) {
            for (int col = -mx; col <= mx; ++col) {
                int d = row * row + col * col;
                if (d <= viewRadius2) {
                    visionOffsets.add(new Tile(row, col));
                }
            }
        }

        discovered = new boolean[rows][cols];
        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < cols; ++col) {
                discovered[row][col] = false;
            }
        }
        
        this.iCrashTurn = 1;
    }

    /**
     * Returns timeout for initializing and setting up the bot on turn 0.
     * 
     * @return timeout for initializing and setting up the bot on turn 0
     */
    public int getLoadTime() {
        return loadTime;
    }

    /**
     * Returns timeout for a single game turn, starting with turn 1.
     * 
     * @return timeout for a single game turn, starting with turn 1
     */
    public int getTurnTime() {
        return turnTime;
    }

    /**
     * Returns game map height.
     * 
     * @return game map height
     */
    public int getRows() {
        return rows;
    }

    /**
     * Returns game map width.
     * 
     * @return game map width
     */
    public int getCols() {
        return cols;
    }

    /**
     * Returns maximum number of turns the game will be played.
     * 
     * @return maximum number of turns the game will be played
     */
    public int getTurns() {
        return turns;
    }

    public void incrementTurn() {
        currentTurn++;
    }

    public int getCurrentTurn() {
        return currentTurn;
    }
    
    /**
     * Returns squared view radius of each ant.
     * 
     * @return squared view radius of each ant
     */
    public int getViewRadius2() {
        return viewRadius2;
    }

    /**
     * Returns squared attack radius of each ant.
     * 
     * @return squared attack radius of each ant
     */
    public int getAttackRadius2() {
        return attackRadius2;
    }

    /**
     * Returns squared spawn radius of each ant.
     * 
     * @return squared spawn radius of each ant
     */
    public int getSpawnRadius2() {
        return spawnRadius2;
    }

    /**
     * Sets turn start time.
     * 
     * @param turnStartTime turn start time
     */
    public void setTurnStartTime(long turnStartTime) {
        this.turnStartTime = turnStartTime;
    }

    /**
     * Returns how much time the bot has still has to take its turn before timing out.
     * 
     * @return how much time the bot has still has to take its turn before timing out
     */
    public int getTimeRemaining() {
        return turnTime - (int)(System.currentTimeMillis() - turnStartTime);
    }

    /**
     * Returns ilk at the specified location.
     * 
     * @param tile location on the game map
     * 
     * @return ilk at the <cod>tile</code>
     */
    public Ilk getIlk(Tile tile) {
        return map[tile.getRow()][tile.getCol()];
    }

    /**
     * Sets ilk at the specified location.
     * 
     * @param tile location on the game map
     * @param ilk ilk to be set at <code>tile</code>
     */
    public void setIlk(Tile tile, Ilk ilk) {
        map[tile.getRow()][tile.getCol()] = ilk;
    }

    /**
     * Returns ilk at the location in the specified direction from the specified location.
     * 
     * @param tile location on the game map
     * @param direction direction to look up
     * 
     * @return ilk at the location in <code>direction</code> from <cod>tile</code>
     */
    public Ilk getIlk(Tile tile, Aim direction) {
        Tile newTile = getTile(tile, direction);
        return map[newTile.getRow()][newTile.getCol()];
    }

    public Ilk getIlk(int row, int col) {
    	return getIlk(getTile(row, col));
    }
    
    /**
     * Returns location in the specified direction from the specified location.
     * 
     * @param tile location on the game map
     * @param direction direction to look up
     * 
     * @return location in <code>direction</code> from <cod>tile</code>
     */
    public Tile getTile(Tile tile, Aim direction) {
    	return getTile(tile, new Tile(direction.getRowDelta(), direction.getColDelta()));
    }
    
    public Tile getTile(int row, int col) {
    	return new Tile(getRow(row), getCol(col));
    }
    
    public Tile getTileAllowNegatives(int row, int col) {
    	return new Tile(row, col);
    }

    /**
     * Returns location with the specified offset from the specified location.
     * 
     * @param tile location on the game map
     * @param offset offset to look up
     * 
     * @return location with <code>offset</code> from <cod>tile</code>
     */
    public Tile getTile(Tile tile, Tile offset) {
        return getTile(tile, offset.getRow(), offset.getCol());
    }
    
    // Get tile a specified number of spaces in a given direction
    public Tile getTile(Tile tile, Aim a, int dist) {
    	return getTile(tile, new Tile(a.getRowDelta() * dist, a.getColDelta() * dist));
    }

    public Tile getTile(Tile tile, int iRows, int iCols) {
        return new Tile(getRow(tile.getRow(), iRows), getCol(tile.getCol(), iCols));
    }

    public int getRow(int r) {
    	return (r + rows) % rows;
    }

    public int getRow(int r1, int r2) {
    	return getRow(r1 + r2);
    }

    public int getCol(int c) {
    	return (c + cols) % cols;
    }

    public int getCol(int c1, int c2) {
    	return getCol(c1 + c2);
    }

    public boolean isPassible(Tile t) {
    	return this.getIlk(t).isPassable();
    }

    public boolean isPassible(int row, int col) {
    	return this.getIlk(new Tile(row, col)).isPassable();
    }
    
    // TODO: Do I need to time this out at some point?
    public Tile getTileNextPassible(Tile tile, Aim direction) {
    	Tile next; 
    	while(!isPassible(next = getTile(tile, direction))) { }
        return next;
    }
    
    
    /**
     * Returns a set containing all my ants locations.
     * 
     * @return a set containing all my ants locations
     */
    public Set<Tile> getMyAnts() {
        return myAntTiles;
    }

    /**
     * Returns a set containing all enemy ants locations.
     * 
     * @return a set containing all enemy ants locations
     */
    public Set<Tile> getEnemyAnts() {
        return enemyAntTiles;
    }

    /**
     * Returns a set containing all my hills locations.
     * 
     * @return a set containing all my hills locations
     */
    public Set<Tile> getMyHills() {
        return myHillTiles;
    }

    /**
     * Returns a set containing all enemy hills locations.
     * 
     * @return a set containing all enemy hills locations
     */
    public Set<Tile> getEnemyHills() {
        return enemyHillTiles;
    }

    /**
     * Returns a set containing all food locations.
     * 
     * @return a set containing all food locations
     */
    public Set<Tile> getFoodTiles() {
        return foodTiles;
    }

    /**
     * Returns a set containing all my ants locations.
     * 
     * @return a set containing all my ants locations
     */
    public Set<Tile> getWaterTiles() {
        return waterTiles;
    }

    

    /**
     * Returns all orders sent so far.
     * 
     * @return all orders sent so far
     */
    public Set<Order> getOrders() {
        return orders;
    }

    /**
     * Returns true if a location is visible this turn
     *
     * @param tile location on the game map
     *
     * @return true if the location is visible
     */
    public boolean isVisible(Tile tile) {
        return visible[tile.getRow()][tile.getCol()];
    }

    /**
     * Returns true if a tile has been discovered this game
     *
     * @param tile location on the game map
     *
     * @return true if the location has been discovered
     */
    public boolean isDiscovered(Tile tile) {
        return discovered[tile.getRow()][tile.getCol()];
    }

    /**
     * Calculates distance between two locations on the game map.
     * 
     * @param t1 one location on the game map
     * @param t2 another location on the game map
     * 
     * @return distance between <code>t1</code> and <code>t2</code>
     */
    public int getDistance(Tile t1, Tile t2) {
        int rowDelta = getRowDelta(t1, t2);
        int colDelta = getColDelta(t1, t2);
        return rowDelta * rowDelta + colDelta * colDelta;
    }

    public Tile getClosestLocation(Tile origin, Set<Tile> destinations)
    {
    	Tile closestTile = null;
    	int closestDist = Integer.MAX_VALUE;
    	int currDist;
    	for (Tile dest : destinations) {
    		if((currDist = getDistance(origin, dest)) < closestDist) {
    			closestDist = currDist;
    			closestTile = dest;
    		}
        }
    	return closestTile;
    }
    
    public int getRowDelta(Tile t1, Tile t2) {
        int rowDelta = Math.abs(t1.getRow() - t2.getRow());
        return Math.min(rowDelta, rows - rowDelta);
    }

    public int getColDelta(Tile t1, Tile t2) {
        int colDelta = Math.abs(t1.getCol() - t2.getCol());
        return Math.min(colDelta, cols - colDelta);
    }

    public int getClosestDistanceAfterTurns(Tile t1, Tile t2, int numTurns) {
        int rowDelta = getRowDelta(t1, t2);
        int colDelta = getColDelta(t1, t2);

        for(int i=1; i<=numTurns; i++) {
        	if(rowDelta > colDelta) {
        		rowDelta = Math.max(0, rowDelta - 1);
        	}
        	else {
        		colDelta = Math.max(0, colDelta - 1);
        	}
        }
        return rowDelta * rowDelta + colDelta * colDelta;
    }

    public boolean areNeighbors(Tile t1, Tile t2)
    {
        int rowDelta = getRowDelta(t1, t2);
        int colDelta = getColDelta(t1, t2);
        
        return (rowDelta + colDelta) == 1;
    }
    
    public List<Aim> getMostAggressiveMove(Tile start, Tile end, Tile tiebreaker) 
    {
    	log("BEGIN getMostAgressiveMove: " + start + " | " + end);
    	List<Aim> aimList = getDirections(start, end);
    	
    	log("aimList: " + aimList);
        // If getDirections only returned one move, return it
        if(aimList.size() >= 2) {
            int rowDelta = getRowDelta(start, end);
            int colDelta = getColDelta(start, end);
        	log("rowDelta: " + rowDelta);
        	log("colDelta: " + colDelta);

        	boolean reverse = false;
            Aim aimFirst = aimList.get(0);
        	log("aimFirst: " + aimFirst);
        	
            // If rowDelta is greater, we're looking for a N or S move. If = E or W, reverse
            reverse = reverse || ((rowDelta > colDelta) && (aimFirst == Aim.EAST || aimFirst == Aim.WEST));

            // If colDelta is greater, we're looking for a E or W move. If = N or S, reverse
            reverse = reverse || ((colDelta > rowDelta) && (aimFirst == Aim.NORTH || aimFirst == Aim.SOUTH));

        	// TODO: tiebreaker!!! 
            // If we got here
            if(reverse) {
        		// reverse the list
            	log("reverse 1: " + aimList);
        		aimList.remove(aimFirst);
            	log("reverse 2: " + aimList);
        		aimList.add(1, aimFirst);
            	log("reverse 3: " + aimList);
            }
            else if(rowDelta == colDelta) {
            	// Figure out what to do here
            	log("Equally spaced: rowDelta == colDelta");
            }
        }

    	log("END getMostAgressiveMove");
    	return aimList;
    }    
    
    /**
     * Returns one or two orthogonal directions from one location to the another.
     * 
     * @param t1 one location on the game map
     * @param t2 another location on the game map
     * 
     * @return orthogonal directions from <code>t1</code> to <code>t2</code>
     */
    public List<Aim> getDirections(Tile t1, Tile t2) {
        List<Aim> directions = new ArrayList<Aim>();
        if (t1.getRow() < t2.getRow()) {
            if (t2.getRow() - t1.getRow() >= rows / 2) {
                directions.add(Aim.NORTH);
            } else {
                directions.add(Aim.SOUTH);
            }
        } else if (t1.getRow() > t2.getRow()) {
            if (t1.getRow() - t2.getRow() >= rows / 2) {
                directions.add(Aim.SOUTH);
            } else {
                directions.add(Aim.NORTH);
            }
        }
        if (t1.getCol() < t2.getCol()) {
            if (t2.getCol() - t1.getCol() >= cols / 2) {
                directions.add(Aim.WEST);
            } else {
                directions.add(Aim.EAST);
            }
        } else if (t1.getCol() > t2.getCol()) {
            if (t1.getCol() - t2.getCol() >= cols / 2) {
                directions.add(Aim.EAST);
            } else {
                directions.add(Aim.WEST);
            }
        }
        return directions;
    }
    
    public Aim getDirection(Tile t1, Tile t2) {
    	List<Aim> directions = getDirections(t1, t2);
    	if(directions == null || directions.size() == 0) {
    		return null;
    	}
    	
    	return directions.get(0);
    }
    
    
    
    
    /**
     * Clears game state information about my ants locations.
     */
    public void clearMyAnts() {
        for (Tile myAnt : myAntTiles) {
            map[myAnt.getRow()][myAnt.getCol()] = Ilk.LAND;
        }
        myAntTiles.clear();
    }

    /**
     * Clears game state information about enemy ants locations.
     */
    public void clearEnemyAnts() {
        for (Tile enemyAnt : enemyAntTiles) {
            map[enemyAnt.getRow()][enemyAnt.getCol()] = Ilk.LAND;
        }
        enemyAntTiles.clear();
    }

    /**
     * Clears game state information about food locations.
     */
    public void clearFood() {
        for (Tile food : foodTiles) {
            map[food.getRow()][food.getCol()] = Ilk.LAND;
        }
        foodTiles.clear();
    }

    /**
     * Clears game state information about my hills locations.
     */
    public void clearMyHills() {
        myHillTiles.clear();
    }

    /**
     * Clears game state information about enemy hills locations.
     */
    public void clearEnemyHills() {
        enemyHillTiles.clear();
    }

    /**
     * Clears game state information about dead ants locations.
     */
    public void clearDeadAnts() {
        //currently we do not have list of dead ants, so iterate over all map
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (map[row][col] == Ilk.DEAD) {
                    map[row][col] = Ilk.LAND;
                }
            }
        }
    }

    /**
     * Clears discovered tile information
     */
    public void clearDiscoveredTiles() {
        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < cols; ++col) {
                discovered[row][col] = false;
            }
        }
    }

    /**
     * Clears visible information
     */
    public void clearVision() {
        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < cols; ++col) {
                visible[row][col] = false;
            }
        }
    }

    /**
     * Calculates visible information
     * also keeps an view of which tiles have been discovered during the game
     */
    public void setVision() {
        for (Tile antLoc : myAntTiles) {
            for (Tile locOffset : visionOffsets) {
                Tile newLoc = getTile(antLoc, locOffset);
                visible[newLoc.getRow()][newLoc.getCol()] = true;
                discovered[newLoc.getRow()][newLoc.getCol()] = true;
            }
        }
    }

    /**
     * Updates game state information about new ants and food locations.
     * 
     * @param ilk ilk to be updated
     * @param tile location on the game map to be updated
     */
    public void update(Ilk ilk, Tile tile) {
        map[tile.getRow()][tile.getCol()] = ilk;
        switch (ilk) {
            case FOOD:
                foodTiles.add(tile);
            break;
            case MY_ANT:
                myAntTiles.add(tile);
            break;
            case WATER:
            	waterTiles.add(tile);
            break;
            case ENEMY_ANT:
                enemyAntTiles.add(tile);
            break;
        }
    }

    /**
     * Updates game state information about hills locations.
     *
     * @param owner owner of hill
     * @param tile location on the game map to be updated
     */
    public void updateHills(int owner, Tile tile) {
        if (owner > 0)
            enemyHillTiles.add(tile);
        else
            myHillTiles.add(tile);
    }

    public void log(String log){
    	try {

    	    out.write(log + "\r\n");
    	    out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public int getCrashTurn() {
    	return iCrashTurn;
    }    
    
    public void logcrash(String str){
    	if(getCurrentTurn() == this.iCrashTurn)
    		log(str);
    }
    
    /**
     * Issues an order by sending it to the system output.
     * 
     * @param myAnt map tile with my ant
     * @param direction direction in which to move my ant
     */
    public void issueOrder(Tile myAnt, Aim direction) {
        Order order = new Order(myAnt, direction);
        orders.add(order);
        System.out.println(order);
    }
}

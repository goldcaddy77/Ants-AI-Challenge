import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class AntMap {

	private int ROWS;
	private int COLS;
	
	private Game game;

	// Different map views of the squares.  In theory, these should all be parameters of a Tile or Square
    private boolean visible[][];
    private boolean discovered[][];
    private final Ilk map[][];
    private String detailedMap[][];
	
    private Map<Tile, Ant> myAntLocations = new HashMap<Tile,Ant>();

    private Set<Tile> inputFriendlyAntTiles = new HashSet<Tile>();
    private Set<Tile> allFriendlyAntTiles = new HashSet<Tile>();
    private Set<Ant> newFriendlyAnts = new HashSet<Ant>();
    private Set<Ant> allFriendlyAnts = new HashSet<Ant>();

    private Set<Tile> newWaterTiles = new HashSet<Tile>();
    private Set<Tile> allWaterTiles = new HashSet<Tile>();
	
    private Set<Tile> inputFoodTiles = new HashSet<Tile>();
    private Set<Tile> newFoodTiles = new HashSet<Tile>();
    private Set<Tile> allFoodTiles = new HashSet<Tile>();

    private Set<Tile> inputFriendlyHillTiles = new HashSet<Tile>();
    private Set<Tile> allFriendlyHillTiles = new HashSet<Tile>();

    private Set<Tile> inputEnemyHillTiles = new HashSet<Tile>();
    private Set<Tile> newEnemyHillTiles = new HashSet<Tile>();
    private Set<Tile> allEnemyHillTiles = new HashSet<Tile>();

    private Set<Tile> allUnnavigableTiles = new HashSet<Tile>();
    private Set<Tile> allDeadEndTiles = new HashSet<Tile>();

    // No way to tell which enemies are new for now, so we need to take all of their locations each turn no matter what
    private Set<Tile> allEnemyAntTiles = new HashSet<Tile>();
    
    private Offset offVision;
    private Offset offAttack;
    private Offset offNextAttack;
    private HeatMap hmMyVision;
    private HeatMap hmEnemyVision;
    private HeatMap hmMyAttack;
    private HeatMap hmMyNextAttack;
    private HeatMap hmEnemyAttack;
    private HeatMap hmEnemyNextAttack;
    private HeatMap hmNextMyAttackVsEnemyDelta;
    private Map<Ant, Set<Tile>> mapMyAntToCloseByEnemies = new HashMap<Ant, Set<Tile>>();
    private Map<Tile,Set<Ant>> mapEnemyToMyCloseByAnts = new HashMap<Tile,Set<Ant>>();

    // Unused items
    // private Map<Tile, Tile> enemyMoves = new HashMap<Tile,Tile>();
    // private Set<Tile> newEnemyLocations = new HashSet<Tile>();
    // private Set<Integer> enemies = new HashSet<Integer>();
    
	public AntMap(Game g) 
	{
		this.game = g;
		this.ROWS = g.getRows();
		this.COLS = g.getCols();
		
        map = new Ilk[ROWS][COLS];
        for (Ilk[] row : map) {
            Arrays.fill(row, Ilk.LAND);
        }

		discovered = new boolean[ROWS][COLS];
        for (int row = 0; row < ROWS; ++row) {
            for (int col = 0; col < COLS; ++col) {
                discovered[row][col] = false;
            }
        }

        visible = new boolean[ROWS][COLS];
        for (boolean[] row : visible) {
            Arrays.fill(row, false);
        }
        
        // Set up heatmaps
        this.offVision = new Offset(game.getViewRadius2(), this);
        this.hmMyVision = new HeatMap(this, offVision);
        this.hmEnemyVision = new HeatMap(this, offVision);
        
        this.offAttack = new Offset(game.getAttackRadius2(), this);
        this.hmMyAttack = new HeatMap(this, offAttack, false);
        this.hmEnemyAttack = new HeatMap(this, offAttack, false);
        
        // Find all tiles enemy could be at next turn
        // not using this right now, we'll heatmap the enemy's expected moves instead
        this.offNextAttack = new Offset(game.getAttackRadius2(), this);
        this.offNextAttack.add(new Offset(game.getAttackRadius2(), this, new Tile(0, -1))); // add tiles if moved north
    	this.offNextAttack.add(new Offset(game.getAttackRadius2(), this, new Tile(0, 1)));  // add tiles if moved south
    	this.offNextAttack.add(new Offset(game.getAttackRadius2(), this, new Tile(1, 0)));  // add tiles if moved east
    	this.offNextAttack.add(new Offset(game.getAttackRadius2(), this, new Tile(-1, 0))); // add tiles if moved west

    	// Find heat of all enemy / friendly attack squares
    	this.hmEnemyNextAttack = new HeatMap(this, offNextAttack, false);
    	this.hmMyNextAttack = new HeatMap(this, offNextAttack, false);
        this.hmNextMyAttackVsEnemyDelta = new HeatMap(this, offNextAttack, false);

        detailedMap = new String[ROWS][COLS];
        for (int r = 0; r < ROWS; ++r) {
            for (int c = 0; c < COLS; ++c) {
            	detailedMap[r][c] = "_";
            }
        }
	}
	
	public void beforeUpdate()
	{
		clearVision();
		
		// Get the input Sets ready
		inputFriendlyAntTiles.clear();
        inputFoodTiles.clear();
        inputFriendlyHillTiles.clear();
        inputEnemyHillTiles.clear();
        allEnemyAntTiles.clear();
        
        // Clear out the new sets
        newFriendlyAnts.clear();
        newFoodTiles.clear();
        newEnemyHillTiles.clear();
        newWaterTiles.clear();

        for (int r = 0; r < ROWS; ++r) {
            for (int c = 0; c < COLS; ++c) {
            	if(map[r][c].isClearable()) {
            		map[r][c] = Ilk.LAND;
            	}
            }
        }
	}
	
	
	

	public void afterUpdate() 
	{
    	log("[" + game.getTimeRemaining() + "] Calculate Dead Ends");
    	calculateDeadEnds();

    	log("[" + game.getTimeRemaining() + "] Recalc Food HeatMap");
    	this.hmMyVision.calculate(allFriendlyAntTiles, allWaterTiles);
    	this.hmEnemyVision.calculate(allEnemyAntTiles, allWaterTiles);

    	log("[" + game.getTimeRemaining() + "] Recalc My Attack HeatMap");
    	this.hmMyAttack.calculate(allFriendlyAntTiles, new HashSet<Tile>());

    	log("[" + game.getTimeRemaining() + "] Recalc My Next Attack HeatMap");
    	this.hmMyNextAttack.calculate(allFriendlyAntTiles, allWaterTiles);

    	log("[" + game.getTimeRemaining() + "] Recalc Enemy Attack HeatMap");
    	this.hmEnemyAttack.calculate(allEnemyAntTiles, allWaterTiles);
        
    	log("[" + game.getTimeRemaining() + "] Recalc Next Enemy Attack HeatMap");
    	this.hmEnemyNextAttack.calculate(allEnemyAntTiles, allWaterTiles);
    	// this.hmEnemyNextAttack.print();
        
    	log("[" + game.getTimeRemaining() + "] Recalc Me vs Enemy Next Attack");
    	this.hmNextMyAttackVsEnemyDelta.copyMapFrom(hmMyNextAttack);
    	this.hmNextMyAttackVsEnemyDelta.subtract(hmEnemyNextAttack);
    	// this.hmNextMyAttackVsEnemyDelta.print();


    	// remove my sacked hills - need to use iterator when removing
    	for (Iterator<Tile> hillIter = allFriendlyHillTiles.iterator(); hillIter.hasNext(); ) {
	        Tile hill = hillIter.next();
            if (isVisible(hill) && !inputFriendlyHillTiles.contains(hill)) {
	        	hillIter.remove();
    			log("Discovered my hill was destroyed: " + hill);
	        }
        }    	
        
    	// remove sacked enemy hills - need to use iterator when removing
    	// Also, to remove a hill, you need to check to see if the hill is in your site and 
    	// the data doesn't contain it - then you know it's gone
    	for (Iterator<Tile> hillIter = allEnemyHillTiles.iterator(); hillIter.hasNext(); ) {
	        Tile hill = hillIter.next();

	        // Hill tile is visible and there is no hill there - DEAD
	        if (isVisible(hill) && !inputEnemyHillTiles.contains(hill)) {
	        	hillIter.remove();

	        	// Tell all of my ants that were ordered here that they no longer have orders
	        	for(Ant ant : allFriendlyAnts) {
	            	if(ant.getDestination() != null && ant.getDestination().equals(hill)) {
	            		ant.clearPath();
	            	}
	            }	        	
    			log("Discovered enemyhill was destroyed: " + hill);
	        }
        }    	

    	// Add new enemy hills to the new enemy hills set (and 'all' set)
        for (Tile tileEnemyHill : inputEnemyHillTiles) {
            if (!allEnemyHillTiles.contains(tileEnemyHill)) {
            	newEnemyHillTiles.add(tileEnemyHill);
            	allEnemyHillTiles.add(tileEnemyHill);
                log("[Map] New enemy hill added: " + tileEnemyHill);
            }
        }

    	// Add new food to the new food set (and 'all' set)
        for (Tile tileFood : inputFoodTiles) {
            if (!allFoodTiles.contains(tileFood)) {
            	newFoodTiles.add(tileFood);
            	allFoodTiles.add(tileFood);
                log("[Map] New food added: " + tileFood);
            }
        }
        
        mapMyAntToCloseByEnemies = new HashMap<Ant, Set<Tile>>();
        mapEnemyToMyCloseByAnts = new HashMap<Tile,Set<Ant>>();

    	for(Tile tileEnemyAnt: allEnemyAntTiles)
    	{
    		if(game.getTimeRemaining() < 100) { break; }
    		for(Ant ant: allFriendlyAnts)
    		{
    			Integer distWithMoves = getClosestDistanceAfterTurns(ant.getCurrentTile(), tileEnemyAnt, 2);
    			
    			if(distWithMoves <= game.getAttackRadius2()) 
    			{
    				log("Ant " + ant.getCurrentTile() + " is close to enemy at location " + tileEnemyAnt + ".  Adding mapping.");
    							
    				if(!mapMyAntToCloseByEnemies.containsKey(ant)){
    					mapMyAntToCloseByEnemies.put(ant,new HashSet<Tile>());
    				}
    				mapMyAntToCloseByEnemies.get(ant).add(tileEnemyAnt);
    	
    				if(!mapEnemyToMyCloseByAnts.containsKey(tileEnemyAnt)){
    					mapEnemyToMyCloseByAnts.put(tileEnemyAnt, new HashSet<Ant>());
    				}
    				mapEnemyToMyCloseByAnts.get(tileEnemyAnt).add(ant);
    			}
    		}
    	}
    	
    	setVision();    	
	}
	
    /////////////////////////////////////////////////////////////////////////////////////////
	// The following items are sent every turn, so just return what we're passed
    // My Ants | Enemy Ants | Food
    public Set<Tile> getAllFriendlyAntTiles() {
        return allFriendlyAntTiles;
    }

    public Set<Tile> getAllEnemyAntTiles() {
        return allEnemyAntTiles;
    }

    public Set<Tile> getAllFoodTiles() {
        return inputFoodTiles;
    }

    // The following items are only sent once, so maintain our own Sets
    // My Hills | Enemy Hills | Water
    public Set<Tile> getFriendlyHills() {
        return allFriendlyHillTiles;
    }
    
    public Set<Tile> getAllEnemyHills() {
        return allEnemyHillTiles;
    }

    public Set<Tile> getAllWaterTiles() {
        return allWaterTiles;
    }
    
    public Set<Ant> getAllFriendlyAnts() {
        return allFriendlyAnts;
    }

    public Map<Tile, Ant> getLocationAntMap() {
        
    	log("getLocationAntMap: myAntLocations is " + myAntLocations);

    	return myAntLocations;
    }

    public Set<Ant> getNewFriendlyAnts() {
        return newFriendlyAnts;
    }
    
    
    /**
     * Clears visible information
     */
    public void clearVision() {
        for (int row = 0; row < ROWS; ++row) {
            for (int col = 0; col < COLS; ++col) {
                visible[row][col] = false;
            }
        }
    }
        

    /**
     * Calculates visible information
     * also keeps an view of which tiles have been discovered during the game
     */
    public void setVision() {
        for (Tile antLoc : allFriendlyAntTiles) {
            for (Tile locOffset : offVision.getOffsets()) {
                Tile newLoc = getTile(antLoc, locOffset);
                visible[newLoc.getRow()][newLoc.getCol()] = true;
                discovered[newLoc.getRow()][newLoc.getCol()] = true;
            }
        }
    }
    

    protected void processFood(int row, int col) {
    	setIlk(row, col, Ilk.FOOD);
		inputFoodTiles.add(new Tile(row, col));
    }
    
    
    protected void processWater(int row, int col) {
    	setIlk(row, col, Ilk.WATER);
		newWaterTiles.add(new Tile(row, col));
		allUnnavigableTiles.add(new Tile(row, col));
    }

    protected void processLiveAnt(int row, int col, int owner) 
    {
		Tile tile = new Tile(row, col);
    	if(owner == 0) 
		{
	    	setIlk(row, col, Ilk.MY_ANT);
			inputFriendlyAntTiles.add(tile);
			
			// Identify brand new ants
		    if(!allFriendlyAntTiles.contains(tile))
		    {
		        Ant ant = new Ant(row,col);
		    	myAntLocations.put(new Tile(row,col), ant);
		    	
		    	log("processLiveAnt: myAntLocations is " + myAntLocations);

		    	allFriendlyAntTiles.add(tile);
		    	allFriendlyAnts.add(ant);
		    	newFriendlyAnts.add(ant);
		        game.log("processLiveAnt: added ant " + ant);
			
		    }
		}
		else {
	    	setIlk(row, col, Ilk.ENEMY_ANT);
	    	allEnemyAntTiles.add(new Tile(row, col));
			
			// TODO: Do we want to keep track of how many opponents we've seen?
	    	// enemies.add(new Integer(owner));
		}
    }
    

    protected void processHill(int row, int col, int owner) 
    {
		if(owner == 0) {
	    	setIlk(row, col, Ilk.MY_HILL);
	    	inputFriendlyHillTiles.add(new Tile(row, col));
	    	allFriendlyHillTiles.add(new Tile(row, col));
		}
		else {
	    	setIlk(row, col, Ilk.ENEMY_HILL);
	    	inputEnemyHillTiles.add(new Tile(row, col));
		}
    }

    // Not really processing dead ants right now, are we?
    protected void processDeadAnt(int row, int col, int owner) {
    	game.log("Dead ant processed by 'processDeadAnt': (" + row + ", " + col + ") - owner " + owner);
    	
    	// TODO: this could tell us who our dead ants are so that we don't need to do the comparison
        if(owner == 0 && myAntLocations.containsKey(new Tile(row,col))) {
        	
        	Ant removedAnt = myAntLocations.remove(new Tile(row,col));
        	allFriendlyAntTiles.remove(removedAnt.getCurrentTile());
        	allFriendlyAnts.remove(removedAnt);
        	
        	log("My ant {" + removedAnt + "} died at " + row + " : " + col);
        }
    }

    public void moveAnt(Ant ant, Tile tile) 
    {
    	log("moveAnt: " + ant + " | " + tile);
    	
    	myAntLocations.remove(ant.getCurrentTile());
    	allFriendlyAntTiles.remove(ant.getCurrentTile());

    	myAntLocations.put(tile, ant);
    	allFriendlyAntTiles.add(tile);
    	
    	ant.move(tile);
    }
    
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
    
    public Tile getFarthestLocation(Tile origin, Set<Tile> destinations)
    {
    	Tile farthestTile = null;
    	int farthestDist = Integer.MIN_VALUE;
    	int currDist;
    	for (Tile dest : destinations) {
    		if((currDist = getDistance(origin, dest)) > farthestDist) {
    			farthestDist = currDist;
    			farthestTile = dest;
    		}
        }
    	return farthestTile;
    }
    
    public int getRowDelta(Tile t1, Tile t2) {
        int rowDelta = Math.abs(t1.getRow() - t2.getRow());
        return Math.min(rowDelta, ROWS - rowDelta);
    }

    public int getColDelta(Tile t1, Tile t2) {
        int colDelta = Math.abs(t1.getCol() - t2.getCol());
        return Math.min(colDelta, COLS - colDelta);
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
    
    public List<Aim> getDirectionsMostAggressive(Tile start, Tile end, Tile tiebreaker) 
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
            	// log("Equally spaced: rowDelta == colDelta");
            }
        }

    	// log("END getMostAgressiveMove");
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
            if (t2.getRow() - t1.getRow() >= ROWS / 2) {
                directions.add(Aim.NORTH);
            } else {
                directions.add(Aim.SOUTH);
            }
        } else if (t1.getRow() > t2.getRow()) {
            if (t1.getRow() - t2.getRow() >= ROWS / 2) {
                directions.add(Aim.SOUTH);
            } else {
                directions.add(Aim.NORTH);
            }
        }
        if (t1.getCol() < t2.getCol()) {
            if (t2.getCol() - t1.getCol() >= COLS / 2) {
                directions.add(Aim.WEST);
            } else {
                directions.add(Aim.EAST);
            }
        } else if (t1.getCol() > t2.getCol()) {
            if (t1.getCol() - t2.getCol() >= COLS / 2) {
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
    	
    public Ilk getIlk(Tile tile) {
        return map[tile.getRow()][tile.getCol()];
    }

    public void setIlk(Tile tile, Ilk ilk) {
        map[tile.getRow()][tile.getCol()] = ilk;
    }

    public void setIlk(int row, int col, Ilk ilk) {
        map[row][col] = ilk;
    }
    
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

    public Tile getTileAllowNegatives(Tile tile, Aim aim) {
    	return new Tile(tile.getRow() + aim.getRowDelta(), tile.getCol() + aim.getColDelta());
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
    public int getRows() {
    	return ROWS;
    }

    public int getCols() {
    	return COLS;
    }
    
    public int getRow(int r) {
    	return (r + ROWS) % ROWS;
    }

    public int getRow(int r1, int r2) {
    	return getRow(r1 + r2);
    }

    public int getCol(int c) {
    	return (c + COLS) % COLS;
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

    public void log(String str) {
    	game.log(str);
    }
    
    public boolean isVisible(Tile tile) {
        return visible[tile.getRow()][tile.getCol()];
    }

    public boolean isDiscovered(Tile tile) {
        return discovered[tile.getRow()][tile.getCol()];
    }
    
    public boolean isEnemyNextAttackBorder(Tile tile) {
    	return hmEnemyNextAttack.isBorder(tile);
    }

    public Set<Tile> findEnemyNextAttackBorders(Tile tile) {
    	return hmEnemyNextAttack.findNeighboringBorders(tile);
    }

    public int getEnemyNextAttackHeat(Ant ant) {
    	return hmEnemyNextAttack.getHeat(ant); 
    }
    
    public int getMyNextAttackHeat(Tile tile) {
    	return hmMyNextAttack.getHeat(tile); 
    }

    public Set<Ant> getMyAntsInEnemyAttackRange(Tile tile) {
    	return mapEnemyToMyCloseByAnts.get(tile); 
    }

    public Set<Tile> getEnemyAntsInMyAttackRange(Ant ant) {
    	return this.mapMyAntToCloseByEnemies.get(ant); 
    }

    public Map<Tile,Set<Ant>> getMapEnemyToMyCloseByAnts() {
    	return mapEnemyToMyCloseByAnts; 
    }

    public Map<Ant, Set<Tile>> getMyAntToCloseByEnemies() {
    	return this.mapMyAntToCloseByEnemies; 
    }
    
    public HeatMap getMyVisionHeatMap() {
    	return hmMyVision; 
    }

    public HeatMap getEnemyVisionHeatMap() {
    	return hmEnemyVision; 
    }
    
    // TODO: Do I need to time this out at some point?
    public Tile getTileNextPassible(Tile tile, Aim direction) {
    	Tile next; 
    	while(!isPassible(next = getTile(tile, direction))) { }
        return next;
    }
    
	public Set<Tile> findHorizen(Ant ant) 
	{
		log("findHorizen: " + ant);
		log("Total horizen: " + offVision.getHorizon());
		
		if(game.getCurrentTurn() == 21) {
			log("hello world");
		}
		
		Set<Tile> horizon = new HashSet<Tile>();
		for(Tile tileOffset: offVision.getHorizon()) {
			Tile tile = getTile(ant.getCurrentTile(), tileOffset);
			if(!isVisible(tile)) {
				horizon.add(tile);
			}
		}

		log("Invisible horizen: " + horizon);

		return horizon;
	}

	public boolean isOnHill(Ant ant) {
		return allFriendlyHillTiles.contains(ant.getCurrentTile());
	}

	public Set<Tile> getUnnavigableTiles() {
		return allUnnavigableTiles;
	}
	
//	public Set<Tile> findInvisibleHorizen(Ant ant) 
//	{
//		Set<Tile> invisible = new HashSet<Tile>();
//		for(Tile tile: findHorizen(ant))) {
//			if(!isVisible(tile)) {
//				horizon.add(tile);
//			}
//		}
//		return horizon;
//	}

	private void calculateDeadEnds()
	{
		// Looks for segments > 2 in length, then look to see if they trap tiles on either side
	    int iFirstNorth, iLastNorth, iFirstSouth, iLastSouth;
	    int iSegmentStart = Integer.MAX_VALUE;
	    int iSegmentLen = -1;
	    boolean bThisTileWater;

	    String STR_WATER = "*";
	    String STR_UNKNOWN = "_";
	    String STR_LAND = " ";
	    String STR_NORTH = "N";
	    String STR_SOUTH = "S";
	    String STR_EAST = "E";
	    String STR_WEST = "W";

    	boolean bWorkingOnPath = false;
    	boolean bRequiresWrap = false;
    	boolean bSkippedBeginning = false;
    	boolean bAllWater;
	    for (int r = 0; r < ROWS; ++r)
	    {
	    	bWorkingOnPath = false; // need to special handle wrapping around the board, so keep track of whether we're working on a path

	    	// require wrap if both the first and last spaces are not passable (water)
	    	// bRequiresWrap = !isPassible(r, 0) && !isPassible(r, COLS - 1);
	    	bRequiresWrap = !getIlk(new Tile(r, 0)).equals(Ilk.WATER) && !getIlk(new Tile(r, COLS - 1)).equals(Ilk.WATER);
	    	bSkippedBeginning = false;
	    	bAllWater = true;
	    	
	    	for (int c = 0; c < COLS || (bWorkingOnPath && !bAllWater); ++c)
	        {
	    		int c2 = c % COLS;
            	if(isDiscovered(new Tile(r, c2)) && detailedMap[r][c2] == STR_UNKNOWN) {
	            	detailedMap[r][c2] = STR_LAND;
            	}

	    		if(getIlk(new Tile(r, c2)).equals(Ilk.WATER)) {
	    			// skip if you require wrap and haven't skipped the beginning yet
	    			if(!bRequiresWrap || bSkippedBeginning) {
		            	if(getIlk(new Tile(r, c2)).equals(Ilk.WATER)) {
		            		detailedMap[r][c2] = STR_WATER;
		            	}
		            	iSegmentLen = (iSegmentStart == Integer.MAX_VALUE) ? 1 : ++iSegmentLen;
		            	iSegmentStart = (iSegmentStart == Integer.MAX_VALUE) ? c2 : iSegmentStart;
	            	}
	            }
	        	else
	        	{
	        		bAllWater = false;
	        		bWorkingOnPath = false;
	    			bSkippedBeginning = true;
	        		if(iSegmentLen > 0)
	        		{
	        			Tile tileStart = new Tile(r, iSegmentStart);

	        			iFirstNorth = Integer.MAX_VALUE - 100;
	        			iLastNorth = -1;

	        			// don't just look for the first water tile parallel to the water segment
	        			// look one to each side, because those water tiles would make the rest of
	        			// the inner tiles unneeded (see tutorial map for example)
	                	for (int i=-1; i<iSegmentLen+1; ++i)
	                	{
	                		Tile t = getTile(tileStart, -1, i);
	        				bThisTileWater = getIlk(t).equals(Ilk.WATER);
	                		if(bThisTileWater) {
	                			iFirstNorth = Math.min(iFirstNorth, i);
	                   			iLastNorth = Math.max(iLastNorth, i);
	                		}
	               		}

	                	if(iLastNorth - iFirstNorth > 1) {
	                    	for (int i=iFirstNorth+1; i < iLastNorth; ++i) {
		                		Tile t = getTile(tileStart, -1, i);
	                    		if(detailedMap[t.getRow()][t.getCol()] != STR_WATER) {
	                    			detailedMap[t.getRow()][t.getCol()] = STR_NORTH;
	                    		}
	                    	}
	                	}

	        			iFirstSouth = Integer.MAX_VALUE - 100;
	        			iLastSouth = -1;

	                	for (int i=-1; i<iSegmentLen+1; ++i)
	                	{
	                		Tile t = getTile(tileStart, 1, i);
	        				bThisTileWater = getIlk(t).equals(Ilk.WATER);
	                		if(bThisTileWater) {
	                			iFirstSouth = Math.min(iFirstSouth, i);
	                			iLastSouth = Math.max(iLastSouth, i);
	                		}
	               		}

	                	if(iLastSouth - iFirstSouth > 1) {
	                    	for (int i=iFirstSouth+1; i < iLastSouth; ++i) {
		                		Tile t = getTile(tileStart, 1, i);
	                    		if(detailedMap[t.getRow()][t.getCol()] != STR_WATER) {
	                    			detailedMap[t.getRow()][t.getCol()] = STR_SOUTH;
	                    		}
	                    	}
	                	}
	                	// End south side
	            	}

	        		// Reset the segment start
	        		iSegmentLen = -1;
	        		iSegmentStart = Integer.MAX_VALUE;
	        	}
	        }
	    }
	    // End horizontal pieces

	    // Looks for segments > 2 in length, then look to see if they trap tiles on either side
	    int iFirstWest, iLastWest, iFirstEast, iLastEast;
	    iSegmentStart = Integer.MAX_VALUE;
	    iSegmentLen = -1;

		bWorkingOnPath = false;
		bSkippedBeginning = true;
		bAllWater = true;
    	for (int c = 0; c < COLS; ++c)
	    {
	    	bWorkingOnPath = false; // need to special handle wrapping around the board, so keep track of whether we're working on a path

	    	// require wrap if both the first and last spaces are not passable (water)
	    	bRequiresWrap = !getIlk(new Tile(0, c)).equals(Ilk.WATER) && !getIlk(new Tile(ROWS - 1, c)).equals(Ilk.WATER);
	    	bSkippedBeginning = false;
	    	bAllWater = true;

	    	for (int r = 0; r < ROWS || (bWorkingOnPath && !bAllWater); ++r)
	        {
	    		int r2 = r % ROWS;
            	if(isDiscovered(new Tile(r2, c)) && detailedMap[r2][c] == STR_UNKNOWN) {
	            	detailedMap[r2][c] = STR_LAND;
            	}

	    		if(getIlk(new Tile(r2, c)).equals(Ilk.WATER)) {
	    			bWorkingOnPath = true;
	    			// skip if you require wrap and haven't skipped the beginning yet
	    			if(!bRequiresWrap || bSkippedBeginning) {
		            	if(getIlk(new Tile(r2, c)).equals(Ilk.WATER)) {
		    				detailedMap[r2][c] = STR_WATER;
		            	}

		            	iSegmentLen = (iSegmentStart == Integer.MAX_VALUE) ? 1 : ++iSegmentLen;
		            	iSegmentStart = (iSegmentStart == Integer.MAX_VALUE) ? r2 : iSegmentStart;
	            	}
	            }
	        	else
	        	{
	    			bWorkingOnPath = false;
	    			bSkippedBeginning = true;
	    			bAllWater = false;
	         	   
	        		if(iSegmentLen > 0)
	        		{
	        			Tile tileStart = new Tile(iSegmentStart, c);

	        			iFirstWest = Integer.MAX_VALUE - 100;
	        			iLastWest = -1;

	        			// don't just look for the first water tile parallel to the water segment
	        			// look one to each side, because those water tiles would make the rest of
	        			// the inner tiles unneeded (see tutorial map for example)
	                	for (int i=-1; i<iSegmentLen+1; ++i)
	                	{
	                		Tile t = getTile(tileStart, i, -1);
	        				bThisTileWater = getIlk(t).equals(Ilk.WATER);
	                		if(bThisTileWater) {
	                			iFirstWest = Math.min(iFirstWest, i);
	                   			iLastWest = Math.max(iLastWest, i);
	                		}
	               		}

	            	    if(iLastWest - iFirstWest > 1) {
	                    	for (int i=iFirstWest+1; i < iLastWest; ++i) {
		                		Tile t = getTile(tileStart, i, -1);
	                    		if(detailedMap[t.getRow()][t.getCol()] != STR_WATER) {
	                    			detailedMap[t.getRow()][t.getCol()] = STR_WEST;
	                    		}
	                    	}
	                	}

	        			iFirstEast = Integer.MAX_VALUE - 100;
	        			iLastEast = -1;

	                	for (int i=-1; i<iSegmentLen+1; ++i)
	                	{
	                		Tile t = getTile(tileStart, i, 1);
	        				bThisTileWater = getIlk(t).equals(Ilk.WATER);
	                		if(bThisTileWater) {
	                			iFirstEast = Math.min(iFirstEast, i);
	                			iLastEast = Math.max(iLastEast, i);
	                		}
	               		}

	                	if(iLastEast - iFirstEast > 1) {
	                    	for (int i=iFirstEast+1; i < iLastEast; ++i) {
		                		Tile t = getTile(tileStart, i, 1);
	                    		if(detailedMap[t.getRow()][t.getCol()] != STR_WATER) {
	                    			detailedMap[t.getRow()][t.getCol()] = STR_EAST;
	                    		}
	                    	}
	                	}
	                	// End east side
	            	}

	        		// Reset the segment start
	        		iSegmentLen = -1;
	        		iSegmentStart = Integer.MAX_VALUE;
	        	}
	        }
	    }
	    // End vertical pieces

    	// Set Ilk for new DEADEND tiles
    	for(int r = 0; r<ROWS; r++){
			for(int c = 0; c<COLS; c++){
				if(detailedMap[r][c] == STR_NORTH ||
						detailedMap[r][c] == STR_SOUTH ||
						detailedMap[r][c] == STR_EAST ||
						detailedMap[r][c] == STR_WEST) {
					setIlk(new Tile(r,c), Ilk.DEADEND);
					allDeadEndTiles.add(new Tile(r, c));
					allUnnavigableTiles.add(new Tile(r, c));
				}
			}
		}

//		log("----------");
//		for(int r = 0; r<ROWS; r++){
//			String out = "";
//			for(int c = 0; c<COLS; c++){
//				out = out + detailedMap[r][c];
//				if(detailedMap[r][c] == STR_NORTH ||
//						detailedMap[r][c] == STR_SOUTH ||
//						detailedMap[r][c] == STR_EAST ||
//						detailedMap[r][c] == STR_WEST) {
//					game.setIlk(new Tile(r,c), Ilk.DEADEND);
//				}
//			}
//			log(out);
//		}
//		log("----");
	}
}

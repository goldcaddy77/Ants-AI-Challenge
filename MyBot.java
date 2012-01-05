import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Set;

/**
 * Scannigan Bot
 */
public class MyBot extends Bot {

    private Map<Tile, Tile> orders = new HashMap<Tile, Tile>();
    private Set<Tile> myAntTiles = new HashSet<Tile>();

    private Map<Tile, Ant> myAntLocations = new HashMap<Tile,Ant>();
    private Set<Ant> myAnts = new HashSet<Ant>();
    private Set<Ant> antOrders = new HashSet<Ant>();

    private Set<Tile> enemyHills = new HashSet<Tile>();
    private Game game;
    private int harvestorHeatMap[][];

    private Set<Tile> targets = new HashSet<Tile>();

    private FileWriter fstream;
    private BufferedWriter out;
    private PathStore ps;

    private int iCrashTurn;

    public void setup(int loadTime, int turnTime, int rows, int cols, int turns, int viewRadius2, int attackRadius2, int spawnRadius2)
    {
        super.setup(loadTime, turnTime, rows, cols, turns, viewRadius2, attackRadius2, spawnRadius2);

	    try {
			fstream = new FileWriter("out.txt");
		    out = new BufferedWriter(fstream);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        game = this.getGame();
        harvestorHeatMap = new int[game.getCols()][game.getRows()];

        for (int r = 0; r < game.getRows(); ++r) {
            for (int c = 0; c < game.getCols(); ++c) {
                harvestorHeatMap[c][r] = 0;
            }
        }

        ps = new PathStore(game);
    	this.iCrashTurn = 20;
    }

    public void removeAnt(int row, int col, int owner) {
        super.removeAnt(row, col, owner);
        if(owner == 0 && myAntLocations.containsKey(new Tile(row,col))) {

        	Ant removedAnt = myAntLocations.remove(new Tile(row,col));
        	myAnts.remove(removedAnt);

        	log("My ant {" + removedAnt + "} died at " + row + " : " + col);
        }
    }

    public void addAnt(int row, int col, int owner) {
    	super.addAnt(row, col, owner);
    	// log("adding an ant for owner " + owner + " at location " + row + " " + col);
        if(owner == 0 && !myAntLocations.containsKey(new Tile(row,col))) {
            Ant newAnt = new Ant(row,col);
            log("Adding my ant " + newAnt + " at " + row + " : " + col);
        	myAntLocations.put(new Tile(row,col), newAnt);
        	myAnts.add(newAnt);
        }
    }

    public void afterUpdate()
    {
        super.afterUpdate();
        orders.clear();
        targets.clear();
        myAntTiles = game.getMyAnts();
        for (int r = 0; r < game.getRows(); ++r) {
            for (int c = 0; c < game.getCols(); ++c) {
                harvestorHeatMap[c][r] = 0;
            }
        }
    	antOrders.clear();
    	calculateHeatMap();

    	// remove sacked enemy hills - need to use iterator when removing
    	// Also, to remove a hill, you need to check to see if the hill is in your site and
    	// the data doesn't contain it - then you know it's gone
    	for (Iterator<Tile> hillIter = enemyHills.iterator(); hillIter.hasNext(); ) {
	        Tile hill = hillIter.next();
            if (game.isVisible(hill) && !game.getEnemyHills().contains(hill)) {
	        	hillIter.remove();
    			// System.out.println("Discovered enemy hill was destroyed: " + hill);
	        }
        }

        for (Tile enemyHill : game.getEnemyHills()) {
            if (!enemyHills.contains(enemyHill)) {
                enemyHills.add(enemyHill);
                log("enemyHill: " + enemyHill);
            }
        }

    	// add new enemy hills to set
        for (Tile enemyHill : game.getEnemyHills()) {
            if (!enemyHills.contains(enemyHill)) {
                enemyHills.add(enemyHill);
                log("enemyHill: " + enemyHill);
            }
        }

        ps.emptyCache();

        log("----------------------------------------------");
        log("Beginning turn " + game.getCurrentTurn());
    }

    public void doTurn()
    {
    	if(game.getCurrentTurn() == iCrashTurn) {
    		String s = "Crash Breakpoint";
    	}

       	log("[" + game.getTimeRemaining() + "] Begin checking food distances");

        // Priority 1: food | For each visible food, send the closest ant
        SortedMap<Integer, Route> antDist = new TreeMap<Integer, Route>();
        for (Tile foodLoc : game.getFoodTiles())
        {
            for (Tile antLoc : myAntTiles)
            {
            	if(game.getTimeRemaining() < 100) { break; }
            	logcrash("[" + game.getTimeRemaining() + "] Calculating Food: " + foodLoc + " | " + antLoc);

                // Integer dist = ps.getDistance(antLoc, foodLoc);
                Integer dist = game.getDistance(antLoc, foodLoc);

                if(dist < Integer.MAX_VALUE) {
                antDist.put(dist, new Route(antLoc, foodLoc));
            }
            }
        }

       	log("[" + game.getTimeRemaining() + "] Begin issuing food routes");

       	Route routeTemp;
       	for (Map.Entry<Integer, Route> foodMap : antDist.entrySet())
       	{
        	if(game.getTimeRemaining() < 100) { break; }

       		// We don't want to go for food too far away
       		if(foodMap.getKey() > 25) {
       			// continue;
       		}
       	    routeTemp = foodMap.getValue();

            if (!targets.contains(routeTemp.end)) {
            	if(myAntLocations.containsKey(routeTemp.start)){
                	logcrash("[" + game.getTimeRemaining() + "] Trying food route: " + routeTemp.start + " | " + routeTemp.end);

            		doMoveLocation(myAntLocations.get(routeTemp.start), routeTemp.end);
            	}
            }
        }

       	log("[" + game.getTimeRemaining() + "] Begin checking hill distances");

       	// Priority 2: attack hills | Any ant not grabbing food should head to an enemy hill if we see one
        Map<Integer, Route> hillDist = new TreeMap<Integer, Route>();
        for (Tile tileHill : enemyHills)
        {
            for(Ant ant : myAnts)
            {
            	if(game.getTimeRemaining() < 100) { break; }

            	if(!antOrders.contains(ant)) {
                   	logcrash("[" + game.getTimeRemaining() + "] Checking enemy hills: Ant: " + ant + " | " + tileHill);

                   	// int dist = ps.getDistance(ant.getCurrentTile(), tileHill);
                   	int dist = game.getDistance(ant.getCurrentTile(), tileHill);

                    if(dist < Integer.MAX_VALUE) {
                    hillDist.put(dist, new Route(ant.getCurrentTile(), tileHill));
                }
            }
        }
        }

       	log("[" + game.getTimeRemaining() + "] Begin issuing hill routes");

        for (Route route : hillDist.values())
        {
        	if(game.getTimeRemaining() < 100) { break; }
           	logcrash("[" + game.getTimeRemaining() + "] Attempting hill move : " + route.start + " | " + route.end);

        	doMoveLocation(myAntLocations.get(route.start), route.end);
        }

       	log("[" + game.getTimeRemaining() + "] Begin heatmap flurry");

        // Priority 3: map coverage | Use the heat map to get optimal map coverage
        for(Ant ant : myAnts)
        {
        	if(!antOrders.contains(ant))
        	{
               	logcrash("[" + game.getTimeRemaining() + "] Attempting heatmap flurry : " + ant);
            	if(game.getTimeRemaining() < 100) { break; }

        		int lowestVal = Integer.MAX_VALUE;
        		Aim d = Aim.NORTH;
        		int x = ant.getCurrentTile().getCol();
        		int y = ant.getCurrentTile().getRow();
        		int c = game.getCols();
        		int r = game.getRows();

        		List<Aim> dir = new ArrayList<Aim>();
        		for(Aim aim: Aim.values()){
        			if(harvestorHeatMap[(c+x+ aim.getColDelta())%c][(r+y+ aim.getRowDelta())%r] < lowestVal){
        				dir.clear();
        				lowestVal = harvestorHeatMap[(c+x+aim.getColDelta())%c][(r+y+aim.getRowDelta())%r];
        				dir.add(aim);
        			} else if (harvestorHeatMap[(c+aim.getColDelta())%c][(r+aim.getRowDelta())%r] == lowestVal){
        				dir.add(aim);
        			}
        		}

        		log("have " + dir.size() + " options! And lowestval is " + lowestVal);
        		if(dir.size() > 0){
        			d = dir.get(new Random().nextInt(dir.size()));

        		}
        		// choose a random direction
        		doMoveDirection(ant,d);
        	}
        }

         // TODO: Can this be taken care of in the doMoveLocation function?
         for (Tile myHill : game.getMyHills()) {
            if (myAntTiles.contains(myHill) && !orders.containsValue(myHill)) {
                for (Aim direction : Aim.values()) {
                    if (doMoveDirection(myAntLocations.get(myHill), direction)) {
                        break;
                    }
                }
            }
        }

        log("End Turn | Remaining time: " + game.getTimeRemaining() + " | Cache size: " + ps.size());
    }

    public boolean doMoveLocation(Ant ant, Tile destLoc)
    {
    	// TODO: This should never happen, but it was happening in the play_one_game.cmd
    	//       uncomment and fix bug eventually
    	if(ant == null) {
    		return false;
    	}

    	log("Attempting to move " + ant + " | " + ant.getCurrentTile() + " to " + destLoc);

    	// If ant already has a path and the destination is the one we're trying to go to, just return the next
    	// move in the current path.  If not, recompute the path
    	Path path;
    	if((path = ant.getPath()) == null || !path.end().equals(destLoc)) {
    		path = ps.getPath(ant.getCurrentTile(), destLoc);
        	log("Path for move " + path);
    	}

        if(path != null) {
        	ant.setPath(path);
        	List<Aim> directions = game.getDirections(ant.getCurrentTile(), path.start());

        for (Aim direction : directions) {
            if (this.doMoveDirection(ant, direction)) {
                targets.add(destLoc);
                return true;
            }
        }
        }

        return false;
    }

    public boolean doMoveDirection(Ant ant, Aim direction)
    {
        // Track all moves, prevent collisions
        Tile newLoc = game.getTile(ant.getCurrentTile(), direction);
        if (game.getIlk(newLoc).isUnoccupied() && !orders.containsKey(newLoc)) {
            game.issueOrder(ant.getCurrentTile(), direction);
            orders.put(newLoc, ant.getCurrentTile());
            myAntLocations.remove(ant.getCurrentTile());
            myAntLocations.put(newLoc, ant);
            antOrders.add(ant);
            ant.move(newLoc);

            return true;
        } else {
            return false;
        }
    }

    private void calculateHeatMap(){
    	int distance = Math.min(10, Math.min(this.game.getCols(), this.game.getRows())-2);

    	for(Ant ant:myAnts){
    		for(int y = distance-1 ; y > distance * -1; y--){
    			for(int x = distance-1; x> distance * -1; x--){
    				int newcol = (game.getCols()+ x+ant.getCurrentTile().getCol()) % game.getCols();
    				int newrow = (game.getRows()+ y+ant.getCurrentTile().getRow()) % game.getRows();
    				if(game.getIlk(new Tile(newrow,newcol)).isPassable()){
    					harvestorHeatMap[newcol][newrow] += Math.pow((distance-Math.abs(x)+distance-Math.abs(y)),2);
    				} else {
    					harvestorHeatMap[newcol][newrow] = Integer.MAX_VALUE;
    				}
    			}

    		}
    	}


    	/*
		log("----------");
		for(int j = 0; j<game.getRows(); j++){
			String out = "";
			for(int i = 0; i<game.getCols(); i++){
    			out = out + " " + String.format("%02d",harvestorHeatMap[i][j]);
    		}
    		log(out);
    	}
		log("----");
    	*/
    }

    private void log(String log){
    	/*
    	try {

    	    out.write(log + "\r\n");
    	    out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
    }

    private void logcrash(String str){
    	if(game.getCurrentTurn() == this.iCrashTurn)
    		log(str);
    }

    public static void main(String[] args) throws IOException {
        new MyBot().readSystemInput();
    }
}
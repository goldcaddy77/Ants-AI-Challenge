import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
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

	private int COLS;
	private int ROWS;

    private Map<Tile, Ant> myAntLocations = new HashMap<Tile,Ant>();
    private Set<Ant> myAnts = new HashSet<Ant>();
    private Set<Ant> newAnts = new HashSet<Ant>();

    private Set<Ant> antOrders = new HashSet<Ant>();
    private Map<Ant,  Pair<Ant, Aim>> queueOrders = new HashMap<Ant,  Pair<Ant, Aim>>();
    
    private Set<Tile> enemyHills = new HashSet<Tile>();
    private Set<Integer> enemies = new HashSet<Integer>();
    private Game game;
    private int harvestorHeatMap[][];
    private int enemyHeatMap[][];
    

    private FileWriter fstream;
    private BufferedWriter out;
    private PathStore ps;
    
    private int iCrashTurn;
    private int iViewSpaces;
    
    private Map<Tile, ArrayList<Tile>> hillStartTargets = new HashMap<Tile,ArrayList<Tile>>();
    private Map<Tile, Integer> antsSentRandomlyFromLocation = new HashMap<Tile, Integer>();
    private Set<Tile> startLocations = new HashSet<Tile>();
    
    private Set<Tile> myHills = new HashSet<Tile>();

    
    
    private String detailedMap[][];

    public void setup(int loadTime, int turnTime, int rows, int cols, int turns, int viewRadius2, int attackRadius2, int spawnRadius2) 
    {
        super.setup(loadTime, turnTime, rows, cols, turns, viewRadius2, attackRadius2, spawnRadius2);

        this.iViewSpaces = (int)Math.floor(Math.sqrt((double)viewRadius2));
        
		this.ROWS = rows;
		this.COLS = cols;

	    try {
			fstream = new FileWriter("out.txt");
		    out = new BufferedWriter(fstream);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        game = this.getGame();

        harvestorHeatMap = new int[ROWS][COLS];
        enemyHeatMap = new int[ROWS][COLS];
        
        for (int r = 0; r < ROWS; ++r) {
            for (int c = 0; c < COLS; ++c) {
                harvestorHeatMap[r][c] = 0;
            }
        }

        for (int r = 0; r < ROWS; ++r) {
            for (int c = 0; c < COLS; ++c) {
            	enemyHeatMap[r][c] = 0;
            }
        }

        detailedMap = new String[ROWS][COLS];
        for (int r = 0; r < ROWS; ++r) {
            for (int c = 0; c < COLS; ++c) {
            	detailedMap[r][c] = "_";
            }
        }

        ps = new PathStore(game);
    	this.iCrashTurn = 20;
    	
    	 this.hillStartTargets = new HashMap<Tile,ArrayList<Tile>>();

         this.startLocations = new HashSet<Tile>();
         int startCols = Math.min(iViewSpaces + 2, 10);
         int startRows = startCols;
         startLocations.add(game.getTileAllowNegatives(-1 * startRows, -1 * startCols)); // NW
         startLocations.add(game.getTileAllowNegatives(-1 * startRows, startCols)); // NE
         startLocations.add(game.getTileAllowNegatives(startRows, -1 * startCols)); // SW
         startLocations.add(game.getTileAllowNegatives(startRows, startCols)); // SE
         startLocations.add(game.getTileAllowNegatives(-1 * startRows, 0)); // N
         startLocations.add(game.getTileAllowNegatives(0, -1 * startCols)); // W
         startLocations.add(game.getTileAllowNegatives(startRows, 0)); // S
         startLocations.add(game.getTileAllowNegatives(0, startCols)); // E
    	
    	
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
            log("[" + game.getTimeRemaining() + "] Adding my ant " + newAnt + " at " + row + " : " + col);
        	myAntLocations.put(new Tile(row,col), newAnt);
        	myAnts.add(newAnt);
        	newAnts.add(newAnt);
        } else if (owner != 0) {
        	// its an enemy ant!
        	enemies.add(new Integer(owner));
        		
        }
    }
    
    public void addWater(int row, int col) {
    	super.addWater(row, col);
    	log("[" + game.getTimeRemaining() + "] Adding water at " + row + " : " + col);
    	
    }

    public void beforeUpdate() 
    {
        super.beforeUpdate();
        log("----------------------------------------------");
        log("[" + game.getTimeRemaining() + "] Beginning beforeupdate " + game.getCurrentTurn());

    	
    }
    public void afterUpdate() 
    {
        super.afterUpdate();

        log("----------------------------------------------");
        log("[" + game.getTimeRemaining() + "] Beginning afterUpdate " + game.getCurrentTurn());

      

      	log("[" + game.getTimeRemaining() + "] Clear heatmaps and such");
      	

        for (int r = 0; r < ROWS; ++r) {
            for (int c = 0; c < COLS; ++c) {
                harvestorHeatMap[r][c] = 0;
            }
        }

        for (int r = 0; r < ROWS; ++r) {
            for (int c = 0; c < COLS; ++c) {
                enemyHeatMap[r][c] = 0;
            }
        }

      	log("[" + game.getTimeRemaining() + "] Calculating Heat Maps");
    	antOrders.clear();
    	queueOrders.clear();
    	calculateHeatMap();
    	calculateEnemyHeatMap();
    	calculateDeadEnds();

      	log("[" + game.getTimeRemaining() + "] Removing enemy sacked hills");
    	// remove sacked enemy hills - need to use iterator when removing
    	// Also, to remove a hill, you need to check to see if the hill is in your site and 
    	// the data doesn't contain it - then you know it's gone
    	for (Iterator<Tile> hillIter = enemyHills.iterator(); hillIter.hasNext(); ) {
	        Tile hill = hillIter.next();
            if (game.isVisible(hill) && !game.getEnemyHills().contains(hill)) {
	        	hillIter.remove();

	        	// Tell all of my ants that were ordered here that they no longer have orders
	        	for(Ant ant : myAnts) {
	            	if(ant.getDestination() != null && ant.getDestination().equals(hill)) {
	            		ant.clearPath();
	            	}
	            }	        	
	        	
    			// System.out.println("Discovered enemy hill was destroyed: " + hill);
	        }
        }    	


      	log("[" + game.getTimeRemaining() + "] Add new enemy hills");
      	
    	// add new enemy hills to set
        for (Tile enemyHill : game.getEnemyHills()) {
            if (!enemyHills.contains(enemyHill)) {
                enemyHills.add(enemyHill);
                log("enemyHill: " + enemyHill);
            }
        }
        

      	log("[" + game.getTimeRemaining() + "] Set up corner locations for my " + game.getMyHills() + " hills");
      	
        // Set up corner locations
        // TODO: figure out a better solution for this (maybe 40% instead of 33%)?
     // add my hills to set
        for (Tile myHill : game.getMyHills()) {
            if (!myHills.contains(myHill)) {
            	myHills.add(myHill);
            	log("MY NEW HILL: " + myHill);

            	this.antsSentRandomlyFromLocation.put(myHill, 0);
            	this.hillStartTargets.put(myHill, new ArrayList<Tile>());
            	for(Tile tOffset: startLocations) {
                	this.hillStartTargets.get(myHill).add(game.getTile(myHill, tOffset));
            	}
            }
        }
        ps.emptyCache();
    }

    public void doTurn() 
    {
    	

        log("----------------------------------------------");
        log("[" + game.getTimeRemaining() + "] Beginning doTurn " + game.getCurrentTurn());
      	
    	if(game.getCurrentTurn() == iCrashTurn) {
    		String s = "Crash Breakpoint";
    	}

        // Priority 0: Battle Resolution
        // how to check if an ant dies
        // for every ant:
    	//     for each enemy in range of ant (using attackadius2):
    	//         if (enemies(of ant) in range of ant) >= (enemies(of enemy) in range of enemy) then
    	//             the ant is marked dead (actual removal is done after all battles are resolved)
    	//             break out of enemy loop
    	log("[" + game.getTimeRemaining() + "] Begin checking enemies");

        Map<Ant, List<Tile>> enemyByAnt = new HashMap<Ant, List<Tile>>();
        Map<Tile,List<Ant>> antByEnemy = new HashMap<Tile,List<Ant>>();
        Set<Ant> dangerAnts = new HashSet<Ant>();
        
        log("We have " + game.getEnemyAnts().size() + " enemy ants visible and " + myAnts.size() + " ants");
         
    	for(Tile enemyAnt: game.getEnemyAnts())
    	{
    		if(game.getTimeRemaining() < 100) { break; }
    		for(Ant ant: myAnts)
    		{
    			Integer dist = game.getClosestDistanceAfterTurns(ant.getCurrentTile(), enemyAnt,2);
    			//log("looking at enemy at location " + enemyAnt + " and I'm " + dist + " away while the spawn radius is " + game.getAttackRadius2());

    			// if(dist <= game.getSpawnRadius2()+3){ // old code
    			if(dist <= game.getAttackRadius2()) 
    			{
    				log("Ant " + ant + " is dangerously close to ant at location " + enemyAnt + " (d:" + dist + ") and spanw radius squared is " + game.getAttackRadius2());
    							
    				dangerAnts.add(ant);
    				if(!enemyByAnt.containsKey(ant)){
    					enemyByAnt.put(ant,new LinkedList<Tile>());
    				}
    				enemyByAnt.get(ant).add(enemyAnt);
    				if(!antByEnemy.containsKey(enemyAnt)){
    					antByEnemy.put(enemyAnt, new LinkedList<Ant>());
    				}
    				antByEnemy.get(enemyAnt).add(ant);
    			}
    		}
    	}
    	
    	log("[" + game.getTimeRemaining() + "] Begin issuing BR moves");

    	// CURRENTLY JUST DOING "RUN AWAY!"
    	
    	for(Ant ant: dangerAnts){
    		
    		if(!antOrders.contains(ant)){
    			// find the closest ant
    			
    			int nearestFoeDistance = Integer.MAX_VALUE;
    			Tile nearestFoe = null;
    			
    			for(Tile t:  enemyByAnt.get(ant)){
    				if(game.getDistance(t, ant.getCurrentTile())<nearestFoeDistance){
    					nearestFoeDistance = game.getDistance(t, ant.getCurrentTile());
    					nearestFoe = t;
    				}						
    			}
    			

    			log("Its foe is " + nearestFoe);
    			
    			// DON'T DO CRAP!
    			
//    			List<Aim> la = game.getDirections(ant.getCurrentTile(),nearestFoe);
//    			if(la.size() > 0){
//    				Aim newDirection = Aim.getOpposite(game.getDirections(ant.getCurrentTile(),nearestFoe).get(0));
//    				doMoveDirection(ant, newDirection);
//    				ant.addLog(game.getCurrentTurn(), "Running away from from foe at " + nearestFoe);
//    				
//    			}
    			
    		}

    	}
    	
    	/*
    	THIS IS OLD CODE FOR ISSUEING BR MOVES THAT ONLY ATTACKED WHEN THERE WHERE MORE OF US THAN THEM
    	BUT IT WAS REALLY FLAKEY
    	UPDATE TO "HEAT MAP" MODE INSTEAD
    	HERE FOR REFERENCE
    	
    	//for(Map.Entry<Integer,Ant> antEntry:dangerAnts.entrySet()){
    	for(Ant ant: dangerAnts){
    		
    		// Ok, so we are looping through my ants that have the closest foe.
    		// Need to check to see how many enemy ants that this ant can see
    		// Need to check to see how many of my ants that he can see
    		
    		//Ant ant = antEntry.getValue();
    		int numberFoes = enemyByAnt.get(ant).size();
    		

    		log("This ant is under attack " + ant);
    		
    		if(!antOrders.contains(ant)){
    			
    			// find the closest ant
    			
    			int nearestFoeDistance = Integer.MAX_VALUE;
    			Tile nearestFoe = null;
    			
    			for(Tile t:  enemyByAnt.get(ant)){
    				if(game.getDistance(t, ant.getCurrentTile())<nearestFoeDistance){
    					nearestFoeDistance = game.getDistance(t, ant.getCurrentTile());
    					nearestFoe = t;
    				}						
    			}
    			

    			log("Its foe is " + nearestFoe);
    			
    			int numberAllies = antByEnemy.get(nearestFoe).size();
    			
    			
    		
    			if(numberFoes < numberAllies){
    				log("ATTACK");
    				// ATTACK
    				doMoveLocation(ant,nearestFoe);
    				for(Ant myAlly:antByEnemy.get(nearestFoe)){
    					doMoveLocation(myAlly, nearestFoe);
    					//dangerAnts.remove(myAlly);
    				}
    			} else {
    				log("RUN");
    				// Just sit there
    				
    				List<Aim> la = game.getDirections(ant.getCurrentTile(),nearestFoe);
    				if(la.size() > 0){
    					Aim newDirection = Aim.getOpposite(game.getDirections(ant.getCurrentTile(),nearestFoe).get(0));
    					doMoveDirection(ant, newDirection);
    					antOrders.add(ant);
    					for(Ant myAlly:antByEnemy.get(nearestFoe)){
    						doMoveDirection(myAlly, newDirection);
    						antOrders.add(myAlly);
    						//dangerAnts.remove(myAlly);
    					}
    				} else {
    					antOrders.add(ant);
    					for(Ant myAlly:antByEnemy.get(nearestFoe)){
    						antOrders.add(myAlly);
    						//dangerAnts.remove(myAlly);
    					}
    					
    				}
    				
    				
    			}
    		}
    	}
*/    	
    	
       	log("[" + game.getTimeRemaining() + "] Begin checking food distances");

        // Priority 1: food | For each visible food, send the closest ant

        Set<Tile> foodClaimed = new HashSet<Tile>();
        Map<Ant,Integer> antFoodDistance= new HashMap<Ant,Integer>();
        
        TreeMultiMap<Integer, Pair<Ant, Tile>> antDist = new TreeMultiMap<Integer, Pair<Ant, Tile>>();
        
        for (Tile foodLoc : game.getFoodTiles()) 
        {
    		for(Ant ant : myAnts)
            {
            	if(game.getTimeRemaining() < 100) { break; }
    			logcrash("[" + game.getTimeRemaining() + "] Calculating Food: " + foodLoc + " | " + ant.getCurrentTile());

    			Integer dist = Integer.MAX_VALUE;
    			
    			if(myAnts.size()<10)
    				dist=ps.getDistance(ant.getCurrentTile(), foodLoc);
    			else
    				dist=game.getDistance(ant.getCurrentTile(), foodLoc);
    			
                if(dist < Integer.MAX_VALUE) {
            
    				antDist.put(dist,new Pair<Ant, Tile>(ant, foodLoc));
                }
            }
        }
        
       	log("[" + game.getTimeRemaining() + "] Begin deciding possible food routes");
       	
       	// Figure out food to food distances
       	
       	// This stores the foodMove that I may want to make, ordered by length of the route
       	//SortedMap<Integer,List<Pair<Ant,Tile>>> foodMoves = new TreeMap<Integer,List<Pair<Ant,Tile>>>();
       	
       	TreeMultiMap<Integer,Pair<Ant,Tile>> foodMoves = new TreeMultiMap<Integer,Pair<Ant,Tile>>();
       	
       	for(TreeMultiMap.Entry<Integer, Pair<Ant, Tile>> foodDist : antDist.entryList()){

       		if(game.getTimeRemaining() < 100) { break; }

       		// We don't want to go for food too far away
       		if(foodDist.getKey() > 25) {
       			// continue;
       		}
       		if (!foodClaimed.contains(foodDist.getValue().second) && !antFoodDistance.containsKey(foodDist.getValue().first)) {

       			logcrash("[" + game.getTimeRemaining() + "] Trying food route: " + foodDist.getValue().first + " | " + foodDist.getValue().second);

       			/*
            		if(!foodMoves.containsKey(dist)){
            			foodMoves.put(dist,new LinkedList<Pair<Ant,Tile>>());
            		}
       			 */
       			for(TreeMultiMap.Entry<Integer,Pair<Ant,Tile>> fm: foodMoves.entryList()){
       				int distance;
       				if(myAnts.size() < 10){
       					distance = ps.getDistance(fm.getValue().second, foodDist.getValue().second);
       				} else {
       					distance = game.getDistance(fm.getValue().second, foodDist.getValue().second);
       				}

       				distance += antFoodDistance.get(fm.getValue().first)-1;

       				if(distance < foodDist.getKey()+2){
       					// EASIER TO JUST SEND THIS DUDE
       					foodClaimed.add(foodDist.getValue().second);
       					antFoodDistance.put(fm.getValue().first, distance);
       				}

       			}
       			if(!foodClaimed.contains(foodDist.getValue().second)){
       				// this food still isn't claimed, so have this guy do it.
       				foodClaimed.add(foodDist.getValue().second);
       				antFoodDistance.put(foodDist.getValue().first, foodDist.getKey());
       				foodMoves.put(foodDist.getKey(), new Pair<Ant,Tile>(foodDist.getValue().first, foodDist.getValue().second));
       				// move this guy.

       				foodDist.getValue().first.addLog(game.getCurrentTurn(), "Gonna grab me some food at  " + foodDist.getValue().second);
       				doMoveLocation(foodDist.getValue().first, foodDist.getValue().second);



       			}
       			//foodMoves.put(foodDist.getKey(),new Pair<Ant,Tile>(foodDist.getValue().first,foodDist.getValue().second));
       			//foodClaimed.add(foodDist.getValue().second);

       			//antFoodDistance.put(foodDist.getValue().first, foodDist.getKey());


       		}

       	}

       	// I loop through the ordered list of food twice, sorted by order to save work.
       	// For a given food order, i look to see if its quicker for another ant with a shorter distance to already go to pick up its food, then go to the food that I want to go to
       	// If so, I basically say that this order is null and void.
       	// Note: this isn't perfect.  Technically I should then make that ant have a longer route (last route + distance to food), but this data structure is ordered by distance, and I think changing the distance would break something
//       	log("[" + game.getTimeRemaining() + "] Culling Food Routes that aren't needed.  Checking # of food Routes: " + foodMoves.size());
//       	Set<Ant> cancelFoodOrder = new HashSet<Ant>();
//       	for(TreeMultiMap.Entry<Integer,Pair<Ant,Tile>> outerMove : foodMoves.entryList()){
//       		if(game.getTimeRemaining() < 100) { break; }
//       		log("[" + game.getTimeRemaining() + "] Checking outer food loop with distance of " + outerMove.getKey() + " and containing order " + outerMove.getValue());
//       		for(TreeMultiMap.Entry<Integer,Pair<Ant,Tile>> innerMove : foodMoves.entryList()){
//       			log("[" + game.getTimeRemaining() + "] - Checking inner food loop with distance of " + innerMove.getKey() + " and containing order " + innerMove.getValue());
//       			if(game.getTimeRemaining() < 100) { break; }
//       			if(outerMove.equals(innerMove)){
//       				break;
//       			}
//       			log("[" + game.getTimeRemaining() + "] - - Still checking that loop " + innerMove.getKey() + " and containing order " + innerMove.getValue());
//       			// Since its important to get off to a good start, I figured it would be worthwhile to do a real distance search if there aren't all that many food items to pick up
//       			// Probably should do something generic with this
//       			int distance;
//       			if(game.getFoodTiles().size() < 5){
//       				distance = ps.getDistance(innerMove.getValue().second, outerMove.getValue().second);
//       			} else {
//       				distance = game.getDistance(innerMove.getValue().second, outerMove.getValue().second);
//       			}
//       			
//       			
//       			if(antFoodDistance.containsKey(innerMove.getValue().first)){
//       				distance += antFoodDistance.get(innerMove.getValue().first);
//       			}
//       			if(innerMove.getKey() + distance < outerMove.getKey()){
//       				log("[" + game.getTimeRemaining() + "]  - - Don't bother sending ant " + outerMove.getValue().first + " to " + outerMove.getValue().second + " Because we may as well send ant #" + innerMove.getValue().first + " a location " + innerMove.getValue().second);
//       				cancelFoodOrder.add(outerMove.getValue().first);
//       				antFoodDistance.put(innerMove.getValue().first, distance);
//       				
//       			}				
//       		}
//       	}
//    
  		// Now actually pick up the food!
//  		log("[" + game.getTimeRemaining() + "] Issue Food Routes (after removing  " + cancelFoodOrder.size() + " orders)");
//  		
//  		for(TreeMultiMap.Entry<Integer,Pair<Ant,Tile>> moves : foodMoves.entryList()){
//  				if(game.getTimeRemaining() < 100) { break; }
//  				
//  				if(!cancelFoodOrder.contains(moves.getValue().first) && !antOrders.contains(moves.getValue().first)){
//  					doMoveLocation(moves.getValue().first, moves.getValue().second);
//  					moves.getValue().first.addLog(game.getCurrentTurn(), "Getting food at location " +moves.getValue().second);
//  				}
//  		}
      	
       	
      	log("[" + game.getTimeRemaining() + "] Begin checking hill distances");

       	// Priority 2: attack hills | Any ant not grabbing food should head to an enemy hill if we see one
        TreeMultiMap<Integer, Pair<Ant, Tile>> hillDist = new TreeMultiMap<Integer, Pair<Ant, Tile>>();
        for (Tile tileHill : enemyHills) 
        {
            for(Ant ant : myAnts) 
            {
            	if(game.getTimeRemaining() < 100) { break; }

            	if(!antOrders.contains(ant)) {
                   	logcrash("[" + game.getTimeRemaining() + "] Checking enemy hills: Ant: " + ant + " | " + tileHill);

                   	// int dist = ps.getDistance(ant.getCurrentTile(), tileHill);
                   	int dist = game.getDistance(ant.getCurrentTile(), tileHill);
                    
                   	// Only send ants within 100 radius
                   	if(dist < 100) {
                    	hillDist.put(dist, new Pair<Ant, Tile>(ant, tileHill));
                    }
            	}
            }
        }

       	log("[" + game.getTimeRemaining() + "] Begin issuing hill routes");

       	for (TreeMultiMap.Entry<Integer,Pair<Ant, Tile>> entry : hillDist.entryList()) 
        {
        	if(game.getTimeRemaining() < 100) { break; }
           	logcrash("[" + game.getTimeRemaining() + "] Attempting hill move : " + entry.getValue().first + " | " + entry.getValue().second);

        	if(!antOrders.contains(entry.getValue().first)) {
        		doMoveLocation(entry.getValue().first, entry.getValue().second);
        		entry.getValue().first.addLog(game.getCurrentTurn(), "Attacking hill at " + entry.getValue().second);
        	}
        }       	
       	
      	log("[" + game.getTimeRemaining() + "] Send ants to Previous destinations");

     // Priority 4a: Give new ants initial path if they would not have had one by above logic
      	int CLOSEST_ANT_TO_HILL_THRESHOLD = 100; 
      	for (Iterator<Ant> antIter = newAnts.iterator(); antIter.hasNext(); ) 
      	{
	        Ant ant = antIter.next();
	        int iClosestAnt = Integer.MAX_VALUE;
	        if(!antOrders.contains(ant)) 
	        {
	        	Tile tileClosestEnemy = null;
	        	if(game.getEnemyAnts().size() > 0) {
	        		tileClosestEnemy = game.getClosestLocation(ant.getCurrentTile(), game.getEnemyAnts());
		        	iClosestAnt = game.getDistance(ant.getCurrentTile(), tileClosestEnemy); 
	        	}
	        	
	        	if(iClosestAnt < CLOSEST_ANT_TO_HILL_THRESHOLD) {
	              	log("[" + game.getTimeRemaining() + "] [NEW ANT] Mofo is too close... sending new ant at " + tileClosestEnemy);
		         	doMoveLocation(ant, tileClosestEnemy);
	        	}
	        	else if(enemyHills.size() > 0) {
	        		Tile tileClosestHill = game.getClosestLocation(ant.getCurrentTile(), enemyHills);
	              	log("[" + game.getTimeRemaining() + "] [NEW ANT] We see a hill... send ant to " + tileClosestHill);
		         	doMoveLocation(ant, tileClosestHill);
	        	}
	        	else {
	              	log("[" + game.getTimeRemaining() + "] [NEW ANT] Sending new ant to next hill location");
	              	List<Tile> myTargets = hillStartTargets.get(ant.getCurrentTile());

	              	log("[" + game.getTimeRemaining() + "] [NEW ANT] myTargets: " + myTargets);

	              	int iNumTargetsForHill = myTargets.size();  

	              	log("[" + game.getTimeRemaining() + "] [NEW ANT] iNumTargetsfForHill: " + iNumTargetsForHill);

	              	// Get the target offset we want to use for this ant
	              	int iPrevAnts = antsSentRandomlyFromLocation.get(ant.getCurrentTile());
	              	antsSentRandomlyFromLocation.put(ant.getCurrentTile(), iPrevAnts + 1);
	        		
	              	log("[" + game.getTimeRemaining() + "] [NEW ANT] iPrevAnts: " + iPrevAnts);
	              	log("[" + game.getTimeRemaining() + "] [NEW ANT] iPrevAnts % iNumTargetsForHill: " + iPrevAnts % iNumTargetsForHill);

	              	
	              	
	              	Tile next = myTargets.get(iPrevAnts % iNumTargetsForHill);

	              	log("[" + game.getTimeRemaining() + "] [NEW ANT] Sending to corner " + next);

	              	// TODO: if this target is invalid, the ant will immediately go into heatmap mode, need to 
	              	// retry another path and remove the old one
	              	doMoveLocation(ant, next);
	        	}
	        }
         	antIter.remove();
        }
      	
       	// Priority 3: Have ants follow a path if they have one
		for(Ant ant : myAnts)
        {
			if(ant.hasPath() && !antOrders.contains(ant)) {
				doMoveLocation(ant, ant.getDestination());
			}
        }

      	log("[" + game.getTimeRemaining() + "] Begin heatmap flurry");

       	// Priority 4: map coverage | Use the heat map to get optimal map coverage
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
        		
        		List<Aim> dir = new ArrayList<Aim>();
        		for(Aim aim: Aim.values())
        		{
        			if(harvestorHeatMap[(ROWS+y+ aim.getRowDelta())%ROWS][(COLS+x+ aim.getColDelta())%COLS] < lowestVal){
        				dir.clear();
        				lowestVal = harvestorHeatMap[(ROWS+y+aim.getRowDelta())%ROWS][(COLS+x+aim.getColDelta())%COLS];
        				dir.add(aim);
        			} else if (harvestorHeatMap[(ROWS+aim.getRowDelta())%ROWS][(COLS+aim.getColDelta())%COLS] == lowestVal){
        				dir.add(aim);        				
        			}
        		}
        		
        		log("have " + dir.size() + " options! And lowestval is " + lowestVal);
        		if(dir.size() > 0){
        			d = dir.get(new Random().nextInt(dir.size()));
        		
        		}

        		// This would move the ant around walls if it hit them
        		// doMoveDirectionNextPassible(ant,d); // This will put you on a path to go around objects if you run into a wall

        		// choose a random direction
        		if(!doMoveLocation(ant, game.getTile(ant.getCurrentTile(), d, iViewSpaces))) {
        			doMoveDirection(ant, d);
        		}
        		 ant.addLog(game.getCurrentTurn(), "Doing a heatmap move.  have " + dir.size() + " options! And lowestval is " + lowestVal);
        	}
        }


      	log("[" + game.getTimeRemaining() + "] Moving guys off hill");
      	
        // TODO: Can this be taken care of in the doMoveLocation function?
        for (Tile myHill : game.getMyHills()) 
		{
    		if (myAntLocations.containsKey(myHill) && !antOrders.contains(myAntLocations.get(myHill))) 
    		{
            	Ant ant = myAntLocations.get(myHill);
                for (Aim direction : Aim.values()) 
                {

                    if (doMoveDirection(ant, direction)) 
                    {

                    	ant.addLog(game.getCurrentTurn(), "Moved myself off the hill at location " + myHill);
                        break;
                    }
                }
            }        
        }
        
        // Which ants haven't been issued an order?
      	log("[" + game.getTimeRemaining() + "] Which ants haven't been issued an order?");
		for(Ant ant : myAnts)
        {
			if(!antOrders.contains(ant)) {
				log("No orders for ant " + ant + " [" + ant.getCurrentTile() + "]");
				ant.addLog(game.getCurrentTurn(), "I wasn't told to do anything!");
			}
        }
		
		log("TURN RECAP------------------------------------------------");
		for(Ant ant: myAnts){
			log(ant.getLastLog());
			
		}
        
        log("End Turn | Remaining time: " + game.getTimeRemaining() + " | Cache size: " + ps.size());
    }
    
    public boolean doMoveLocation(Ant ant, Tile destLoc) 
    {
    	// TODO: This should never happen, but it was happening in the play_one_game.cmd
    	//       uncomment and fix bug eventually
    	if(ant == null) {
    		log("ERROR: doMoveLocation: found null ant: Destination [" + destLoc + "]");
    		return false;
    	}
    
    	if(antOrders.contains(ant)) {
    		log("ERROR: doMoveLocation: attempt to issue duplicate order for ant " + ant + " [" + ant.getCurrentTile() + "]: Destination [" + destLoc + "]");
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
        
        if(path != null) 
        {
        	ant.setPath(path);
        	List<Aim> directions = game.getDirections(ant.getCurrentTile(), path.start());
        	
	        for (Aim direction : directions) {
	            if (this.doMoveDirection(ant, direction)) {
	                return true;
	            }
	        }
        }
        
        return false;
    }

	public boolean doMoveDirectionNextPassible(Ant ant, Aim direction) {
		if(doMoveDirection(ant, direction)) {
			return true;
		}
		else {
			return doMoveLocation(ant, game.getTileNextPassible(ant.getCurrentTile(), direction));
		}
	}


    public boolean doMoveDirection(Ant ant, Aim direction) 
    {
    	if(antOrders.contains(ant)) {
    		log("ERROR: doMoveDirection: attempt to issue duplicate order for ant [" + ant.getCurrentTile() + "]: Direction [" + direction + "]");
    		return false;
    	}
    	
    	// Track all moves, prevent collisions
        Tile newLoc = game.getTile(ant.getCurrentTile(), direction);

        // isUnoccupied isn't necessarily correct - need to figure out what to put here
        if (game.getIlk(newLoc).isMovable()) 
        {
        	if(myAntLocations.containsKey(newLoc))
        	{
        		queueOrders.put(myAntLocations.get(newLoc), new Pair<Ant, Aim>(ant, direction));
        		return false;
        	}
        	else {
                game.issueOrder(ant.getCurrentTile(), direction);
                myAntLocations.remove(ant.getCurrentTile());
                myAntLocations.put(newLoc, ant);
                antOrders.add(ant);
                ant.move(newLoc);

                // Attempt to run a dependent move
                if(queueOrders.containsKey(ant)) {
                	Pair<Ant, Aim> move = queueOrders.get(ant);
                	doMoveDirection(move.first, move.second);
                	queueOrders.remove(ant);
                }
                
                return true;
        	}

        } else {
            return false;
        }
    }
        
    private void calculateHeatMap()
    {
    	// int distance = Math.min(20, Math.min(this.COLS, ROWS)-2);
    	int distance = (int) Math.min(Math.floor(Math.sqrt(game.getViewRadius2())), 10);
    	
    	// log("Heatmap Distance: " + distance);

    	for(Ant ant:myAnts)
    	{
    		for(int y = distance-1 ; y > distance * -1; y--)
    		{
    			for(int x = distance-1; x > distance * -1; x--)
    			{
    				int newcol = (COLS+ x+ant.getCurrentTile().getCol()) % COLS;
    				int newrow = (ROWS+ y+ant.getCurrentTile().getRow()) % ROWS;
    				if(game.getIlk(new Tile(newrow, newcol)).isMovable()){
    					
//    			    	log("sqrt: "+ Math.sqrt((Math.pow(distance-Math.abs(x), 2) + Math.pow(distance-Math.abs(y), 2))));
    					
    					// harvestorHeatMap[newcol][newrow] += (int)Math.floor(Math.sqrt((Math.pow(distance - 1 - Math.abs(x), 2) + Math.pow(distance - 1 - Math.abs(y), 2))));
    					harvestorHeatMap[newrow][newcol] += distance + 2 - Math.min(Math.abs(x) + Math.abs(y), distance + 2);
    				
    				} else {
    					harvestorHeatMap[newrow][newcol] = Integer.MAX_VALUE;
    				}
    			}
    		}
    	}

//    	distance = 3;
//    	for(Tile waterTile:game.getWaterTiles()){
//    		for(int y = distance-1 ; y > distance * -1; y--){
//    			for(int x = distance-1; x> distance * -1; x--){
//    				int newcol = (COLS+ x+waterTile.getCol()) % COLS;
//    				int newrow = (ROWS+ y+waterTile.getRow()) % ROWS;
//    				if(game.getIlk(new Tile(newrow,newcol)).isPassable()){
//    					// harvestorHeatMap[newcol][newrow] += Math.pow((distance-Math.abs(x)+distance-Math.abs(y)),2);
//    					 harvestorHeatMap[newcol][newrow] += 1;
//    				} else {
//    					harvestorHeatMap[newcol][newrow] = -1;
//    				}
//    			}
//    			
//    		}
//    	}
    	
//    	//    	/*
//		log("----------");
//		for(int r = 0; r<ROWS; r++){
//			String out = "";
//			for(int c = 0; c<COLS; c++){
//    			out = out + " " + ((harvestorHeatMap[r][c] == Integer.MAX_VALUE) ? "-1" : String.format("%02d",harvestorHeatMap[r][c]));
//    		}
//    		log(out);
//    	}
//		log("----");
//		//    	*/
    }

    private void calculateEnemyHeatMap()
    {
    	int distance = 15;
    	
    	// log("Heatmap Distance: " + distance);

    	for(Tile tileEnemy: game.getEnemyAnts())
    	{
    		for(int y = distance-1 ; y > distance * -1; y--)
    		{
    			for(int x = distance-1; x > distance * -1; x--)
    			{
    				int newcol = (COLS+ x + tileEnemy.getCol()) % COLS;
    				int newrow = (ROWS+ y + tileEnemy.getRow()) % ROWS;
    				if(game.getIlk(new Tile(newrow, newcol)).isMovable()){
    					enemyHeatMap[newrow][newcol] += distance + 2 - Math.min(Math.abs(x) + Math.abs(y), distance + 2);
    				
    				} else {
    					enemyHeatMap[newrow][newcol] = Integer.MAX_VALUE;
    				}
    			}
    		}
    	}

//    	//    	/*
//		log("----------");
//		for(int r = 0; r<ROWS; r++){
//			String out = "";
//			for(int c = 0; c<COLS; c++){
//    			out = out + " " + ((enemyHeatMap[r][c] == Integer.MAX_VALUE) ? "-1" : String.format("%02d",enemyHeatMap[r][c]));
//    		}
//    		log(out);
//    	}
//		log("----");
//		//    	*/

    	//    	/*
//		log("----------");
//		for(int r = 0; r<ROWS; r++){
//			String out = "";
//			for(int c = 0; c<COLS; c++){
//				int diff = 0;
//				if(!(harvestorHeatMap[r][c] == Integer.MAX_VALUE) && enemyHeatMap[r][c] > harvestorHeatMap[r][c]) {
//					diff = enemyHeatMap[r][c] - harvestorHeatMap[r][c];
//				}
//				
//				if(game.getMyHills().contains(new Tile(r, c))) {
//					out = out + " BB";
//				}
//				else {
//					out = out + " " + ((diff > 1000000) ? "-1" : String.format("%02d",diff));
//				}
//    		}
//    		log(out);
//    	}
//		log("----");
		//    	*/
    
    
    }
    
    
    
    
	private void calculateDeadEnds() {
		// Looks for segments > 2 in length, then look to see if they trap tiles
		// on either side
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
		for (int r = 0; r < ROWS; ++r) {
			bWorkingOnPath = false; // need to special handle wrapping around
									// the board, so keep track of whether we're
									// working on a path

			// require wrap if both the first and last spaces are not passable
			// (water)
			// bRequiresWrap = !game.isPassible(r, 0) && !game.isPassible(r,
			// COLS - 1);
			bRequiresWrap = !game.getIlk(new Tile(r, 0)).equals(Ilk.WATER)
					&& !game.getIlk(new Tile(r, COLS - 1)).equals(Ilk.WATER);
			bSkippedBeginning = false;
			bAllWater = true;

			for (int c = 0; c < COLS || (bWorkingOnPath && !bAllWater); ++c) {
				int c2 = c % COLS;
				if (game.isDiscovered(new Tile(r, c2))
						&& detailedMap[r][c2] == STR_UNKNOWN) {
					detailedMap[r][c2] = STR_LAND;
				}

				if (game.getIlk(new Tile(r, c2)).equals(Ilk.WATER)) {
					// skip if you require wrap and haven't skipped the
					// beginning yet
					if (!bRequiresWrap || bSkippedBeginning) {
						if (game.getIlk(new Tile(r, c2)).equals(Ilk.WATER)) {
							detailedMap[r][c2] = STR_WATER;
						}
						iSegmentLen = (iSegmentStart == Integer.MAX_VALUE) ? 1
								: ++iSegmentLen;
						iSegmentStart = (iSegmentStart == Integer.MAX_VALUE) ? c2
								: iSegmentStart;
					}
				} else {
					bAllWater = false;
					bWorkingOnPath = false;
					bSkippedBeginning = true;
					if (iSegmentLen > 0) {
						Tile tileStart = new Tile(r, iSegmentStart);

						iFirstNorth = Integer.MAX_VALUE - 100;
						iLastNorth = -1;

						// don't just look for the first water tile parallel to
						// the water segment
						// look one to each side, because those water tiles
						// would make the rest of
						// the inner tiles unneeded (see tutorial map for
						// example)
						for (int i = -1; i < iSegmentLen + 1; ++i) {
							Tile t = game.getTile(tileStart, -1, i);
							bThisTileWater = game.getIlk(t).equals(Ilk.WATER);
							if (bThisTileWater) {
								iFirstNorth = Math.min(iFirstNorth, i);
								iLastNorth = Math.max(iLastNorth, i);
							}
						}

						if (iLastNorth - iFirstNorth > 1) {
							for (int i = iFirstNorth + 1; i < iLastNorth; ++i) {
								Tile t = game.getTile(tileStart, -1, i);
								if (detailedMap[t.getRow()][t.getCol()] != STR_WATER) {
									detailedMap[t.getRow()][t.getCol()] = STR_NORTH;
									// game.setIlk(t, Ilk.DEADEND);
								}
							}
						}

						iFirstSouth = Integer.MAX_VALUE - 100;
						iLastSouth = -1;

						for (int i = -1; i < iSegmentLen + 1; ++i) {
							Tile t = game.getTile(tileStart, 1, i);
							bThisTileWater = game.getIlk(t).equals(Ilk.WATER);
							if (bThisTileWater) {
								iFirstSouth = Math.min(iFirstSouth, i);
								iLastSouth = Math.max(iLastSouth, i);
							}
						}

						if (iLastSouth - iFirstSouth > 1) {
							for (int i = iFirstSouth + 1; i < iLastSouth; ++i) {
								Tile t = game.getTile(tileStart, 1, i);
								if (detailedMap[t.getRow()][t.getCol()] != STR_WATER) {
									detailedMap[t.getRow()][t.getCol()] = STR_SOUTH;
									// game.setIlk(t, Ilk.DEADEND);
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

		// Looks for segments > 2 in length, then look to see if they trap tiles
		// on either side
		int iFirstWest, iLastWest, iFirstEast, iLastEast;
		iSegmentStart = Integer.MAX_VALUE;
		iSegmentLen = -1;

		bWorkingOnPath = false;
		bSkippedBeginning = true;
		bAllWater = true;
		for (int c = 0; c < COLS; ++c) {
			bWorkingOnPath = false; // need to special handle wrapping around
									// the board, so keep track of whether we're
									// working on a path

			// require wrap if both the first and last spaces are not passable
			// (water)
			bRequiresWrap = !game.getIlk(new Tile(0, c)).equals(Ilk.WATER)
					&& !game.getIlk(new Tile(ROWS - 1, c)).equals(Ilk.WATER);
			bSkippedBeginning = false;
			bAllWater = true;

			for (int r = 0; r < ROWS || (bWorkingOnPath && !bAllWater); ++r) {
				int r2 = r % ROWS;
				if (game.isDiscovered(new Tile(r2, c))
						&& detailedMap[r2][c] == STR_UNKNOWN) {
					detailedMap[r2][c] = STR_LAND;
				}

				if (game.getIlk(new Tile(r2, c)).equals(Ilk.WATER)) {
					bWorkingOnPath = true;
					// skip if you require wrap and haven't skipped the
					// beginning yet
					if (!bRequiresWrap || bSkippedBeginning) {
						if (game.getIlk(new Tile(r2, c)).equals(Ilk.WATER)) {
							detailedMap[r2][c] = STR_WATER;
						}

						iSegmentLen = (iSegmentStart == Integer.MAX_VALUE) ? 1
								: ++iSegmentLen;
						iSegmentStart = (iSegmentStart == Integer.MAX_VALUE) ? r2
								: iSegmentStart;
					}
				} else {
					bWorkingOnPath = false;
					bSkippedBeginning = true;
					bAllWater = false;

					if (iSegmentLen > 0) {
						Tile tileStart = new Tile(iSegmentStart, c);

						iFirstWest = Integer.MAX_VALUE - 100;
						iLastWest = -1;

						// don't just look for the first water tile parallel to
						// the water segment
						// look one to each side, because those water tiles
						// would make the rest of
						// the inner tiles unneeded (see tutorial map for
						// example)
						for (int i = -1; i < iSegmentLen + 1; ++i) {
							Tile t = game.getTile(tileStart, i, -1);
							bThisTileWater = game.getIlk(t).equals(Ilk.WATER);
							if (bThisTileWater) {
								iFirstWest = Math.min(iFirstWest, i);
								iLastWest = Math.max(iLastWest, i);
							}
						}

						if (iLastWest - iFirstWest > 1) {
							for (int i = iFirstWest + 1; i < iLastWest; ++i) {
								Tile t = game.getTile(tileStart, i, -1);
								if (detailedMap[t.getRow()][t.getCol()] != STR_WATER) {
									detailedMap[t.getRow()][t.getCol()] = STR_WEST;
									// game.setIlk(t, Ilk.DEADEND);
								}
							}
						}

						iFirstEast = Integer.MAX_VALUE - 100;
						iLastEast = -1;

						for (int i = -1; i < iSegmentLen + 1; ++i) {
							Tile t = game.getTile(tileStart, i, 1);
							bThisTileWater = game.getIlk(t).equals(Ilk.WATER);
							if (bThisTileWater) {
								iFirstEast = Math.min(iFirstEast, i);
								iLastEast = Math.max(iLastEast, i);
							}
						}

						if (iLastEast - iFirstEast > 1) {
							for (int i = iFirstEast + 1; i < iLastEast; ++i) {
								Tile t = game.getTile(tileStart, i, 1);
								if (detailedMap[t.getRow()][t.getCol()] != STR_WATER) {
									detailedMap[t.getRow()][t.getCol()] = STR_EAST;
									// game.setIlk(t, Ilk.DEADEND);
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

//		log("----------");
//		for (int r = 0; r < ROWS; r++) {
//			String out = "";
//			for (int c = 0; c < COLS; c++) {
//				out = out + detailedMap[r][c];
//				if (detailedMap[r][c] == STR_NORTH
//						|| detailedMap[r][c] == STR_SOUTH
//						|| detailedMap[r][c] == STR_EAST
//						|| detailedMap[r][c] == STR_WEST) {
//					game.setIlk(new Tile(r, c), Ilk.DEADEND);
//				}
//			}
//			log(out);
//		}
//		log("----");
	}
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
    	boolean bHasLand = false;
	    for (int r = 0; r < ROWS; ++r)
	    {
	    	bWorkingOnPath = false; // need to special handle wrapping around the board, so keep track of whether we're working on a path

	    	// require wrap if both the first and last spaces are not passable (water)
	    	// bRequiresWrap = !game.isPassible(r, 0) && !game.isPassible(r, COLS - 1);
	    	bRequiresWrap = !game.getIlk(new Tile(r, 0)).equals(Ilk.WATER) && !game.getIlk(new Tile(r, COLS - 1)).equals(Ilk.WATER);
	    	bSkippedBeginning = false;

	    	for (int c = 0; c < COLS || (bWorkingOnPath && bHasLand); ++c)
	        {
	    		int c2 = c % COLS;
            	if(game.isDiscovered(new Tile(r, c2)) && detailedMap[r][c2] == STR_UNKNOWN) {
	            	detailedMap[r][c2] = STR_LAND;
            	}

	    		if(game.getIlk(new Tile(r, c2)).equals(Ilk.WATER)) {
	    			// skip if you require wrap and haven't skipped the beginning yet
	    			if(!bRequiresWrap || bSkippedBeginning) {
		            	if(game.getIlk(new Tile(r, c2)).equals(Ilk.WATER)) {
		            		detailedMap[r][c2] = STR_WATER;
		            	}
		            	iSegmentLen = (iSegmentStart == Integer.MAX_VALUE) ? 1 : ++iSegmentLen;
		            	iSegmentStart = (iSegmentStart == Integer.MAX_VALUE) ? c2 : iSegmentStart;
	            	}
	            }
	        	else
	        	{
	        		bHasLand = true;
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
	                		Tile t = game.getTile(tileStart, -1, i);
	        				bThisTileWater = game.getIlk(t).equals(Ilk.WATER);
	                		if(bThisTileWater) {
	                			iFirstNorth = Math.min(iFirstNorth, i);
	                   			iLastNorth = Math.max(iLastNorth, i);
	                		}
	               		}

	                	if(iLastNorth - iFirstNorth > 1) {
	                    	for (int i=iFirstNorth+1; i < iLastNorth; ++i) {
		                		Tile t = game.getTile(tileStart, -1, i);
	                    		if(detailedMap[t.getRow()][t.getCol()] != STR_WATER) {
	                    			detailedMap[t.getRow()][t.getCol()] = STR_NORTH;
	                    			// game.setIlk(t, Ilk.DEADEND);
	                    		}
	                    	}
	                	}

	        			iFirstSouth = Integer.MAX_VALUE - 100;
	        			iLastSouth = -1;

	                	for (int i=-1; i<iSegmentLen+1; ++i)
	                	{
	                		Tile t = game.getTile(tileStart, 1, i);
	        				bThisTileWater = game.getIlk(t).equals(Ilk.WATER);
	                		if(bThisTileWater) {
	                			iFirstSouth = Math.min(iFirstSouth, i);
	                			iLastSouth = Math.max(iLastSouth, i);
	                		}
	               		}

	                	if(iLastSouth - iFirstSouth > 1) {
	                    	for (int i=iFirstSouth+1; i < iLastSouth; ++i) {
		                		Tile t = game.getTile(tileStart, 1, i);
	                    		if(detailedMap[t.getRow()][t.getCol()] != STR_WATER) {
	                    			detailedMap[t.getRow()][t.getCol()] = STR_SOUTH;
	                    			// game.setIlk(t, Ilk.DEADEND);
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
		bHasLand = false;
    	for (int c = 0; c < COLS; ++c)
	    {
	    	bWorkingOnPath = false; // need to special handle wrapping around the board, so keep track of whether we're working on a path

	    	// require wrap if both the first and last spaces are not passable (water)
	    	bRequiresWrap = !game.getIlk(new Tile(0, c)).equals(Ilk.WATER) && !game.getIlk(new Tile(ROWS - 1, c)).equals(Ilk.WATER);
	    	bSkippedBeginning = false;

	    	for (int r = 0; r < ROWS || (bWorkingOnPath && bHasLand); ++r)
	        {
	    		int r2 = r % ROWS;
            	if(game.isDiscovered(new Tile(r2, c)) && detailedMap[r2][c] == STR_UNKNOWN) {
	            	detailedMap[r2][c] = STR_LAND;
            	}

	    		if(game.getIlk(new Tile(r2, c)).equals(Ilk.WATER)) {
	    			bWorkingOnPath = true;
	    			// skip if you require wrap and haven't skipped the beginning yet
	    			if(!bRequiresWrap || bSkippedBeginning) {
		            	if(game.getIlk(new Tile(r2, c)).equals(Ilk.WATER)) {
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
	         		bHasLand = true;
	         	   
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
	                		Tile t = game.getTile(tileStart, i, -1);
	        				bThisTileWater = game.getIlk(t).equals(Ilk.WATER);
	                		if(bThisTileWater) {
	                			iFirstWest = Math.min(iFirstWest, i);
	                   			iLastWest = Math.max(iLastWest, i);
	                		}
	               		}

	            	    if(iLastWest - iFirstWest > 1) {
	                    	for (int i=iFirstWest+1; i < iLastWest; ++i) {
		                		Tile t = game.getTile(tileStart, i, -1);
	                    		if(detailedMap[t.getRow()][t.getCol()] != STR_WATER) {
	                    			detailedMap[t.getRow()][t.getCol()] = STR_WEST;
	                    			// game.setIlk(t, Ilk.DEADEND);
	                    		}
	                    	}
	                	}

	        			iFirstEast = Integer.MAX_VALUE - 100;
	        			iLastEast = -1;

	                	for (int i=-1; i<iSegmentLen+1; ++i)
	                	{
	                		Tile t = game.getTile(tileStart, i, 1);
	        				bThisTileWater = game.getIlk(t).equals(Ilk.WATER);
	                		if(bThisTileWater) {
	                			iFirstEast = Math.min(iFirstEast, i);
	                			iLastEast = Math.max(iLastEast, i);
	                		}
	               		}

	                	if(iLastEast - iFirstEast > 1) {
	                    	for (int i=iFirstEast+1; i < iLastEast; ++i) {
		                		Tile t = game.getTile(tileStart, i, 1);
	                    		if(detailedMap[t.getRow()][t.getCol()] != STR_WATER) {
	                    			detailedMap[t.getRow()][t.getCol()] = STR_EAST;
	                    			// game.setIlk(t, Ilk.DEADEND);
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
    
    private void log(String log){
    	try {

    	    out.write(log + "\r\n");
    	    out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    private void logcrash(String str){
    	if(game.getCurrentTurn() == this.iCrashTurn)
    		log(str);
    }
    
    public static void main(String[] args) throws IOException {
        new MyBot().readSystemInput();
    }    
}
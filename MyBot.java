import java.io.IOException;
import java.util.ArrayList;
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
public class MyBot extends Bot 
{
    // Tracking Order
    private Set<Ant> antOrders = new HashSet<Ant>();
    private Map<Ant,  Pair<Ant, Aim>> queueOrders = new HashMap<Ant,  Pair<Ant, Aim>>();
    
    // Food
    private int iViewSpaces;
    private Set<Tile> foodClaimed = new HashSet<Tile>();
    private Offset offFlurry;
    private HeatMap hmFlurry;
    private Offset offWater;
    private HeatMap hmWater;
    
    // Hill Defense
    private int HILL_DEFENSE_RADIUS;
    private Set<Tile> enemyDefenseClaimed = new HashSet<Tile>();
    private Offset offHillDefense;
    private HeatMap hmHillDefense;

    // Start Locations
    private Map<Tile, ArrayList<Tile>> hillStartTargets;
    private Map<Tile, Integer> antsSentRandomlyFromLocation = new HashMap<Tile, Integer>();
    private Set<Tile> startLocations = new HashSet<Tile>();

    // Battle Resolution
    private Offset offHelp;
    // private HeatMap hmEnemyProposedNextAttack;
    // private HeatMap hmEnemyAttackDelta;
    
    // Path store
    private PathStore ps;

    public void setup(int loadTime, int turnTime, int rows, int cols, int turns, int viewRadius2, int attackRadius2, int spawnRadius2) 
    {
    	super.setup(loadTime, turnTime, rows, cols, turns, viewRadius2, attackRadius2, spawnRadius2);

        iViewSpaces = (int)Math.floor(Math.sqrt((double)viewRadius2));
        HILL_DEFENSE_RADIUS = Math.min(game.getViewRadius2(), 100);

        // Set up and offsets
        offFlurry = new Offset(169, map);
        hmFlurry = new HeatMap(map, offFlurry, true);

        // Set up water heatmap to push the flurry away from water
        offWater= new Offset(9, map);
        hmWater = new HeatMap(map, offWater, true);
        
        // TODO: figure out best range for hill defense
        offHillDefense = new Offset(HILL_DEFENSE_RADIUS, map);
        hmHillDefense = new HeatMap(map, offHillDefense);

        // TODO: figure out of we should ask for help from more than 3 spaces away
        offHelp= new Offset(9, map);

        // this.hmEnemyProposedNextAttack = new HeatMap(this, offAttack, false);
        // this.hmEnemyAttackDelta = new HeatMap(this, offAttack, false);

        // Send new ants to start locations around the hill
        int startCols = Math.min(iViewSpaces + 2, 10);
        int startRows = startCols;
        startLocations = new HashSet<Tile>();
        startLocations.add(map.getTileAllowNegatives(-1 * startRows, -1 * startCols)); // NW
        startLocations.add(map.getTileAllowNegatives(-1 * startRows, startCols)); // NE
        startLocations.add(map.getTileAllowNegatives(startRows, -1 * startCols)); // SW
        startLocations.add(map.getTileAllowNegatives(startRows, startCols)); // SE
        startLocations.add(map.getTileAllowNegatives(-1 * startRows, 0)); // N
        startLocations.add(map.getTileAllowNegatives(0, -1 * startCols)); // W
        startLocations.add(map.getTileAllowNegatives(startRows, 0)); // S
        startLocations.add(map.getTileAllowNegatives(0, startCols)); // E
        // log("StartLocations: " + startLocations);
        
        ps = new PathStore(map);
    }
    
    public void removeAnt(int row, int col, int owner) {
        super.removeAnt(row, col, owner);
        // AntMap object does everything now
    }
    
    public void addAnt(int row, int col, int owner) {
    	super.addAnt(row, col, owner);
    	// AntMap object does everything now
    }

    public void afterUpdate() 
    {
        super.afterUpdate();
        map.afterUpdate();
        
        log("----------------------------------------------");
        log("Beginning turn " + game.getCurrentTurn());

        foodClaimed.clear();
    	antOrders.clear();
    	queueOrders.clear();
    	enemyDefenseClaimed.clear();
    	// enemyMoves.clear();
    	// newEnemyLocations.clear();

    	hmFlurry.calculate(map.getAllFriendlyAntTiles(), map.getUnnavigableTiles());
    	hmFlurry.print();
    	
    	hmWater.calculate(map.getUnnavigableTiles() , new HashSet<Tile>());
    	hmWater.print();
    	
    	hmFlurry.add(hmWater);
    	hmFlurry.print();
    	
    	
    	hmHillDefense.copyMapFrom(map.getEnemyVisionHeatMap());
    	hmHillDefense.subtract(map.getMyVisionHeatMap());
    	hmHillDefense.addImportance(map.getFriendlyHills(), offHillDefense);
        // this.hmHillDefense.print();
    	
    	// For all new hills added to the map, give them targets
        // On the first turn
    	if(hillStartTargets == null)
    	{
	    	log("Updating hillStartTargets");
    		hillStartTargets = new HashMap<Tile,ArrayList<Tile>>();
	    	
	    	for (Tile myHill : map.getFriendlyHills()) 
	        {
		    	log("Found: " + myHill);
	    		
	    		this.antsSentRandomlyFromLocation.put(myHill, 0);
	        	this.hillStartTargets.put(myHill, new ArrayList<Tile>());
	        	for(Tile tOffset: startLocations) {
	            	this.hillStartTargets.get(myHill).add(map.getTile(myHill, tOffset));
	        	}
	        }    	
    	}
    	
        // TODO: Do we want to empty the cache
        ps.emptyCache();
    }

    public void doTurn() 
    {
    	if(game.getCurrentTurn() == game.getCrashTurn()) {
    		String s = "Crash Breakpoint";
    	}
    	
        // Priority 0: Battle Resolution
    	log("[" + game.getTimeRemaining() + "] Begin Battle Resolution");

		int heat;

		// Loop through all ants that are on a tile that the enemy can attack next turn
       	for (Map.Entry<Ant, Set<Tile>> mapMeAndEnemies : map.getMyAntToCloseByEnemies().entrySet())
       	{
           	Ant ant = mapMeAndEnemies.getKey();

			int heatOnMe = map.getEnemyNextAttackHeat(ant);
			log("Ant " + ant + " can be attacked by " + heatOnMe + " enemies next turn ");

   			Tile tileClosestEnemy = getClosestEnemy(ant);
   			int heatOnEnemy = map.getMyNextAttackHeat(tileClosestEnemy);

   			log("Enemy " + tileClosestEnemy + " can be attacked by " + heatOnEnemy + " enemies next turn ");

			if(heatOnMe < heatOnEnemy) 
			{
				// TODO: make sure these dudes haven't been given orders already 
				// 1 1E1 1 
				 // 1 1 1 1 
				 // 1 1 1 1 
				 // 0A1 1A1 

				log("BR Decision: ATTACK!!!");
				
				// I want to attack, but I should be smarter about what tiles I go to
				for(Ant attacker: map.getMyAntsInEnemyAttackRange(tileClosestEnemy)) {
					for(Aim a: map.getDirectionsMostAggressive(attacker.getCurrentTile(), tileClosestEnemy, new Tile(0,0))) {
						attacker.addLog(game.getCurrentTurn(), "Battle Resolution: found enemy with less heat than I have, attack in direction: " + a);;
						doMoveDirection(attacker, a);
					}
				}
			}
			else if(heatOnMe > heatOnEnemy) 
			{
				log("BR Decision: RUN!");
				
				List<Aim> dirOpposites = map.getDirectionsMostAggressive(tileClosestEnemy, ant.getCurrentTile(), new Tile(0, 0));

				log("Dir opposites: " + dirOpposites);

				for(Aim aim: dirOpposites) {
					ant.addLog(game.getCurrentTurn(), "Battle Resolution: taking too much heat... move in opposite direction");
					if(doMoveDirection(ant, aim)) {
						log("My ant " + ant + " ran away from " + tileClosestEnemy + " by moving " + aim);
						break;
					}
				}
			}
			else {
				// TODO: if no help, look for safe moves by looking at enemy next move heatmap
				// Pick "best" safe move depending on what you're trying to do
				log("BR Decision: Even heat, ask for help");

				Ant helper = getClosestHelp(ant, tileClosestEnemy);
				
				// If help was found, join forces
				if(helper != null) 
				{
					log("Found help: " + helper);

					Set<Ant> attackGroup = new HashSet<Ant>();
					attackGroup.add(ant);
					attackGroup.add(helper);
					// moveToBorders(attackGroup, hmEnemyNextAttack);

					// Ok, Assuming we got here, we decided we can't kill the enemy on this turn.
					// First move the ant to a safe spot
					if(map.isEnemyNextAttackBorder(ant.getCurrentTile())) {
						log("Ant is already on a border, telling him to stay put");
						ant.addLog(game.getCurrentTurn(), "Battle Resolution: equal heat and this ant is on a border - stay put");
						antOrders.add(ant);
					}
					else 
					{
						Set<Tile> tileMoves = map.findEnemyNextAttackBorders(helper.getCurrentTile());

						for(Tile tile: tileMoves) {
							helper.addLog(game.getCurrentTurn(), "Battle Resolution: attempting to move helper to border by going to tile " + tile);
							if(doMoveDirection(helper, map.getDirection(ant.getCurrentTile(), tile))) {
								log("Helper " + ant + "successfully moved to border by moving to tile " + tile);
								break;
							}
						}
						
						if(!antOrders.contains(ant)) {
							ant.addLog(game.getCurrentTurn(), "Battle Resolution: couldn't find a border, so just headed towards " + helper);
							log("Couldn't find a border, so just sent " + ant + " towards" + helper);
							doMoveLocation(ant, helper.getCurrentTile());
						}
					}
					
					// If helper is on a border, stay put
					// TODO: this will work for now, but we want to get the helper as close to our ant
					if(map.isEnemyNextAttackBorder(helper.getCurrentTile())) {
						helper.addLog(game.getCurrentTurn(), "Battle Resolution: helper is already on a border, telling him to stay put");
						log("Helper is already on a border, telling him to stay put");
						antOrders.add(helper);
					}
					else 
					{
						Set<Tile> tileMoves = map.findEnemyNextAttackBorders(helper.getCurrentTile());

						for(Tile tile: tileMoves) {
							helper.addLog(game.getCurrentTurn(), "Attempting to move helper to border");
							
							if(doMoveDirection(helper, map.getDirection(helper.getCurrentTile(), tile))) {
								log("Helper " + helper + "Battle Resolution: successfully moved to border by moving to tile " + tile);
								break;
							}
						}
						
						if(!antOrders.contains(helper)) {
							helper.addLog(game.getCurrentTurn(), "Battle Resolution: couldnt find a border, so just headed towards" + ant);
							log("Couldn't find a border, so just sent " + helper + " towards" + ant);
							doMoveLocation(helper, ant.getCurrentTile());
						}
					}

//					 2 2 1 1 1 0 
//					 2 2 1 1 1 1 
//					 2 2E1 1 1 1 
//					 2 1 1 1 1 1 
//					 1 1 1 1 1 0 
//					 0 1 1 1A0 0 
//					 0 0 0 0A0 0 
//					 0 0 0 0 0 0 


					
				}
				// Run away
				else {
					log("No help found, run away");
					
					// TODO: This should be smart about running away, but this is fine for now
					// to get opposite directions, just aim the enemy at me
					List<Aim> dirOpposites = map.getDirections(tileClosestEnemy, ant.getCurrentTile());

					ant.addLog(game.getCurrentTurn(), "Battle Resolution: couldn't find help, so I tried to run away");
					for(Aim aim: dirOpposites) {
						if(doMoveDirection(ant, aim)) {
							log("My ant " + ant + " ran away from " + tileClosestEnemy + " by moving " + aim);
							break;
						}
					}
				}
			}
   			
//    		int enemyheat = hmNextMyAttackVsEnemyDelta.getHeat(tileClosestEnemy);
//    			
//    		if(enemyheat > 0) {
//        		log("Enemy at at " + tileClosestEnemy + " is fucked, sending: " + mapEnemyToMyCloseByAnts.get(tileClosestEnemy));
//    			moveAnts(mapEnemyToMyCloseByAnts.get(tileClosestEnemy), tileClosestEnemy);
//    		}
//    		else {
//    		}
    	}
    	
    	// Priority 1: Hill Defense
    	log("[" + game.getTimeRemaining() + "] Begin hill defense");
    	
    	// Only need to defend the hill if our hill defense HeatMap popped up an issue
    	SortedMap<Integer, Pair<Ant, Tile>> enemyDist = new TreeMap<Integer, Pair<Ant, Tile>>();
    	if(hmHillDefense.hasHeat())
    	{
	        for (Tile enemyAnt: map.getAllEnemyAntTiles()) {
	        	Integer distFromHill = map.getDistance(new Tile(9, 65), enemyAnt);
	        	if(distFromHill < HILL_DEFENSE_RADIUS) {
		    		for(Ant ant : map.getAllFriendlyAnts()) {
		            	if(game.getTimeRemaining() < 100) { break; }

		    			Integer dist = map.getDistance(ant.getCurrentTile(), enemyAnt);

		                if(dist < Integer.MAX_VALUE) {
		                	enemyDist.put(dist, new Pair<Ant, Tile>(ant, enemyAnt));
		                }
		            }
	        	}
	        }    		
    	}

       	for (Map.Entry<Integer, Pair<Ant, Tile>> closeEnemyMap : enemyDist.entrySet())
       	{
        	if(game.getTimeRemaining() < 100) { break; }

           	Pair<Ant, Tile> pairTemp = closeEnemyMap.getValue();
        	
    		if (!enemyDefenseClaimed.contains(pairTemp.second)) 
    		{
            	if(!antOrders.contains(pairTemp.first))
            	{
            		pairTemp.first.addLog(game.getCurrentTurn(), "Hill Defense: We're under attack: " + pairTemp.first.getCurrentTile() + " | " +  pairTemp.second);
            		
    				if(doMoveLocation(pairTemp.first, pairTemp.second)) {
    			    	log("[" + game.getTimeRemaining() + "] We're under attack: " + pairTemp.first.getCurrentTile() + " | " +  pairTemp.second);
    					enemyDefenseClaimed.add(pairTemp.second);
    				}
            	}
            }
        }
    	
    	log("[" + game.getTimeRemaining() + "] Begin checking food distances");

        // Priority 2: food | For each visible food, send the closest ant
        Set<Tile> foodClaimed = new HashSet<Tile>();
        Map<Ant,Integer> antFoodDistance= new HashMap<Ant,Integer>();
        
        TreeMultiMap<Integer, Pair<Ant, Tile>> antDist = new TreeMultiMap<Integer, Pair<Ant, Tile>>();

        for (Tile foodLoc : map.getAllFoodTiles()) 
        {
    		for(Ant ant : map.getAllFriendlyAnts())
            {
            	if(game.getTimeRemaining() < 100) { break; }
    			logcrash("[" + game.getTimeRemaining() + "] Calculating Food: " + foodLoc + " | " + ant.getCurrentTile());

    			Integer dist = Integer.MAX_VALUE;
    			
    			if(map.getAllFriendlyAnts().size()<10)
    				dist=ps.getDistance(ant.getCurrentTile(), foodLoc);
    			else
    				dist=map.getDistance(ant.getCurrentTile(), foodLoc);
    			
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
       	
       	for(TreeMultiMap.Entry<Integer, Pair<Ant, Tile>> segment : antDist.entryList())
       	{
       		Tile segmentFood = segment.getValue().second;
       		Ant segmentAnt = segment.getValue().first;
       		int segmentLength = segment.getKey();
       		
       		
       		
       		if(game.getTimeRemaining() < 100) { break; }

       		if (!foodClaimed.contains(segmentFood) && !antFoodDistance.containsKey(segmentAnt)) 
       		{

       			logcrash("[" + game.getTimeRemaining() + "] Trying food route: " + segmentAnt + " | " + segmentFood);

       			/*
            		if(!foodMoves.containsKey(dist)){
            			foodMoves.put(dist,new LinkedList<Pair<Ant,Tile>>());
            		}
       			 */
       			for(TreeMultiMap.Entry<Integer,Pair<Ant,Tile>> previousMove: foodMoves.entryList())
       			{
       				Tile previousMoveFood = previousMove.getValue().second;
       				Ant previousMoveAnt = previousMove.getValue().first;
       				
       				
       				
       				int distance;
       				// TODO: figure out the best way to use ps searches here.
       				if(map.getAllFriendlyAnts().size() < 10){
       					distance = ps.getDistance(previousMoveFood, segmentFood);
       				} else {
       					distance = map.getDistance(previousMoveFood, segmentFood);
       				}

       				distance += antFoodDistance.get(previousMoveAnt)-1;

       				if(distance < segmentLength){
       					// EASIER TO JUST SEND THIS DUDE

       					previousMoveAnt.addLog(game.getCurrentTurn(), "I'm hungry - also gonna grab " + segmentFood);

       					foodClaimed.add(segmentFood);
       					antFoodDistance.put(previousMoveAnt, distance);
       				}

       			}
       			if(!foodClaimed.contains(segmentFood)){
       				// this food still isn't claimed, so have this guy do it.
       				foodClaimed.add(segmentFood);
       				antFoodDistance.put(segmentAnt, segmentLength);
       				foodMoves.put(segmentLength, new Pair<Ant,Tile>(segmentAnt, segmentFood));
       				// move this guy.

       				segmentAnt.addLog(game.getCurrentTurn(), "Gonna grab me some food at  " + segmentFood);
       				doMoveLocation(segmentAnt, segmentFood);
       			}
       		}
       	}

      	log("[" + game.getTimeRemaining() + "] Begin checking hill distances");

       	// Priority 3: attack hills | Any ant not grabbing food should head to an enemy hill if we see one
        TreeMultiMap<Integer, Pair<Ant, Tile>> hillDist = new TreeMultiMap<Integer, Pair<Ant, Tile>>();
        for (Tile tileHill : map.getAllEnemyHills()) 
        {
            for(Ant ant : map.getAllFriendlyAnts()) 
            {
            	if(game.getTimeRemaining() < 100) { break; }

            	if(!antOrders.contains(ant)) {
                   	logcrash("[" + game.getTimeRemaining() + "] Checking enemy hills: Ant: " + ant + " | " + tileHill);

                   	// int dist = ps.getDistance(ant.getCurrentTile(), tileHill);
                   	int dist = map.getDistance(ant.getCurrentTile(), tileHill);
                    
                   	// TODO: we should be smarter about this
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
        		
        		entry.getValue().first.addLog(game.getCurrentTurn(), "Attacking enemy base because I'm close enough: " + entry.getValue().second);

        		doMoveLocation(entry.getValue().first, entry.getValue().second);
        		entry.getValue().first.addLog(game.getCurrentTurn(), "Attacking hill at " + entry.getValue().second);
        	}
        }       	
       	       	
      	log("[" + game.getTimeRemaining() + "] Send ants to Previous destinations");

    	// Priority 4a: If ant was previously given a path, have them follow it
		for(Ant ant : map.getAllFriendlyAnts())
        {
			if(ant.hasPath() && !antOrders.contains(ant)) {
				ant.addLog(game.getCurrentTurn(), "Attempting to resume existing path to " + ant.getDestination());
				doMoveLocation(ant, ant.getDestination());
			}
        }
      	
    	// Priority 4b: Ant is an explorer if one of their horizon tiles is invisible
		for(Ant ant : map.getAllFriendlyAnts()) 
		{
			if(!antOrders.contains(ant)) 
			{
				log("Looking for horizon for ant " + ant);
				Set<Tile> setTilesHorizon = map.findHorizen(ant);
				if(setTilesHorizon.size() > 0) {
					log("Found horizon tiles for ant " + ant + " : " + setTilesHorizon);
					Tile closestHill = map.getClosestLocation(ant.getCurrentTile(), map.getFriendlyHills());
					Tile tileFarthest = map.getFarthestLocation(closestHill, setTilesHorizon);

					// TODO: CANT JUST DO A DOMOVELOCATION HERE
					ant.addLog(game.getCurrentTurn(), "Attempting blind horizon move to " + tileFarthest);
					if(map.isOnHill(ant) || !doMoveInDirection(ant, tileFarthest)) {
						doMoveLocation(ant, tileFarthest);
					}
				}
			}
        }

    	// Priority 4c: Give new ants initial path if they would not have had one by above logic
      	int CLOSEST_ANT_TO_HILL_THRESHOLD = 100;
      	for (Iterator<Ant> antIter = map.getNewFriendlyAnts().iterator(); antIter.hasNext(); ) 
      	{
	        Ant ant = antIter.next();
	        int iClosestAnt = Integer.MAX_VALUE;
	        if(!antOrders.contains(ant)) 
	        {
	        	Tile tileClosestEnemy = null;
	        	if(map.getAllEnemyAntTiles().size() > 0) {
	        		tileClosestEnemy = map.getClosestLocation(ant.getCurrentTile(), map.getAllEnemyAntTiles());
		        	iClosestAnt = map.getDistance(ant.getCurrentTile(), tileClosestEnemy); 
	        	}
	        	
	        	if(iClosestAnt < CLOSEST_ANT_TO_HILL_THRESHOLD) {
	              	log("[" + game.getTimeRemaining() + "] [NEW ANT] Mofo is too close... sending new ant at " + tileClosestEnemy);
					ant.addLog(game.getCurrentTurn(), "New ant [defense] mofo is too close... sending new ant at " + tileClosestEnemy);
	              	doMoveLocation(ant, tileClosestEnemy);
	        	}
	        	else if(map.getAllEnemyHills().size() > 0) {
	        		Tile tileClosestHill = map.getClosestLocation(ant.getCurrentTile(), map.getAllEnemyHills());
	              	log("[" + game.getTimeRemaining() + "] [NEW ANT] We see a hill... send ant to " + tileClosestHill);
					ant.addLog(game.getCurrentTurn(), "New ant [attack] We see a hill... send ant to " + tileClosestHill);
		         	doMoveLocation(ant, tileClosestHill);
	        	}
	        	else {
	              	List<Tile> myTargets = hillStartTargets.get(ant.getCurrentTile());

	              	int iNumTargetsForHill = myTargets.size();  

	              	// Get the target offset we want to use for this ant
	              	int iPrevAnts = antsSentRandomlyFromLocation.get(ant.getCurrentTile());
	              	antsSentRandomlyFromLocation.put(ant.getCurrentTile(), iPrevAnts + 1);
	              	
	              	Tile next = myTargets.get(iPrevAnts % iNumTargetsForHill);

	              	log("[" + game.getTimeRemaining() + "] [NEW ANT] Sending new ant to next hill location: " + next);

	              	// TODO: if this target is invalid, the ant will immediately go into heatmap mode, need to 
	              	// retry another path and remove the old one
	              	if(map.getIlk(next) != Ilk.WATER) {
						ant.addLog(game.getCurrentTurn(), "New ant [start locations] send ant to " + next);
	              		doMoveLocation(ant, next);
	              	}
	              	else {
	              		// For now, just remove a tile from our targets if it is water
	              		// TODO: we should handle this better
	              		hillStartTargets.get(ant.getCurrentTile()).remove(next);
	              		log("[" + game.getTimeRemaining() + "] [NEW ANT] Target was water, so I've removed it: " + next);
	              	}
	        	}
	        }
         	antIter.remove();
        }
		
      	log("[" + game.getTimeRemaining() + "] Begin heatmap flurry");

       	// Priority 5: map coverage | Use the heat map to get optimal map coverage
        for(Ant ant : map.getAllFriendlyAnts())
        {
        	if(!antOrders.contains(ant))
        	{
               	logcrash("[" + game.getTimeRemaining() + "] Attempting heatmap flurry : " + ant);
            	if(game.getTimeRemaining() < 100) { break; }

        		int lowestVal = Integer.MAX_VALUE;
        		Aim d = Aim.NORTH;
        		
        		List<Aim> dir = new ArrayList<Aim>();
        		for(Aim aim: Aim.values())
        		{
        			heat = hmFlurry.getHeat(ant.getCurrentTile(), aim);
        			
        			if(heat < lowestVal){
        				dir.clear();
        				lowestVal = heat;
        				dir.add(aim);
        			} else if (heat == lowestVal){
        				dir.add(aim);        				
        			}
        		}
        		
        		log("have " + dir.size() + " options! And lowestval is " + lowestVal);

        		// choose a random direction
        		if(dir.size() > 0){
        			d = dir.get(new Random().nextInt(dir.size()));
        		}

        		// This would move the ant around walls if it hit them
        		// doMoveDirectionNextPassible(ant,d); // This will put you on a path to go around objects if you run into a wall
        		// TODO: decide what we want to do here. In theory we'd want to send the ant to the closest invisible or unexplored tile
        		// if(!doMoveLocation(ant, map.getTile(ant.getCurrentTile(), d, 6))) {
				ant.addLog(game.getCurrentTurn(), "Heatmap flurry... going " + d);
        		doMoveDirection(ant, d);
        		// } 
        	}
        }

        // TODO: Can this be taken care of in the doMoveLocation function?
        for (Tile myHill : map.getFriendlyHills()) 
		{
        	boolean bAntOnMyHill = map.getAllFriendlyAntTiles().contains(myHill);
    		Ant antOnMyHill = map.getLocationAntMap().get(myHill);
        	
    		// If there is an ant on my hill and they don't have orders, move them off
        	if (bAntOnMyHill && !antOrders.contains(antOnMyHill)) {
                for (Aim direction : Aim.values()) {
    				antOnMyHill.addLog(game.getCurrentTurn(), "Attempting to move off the hill");
                    if (doMoveDirection(map.getLocationAntMap().get(myHill), direction)) {
                        break;
                    }
                }
            }        
        }
        
        // Which ants haven't been issued an order?
      	log("[" + game.getTimeRemaining() + "] Which ants haven't been issued an order?");
		for(Ant ant : map.getAllFriendlyAnts())
        {
			if(!antOrders.contains(ant)) {
				log("No orders for ant " + ant + " [" + ant.getCurrentTile() + "]");
			}
        }
 
		
		log("\n\n\n\n\n********************** Beginning Ant Dump for turn " + game.getCurrentTurn() + " **********************");
		for(Ant ant : map.getAllFriendlyAnts())
        {
			LinkedList<String> list = ant.getOrderLog(game.getCurrentTurn()); 
			if(list != null) {
				for(String str: list) {
					log("[" + game.getCurrentTurn() + "]" + ant + ": " + str);
				}
        	}	
        }
		
        log("End Turn | Remaining time: " + game.getTimeRemaining() + " | Cache size: " + ps.size());
		log("\n\n\n\n\n");
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
        	List<Aim> directions = map.getDirections(ant.getCurrentTile(), path.start());
        	
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
			return doMoveLocation(ant, map.getTileNextPassible(ant.getCurrentTile(), direction));
		}
	}

//	private boolean movePossible(Ant ant, Aim direction) 
//	{
//		Tile tileTo = map.getTile(ant.getCurrentTile(), direction);
//    	
//		return !map.getLocationAntMap().containsKey(tileTo) && map.getIlk(tileTo).isPassable();
//	}
	
	// Attempts to move to Tile without doing an AStar search
	public boolean doMoveInDirection(Ant ant, Tile tile) {
		List<Aim> directions = map.getDirectionsMostAggressive(ant.getCurrentTile(), tile, new Tile(0, 0));

		for (Aim direction : directions) {
            if (this.doMoveDirection(ant, direction)) {
                return true;
            }
        }
        return false;
	}
	
	public boolean doMoveDirection(Ant ant, Aim direction) 
    {
		log("Ant " + ant + ": attempting to move " + direction);

		if(ant == null) {
    		log("ERROR: doMoveDirection: ant is null");
		}
    	
		if(antOrders.contains(ant)) {
    		log("ERROR: doMoveDirection: attempt to issue duplicate order for ant [" + ant.getCurrentTile() + "]: Direction [" + direction + "]");
    		return false;
    	}
    	
    	// Track all moves, prevent collisions
        Tile newLoc = map.getTile(ant.getCurrentTile(), direction);

        // isUnoccupied isn't necessarily correct - need to figure out what to put here
        if (map.getIlk(newLoc).isMovable()) 
        {
        	if(map.getLocationAntMap().containsKey(newLoc))
        	{
        		queueOrders.put(map.getLocationAntMap().get(newLoc), new Pair<Ant, Aim>(ant, direction));
        		return false;
        	}
        	else {
                game.issueOrder(ant.getCurrentTile(), direction);
                map.moveAnt(ant, newLoc);
                antOrders.add(ant);

                // Attempt to run a dependent move
                if(queueOrders.containsKey(ant)) {
                	Pair<Ant, Aim> move = queueOrders.get(ant);
                	doMoveDirection(move.first, move.second);
                	queueOrders.remove(ant);
                }
                
                return true;
        	}

        } else {
        	log("Ant " + ant + " attempted to move to unmovable tile " + newLoc + " with ILK " + map.getIlk(newLoc));
            return false;
        }
    }

	
	
    public boolean moveAnts(Set<Ant> ants, Tile target)
    {
    	int iMoved = 0;
    	for(Ant ant: ants) 
    	{
    		// getHottestNeighbor(Tile t) {
    		
    		if(doMoveLocation(ant, target)) {
    			iMoved++;
    		}
    	}
    	
    	log("moveAnts: was able to move " + iMoved + " ants out of " + ants.size());
    	
    	// We're going to want to make sure we can move all of the ants before just doing it by
    	// using movePossible(Ant ant, Aim direction)
       	return true;
    }
    
    public Set<Tile> getZoneNeighbors(Ant ant, Tile target, Offset offset)
    {
    	Set<Tile> tiles = new HashSet<Tile>();
    	
		log("getZoneNeighbors: " + ant.getCurrentTile() + " | " + target);
    	
       	for (Map.Entry<Tile, Integer> enemyTileMap : offset.getOffsetMap().entrySet()) 
       	{
    		Tile tile = map.getTile(target, enemyTileMap.getKey());
       		log("Checking tile: " + tile);

       		if(map.areNeighbors(ant.getCurrentTile(), tile) && (map.getIlk(tile) != Ilk.WATER)) {
       			tiles.add(tile);
       		}
       	}

       	if(tiles.size() > 0) {
    		log("SUCCESSFUL ATTACK: " + ant.getCurrentTile() + " | " + tiles);
       	}

       	return tiles;
    }

    public Tile getClosestEnemy(Ant ant)
    {
		   return map.getClosestLocation(ant.getCurrentTile(), map.getEnemyAntsInMyAttackRange(ant));
    }
 
	// TODO: Make this more efficient later
    // Currently, look for any ants within your view distance
    public Ant getClosestHelp(Ant ant, Tile enemyTile)
	{
    	Tile closestFriend = null;
    	int highestHeat = Integer.MIN_VALUE;
    	
       	for (Map.Entry<Tile, Integer> mapView : this.offHelp.getOffsetMap().entrySet())
       	{
       		Tile tileCheck = map.getTile(ant.getCurrentTile(), mapView.getKey());
       		
       		if(!tileCheck.equals(ant.getCurrentTile()) && map.getAllFriendlyAntTiles().contains(tileCheck)) {
       			if(mapView.getValue() > highestHeat) {
       				closestFriend = tileCheck;
       				highestHeat = mapView.getValue();
       			}
       		}  
       	}

       	// Also look to see if the guy can be found in the  hash
       	if(closestFriend == null) {
       		for(Ant a: map.getMyAntsInEnemyAttackRange(enemyTile)) {
       			if(a != ant) {
       				log("Found and ant outside of my help range along this enemy's perimeter");
       				return a;
       			}
       		}
       	}

       	return (closestFriend == null) ? null : this.map.getLocationAntMap().get(closestFriend);
	}

    public boolean moveToBorders(Set<Ant> ants, HeatMap hm)
    {
    	return true;
    	
//    	// Set<Pair<Ant, Set<Tile>>> 
//
//    	
//    	// Add all borders that this ant can get to
//    	Set<Tile> antBorders = new HashSet<Tile>();
//    	if(hmEnemyNextAttack.isBorder(ant.getCurrentTile())) {
//    		antBorders.add(ant.getCurrentTile());
//    	}
//    	antBorders.addAll(hmEnemyNextAttack.findNeighboringBorders(ant.getCurrentTile()));
//    	
//    	// Add all borders that the helper can get to
//    	Set<Tile> helperBorders = new HashSet<Tile>();
//    	if(hmEnemyNextAttack.isBorder(helper.getCurrentTile())) {
//    		helperBorders.add(helper.getCurrentTile());
//    	}
//    	helperBorders.addAll(hmEnemyNextAttack.findNeighboringBorders(helper.getCurrentTile()));
//
//    	log("Ant can get to borders: " + antBorders);
//    	log("Helper can get to borders: " + helperBorders);
//
    	
    	
    	
 	
    	
    	
    }
    
	public void log(String str) {
		game.log(str);
	}

	public void logcrash(String str) {
		game.logcrash(str);
	}
	
    public static void main(String[] args) throws IOException {
        new MyBot().readSystemInput();
    }
}
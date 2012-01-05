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

public class MyBot extends Bot 
{
	public static boolean DEBUG = false;
	
    // Tracking Order
    private Set<Ant> antOrders = new HashSet<Ant>();
    private Map<Ant,  Pair<Ant, Aim>> queueOrders = new HashMap<Ant,  Pair<Ant, Aim>>();
    
    // Food
    private int MAX_FOOD_DISTANCE;
    private Set<Tile> foodClaimed = new HashSet<Tile>();
    
    // Hill Defense
	private int HILL_DEFENSE_RADIUS;
	private Offset offHillDefense;
	private HeatMap hmHillDefense;    
    
    
    // Influence Maps
	InfluenceMap iMapFriendlyAnts;
	InfluenceMap iMapFriendlyAntsPull;
	InfluenceMap iMapFriendlyHills;
	InfluenceMap iMapFood;
	InfluenceMap iMapEnemyAnts;
	InfluenceMap iMapHorizon;
   	InfluenceMap iMapEnemyHills;
	InfluenceMap iMapEnemyAttackPerimeter;
   	InfluenceMap iMapEnemyAttackHorizon;
   	// InfluenceMap iMapAntReach;
	InfluenceMap iCombined;
    
	BattleResolution br;
	
    // Path store
    private PathStore ps;
    
    private Visualizer visualizer;

    public void setup(int loadTime, int turnTime, int rows, int cols, int turns, int viewRadius2, int attackRadius2, int spawnRadius2) 
    {
    	super.setup(loadTime, turnTime, rows, cols, turns, viewRadius2, attackRadius2, spawnRadius2);

        MAX_FOOD_DISTANCE = 20;
    	
		HILL_DEFENSE_RADIUS = 16;
		offHillDefense = new Offset(HILL_DEFENSE_RADIUS, map);
		hmHillDefense = new HeatMap(map);
        
    	iMapFriendlyAnts = new InfluenceMap(map, 2.5f, 0.65f, 1, false);
    	iMapFriendlyAntsPull = new InfluenceMap(map, -2.5f, 0.4f, 1, false);
    	iMapFriendlyHills = new InfluenceMap(map, 1.0f, 0.85f, 1, false);
    	iMapFood = new InfluenceMap(map, -8, 0.8f, .8f, true);
    	// iMapAntReach = new InfluenceMap(map, -2, 0.79f, 1, false);
    	iMapEnemyAnts = new InfluenceMap(map, -7, 0.60f, 1, true);
    	iMapHorizon = new InfluenceMap(map, -5, 0.95f, 0.2f, false);
	   	iMapEnemyHills = new InfluenceMap(map, -10, 0.95f, 1, false);
    	// iMapEnemyAttackPerimeter = new InfluenceMap(map, 2, 0.0f, 1, true);
	    // iMapEnemyAttackHorizon = new InfluenceMap(map, 1, 0.0f, 1, true);
    	
    	iCombined = new InfluenceMap(map);
        
        visualizer = new Visualizer(map, game);
        
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
        log("Beginning turn " + game.getCurrentTurn());
        loginfo("Doing Bot afterUpdate() ");
    	super.afterUpdate();

        foodClaimed.clear();
    	antOrders.clear();
    	queueOrders.clear();

		loginfo("Calculating HeatMaps hmHillDefense");
		loginfo("Ememy Vision");
		hmHillDefense.copyMapFrom(map.getEnemyVisionHeatMap());
		// hmHillDefense.print();
		loginfo("Subtracting my vision");
		hmHillDefense.subtract(map.getMyVisionHeatMap());
		// hmHillDefense.print();
		loginfo("Adding importance");
		hmHillDefense.addImportance(map.getFriendlyHills(), offHillDefense);
		// hmHillDefense.print();    	
    	
        // TODO: Do we want to empty the cache
        ps.emptyCache();
    }

    public void doTurn() 
    {
    	// visualizer.drawHeatMap(hmHillDefense);
    	
    	// Create all of the diffusion maps at the beginning of the turn since they're expensive
    	log("Creating ant influence map");
    	long begin = System.currentTimeMillis();
    	long step =  System.currentTimeMillis();

    	// Influence 0: From friendly hills
    	iMapFriendlyHills.propagate(map.getFriendlyHills());
    	long end = System.currentTimeMillis(); log("Time for Friendly Hills = " + (end - step) + " ms" ); step =  System.currentTimeMillis();
    	
    	iMapFriendlyAnts.propagate(map.getAllFriendlyAntTiles());
    	iMapFriendlyAntsPull.propagate(map.getAllFriendlyAntTiles());
    	end = System.currentTimeMillis(); log("Time for Friendly Ants = " + (end - step) + " ms" ); step =  System.currentTimeMillis();
    	
    	// Influence 2: From food
    	Set<Tile> allAnts = new HashSet<Tile>();
    	allAnts.addAll(map.getAllFriendlyAntTiles());
    	allAnts.addAll(map.getAllEnemyAntTiles());
    	iMapFood.propagate(map.getAllFoodTiles(), allAnts);
    	
    	// iMapAntReach.propagate(map.getAllFriendlyAntTiles());
    	// end = System.currentTimeMillis(); log("Time for Enemy Ants = " + (end - step) + " ms" ); step =  System.currentTimeMillis();

    	iMapEnemyAnts.propagate(map.getAllEnemyAntTiles(), map.getAllFriendlyAntTiles());
    	end = System.currentTimeMillis(); log("Time for Enemy Ants = " + (end - step) + " ms" ); step =  System.currentTimeMillis();
    	
    	// iMapHorizon.setMaxIncreasePct(0.01f);
       	iMapHorizon.propagate(map.getAllHorizonTiles());
    	// iMapHorizon.print();
    	end = System.currentTimeMillis(); log("Time for Horizon = " + (end - step) + " ms" ); step =  System.currentTimeMillis();

	   	iMapEnemyHills.propagate(map.getAllEnemyHills(), false);
    	end = System.currentTimeMillis(); log("Time for Enemy Hills = " + (end - step) + " ms" ); step =  System.currentTimeMillis();

//    	iMapEnemyAttackPerimeter.propagate(map.getEnemyNextAttackPerimeter());
//    	end = System.currentTimeMillis(); log("Time for Enemy Attack Perimeter = " + (end - step) + " ms" ); step =  System.currentTimeMillis();
//
//	   	iMapEnemyAttackHorizon.propagate(map.getEnemyNextAttackHorizon());
//    	end = System.currentTimeMillis(); log("Time for Enemy Attack Horizon = " + (end - step) + " ms" ); step =  System.currentTimeMillis();

	   	iCombined.clearMap();
    	iCombined.addMap(iMapFriendlyHills);
    	iCombined.addMap(iMapHorizon);
    	iCombined.addMap(iMapEnemyAnts);
    	iCombined.addMap(iMapFood);
    	iCombined.addMap(iMapEnemyHills);

    	end = System.currentTimeMillis(); log("Time for creating combined map = " + (end - step) + " ms" ); step =  System.currentTimeMillis();
    	
    	// visualizer.drawInfluenceMap(iMapFriendlyAnts, new Color(255, 0, 0));    
    	// visualizer.drawInfluenceMap(iMapEnemyAnts, new Color(255, 0, 0));    
    	// visualizer.drawInfluenceMap(iMapHorizon, new Color(255, 0, 0));    
    	// visualizer.drawInfluenceMap(iCombined, new Color(255, 0, 0));    

    	end = System.currentTimeMillis(); log("Time for drawing combined map = " + (end - step) + " ms" ); step =  System.currentTimeMillis();
    	
    	log("Total Time = " + (end - begin) + " ms" ); 
    	    	
    	// Battle Resolution!!!
    	// ***** NOTE: THIS MUST BE DONE AFTER COLLECTING FOOD AND CLIMBING, 
    	// BUT MUST BE UP HERE TO TEST OR IT LOOKS LIKE GUYS ALREADY MOVED
    	br = new BattleResolution(map, iMapEnemyAnts, iMapFriendlyAntsPull, iMapFood);

//    	// BR VIZ
//    	visualizer.drawTileSet(br.getRemovedTiles());
//    	ArrayList<BattleResolutionParticipant> participants = br.getParticipants(); 
//    	visualizer.drawBattles(participants);
//
//		Set<Tile> e = new HashSet<Tile>();
//		e.add(new Tile(25, 33));
//		visualizer.drawTileSet(e, new Color(255, 0, 0));
//
//		Set<Tile> f = new HashSet<Tile>();
//		f.add(new Tile(25, 36));
//		f.add(new Tile(28, 33));
//		f.add(new Tile(27, 35));
//		visualizer.drawTileSet(f, new Color(255, 255, 255));

//      visualizer.drawAntNextMoveBoxes(iCombined);
    	
        TreeMultiMap<Float, Pair<Tile, BattleResolution.direction>> movePriority = new TreeMultiMap<Float, Pair<Tile, BattleResolution.direction>>(true);

        for (BattleResolutionParticipant brp : br.getMyParticipants()) 
        {
        	for(int i=0; i<5; i++) {
        		int num = brp.getDistro()[i];
        		int possible = brp.getMovePossibleArray()[i];
            	if(num > 1) {
            		movePriority.put((float)num / (float)possible, 
            				new Pair<Tile, BattleResolution.direction>(brp.getStartLocation(), BattleResolution.direction.get(i)));
            	}
        	}
        }
    	
        Set<Tile> movedAntTiles = new HashSet<Tile>();
        
       	for(TreeMultiMap.Entry<Float, Pair<Tile, BattleResolution.direction>> move : movePriority.entryList())
       	{
       		Tile tileAnt = move.getValue().first;
       		
       		// If we've already moved this ant, keep going
       		if(movedAntTiles.contains(tileAnt)) {
       			continue;
       		}
       		
			Ant ant = map.getLocationAntMap().get(tileAnt);

			log("Attemping to move ant " + ant + " at tile " + tileAnt);
			
			// Check both that this ant doesn't have an order in the official antOrders set too
       		if(!antOrders.contains(ant)) {
           		BattleResolution.direction direction = move.getValue().second;
    			Aim aim = direction.getAim();

    			log("direction " + direction);
    			log("aim " + aim);
    			
    			// If aim is null, that means we're supposed to stay put
    			if(aim == null) {
	            	movedAntTiles.add(tileAnt);
    				antOrders.add(ant);
    			}
    			else {
    	            if(doMoveDirection(ant, aim)) {
    	            	movedAntTiles.add(tileAnt);
    	            }
   	            }
       		}
       	}
        
    	// COLLECT FOOD... YIPPY!
    	// collectFood();
    	

		for(Ant ant : map.getAllFriendlyAnts()) {
			if(!antOrders.contains(ant)) 
			{
				TreeMultiMap<Float, Tile> moves = iCombined.getMoves(ant.getCurrentTile());

				// log("Moves for ant " + ant + " | "+ moves);

		       	for(TreeMultiMap.Entry<Float, Tile> move : moves.entryList())
		       	{
					if(doMoveLocation(ant, move.getValue(), false)) {
						break;
					}
		       	}
			}
		}

		
    	// Check all battles and see if we need help
    	
    	// Start sending troops to battles starting with closest battle
		
        // Which ants haven't been issued an order?
      	// log("Which ants haven't been issued an order?");
		for(Ant ant : map.getAllFriendlyAnts())
        {
			if(!antOrders.contains(ant)) {
				// log("No orders for ant " + ant + " [" + ant.getCurrentTile() + "]");
			}
        }
 
 		if(game.getCurrentTurn() > 0)
		{
			game.debugjs("debug_orders[" + (game.getCurrentTurn()-1) + "] = {");
			for(Ant ant : map.getAllFriendlyAnts())
	        {
				LinkedList<String> list = ant.getOrderLog(game.getCurrentTurn()); 
				if(list != null) {
					
					for(String str: list) {
						game.debugjs(str + ",");
						loginfo("[" + game.getCurrentTurn() + "]" + ant + ": " + str);
					}
					
	        	}	
	        }
			game.debugjs("};");
		}
		
		
        log("End Turn | Cache size: " + ps.size() + "\n");
    }
 
    public boolean doMoveLocation(Ant ant, Tile destLoc, boolean bNavToNeighbor) 
    {
    	// TODO: This should never happen, but it was happening in the play_one_game.cmd
    	//       uncomment and fix bug eventually
    	if(ant == null) {
    		logerror("doMoveLocation: found null ant");
        	if(destLoc != null) {
        		logerror("...and he was trying to get to: " + destLoc);
        	}
        	else {
        		logerror("...and the destination he was trying to get to was also null... FUCK.");
        	}
    		return false;
    	}
    
    	if(antOrders.contains(ant)) {
    		logerror("doMoveLocation: attempt to issue duplicate order for ant " + ant + " [" + ant.getCurrentTile() + "]: Destination [" + destLoc + "]");
    		return false;
    	}
    	
    	loginfo("Attempting to move " + ant + " | " + ant.getCurrentTile() + " to " + destLoc);

    	// If ant already has a path and the destination is the one we're trying to go to, just return the next
    	// move in the current path.  If not, recompute the path
    	Path path;
    	if((path = ant.getPath()) == null || !path.end().equals(destLoc)) {
    		path = ps.getPath(ant.getCurrentTile(), destLoc, bNavToNeighbor);
    		loginfo("Path for move " + path);
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
			return doMoveLocation(ant, map.getTileNextPassible(ant.getCurrentTile(), direction), false);
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
		// log("Ant " + ant + ": attempting to move " + direction);

		if(ant == null) {
    		log("ERROR: doMoveDirection: ant is null");
    		return false;
		}
    	
		if(antOrders.contains(ant)) {
    		log("ERROR: doMoveDirection: attempt to issue duplicate order for ant [" + ant.getCurrentTile() + "]: Direction [" + direction + "]");
    		return false;
    	}
    	
    	// Track all moves, prevent collisions
        Tile newLoc = map.getTile(ant.getCurrentTile(), direction);

        // TODO: Need to fix this so that we're not stuck on the tutorial map
        if (map.getIlk(newLoc).isNavigable())
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

    public Tile getClosestEnemy(Ant ant) {
		   return map.getClosestLocation(ant.getCurrentTile(), map.getEnemyAntsInMyAttackRange(ant));
    }
     
    private void collectFood() 
    {
    	log("Begin checking food distances");
      	
      	Set<Tile> foodClaimed = new HashSet<Tile>();
        Map<Ant,Integer> antFoodDistance= new HashMap<Ant,Integer>();
        
        TreeMultiMap<Integer, Pair<Ant, Tile>> antDist = new TreeMultiMap<Integer, Pair<Ant, Tile>>();

        for (Tile foodLoc : map.getAllFoodTiles()) 
        {
    		for(Ant ant : map.getAllFriendlyAnts())
            {
    			if(game.getTimeRemaining() < 100) { break; }
    			loginfo("Calculating Food: " + foodLoc + " | " + ant.getCurrentTile());

    			Integer dist = Integer.MAX_VALUE;
   				dist = map.getDistance(ant.getCurrentTile(), foodLoc);
    			
                if(dist < MAX_FOOD_DISTANCE) 
				{
       				int dist2 = map.getDistanceThroughObstacles(ant.getCurrentTile(), foodLoc);
       				if(dist2 < MAX_FOOD_DISTANCE) {
       					antDist.put(dist2, new Pair<Ant, Tile>(ant, foodLoc));
       				}
                }
            }
        }
        
       	log("Begin deciding possible food routes");
       	
       	// Figure out food to food distances
       	// This stores the foodMove that I may want to make, ordered by length of the route
       	TreeMultiMap<Integer,Pair<Ant,Tile>> foodMoves = new TreeMultiMap<Integer,Pair<Ant,Tile>>();
       	
       	for(TreeMultiMap.Entry<Integer, Pair<Ant, Tile>> segment : antDist.entryList())
       	{
       		Tile segmentFood = segment.getValue().second;
       		Ant segmentAnt = segment.getValue().first;
       		int segmentLength = segment.getKey();
       		
       		if(game.getTimeRemaining() < 100) { break; }

       		if (!foodClaimed.contains(segmentFood) && !antFoodDistance.containsKey(segmentAnt)) 
       		{
       			logcrash("Trying food route: " + segmentAnt + " | " + segmentFood);

       			for(TreeMultiMap.Entry<Integer,Pair<Ant,Tile>> previousMove: foodMoves.entryList())
       			{
       				Tile previousMoveFood = previousMove.getValue().second;
       				Ant previousMoveAnt = previousMove.getValue().first;
       				
       				int distance;
   					distance = map.getDistanceThroughObstacles(previousMoveFood, segmentFood);
       				distance += antFoodDistance.get(previousMoveAnt)-1;

       				if(distance < segmentLength){
       					// EASIER TO JUST SEND THIS DUDE
       					previousMoveAnt.addLog(game, "I'm hungry - also gonna grab " + segmentFood);
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
       				segmentAnt.addLog(game, "Gonna grab me some food at  " + segmentFood);
       				
       	       		if(!antOrders.contains(segmentAnt)) {
           				doMoveLocation(segmentAnt, segmentFood, true);
       	       		}
       	       		else {
           				segmentAnt.addLog(game, "Tried to reissue this ant another order to grab food at " + segmentFood);
       	       		}
       			}
       		}
       	}
    }
    
    public void log(String str) {
   		map.log(str);
    }

    public void loginfo(String str) {
   		map.loginfo(str);
    }

    public void logerror(String str) {
   		map.logerror(str);
    }
    
    public void logcrash(String str) {
    	if(MyBot.DEBUG){
    		game.logcrash(str);
    	}
    }

    public static void main(String[] args) throws IOException {
    	boolean debug = false;
    	for(String r: args){
    		if(r.toLowerCase().equals("debug")){
    			MyBot.DEBUG = true;
    		}
    	}
    	new MyBot().readSystemInput();
    }
}
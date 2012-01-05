import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Battle {
	private AntMap map;
	private HeatMap hmDelta;
	private Tile tileStart;
	private Set<Tile> tilesAnts;
	private Set<Tile> tilesEnemies; 
	private Set<Tile> tilesPerimeterAnts;
	private Set<Tile> tilesPerimeterEnemies; 
	private Set<Tile> tilesInBattle;
	private Set<Tile> tilesInBattleWithPerimeters;
	private int iMinRow;
	private int iMinCol;
	private int iMaxRow;
	private int iMaxCol;
	private int iNumPerims;
	private Set<Tile> tileTargets;
	private Map<Integer, Set<Tile>> mapPerimeter;
	private TreeMultiMap<Integer, HashMap<Tile, Tile>> moveMap;
	private LinkedList<Pair<Tile, LinkedList<Tile>>> listPerimMoves;
	
	public Battle(AntMap map) {
		this.map = map;
		this.hmDelta = map.getNextAttackDeltaHeatMap();
		this.tilesAnts = new HashSet<Tile>();
		this.tilesEnemies = new HashSet<Tile>();
		this.tilesPerimeterAnts = new HashSet<Tile>();
		this.tilesPerimeterEnemies = new HashSet<Tile>();
		this.tilesInBattle = new HashSet<Tile>();
		this.tilesInBattleWithPerimeters = new HashSet<Tile>();
		this.mapPerimeter = new HashMap<Integer, Set<Tile>>();
		this.iMinRow = Integer.MAX_VALUE;
		this.iMinCol = Integer.MAX_VALUE;
		this.iMaxRow = Integer.MIN_VALUE;
		this.iMaxCol = Integer.MIN_VALUE;
		this.iNumPerims = 0;
	}

	public void initialize(Tile tileStart) {
		this.tileStart = tileStart;

		// Initialize the perimeter set
		mapPerimeter.put(1, new HashSet<Tile>());

		// Add the first tile, this will start expanding the battle
		addTile(tileStart, 0, 0);
		
		map.loginfo("Done adding tiles, adding perimeter 1");
		
		// Add the first perimeter to pull in any ants that are on it
		addPerimeter(2);
		
		map.loginfo("Done adding perimeter");
	}

	public Set<Tile> getEnemies() {
		return tilesEnemies;
	}
	
	private void addNeighbors(Tile tile, int iRowOffset, int iColOffset) {
		for(Pair<Tile, Aim> pairTileAim: tile.getNeighborsWithDirections(map)) {
			Tile tileTemp = pairTileAim.first;
			Aim aimTemp = pairTileAim.second;
			addTile(tileTemp, iRowOffset + aimTemp.getRowDelta(), iColOffset + aimTemp.getColDelta());
		}
	}

	private void addPerimeter(int iLevel) {
		iNumPerims = iLevel;

		mapPerimeter.put(iLevel, new HashSet<Tile>());

		Set<Tile> prevPerim = mapPerimeter.get(iLevel-1);

		for(Tile tile: prevPerim) {
			addPerimeterTile(tile, iLevel);
		}
	}

	private boolean addPerimeterTile(Tile tile, int iLevel) {
		if(tilesInBattleWithPerimeters.contains(tile)) {
			return false;
		}

		processTilePerimeter(tile, iLevel);
		
		mapPerimeter.get(iLevel).add(tile);
		tilesInBattleWithPerimeters.add(tile);
		return true;
	}
	
	private boolean isConflictTile(Tile tile) {
		return hmDelta.getHeat(tile) > Integer.MIN_VALUE;
	}
	
	private boolean addTile(Tile tile, int iRowOffset, int iColOffset) {
		if(tilesInBattle.contains(tile)) {
			return false;
		}

		iMinRow = Math.min(iMinRow, iRowOffset);
		iMinCol = Math.min(iMinCol, iColOffset);
		iMaxRow = Math.max(iMaxRow, iRowOffset);
		iMaxCol = Math.max(iMaxCol, iColOffset);

		processTile(tile);
		
		// For each of my neighbors with an attack delta, add it to the battle set
		// If there is an ant on the tile, add it
		// If the tile is an enemy hill, add it
		// If there is food, grab it

		// If this is not a conflict tile and not the start tile, no need to add
		// else, keep adding
		if(!isConflictTile(tile) && !tile.equals(tileStart)) {
			mapPerimeter.get(1).add(tile);
			tilesInBattleWithPerimeters.add(tile);
			return false;
		}
		else {
			tilesInBattle.add(tile);
			tilesInBattleWithPerimeters.add(tile);
			addNeighbors(tile, iRowOffset, iColOffset);
			return true;
		}
	}

	private void processTile(Tile tile) 
	{
		switch (map.getIlk(tile)) {
          case ENEMY_ANT :
        	  tilesEnemies.add(tile);
        	  break;
          case ENEMY_HILL:
        	  // Do I need this or should I just do a search of how close we are to the
        	  // hill since it might not be part of the battle?
        	  // tileTargets.add(tile);
        	  break;
          case MY_ANT:
        	  tilesAnts.add(tile);
        	  break;
          case MY_HILL:
        	  // Same as above comment
        	  break;
          case FOOD:
        	  // Do I want to worry about food in these scenarios?
        	  break;
          
          default: 
        	  break;
      }
	}

	private void processTilePerimeter(Tile tile, int iLevel) 
	{
		switch (map.getIlk(tile)) {
          case ENEMY_ANT :
        	  tilesPerimeterEnemies.add(tile);
        	  break;
          case ENEMY_HILL:
        	  // Do I need this or should I just do a search of how close we are to the
        	  // hill since it might not be part of the battle?
        	  // tileTargets.add(tile);
        	  break;
          case MY_ANT:
        	  tilesPerimeterAnts.add(tile);
        	  break;
          case MY_HILL:
        	  // Same as above comment
        	  break;
          case FOOD:
        	  // Do I want to worry about food in these scenarios?
        	  break;
          
          default: 
        	  break;
      }
	}

	public HashMap<Tile, Tile> getAntsToClosestPerimeterTiles()
	{
		listPerimMoves = new LinkedList<Pair<Tile, LinkedList<Tile>>>();
		
		Set<Tile> allFriendlyAntTiles = new HashSet<Tile>(tilesAnts);
		allFriendlyAntTiles.addAll(tilesPerimeterAnts);
		
		for(Tile tile: allFriendlyAntTiles) 
		{
			LinkedList<Tile> moves = new LinkedList<Tile>();
			// Get all tiles this ant can get to next turn
			for(Tile tNeighbor: tile.getPossibleDestinationsNextTurn(map)) {
				// If tile is on the perimeter, add it
				if(mapPerimeter.get(1).contains(tNeighbor) || hmDelta.getHeat(tile) > 0) {
					moves.add(tNeighbor);
				}
			}
			listPerimMoves.add(new Pair<Tile, LinkedList<Tile>>(tile, moves));
		}

		moveMap = new TreeMultiMap<Integer, HashMap<Tile, Tile>>();
		addMoves(0, new HashMap<Tile, Tile>());

		map.log("Perimeter move map: " + listPerimMoves);

     	for (TreeMultiMap.Entry<Integer, HashMap<Tile, Tile>> entry : moveMap.entryList()) 
        {
     		map.log("Move set # " + entry.getKey());
     		
     		for(Map.Entry<Tile, Tile> move : entry.getValue().entrySet()) {
     			map.log("Move from " + move.getKey() + " to " + move.getValue());
     		}
     		
     		return entry.getValue();
        }
 		return null;
	}
	
	public void addMoves(int index, HashMap<Tile, Tile> moves)
	{
		// Once we've added all the moves and get here, we have a legit set of moves
		if(index == listPerimMoves.size()) {


			
			moveMap.put(moveMap.size(), moves);
			return;
		}
		
		Tile tile = listPerimMoves.get(index).first;
		for(Tile neighbor: listPerimMoves.get(index).second) {
			if(!moves.containsValue(neighbor)) {
				HashMap<Tile, Tile> movesNew = new HashMap<Tile, Tile>(moves);
				movesNew.put(tile, neighbor);
				addMoves(index+1, movesNew);
			}
		}
	}
	
	public void print() 
	{
		map.log("Printing battle with enemy ant at " + tileStart);
		String strRow = "";
		int x;
		int y;
		for(x=iMinRow-iNumPerims; x<=iMaxRow+iNumPerims; x++)
		{
			strRow = "[" + String.format("%02d", x) + "]";
			
			for(y=iMinCol-iNumPerims; y<=iMaxCol+iNumPerims; y++)
			{
				Tile t = map.getTile(tileStart, new Tile(x, y));
				
				if(this.tilesEnemies.contains(t)) {
					if(this.tilesInBattle.contains(t))
						strRow += "E";
					else
						strRow += "e";
				}
				else if(this.tilesAnts.contains(t)) {
					if(this.tilesInBattle.contains(t))
						strRow += "A";
					else
						strRow += "a";
				}
				else if(this.tilesInBattle.contains(t)) {
					strRow += " ";		
				}
				else if(map.getIlk(t) == Ilk.WATER) {
					strRow += "W";		
				}
				else {
					strRow += " ";		
				}
				int heat = this.map.getNextMyAttackVsEnemyDeltaHeatMap().getHeat(t);
				strRow += (heat == Integer.MIN_VALUE) ? "  " : String.format("%02d", heat);
			}
			map.log(strRow);
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public void fight()
	{
		map.log("tilesEnemies: " + tilesEnemies);
		map.log("tilesAnts: " + tilesAnts);
		map.log("calculateBattle: " + calculateBattle(tilesEnemies, tilesAnts));
		
		for(int i=0; i<1000; i++) {
			calculateBattle(tilesEnemies, tilesAnts);
		}
		
		
//		Pair<Integer, List<Pair<Ant,Aim>>> bestMove = guessMove(targetAnts, new LinkedList<Pair<Ant,Aim>>(), baddies);
//		for(Pair<Ant,Aim> mv:bestMove.second){
//			mv.first.addLog(game.getCurrentTurn(), "Battle rez says htis was a good idea...score: " + bestMove.first);
//			doMoveDirection(mv.first, mv.second);
//
//		}
	}
	
	private Pair<Integer, List<Pair<Ant,Aim>>> guessMove(List<Ant> myAnts, List<Pair<Ant,Aim>> moves, Set<Tile> enemies)
	{
		if(myAnts.size() == 0)
		{
			Set<Tile> enemyLocations = new HashSet<Tile>();
			for(Tile enemy: enemies){
				Tile newEnemyLocation = map.getTile(enemy,map.getDirection(enemy, moves.get(0).first.getCurrentTile()));
				enemyLocations.add(newEnemyLocation);
			}

			Set<Tile> friendLocations = new HashSet<Tile>();
			for(Pair<Ant,Aim> p:moves){
				friendLocations.add(map.getTile(p.first.getCurrentTile(),p.second));
			}
			
			// int score = calculateBattle(enemyLocations,friendLocations);
			Pair<Integer, Integer> scorePair = calculateBattle(enemyLocations,friendLocations);
			int score = scorePair.first * 1300 - scorePair.second * 1000;
			
			List<Pair<Ant,Aim>> ret = new LinkedList<Pair<Ant,Aim>>();
			for(Pair<Ant,Aim> p: moves){
				ret.add(new Pair<Ant,Aim>(p.first, p.second));
			}
			return new Pair<Integer, List<Pair<Ant,Aim>>>(score,ret);
		}
		else
		{
			Ant nextAnt = myAnts.remove(0);
			Pair<Integer, List<Pair<Ant,Aim>>> bestMove = null;
			for(Aim a:Aim.values()){
				moves.add(0,new Pair<Ant,Aim>(nextAnt,a));
				Pair<Integer, List<Pair<Ant,Aim>>> tmp = guessMove(myAnts, moves, enemies);
				if(bestMove == null || tmp.first > bestMove.first){
					bestMove = tmp;
				}
				moves.remove(0);
			}
			myAnts.add(0,nextAnt);
			return bestMove;
		}
	}

	private Pair<Integer, Integer> calculateBattle(Set<Tile> enemies, Set<Tile> friends)
	{
		Map<Tile,HashSet<Tile>> friendsInRangeOfEnemy = new HashMap<Tile,HashSet<Tile>>();
		Map<Tile,HashSet<Tile>> enemiesInRangeOfFriend = new HashMap<Tile,HashSet<Tile>>();

		// int totalDistance = 0;
		for(Tile enemy:enemies) {
			for(Tile friend:friends) {
				// totalDistance+= map.getDistance(friend, enemy);
				if(map.getDistanceSquared(friend, enemy) <= map.getGame().getAttackRadius2()) {
					if(!friendsInRangeOfEnemy.containsKey(enemy)){
						friendsInRangeOfEnemy.put(enemy, new HashSet<Tile>());
					}
					friendsInRangeOfEnemy.get(enemy).add(friend);
					if(!enemiesInRangeOfFriend.containsKey(friend)){
						enemiesInRangeOfFriend.put(friend, new HashSet<Tile>());
					}
					enemiesInRangeOfFriend.get(friend).add(enemy);
				}

			}
		}
		Set<Tile> deadFriends = new HashSet<Tile>();
		Set<Tile> deadEnemies = new HashSet<Tile>();

		for(Tile friend:friends)
		{
			if(enemiesInRangeOfFriend.containsKey(friend)){
				for(Tile enemy: enemiesInRangeOfFriend.get(friend)){
					if(enemiesInRangeOfFriend.get(friend).size() > friendsInRangeOfEnemy.get(enemy).size()){
						deadFriends.add(friend);
					} else if (enemiesInRangeOfFriend.get(friend).size() < friendsInRangeOfEnemy.get(enemy).size()){
						deadEnemies.add(enemy);
					} else {
						deadFriends.add(friend);
						deadEnemies.add(enemy);
					}
				}
			}
		}

		// return deadEnemies.size()*1000 - deadFriends.size()*1300-(int)Math.floor(Math.sqrt((double)totalDistance));
		return new Pair<Integer, Integer>(deadEnemies.size(), deadFriends.size());
	}
	
    public Set<Tile> getZoneNeighbors(Ant ant, Tile target, Offset offset)
    {
    	Set<Tile> tiles = new HashSet<Tile>();
    	
    	// log("getZoneNeighbors: " + ant.getCurrentTile() + " | " + target);
    	
       	for (Map.Entry<Tile, Integer> enemyTileMap : offset.getOffsetMap().entrySet()) 
       	{
    		Tile tile = map.getTile(target, enemyTileMap.getKey());
       		map.log("Checking tile: " + tile);

       		if(map.areNeighbors(ant.getCurrentTile(), tile) && (map.getIlk(tile) != Ilk.WATER)) {
       			tiles.add(tile);
       		}
       	}

       	if(tiles.size() > 0) {
       		// log("SUCCESSFUL ATTACK: " + ant.getCurrentTile() + " | " + tiles);
       	}

       	return tiles;
    }
    
	// TODO: Make this more efficient later
    // Currently, look for any ants within your view distance
    public void getClosestHelp(Ant ant, Tile enemyTile)
	{
    	
//    	Tile closestFriend = null;
//    	int highestHeat = Integer.MIN_VALUE;
//    	
//       	for (Map.Entry<Tile, Integer> mapView : map.offHelp.getOffsetMap().entrySet())
//       	{
//       		Tile tileCheck = map.getTile(ant.getCurrentTile(), mapView.getKey());
//       		
//       		if(!tileCheck.equals(ant.getCurrentTile()) && map.getAllFriendlyAntTiles().contains(tileCheck)) {
//       			if(mapView.getValue() > highestHeat) {
//       				closestFriend = tileCheck;
//       				highestHeat = mapView.getValue();
//       			}
//       		}  
//       	}
//
//       	// Also look to see if the guy can be found in the  hash
//       	if(closestFriend == null) {
//       		for(Ant a: map.getMyAntsInEnemyAttackRange(enemyTile)) {
//       			if(a != ant) {
//       			    map.log("Found and ant outside of my help range along this enemy's perimeter");
//       				return a;
//       			}
//       		}
//       	}
//
//       	return (closestFriend == null) ? null : this.map.getLocationAntMap().get(closestFriend);
	}

//	public LinkedList<Pair<Ant, Aim>> moveToPerimeter()
//	{
//		
//		for(Tile t: tilesAnts) {
//			
//		}
//		
//	}
	

	public boolean moveAnts(Set<Ant> ants, Tile target)
    {
//    	int iMoved = 0;
//    	for(Ant ant: ants) 
//    	{
//    		// getHottestNeighbor(Tile t) {
//    		
//    		if(doMoveLocation(ant, target, false)) {
//    			iMoved++;
//    		}
//    	}
    	
    	// log("moveAnts: was able to move " + iMoved + " ants out of " + ants.size());
    	
    	// We're going to want to make sure we can move all of the ants before just doing it by
    	// using movePossible(Ant ant, Aim direction)
       	return true;
    }	
    
	public void fightold()
	{
//		int heat;
//
//		// Loop through all ants that are on a tile that the enemy can attack next turn
//       	for (Map.Entry<Ant, Set<Tile>> mapMeAndEnemies : map.getMyAntToCloseByEnemies().entrySet())
//       	{
//           	Ant ant = mapMeAndEnemies.getKey();
//
//			int heatOnMe = map.getEnemyNextAttackHeat(ant);
//			log("Ant " + ant + " can be attacked by " + heatOnMe + " enemies next turn ");
//
//   			Tile tileClosestEnemy = getClosestEnemy(ant);
//   			int heatOnEnemy = map.getMyNextAttackHeat(tileClosestEnemy);
//
//   			log("Enemy " + tileClosestEnemy + " can be attacked by " + heatOnEnemy + " enemies next turn ");
//
//			if(heatOnMe < heatOnEnemy) 
//			{
//				log("BR Decision: ATTACK!!!");
//				
//				// I want to attack, but I should be smarter about what tiles I go to
//				for(Ant attacker: map.getMyAntsInEnemyAttackRange(tileClosestEnemy)) {
//					for(Aim a: map.getDirectionsMostAggressive(attacker.getCurrentTile(), tileClosestEnemy, new Tile(0,0))) {
//						attacker.addLog(game.getCurrentTurn(), "Battle Resolution: found enemy with less heat than I have, attack in direction: " + a);
//						doMoveDirection(attacker, a);
//					}
//				}
//			}
//			else if(heatOnMe > heatOnEnemy) 
//			{
//				log("BR Decision: RUN!");
//				
//				List<Aim> dirOpposites = map.getDirectionsMostAggressive(tileClosestEnemy, ant.getCurrentTile(), new Tile(0, 0));
//
//				log("Dir opposites: " + dirOpposites);
//
//				for(Aim aim: dirOpposites) {
//					ant.addLog(game.getCurrentTurn(), "Battle Resolution: taking too much heat... move in opposite direction");
//					if(doMoveDirection(ant, aim)) {
//						log("My ant " + ant + " ran away from " + tileClosestEnemy + " by moving " + aim);
//						break;
//					}
//				}
//			}
//			else {
//				// TODO: if no help, look for safe moves by looking at enemy next move heatmap
//				// Pick "best" safe move depending on what you're trying to do
//				log("BR Decision: Even heat, ask for help");
//
//				Ant helper = getClosestHelp(ant, tileClosestEnemy);
//				
//				// If help was found, join forces
//				if(helper != null) 
//				{
//					log("Found help: " + helper);
//
//					Set<Ant> attackGroup = new HashSet<Ant>();
//					attackGroup.add(ant);
//					attackGroup.add(helper);
//					// moveToBorders(attackGroup, hmEnemyNextAttack);
//
//					// Ok, Assuming we got here, we decided we can't kill the enemy on this turn.
//					// First move the ant to a safe spot
//					if(map.isEnemyNextAttackBorder(ant.getCurrentTile())) {
//						log("Ant is already on a border, telling him to stay put");
//						ant.addLog(game.getCurrentTurn(), "Battle Resolution: equal heat and this ant is on a border - stay put");
//						antOrders.add(ant);
//					}
//					else 
//					{
//						Set<Tile> tileMoves = map.findEnemyNextAttackBorders(helper.getCurrentTile());
//
//						for(Tile tile: tileMoves) {
//							helper.addLog(game.getCurrentTurn(), "Battle Resolution: attempting to move helper to border by going to tile " + tile);
//							if(doMoveDirection(helper, map.getDirection(ant.getCurrentTile(), tile))) {
//								log("Helper " + ant + "successfully moved to border by moving to tile " + tile);
//								break;
//							}
//						}
//						
//						if(!antOrders.contains(ant)) {
//							ant.addLog(game.getCurrentTurn(), "Battle Resolution: couldn't find a border, so just headed towards " + helper);
//							log("Couldn't find a border, so just sent " + ant + " towards" + helper);
//							doMoveLocation(ant, helper.getCurrentTile(), false);
//						}
//					}
//					
//					// If helper is on a border, stay put
//					// TODO: this will work for now, but we want to get the helper as close to our ant
//					if(map.isEnemyNextAttackBorder(helper.getCurrentTile())) {
//						helper.addLog(game.getCurrentTurn(), "Battle Resolution: helper is already on a border, telling him to stay put");
//						log("Helper is already on a border, telling him to stay put");
//						antOrders.add(helper);
//					}
//					else 
//					{
//						Set<Tile> tileMoves = map.findEnemyNextAttackBorders(helper.getCurrentTile());
//
//						for(Tile tile: tileMoves) {
//							helper.addLog(game.getCurrentTurn(), "Attempting to move helper to border");
//							
//							if(doMoveDirection(helper, map.getDirection(helper.getCurrentTile(), tile))) {
//								log("Helper " + helper + "Battle Resolution: successfully moved to border by moving to tile " + tile);
//								break;
//							}
//						}
//						
//						if(!antOrders.contains(helper)) {
//							helper.addLog(game.getCurrentTurn(), "Battle Resolution: couldnt find a border, so just headed towards" + ant);
//							log("Couldn't find a border, so just sent " + helper + " towards" + ant);
//							doMoveLocation(helper, ant.getCurrentTile(), false);
//						}
//					}
//
////					 2 2 1 1 1 0 
////					 2 2 1 1 1 1 
////					 2 2E1 1 1 1 
////					 2 1 1 1 1 1 
////					 1 1 1 1 1 0 
////					 0 1 1 1A0 0 
////					 0 0 0 0A0 0 
////					 0 0 0 0 0 0 
//
//
//					
//				}
//				// Run away
//				else {
//					log("No help found, run away");
//					
//					// TODO: This should be smart about running away, but this is fine for now
//					// to get opposite directions, just aim the enemy at me
//					List<Aim> dirOpposites = map.getDirections(tileClosestEnemy, ant.getCurrentTile());
//
//					ant.addLog(game.getCurrentTurn(), "Battle Resolution: couldn't find help, so I tried to run away");
//					for(Aim aim: dirOpposites) {
//						if(doMoveDirection(ant, aim)) {
//							log("My ant " + ant + " ran away from " + tileClosestEnemy + " by moving " + aim);
//							break;
//						}
//					}
//				}
//			}
//   			
////    		int enemyheat = hmNextMyAttackVsEnemyDelta.getHeat(tileClosestEnemy);
////    			
////    		if(enemyheat > 0) {
////        		log("Enemy at at " + tileClosestEnemy + " is fucked, sending: " + mapEnemyToMyCloseByAnts.get(tileClosestEnemy));
////    			moveAnts(mapEnemyToMyCloseByAnts.get(tileClosestEnemy), tileClosestEnemy);
////    		}
////    		else {
////    		}
//    	}
	}
	
}

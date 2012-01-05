import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BattleResolution {
	private AntMap map;
	private ArrayList<BattleResolutionParticipant> participants;
	private HashMap<Tile, BattleResolutionParticipant> mapOriginalState; 
	private HashMap<Tile, BattleResolutionParticipant> mapRunningState; 
	private Set<Tile> friendLocations; 
	private Set<Tile> enemyLocations; 
	private Set<BattleResolutionParticipant> allFriendBRPs; 
	private Set<BattleResolutionParticipant> allEnemyBRPs; 
	private TreeMultiMap<Float, BattleResolution.direction> scoreMap;
	private Set<Tile> removedTiles;
	private Set<Tile> antTilesInBattle;
	private InfluenceMap iMapEnemyAnts;
	private InfluenceMap iMapFriendlyAnts;
	private InfluenceMap iMapFood;
	

	public static enum direction {
		X(0, 0), 
		N(-1, 0), 
		S(1, 0), 
		E(0, 1), 
		W(0, -1);

		private int rowOffset;
		private int colOffset;

		private direction(int rowOffset, int colOffset) {
			this.rowOffset = rowOffset;
			this.colOffset = colOffset;
		}
		
		public int getRowOffset() {
			return this.rowOffset;
		}

		public int getColOffset() {
			return this.colOffset;
		}
		
		public static direction get(int index) {
			switch(index) {
			case 0:  return X;
			case 1:  return N;
			case 2:  return S;
			case 3:  return E;
			case 4:  return W;
			default: return X;
			}
		}

		public Aim getAim() {
			switch(this) {
			case N:  return Aim.NORTH;
			case S:  return Aim.SOUTH;
			case E:  return Aim.EAST;
			case W:  return Aim.WEST;
			default: return null;
			}
		}
	};   
		
	public BattleResolution(AntMap map, InfluenceMap iMapEnemyAnts, InfluenceMap iMapFriendlyAnts, InfluenceMap iMapFood) 
	{ 
		this.map = map;
		this.scoreMap = new TreeMultiMap<Float, BattleResolution.direction>(true); // the true param says this is a maximum ordered treemap
		this.removedTiles = new HashSet<Tile>();
		this.iMapEnemyAnts = iMapEnemyAnts;
		this.iMapFriendlyAnts = iMapFriendlyAnts;
		this.iMapFood = iMapFood;
		
		
		Map<Tile, Set<Tile>> ants = map.getMyAntTilesToCloseByEnemies();
		Map<Tile, Set<Tile>> enemies = map.getMapEnemyToMyCloseByAntTiles();
		mapOriginalState = new HashMap<Tile, BattleResolutionParticipant>(); 
		mapRunningState = new HashMap<Tile, BattleResolutionParticipant>(); 
		friendLocations = new HashSet<Tile>();
		enemyLocations = new HashSet<Tile>();
		antTilesInBattle = new HashSet<Tile>();
		
		allFriendBRPs = new HashSet<BattleResolutionParticipant>();
		allEnemyBRPs = new HashSet<BattleResolutionParticipant>();
		
//		Set<Tile> e = new HashSet<Tile>();
//		e.add(new Tile(25, 33));
//
//		Set<Tile> f = new HashSet<Tile>();
//		f.add(new Tile(25, 36));
//		f.add(new Tile(28, 33));
//		f.add(new Tile(27, 35));
//		
//		enemies.clear();
//		enemies.put(new Tile(25, 33), f);
//		
//		ants.clear();
//		ants.put(new Tile(25, 36), e);
//		ants.put(new Tile(28, 33), e);
//		ants.put(new Tile(27, 35), e);
		
		// Create array of friendly ants and enemies = all battle participants
		participants = new ArrayList<BattleResolutionParticipant>(ants.size() + enemies.size()); 

		int id = 0;

		// Add all of the enemies
		for(Map.Entry<Tile, Set<Tile>> enemy : enemies.entrySet()) {
			BattleResolutionParticipant brp = new BattleResolutionParticipant(map, false, enemy.getKey(), ++id);
			participants.add(brp); // add to full participants list
			mapOriginalState.put(enemy.getKey(), brp); // add to original state list
			mapRunningState.put(enemy.getKey(), brp); // add to running state list (for now)
			enemyLocations.add(enemy.getKey());
			allEnemyBRPs.add(brp);
		}

		// Add all of the friendly ants
		for(Map.Entry<Tile, Set<Tile>> ant : ants.entrySet()) {
			BattleResolutionParticipant brp = new BattleResolutionParticipant(map, true, ant.getKey(), ++id);
			antTilesInBattle.add(ant.getKey());
			participants.add(brp);  // add to full participants list
			mapOriginalState.put(ant.getKey(), brp); // add to original state list
			mapRunningState.put(ant.getKey(), brp); // add to running state list (for now)
			friendLocations.add(ant.getKey());
			allFriendBRPs.add(brp); 
			
			// Also add the connections between participants and potential enemies (needed to add the enemies in above loop first)
			for(Tile t: ant.getValue()) {
				BattleResolutionParticipant brpEnemy = mapOriginalState.get(t);
				brp.addPotentialOpponent(brpEnemy);
				brpEnemy.addPotentialOpponent(brp);
			}
		}
		
		if(participants.size() == 0) {
			map.log("Looks like there are no battles... later!");
			return;
		}
		
		// Before we start, loop through all friendly ants and remove moves that would push us into
		// a non-battle resolution friendly ant.
		for(int i=0; i<participants.size(); i++)
		{
			// Note - we should also remove options that won't work for the computer for better accuracy
			// Like if they were up against water or if they had one of their friends behind them
			BattleResolutionParticipant brp = participants.get(i);
			if(!brp.isFriendly()) {
				continue;
			}
	
			// Remove directions this player cannot move because there is already another non-BR dude there
			for (Iterator<BattleResolution.direction> itrDirections = brp.getPossibleMoves().iterator(); itrDirections.hasNext(); ) 
			{
				BattleResolution.direction direction = itrDirections.next();
				Tile tileNew = brp.getTile(direction);
				
				// Check for ants on this space that are not in battle
				if(map.getLocationAntMap().containsKey(tileNew) && !mapOriginalState.containsKey(tileNew)) {
					logThorough("REMOVED tile " + tileNew + " because non-BR dude there: from " + brp);
					itrDirections.remove();
					removedTiles.add(tileNew);
				}
			}
		}		
		
		// Step 1: make best guess at all moves - friendly and enemy
		// - Move everybody towards each other
		TreeMultiMap<Float, Tile> moves;
		for(int i=0; i<participants.size(); i++)
		{
			BattleResolutionParticipant brp = participants.get(i);
			InfluenceMap oppositionMap = brp.isFriendly() ? iMapEnemyAnts : iMapFriendlyAnts;
					
			moves = oppositionMap.getMoves(brp.getStartLocation());
			
	       	for(TreeMultiMap.Entry<Float, Tile> move : moves.entryList())
	       	{
	       		if(!mapRunningState.containsKey(move.getValue())) {
	    			map.log("Making inital move for " + brp + " to " + move.getValue());
	       			moveParticipantData(brp, brp.getDirection(move.getValue()));
	       		}
	       		// only try moving in the first direction, if not, stay put
	       		break;
	       	}
			
			// Put each ant in the proposed moves map based on hill climb
			// Put each enemy ant on the moves map based on 
			// more power --> move forward
			// less power --> move back
			// even power --> stand still
		}
		
		// Step 2: Set up the initial battle information for all participants

		// First simulate the entire battle between all friends and enemies
		this.performBattleAndUpdateParticipants(allFriendBRPs, allEnemyBRPs);
		
		for(int i=0; i<participants.size(); i++)
		{
			// Next update all of the other per-participant info (horizon, perimeter, etc...)
			BattleResolutionParticipant brp = participants.get(i);
			updateNonBattleHeuristicData(brp);
		}

		// Step 3: iterate until we're out of time
		int iMaxIters = 12345678;
		while(map.getGame().getTimeRemaining() > 50 && iMaxIters-- > 0) {
			iterate();
		}
		
		// Step 4: Move ants them to the best new, randomly-sampled position
		// - tweak their moves to the most-often-best one to avoid the chance that you had a bad random number causing an ant to make a stupid move. You could even do this twice in a row in case an ant was in a bad position, moves to a new one, and another ant wants to move where it used to be.
		for(int i=0; i<participants.size(); i++)
		{
			 BattleResolutionParticipant brp = participants.get(i);
			 map.log(brp.toStringLong());
		}
	}
	
	private void iterate() 
	{
		// map.log("Enter BattleResolution.iterate");

		// Step 1: shuffle the list of ants
		Collections.shuffle(participants);

		// Step 2: traverse shuffled list in order, updating best move for each ant
		for(int i=0; i<participants.size(); i++)
		{
			BattleResolutionParticipant brp = participants.get(i);
			logThorough("Participant: " + brp);
			
			// GET BEST MOVE
			//   for this participant given the current world if there is a tie, we'll increment both
			Pair<Set<BattleResolution.direction>, Set<BattleResolution.direction>> bestAndPossibleMoves = evaluate(brp);
			
			if(bestAndPossibleMoves == null) {
				map.logerror("BattleResolution.iterate: bestmoves came back null from evaluate function: " + brp);
				map.logerror("BRP possible moves: " + brp.getPossibleMoves());
				continue;
			}
			
			logThorough("# of best moves: " + bestAndPossibleMoves.first.size());
			logThorough("bestmoves: " + bestAndPossibleMoves.first);
			
			// INCREMENT BEST MOVES IN DISTRO 
			for(BattleResolution.direction direction : bestAndPossibleMoves.first) {
				logThorough("BattleResolutionParticipant.increment " + direction);
				brp.increment(direction);
			}
			
			// MOVE ANT TO RANDOM SPOT BASED ON WEIGHTED ROULETTE SPIN
			// - Keep trying until we get a direction that is both possible and not occupied
			BattleResolution.direction nextMove = BattleResolution.direction.X; // temporary
			boolean bDone = false;
			while(!bDone) {
				nextMove = brp.getRouletteSpin();
				if(brp.getPossibleMoves().contains(nextMove) && bestAndPossibleMoves.second.contains(nextMove)) {
					bDone = true;
				}
			}
			
			// Tile tileCurrent = brp.getStartLocation();
			// Tile tileNew = getTile(tileCurrent, nextMove); 
			moveParticipantAndUpdateBattles(brp, nextMove);
			
			// map.log("Moved participant from " + tileCurrent + " to " + tileNew);
		}
		// TODO: (version 2) If the same move is the best 99% of the time, stop sampling this ant and solidify his move		
		
	}


	//	You're trying to maximize your evaluation function, which measures how good you think the resulting position 
	//	is to you number of ants, captured hills, area control by some measure, total map exploration, availability 
	//	of defensive ants
		
	//  Your enemies are trying to minimize this same function: that is, you assume they're trying to make 
	//	the situation as bad for you as possible according to your own evaluation function

	// A1k0n: (On casualty trade offs) I don't, but that could be a nice extension. My scoring is basically a bunch of arbitrary heuristics.
	private Pair<Set<BattleResolution.direction>, Set<BattleResolution.direction>> evaluate(BattleResolutionParticipant brp) 
	{
		logThorough("********** Begin evaluating moves for " + brp);
		
		// Clear the data structure that holds the scores
		scoreMap.clear();

		// Get all directions this player could potentially move.  Create a new set of these directions as
		// there was a bug before where we actually deleted them from the object.
		Set<BattleResolution.direction> directions = new HashSet<BattleResolution.direction>();
		for(BattleResolution.direction dir : brp.getPossibleMoves()) {
			directions.add(dir);
		}
		
		// Remove directions this player cannot move because there is already another dude there
		for (Iterator<BattleResolution.direction> itrDirections = directions.iterator(); itrDirections.hasNext(); ) 
		{
			BattleResolution.direction direction = itrDirections.next();
			Tile tileNew = brp.getTile(direction);

			if(!map.getIlk(tileNew).isMovable()) {
				map.log("What the fuck... you can't move to " + tileNew);
			}
								
			// My friend is on this space, remove this as an option, make sure not to remove yourself from this pool
			if(mapRunningState.containsKey(tileNew) && !tileNew.equals(brp.getCurrentLocation())) {
				logThorough("REMOVING MOVE OPTION " + tileNew + " for " + brp);
				itrDirections.remove();
			}
		}

		if(directions.size() == 0) {
			map.logerror("BattleResolution.evaluate: ended up with an empty set of directions (1)");
			return null;
		}

		logThorough("Possible moves: " + directions);
		
		// TODO: What do we do if we only have 1 move?
		
		// Now we have only good moves in the directions set, score each one
		// Do the scoring with the current state of the ants
		// If enemy, use a function that tries to minimize our score (like taking an even trade or better in BR)
		// TODO: fix the scoring... it's doing it randomly now
//		Set<Tile> friends = new HashSet<Tile>();
//		Set<Tile> enemies = new HashSet<Tile>();

//		for(int i=0; i<participants.size(); i++)
//		{
//			BattleResolutionParticipant participant = participants.get(i);
//			
//			logThorough("Adding participant: " + participant);
//			logThorough("participant.getId(): " + participant.getId());
//			logThorough("brp.getId(): " + brp.getId());
//			
////			if(participant.getId() == brp.getId()) {
////				continue;
////			}
//			
//			// If we're on the same team, add to friends list
//			if(participant.isFriendly() == brp.isFriendly()) {
//				friends.add(participant.getCurrentLocation());
//			}
//			else {
//				enemies.add(participant.getCurrentLocation());
//			}
//		}
//		
//		logThorough("Friends: " + friends);
//		logThorough("Enemies: " + enemies);
//		
		// Move the participant from the tile they're currently on to each of the possible moves
		// adjusting the battle along the way
		for(BattleResolution.direction direction : directions)
		{
			// Move the participant to the new spot
			// - returns all friends and enemies affected by the move
			moveParticipantAndUpdateBattles(brp, direction);
			
			// Calculate the battle
			float fScore = getCurrentBattleScore();

			// Make the score negative for an enemy so they try to pick worst move
			fScore *= brp.isFriendly() ? 1 : -1;
			logThorough("After reversing scores for enemies: " + fScore);
			
			// Always make the participant want to move forward
			fScore -= brp.getCombinedEnemyDistance();
			logThorough("After subtracting the total distance between them: " + fScore);
			
			scoreMap.put(fScore, direction);
		}
		
		// map.log("scoreMap: " + scoreMap + " for " + brp);
		
		if(scoreMap.firstKey() != null && scoreMap.containsKey(scoreMap.firstKey())) {
			return new Pair<Set<BattleResolution.direction>, Set<BattleResolution.direction>>(scoreMap.get(scoreMap.firstKey()), directions);
		}
		else {
			map.logerror("BattleResolution.evaluate: ended up with an empty set of directions (2)");
			return null;
		}
	}
	
	// Pair<Set<BattleResolutionParticipant>, Set<BattleResolutionParticipant>>
	private void moveParticipantAndUpdateBattles(BattleResolutionParticipant brp, BattleResolution.direction direction) 
	{
		// MUST BE FROM STARTDIRECTION!!!

		// Perform the actual move of the participant in our hypothetical battleground
		Tile tileTo = moveParticipantData(brp, direction);
		
		this.logThorough("About to move " + brp.getStartLocation() + "(start) to " + tileTo);
		// this.logThorough("Gotta add old enemies first");
		
		// Keep track of all participants (friendly and enemy) affected by this move
		Set<BattleResolutionParticipant> participantsAffectedByMove = new HashSet<BattleResolutionParticipant>();
    	
		// Add all enemies I used to be able to see
		participantsAffectedByMove.addAll(brp.currentlyEngagedEnemyBRPs()); 

		// this.logThorough("participantsAffectedByMove (old enemies): " + participantsAffectedByMove);

		// I'm also affected by the move
		participantsAffectedByMove.add(brp);

		// this.logThorough("participantsAffectedByMove (add me): " + participantsAffectedByMove);

		// Get all the new enemies I can now see, and add them
		Set<BattleResolutionParticipant> newEnemies = getEnemiesCoveringTile(tileTo, brp.getPotentialOpponents());

		// this.logThorough("newEnemies: " + newEnemies);
		
		participantsAffectedByMove.addAll(newEnemies);

		// this.logThorough("participantsAffectedByMove (after adding new): " + participantsAffectedByMove);

		// Keep a queue of people affected and recurse through until all affected participants have been followed
		List<BattleResolutionParticipant> lstToAnalyze = new LinkedList<BattleResolutionParticipant>();
		lstToAnalyze.addAll(participantsAffectedByMove);
		
		// Keep looping through all of the participants currently in the list to see how far the affect of this move stretches
   		Set<BattleResolutionParticipant> alreadyAnalyzed = new HashSet<BattleResolutionParticipant>();
		
    	while(!lstToAnalyze.isEmpty()) 
    	{
    		BattleResolutionParticipant next = lstToAnalyze.remove(0);

    		// Add this ant to the affected group
    		participantsAffectedByMove.add(next);

    		// this.logThorough("Analyzing: " + next);

			// If we've already added it, skip over
    		if(alreadyAnalyzed.contains(next)) {
				continue;
			}

    		alreadyAnalyzed.add(next); // Ok, now I've been added and analyzed

    		// this.logThorough("brpNext currentlyEngagedEnemyBRPs: " + next.currentlyEngagedEnemyBRPs());

    		lstToAnalyze.addAll(next.currentlyEngagedEnemyBRPs());

    		// this.logThorough("participantsAffectedByMove (adding the following for " + next +  " ): " + next.currentlyEngagedEnemyBRPs());
    	}
		
		// Prepare the affected sets of enemies / friends for returning
    	Set<BattleResolutionParticipant> friendsAffected = new HashSet<BattleResolutionParticipant>();
    	Set<BattleResolutionParticipant> enemiesAffected = new HashSet<BattleResolutionParticipant>();
    	
    	for(BattleResolutionParticipant brpAffected : participantsAffectedByMove) {
    		if(brpAffected.isFriendly()) {
    			friendsAffected.add(brpAffected);
    		}
    		else {
    			enemiesAffected.add(brpAffected);
    		}
    	}

		// Perform the battle resolution and update the BattleResolutionParticipant records
		performBattleAndUpdateParticipants(friendsAffected, enemiesAffected);    	
	}

	private Tile moveParticipantData(BattleResolutionParticipant brp, BattleResolution.direction direction) {
		// Move the BRP from it's current location to the chosen location
		this.logThorough("moveParticipantData brp: " + brp.toStringLong() + " in direction " + direction);
		
		Tile tileFrom = brp.getCurrentLocation();
		Tile tileTo = brp.getTile(direction);

		mapRunningState.remove(tileFrom);
		mapRunningState.put(tileTo, brp);
		brp.move(direction, tileTo);

		if(brp.isFriendly()) {
			friendLocations.remove(tileFrom);
			friendLocations.add(tileTo);
		}
		else {
			enemyLocations.remove(tileFrom);
			enemyLocations.add(tileTo);
		}
		
		return tileTo;
	}
	
	private Set<BattleResolutionParticipant> getEnemiesCoveringTile(Tile tile, Set<BattleResolutionParticipant> potentialEnemyBRPs)
	{
		Set<BattleResolutionParticipant> enemies = new HashSet<BattleResolutionParticipant>();
		int iRadius2 = map.getGame().getAttackRadius2();
		for(BattleResolutionParticipant brpEnemy: potentialEnemyBRPs)
		{
			int distance = map.getDistanceSquared(tile, brpEnemy.getCurrentLocation());
			if(distance <= iRadius2) {
				enemies.add(brpEnemy);
			}
		}	
		return enemies;
	}
	
	private void performBattleAndUpdateParticipants(Set<BattleResolutionParticipant> friendBRPs, Set<BattleResolutionParticipant> enemyBRPs)
	{
		logThorough("Starting updateBattle");
		logThorough("friends to recalculate for battle: " + friendBRPs);
		logThorough("enemies to recalculate for battle: " + enemyBRPs);

		// First, revive everybody in this battle, then we'll kill people off again
		for(BattleResolutionParticipant brp: friendBRPs) {
			brp.clearCurrentlyEngagedEnemies();
			brp.setDeadFlg(false);
		}
		for(BattleResolutionParticipant brp: enemyBRPs) {
			brp.clearCurrentlyEngagedEnemies();
			brp.setDeadFlg(false);
		}

		// Now, re-run the battle setting people to dead if they should be
		// Have each participant battle only people in their potential attack range
		// int totalDistance = 0;
		for(int i=0; i<participants.size(); i++) 
		{
			BattleResolutionParticipant brpParticipant = participants.get(i);

//			// We should only need to loop through either friends or enemies to build the connections
//			if(!brpParticipant.isFriendly()) {
//				continue;
//			}
			// map.log("brpParticipant: " + brpParticipant);
			// map.log("brpParticipant.getPotentialOpponents(): " + brpParticipant.getPotentialOpponents());
			
			for(BattleResolutionParticipant brpEnemy : brpParticipant.getPotentialOpponents()) 
			{
				Tile tileParticipant = brpParticipant.getCurrentLocation();
				Tile tileEnemy = brpEnemy.getCurrentLocation();

				// map.log("tileParticipant: " + tileParticipant);

				// If these 2 are in range, add connection between eachother
				if(map.getDistanceSquared(tileParticipant, tileEnemy) <= map.getGame().getAttackRadius2()) 
				{
					// map.log("tileEnemy: " + tileEnemy);
					// map.log("brpEnemy: " + brpEnemy);

					brpParticipant.addEngagedEnemy(brpEnemy);
					brpEnemy.addEngagedEnemy(brpParticipant); 
				}
			}
		}

		// Loop through all participants.  If their focus is greater than their lowest enemy focus, kill them
		for(int i=0; i<participants.size(); i++) 
		{
			BattleResolutionParticipant brp = participants.get(i);
			int myFocus = brp.getMyFocus();
			int lowestEnemyFocus = brp.getEnemyMinFocus();

			if(myFocus >= lowestEnemyFocus){
				brp.setDeadFlg(true);
			}
		}
	}	

	private void updateNonBattleHeuristicData(BattleResolutionParticipant brp) 
	{
		// Is this participant on the enemy attack perimeter?
		Set<Tile> perimeter = brp.isFriendly() ? map.getFriendlyAttackPerimeter() : map.getEnemyAttackPerimeter();
		brp.setAttackPerimeterFlg(perimeter.contains(brp.getCurrentLocation()));
		
		// Is this participant on the enemy attack horizon?
		Set<Tile> horizon = brp.isFriendly() ? map.getFriendlyAttackHorizon() : map.getEnemyAttackHorizon();
		brp.setAttackHorizonFlg(horizon.contains(brp.getCurrentLocation()));

		// Should this participant be grabbing food?
		brp.setGettingFood(iMapFood.hasInfluence(brp.getCurrentLocation()));

				
//		// Is this participant on the enemy attack horizon?
//		Set<Tile> horizon = brp.isFriendly() ? map.getFriendlyNextAttackHorizon() : map.getEnemyNextAttackHorizon();
//		brp.setNextAttackHorizonFlg(horizon.contains(brp.getCurrentLocation()));
		
		
		// Is this participant on the enemy attack horizon?
		Set<Tile> hills = brp.isFriendly() ? map.getFriendlyHills() : map.getAllEnemyHills();
		brp.setBaseNeighborFlg(hills.contains(brp.getCurrentLocation()));
		
		// Is this participant on the enemy attack horizon?
//		Set<Tile> hillNeighbors = brp.isFriendly() ? map.getFriendlyHillsAndNeighbors() : map.getEnemyHillsAndNeighbors();
//		brp.setBaseNeighborFlg(horizon.contains(brp.getCurrentLocation()));
	}
		
	private float getCurrentBattleScore() {
		float fScore = 0;
		
		logThorough("Getting current battle score: ");
		
		for(int i=0; i<participants.size(); i++)
		{
			BattleResolutionParticipant brp = participants.get(i);
			fScore += brp.isFriendly() ? brp.getScore() : brp.getScore() * -1;

			logThorough("Score: " + brp.getScore() + " | for : " + brp);
		}

		return fScore;
	}
	
//	private Tile getTile(Tile tile, BattleResolution.direction dir) {
//		return map.getTile(tile, dir.getRowOffset(), dir.getColOffset());
//	}
	
	public ArrayList<BattleResolutionParticipant> getParticipants() {
		return participants;
	}

	public ArrayList<BattleResolutionParticipant> getMyParticipants() {
		ArrayList<BattleResolutionParticipant> friendlyParticipants = new ArrayList<BattleResolutionParticipant>(); 

		for(int i=0; i<participants.size(); i++)
		{
			BattleResolutionParticipant brp = participants.get(i);
			if(brp.isFriendly()) {
				friendlyParticipants.add(brp);
			}
		}

		return friendlyParticipants;
	}
	
	
	public Set<Tile> getAntsInBattle() {
		return antTilesInBattle;
	}
	
	public Set<Tile> getRemovedTiles() {
		return removedTiles;
	}
	
	private void logThorough(String str) {
		// map.log(str);
	}
	
}
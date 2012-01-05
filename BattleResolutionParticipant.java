import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;


public class BattleResolutionParticipant {

	private AntMap map;
	private int[] distro;
	private int[] movepossible;
	private int count;
	private int id;
	private boolean bFriendly;
	private Tile participantStart;
	private Tile participantCurrent;
	private Set<Tile> currentlyEngagedEnemyTiles;
	private Set<BattleResolutionParticipant> potentialOpponents; // These need to be BRPs too since they'll be moving
	private Set<BattleResolutionParticipant> currentlyEngagedEnemyBRPs;
	private LinkedList<BattleResolution.direction> possibleMoves;
	private BattleResolution.direction brdCurrent;
	private HashMap<BattleResolution.direction, Tile> mapDirectionToTile;
	private HashMap<Tile, BattleResolution.direction> mapTileToDirection;
	private boolean bCurrentlyDead;
	private boolean bCurrentlyOnAttackHorizon;
	private boolean bCurrentlyOnAttackPerimeter;
	private boolean bCurrentlyOnEnemyBasePerimeter;
	private boolean bCurrentlyGettingFood;
	
	public BattleResolutionParticipant(AntMap map, boolean bFriendly, Tile participantStart, int id) {
		this.map = map;
		this.distro = new int[5];
		this.movepossible = new int[5];
		this.count = 5;
		this.id = id;
		this.participantStart = participantStart;
		this.participantCurrent = participantStart;
		this.potentialOpponents = new HashSet<BattleResolutionParticipant>();
		this.currentlyEngagedEnemyTiles = new HashSet<Tile>();
		this.currentlyEngagedEnemyBRPs = new HashSet<BattleResolutionParticipant>();
		
		this.bFriendly = bFriendly;
		this.possibleMoves = new LinkedList<BattleResolution.direction>();

		this.bCurrentlyDead = false;
		this.bCurrentlyOnAttackHorizon = false;
		this.bCurrentlyOnAttackPerimeter = false;
		this.bCurrentlyOnEnemyBasePerimeter = false;
		this.bCurrentlyGettingFood = false;
		
		this.mapDirectionToTile = new HashMap<BattleResolution.direction, Tile>();
		this.mapTileToDirection = new HashMap<Tile, BattleResolution.direction>();
		this.brdCurrent = BattleResolution.direction.X;
		
		addPossibleMoves();

		// Init distro to all 1's
		for(int i = 0; i < 5; i++) {
			distro[i] = 1;
		}
		for(int i = 0; i < 5; i++) {
			movepossible[i] = 1;
		}
	}

	public void increment(BattleResolution.direction dir) {
		// map.log("Incrementing " + this.toString() + " | " + dir);

		distro[dir.ordinal()]++;
		count++;
	}

	public Tile move(BattleResolution.direction dir, Tile tile) {
		participantCurrent = mapDirectionToTile.get(dir);
		brdCurrent = dir;
		movepossible[dir.ordinal()]++;
		return participantCurrent;
	}

	public void removeMovePossibility(BattleResolution.direction direction) {
		possibleMoves.remove(direction);
		mapDirectionToTile.remove(direction);
	}

	private void addPossibleMoves() {
		possibleMoves.add(BattleResolution.direction.X);
		mapDirectionToTile.put(BattleResolution.direction.X, participantStart);
		mapTileToDirection.put(participantStart, BattleResolution.direction.X);

		for(Aim aim : participantStart.getMovableDirections(map)) {
			Tile tile = map.getTile(participantStart, aim);
			possibleMoves.add(aim.getBRDistroIndex());
			mapDirectionToTile.put(aim.getBRDistroIndex(), tile);
			mapTileToDirection.put(tile, aim.getBRDistroIndex());
		}
	}

	public LinkedList<BattleResolution.direction> getPossibleMoves() {
		return possibleMoves;
	}
	
	public Tile getTile(BattleResolution.direction dir) {
//		if(dir.equals(BattleResolution.direction.X)) {
//			return participantStart;
//		}
		if(mapDirectionToTile.get(dir) == null) {
			map.logerror("BattleResolution.getTile returned null for brp " + this.toString() + " with possible directions: " + possibleMoves);
		}
		return mapDirectionToTile.get(dir);
	}

	public BattleResolution.direction getDirection(Tile tile) {
//		if(tile.equals(participantStart)) {
//			return BattleResolution.direction.X;
//		}
		
		return mapTileToDirection.get(tile);
	}

	public BattleResolution.direction getRouletteSpin() {
		int iRandom = (int)Math.floor(count * Math.random());
		// map.log("iRandom: " + iRandom);
		for(int i = 0; i < 5; i++) {
			iRandom -= distro[i];
			if(iRandom < 0) {
				// map.log("Return index: " + i);
				return BattleResolution.direction.get(i);
			}
		}
		map.logerror("BattleResolutionParticipant.getRouletteMove: should not have gotten here - bad math");
		return BattleResolution.direction.X;
	}
	
	public int[] getDistro() {
		return distro;
	}

	public int[] getMovePossibleArray() {
		return movepossible;
	}
	
	public int getCount() {
		return count;
	}

	public Tile getStartLocation() {
		return participantStart;
	}
	
	public Tile getCurrentLocation() {
		return participantCurrent;
	}

	public void clearCurrentlyEngagedEnemies() {
		currentlyEngagedEnemyTiles.clear();
		currentlyEngagedEnemyBRPs.clear();
	}

	public void addEngagedEnemy(BattleResolutionParticipant brpEnemy) {
		currentlyEngagedEnemyBRPs.add(brpEnemy);
		currentlyEngagedEnemyTiles.add(brpEnemy.getCurrentLocation());
	}

	public void addPotentialOpponent(BattleResolutionParticipant opponent) {
		potentialOpponents.add(opponent);
	}

	public int getMyFocus() {
		return currentlyEngagedEnemyTiles.size();
	}

	public int getEnemyMinFocus() {
		int iMinFocus = Integer.MAX_VALUE;
		
		for(BattleResolutionParticipant brpEnemy: currentlyEngagedEnemyBRPs) { 
			iMinFocus = Math.min(brpEnemy.getMyFocus(), iMinFocus);
		}
		return iMinFocus;
	}

	public Set<Tile> currentlyEngagedEnemyTiles() {
		return currentlyEngagedEnemyTiles;
	}
	
	public Set<BattleResolutionParticipant> currentlyEngagedEnemyBRPs() {
		return currentlyEngagedEnemyBRPs;
	}

	public int getCombinedEnemyDistance() {
		int distance = 0;
		for(BattleResolutionParticipant brp : potentialOpponents) {
			distance += map.getDistanceSquared(participantCurrent, brp.getCurrentLocation());
		}
		return distance;
	}
	public Set<BattleResolutionParticipant> getPotentialOpponents() {
		return potentialOpponents;
	}

	public boolean isFriendly() {
		return bFriendly;
	}

	public int getId() {
		return id;
	}

	public void setDeadFlg(boolean bool) {
		bCurrentlyDead = bool;
	}

	public void setAttackHorizonFlg(boolean bool) {
		bCurrentlyOnAttackHorizon = bool;
	}

	public void setAttackPerimeterFlg(boolean bool) {
		bCurrentlyOnAttackPerimeter = bool;
	}

	public void setBaseNeighborFlg(boolean bool) {
		bCurrentlyOnEnemyBasePerimeter = bool;
	}

	public void setGettingFood(boolean bool) {
		bCurrentlyGettingFood = bool;
	}

	public float getScore() 
	{
		float fScore = 0;

		if(bCurrentlyDead) {
			fScore += bFriendly ? -1000 : -700;
		}

		if(bFriendly && bCurrentlyGettingFood) {
			fScore += 400;
		}
		
		// fScore += bCurrentlyOnAttackHorizon ? 250 : 0;
		// fScore += bCurrentlyOnAttackPerimeter ? 600 : 0;
		// fScore += bCurrentlyOnEnemyBasePerimeter ? 1500 : 0;

		// map.log("Score for: " + (bFriendly ? "Friendly" : "Enemy") + " ant: " + fScore + " | Dead: " + bCurrentlyDead + " | Distance: " + getCombinedEnemyDistance() + " | At Base: " + bCurrentlyOnEnemyBasePerimeter + " | On Attack Perim: " + bCurrentlyOnAttackPerimeter + " | On Attack Horizon: " + bCurrentlyOnAttackHorizon);
		
		return fScore;
	}
	
	/**
     * {@inheritDoc}
     */
    @Override

    public String toString() {
    	return id + ": " + (bFriendly ? "Friendly" : "Enemy") + " at (" + participantCurrent + ")";
    }

    public String toStringLong() {
    	return id + ": " + (bFriendly ? "Friendly" : "Enemy") + " ant starting at (" + participantStart + ") --> At (" + participantCurrent + ") with scores " + distro[0] + " | " + distro[1] + " | " + distro[2] + " | " + distro[3] + " | " + distro[4] + " and possible scores " + movepossible[0] + " | " + movepossible[1] + " | " + movepossible[2] + " | " + movepossible[3] + " | " + movepossible[4] + " with currently engaged enemies " + currentlyEngagedEnemyTiles;
    }
}

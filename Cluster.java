import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class Cluster {

	private Set<Tile> members;
	private Set<Tile> perimeter;
	private List<Tile> horizon;
	private Map<Tile, Integer> mapTileToWalkNumber;
	private Aim aimCurrent;
	private int iCount;
	private Tile tileCurrent;
	
	public Cluster() {
		members = new HashSet<Tile>();
		perimeter = new HashSet<Tile>(); 
		horizon = new LinkedList<Tile>();
		mapTileToWalkNumber = new HashMap<Tile, Integer>();
	}

	public Cluster(Tile t) {
		this();
		add(t);
	}
	
	public boolean isMember(Tile t) {
		return members.contains(t);
	}
	
	public Set<Tile> getMembers() {
		return members;
	}
	
	public Map<Tile, Integer> getMapTileToWalkNumber() {
		return mapTileToWalkNumber;
	}
	
	public void add(Tile t) {
		members.add(t);
	}
	
	public void recomputeHorizonAndWalk(AntMap map)
	{
		mapTileToWalkNumber.clear();
		horizon.clear();
		perimeter.clear();
		
		// First set up perimeter and horizon tiles
		for (Tile tile: members) 
		{
      		boolean bPerimeter = false;
			Tile neighbor = null;

      		// If any of the neighboring items (including diagonals) cannot be found, then the unfound tile is horizon
			for(Tile tileOffset: map.getOffsetNeighborsWithDiags().getOffsets()) {
				neighbor = map.getTile(tile, tileOffset);
	      		if(!members.contains(neighbor)) {
	      			horizon.add(neighbor);
	      			bPerimeter = true;
	      		}
			}			
			
			// If any of the items were horizon tiles, then this is a perimeter tile
			if(bPerimeter) {
      			perimeter.add(tile);
      		}
       	}		
		
		// Next, walk around the horizon tiles giving each horizon tile a number, starting with 1 and moving on
		iCount = 1;
		aimCurrent = Aim.WEST;
		tileCurrent = horizon.get(0);

		// Walk around the horizon
		while(walk(map)) { }
	}

	private boolean walk(AntMap map) 
	{
		List<Aim> directions = new ArrayList<Aim>();
		directions.add(aimCurrent.left());
		directions.add(aimCurrent);
		directions.add(aimCurrent.right());
		directions.add(aimCurrent.behind());
		
		// try 4 directions in order, attempting to turn left at corners
		for (Aim new_direction : directions) {
			Tile destination = map.getTile(tileCurrent, new_direction);
			// if this tile is a horizon tile and it hasn't already been added, add and return
			if(horizon.contains(destination) && !mapTileToWalkNumber.containsKey(destination)) {
				tileCurrent = destination;
				mapTileToWalkNumber.put(tileCurrent, iCount);
				aimCurrent = new_direction;
				iCount++;
				return true;
			}
		}

		return false;
	}

	public int getPenalty(AntMap map, Tile tile, Aim aim) 
	{
		// Get direction opposite to this aim
		Aim opposite = Aim.getOpposite(aim);
		
		Tile tileHorizon1 = getHorizonTile(map, tile, aim);
		Tile tileHorizon2 = getHorizonTile(map, tile, opposite);

		if(tileHorizon1 == null || tileHorizon2 == null) {
			map.logerror("Cluster.getPenalty: horizon tile null. Tile 1: " + ((tileHorizon1==null)?"Null":tileHorizon1) + " | " + ((tileHorizon2==null)?"Null":tileHorizon2));
			return 0;
		}

		if(mapTileToWalkNumber == null) {
			map.logerror("Cluster.getPenalty: mapTileToWalkNumber is null");
			return 0;
		}

		Integer iWalk1 = mapTileToWalkNumber.get(tileHorizon1);
		Integer iWalk2 = mapTileToWalkNumber.get(tileHorizon2);

		if(iWalk1 == null || iWalk2 == null) {
			map.logerror("Cluster.getPenalty: a walk number is null" + ((iWalk1==null)?"Null":iWalk1) + " | " + ((iWalk2==null)?"Null":iWalk2));
			return 999999;
		}
		
		return Math.min(Math.abs(iWalk1 - iWalk2), mapTileToWalkNumber.size() - Math.abs(iWalk1 - iWalk2));
	}
	
	private Tile getHorizonTile(AntMap map, Tile tile, Aim aim)
	{
		int iCurrStep = 0;
		int iMaxSteps = (aim == Aim.NORTH || aim == Aim.SOUTH) ? map.getCols() : map.getRows();
		
		Tile tileTemp = tile;
		// Keep looking for a horizon tile by navigating in this direction
		// If we loop around the whole board, jump out (this means the entire board is covered with water in this row or column)
		while((members.contains(tileTemp) || horizon.contains(tileTemp)) && iCurrStep++ < iMaxSteps) {
			tileTemp = map.getTile(tileTemp, aim);
			if(horizon.contains(tileTemp)) {
				return tileTemp;
			}
		}
		return null;
	}
	
	public boolean belongs(Tile t, AntMap map) {
		Offset off = map.getOffsetNeighborsWithDiags(); 
		for(Tile member: members) {
			for(Tile offset: off.getOffsets()) {
				if(map.getTile(member, offset).equals(t)) {
					return true;
				}
			}
		}
		return false;
	}

	
	
	
	
	 /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
		return members.toString();
    }	
	
}

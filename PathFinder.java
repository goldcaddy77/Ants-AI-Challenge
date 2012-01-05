/*    
 * A* algorithm implementation.
 */

import java.util.*;

/*
 * Example.
 */
public class PathFinder extends AStar<Tile>
{
    // private PathStore store;
    private AntMap map;
    // private Tile tileFrom;
    private Tile tileTo;
    private boolean bGetToNeighbor;
    
    // TODO: it's possible to give this a bad destination (water) and it will spin through the cycles.
    public PathFinder(PathStore store, Tile from, Tile to, boolean bGetToNeighbor){
    	// this.store = store;
    	this.map = store.getMap();
    	// this.tileFrom = from;
    	this.tileTo = to;
    	this.bGetToNeighbor = bGetToNeighbor;
    }

    protected boolean isGoal(Tile t) {
        return t.equals(tileTo) || (bGetToNeighbor && t.isNeighbor(tileTo, map));
    }

    protected Double g(Tile from, Tile to)
    {
    	if(from.equals(to)) {
    		return 0.0;
    	}
    	else if(map.getIlk(to).isPassable() || isGoal(to)) {
    		return 1.0;
    	}
    	else {
            return Double.MAX_VALUE;
    	}
    }
    
    protected Double h(Tile from, Tile to){
        /* Use the Manhattan distance heuristic.  */
        return (double) map.getDistance(from, to);
    }
    
    protected List<Tile> generateSuccessors(Tile tile){
        List<Tile> successors = new LinkedList<Tile>();

        List<Aim> dirs = new ArrayList<Aim>();
        for(Aim direction : Aim.values()){
        	dirs.add(direction);
        }
        
        while(dirs.size() > 0){
        	Aim direction = dirs.remove(new Random().nextInt(dirs.size()));
        	Tile t = map.getTile(tile, direction);
            if(map.getIlk(t).isPassable() || isGoal(t))
            	successors.add(t);
        }

//      	TreeMultiMap<Integer, Tile> aimDist = new TreeMultiMap<Integer, Tile>();
//      	for(Aim direction : Aim.values())
//        {
//        	Tile t = map.getTile(tile, direction);
//        	int dist = map.getDistance(t, tileTo);
//        	aimDist.put(dist, t);
//        }
//        	
//       	for (TreeMultiMap.Entry<Integer, Tile> entry : aimDist.entryList()) 
//        {
//       		Tile t = entry.getValue();	
//            if(map.getIlk(t).isPassable() || isGoal(t))
//            	successors.add(t);
//        }
        
        return successors;
    }
}
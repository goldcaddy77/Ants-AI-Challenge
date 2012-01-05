/*    
 * A* algorithm implementation.
 */

import java.util.*;

/*
 * Example.
 */
public class PathFinder extends AStar<Tile>
{
    private PathStore store = null;
    private Game game = null;
    private Tile tileFrom = null;
    private Tile tileTo = null;
    
    public PathFinder(PathStore store, Tile from, Tile to){
    	this.store = store;
    	this.game = store.getGame();
    	this.tileFrom = from;
    	this.tileTo = to;
    }

    protected boolean isGoal(Tile t) {
        return t.equals(this.tileTo);
    }

    protected Double g(Tile from, Tile to)
    {
    	if(from.equals(to)) {
    		return 0.0;
    	}
    	else if(this.game.getIlk(to).isPassable() || isGoal(to)) {
    		return 1.0;
    	}
    	else {
            return Double.MAX_VALUE;
    	}
    }
    
    protected Double h(Tile from, Tile to){
        /* Use the Manhattan distance heuristic.  */
        return (double) this.game.getDistance(from, to);
    }
    
    protected List<Tile> generateSuccessors(Tile tile){
        List<Tile> successors = new LinkedList<Tile>();

        for (Aim direction : Aim.values()) {
        	Tile t = game.getTile(tile, direction);
            if(game.getIlk(t).isPassable() || isGoal(t))
            	successors.add(t);
        }
        
        return successors;
    }
}
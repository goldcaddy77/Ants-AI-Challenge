import java.util.HashMap;
import java.util.List;

public class PathStore {

    private AntMap map;
    private boolean bDebug;
    private int size;
    
    private HashMap<Integer, HashMap<Integer, Path>> store = new HashMap<Integer, HashMap<Integer, Path>>();
    
    public PathStore(AntMap map){
    	this.map = map;
    	this.bDebug = false;
    	this.size = 0;
    }	
	
    public AntMap getMap(){
    	return this.map;
    }

    public void emptyCache(){
    	this.store = new HashMap<Integer, HashMap<Integer, Path>>();
    }

    public Path getPath(Tile from, Tile to)
    {
    	Path path = null;
//    	if((path = this.getTileCache(from).get(to.id())) != null) 
//    	{
//    		// refresh the path based on newest game data
//    		path.refresh(this.game);
//    		
//    		// if we learn that this path is obstructed, drop the cache and determine a new path
//    		if(path.obstructed()) {
//    			this.getTileCache(from).put(to.id(), null);
//    			path = getPath(from, to);
//    		}
//		}
//    	else {
        	PathFinder pf = new PathFinder(this, from, to);
        	long begin = System.currentTimeMillis();
        	
        	List<Tile> tiles = pf.compute(from);
        	long end = System.currentTimeMillis(); 
//        	map.log("Time = " + (end - begin) + " ms" ); 
//        	map.log("Expanded = " + pf.getExpandedCounter()); 
//        	map.log("Cost = " + pf.getCost()); 
//        	map.log("Tiles: " + tiles);
    		
        	// System.out.println("Caching tiles: " + tiles);
        	if(tiles != null) {
            	path = cachePath(tiles);
        	}
//    }
    
    	return path;
    }
    
    private Path cachePath(List<Tile> tiles)
    {
    	// For each tile, check to see if it's invisible. If so, mark the path incomplete
        int iLen = tiles.size();
    	boolean bPathComplete = true;
    	for (int i=0; i<iLen; i++) {
       		bPathComplete = bPathComplete && map.isVisible(tiles.get(i));
        }
        
    	// Get the cache for the starting tile
    	HashMap<Integer, Path> tileCache = getTileCache(tiles.get(0));

    	// Create a path from the 2nd tile in the list to the end
    	// do this so that we're not caching the starting node every time
    	if(tiles.size() != 1) {
    		tiles = tiles.subList(1, tiles.size());
       	}
    	
    	Path p = new Path(tiles, bPathComplete);
        	
    	// Add the new path to the tiles cache pointing at the last node
    	tileCache.put(tiles.get(tiles.size() - 1).id(), p);

    	// Keep track of the size of the cache (this isn't an exact count, but a good approximation)
    	size++;
    	
    	return p;
    }
    
    	
    public int getDistance(Tile from, Tile to)
    {
    	Log("getDistance: " + from + " | " + to);
    	if(from.equals(to)) {
    		return 0;
    	}
    	
    	Path p = this.getPath(from, to);
    	if(p == null) {
    		// System.out.println("Failed to get path from " + from + " to " + to);
    		return Integer.MAX_VALUE;
    	}

    	return p.getSize();
    }
    
    private HashMap<Integer, Path> getTileCache(Tile t)
    {
    	HashMap<Integer, Path> tilecache = store.get(t.id());
    	
    	if(tilecache == null) {
    		tilecache = new HashMap<Integer, Path>();
    		store.put(t.id(), tilecache);
    	}
    	return tilecache;
    }

    private void Log(String str)
    {
    	if(bDebug) {
    		System.out.println(str);
    	}
    }
    
    public int size()
    {
    	return this.size;
    }
}
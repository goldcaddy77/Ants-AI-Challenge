import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class Offset {
	
	private int radius2; // radius squared
	// private AntMap map;
	private Set<Tile> perimeter;
	private Set<Tile> horizon;
	private Set<Tile> offsets;
	private Map<Tile, Integer> offsetMap;

	public Offset(int radius2, AntMap map) {
		this(radius2, map, new Tile(0, 0));
	}

	public Offset(int radius2, AntMap map, Tile start) {
		// this.map = map;
		this.radius2 = radius2;
		this.perimeter = new HashSet<Tile>(); 
		this.horizon = new HashSet<Tile>(); 
		this.offsets = new HashSet<Tile>();
		this.offsetMap = new HashMap<Tile, Integer>();

	    int mx = (int)Math.sqrt(radius2);
	    for (int row = -mx; row <= mx; ++row) {
	        for (int col = -mx; col <= mx; ++col) {
	            int d = row * row + col * col;
	            if (d <= radius2) {
	            	// TODO: do we want to do cost = Math.floor(Math.sqrt(radius2 - d - 1))???
		            // int cost = (int)Math.floor(Math.sqrt(radius2 - d));
		            int cost = Math.max(mx + 1 - Math.abs(row) - Math.abs(col), 0); 
		            Tile tileNew = new Tile(row + start.getRow(), col + start.getCol());
	            	this.offsets.add(tileNew);
	            	this.offsetMap.put(tileNew, cost);
	            }
	        }
	    }	

	    setPerimeterAndHorizon(map);
	}
	
	public Set<Tile> getPerimeter() {
		return this.perimeter;
	}

	public Set<Tile> getHorizon() {
		return this.horizon;
	}
	
	public void add(Offset off, AntMap map) 
	{
       	for (Map.Entry<Tile, Integer> tileMap : off.offsetMap.entrySet()) 
       	{
       		if(!this.offsetMap.containsKey(tileMap.getKey())) {
       			map.loginfo("New item added to offset: " + tileMap.getKey());
       			this.offsetMap.put(tileMap.getKey(), tileMap.getValue());
       			this.offsets.add(tileMap.getKey());
       		}
       	}
       	
       	setPerimeterAndHorizon(map);
	}
	
    private void setPerimeterAndHorizon(AntMap map) 
    {
    	horizon.clear();
		perimeter.clear();
		
    	// Set perimeter and horizon
    	// for (Map.Entry<Tile, Integer> tileMap : offsetMap.entrySet()) 
    	for (Tile tile: offsets) 
    	{
      		boolean bPerimeter = false;
    		Tile neighbor = null;

      		// If any of the items to the N, S, E or W cannot be found, then the unfound tile is horizon
    		for(Aim aim: Aim.values()) {
    			neighbor = map.getTileAllowNegatives(tile, aim);
          		if(!offsets.contains(neighbor)) {
          			horizon.add(neighbor);
          			bPerimeter = true;
          		}
    		}			
    		
    		// If any of the items were horizon tiles, then this is a perimeter tile
    		if(bPerimeter) {
      			perimeter.add(tile);
      		}
       	}	
    	
    	map.loginfo("Perim size: " + perimeter.size());
    	map.loginfo("Horizon size: " + horizon.size());
    }

	public int getRadius2(){
		return this.radius2;
	}

	public Map<Tile, Integer> getOffsetMap(){
		return this.offsetMap;
	}

	public Set<Tile> getOffsets(){
		return this.offsets;
	}
	
	
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
    	String strReturn = "";
    	strReturn += "Size: " + String.format("%01d", this.offsetMap.size());
    	
       	for (Map.Entry<Tile, Integer> tileMap : this.offsetMap.entrySet()) 
       	{
       		strReturn += "[" + tileMap.getKey() + ", " + tileMap.getValue() + "] ";
       	}
       	return strReturn;
    }
}

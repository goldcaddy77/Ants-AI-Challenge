import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


public class HeatMap 
{
	private AntMap map;
	private int rows;
	private int cols;
	private int heat[][];
	private Offset offset;
	private int invalidValue; 
	private boolean weighted;
	private Set<Tile> importantTiles;
	private Set<Tile> horizon;
	
	public enum Func {
		 ONE, ONE_ADD, DIST, DIST_SQUARE, MIN, MAX;
	}	
	
	public HeatMap(AntMap map)
	{
		this.map = map;
		this.rows = map.getRows();
		this.cols = map.getCols();
		// this.offset = offset;
		// this.weighted = weighted;
		// this.invalidValue = Integer.MAX_VALUE;
		this.importantTiles = new HashSet<Tile>();
		this.horizon = new HashSet<Tile>();
		
		this.heat = new int[rows][cols];
		this.clear();
	}
	
	// TODO: REMOVED && heat[r][c] < Integer.MAX_VALUE
	public boolean hasHeat() {
        for (int r = 0; r < rows; ++r) {
            for (int c = 0; c < cols; ++c) {
                if(heat[r][c] > 0) {
                	return true;
                }
            }
        }
        return false;
	}
	
	public void clear() {
		horizon.clear();
        for (int r = 0; r < rows; ++r) {
            for (int c = 0; c < cols; ++c) {
                heat[r][c] = 0;
            }
        }
	}
	
	public int[][] getMap() {
		return this.heat;
	}
	
	public Set<Tile> getHorizon() {
		return horizon;
	}
	
	public int getHeat(int r, int c) {
		return heat[map.getRow(r)][map.getCol(c)];
	}

	public int getHeat(Ant a) {
		return this.getHeat(a.getCurrentTile());
	}

	public int getHeat(Tile t) {
		return this.getHeat(t.getRow(), t.getCol());
	}

	public int getHeat(Tile t, Aim a) {
		return this.getHeat(map.getTile(t, a));
	}

	public Tile getColdestNonZeroNeighbor(Tile t) {
		int coolest = Integer.MIN_VALUE;
		Tile tileReturn = null;
		for(Aim aim: Aim.values()) {
			Tile temp = map.getTile(t, aim);
			if(getHeat(temp) > 0 && getHeat(temp) > coolest) {
				coolest = getHeat(temp);
				tileReturn = temp;
			}
		}
		return tileReturn;
	}

	public Tile getHottestNeighbor(Tile t) {
		int hottest= Integer.MIN_VALUE;
		Tile tileReturn = null;
		for(Aim aim: Aim.values()) {
			Tile temp = map.getTile(t, aim);
			if(getHeat(temp) > hottest) {
				hottest = getHeat(temp);
				tileReturn = temp;
			}
		}
		return tileReturn;
	}
	
	// Return true if this spot is the border between heat and no heat
	// ...or this tile has no heat, but one of its neighbors does
	public boolean isBorder(Tile t)
	{
		boolean bNeighborHeat = false;
				
		// map.log("isBorder (" + t + ")... does this tile have heat? I'm showing: " + getHeat(t));

		for(Aim aim: Aim.values()) {
			bNeighborHeat = bNeighborHeat || ((getHeat(t, aim) > 0));
			// map.log("isBorder (" + t + ") checking " + aim + ": " + getHeat(t, aim));
		}

		// map.log("isBorder (" + t + ") returned: " + (getHeat(t) == 0 && bNeighborHeat));

		boolean bReturn = (getHeat(t) == 0 && bNeighborHeat);
		if(bReturn) {
			// map.log("THANK GOD, WE FOUND A BORDER");
		}
		
		return (getHeat(t) == 0 && bNeighborHeat);

	}

	public Set<Tile> findNeighboringBorders(Tile t)
	{
		Set<Tile> tilesRet = new HashSet<Tile>();
		Tile tileTemp;
		for(Aim aim: Aim.values()) {
			tileTemp = map.getTile(t, aim);
			if(isBorder(tileTemp)) {
				tilesRet.add(tileTemp);
			}
		}

		// map.log("findNeighboringBorders (" + t + ") returned: " + tilesRet);
		
		return tilesRet;
	}

	public Set<Ant> getAntsWithHeat(Set<Ant> ants) {
		Set<Ant> antsReturn = new HashSet<Ant>();

		for(Ant ant: ants) {
			if(getHeat(ant.getCurrentTile()) > Integer.MIN_VALUE) {
				antsReturn.add(ant);
			}

		}
		return antsReturn;
	}
	
	public void copyMapFrom(HeatMap hm) {
		heat = this.getCopy(hm.getMap());
	}
	
	public void subtract(HeatMap hm) {
        for (int r = 0; r < rows; ++r) {
            for (int c = 0; c < cols; ++c) {
                if(heat[r][c] == 0 || hm.getHeat(r, c) == 0) {
                	heat[r][c] = Integer.MIN_VALUE;
                }
                else {
                	heat[r][c] -= hm.getHeat(r, c);
                }
            }
        }
	}

	public void add(HeatMap hm) {
        for (int r = 0; r < rows; ++r) {
            for (int c = 0; c < cols; ++c) {
            	int temp = heat[r][c] + hm.getHeat(r, c);
           		heat[r][c] = (temp < heat[r][c]) ? Integer.MAX_VALUE : temp;
            }
        }
	}

	private int[][] getCopy(int[][] mapSource) {
        int[][] temp = new int[rows][cols];
		for (int r = 0; r < rows; ++r) {
            for (int c = 0; c < cols; ++c) {
                temp[r][c] = mapSource[r][c];
            }
        }
		return temp;
	}
	
	public void addImportance(Set<Tile> tiles, Offset offset)
	{
		// Take a copy of the current map tiles, then clear them
		// we'll re-add all tiles that are important based on the offsets
		int[][] temp = getCopy(this.heat);
		this.clear();
				
		this.importantTiles.clear();
		
	    // for each input tile, add importance to all tiles within the given offset
	    for (Tile tile : tiles) 
        {
	    	this.importantTiles.add(tile);
	    	Iterator<Entry<Tile, Integer>> it = offset.getOffsetMap().entrySet().iterator();
		    while (it.hasNext()) 
		    {
		        Map.Entry<Tile, Integer> pairs = (Map.Entry<Tile, Integer>)it.next();
		        Tile tileOffset = map.getTile(tile, pairs.getKey());
		        // int cost = pairs.getValue();
		        
		        // Multiply the offset value by the current heat to get the new heat
		        this.heat[tileOffset.getRow()][tileOffset.getCol()] += temp[tileOffset.getRow()][tileOffset.getCol()];
		    }
    	}
	}

	public int calculateFunc(HeatMap.Func func, int iDistance, int iCurrValue) {
		switch(func) {
			case ONE :
				return 1;
			case ONE_ADD :
				return iCurrValue + 1;
			case DIST :
				return iCurrValue + iDistance;
			case DIST_SQUARE :
				return iCurrValue + (iDistance * iDistance);
			case MIN :
				return Integer.MIN_VALUE;
			case MAX :
				return Integer.MAX_VALUE;
		    default: 
		    	return 1;
		}
	}

	public void calculate(Set<Tile> points, Offset offset, HeatMap.Func func) {
		clear();
		add(points, offset, func);
	}
	
	public void add(Set<Tile> points, Offset offset, HeatMap.Func func)
	{
		if(points == null) {
			return; // if there are no points, then return the blank map
		}

		// Loop through each item passed in
		for(Tile tile: points)
    	{
    		// Loop through the offset squares and add to the map
		    Iterator<Entry<Tile, Integer>> it = offset.getOffsetMap().entrySet().iterator();
		    while (it.hasNext()) 
		    {
		        Map.Entry<Tile, Integer> pairs = (Map.Entry<Tile, Integer>)it.next();
		        Tile tileOffset = map.getTile(tile, pairs.getKey());

		        // TODO: need to not add to invalid tiles
		        heat[tileOffset.getRow()][tileOffset.getCol()] = calculateFunc(func, pairs.getValue(), heat[tileOffset.getRow()][tileOffset.getCol()]);
		    }
		    
		    // Loop through the horizon squares to build the horizon
		    for(Tile tileHorizon : offset.getPerimeter()) {
		    	horizon.add(map.getTile(tile, tileHorizon));
		    }
    	}
	}
	
    public void print() 
    {
    	String strMap = "", strRow = "", strCell = "";
		for(int r = 0; r < this.rows; r++){
			strRow = "[" + String.format("%03d", r) + "]";
			for(int c = 0; c < this.cols; c++)
			{
				strCell = "";
				if(heat[r][c] > 9999999) {
					strCell += "MAX";
				} 
				else if(heat[r][c] < -9999999) {
					strCell += "MIN";
				}
				else {
					if(this.importantTiles.contains(new Tile(r, c))) {
						strCell += "B";
					} else if(map.getIlk(r, c) == Ilk.ENEMY_ANT) {
						strCell += "E";
					} else if(map.getIlk(r, c) == Ilk.MY_ANT) {
						strCell += "A";
					} 

					if(heat[r][c] != Integer.MIN_VALUE) {
						strCell += String.format("%03d",heat[r][c]);
						// strCell += heat[r][c]; // Integer.toString(heat[r][c]);  
					}
				}
				
				strRow += leftPad(strCell, 4);
    		}
    		map.log(strRow);
    	}
    }	
	
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
    	return "";
    }

    public String leftPad(String str, int size) {
        return leftPad(str, size, ' ');
    }
    
    public String leftPad(String str, int size, char padChar) {
        if (str == null) {
            return null;
        }
        int pads = size - str.length();
        if (pads <= 0) {
            return str; // returns original String when possible
        }

        return padding(pads, padChar).concat(str);
    }
    
    private String padding(int repeat, char padChar) throws IndexOutOfBoundsException {
        if (repeat < 0) {
            throw new IndexOutOfBoundsException("Cannot pad a negative amount: " + repeat);
        }
        final char[] buf = new char[repeat];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = padChar;
        }
        return new String(buf);
    }
    
}
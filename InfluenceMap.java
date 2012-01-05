import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class InfluenceMap 
{
	private boolean initialized;
	private boolean bAdditive;
	private boolean bBlended;
	private AntMap antmap;
	private float iMap[][];
	// private float iMapMin[][];
	private float iMapMax[][];
	private float iMapPrev[][];
    private float fBaseInfluence;
    private float fMomentum; // How long does the information stick around
    private float fDecay; // How quickly does the value decay
    private float fThreshold;
    private float iMax;
    private float iMin;
    private float influenceMultiplier;
    
    private boolean bCappedDecreaseAmount;
	private float fMaxDecreasePct;
    
    private InflueceType iType;
    
	public static enum InflueceType {
		 ATTRACTOR, REPELLER;
	}    
    
    public InfluenceMap(AntMap antmap, float fBaseInfluence, float fDecay, float fMomentum, boolean bAdditive) {
    	this.bCappedDecreaseAmount = true;
    	this.fMaxDecreasePct = 1.0f;
    	this.initialized = true;
        this.bAdditive = bAdditive;
        this.fMomentum = fMomentum;
        this.fDecay = fDecay;
        this.antmap = antmap;
        this.iMap = new float[antmap.getRows()][antmap.getCols()];
        this.iMapPrev = new float[antmap.getRows()][antmap.getCols()];
        this.iMapMax = new float[antmap.getRows()][antmap.getCols()];
        this.iType = fBaseInfluence > 0 ? InflueceType.REPELLER : InflueceType.ATTRACTOR;
        this.influenceMultiplier = (iType == InflueceType.REPELLER) ? 1 : -1;
        this.fBaseInfluence = Math.abs(fBaseInfluence);
        this.fThreshold = this.fBaseInfluence / 10;
        this.iMax = Float.MIN_VALUE;
        this.iMin = Float.MAX_VALUE;
    	this.clearMap();
    }

    // for adding, this is used to return
    public InfluenceMap(float iMap[][], AntMap antmap, float min, float max) {
    	this.initialized = false;
        this.antmap = antmap;
    	this.influenceMultiplier = 1;
        this.iMin = min;
        this.iMax = max;
    	this.iMap = iMap;
    }

    // for combined map
    public InfluenceMap(AntMap antmap) {
    	this.initialized = false;
        this.antmap = antmap;
    	this.influenceMultiplier = 1;
        this.iMax = Float.MIN_VALUE;
        this.iMin = Float.MAX_VALUE;
        this.iMap = new float[antmap.getRows()][antmap.getCols()];
        this.clearMap();
    }

    public void setMaxDecreasePct(float fNew) {
    	fMaxDecreasePct = fNew;
    }
    
    public void propagate(Set<Tile> goalAgents) {
    	propagate(goalAgents, true);
    }

    public void propagate(Set<Tile> goalAgents, boolean bStopAtInvisible) {
    	propagate(goalAgents, bStopAtInvisible, new HashSet<Tile>());
    }

    public void propagate(Set<Tile> goalAgents, Set<Tile> shortCircuits) {
    	propagate(goalAgents, true, shortCircuits);
    }
    
    public void propagate(Set<Tile> goalAgents, boolean bStopAtInvisible, Set<Tile> shortCircuits)
    {
    	this.copyToPrevMap();
    	this.clearMap();

    	if(bAdditive) {
            for (Tile tile : goalAgents) {
            	propagateAgent(tile, bStopAtInvisible, shortCircuits);
            }
    	}
    	else {
    		propagateNonAdditive(goalAgents, bStopAtInvisible);
    	}
    		
        this.blendMaps();
    }

    private void propagateAgent(Tile t, boolean bStopAtInvisible, Set<Tile> shortCircuits)
    {
    	
    	Set<Tile> propagated = new HashSet<Tile>(); // already propagated so we don't double up
    	List<Pair<Tile, Integer>> topropagate = new LinkedList<Pair<Tile, Integer>>(); // running list so that we propagate in order of distance
    	topropagate.add(new Pair<Tile, Integer>(t, 0));
    	boolean bHitShortCircuit = false;

    	while(!topropagate.isEmpty()) {
    		Pair<Tile, Integer> pair = topropagate.remove(0);
    		Tile tile = pair.first;

			if(propagated.contains(tile)) {
				continue;
			}
    		
    		// Compute and add this item
    		float fInfluence = addInfluence(tile, pair.second);
    		propagated.add(tile);

    		// Exit out after adding the first short-circuit tile
    		if(shortCircuits.contains(tile)) {
				return;
			}
    		
    		// Add neighbors if the influence at this tile is greater than a certain threshold
    		if(fInfluence > fThreshold && !bHitShortCircuit)
    		{
	    		// Add neighbors that haven't already been added
	    		Set<Tile> neighbors = tile.getMovableNeighbors(antmap);

	    		for(Tile neighbor: neighbors) {
	    			// If one of the previously propagated tiles is a neighbor of a short-circuit -> exit
	    			if(shortCircuits.contains(neighbor)) {
	        			return;
	    			}

	    			if(!propagated.contains(neighbor) && (!bStopAtInvisible || antmap.isVisible(neighbor))) {
	    				topropagate.add(new Pair<Tile, Integer>(neighbor, pair.second + 1));
	    			}
	    		}
    		}
    	}
    }


    private float getBaseInfluenceWithAge(Tile tile) {
//    	if(iMapPrev[tile.getRow()][tile.getCol()] == Float.MIN_VALUE) {
//    		return fBaseInfluence;
//    	}
//    	else if(iMapPrev[tile.getRow()][tile.getCol()] > (fDecay * fDecay * fDecay * fBaseInfluence)) {
//    		return fBaseInfluence;
//    	}
//    	else {
//    		// antmap.log("Tile: " + tile + " : " + iMapPrev[tile.getRow()][tile.getCol()]);
//    		return 0;
//    		// return iMapPrev[tile.getRow()][tile.getCol()];
//    	}    	

    	    	int numTurns = antmap.getNumberTurnsSinceLastSeen(tile);
    	    	int numTimes = antmap.getNumberTimesSeen(tile);
    	    	if(numTurns == Integer.MAX_VALUE || numTimes < 30000) {
    	    		return fBaseInfluence;
    	    	}
    	    	else if(numTurns < 3) {
    	    		return .01f;
    	    	}
    	    	else if(numTurns < 13) {
    	    		return fBaseInfluence * (numTurns - 3) / 10;
    	    	}
    	    	else {
    	    		return fBaseInfluence;
    	    	}
    }
    
    private void propagateNonAdditive(Set<Tile> goalAgents, boolean bStopAtInvisible)
    { 
    	TreeMultiMap<Float, Tile> currentNodes = new TreeMultiMap<Float, Tile>(true);
    	Set<Tile> added = new HashSet<Tile>();
    	Set<Tile> ignoredHorizon = new HashSet<Tile>();

    	for(Tile agent : goalAgents) {
    		antmap.loginfo("getBaseInfluenceWithAge: " + agent);
    		float influence = getBaseInfluenceWithAge(agent);
    		if(influence > 0) {
    			currentNodes.put(influence, agent);
    			// currentNodes.put(fBaseInfluence, agent);
    			added.add(agent);
    		}
    		else {
    			ignoredHorizon.add(agent);
    		}
    	}
    	
    	boolean bIterating = true;
    	float fCurrent;
       	while(!currentNodes.isEmpty())
       	{
       		bIterating = false;
       		fCurrent = currentNodes.firstKey();
       		// antmap.log("Checking float: " + fCurrent);
       		
	    	for(Tile tileNode : currentNodes.get(fCurrent))
	       	{
	    		float newInfluence = addInfluence(tileNode, currentNodes.firstKey());

	    		Set<Tile> neighbors = tileNode.getMovableNeighbors(antmap);
	    		for(Tile neighbor: neighbors) {
		    		// if(!added.contains(neighbor) && (!bStopAtInvisible || antmap.isDiscovered(neighbor) || ignoredHorizon.contains(neighbor))) {
	    			if(!added.contains(neighbor) && (!bStopAtInvisible || antmap.isVisible(neighbor) || ignoredHorizon.contains(neighbor))) {
	    	    		currentNodes.put(newInfluence, neighbor);
	    	    		added.add(neighbor);
	    	    		bIterating = true;
	    			}
	    		}
	       	}

    		currentNodes.removeKey(fCurrent);

       		
//	    	for(TreeMultiMap.Entry<Float, Tile> node : currentNodes.entryList())
//	       	{
//	    		float newInfluence = addInfluence(node.getValue(), node.getKey());
//	    		currentNodes.removeValue(node.getValue());
//
//	    		Set<Tile> neighbors = node.getValue().getMovableNeighbors(antmap);
//	    		for(Tile neighbor: neighbors) {
//		    		// if(!added.contains(neighbor) && (!bStopAtInvisible || antmap.isDiscovered(neighbor) || ignoredHorizon.contains(neighbor))) {
//	    			if(!added.contains(neighbor) && (!bStopAtInvisible || antmap.isVisible(neighbor) || ignoredHorizon.contains(neighbor))) {
//	    	    		currentNodes.put(newInfluence, neighbor);
//	    	    		added.add(neighbor);
//	    	    		bIterating = true;
//	    			}
//	    		}
//	       	}
       	}
    }
    
    public float computeInfluenceFromPrevInfluence(float fInfluence) {
    	return fInfluence * fDecay;
    }

    public float computeInfluenceFromDistance(int distance) {
    	return new Float(fBaseInfluence * Math.pow(fDecay, distance));
    }
    
    public float addInfluence(Tile t, float fPrevWeight) {
    	return addInfluence2(t, computeInfluenceFromPrevInfluence(fPrevWeight));
    }
    
    public float addInfluence(Tile t, int distance) {
    	return addInfluence2(t, computeInfluenceFromDistance(distance));
    }
    
    public float addInfluence2(Tile t, float newInfluence) {
    	if(bAdditive) {
        	iMap[t.getRow()][t.getCol()] += newInfluence;
    	}
    	else {
    		iMap[t.getRow()][t.getCol()] = Math.max(iMap[t.getRow()][t.getCol()], newInfluence);
    	}
    	
    	iMapMax[t.getRow()][t.getCol()] = Math.max(iMapMax[t.getRow()][t.getCol()], iMap[t.getRow()][t.getCol()]);
    	
    	iMax = Math.max(iMax, iMap[t.getRow()][t.getCol()]);
    	iMin = Math.min(iMin, iMap[t.getRow()][t.getCol()]);
    	return newInfluence;
    }

    public void addMap(InfluenceMap iMapAdd) {
    	int ROWS = antmap.getRows();
    	int COLS = antmap.getCols();
    	float val1, val2;
    	
    	for (int row = 0; row < ROWS; ++row) {
    		for (int col = 0; col < COLS; ++col) {
    			val1 = iMapAdd.getInfluence(row, col);
    			val2 = getInfluence(row, col);
    			if(val1 == Float.MIN_VALUE && val2 == Float.MIN_VALUE) {
    				iMap[row][col] = Float.MIN_VALUE; 
    			}
    			else {
        			if(val1 == Float.MIN_VALUE) {
        				iMap[row][col] = val2; 
        			}
        			else if(val2 == Float.MIN_VALUE) {
        				iMap[row][col] = val1; 
        			}
        			else {
        				iMap[row][col] = val1 + val2; 
        			}
        			iMin = Math.min(iMin, iMap[row][col]);
        			iMax = Math.max(iMax, iMap[row][col]);
    			}
    			
    	    	// antmap.log("Row " + row + " | Col " + col + " " + iMapReturn[row][col]);
    			// antmap.log("iMinVal: " + iMinVal);
    			// antmap.log("iMaxVal: " + iMaxVal);
    		}
    	}
    }

    public boolean hasInfluence(Tile t) {
    	return iMap[t.getRow()][t.getCol()] > Float.MIN_VALUE;
    }
    
    public float getInfluence(Tile t) {
    	return getInfluence(t.getRow(), t.getCol());
    }
    
    public float getInfluence(int row, int col) {
    	if(bBlended) {
    		return getInfluenceBlended(row, col);
    	}
    	else {
    		return getInfluenceUnBlended(row, col);
    	}
    }

    private float getInfluenceBlended(int row, int col) {
    	return (iMap[row][col] == Float.MIN_VALUE) ? Float.MIN_VALUE : influenceMultiplier * (iMap[row][col] + iMapMax[row][col]) / 2 ;
    }
    
    private float getInfluenceUnBlended(int row, int col) {
    	return (iMap[row][col] == Float.MIN_VALUE) ? Float.MIN_VALUE : influenceMultiplier * iMap[row][col];
    }

    public TreeMultiMap<Float, Tile> getMoves(Tile tile) {
		float fTemp;
	    TreeMultiMap<Float, Tile> returnMoves = new TreeMultiMap<Float, Tile>();
		
    	Set<Tile> neighbors = tile.getMovableNeighbors(antmap);
    	
		for(Tile neighbor: neighbors) {
			fTemp = getInfluence(neighbor.getRow(), neighbor.getCol());
			returnMoves.put(fTemp, neighbor);
		}

		return returnMoves;
    }
    
    public float getMax() {
    	return iMax * influenceMultiplier;
    }
    
    public float getMin() {
    	return iMin * influenceMultiplier;
    }
    
    public void copyToPrevMap() {
    	int ROWS = antmap.getRows();
    	int COLS = antmap.getCols();
        for (int row = 0; row < ROWS; ++row) {
            for (int col = 0; col < COLS; ++col) {
            	iMapPrev[row][col] = iMap[row][col];
            }
        }
    }

    public void clearPrevMap() {
    	int ROWS = antmap.getRows();
    	int COLS = antmap.getCols();
        for (int row = 0; row < ROWS; ++row) {
            for (int col = 0; col < COLS; ++col) {
            	iMapPrev[row][col] = Float.MIN_VALUE;
            }
        }
    }

    public void clearMap() {
    	int ROWS = antmap.getRows();
    	int COLS = antmap.getCols();
        for (int row = 0; row < ROWS; ++row) {
            for (int col = 0; col < COLS; ++col) {
            	iMap[row][col] = Float.MIN_VALUE;
            }
        }
    }

    public void blendMaps() {
    	int ROWS = antmap.getRows();
    	int COLS = antmap.getCols();
    	float fTemp, fMinAmt;
        for (int row = 0; row < ROWS; ++row) {
            for (int col = 0; col < COLS; ++col) {
            	if(iMap[row][col] != Float.MIN_VALUE && iMapPrev[row][col] != Float.MIN_VALUE) {
            		fTemp = (fMomentum * iMap[row][col]) + ((1-fMomentum) * iMapPrev[row][col]);
            		fMinAmt = iMapPrev[row][col] * (1 - fMaxDecreasePct);
            		iMap[row][col] = (fTemp < fMinAmt) ? fMinAmt : fTemp; 
            	}
        	}
        }
	}

    public void print() 
    {
    	int ROWS = antmap.getRows();
    	int COLS = antmap.getCols();

    	antmap.log("Printing Influence Map");
    	antmap.log("Min: " + getMin());
    	antmap.log("Max: " + getMax());
    	
    	String strRow = "", strCell = "";
		for(int r = 0; r < ROWS; r++){
			strRow = "[" + String.format("%03d", r) + "]";
			for(int c = 0; c < COLS; c++)
			{
				if(getInfluence(r, c) != Float.MIN_VALUE) {
					strCell = String.format("%03d", (int)Math.floor(getInfluence(r, c)));
				}
				else {
					strCell = "   ";
				}
				
				strRow += strCell;
    		}
    		antmap.log(strRow);
    	}
    }	
}

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Holds all game data and current game state.
 */
public class Game {
    /** Maximum map size. */
    public static final int MAX_MAP_SIZE = 256 * 2;

    private final int loadTime;
    private final int turnTime;
    private final int rows;
    private final int cols;
    private final int turns;
    private final int viewRadius2;
    private final int attackRadius2;
    private final int spawnRadius2;


    private long turnStartTime;
    private int currentTurn;

    private final Set<Order> orders = new HashSet<Order>();

    // Items used in logging
    private FileWriter fstream;
    private BufferedWriter out;
    private int iCrashTurn;
    

    private FileWriter fstream_debugjs;
    private BufferedWriter out_debugjs;
    
    /**
     * Creates new {@link Game} object.
     * 
     * @param loadTime timeout for initializing and setting up the bot on turn 0
     * @param turnTime timeout for a single game turn, starting with turn 1
     * @param rows game map height
     * @param cols game map width
     * @param turns maximum number of turns the game will be played
     * @param viewRadius2 squared view radius of each ant
     * @param attackRadius2 squared attack radius of each ant
     * @param spawnRadius2 squared spawn radius of each ant
     */
    public Game(int loadTime, int turnTime, int rows, int cols, int turns, int viewRadius2, int attackRadius2, int spawnRadius2) {
    	this.loadTime = loadTime;
        this.turnTime = turnTime;
        this.rows = rows;
        this.cols = cols;
        this.turns = turns;
        this.viewRadius2 = viewRadius2;
        this.attackRadius2 = attackRadius2;
        this.spawnRadius2 = spawnRadius2;

        // Set up logging
        try {
        	
        	File file=new File("out.txt");
        	if(file.exists()) {
        		file.renameTo(new File("out.txt.bak"));
        	}
        	
			fstream = new FileWriter("out.txt");
		    out = new BufferedWriter(fstream);
		    

		    fstream_debugjs = new FileWriter("debug.js");
		    out_debugjs = new BufferedWriter(fstream_debugjs);
		    
		    this.debugjs("var debug_orders = [];");
		    
		    
		    
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    

    /**
     * Returns timeout for initializing and setting up the bot on turn 0.
     * 
     * @return timeout for initializing and setting up the bot on turn 0
     */
    public int getLoadTime() {
        return loadTime;
    }

    /**
     * Returns timeout for a single game turn, starting with turn 1.
     * 
     * @return timeout for a single game turn, starting with turn 1
     */
    public int getTurnTime() {
        return turnTime;
    }

    /**
     * Returns game map height.
     * 
     * @return game map height
     */
    public int getRows() {
        return rows;
    }

    /**
     * Returns game map width.
     * 
     * @return game map width
     */
    public int getCols() {
        return cols;
    }

    /**
     * Returns maximum number of turns the game will be played.
     * 
     * @return maximum number of turns the game will be played
     */
    public int getTurns() {
        return turns;
    }

    /**
     * Returns squared view radius of each ant.
     * 
     * @return squared view radius of each ant
     */
    public int getViewRadius2() {
        return viewRadius2;
    }

    /**
     * Returns squared attack radius of each ant.
     * 
     * @return squared attack radius of each ant
     */
    public int getAttackRadius2() {
        return attackRadius2;
    }

    public int getSpawnRadius2() {
        return spawnRadius2;
    }

    public void setTurnStartTime(long turnStartTime) {
        this.turnStartTime = turnStartTime;
    }

    public int getTimeRemaining() {
        return turnTime - (int)(System.currentTimeMillis() - turnStartTime);
    }

    public Set<Order> getOrders() {
        return orders;
    }


	public void incrementTurn() {
        currentTurn++;
    }

    public int getCurrentTurn() {
        return currentTurn;
    }	
		
    public void log(String log){
		if(MyBot.DEBUG){
			try {
	
				out.write("[" + getTimeRemaining() + "] " + log + "\r\n");
				out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
    }
    
    public void debugjs(String js){
		if(MyBot.DEBUG){
    	try {

    	    out_debugjs.write(js + "\r\n");
    	    out_debugjs.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	}

    public int getCrashTurn() {
    	return iCrashTurn;
    }    
    
    public void logcrash(String str){
    	if(getCurrentTurn() == this.iCrashTurn)
    		log(str);
    }
    
    /* Issues an order by sending it to the system output.
     * 
     * @param myAnt map tile with my ant
     * @param direction direction in which to move my ant
     */
    public void issueOrder(Tile myAnt, Aim direction) {
        Order order = new Order(myAnt, direction);
        orders.add(order);
        System.out.println(order);
    }
}
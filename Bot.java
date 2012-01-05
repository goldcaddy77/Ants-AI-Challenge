/**
 * Provides basic game state handling.
 */
public abstract class Bot extends AbstractSystemInputParser {
    protected Game game;
    protected AntMap map;
    protected int ROWS;
    protected int COLS;
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setup(int loadTime, int turnTime, int rows, int cols, int turns, int viewRadius2, int attackRadius2, int spawnRadius2) 
    {
    	Game g = new Game(loadTime, turnTime, rows, cols, turns, viewRadius2, attackRadius2, spawnRadius2);
    	setGame(g);
        ROWS = game.getRows();
		COLS = game.getCols();
    	
    	setMap(new AntMap(g));
    }
    
    /**
     * Returns game state information.
     * 
     * @return game state information
     */
    public Game getGame() {
        return game;
    }
    
    /**
     * Sets game state information.
     * 
     * @param game game state information to be set
     */
    protected void setGame(Game game) {
        this.game = game;
    }
    
    public AntMap getMap() {
        return map;
    }
    
    protected void setMap(AntMap map) {
        this.map = map;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeUpdate() {
    	map.log("Bot.beforeUpdate");
    	map.log("Setting start time");
    	game.setTurnStartTime(System.currentTimeMillis());

    	map.log("Clearing orders");
    	game.getOrders().clear();

    	map.log("Incrementing turn");
    	game.incrementTurn();

    	map.log("map.beforeUpdate (called from Bot)");
        map.beforeUpdate();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void addWater(int row, int col) {
        map.processWater(row, col);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void addAnt(int row, int col, int owner) {
    	map.processLiveAnt(row, col, owner);
    	// map.update(owner > 0 ? Ilk.ENEMY_ANT : Ilk.MY_ANT, new Tile(row, col));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void addFood(int row, int col) {
    	map.processFood(row, col);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAnt(int row, int col, int owner) {
    	map.processDeadAnt(row, col, owner);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void addHill(int row, int col, int owner) {
    	map.processHill(row, col, owner);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void afterUpdate() {
    	map.afterUpdate();
    }
}

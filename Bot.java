/**
 * Provides basic game state handling.
 */
public abstract class Bot extends AbstractSystemInputParser {
    private Game game;
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setup(int loadTime, int turnTime, int rows, int cols, int turns, int viewRadius2,
            int attackRadius2, int spawnRadius2) {
        setGame(new Game(loadTime, turnTime, rows, cols, turns, viewRadius2, attackRadius2, spawnRadius2));
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
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeUpdate() {
        game.setTurnStartTime(System.currentTimeMillis());
        game.clearMyAnts();
        game.clearEnemyAnts();
        game.clearMyHills();
        game.clearEnemyHills();
        game.clearFood();
        game.clearDeadAnts();
        game.getOrders().clear();
        game.clearVision();
        game.incrementTurn();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void addWater(int row, int col) {
        game.update(Ilk.WATER, new Tile(row, col));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void addAnt(int row, int col, int owner) {
        game.update(owner > 0 ? Ilk.ENEMY_ANT : Ilk.MY_ANT, new Tile(row, col));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void addFood(int row, int col) {
        game.update(Ilk.FOOD, new Tile(row, col));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAnt(int row, int col, int owner) {
        game.update(Ilk.DEAD, new Tile(row, col));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void addHill(int row, int col, int owner) {
        game.updateHills(owner, new Tile(row, col));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void afterUpdate() {
        game.setVision();
    }
}

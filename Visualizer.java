import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class Visualizer 
{
	private AntMap map;
	private Game game;

	public Visualizer(AntMap map, Game game) {
		this.map = map;
		this.game = game;
	}

	public void drawObstaclePaths()
	{
		if(MyBot.DEBUG)
		{
			for(Cluster c: map.getAllObstacles()) 
			{
				for (Map.Entry<Tile, Integer> mapTileToWalkNumber : c.getMapTileToWalkNumber().entrySet())
				{
					float color = (float)mapTileToWalkNumber.getValue() / (float)c.getMapTileToWalkNumber().size() * (float)256.0;
					viz_setFillColor((int)color, (int)color, (int)color, 70.0);
					viz_tile(mapTileToWalkNumber.getKey());
				}

			}	
		}
	}

	public void drawMap(boolean[][] bMap)
	{
		if(MyBot.DEBUG)
		{
			drawMap(bMap, true, new Color());
		}	
	}


	public void drawMap(boolean[][] bMap, boolean bTrigger, Color color)
	{
		if(MyBot.DEBUG)
		{
			viz_setFillColor(color.red(), color.green(), color.blue(), .500);

			int totRows = bMap.length;
			int totCols = bMap[0].length;
			for (int row = 0; row < totRows; ++row) {
				for (int col = 0; col < totCols; ++col) {
					if(bMap[row][col] == bTrigger) {
						viz_tile(new Tile(row, col));
					}
				}
			}
		}
	}

	

	public void drawInfluenceMap(InfluenceMap iMap, Color color)
	{
		if(MyBot.DEBUG)
		{
			int totRows = map.getRows();
			int totCols = map.getCols();
			float iMax = iMap.getMax();
			float iMin = iMap.getMin();
			// map.log("Min: " + String.format("%03d", (int)iMin));
			// map.log("Max: " + String.format("%03d", (int)iMax));
			for (int row = 0; row < totRows; ++row) {
				for (int col = 0; col < totCols; ++col) {
					float influence = iMap.getInfluence(row, col);

					// map.log("Influence: " + String.format("%03d", (int)influence));

					if(influence != Float.MIN_VALUE) {
						// Color colorNew = color.withPercentage(influence / 15);
						// viz_setFillColor(colorNew.red(), colorNew.green(), colorNew.blue(), 50.0);

						// map.log(String.format("%03d", (int)((influence-iMin) / (iMax-iMin) * 100)));

						Color c = getRGBColor((influence-iMin) / (iMax-iMin));

						viz_setFillColor(c.red(), c.green(), c.blue(), .5);
						viz_tile(new Tile(row, col));
					}
				}
			}
		}
	}

	/*
		Set<Tile> tilesAttackPerimeter = map.getOffAttack().getPerimeter();
    	Set<Tile> tilesAttackHorizon = map.getOffAttack().getHorizon();
		Set<Tile> tilesNextAttackPerimeter = map.getOffNextAttack().getPerimeter();
    	Set<Tile> tilesNextAttackHorizon = map.getOffNextAttack().getHorizon();

    	for(Tile tileEnemy: map.getAllEnemyAntTiles()) {

			Set<Tile> tilesPerim = new HashSet<Tile>();
	    	for(Tile t : tilesAttackPerimeter) {
	    		tilesPerim.add(map.getTile(t, tileEnemy));
			}
			visualizer.drawTileSet(tilesPerim, new Color(255, 255, 255));
    	}
	 * */
	

	public Color getRGBColor(float f) 
	{
		// map.log("getRGBColor: " + f);

		List<Color> colors = new LinkedList<Color>();
		colors.add(new Color(0, 0, 0));
		colors.add(new Color(63, 60, 173));
		colors.add(new Color(75, 133, 243));
		colors.add(new Color(86, 211, 85));
		colors.add(new Color(255, 251, 61));
		colors.add(new Color(255, 159, 72));
		colors.add(new Color(255, 0, 0));
		colors.add(new Color(255, 255, 255));

		float fColor = f * (colors.size() - 1);
		int index = (int)Math.floor(fColor);

		if(fColor == index) {
			return colors.get(index);
		}
		else {
			float pct2 = fColor - index;
			float pct1 = 1 - pct2;
			int red1 = (int)(colors.get(index).red() * pct1);
			int green1 = (int)(colors.get(index).green() * pct1);
			int blue1 = (int)(colors.get(index).blue() * pct1);
			int red2 = (int)(colors.get(index + 1).red() * pct2);
			int green2 = (int)(colors.get(index + 1).green() * pct2);
			int blue2 = (int)(colors.get(index + 1).blue() * pct2);
			return new Color(red1 + red2, green1 + green2, blue1 + blue2);
		}
	}

	public void drawBattles(ArrayList<BattleResolutionParticipant> participants) {
		
		// using traditional for loop
		for(int i=0; i<participants.size(); i++)
		{
			BattleResolutionParticipant brp = participants.get(i);
			Set<BattleResolutionParticipant> opponents = brp.getPotentialOpponents();
			
			for(BattleResolutionParticipant opponent: opponents) {
				viz_line(brp.getStartLocation() , opponent.getCurrentLocation());
			}
		}
	}

	public void drawTileSet(Set<Tile> tiles) {
		drawTileSet(tiles, new Color(255, 255, 255));
	}
	

	public void drawTileSet(Set<Tile> tiles, Color color) {
		viz_setFillColor(color.red(), color.green(), color.blue(), .4);
		for(Tile tile : tiles) {
			viz_tile(tile);
		}
	}
	
	public void drawAntPaths()
	{
		if(MyBot.DEBUG)
		{
			// TEST: DRAW ALL LINES //
			for(Ant ant:map.getAllFriendlyAnts()){
				if(ant.getPath() != null){
					Tile prev = ant.getCurrentTile();
					for(Tile t:ant.getPath().getTiles()){
						if(Math.abs(prev.getCol() - t.getCol())<2 && Math.abs(prev.getRow() - t.getRow())<2) {
							viz_line(prev, t);
						}
						prev = t;
					}
				}
			}
		}
	}

	public void drawAntNextMoveBoxes(InfluenceMap iMap)
	{
		if(MyBot.DEBUG)
		{
			for(Ant ant:map.getAllFriendlyAnts()) {
				TreeMultiMap<Float, Tile> moves = iMap.getMoves(ant.getCurrentTile());

				map.log("Moves for ant " + ant + " | "+ moves);

		       	for(TreeMultiMap.Entry<Float, Tile> move : moves.entryList())
		       	{
					// viz_rect(next, 1, 1, 1);
					viz_arrow(ant.getCurrentTile(), move.getValue());
					break;
		       	}
			}
		}
	}

	
	public void drawHeatMap(HeatMap heatmap)
	{
		if(MyBot.DEBUG)
		{
			// TEST: DRAW HEATMAP FLURRY //
			int max_heat = 0;
			int[][] hm = heatmap.getMap();
			for(int i = 0; i<game.getRows(); i++){
				for(int j = 0; j<game.getCols(); j++){
					if(hm[i][j] > max_heat && hm[i][j] != Integer.MAX_VALUE){

						max_heat = hm[i][j];
					}
				}
			}
			//		map.log("max heat: " + max_heat);
			//		map.log("" + ((double)12)/((double)max_heat));
			for(int i = 0; i<game.getRows(); i++){
				for(int j = 0; j<game.getCols(); j++){
					if(hm[i][j] > 0){
						viz_setFillColor(255, 0, 0, Math.min(.5,((double)hm[i][j])/((double)max_heat)));
						viz_tile(new Tile(i,j));
					}
				}
			}	
		}
	}

	public void drawAntsAndAllFoodTargets()
	{
		if(MyBot.DEBUG)
		{
			viz_setLineColor(255, 55, 0, 1.0);
			for(Ant ant: map.getAllFriendlyAnts()){
				Tile prevTile = ant.getCurrentTile();
				for(Tile foodTile: ant.getAllFoodTargets()){
					viz_line(prevTile, foodTile);
					prevTile = foodTile;
				}
			}		
		}
	}

	//  v setLineWidth width
	//  v setLineColor r g b a
	//  v setFillColor r g b a
	//  v arrow row1 col1 row2 col2
	//  v circle row col radius fill
	//  v line row1 col1 row2 col2
	//  v rect row col width height fill
	//  v star row col inner_radius outer_radius points fill
	//  v tile row col
	//  v tileBorder row col subtile
	//  v tileSubTile row col subtile
	private void viz_setLineWidth(int width){
		if(MyBot.DEBUG){
			System.out.println("v setLineWidth " + width);
		}
	}

	private void viz_setLineColor(Integer r, Integer g, Integer b, Double alpha){
		if(MyBot.DEBUG){
			System.out.println("v setLineColor " + r + " " + g + " " + b + " " + alpha);
		}
	}

	private void viz_setFillColor(Integer r, Integer g, Integer b, Double alpha){
		if(MyBot.DEBUG){
			System.out.println("v setFillColor " + r + " " + g + " " + b + " " + alpha);
		}
	}

	private void viz_arrow(Tile from, Tile to){
		if(MyBot.DEBUG){
			System.out.println("v arrow " + from.getRow() + " " + from.getCol() + " " + to.getRow() + " " + to.getCol());
		}

	}

	private void viz_circle(Tile tile, int radius, int fill){
		if(MyBot.DEBUG){
			System.out.println("v circle " + tile.getRow() + " " + tile.getCol() + " " + radius + " " + fill);
		}

	}

	private void viz_line(Tile from, Tile to){
		if(MyBot.DEBUG){
			System.out.println("v line " + from.getRow() + " " + from.getCol() + " " + to.getRow() + " " + to.getCol());
		}

	}

	private void viz_rect(Tile tile, int width, int height, int fill){
		if(MyBot.DEBUG){
			System.out.println("v rect " + tile.getRow() + " " + tile.getCol() + " " + width + " " + height + " " + fill);
		}

	}

	private void viz_star(Tile tile, int inner_radius, int outer_radius, int points, int fill){
		if(MyBot.DEBUG){
			System.out.println("v star " + tile.getRow() + " " + tile.getCol() + " " + inner_radius + " " + outer_radius + " " + points + " " + fill);
		}
	}

	private void viz_tile(Tile tile){
		if(MyBot.DEBUG){
			System.out.println("v tile " + tile.getRow() + " " + tile.getCol());
		}

	}

	private void viz_tileBorder(Tile tile, int subtile){
		if(MyBot.DEBUG){
			System.out.println("v tileBorder " + tile.getRow() + " " + tile.getCol() + " " + subtile);    	
		}
	}

	private void viz_tileSubTile(Tile tile, int subtile){
		if(MyBot.DEBUG){
			System.out.println("v tileSubTile " + tile.getRow() + " " + tile.getCol() + " " + subtile);    	
		}
	}	
}

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RandomGame {

	private static String BOT_DIR = "bots";
	private static String MAP_DIR = "tools/maps";
	private static String OUTPUT_DIR = "random_game_logs";
	private static String ANT_TOOLS = "/ant_tools/";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		int iStartGameNum = (int)(((double)Calendar.getInstance().getTimeInMillis() / 10000) - (double)132372000);
		
//		if(args.length > 0){
//			numTurns = Integer.parseInt(args[0]);			
//		}
//		
//		if(args.length > 1){
//			turnTime = Integer.parseInt(args[1]);			
//		}
	
		
		String strTempFile = new File(".").getAbsolutePath();
		File currentDirectory = new File(strTempFile).getParentFile();
		// System.out.println(currentDirectory);
		
		File parentDirectory = currentDirectory.getParentFile();
		// System.out.println(parentDirectory);
		
		
//		String currentDir = new File();
//		System.out.println("currentDir: " + currentDir);
//		System.out.println("currentDir Path: " + currentDir);

		
//		File parent = new File(currentDir.getParent());
//		File parent2 = new File(parent.getParent());
//				
//		System.out.println("parent: " + parent);
//		System.out.println("parent2: " + parent2);
//		System.out.println("parent: " + parent.getAbsolutePath());
		File botdir = new File(parentDirectory + ANT_TOOLS + BOT_DIR);
		
		long mostRecentModified = 0;
		String mostRecentExec = "";
		
		List<String> potentialBots = new ArrayList<String>();
		
		for(File b: botdir.listFiles()){
			if(b.isDirectory()){
				// System.out.println(b);

				String exec = getExecFromDir(b);				

				// System.out.println(exec);
				
				if(b.lastModified() > mostRecentModified){
					mostRecentModified = b.lastModified();
					mostRecentExec = exec;
				}
				
				if(exec.length()>0){
					potentialBots.add(exec);
				}
				
			}
		}
		
		List<String> potentialMaps = new ArrayList<String>();
		
		File mapdir = new File(parentDirectory  + ANT_TOOLS + MAP_DIR);
		
		for(File m: mapdir.listFiles()){
			if(m.isDirectory() && !m.getName().equals("example")){
				String mapName = m.getName() + "/";
				
				for(String mm: m.list()){
					potentialMaps.add(mapName + mm);
					//System.out.println(mapName + mm);
				}
			}
		}
		
		for(int i=0; i<10; i++) {
			String s = getRandomGameString(potentialMaps, potentialBots, ++iStartGameNum);
			System.out.println(s);
		}
		
//			
//		 try {
//             Runtime rt = Runtime.getRuntime();
//             Process pr = rt.exec("cmd /c dir");
//             pr = rt.exec(runString);
//
//             BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
//
//             String line=null;
//
//             while((line=input.readLine()) != null) {
//                 System.out.println(line);
//             }
//
//             int exitVal = pr.waitFor();
//             System.out.println("Exited with error code "+exitVal);
//
//         } catch(Exception e) {
//             System.out.println(e.toString());
//             e.printStackTrace();
//         }
		
	}

	public static String getRandomGameString(List<String> potentialMaps, List<String> potentialBots, int iGameNum)
	{
		List<String> execBots = new ArrayList<String>();
		execBots.add("java -jar ../ants/MyBot.jar debug");
		execBots.add("java -jar bots/09_BR_1000_800_w_Dist/09_BR_1000_800_w_Dist.jar");
		
		// execBots.add("java -jar bots/10_BR_Fixed_Day_2/10_BR_Fixed_Day_2.jar");

		int numTurns = 500;
		int turnTime = 500;
	
		String randomMap = potentialMaps.get((int)Math.floor(Math.random()*potentialMaps.size()));
		// System.out.println("randomMap: " + randomMap);

		// System.out.println("potentialMaps.size(): " + potentialMaps.size());

		
		Pattern p = Pattern.compile("\\d\\d");
		Matcher m = p.matcher(randomMap);

		int numPlayers = 0;
		
		if(m.find()){
			numPlayers = Integer.parseInt(m.group().replace("p",""));
			
			// System.out.println("m.group() :" + m.group());
			// System.out.println("numPlayers :" + numPlayers);
		}

		//System.out.println(numPlayers);
		
		while(execBots.size() < numPlayers){
			execBots.add(potentialBots.get((int)Math.floor(Math.random()*potentialBots.size())));
			
		}		
		
		// System.out.println("execBots.size() :" + execBots.size() );
		
		String runString = "python tools/playgame.py -I ";
		
		while(execBots.size() > 0){
			int randomIndex = (int)Math.floor(Math.random()*execBots.size());
			
			runString = runString + " \"" + execBots.get(randomIndex) + "\"";
			execBots.remove(randomIndex);
			
		}
		
		runString = runString + " --map_file " + MAP_DIR +"/" + randomMap + " --log_dir game_logs --turns " + numTurns + " --engine_seed 42 --player_seed 42 --end_wait=0.25 --verbose -e";
		runString += " --turntime=" + turnTime;
		runString += " --game=" + iGameNum;
		
		return runString;
	}
	
	
	private static String getExecFromDir(File directory){
		
		String out = "";
		
		for(String f: directory.list()){
			if(f.startsWith(directory.getName())){
				String prefix = "";
				if(f.endsWith(".jar")){
					prefix = "java -jar ";
				} else {
					prefix = "node ";			
				}					
				out = prefix + "bots/" + directory.getName() + "/" + f;
			}
		}
		
		
		
		return out;
		
	}
	
}

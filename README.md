AI Challenge Java Bot
===================================

I participated in the Fall 2011 [AI Challenge](http://aichallenge.org/) and placed #143 out of 7897 - [link to my profile](http://aichallenge.org/profile.php?user=4165) - (although there were many "starter bots" that contained no original code).  I wrote my bot in a private repo and moved different versions of the bot into GitHub.

*1st bot - Food, Attacking and HeatMaps*

My first bot used Manhattan distance to figure out the closest ant to each food tile and issued A-Star searches to get the ant to the food.  When it saw an enemy base, it sent all ants that didn't have a food order at the base.  If there were no enemy bases, I used a heatmap to push my ants away from eachother.  This worked pretty well on wide open maps, but was horrible at mazes.

*2st bot - Added Predefined Paths, Deadend logic*

The second iteration chose a fixed number of pre-defined paths (N, S, E, W, NW, NE, SE, SW) for each of my ant hills and sent my ants on those paths.  This helped a lot with exploring mazes initially, but still resulted in poor exploration.  I also added logic that would "color in" any land tiles in between water tiles that created a deadend so that my ants wouldn't get stuck in them.  This worked really well in addition to the paths and heatmap logic to spread ants, but I ended up scrapping all of this code later.

*3rd bot - Added Simple Battle Resolution and Hill Defense*

Next, I added simple battle resolution that would determine battles (based on who could be attacked on the next turn) and if I outnumbered the enemy, I would attack, if we were even, I would stay and if they had more ants, I would retreat.  This worked ok, but made tons of bad decisions against good bots.  For hill defense, If an enemy was within 10 tiles of my base, any new ants would be sent in the direction of that enemy.

*4th bot - Final*

So I tried a bunch of things in between the 3rd and 4th bot I uploaded.  The first thing I did was basically throw out all of the things I did above and move to a hill-climbing strategy using a duffusion via a class I created called InfluenceMap.  The following items would generate a various amount of Influence, which were then added together and ultimately, the ant would just climb to the neighboring square with the most influence:

* `Food` - Food generated a strong pull, but "short circuited" once it hit the first friendly ant.  This way, multiple ants wouldn't head for the same food.  Also, 1 ant could get 3 pieces of food that were close to each other allowing another ant to explore elsewhere
* `Horizon tiles` - I defined a horizon tile as an invisible tile that bordered a visible tile.  Horizon tiles generated a pull, but did not propagate through water tiles, so an ant would constantly move towards unexplored areas.  This worked A LOT better than the heatmaps I used before
* `Enemy ants` - Enemy ants generated a pull that pretty large, but not so much that my ant wouldn't grab food if he was nearby.  This helped my build barriers in front of oncoming enemy ants so they couldn't get into my territory
* `Enemy hills` - Enemy hills generated a huge pull so that if my ant saw one, they would head towards it no matter what

So everything was done through influence maps except for my new battle resolution, which I ended up using Gibbs Sampling to achieve.  At the beginning of my turn, I would determine which of my ants were not in battle, generate influence maps and move those ants.  Then I would use the rest of the turn to sample various battle simulations and give out a score based on the moves that were chosen.  A dead enemy was worth 1000 points, a dead friendly ant worth -800 points (the difference was so that I wouldn't take 1 to 1 trades) and then I gave points for moves that resulted in the closest distance to my enemies.  Once the turn was about to end, I moved all of my ants to the positions that resulted in the highest score.

Post Mortem
---------------
I don't come from an AI background, so I ended up going down a lot of bad paths before finding what turned out being a very successful way to do things.  Here are some things I tried and never ended up using, but really got me thinking in new ways:

Don't have time to fill all of this in now, will come back:

* A-Star searching and PathStore
* HeatMaps
* Deadend tiles
* Clustering

Also, I did almost all of the work between the 3rd and 4th (final bot) in the last week or two before the competition.  The resulting bot was way better than the original bot, but I introduced an issue in the last few days that caused my bot to timeout in ~30% of my games.  Even with the timeouts, my bot was better than before, but I wish I had more time to find and fix the timeout issue - which ultimately had to do with automatic garbage collection happening close to the end of my Battle Resolution sampling, putting me over the time limit.
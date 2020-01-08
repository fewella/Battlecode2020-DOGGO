package FirstPlayer;

import battlecode.common.*;

public class HQ {

    static Direction[] directions = Direction.allDirections();
    static int dirBuild = 0;

    public static int miners = 0;
    public static final int MAX_MINERS = 5;

    public static void run(RobotController rc) throws GameActionException {

        // built-in NET GUN check for and shoot drones
        MapLocation myLocation = rc.getLocation();
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots();
        int nearestDist = 9999;
        int nearestID = -1;
        for (RobotInfo enemy : nearbyEnemies) {
            int currDist = myLocation.distanceSquaredTo(enemy.location);
            if (currDist < nearestDist) {
                nearestDist = currDist;
                nearestID = enemy.ID;
            }
        }
        if (nearestID != -1 && rc.canShootUnit(nearestID)) {
            rc.shootUnit(nearestID);
        }

        // Try every direction to build a miner
        for (int i=0; i<directions.length; i++) {
            if(rc.getTeamSoup() > GameConstants.INITIAL_SOUP + Common.MINER_COST && miners < MAX_MINERS) {
                if (Common.tryBuild(rc, RobotType.MINER, directions[dirBuild % directions.length])) {
                    miners++;
                }
            }
            dirBuild++;
        }


    }
}

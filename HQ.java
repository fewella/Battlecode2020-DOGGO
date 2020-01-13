package FirstPlayer;

import battlecode.common.*;

public class HQ {

    static Direction[] directions = Direction.allDirections();
    static int dirBuild = 0;

    static int miners = 0;

    static boolean buried = false;

    public static void run(RobotController rc) throws GameActionException {
        // built-in NET GUN check for and shoot drones
        MapLocation myLocation = rc.getLocation();
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(RobotType.HQ.sensorRadiusSquared, rc.getTeam().opponent());
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
        int MAX_MINERS = 5 + (rc.getRoundNum() / 200);
        for (int i = 0; i < directions.length; i++) {
            if(rc.getTeamSoup() > RobotType.MINER.cost && miners < MAX_MINERS) {
                if (Common.tryBuild(rc, RobotType.MINER, directions[dirBuild % directions.length])) {
                    miners++;
                }
            }
            dirBuild++;
        }

        // Check to see if there's dirt
        if (rc.getDirtCarrying() > 0 && !buried) {
            System.out.println("HELP ME");
            Common.broadcast(rc, Common.BroadcastType.HQBeingBuried, 1, 0);
            buried = true;
        }

        if (buried && rc.getDirtCarrying() == 0) {
            System.out.println("Saved");
            Common.broadcast(rc, Common.BroadcastType.HQBeingBuried, 0, 0);
            buried = false;
        }

    }
}

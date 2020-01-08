package FirstPlayer;

import battlecode.common.*;

public class Miner {

    static Direction searchDirection = null;

    public static void run(RobotController rc) throws GameActionException {

        // Search for soup!
        int radius = Common.getRealRadius(RobotType.MINER);
        MapLocation currLocation = rc.getLocation();
        MapLocation senseLocation = new MapLocation(currLocation.x - radius, currLocation.y - radius);

        boolean searchingEast = true;
        for (int i = 0; i < radius * 2; i++) {
            for (int j  = 0; j < radius * 2; j++) {
                int soupFound = 0;
                if (rc.canSenseLocation(senseLocation)) {
                    soupFound = rc.senseSoup(senseLocation);
                }
                if (soupFound > 0) {
                    System.out.println("FOUND SOUP - NOTIFY OTHER MINERS");
                }

                if (searchingEast) {
                    senseLocation = senseLocation.add(Direction.EAST);
                } else {
                    senseLocation = senseLocation.add(Direction.WEST);
                }
            }
            senseLocation = senseLocation.add(Direction.NORTH);
            searchingEast = !searchingEast;
        }
        System.out.println(("Bytecode used after soup search: " + Clock.getBytecodeNum()));

        // Logic for moving!
        // Determine which direction to move if I haven't yet
        if (searchDirection == null) {
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
            for (RobotInfo robot : nearbyRobots) {
                if (robot.getType() == RobotType.HQ && robot.getTeam() == rc.getTeam()) {
                    searchDirection = rc.getLocation().directionTo(robot.location);
                    break;
                }
            }
        }

        // Move in that direction
        if (rc.canMove(searchDirection)) {
            rc.move(searchDirection);
        }
    }

}

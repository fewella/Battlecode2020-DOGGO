package FirstPlayer;

import battlecode.common.*;

public class Miner {

    static Direction searchDirection = null;
    static MapLocation refineryLocation = null;

    public static void run(RobotController rc) throws GameActionException {

        //TODO: read block

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
                    searchDirection = rc.getLocation().directionTo(robot.location).opposite();
                    break;
                }
            }
        }
        //Build refinery if can and broadcast the location
        if(refineryLocation == null) {
            for (Direction dir : Direction.allDirections()) {
                if(Common.tryBuild(rc, RobotType.REFINERY, dir))
                {
                    refineryLocation = rc.getLocation().add(dir);
                    Common.broadcast(BroadcastType.MinerBuiltRefinery, refineryLocation.x, refineryLocation.y);
                }
            }
        }

        // Move in that direction
        for (int i=0; i < Direction.allDirections().length; i++) {
            if (rc.canMove(searchDirection) && !rc.senseFlooding(rc.getLocation().add(searchDirection))) {
                rc.move(searchDirection);
            } else {
                //if a wall is hit, try a different direction -> TODO: should also get it to back away from wall to improve search area
                searchDirection = searchDirection.rotateLeft();
            }
        }
    }

}

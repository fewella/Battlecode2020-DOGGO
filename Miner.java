package FirstPlayer;

import battlecode.common.*;

public class Miner {

    static Direction searchDirection = null;
    static MapLocation refineryLocation = null;
    static MapLocation soupLocation = null;

    public static void run(RobotController rc) throws GameActionException {

        // TODO: read block
        // Check PREVIOUS TURN chain, check for soup and refinery
        Transaction[] transactions = rc.getBlock(rc.getRoundNum() - 1);
        for (Transaction transaction : transactions) {
            int[] message = transaction.getMessage();
            if (message[6] == Common.SIGNATURE) {

                if (message[0] == Common.MINER_FOUND_SOUP_NUM) {
                    soupLocation = new MapLocation(message[1], message[2]);
                    System.out.println("SETTING SOUP LOCATION");

                } else if (message[0] == Common.MINER_FOUND_REFINERY_NUM) {
                    refineryLocation = new MapLocation(message[1], message[2]);
                    System.out.println("SETTING REFINERY LOCATION");

                }
            }
        }

        // Search for soup! But only if we haven't found soup
        if (soupLocation == null) {
            int radius = Common.getRealRadius(RobotType.MINER);
            MapLocation currLocation = rc.getLocation();
            MapLocation senseLocation = new MapLocation(currLocation.x - radius, currLocation.y - radius);

            boolean searchingEast = true;
            for (int i = 0; i < radius * 2; i++) {
                for (int j = 0; j < radius * 2; j++) {
                    int soupFound = 0;
                    if (rc.canSenseLocation(senseLocation)) {
                        soupFound = rc.senseSoup(senseLocation);
                    }
                    if (soupFound > 0) {
                        Common.broadcast(rc, Common.BroadcastType.MinerFoundSoup, senseLocation.x, senseLocation.y);
                        soupLocation = senseLocation;
                        break;
                    }

                    if (searchingEast) {
                        senseLocation = senseLocation.add(Direction.EAST);
                    } else {
                        senseLocation = senseLocation.add(Direction.WEST);
                    }
                }
                if (soupLocation != null) {
                    break;
                }
                senseLocation = senseLocation.add(Direction.NORTH);
                searchingEast = !searchingEast;
            }
        }

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

        // Build refinery if can and broadcast the location
        if(refineryLocation == null) {
            for (Direction dir : Direction.allDirections()) {
                if(Common.tryBuild(rc, RobotType.REFINERY, dir))
                {
                    refineryLocation = rc.getLocation().add(dir);
                    Common.broadcast(rc, Common.BroadcastType.MinerBuiltRefinery, refineryLocation.x, refineryLocation.y);
                }
            }
        }


        // Move in that direction (if haven't found soup)
        if (soupLocation == null) {
            // if areas of no pollution:
            Direction initialSearchDirection = searchDirection;
            for (int i = 0; i < Direction.allDirections().length; i++) {
                if (rc.canMove(searchDirection) && !rc.senseFlooding(rc.getLocation().add(searchDirection))
                        && rc.sensePollution(rc.getLocation().add(searchDirection)) == 0) {
                    rc.move(searchDirection);
                } else {
                    //if a wall is hit, try a different direction -> TODO: should also get it to back away from wall to improve search area
                    searchDirection = searchDirection.rotateLeft();
                }
            }
            // if there is pollution:
            searchDirection = initialSearchDirection;
            for (int i = 0; i < Direction.allDirections().length; i++) {
                if (rc.canMove(searchDirection) && !rc.senseFlooding(rc.getLocation().add(searchDirection))) {
                    rc.move(searchDirection);
                } else {
                    //if a wall is hit, try a different direction -> TODO: should also get it to back away from wall to improve search area
                    searchDirection = searchDirection.rotateLeft();
                }
            }
            //make sure next turn robot goes initial way it was supposed to search
            searchDirection = initialSearchDirection;

        } else {
            // IF WE FOUND THE SOUP, GO GIT IT

            // Algorithm:
            // 1. If I can mine soup, mine it.
            // 2. If can't check other locations and update soupLocation if necessary
            // 3. Lastly, move towards soupLocation (pathfinding algo)
            // 4. If no soup - set soupLocation to null (starts scan again)

            Direction toSoup = rc.getLocation().directionTo(soupLocation);
            if (rc.canMineSoup(toSoup)) {
                rc.mineSoup(toSoup);
            } else {
                for (Direction dir : Direction.allDirections()) {
                    if (rc.canMineSoup(dir)) {
                        rc.mineSoup(dir);
                    }
                }
            }

            if (rc.canMove(toSoup)) {
                rc.move(toSoup);
            }

        }

        System.out.println("TOTAL MINER BYTECODE USED: " + Clock.getBytecodeNum());

    }
}

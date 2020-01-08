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

        // Check if refinery exists nearby
        if (refineryLocation == null) {
            RobotInfo[] nearby = rc.senseNearbyRobots();
            for (RobotInfo curr : nearby) {
                if (curr.getType() == RobotType.REFINERY && curr.getTeam() == rc.getTeam()) {
                    refineryLocation = curr.location;
                }
            }
        }

        // Build refinery if can and broadcast the location
        if(refineryLocation == null && soupLocation != null) {
            for (Direction dir : Direction.allDirections()) {
                if(Common.tryBuild(rc, RobotType.REFINERY, dir))
                {
                    refineryLocation = rc.getLocation().add(dir);
                    Common.broadcast(rc, Common.BroadcastType.MinerBuiltRefinery, refineryLocation.x, refineryLocation.y);
                }
            }
        }

        // Move in that direction
        int soupCarrying = rc.getSoupCarrying();
        boolean haveSpace = soupCarrying < RobotType.MINER.soupLimit;

        if (soupLocation == null && refineryLocation == null) {
            //if areas of no pollution
            moveInDirection(rc, searchDirection);

        } else if (soupLocation != null && haveSpace) {
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

        } else if (!haveSpace && refineryLocation != null) {

            // Algorithm:
            // 1. If I can deposit in direction, do that
            // 2. If fails, check remaining directions to be safe
            // 3. Else, move towards refinery
            // TODO: Use nearer refinery if I know exists

            Direction toRefinery = rc.getLocation().directionTo(refineryLocation);
            if (rc.canDepositSoup(toRefinery)) {
                rc.depositSoup(toRefinery, soupCarrying);
            } else {
                for (Direction dir : Direction.allDirections()) {
                    if (rc.canDepositSoup(dir)) {
                        rc.depositSoup(dir, soupCarrying);
                    }
                }
            }

            if (rc.canMove(toRefinery)) {
                moveInDirection(rc, toRefinery);
            }
        }

        System.out.println("TOTAL MINER BYTECODE USED: " + Clock.getBytecodeNum());

    }

    public static boolean moveInDirection(RobotController rc, Direction dir) throws GameActionException {
        int tolerablePollution = RobotType.REFINERY.globalPollutionAmount + RobotType.REFINERY.localPollutionAdditiveEffect;

        // If areas of no pollution
        Direction initialSearchDirection = dir;
        for (int i = 0; i < Direction.allDirections().length; i++) {
            if (rc.canMove(dir) && !rc.senseFlooding(rc.getLocation().add(dir))
                    && rc.sensePollution(rc.getLocation().add(dir)) <= tolerablePollution) { //TODO: determine actual good value to avoid

                rc.move(dir);
                break;
            } else {
                //if a wall is hit, try a different direction -> TODO: should also get it to back away from wall to improve search area
                dir = dir.rotateLeft();
            }
        }

        // if there is pollution:
        dir = initialSearchDirection;
        for (int i = 0; i < Direction.allDirections().length; i++) {
            if (rc.canMove(dir) && !rc.senseFlooding(rc.getLocation().add(dir))) {
                rc.move(dir);
            } else {
                //if a wall is hit, try a different direction -> TODO: should also get it to back away from wall to improve search area
                dir = dir.rotateLeft();
            }
        }
        //make sure next turn robot goes initial way it was supposed to search
        dir = initialSearchDirection;

        return true;
    }

}



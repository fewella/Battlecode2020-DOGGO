package FirstPlayer;

import battlecode.common.*;

public class Miner {

    static Direction searchDirection = null;
    static MapLocation refineryLocation = null;
    static MapLocation soupLocation = null;

    // toggle for building design school/fulfillment center
    static boolean builtDesignSchool = false;
    static boolean builtRefinery = false;

    public static void run(RobotController rc) throws GameActionException {
        MapLocation currLocation = rc.getLocation();
        int currSoup = rc.getTeamSoup();

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
                    searchDirection = currLocation.directionTo(robot.location).opposite();
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
                    refineryLocation = currLocation.add(dir);
                    Common.broadcast(rc, Common.BroadcastType.MinerBuiltRefinery, refineryLocation.x, refineryLocation.y);
                }
            }
        }

        // Build fulfillment center or design school if able!
//        if (builtDesignSchool) {
//            if (tryBuildBuilding(rc, RobotType.FULFILLMENT_CENTER, currLocation)) {
//                builtDesignSchool = true;
//            }
//        } else {
//            if (tryBuildBuilding(rc, RobotType.DESIGN_SCHOOL, currLocation)) {
//                builtDesignSchool = false;
//            }
//        }

        // Move.
        int soupCarrying = rc.getSoupCarrying();
        System.out.println("I HAVE " + soupCarrying + " SOUP");
        boolean haveSpace = soupCarrying < RobotType.MINER.soupLimit;

        if (soupLocation != null && haveSpace) {
            // IF WE FOUND THE SOUP, GO GIT IT

            // Algorithm:
            // 1. If I can mine soup, mine it.
            // 2. If can't check other locations and update soupLocation if necessary
            // 3. Lastly, move towards soupLocation (pathfinding algo)
            // 4. If no soup - set soupLocation to null (starts scan again)

            Direction toSoup = currLocation.directionTo(soupLocation);
            if (rc.canMineSoup(toSoup)) {
                rc.mineSoup(toSoup);

            } else {
                for (Direction dir : Direction.allDirections()) {
                    if (rc.canMineSoup(dir)) {
                        rc.mineSoup(dir);
                        soupLocation = currLocation.add(dir);
                    }
                }
            }

            moveInDirection(rc, toSoup);

        } else if (!haveSpace && refineryLocation != null) {
            // GO BACK TO REFINERY

            // Algorithm:
            // 1. If I can deposit in direction, do that
            // 2. If fails, check remaining directions to be safe
            // 3. Else, move towards refinery

            Direction toRefinery = currLocation.directionTo(refineryLocation);
            if (rc.canDepositSoup(toRefinery)) {
                rc.depositSoup(toRefinery, soupCarrying);
            } else {
                for (Direction dir : Direction.allDirections()) {
                    if (rc.canDepositSoup(dir)) {
                        rc.depositSoup(dir, soupCarrying);
                    }
                }
            }

            moveInDirection(rc, toRefinery);

        } else {
            System.out.println("SEARCH DIRECTION");
            moveInDirection(rc, searchDirection);
        }

        System.out.println("TOTAL MINER BYTECODE USED: " + Clock.getBytecodeNum());
    }


    public static boolean moveInDirection(RobotController rc, Direction dir) throws GameActionException {
        int tolerablePollution = RobotType.REFINERY.globalPollutionAmount + RobotType.REFINERY.localPollutionAdditiveEffect;

        // If areas of no pollution
        Direction initialSearchDirection = dir;
        for (int i = 0; i < Direction.allDirections().length; i++) {
            System.out.println("Trying direction:" + dir.toString());
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
            System.out.println("Trying direction (pollution loop):" + dir.toString());
            if (rc.canMove(dir) && !rc.senseFlooding(rc.getLocation().add(dir))) {
                rc.move(dir);
            } else {
                //if a wall is hit, try a different direction -> TODO: should also get it to back away from wall to improve search area
                dir = dir.rotateLeft();
            }
        }

        return true;
    }

    static boolean tryBuildBuilding(RobotController rc, RobotType building, MapLocation currLocation) throws GameActionException{
        for (Direction dir : Direction.allDirections()) {
            boolean isSoup = rc.senseSoup(currLocation.add(dir)) > 0;
            if (!isSoup && rc.isReady() && rc.canBuildRobot(building, dir)) {
                rc.buildRobot(building, dir);
                return true;
            }
        }

        return false;
    }
}



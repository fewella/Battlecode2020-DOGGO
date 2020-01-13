package FirstPlayer;

import battlecode.common.*;

public class Miner {

    static Direction searchDirection = null;

    static Direction prevDirection = null;
    static MapLocation soupLocation = null;
    static MapLocation myHQLocation = null;
    static MapLocation refineryLocation = null;

    static boolean builtDesignSchool = false;
    static boolean builtFulfillmentCenter = false;


    static boolean goingBackToHQ = false;

    public static void run(RobotController rc) throws GameActionException {
        MapLocation currLocation = rc.getLocation();
        int currSoup = rc.getTeamSoup();

        // TODO: read block
        // Check PREVIOUS TURN chain, check for soup and refinery
        Transaction[] transactions = rc.getBlock(rc.getRoundNum() - 1);
        for (Transaction transaction : transactions) {
            int[] message = transaction.getMessage();
            if (message[6] == Common.SIGNATURE) {

                if (message[0] == Common.MINER_FOUND_SOUP_NUM && soupLocation == null) {
                    soupLocation = new MapLocation(message[1], message[2]);
                    System.out.println("SETTING SOUP LOCATION");
                }
            }
        }

        // Search for soup! But only if we haven't found soup
        // If find, broadcast its location
        if (soupLocation == null) {
            int radius = Common.getRealRadius(RobotType.MINER);
            soupLocation = Common.searchForTile(rc, currLocation, Common.SEARCH_SOUP, radius);
            if (soupLocation != null) {
                //Common.broadcast(rc, Common.BroadcastType.MinerFoundSoup, soupLocation.x, soupLocation.y);
            }
        }

        // Logic for moving!
        // First, determine if I need to go back to HQ
        if (goingBackToHQ) {
            if (designSchoolTooFar(currLocation)) {
                moveInDirection(rc, currLocation.directionTo(myHQLocation));
            } else {
                goingBackToHQ = false;
            }
        }

        // Now, determine which direction to move if I haven't yet
        if (searchDirection == null) {
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
            for (RobotInfo robot : nearbyRobots) {
                if (robot.getType() == RobotType.HQ && robot.getTeam() == rc.getTeam()) {
                    searchDirection = currLocation.directionTo(robot.location).opposite();
                    break;
                }
            }
        }

        // TODO: Make sure other uses of `nearby` don't require other team robots
        RobotInfo[] nearby = rc.senseNearbyRobots(RobotType.MINER.sensorRadiusSquared, rc.getTeam());

        // Check if refinery exists nearby
        // While we're at it, check for fulfillment centers, design schools, and HQ

        for (RobotInfo curr : nearby) {
            switch (curr.getType()) {
                case DESIGN_SCHOOL:
                    builtDesignSchool = true;
                    break;

                case FULFILLMENT_CENTER:
                    builtFulfillmentCenter = true;
                    break;

                case HQ:
                    myHQLocation = curr.location;
                    break;

                case REFINERY:
                    refineryLocation = curr.location;
                    break;

                default:
                    break;
            }
        }


        // Build fulfillment center or design school if able
        // Make sure they're not too far
        if (!builtDesignSchool) {
            if (tryBuildBuilding(rc, RobotType.DESIGN_SCHOOL, currLocation, false)) {
                if (designSchoolTooFar(currLocation)) {
                    goingBackToHQ = true;
                } else {
                    if (tryBuildBuilding(rc, RobotType.DESIGN_SCHOOL, currLocation, true)) {
                        builtDesignSchool = true;
                    }
                }
            }
        }
        if (!builtFulfillmentCenter) {
            if (tryBuildBuilding(rc, RobotType.FULFILLMENT_CENTER, currLocation, false)) {
                if (fulfillmentCenterTooFar(currLocation)) {
                    goingBackToHQ = true;
                } else {
                    if (tryBuildBuilding(rc, RobotType.FULFILLMENT_CENTER, currLocation, true)) {
                        builtFulfillmentCenter = true;
                    }
                }
            }
        }

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

            boolean mined = false;

            Direction toSoup = currLocation.directionTo(soupLocation);
            if (rc.canMineSoup(toSoup)) {
                rc.mineSoup(toSoup);
                mined = true;

            } else {
                for (Direction dir : Direction.allDirections()) {
                    if (rc.canMineSoup(dir)) {
                        rc.mineSoup(dir);
                        mined = true;
                        soupLocation = currLocation.add(dir);
                    }
                }
            }

            // Set soupLocation to null if soup is depleted in an area
            int distanceToSoup = currLocation.distanceSquaredTo(soupLocation);
            if (!mined && distanceToSoup <= 2) {
                soupLocation = null;
            }

            moveInDirection(rc, toSoup);

        } else if (!haveSpace && myHQLocation != null) {
            // GO BACK TO REFINERY

            // Algorithm:
            // 1. If I can deposit in direction, do that
            // 2. If fails, check remaining directions to be safe
            // 3. Else, move towards refinery

            Direction toRefinery = currLocation.directionTo(myHQLocation);
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
            System.out.println("SEARCH DIRECTION: " + searchDirection);
            moveInDirection(rc, searchDirection);
        }

        System.out.println("TOTAL MINER BYTECODE USED: " + Clock.getBytecodeNum());
    }


    public static boolean moveInDirection(RobotController rc, Direction dir) throws GameActionException {
        MapLocation currLocation = rc.getLocation();

        int tolerablePollution = (RobotType.REFINERY.globalPollutionAmount + RobotType.REFINERY.localPollutionAdditiveEffect)*100000;

        // If areas of no pollution
        Direction initialSearchDirection = dir;
        for (int i = 0; i < Direction.allDirections().length; i++) {
            // System.out.println("Trying direction:" + dir.toString());

            // If the miner can see water or wall, it should try another direction
            boolean isObstacle = false;

            if (soupLocation == null) {
                Direction left = dir.rotateLeft();
                Direction right = dir.rotateRight();

                //TODO: is this chunk necessary?
                Direction[] testDirections = {left, dir, right};
                for (Direction searchDirection : testDirections) {
                    MapLocation senseLocation = currLocation.add(searchDirection);
                    while (rc.canSenseLocation(senseLocation)) {
                        if (rc.senseFlooding(senseLocation) ||
                                rc.senseElevation(senseLocation) - rc.senseElevation(rc.getLocation()) >= 3
                        || rc.senseElevation(senseLocation) - rc.senseElevation(rc.getLocation()) <= 3) {
                            isObstacle = true;
                        }
                        senseLocation = senseLocation.add(dir);
                    }
                }
            }

            if (rc.canMove(dir) && !rc.senseFlooding(currLocation.add(dir))
                    && rc.sensePollution(currLocation.add(dir)) <= tolerablePollution //TODO: determine actual good value to avoid
                    && (prevDirection == null || !prevDirection.equals(dir.opposite())) ) {
                rc.move(dir);
                prevDirection = dir;
                break;
            } else {
//                //if a wall is hit, try a different direction ->
//                TODO: should also get it to back away from wall to improve search area, loops

                dir = dir.rotateLeft();
            }
        }

        // if there is pollution: //TODO: currently pollution not used
        dir = initialSearchDirection;
        for (int i = 0; i < Direction.allDirections().length; i++) {
            // System.out.println("Trying direction (pollution loop):" + dir.toString());
            if (rc.canMove(dir) && !rc.senseFlooding(rc.getLocation().add(dir))
                    && (prevDirection == null || !prevDirection.equals(dir.opposite())) ) {
                rc.move(dir);
                prevDirection = dir;
            } else {
                //if a wall is hit, try a different direction ->
                // TODO: should also get it to back away from wall to improve search area, also avoid loops
//
                dir = dir.rotateLeft();
            }
        }

        return true;
    }

    /**
     *
     * @param rc RobotController
     * @param building Type of building to build
     * @param currLocation Current location of robot trying to build
     * @param build whether or not to actually build the building, or just determine whether CAN build
     * @return whether build can be or was built
     */
    static boolean tryBuildBuilding(RobotController rc, RobotType building, MapLocation currLocation, boolean build) throws GameActionException {
        for (Direction dir : Direction.allDirections()) {
            MapLocation buildLocation = currLocation.add(dir);

            boolean isSoup = rc.senseSoup(buildLocation) > 0;
            boolean adjacentToHQ = buildLocation.distanceSquaredTo(myHQLocation) < 9;

            if (!isSoup && !adjacentToHQ && rc.canBuildRobot(building, dir)) {
                if (build) {
                    rc.buildRobot(building, dir);
                }
                return true;
            }
        }

        return false;
    }

    /**
     * @param currLocation Location of miner
     * @return whether a landscaper is 100% for sure able to see the HQ
     */
    static boolean designSchoolTooFar(MapLocation currLocation) {
        Direction oppositeHQ = currLocation.directionTo(myHQLocation).opposite();
        MapLocation farthestBuild = currLocation.add(oppositeHQ);
        return (!farthestBuild.isWithinDistanceSquared(myHQLocation, RobotType.LANDSCAPER.sensorRadiusSquared));
    }

    static boolean fulfillmentCenterTooFar(MapLocation currLocation) {
        return currLocation.distanceSquaredTo(myHQLocation) >= 50;
    }

}



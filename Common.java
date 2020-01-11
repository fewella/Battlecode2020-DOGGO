package FirstPlayer;

import battlecode.common.*;

public class Common {

    enum BroadcastType {
        MinerFoundSoup,
        MinerBuiltRefinery,
        LandscaperWantsDrone
    }

    static final int MINER_FOUND_SOUP_NUM     = 0;
    static final int MINER_FOUND_REFINERY_NUM = 1;
    static final int LANDSCAPER_WANTS_DRONE = 2;

    static final int SEARCH_SOUP = 1;
    static final int SEARCH_FLOOD = 2;

    static final int START_COST         = 15;

    static final int SIGNATURE = 687971717;


    static boolean tryBuild(RobotController rc, RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }

    static int getRealRadius(RobotType robotType) {
        return (int)Math.ceil(Math.sqrt(robotType.sensorRadiusSquared));
    }

    /**
     * Searches the radius of a robot for a particular type of tile
     * @param tile 1 -> soup; 2 -> water/flooded
     * @param radius radius to search
     * @return MapLocation of tile if found, null otherwise
     */
    static MapLocation searchForTile(RobotController rc, MapLocation currLocation, int tile, int radius) throws GameActionException {
        MapLocation tileLocation = null;
        MapLocation senseLocation = new MapLocation(currLocation.x - radius, currLocation.y - radius);

        boolean searchingEast = true;
        int maxSoup = 0;
        for (int i = 0; i < radius * 2; i++) {
            for (int j = 0; j < radius * 2; j++) {
                if (rc.canSenseLocation(senseLocation)) {

                    if (tile == SEARCH_SOUP) {
                        int soupFound = rc.senseSoup(senseLocation);
                        if (soupFound > maxSoup) {
                            tileLocation = senseLocation;
                            maxSoup = soupFound;
                        }
                    } else if (tile == SEARCH_FLOOD) {
                        if (rc.senseFlooding(senseLocation)) {
                            tileLocation = senseLocation;
                            break;
                        }
                    } else if (tile > 100) {
                        if(rc.canSenseRobot(tile)){
                            tileLocation = senseLocation;
                            break;
                        }
                    }
                }

                if (searchingEast) {
                    senseLocation = senseLocation.add(Direction.EAST);
                } else {
                    senseLocation = senseLocation.add(Direction.WEST);
                }
            }

            if (tileLocation != null && tile != SEARCH_SOUP) {
                break;
            }
            senseLocation = senseLocation.add(Direction.NORTH);
            searchingEast = !searchingEast;
        }

        return tileLocation;
    }

    /**
     * Broadcasts a certain type of update to the blockchain
     *
     * @return whether broadcast was successful or not
     */
    static boolean broadcast(RobotController rc, BroadcastType type, int x, int y) throws GameActionException {
        int[] message = new int[7];

        switch (type) {

            case MinerFoundSoup:
                message[0] = MINER_FOUND_SOUP_NUM;
                break;

            case MinerBuiltRefinery:
                message[0] = MINER_FOUND_REFINERY_NUM;
                break;

            case LandscaperWantsDrone:
                message[0] = LANDSCAPER_WANTS_DRONE;
                //message[4] = //TODO transmit HQ coords

            default:
                message[0] = -1;
                break;
        }

        message[1] = x;
        message[2] = y;
        message[6] = SIGNATURE;

        if (rc.canSubmitTransaction(message, START_COST)) {
            rc.submitTransaction(message, START_COST);
            System.out.println("TRANSMITTING");
            return true;
        } else {
            return false;
        }
    }
}

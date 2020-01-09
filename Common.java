package FirstPlayer;

import battlecode.common.*;



public class Common {

    enum BroadcastType {
        MinerFoundSoup,
        MinerBuiltRefinery
    }

    static final int MINER_FOUND_SOUP_NUM     = 0;
    static final int MINER_FOUND_REFINERY_NUM = 1;

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

            default:
                message[-1] = -1;
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

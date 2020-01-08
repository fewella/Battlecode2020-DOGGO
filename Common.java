package FirstPlayer;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

enum BroadcastType {
    MinerFoundSoup,
    MinerBuiltRefinery
}

public class Common {

    static final int MINER_COST         = 70;
    static final int LANDSCAPER_COST    = 150;
    static final int DRONE_COST         = 150;
    static final int REFINERY_COST      = 200;
    static final int VAPORATOR_COST     = 1000;
    static final int DESIGN_SCHOOL_COST = 150;
    static final int FULFILLMENT_COST   = 150;
    static final int NET_GUN_COST       = 250;

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
    static boolean broadcast(BroadcastType type, int x, int y) {
        return true;
    }

}

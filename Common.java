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

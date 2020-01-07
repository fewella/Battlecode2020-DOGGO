package FirstPlayer;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

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

}

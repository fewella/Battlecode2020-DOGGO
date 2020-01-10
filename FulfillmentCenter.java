package FirstPlayer;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class FulfillmentCenter {
    public static void run(RobotController rc) throws GameActionException {
        if(rc.getTeamSoup()/2 > RobotType.DELIVERY_DRONE.cost && rc.getRoundNum()%2 == 1) {
            for (Direction dir : Direction.allDirections()) {
                Common.tryBuild(rc, RobotType.DELIVERY_DRONE, dir);
            }
        }

    }
}

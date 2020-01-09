package FirstPlayer;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class FulfillmentCenter {
    public static void run(RobotController rc) throws GameActionException {
        if(rc.getTeamSoup()/4 > RobotType.DELIVERY_DRONE.cost) {
            for (Direction dir : Direction.allDirections()) {
                Common.tryBuild(rc, RobotType.DELIVERY_DRONE, dir);
            }
        }

    }
}

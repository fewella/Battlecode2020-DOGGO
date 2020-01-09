package FirstPlayer;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class DesignSchool {


    public static void run(RobotController rc) throws GameActionException {
        if(rc.getTeamSoup()/2 > RobotType.LANDSCAPER.cost)
        {
             for (Direction dir : Direction.allDirections()) {
            Common.tryBuild(rc, RobotType.LANDSCAPER, dir);
            }
        }
    }
}

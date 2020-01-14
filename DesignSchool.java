package FirstPlayer;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class DesignSchool {

    static int attacking = 3;
    static int defending = 8;

    public static void run(RobotController rc) throws GameActionException {

        Direction[] nonCardinal = {Direction.NORTHEAST, Direction.NORTHWEST, Direction.SOUTHEAST, Direction.SOUTHWEST};

        if(rc.getTeamSoup()/2 > RobotType.LANDSCAPER.cost && rc.getRoundNum() % 2 == 0)
        {
            if (attacking > 0) {
                for (Direction dir : Direction.cardinalDirections()) {
                    if(Common.tryBuild(rc, RobotType.LANDSCAPER, dir)) {
                        attacking--;
                        break;
                    }
                }
            }

            else {
                for (Direction dir : nonCardinal) {
                    if (Common.tryBuild(rc, RobotType.LANDSCAPER, dir)) {
                        defending--;
                        break;
                    }
                }
            }

        }

    }
}

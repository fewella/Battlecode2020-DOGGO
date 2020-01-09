package FirstPlayer;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Landscaper {

    static MapLocation HQLocation = null;

    public static void run(RobotController rc) throws GameActionException {
       boolean attacker = rc.getRoundNum()%2 == 0;

        if(!attacker) { //defender, will wall the base TODO: later care about water round elevations
            while (rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
                digFromFlood(rc);
            }
        }else{
            //look to see if enemy HQ in range and dig them in

            //if not, send message to get carried over
        }
    }

    static void digFromFlood(RobotController rc){

    }
}

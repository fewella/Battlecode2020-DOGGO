package FirstPlayer;

import battlecode.common.*;

public class FulfillmentCenter {
    public static void run(RobotController rc) throws GameActionException {

        // Check PREVIOUS TURN chain, check for landscaper wanting drone
        Transaction[] transactions = rc.getBlock(rc.getRoundNum() - 1);
        for (Transaction transaction : transactions) {
            int[] message = transaction.getMessage();
            if (message[6] == Common.SIGNATURE && message[0] == Common.LANDSCAPER_WANTS_DRONE) {
                if(rc.getTeamSoup() > RobotType.DELIVERY_DRONE.cost) {
                    for (Direction dir : Direction.allDirections()) {
                        Common.tryBuild(rc, RobotType.DELIVERY_DRONE, dir);
                    }
                }
            }
        }

    }
}

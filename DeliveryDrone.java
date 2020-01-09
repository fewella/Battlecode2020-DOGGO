package FirstPlayer;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Transaction;

// IDEAS:
// Blitz: drop off miners near enemy HQ and bury it
// Cows: Slow down enemy completely

public class DeliveryDrone {

    static MapLocation landscaperLoc = null;

    public static void run(RobotController rc) throws GameActionException {

        // Check PREVIOUS TURN chain, check for landscaper ready to attack
        Transaction[] transactions = rc.getBlock(rc.getRoundNum() - 1);
        for (Transaction transaction : transactions) {
            int[] message = transaction.getMessage();
            if (message[6] == Common.SIGNATURE) {

                if (message[0] == Common.LANDSCAPER_WANTS_DRONE) {
                    landscaperLoc = new MapLocation(message[1], message[2]);
                    System.out.println("SETTING LANDSCAPER LOCATION");
                }
            }
        }


    }
}

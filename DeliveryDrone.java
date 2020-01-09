package FirstPlayer;

import battlecode.common.*;

// IDEAS:
// Blitz: drop off miners near enemy HQ and bury it
// Cows: Slow down enemy completely

public class DeliveryDrone {

    static MapLocation landscaperLoc = null;
    static MapLocation homeArea = null;
    static int myLandscaperID = -1;

    public static void run(RobotController rc) throws GameActionException {

        // Check PREVIOUS TURN chain, check for landscaper ready to attack
        Transaction[] transactions = rc.getBlock(rc.getRoundNum() - 1);
        for (Transaction transaction : transactions) {
            int[] message = transaction.getMessage();
            if (message[6] == Common.SIGNATURE) {

                if (message[0] == Common.LANDSCAPER_WANTS_DRONE && myLandscaperID == -1) {
                    landscaperLoc = new MapLocation(message[1], message[2]);
                    homeArea = new MapLocation(message[3], message[4]);
                    myLandscaperID = message[5];
                    System.out.println("SETTING LANDSCAPER LOCATION");
                }
            }
        }

        if(myLandscaperID > 0 && landscaperLoc != null && homeArea != null){
            //if next to the landscaper
            if(!rc.isCurrentlyHoldingUnit()) {
                if (rc.getLocation().isAdjacentTo(landscaperLoc)) {
                    if (rc.canPickUpUnit(myLandscaperID)) {
                        rc.pickUpUnit(myLandscaperID);
                    } else { //still looking
                        //robot no longer there
                        if (rc.getLocation().equals(landscaperLoc)) {
                            myLandscaperID = -1;
                            landscaperLoc = null;
                            homeArea = null;
                        }
                        //not near robot yet
                        moveInDirection(rc, rc.getLocation().directionTo(landscaperLoc));
                    }
                }
            }else{
                //look for enemy HQ

            }
        }


    }

    static boolean moveInDirection(RobotController rc, Direction dir) throws GameActionException {
        System.out.println("ENTERING MOVE");

        MapLocation currLocation = rc.getLocation();

        for (int i = 0; i < Direction.allDirections().length; i++) {
            System.out.println("Trying direction " + dir.toString());
            if (rc.canMove(dir)) {
                rc.move(dir);
            } else {
                //if a wall is hit, try a different direction -> TODO: should also get it to back away from wall to improve search area
                return false;
            }
        }
        System.out.println("RETURNING FROM MOVE");
        return true;
    }
}

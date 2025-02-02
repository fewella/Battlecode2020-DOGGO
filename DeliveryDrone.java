package FirstPlayer;

import battlecode.common.*;

// IDEAS:
// Blitz: drop off miners near enemy HQ and bury it
// Cows: Slow down enemy completely

public class DeliveryDrone {

    static MapLocation landscaperLoc = null;
    static MapLocation homeArea = null;
    static MapLocation defHQ = null;
    static int myLandscaperID = -1;
    static int searchSpot = 0;
    static boolean deliveredLandscaper = false;

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
                    System.out.println("SETTING LANDSCAPER LOCATION " + myLandscaperID);
                }
            }
        }

        if(deliveredLandscaper){ //if delivered one, try to help attack HQ
            RobotInfo[] nearby = rc.senseNearbyRobots(RobotType.DELIVERY_DRONE.sensorRadiusSquared, rc.getTeam().opponent());
            for (RobotInfo robot : nearby) {
                if (robot.getType() == RobotType.LANDSCAPER) {
                    if(rc.canPickUpUnit(robot.getID())){
                        rc.canPickUpUnit(robot.getID());
                    }else{
                        moveInDirection(rc, rc.getLocation().directionTo(robot.getLocation()));
                    }
                }
            }
            MapLocation water = Common.searchForTile(rc, rc.getLocation(), Common.SEARCH_FLOOD, RobotType.DELIVERY_DRONE.sensorRadiusSquared);
            if(water != null){
                if(water.isAdjacentTo(rc.getLocation())){
                    rc.dropUnit(rc.getLocation().directionTo(water));
                }else{
                    moveInDirection(rc, rc.getLocation().directionTo(defHQ).opposite());
                }
            }
        }

        //fetch a landscaper
        else if(myLandscaperID > 0 && landscaperLoc != null && homeArea != null){
            //if next to the landscaper
            if(!rc.isCurrentlyHoldingUnit()) {
                 if (rc.canPickUpUnit(myLandscaperID)) {
                     rc.pickUpUnit(myLandscaperID);
                     searchSpot = rc.getRoundNum()%3; //randomize
                 }else { //still looking
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
            else{ //carrying landscaper
                //if know enemy hq
                if(defHQ != null){
                    Direction dropDirInit = rc.getLocation().directionTo(defHQ);
                    Direction[] dropDirs = {dropDirInit, dropDirInit.rotateLeft(), dropDirInit.rotateRight()};
                    boolean dropped = false;
                    for(Direction dropDir: dropDirs) {
                        MapLocation dropLoc = rc.getLocation().add(dropDir);
                        if (dropLoc.isWithinDistanceSquared(defHQ, 2)) {
                            if (rc.canDropUnit(dropDir) && !rc.senseFlooding(dropLoc)) {
                                rc.dropUnit(dropDir);
                                dropped = true;
                                myLandscaperID = -1;
                                searchSpot = 0;
                                deliveredLandscaper = true;
                            } else {

                                //moveInDirection(rc, dropDir.rotateLeft().rotateLeft());
                            }
                        }
                    }
                    if(!dropped){
                            moveInDirection(rc, dropDirInit);
                        }


                }else{
                    //look for enemy HQ
                    MapLocation spot1, spot2, spot3;
                    spot1 = new MapLocation(rc.getMapWidth() - homeArea.x, homeArea.y);
                    spot2 = new MapLocation(homeArea.x, rc.getMapHeight() - homeArea.y);
                    spot3 = new MapLocation(spot1.x, spot2.y);
                    MapLocation[] potentialHQ = {spot1, spot2, spot3};
                    if (rc.getLocation().isWithinDistanceSquared(potentialHQ[searchSpot], RobotType.LANDSCAPER.sensorRadiusSquared / 2)) {
                        boolean HQFound = false;
                        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
                        for (RobotInfo robot : nearbyRobots) {
                            if (robot.getType().equals(RobotType.HQ) && robot.getTeam().equals(rc.getTeam().opponent())) {
                                HQFound = true;
                                System.out.println("I HAVE ENEMY HQ");
                                defHQ = robot.getLocation();
                                break;
                            }
                        }
                        if (HQFound) {
                            Direction dropDirInit = rc.getLocation().directionTo(defHQ);
                            System.out.println("Enemy HQ in dirction: " + dropDirInit);
                            Direction[] dropDirs = {dropDirInit, dropDirInit.rotateLeft(), dropDirInit.rotateRight()};
                            boolean dropped = false;
                            for(Direction dropDir: dropDirs) {
                                MapLocation dropLoc = rc.getLocation().add(dropDir);
                                if (dropLoc.isWithinDistanceSquared(defHQ, 2)) {
                                    System.out.println("Trying to drop in direction: " + dropDir);
                                    if (rc.canDropUnit(dropDir) && !rc.senseFlooding(dropLoc)) {
                                        rc.dropUnit(dropDir);
                                        dropped = true;
                                        myLandscaperID = -1;
                                        searchSpot = 0;
                                        deliveredLandscaper = true;
                                    }
                                }
                            }
                            if(!dropped){
                                moveInDirection(rc, dropDirInit);
                            }
                        }
                        else {
                            searchSpot++;
                            if (searchSpot >= potentialHQ.length)
                                searchSpot = 0;
                        }
                    }
                    moveInDirection(rc, rc.getLocation().directionTo(potentialHQ[searchSpot]));
                }
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
                //if a wall is hit, try a different direction
                dir = dir.rotateLeft();
            }
        }
        System.out.println("RETURNING FROM MOVE");
        return true;
    }
}

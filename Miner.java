package FirstPlayer;

import battlecode.common.*;

public class Miner {

    public static void run(RobotController rc) throws GameActionException {

        // Search for soup!
        int radius = Common.getRealRadius(RobotType.MINER);
        MapLocation currLocation = rc.getLocation();
        MapLocation senseLocation = new MapLocation(currLocation.x - radius, currLocation.y - radius);

        boolean searchingEast = true;
        for (int i = 0; i < radius * 2; i++) {
            for (int j  = 0; j < radius * 2; j++) {
                int soupFound = 0;
                if (rc.canSenseLocation(senseLocation)) {
                    soupFound = rc.senseSoup(senseLocation);
                }
                if (soupFound > 0) {
                    System.out.println("FOUND SOUP - NOTIFY OTHER MINERS");
                }

                if (searchingEast) {
                    senseLocation = senseLocation.add(Direction.EAST);
                } else {
                    senseLocation = senseLocation.add(Direction.WEST);
                }
            }
            senseLocation = senseLocation.add(Direction.NORTH);
            searchingEast = !searchingEast;
        }


        // Logic for moving!
    }



}

package FirstPlayer;

import battlecode.common.*;

import java.util.Arrays;
import java.util.List;

public class Landscaper {

    static MapLocation opponentHQLocation = null;
    static MapLocation myHQLocation = null;

    static boolean placed = false;
    static boolean closestSpotFound = false;

    static boolean chosen = false;
    static boolean attacker = false;
    static boolean digFromHQ = false;
    static boolean broadcastRefinery = false;

    static Direction searchDirection = Direction.WEST;

    static int toTravel = Common.getRealRadius(RobotType.LANDSCAPER);
    static int travelled = toTravel / 2;

    static MapLocation closestSpot = null;
    static MapLocation loopStart = null;

    public static void run(RobotController rc) throws GameActionException {
        // Determine if attacking or defending
        if (!chosen) {
            MapLocation DSLocation = null;
            RobotInfo[] nearby = rc.senseNearbyRobots(RobotType.LANDSCAPER.sensorRadiusSquared, rc.getTeam());
            for (RobotInfo robot : nearby) {
                if (robot.getType() == RobotType.DESIGN_SCHOOL) {
                    DSLocation = robot.getLocation();
                    break;
                }
            }
            Direction fromDS = DSLocation.directionTo(rc.getLocation());

            List<Direction> cardinals = Arrays.asList(Direction.cardinalDirections());
            if (cardinals.contains(fromDS)) {
                attacker = true;
            } else {
                attacker = false;
            }

            chosen = true;
        }

        // If defending, tell miners to build a refinery
        if (!attacker && !broadcastRefinery) {
            Common.broadcast(rc, Common.BroadcastType.BuildRefinery, 0, 0);
            broadcastRefinery = true;
        }

        // Look for HQ if don't have its location
        if(myHQLocation == null){
            RobotInfo[] nearby = rc.senseNearbyRobots(RobotType.LANDSCAPER.sensorRadiusSquared, rc.getTeam());
            for (RobotInfo curr : nearby) {
                if (curr.getType() == RobotType.HQ) {
                    myHQLocation = curr.location;
                }
            }
        }

        System.out.println("after serach hqlocation: " + myHQLocation);

        if (!attacker) {
            //defender, will wall the base TODO: later care about water round elevations
            if (myHQLocation != null) {
                placed = goToHQ(rc);
                if (placed) {
                    holeInHQ(rc);
                }
            } else {
                searchForHQ(rc);
            }

        } else {
            //look to see if enemy HQ in range and dig them in
            if(opponentHQLocation == null) {
                RobotInfo[] nearby = rc.senseNearbyRobots(RobotType.LANDSCAPER.sensorRadiusSquared, rc.getTeam().opponent());
                for (RobotInfo curr : nearby) {
                    if (curr.getType() == RobotType.HQ) {
                        opponentHQLocation = curr.location;
                    }
                }
            }
            if(opponentHQLocation != null){
                Direction opponentHQDirection = rc.getLocation().directionTo(opponentHQLocation);
                if(rc.getLocation().isAdjacentTo(opponentHQLocation)){
                    tryBury(rc, opponentHQDirection);
                }
                else{
                    //either wall in the way, or not right next to HQ
                    if(notClosestSpot(rc, opponentHQLocation)){
                        System.out.println("looking for closest spot:" + opponentHQDirection.toString());
                    }
                    else{
                        System.out.println("found closest spot!");
                        tryDig(rc, opponentHQDirection); //take away wall and put under self
                    }
                }
            }else if(rc.getRoundNum()%20 - rc.getID()%10 < 2){
                //if not, send message to get carried over
                broadcastPickup(rc, rc.getLocation().x, rc.getLocation().y, myHQLocation.x, myHQLocation.y);
            }

        }
    }

    static boolean notClosestSpot(RobotController rc, MapLocation idealAdjSpot) throws GameActionException {
        MapLocation currLocation = rc.getLocation();
        if(currLocation.isAdjacentTo(idealAdjSpot)){
            closestSpot = null;
            closestSpotFound = false;
            loopStart = null;
            System.out.println("1");

            return false;

        }
        else if(loopStart == null){
            loopStart = currLocation;
            closestSpot = currLocation;
            Miner.moveInDirection(rc, currLocation.directionTo(idealAdjSpot));

            System.out.println("2");
            return true;
        }
        else if(loopStart.equals(currLocation)) {
            closestSpotFound = true;
            if (!currLocation.equals(closestSpot)) {
                Miner.moveInDirection(rc, currLocation.directionTo(closestSpot));

                System.out.println("3");
                return true;
            }
            System.out.println("3.1");
        }
        else if(closestSpotFound){
            if (!currLocation.equals(closestSpot)) {
                Miner.moveInDirection(rc, currLocation.directionTo(closestSpot));

                System.out.println("4");
                return true;
            }else{
                closestSpotFound = false;
                closestSpot = null;
                loopStart = null;

                System.out.println("5");
                return false;
            }
        }else{
            if(closestSpot == null || closestSpot.distanceSquaredTo(idealAdjSpot) > currLocation.distanceSquaredTo(idealAdjSpot)){
                closestSpot = currLocation;
            }
            Miner.moveInDirection(rc, currLocation.directionTo(idealAdjSpot));
            System.out.println("6");
            return true;
        }

        System.out.println("7");
        Miner.moveInDirection(rc, currLocation.directionTo(idealAdjSpot));
        return true;
    }

    static void searchForHQ(RobotController rc) throws GameActionException {
        if (travelled < toTravel) {
            if (Miner.moveInDirection(rc, searchDirection)) {
                travelled++;
            }
        } else {
            searchDirection.rotateLeft();
            searchDirection.rotateLeft();
            Miner.moveInDirection(rc, searchDirection);
            travelled = 0;
        }
    }

    static boolean goToHQ(RobotController rc) throws GameActionException {
        MapLocation currLocation = rc.getLocation();
        System.out.println("urrlocation and myhqlocation: " + currLocation + " " + myHQLocation);
        if (currLocation.distanceSquaredTo(myHQLocation) <= 2) {
            return true;

        } else {
            int closestDistance = 9999;
            MapLocation closestStation = null;
            for (Direction dir : Direction.allDirections()) {
                if (dir != Direction.CENTER) {

                    MapLocation station = myHQLocation.add(dir);
                    //Direction directionToStation = currLocation.directionTo(station);
                    int distanceSquaredToStation = currLocation.distanceSquaredTo(station);

                    if (rc.canSenseLocation(station)) {
                        if (rc.senseRobotAtLocation(station) == null && distanceSquaredToStation < closestDistance) {
                            closestDistance = distanceSquaredToStation;
                            closestStation = station;
                        }
                    }
                }
            }

            if (closestStation != null) {
                if (Math.abs(rc.senseElevation(closestStation) - rc.senseElevation(currLocation)) > 6) {
                    System.out.println("need a lift!!");
                }
            }

            if (closestStation != null) {
                Miner.moveInDirection(rc, currLocation.directionTo(closestStation));
            } else {
                Miner.moveInDirection(rc, currLocation.directionTo(myHQLocation));
            }

            return currLocation.distanceSquaredTo(myHQLocation) <= 2;
        }
    }

    static void holeInHQ(RobotController rc) throws GameActionException {
        MapLocation currLocation = rc.getLocation();

        // Before holing in, let's check whether we can scoot around to hit all stations
        Direction[] diagonalsArray = {Direction.NORTHEAST, Direction.NORTHWEST, Direction.SOUTHWEST, Direction.SOUTHEAST};
        List<Direction> diagonals = Arrays.asList(diagonalsArray);

        Direction fromHQ = myHQLocation.directionTo(currLocation);
        if (diagonals.contains(fromHQ)) {
            MapLocation rightLocation = myHQLocation.add(fromHQ.rotateRight());
            MapLocation leftLocation = myHQLocation.add(fromHQ.rotateLeft());

            if (rc.canSenseLocation(rightLocation)) {
                if (rc.senseRobotAtLocation(rightLocation) == null) {
                    Direction rightDir = currLocation.directionTo(rightLocation);
                    if (rc.canMove(rightDir)) {
                        rc.move(rightDir);
                    }
                }
            }

            if (rc.canSenseLocation(leftLocation)) {
                if (rc.senseRobotAtLocation(leftLocation) == null) {
                    Direction leftDir = currLocation.directionTo(leftLocation);
                    if (rc.canMove(leftDir)) {
                        rc.move(leftDir);
                    }
                }
            }
        }

        // Now, Determine which direction to dig dirt from
        // Be sure to check whether HQ needs digging
        Transaction[] transactions = rc.getBlock(rc.getRoundNum() - 1);
        for (Transaction transaction : transactions) {
            int[] message = transaction.getMessage();
            if (message[6] == Common.SIGNATURE && message[0] == Common.HQ_BEING_BURIED) {
                if (message[1] == 1) {
                    digFromHQ = true;
                } else if (message[1] == 0) {
                    digFromHQ = false;
                }
            }
        }

        if (rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
            if (!digFromHQ) {
                Direction digDirection = currLocation.directionTo(myHQLocation).opposite();
                Direction[] sources = {digDirection.rotateLeft(), digDirection, digDirection.rotateRight()};
                for (Direction dir : sources) {
                    boolean robotInWay = false;
                    MapLocation dirtLocation = currLocation.add(dir);
                    RobotInfo rob = rc.senseRobotAtLocation(dirtLocation);

                    if (rob != null) {
                        if (rob.getType() != RobotType.DELIVERY_DRONE) {
                            robotInWay = true;
                        }
                    }

                    if (rc.canDigDirt(dir) && !robotInWay) {
                        rc.digDirt(dir);
                    }
                }

            } else {
                Direction dirToHQ = currLocation.directionTo(myHQLocation);
                if (rc.canDepositDirt(dirToHQ)) {
                    rc.digDirt(dirToHQ);
                }
            }

        } else if (rc.isReady()) {
            // Finally, deposit dirt at the lowest elevation around the HQ
            int lowestElevation = 99999;
            Direction bestDir = null;
            for (Direction dir : Direction.allDirections()) {
                MapLocation station = currLocation.add(dir);
                Direction dirToStation = currLocation.directionTo(station);
                if (rc.canDepositDirt(dirToStation) && !station.equals(myHQLocation) && station.distanceSquaredTo(myHQLocation) <= 2) {
                    int stationElevation = rc.senseElevation(station);

                    if (stationElevation < lowestElevation) {
                        lowestElevation = stationElevation;
                        bestDir = dirToStation;
                    }
                }
            }

            if (rc.canDepositDirt(bestDir)) {
                rc.depositDirt(bestDir);
            }
        }
    }

    static boolean tryBury(RobotController rc, Direction dir) throws GameActionException {
        if(rc.canDigDirt(Direction.CENTER)){
            rc.digDirt(Direction.CENTER); //dig under landscaper
            return true;
        }else if(rc.canDepositDirt(dir)){
            rc.depositDirt(dir);
            return false;
        }
        return false;
    }

    static boolean tryDig(RobotController rc, Direction dir) throws GameActionException {
        if(rc.canDigDirt(dir)){
            rc.digDirt(dir);
            return true;
        }else if(rc.canDepositDirt(Direction.CENTER)){
            rc.depositDirt(Direction.CENTER);
            return false;
        }
        return false;
    }

    static boolean broadcastPickup(RobotController rc, int xL, int yL, int xH, int yH) throws GameActionException {
        int[] message = new int[7];
        message[0] = Common.LANDSCAPER_WANTS_DRONE;
        message[1] = xL;
        message[2] = yL;
        message[3] = xH;
        message[4] = yH;
        message[5] = rc.getID();
        message[6] = Common.SIGNATURE;

        if (rc.canSubmitTransaction(message, Common.START_COST/2)) {
            rc.submitTransaction(message, Common.START_COST/2);
            System.out.println("TRANSMITTING");
            return true;
        } else {
            return false;
        }
    }
}

package FirstPlayer;

import battlecode.common.*;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Map;

public class Landscaper {

    static MapLocation opponentHQLocation = null;
    static MapLocation myHQLocation = null;

    static MapLocation floodLocation = null;

    public static void run(RobotController rc) throws GameActionException {

        boolean attacker = true;
        //hopefully spawn near the HQ and can save it
        if(myHQLocation == null){
            RobotInfo[] nearby = rc.senseNearbyRobots(RobotType.MINER.sensorRadiusSquared, rc.getTeam());
            for (RobotInfo curr : nearby) {
                if (curr.getType() == RobotType.HQ) {
                    myHQLocation = curr.location;
                }
            }
        }if(myHQLocation == null){
            myHQLocation = rc.getLocation();
        }

        if (!attacker) { //defender, will wall the base TODO: later care about water round elevations
            if (rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
                digFromFlood(rc);
            } else {
                depositDirt(rc);
            }

        } else {
            //look to see if enemy HQ in range and dig them in
            if(opponentHQLocation == null){
                RobotInfo[] nearby = rc.senseNearbyRobots(RobotType.MINER.sensorRadiusSquared, rc.getTeam().opponent());
                for (RobotInfo curr : nearby) {
                    if (curr.getType() == RobotType.HQ) {
                        opponentHQLocation = curr.location;
                    }
            }
            if(opponentHQLocation != null){
                Direction opponentHQDirection = rc.getLocation().directionTo(opponentHQLocation);
                if(rc.getLocation().isAdjacentTo(opponentHQLocation)){
                    tryDig(rc, opponentHQDirection.opposite());
                }
                else{
                    //either wall in the way, or not right next to HQ
                    if(moveInDirection(rc, opponentHQDirection)){}
                    else{
                        tryDig(rc, opponentHQDirection);
                    }
                }
            }else if(rc.getRoundNum()%20 == 0){
                //if not, send message to get carried over
                broadcastPickup(rc, rc.getLocation().x, rc.getLocation().y, myHQLocation.x, myHQLocation.y);
            }
            }
        }
    }

    static void digFromFlood(RobotController rc) throws GameActionException {
        // Algorithm:
        // 1. If don't have water location, find it
        //      If can't find, search
        // 2. If have location and can't dig after trying,  go towards
        // 3. Else, dig
        MapLocation currLocation = rc.getLocation();

        if (floodLocation == null) {
            int radius = Common.getRealRadius(RobotType.LANDSCAPER);
            floodLocation = Common.searchForTile(rc, currLocation, Common.SEARCH_FLOOD, radius);
        }

        if (floodLocation != null) {
            int distanceToFlood = currLocation.distanceSquaredTo(floodLocation);
            Direction toFlood = currLocation.directionTo(floodLocation);
            if (rc.canDigDirt(toFlood) && distanceToFlood <= 2) {
                rc.digDirt(toFlood);
            } else {
                for (Direction dir : Direction.allDirections()) {
                    if (rc.canDigDirt(dir) && distanceToFlood <= 2) {
                        rc.digDirt(toFlood);
                    }
                }
            }

            moveInDirection(rc, toFlood);
        }
    }

    static void depositDirt(RobotController rc) throws GameActionException {
        MapLocation currLocation = rc.getLocation();
        Direction toHQ = currLocation.directionTo(myHQLocation);

        boolean deposited = false;

        ArrayList<MapLocation> depositSpots = new ArrayList<>();
        for (Direction dir : Direction.allDirections()) {
            depositSpots.add(myHQLocation.add(dir));
        }

        for (Direction dir : Direction.allDirections()) {
            MapLocation depositLocation = currLocation.add(dir);
            if (depositSpots.contains(depositLocation) && rc.canDepositDirt(dir)) {
                rc.depositDirt(dir);
                deposited = true;
            }
        }

        if (!deposited) {
            moveInDirection(rc, toHQ);
        }
    }

    static boolean tryDig(RobotController rc, Direction dir) throws GameActionException {
        if(rc.canDigDirt(dir)){
            rc.digDirt(dir);
            return true;
        }else if(rc.canDepositDirt(dir.opposite())){
            rc.depositDirt(dir.opposite());
            return false;
        }
        return false;
    }

    static boolean moveInDirection(RobotController rc, Direction dir) throws GameActionException {
        System.out.println("ENTERING MOVE");

        MapLocation currLocation = rc.getLocation();

        for (int i = 0; i < Direction.allDirections().length; i++) {
            System.out.println("Trying direction " + dir.toString());
            if (rc.canMove(dir) && !rc.senseFlooding(rc.getLocation().add(dir))) {
                rc.move(dir);
            } else {
                //if a wall is hit, try a different direction -> TODO: should also get it to back away from wall to improve search area
                return false;
            }
        }
        System.out.println("RETURNING FROM MOVE");
        return true;
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

        if (rc.canSubmitTransaction(message, Common.START_COST)) {
            rc.submitTransaction(message, Common.START_COST);
            System.out.println("TRANSMITTING");
            return true;
        } else {
            return false;
        }
    }
}

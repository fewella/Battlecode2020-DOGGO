package FirstPlayer;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Random;

public class Landscaper {

    static MapLocation opponentHQLocation = null;
    static MapLocation myHQLocation = null;

    static boolean placed = false;

    static boolean chosen = false;
    static boolean attacker = false;

    public static void run(RobotController rc) throws GameActionException {
        if (!chosen) {
            attacker = rc.getRoundNum() % 4 < 2;
            chosen = true;
        }

        //hopefully spawn near the HQ and can save it
        if(myHQLocation == null){
            RobotInfo[] nearby = rc.senseNearbyRobots(RobotType.LANDSCAPER.sensorRadiusSquared, rc.getTeam());
            for (RobotInfo curr : nearby) {
                if (curr.getType() == RobotType.HQ) {
                    myHQLocation = curr.location;
                }
            }
        }if(myHQLocation == null){
            myHQLocation = rc.getLocation();
        }

        if (!attacker) { //defender, will wall the base TODO: later care about water round elevations
            placed = goToHQ(rc);
            if (placed) {
                holeInHQ(rc);
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
                    tryDig(rc, opponentHQDirection.opposite());
                }
                else{
                    //either wall in the way, or not right next to HQ
                    if(moveInDirection(rc, opponentHQDirection)){}
                    else{
                        tryDig(rc, opponentHQDirection);
                    }
                }
            }else if(rc.getRoundNum()%20 - rc.getID()%10 == 0){
                //if not, send message to get carried over
                broadcastPickup(rc, rc.getLocation().x, rc.getLocation().y, myHQLocation.x, myHQLocation.y);
            }

        }
    }

    static boolean goToHQ(RobotController rc) throws GameActionException {
        MapLocation currLocation = rc.getLocation();
        if (currLocation.distanceSquaredTo(myHQLocation) <= 2) {
            return true;

        } else {
            for (Direction dir : Direction.allDirections()) {
                if (dir != Direction.CENTER) {
                    MapLocation station = myHQLocation.add(dir);
                    Direction directionToStation = currLocation.directionTo(station);
                    if (rc.canSenseLocation(station)) {
                        if (rc.senseRobotAtLocation(station) == null) {
                            System.out.println("Moving in direction: " + dir);
                            Miner.moveInDirection(rc, directionToStation);
                        }
                    }
                }
            }

            return currLocation.distanceSquaredTo(myHQLocation) <= 2;
        }
    }

    static void holeInHQ(RobotController rc) throws GameActionException {
        MapLocation currLocation = rc.getLocation();

        if (rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
            Direction digDirection = currLocation.directionTo(myHQLocation).opposite();
            Direction[] sources = {digDirection.rotateLeft(), digDirection, digDirection.rotateRight()};
            for (Direction dir : sources) {
                if (rc.canDigDirt(dir)) {
                    rc.digDirt(dir);
                }
            }
        }

        int lowestElevation = 99999;
        Direction bestDir = null;
        for (Direction dir : Direction.allDirections()) {
            if (dir != Direction.CENTER) {
                MapLocation station = myHQLocation.add(dir);
                if (rc.canSenseLocation(station)) {
                    int currElevation = rc.senseElevation(station);
                    Direction toStation = currLocation.directionTo(station);
                    if (currElevation < lowestElevation && rc.canDepositDirt(toStation)) {
                        lowestElevation = currElevation;
                        bestDir = toStation;
                    }
                }
            }
        }

        if (rc.canDepositDirt(bestDir)) {
            rc.depositDirt(bestDir);
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
        MapLocation currLocation = rc.getLocation();

        for (int i = 0; i < Direction.allDirections().length; i++) {
            if (rc.canMove(dir) && !rc.senseFlooding(rc.getLocation().add(dir))) {
                rc.move(dir);
            } else {
               // dir = dir.rotateLeft();
                //if a wall is hit, try a different direction -> TODO: should also get it to back away from wall to improve search area
                return false;
            }
        }
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

        if (rc.canSubmitTransaction(message, Common.START_COST/2)) {
            rc.submitTransaction(message, Common.START_COST/2);
            System.out.println("TRANSMITTING");
            return true;
        } else {
            return false;
        }
    }
}

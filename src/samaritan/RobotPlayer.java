package samaritan;
import battlecode.common.*;

import java.util.ArrayList;
import java.util.Map;

import static battlecode.common.Direction.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    static Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST
    };
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};

    static MessageController controller;
    static int birthday;
    static int turnCount;
    static LocalMap map;
    static Team myTeam;
    static Team enemyTeam;
    static int totalSoupNearby;
    static MapLocation hqLocation;

    static int goal;

    //Goals
    static int NONE = 0;
    static int STARTUP = 1;
    static int BUILD_MINER = 2;
    static int TRAVEL_TO_SOUP = 3;
    static int MINE = 4;
    static int DEPOSIT = 5;
    static int EXPLORE = 6;
    static int BUILD_VAPORATOR = 7;
    static int BUILD_DESIGN_SCHOOL = 8;
    static int BUILD_DEFENSIVE_WALL = 9;
    static int PLOP = 10;
    static int STANDBY = 11;
    static int BUILD_BUILDER = 12;
    static int FIND_PERCH = 13;
    static int GO_TOWARDS_HQ = 14;
    static int BUILD_FULFILLMENT_CENTER = 15;
    static int BUILD_EXPLORERS = 16;
    static int BUILD_WALL_BUILDERS = 17;
    static int QUEUE = 18;
    static int TREK_TO_SOUP = 19;
    static int ATTACK = 20;
    static int BUILD_REFINERY = 21;
    static int GUARD = 22;
    static int BUILD_NET_GUN = 23;

    //Commands
    static char GATHER_SOUP = 'S';
    static char MAKE_DSCHOOL = 'D';
    static char MAKE_FCENTER = 'F';
    static char CEXPLORE = 'E';
    static char SOUP_TREK = 'T';
    static char DEFEND = 'N';
    static char GATHER = 'G';

    static int minersNeeded = 3;
    static int landscapersNeeded = 8;
    static int dronesNeeded = 1;
    static int transactionPrice = 1;
    static int buildingSpace = 2;
    static int explorersNeeded = 2;
    static int soupRadius = 35;
    static int startingSteps = 8;
    static int maxMiners = 2;

    static int wallBuildingTurn = 350;
    static int mobTurn = 450;

    static int thisRound;

    //Miner variables
    static MapLocation motherRefinery;
    static MapLocation preferredDeposit;
    static MapLocation perch;
    static MapLocation buildDestination = null;
    static MapLocation motherBuilding;
    static MapLocation explorationDestination;
    static Direction buildDirection;

    static MapLocation enemyHQ = null;
    static ArrayList<MapLocation> possibleEnemyHQ = new ArrayList<>();
    static ArrayList<Tile> HQueue = new ArrayList<>();
    static ArrayList<Tile> sentSpots = new ArrayList<>();
    static int previousMessageTurn;
    static ArrayList<Tile> publishQueue = new ArrayList<>();
    static ArrayList<MapLocation> stink = new ArrayList<>();
    static ArrayList<MapLocation> repeat = new ArrayList<>();
    static int stinkLength = 2;

    static ArrayList<int[]> enemyPublications = new ArrayList<>();
    static ArrayList enemyTurns = new ArrayList();

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        RobotPlayer.rc = rc;
        initializeRobot();

        while (true) {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                startOfTurn();

                switch (rc.getType()) {
                    case MINER:              runMiner();             break;
                    case LANDSCAPER:         runLandscaper();        break;
                    case DELIVERY_DRONE:     runDeliveryDrone();     break;
                    case HQ:                 runHQ();                break;
                    case REFINERY:           runRefinery();          break;
                    case NET_GUN:            runNetGun();            break;
                    case VAPORATOR:          runVaporator();         break;
                    case DESIGN_SCHOOL:      runDesignSchool();      break;
                    case FULFILLMENT_CENTER: runFulfillmentCenter(); break;
                }

                endOfTurn();
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runHQ() throws GameActionException {
        Tile closestSoup;
        if(goal == STARTUP) {
            System.out.println("HQ Initiating Startup!" + " TCount: " + rc.getRoundNum());
            addPlacesOfInterestToPublishQueue();
            scanArea(false, false, true);
            //totalSoupNearby = map.totalSoup(rc.getLocation(), rc.getCurrentSensorRadiusSquared());
            hqLocation = rc.getLocation();
            findPotentialEnemyHQ();
            goal = BUILD_MINER;
        }

        if(goal == BUILD_MINER) {
            closestSoup = map.nextSoup();
            Direction dir = rc.getLocation().directionTo(toMapLocation(closestSoup));
            if(rc.getTeamSoup() >= RobotType.MINER.cost + transactionPrice) {
                if(forceBuild(RobotType.MINER, dir)) {
                    commandMiner(GATHER_SOUP, -1, -1);
                    minersNeeded--;
                }
            }
            if(minersNeeded <= 0)
                goal = BUILD_EXPLORERS;
        }

        else if(goal == BUILD_BUILDER) {
            MapLocation dest = findSafeBuildLocation();
            Direction dir = rc.getLocation().directionTo(dest);
            if(rc.getTeamSoup() >= RobotType.MINER.cost + transactionPrice) {
                if(forceBuild(RobotType.MINER, dir)) {
                    commandMiner(MAKE_FCENTER, dest.x, dest.y);
                    goal = QUEUE;
                }
            }

        } else if(goal == BUILD_EXPLORERS) {
            Direction dir = rc.getLocation().directionTo(findCenterOfMap());
            if(rc.getTeamSoup() >= RobotType.MINER.cost + transactionPrice) {
                if(forceBuild(RobotType.MINER, dir)) {
                    commandMiner(CEXPLORE, -1, -1);
                    explorersNeeded--;
                }
            }
            if(explorersNeeded <= 0) {
                goal = BUILD_BUILDER;
            }

        } else if(goal == QUEUE) {
            if(maxMiners <= 0) {

            }
            else if(rc.getTeamSoup() >= RobotType.MINER.cost + transactionPrice) {
                //System.out.println("QUEUE");
                if(HQueue.size() > 0) {
                    Tile tempTile = HQueue.remove(0);
                    sentSpots.add(tempTile);
                    char action = tempTile.getLocationType();
                    if(action == 'C') {
                        MapLocation soup = toMapLocation(tempTile);
                        Direction dir = rc.getLocation().directionTo(soup);

                        if(forceBuild(RobotType.MINER, dir)) {
                            commandMiner(SOUP_TREK, tempTile.getX(), tempTile.getY());
                            maxMiners--;
                        }
                    }
                }
            }

        }
    }

    static void runMiner() throws GameActionException {

        if(goal == STARTUP) {

            System.out.println("Miner Calibrating" + " TCount: " + rc.getRoundNum());
            int temp = findPurpose(); //Get its command form the HQ
            if(temp != -1)
                goal = temp;
            else
                goal = TRAVEL_TO_SOUP;
            findHQ(); //This function can be replaced by the
            findPotentialEnemyHQ();
            scanArea(false, false, true);
            motherRefinery = hqLocation;
            preferredDeposit = toMapLocation(map.nextSoup());
            if(preferredDeposit == null) {
                goal = EXPLORE;
            }
            map.initFog(hqLocation);

        } if(goal == STANDBY) {


        } if(goal == TRAVEL_TO_SOUP) {
            if(rc.getLocation().equals(preferredDeposit)) {
                clearStink();
                goal = MINE;
            } else if(rc.getLocation().distanceSquaredTo(preferredDeposit) <= 2 && !isPassible(preferredDeposit)) {
                clearStink();
                goal = MINE;
            } else {
                Direction direction = rc.getLocation().directionTo(preferredDeposit);
                MapLocation startingSpot = rc.getLocation();
                forceMove(direction);
                addStink(startingSpot);
            }

        } if(goal == TREK_TO_SOUP) {
            if(rc.getLocation().equals(preferredDeposit)) {
                clearStink();
            } else if(rc.getLocation().distanceSquaredTo(preferredDeposit) <= 2 && !isPassible(preferredDeposit)) {
                clearStink();
                goal = MINE;
            } else {
                Direction direction = rc.getLocation().directionTo(preferredDeposit);
                MapLocation startingSpot = rc.getLocation();
                forceMove(direction);
                addStink(startingSpot);
            }

        } if(goal == MINE) {
            if(rc.senseSoup(preferredDeposit) <= 0) { //If soup deposit ran out
                map.removeSoup(preferredDeposit);
                //Announce the depletion of soup
                goal = TRAVEL_TO_SOUP;
                clearStink();
                updateSoupDeposit();
                if(goal == TRAVEL_TO_SOUP)
                    forceMove(rc.getLocation().directionTo(preferredDeposit));
            }
            else if(rc.getSoupCarrying() >= RobotType.MINER.soupLimit - 2) { //If robot is full
                goal = DEPOSIT;
                clearStink();
            }
            else {
                tryMine(rc.getLocation().directionTo(preferredDeposit));
            }

        } if(goal == DEPOSIT) {
            if(rc.getLocation().distanceSquaredTo(motherRefinery) >= 35 && rc.getTeamSoup() >= 250 && rc.getRoundNum() < wallBuildingTurn - 120
                    && rc.getLocation().distanceSquaredTo(preferredDeposit) < 20) {
                goal = BUILD_REFINERY;
                RobotInfo[] robots = rc.senseNearbyRobots(35, myTeam);
                for(int i = 0; i < robots.length; i++) {
                    if(robots[i].getType() == RobotType.REFINERY) {
                        motherRefinery = robots[i].getLocation();
                        goal = DEPOSIT;
                    }
                }
            }

            RobotInfo[] robots = rc.senseNearbyRobots(2, myTeam);
            for(int i = 0; i < robots.length; i++) {
                if(robots[i].getType() == RobotType.REFINERY || robots[i].getType() == RobotType.HQ) {
                    if(tryRefine(rc.getLocation().directionTo(robots[i].getLocation()))) {
                        clearStink();
                        goal = TRAVEL_TO_SOUP;
                    }
                    break;
                }
            }
            if(goal == DEPOSIT) {
                MapLocation startingSpot = rc.getLocation();
                forceMove(startingSpot.directionTo(motherRefinery));
                addStink(startingSpot);
            }

        } if(goal == EXPLORE) {
            if(explorationDestination == null) {
                System.out.println("Finding initial destination");
                Direction dir = hqLocation.directionTo(rc.getLocation());
                MapLocation loc = rc.getLocation();
                for(int i = 0; i < 40; i++) {
                    loc = loc.add(dir);
                }
                explorationDestination = map.getFirstFog(loc);
            }

            if(rc.getLocation().equals(explorationDestination)) {
                clearStink();
                map.disperseFog(explorationDestination);
                explorationDestination = map.getClosestFog(rc.getLocation());
                if(explorationDestination == null)
                    goal = STANDBY;
            } else {
                MapLocation startingSpot = rc.getLocation();
                forceMove(rc.getLocation().directionTo(explorationDestination));
                addStink(startingSpot);

                if(ifRepeat() && turnCount > 26) {
                    clearStink();
                    System.out.println("Entering STANDBY");
                    //Call for help
                    goal = STANDBY;
                }
            }

        } if(goal == BUILD_VAPORATOR) {
            forceBuild(RobotType.VAPORATOR, randomDirection());
            rc.getLocation().equals(repeat);
            goal = EXPLORE;

        } if(goal == BUILD_DESIGN_SCHOOL) {

            if(!rc.getLocation().equals(buildDestination)) {
                forceMove(rc.getLocation().directionTo(buildDestination));
            } else {
                if(forceBuild(RobotType.DESIGN_SCHOOL, hqLocation.directionTo(buildDestination))) {
                    rc.getLocation().equals(repeat);
                    goal = BUILD_NET_GUN;
                    buildDestination = designSchoolSpot();
                }
            }

        } if(goal == BUILD_NET_GUN) {
            if(!rc.getLocation().equals(buildDestination)) {
                forceMove(rc.getLocation().directionTo(buildDestination));
            } else {
                if(forceBuild(RobotType.NET_GUN, hqLocation.directionTo(buildDestination))) {
                    rc.getLocation().equals(repeat);
                    goal = EXPLORE;
                }
            }


        } if(goal == BUILD_FULFILLMENT_CENTER) {
            if(buildDestination == null) {
                buildDestination = fulfillmentCenterSpot();
            }

            if(!rc.getLocation().equals(buildDestination)) {
                forceMove(rc.getLocation().directionTo(buildDestination));
            } else {
                if(forceBuild(RobotType.FULFILLMENT_CENTER, hqLocation.directionTo(buildDestination))) {
                    rc.getLocation().equals(repeat);
                    goal = BUILD_DESIGN_SCHOOL;
                    buildDestination = designSchoolSpot();
                }
            }

        } if(goal == BUILD_REFINERY) {
            if(forceBuild(RobotType.REFINERY, SOUTHEAST)) {
                rc.getLocation().equals(repeat);
                goal = MINE;
                RobotInfo[] robots = rc.senseNearbyRobots(2, myTeam);
                for(int i = 0; i < robots.length; i++) {
                    if(robots[i].getType() == RobotType.REFINERY) {
                        motherRefinery = robots[i].getLocation();
                        break;
                    }
                }
            }
        }
    }

    static void runRefinery() throws GameActionException {
        // System.out.println("Pollution: " + rc.sensePollution(rc.getLocation()));
    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {
        if(rc.getRoundNum() >= wallBuildingTurn)
            goal = BUILD_WALL_BUILDERS;

        if(goal == STARTUP) {
            findHQ();
            findPotentialEnemyHQ();
            buildDirection = rc.getLocation().directionTo(hqLocation);
            goal = STANDBY;

        } if(goal == STANDBY) {
            int landscapersNearby = 0;
            RobotInfo[] robots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), myTeam);
            for(int i = 0; i < robots.length; i++) {
                if(robots[i].getType().equals(RobotType.LANDSCAPER)) {
                    landscapersNearby++;
                }
            }

            if(landscapersNearby < 0) {
                forceBuild(RobotType.LANDSCAPER, randomDirection());
            }

        } if(goal == BUILD_WALL_BUILDERS) {
            if(rc.getRoundNum() > wallBuildingTurn && landscapersNeeded > 0) {
                if(tryBuild(RobotType.LANDSCAPER, buildDirection)) {
                    commandMiner(DEFEND, -1, -1);
                    buildDirection = buildDirection.rotateLeft();
                    landscapersNeeded--;
                    if(landscapersNeeded <= 0)
                        goal = ATTACK;
                }
            }
        } if(goal == ATTACK) {
            if(enemyHQ != null) {
                buildDirection = rc.getLocation().directionTo(enemyHQ);
                if(rc.getTeamSoup() >= RobotType.LANDSCAPER.cost + transactionPrice) {
                    if(forceBuild(RobotType.LANDSCAPER, buildDirection)) {
                        commandMiner('A', enemyHQ.x, enemyHQ.y);
                    }
                }
            }
        }
        readTiles();
    }

    static void runFulfillmentCenter() throws GameActionException {
        while(!rc.isReady()) Clock.yield();
        if(dronesNeeded > 0) {
            Direction dir = rc.getLocation().directionTo(findCenterOfMap());
            if(rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost + transactionPrice) {
                if(forceBuild(RobotType.DELIVERY_DRONE, dir))
                    commandMiner(CEXPLORE, -1, -1);
                dronesNeeded--;
            }
        }

        if(rc.getTeamSoup() >= 150*8 + 150 || rc.getRoundNum() > wallBuildingTurn + 50) {
            if(enemyHQ == null) {
                enemyHQ = findCenterOfMap();
            }
            Direction dir = rc.getLocation().directionTo(enemyHQ);
            if(rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost + transactionPrice) {
                if(forceBuild(RobotType.DELIVERY_DRONE, dir))
                    commandMiner(GATHER, -1, -1);
            }
        }
    }

    static void runLandscaper() throws GameActionException {
        if(goal == STARTUP) {
            findHQ();
            findPotentialEnemyHQ();
            perch = getPerch();
            System.out.println("Perch: " + perch.x + ", " + perch.y);
            int temp = findPurpose(); //Get its command form the HQ
            if(temp != -1)
                goal = temp;
            else
                goal = GUARD;

        } if(goal == GUARD) {
            MapLocation target = null;
            RobotInfo[] robots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), myTeam);
            for(int i = 0; i < robots.length; i++) {
                if(robots[i].getType().equals(RobotType.HQ) || robots[i].getType().equals(RobotType.DESIGN_SCHOOL) ||
                        robots[i].getType().equals(RobotType.FULFILLMENT_CENTER)) {
                    if(robots[i].dirtCarrying > 0) {
                        target = robots[i].getLocation();
                    }
                }
            }

            if(target != null) {
                System.out.println("Has Target");
                if(rc.getLocation().distanceSquaredTo(target) <= 2) {
                    tryDig(rc.getLocation().directionTo(target));
                    tryDump(target.directionTo(rc.getLocation()));
                } else {
                    forceMove(rc.getLocation().directionTo(target));
                }
            }


            if(turnCount == 11) {
                forceMove(randomDirection());
            }
            if(rc.getRoundNum() == wallBuildingTurn - 1) {
                rc.disintegrate();
            }

        } if(goal == FIND_PERCH) {
            System.out.println("Moving towards perch");
            MapLocation startingSpot = rc.getLocation();
            forceMove(startingSpot.directionTo(perch));
            addStink(startingSpot);

            if (rc.getLocation().equals(perch)) {
                goal = BUILD_DEFENSIVE_WALL;
            }

        } else if(goal == BUILD_DEFENSIVE_WALL) {
            //System.out.println("Buulding wall");
            if (rc.getDirtCarrying() >= 25) {
                tryDump(CENTER);
            } else {
                tryDig(hqLocation.directionTo(rc.getLocation()));
            }

        } else if(goal == ATTACK) {
            if(turnCount < 50 && turnCount > 25) {
                tryDig(rc.getLocation().directionTo(hqLocation).rotateRight().rotateRight());
            }

            RobotInfo[] robots = rc.senseNearbyRobots(2, enemyTeam);
            for(int i = 0; i < robots.length; i++) {
                if(robots[i].getType().equals(RobotType.HQ)) {
                    if(rc.getDirtCarrying() <= 0) {
                        tryDig(randomDirection());
                    } else {
                        tryDump(rc.getLocation().directionTo(robots[i].getLocation()));
                    }
                    break;
                }
            }

            if(turnCount > 100 && rc.getLocation().distanceSquaredTo(enemyHQ) > 10) {
                rc.disintegrate();
            }
            MapLocation startingSpot = rc.getLocation();
            forceMove(startingSpot.directionTo(enemyHQ));
            addStink(startingSpot);
        }
    }

    static void runDeliveryDrone() throws GameActionException {

        checkForTargets();
        dropUnit();
        if(goal == STARTUP) {

            System.out.println("Drone Calibrating" + " TCount: " + rc.getRoundNum());
            int temp = findPurpose(); //Get its command form the HQ
            if (temp != -1)
                goal = temp;
            else
                goal = EXPLORE;
            findHQ();
            findMotherBuilding();
            scanArea(false, true, true);
            map.initFog(hqLocation);
            explorationDestination = map.getFirstFog(rc.getLocation());

        } if(goal == EXPLORE) {
            if(rc.getLocation().equals(explorationDestination) || (rc.canSenseLocation(explorationDestination)
                    && rc.getLocation().distanceSquaredTo(explorationDestination) <= 2 && rc.isLocationOccupied(explorationDestination))) {
                scanArea(false, false, true);
                map.disperseFog(explorationDestination);
                explorationDestination = map.getClosestFog(rc.getLocation());
                if(explorationDestination == null)
                    goal = ATTACK;
            } else {
                forceMove(rc.getLocation().directionTo(explorationDestination));
            }

        } if(goal == TRAVEL_TO_SOUP) {
            if(rc.getRoundNum() - birthday <= 17) {
                if(enemyHQ == null)
                    enemyHQ = findCenterOfMap();
                forceMove(rc.getLocation().directionTo(enemyHQ));
            } else {
                goal = STANDBY;
            }

        } if(goal == STANDBY) {
            if(rc.getRoundNum() >= wallBuildingTurn + mobTurn) {
                goal = ATTACK;
            }

        } if(goal == ATTACK) {
            forceMove(rc.getLocation().directionTo(enemyHQ));
        }
        checkForEnemyHQ();
    }

    static void runNetGun() throws GameActionException {

        RobotInfo[] robots = rc.senseNearbyRobots(2, myTeam);
        for(int i = 0; i < robots.length; i++) {
            if(robots[i].getType().equals(RobotType.DESIGN_SCHOOL)) {
                rc.disintegrate();
            }
        }
    }

    public static void checkForTargets() throws GameActionException {
        if(rc.isCurrentlyHoldingUnit())
            return;

        RobotInfo[] robots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), enemyTeam);
        for(int i = 0; i < robots.length; i++) {
            if(robots[i].getType().equals(RobotType.MINER) || robots[i].getType().equals(RobotType.LANDSCAPER)) {
                if(rc.getLocation().distanceSquaredTo(robots[i].getLocation()) <= 2) {
                    if(rc.canPickUpUnit(robots[i].getID())) {
                        rc.pickUpUnit(robots[i].getID());
                    }
                } else {
                    forceMove(rc.getLocation().directionTo(robots[i].getLocation()));
                }
                break;
            }
        }
    }

    public static void dropUnit() throws GameActionException {
        if(!rc.isCurrentlyHoldingUnit())
            return;

        if(rc.senseFlooding(rc.getLocation())) {
            if(rc.canDropUnit(CENTER)) {
                rc.dropUnit(CENTER);
                return;
            }
        }

        if(!rc.isReady()) return;

        Direction dir = NORTH;
        MapLocation location = rc.getLocation().add(dir);
        if(rc.canDropUnit(dir) && rc.senseFlooding(location)) {
            rc.dropUnit(dir);
            return;
        }
        Direction direction;
        for(int i = 0; i < 4; i++) {
            direction = dir;
            for(int j = 0; j <= i; j++) {
                direction = direction.rotateLeft();
            }
            location = rc.getLocation().add(direction);
            if(rc.canDropUnit(direction) && rc.senseFlooding(location)) {
                rc.dropUnit(direction);
                return;
            }

            direction = dir;
            for(int j = 0; j <= i; j++) {
                direction = direction.rotateRight();
            }
            location = rc.getLocation().add(direction);
            if(rc.canDropUnit(direction) && rc.senseFlooding(location)) {
                rc.dropUnit(direction);
                return;
            }
        }

        if(goal == ATTACK) {
            forceMove(enemyHQ.directionTo(rc.getLocation()));
        }
    }

    /**
     * this method could be changed to instead use the 3 possible locations of the HQ and deduce the HQ location
     */
    public static void checkForEnemyHQ() {
        if(enemyHQ != null)
            return;
        RobotInfo[] robots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), enemyTeam);
        for(int i = 0; i < robots.length; i++) {
            if(robots[i].getType().equals(RobotType.HQ)) {
                System.out.println("Found!");
                enemyHQ = robots[i].getLocation();
                Tile tempTile = new Tile(enemyHQ.x, enemyHQ.y);
                tempTile.setLocationType('H');
                publishQueue.add(tempTile);
            }
        }
    }

    public static void readTiles() throws GameActionException {
        Transaction[] t = rc.getBlock(rc.getRoundNum() - 1);
        int[] encodedMessage = null;
        DecodedMessage decodedMessage;

        for(int i = 0; i < t.length; i++) {
            encodedMessage = t[i].getMessage();

            if (controller.validateMessage(encodedMessage, rc.getRoundNum() - 1)){
                System.out.println("Message sent on turn " + (rc.getRoundNum() - 1) + " is valid, decoding...");
                decodedMessage = controller.decodeMessage(encodedMessage, rc.getRoundNum() - 1);
                //get tile information
                for(int j = 0; j < decodedMessage.getNumTiles(); j++) {
                    Tile tempTile = decodedMessage.getTile(j);
                    handleTile(tempTile);
                }

                previousMessageTurn = rc.getRoundNum() - 1;

            } else {
                if((encodedMessage[2] - 11)/5 != rc.getRoundNum() - 1) {
                    System.out.println("Invalid message on turn " + (rc.getRoundNum() - 1));
                    enemyPublications.add(encodedMessage);
                    enemyTurns.add(rc.getRoundNum());
                    if(enemyPublications.size() > 2) {
                        enemyPublications.remove(0);
                        enemyTurns.remove(0);
                    }
                }
            }
        }
    }

    public static void publishTiles() throws GameActionException {
        if(rc.getType().equals(RobotType.MINER) && rc.getRoundNum() - birthday < 10)
            return;
        if(publishQueue.size() <= 0)
            return;
        System.out.println("Running publishTiles. queue size: " + publishQueue.size());

        int encodedMessage[];
        controller.createMapMessage(rc.getRoundNum(), previousMessageTurn);
        previousMessageTurn = rc.getRoundNum();

        int s = publishQueue.size();
        for(int i = 0; i < Math.min(s, 6); i++) {
            controller.encodeLocation(publishQueue.remove(0));
        }
        encodedMessage = controller.getEncodedMessage();
        rc.submitTransaction(encodedMessage, transactionPrice);
        System.out.println("Message sent");
    }

    public static void handleTile(Tile tempTile) {
        //System.out.println("Reading Location: " + tempTile.getX() + ", " + tempTile.getY() + ", " + tempTile.getLocationType());

        if(tempTile.getLocationType() == 'C') {
            if(!map.hasSoup(toMapLocation(tempTile))) {
                //System.out.println("New Soup SPot Found!");
                map.addSoup(toMapLocation(tempTile), 100);
                if(rc.getType().equals(RobotType.HQ) && notRepresented(tempTile)) {
                    HQueue.add(tempTile);
                    System.out.println("HQ adding soup to queue");
                }
            }
        }

        else if(tempTile.getLocationType() == 'H') { //Fix
            enemyHQ = toMapLocation(tempTile);
            System.out.println("Saving Enemy HQ");
        }

        else if(tempTile.getLocationType() == 'I') {
            hqLocation = toMapLocation(tempTile);
            //System.out.println("HQ Location: " + hqLocation.x + ", " + hqLocation.y);
        }
    }

    /**
     * determines if the HQ has already added a similar soup tile to queue
     * @param tempTile
     */
    public static boolean notRepresented(Tile tempTile) {
        for(int i = 0; i < HQueue.size(); i++) {
            if(toMapLocation(tempTile).distanceSquaredTo(toMapLocation(HQueue.get(i))) <= soupRadius) {
                return false;
            }
        }

        for(int i = 0; i < sentSpots.size(); i++) {
            if(toMapLocation(tempTile).distanceSquaredTo(toMapLocation(sentSpots.get(i))) <= soupRadius) {
                return false;
            }
        }
        return true;
    }

    /**
     * method to keep track of stink to help with path finding
     * @param location
     */
    public static void addStink(MapLocation location) {
        stink.add(location);
        if(stink.size() > stinkLength) {
            repeat.add(stink.remove(0));
            if(repeat.size() > 12)
                repeat.remove(0);
        }
    }

    public static void clearStink() {
        stink.clear();
        repeat = new ArrayList<>();
    }

    public static boolean notStink(MapLocation location) {
        for(int i = 0; i < stink.size(); i++) {
            if(stink.get(i).equals(location))
                return false;
        }
        return true;
    }

    public static boolean ifRepeat() {
        for(int i = 0; i < repeat.size(); i++) {
            if(rc.getLocation().equals(repeat.get(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Temporary method
     * Used for the HQ to tell a miner it just created what its purpose is
     * @param command
     */
    static void commandMiner(char command, int destX, int destY) throws GameActionException {
        if(rc.getTeamSoup() < transactionPrice) {
            System.out.println("ERR - commandMiner: called with not enough soup");
            return;
        }
        int[] message = new int[7];
        message[2] = rc.getRoundNum()*5 + 11;
        message[0] = command;
        message[3] = destX;
        message[4] = destY;
        rc.submitTransaction(message, transactionPrice);
        System.out.println("Submitting message on turn " + rc.getRoundNum());
    }

    /**
     * Temporary method
     * Publishes location of hq at start of game
     * @throws GameActionException
     */
    static void addPlacesOfInterestToPublishQueue() throws GameActionException {
        int x = rc.getLocation().x;
        int y = rc.getLocation().y;
        Tile temp = new Tile(x, y);
        temp.setLocationType('I');

        publishQueue.add(temp);
        System.out.println("Adding HQ Location");
        /*int[] encodedMessage;

        controller.createMapMessage(rc.getRoundNum(), 0);
        Tile hq = new Tile(x, y);
        hq.setLocationType('D');
        controller.encodeLocation(hq);
        encodedMessage = controller.getEncodedMessage();
        rc.submitTransaction(encodedMessage, transactionPrice);
        previousMessageTurn = 1;*/
    }

    /**
     * Used for the miner to determine its first goal
     *
     * Make this use encoding for the x and y so that the enemy can't use it
     * @return
     * @throws GameActionException
     */
    static int findPurpose() throws GameActionException {
        //debugBlockchain(rc.getRoundNum() - 1);
        Transaction[] t = rc.getBlock(birthday);
        int[] message = null;
        for(int i = 0; i < t.length; i ++) {
            message = t[i].getMessage();
            if((message[2] - 11)/5 == birthday) {
                System.out.println("Message: " + (char)message[0]);
                if(message[0] == GATHER_SOUP)
                    return TRAVEL_TO_SOUP;
                if(message[0] == MAKE_DSCHOOL) {
                    return BUILD_DESIGN_SCHOOL;
                }
                if(message[0] == MAKE_FCENTER) {
                    buildDestination = new MapLocation(message[3], message[4]);
                    return BUILD_FULFILLMENT_CENTER;
                }
                if(message[0] == CEXPLORE)
                    return EXPLORE;
                if(message[0] == SOUP_TREK) {
                    preferredDeposit = new MapLocation(message[3], message[4]);
                    return TREK_TO_SOUP;
                }
                if(message[0] == 'A') {
                    System.out.println("Attacking sir");
                    enemyHQ = new MapLocation(message[3], message[4]);
                    return ATTACK;
                }
                if(message[0] == DEFEND) {
                    return FIND_PERCH;
                }
                if(message[0] == GATHER) {
                    return TRAVEL_TO_SOUP;
                }
            }
        }
        return -1;
    }

    static void findPotentialEnemyHQ() {
        /*//Horizontal
        MapLocation horizontal = new MapLocation(hqLocation.x, rc.getMapHeight() - hqLocation.y);
        possibleEnemyHQ.add(horizontal);

        //Vertical
        MapLocation vertical = new MapLocation(rc.getMapWidth() - hqLocation.x, hqLocation.y);
        possibleEnemyHQ.add(vertical);

        MapLocation rotational = new MapLocation(rc.getMapWidth() - hqLocation.x, rc.getMapHeight() - hqLocation.y);
        possibleEnemyHQ.add(rotational);*/
    }

    static void initializeRobot() throws GameActionException {
        //System.out.println("I'm a " + rc.getType() + ", my ID is " + rc.getID() + " and I just got created!"  + " TCount: " + rc.getRoundNum());

        turnCount = 0;
        birthday = rc.getRoundNum() - 1;

        controller = new MessageController();
        map = new LocalMap(rc.getLocation(), rc.getMapWidth(), rc.getMapHeight()); //create the local map
        myTeam = rc.getTeam();
        enemyTeam = rc.getTeam().opponent();
        goal = STARTUP;
    }

    public static void startOfTurn() throws GameActionException {
        while(turnCount > 10 && rc.getCooldownTurns() >= 1) { //If the robot wont be able to move anyway, just skip the turn
            //System.out.println("Pollution too high, skipping turn: " + rc.getRoundNum());
            Clock.yield();
        }
        thisRound = rc.getRoundNum();
        turnCount += 1;

        //scanArea(false, true, false);
        //hq/netgun: if drone in range, shoot it down
        //drone: if can pick up enemy unit, do so
        //Landscaper: if next to building that is damaged, pick up dirt from it

        //Read Blockchain
        if(turnCount > 1)
            readTiles();

        if(rc.getType().equals(RobotType.HQ) || rc.getType().equals(RobotType.NET_GUN)) {
            RobotInfo[] robots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), enemyTeam);
            for(int i = 0; i < robots.length; i++) {
                if(robots[i].getType().equals(RobotType.DELIVERY_DRONE)) {
                    if(rc.canShootUnit(robots[i].getID())) {
                        rc.shootUnit(robots[i].getID());
                        break;
                    }
                }
            }
        }
    }

    public static void endOfTurn() throws GameActionException {

        if(rc.getType() == RobotType.MINER || rc.getType() == RobotType.LANDSCAPER || rc.getType() == RobotType.DELIVERY_DRONE) {
            if(turnCount > 1) {
                scanArea(false, false, true); //Scan area before turn
            }
        }
        if(enemyHQ == null) {
            checkForEnemyHQ();
        }
        publishTiles();

        if(rc.getType().equals(RobotType.HQ) && rc.getRoundNum()%42 == 13) {
            publishFake();
        }

        if(rc.getRoundNum() != thisRound) { //Checks is more than 1 round was needed to run this turn
            System.out.println("Bytecode Limit Exceeded - Round Started: " + thisRound + ". Round Finished: " + rc.getRoundNum());
            //if(turnCount <= 10)
            //System.out.print(" On Startup");
        }
    }

    public static void publishFake() throws GameActionException {
        if(enemyPublications.size() < 2)
            return;
        int[] message = new int[7];
        for(int i = 0; i < message.length; i++) {
            int[] m1 = enemyPublications.get(1);
            int[] m0 = enemyPublications.get(0);
            int t1 = (int) enemyTurns.get(1);
            int t0 = (int) enemyTurns.get(0);
            message[i] = (rc.getRoundNum() - t0)*(m1[i] - m0[i])/(t1- t0) + m0[i];
            if(Math.random()*17 < 2) {
                message[i] += 26;
            }
        }
        try{
            if(rc.canSubmitTransaction(message, transactionPrice)) {
                rc.submitTransaction(message, transactionPrice);
            }
        } catch(GameActionException g) {
            System.out.println("Errorowirnow");
        }

    }

    public static void findMotherBuilding() {
        RobotInfo[] r = rc.senseNearbyRobots(2, myTeam);
        for(int i = 0; i < r.length; i++) {
            if(r[i].getClass().equals(RobotType.FULFILLMENT_CENTER) || r[i].getClass().equals(RobotType.DESIGN_SCHOOL)) {
                motherBuilding = r[i].getLocation();
                return;
            }
        }
    }

    /**
     * Saves the location of the HQ
     */
    static void findHQ() throws GameActionException {
        //System.out.println("Starting findHQ");
        int firstTurn = -1;
        int turn = rc.getRoundNum() - 1;
        Transaction[] t;
        int[] encodedMessage = null;
        DecodedMessage decodedMessage;

        while(firstTurn == -1) { //Find first turn
            t = rc.getBlock(turn);

            for(int i = 0; i < t.length; i++) {
                encodedMessage = t[i].getMessage();
                if (controller.validateMessage(encodedMessage, turn)){
                    firstTurn = turn;
                    //System.out.println("Most Recent Message Turn: " + turn);
                }
            }
            turn--;
        }

        previousMessageTurn = firstTurn;

        while(firstTurn >= 1) {
            t = rc.getBlock(firstTurn);

            for(int i = 0; i < t.length; i++) {
                encodedMessage = t[i].getMessage();
                if (controller.validateMessage(encodedMessage, firstTurn)) {
                    //System.out.println("Decoding turn: " + firstTurn);
                    decodedMessage = controller.decodeMessage(encodedMessage, firstTurn);
                    //get tile information
                    for(int j = 0; j < decodedMessage.getNumTiles(); j++) {
                        Tile tempTile = decodedMessage.getTile(j);
                        handleTile(tempTile);
                    }

                    firstTurn = decodedMessage.getLastMessageTurn();
                }
            }
        }
        /*System.out.println("Invalid message (HQ)");*/
    }

    /**
     * Finds a new closest deposit one its preferredDeposit runs out
     */
    static void updateSoupDeposit() {
        //System.out.println("Changing preferredSDeposit");
        preferredDeposit = toMapLocation(map.nextSoup());
        if(preferredDeposit == null) {
            goal = EXPLORE;
        }
    }

    public static MapLocation getPerch() throws GameActionException {
        RobotInfo[] robots = rc.senseNearbyRobots(2, myTeam);
        for(int i = 0; i < robots.length; i++) {
            if(robots[i].getType() == RobotType.DESIGN_SCHOOL) {
                Direction dir = robots[i].getLocation().directionTo(rc.getLocation());
                return hqLocation.add(dir);
            }
        }
        return null;
    }

    /**
     * Tries to dig in the specified direction
     */
    public static boolean tryDig(Direction dir) throws GameActionException {

        for(int i = 0; i < 8; i++) {
            MapLocation loc = rc.getLocation().add(dir);
            if(rc.onTheMap(loc)) {
                 if(!rc.isLocationOccupied(loc)) {
                     break;
                 }
                 RobotInfo robot = rc.senseRobotAtLocation(loc);
                 if(!robot.getType().equals(RobotType.LANDSCAPER) && !robot.getType().equals(RobotType.HQ)) {
                     break;
                 }
            }
            dir = dir.rotateLeft();
            System.out.println("Rotateing!");
        }



        if (rc.isReady() && rc.canDigDirt(dir)) {
            rc.digDirt(dir);
            return true;
        } else return false;
    }

    /**
     * Tries to deposit dirt in the specified direction
     */
    public static boolean tryDump(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositDirt(dir)) {
            rc.depositDirt(dir);
            return true;
        } else return false;
    }


    /**
     * Returns a random Direction.
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    /**
     * Returns a random RobotType spawned by miners.
     * @return a random RobotType
     */
    static RobotType randomSpawnedByMiner() {
        return spawnedByMiner[(int) (Math.random() * spawnedByMiner.length)];
    }


    /**
     * Makes the robot move. If it can't move in the preferred direction, it will move in the next closest direction
     * @param dir
     * @return if the forceMove was successful
     */
    static boolean forceMove(Direction dir) throws GameActionException {
        if(!rc.isReady()) {
            //if(turnCount > 10)
                //System.out.println("ERR - forceMove: failed (1) Direction: " + dir.toString());
            return false;
        }

        MapLocation location = rc.getLocation().add(dir);
        if(rc.canMove(dir) && (!rc.senseFlooding(location) || rc.getType().equals(RobotType.DELIVERY_DRONE)) && notStink(location)) {
            rc.move(dir);
            return true;
        }
        Direction direction;
        for(int i = 0; i < 4; i++) {
            direction = dir;
            for(int j = 0; j <= i; j++) {
                direction = direction.rotateLeft();
            }
            location = rc.getLocation().add(direction);
            if(rc.canMove(direction) && (!rc.senseFlooding(location) || rc.getType().equals(RobotType.DELIVERY_DRONE)) && notStink(location)) {
                rc.move(direction);
                return true;
            }

            direction = dir;
            for(int j = 0; j <= i; j++) {
                direction = direction.rotateRight();
            }
            location = rc.getLocation().add(direction);
            if(rc.canMove(direction) && (!rc.senseFlooding(location) || rc.getType().equals(RobotType.DELIVERY_DRONE)) && notStink(location)) {
                rc.move(direction);
                return true;
            }
        }
        System.out.println("ERR - forceMove: failed");
        return false;
    }

    /**
     * Attempts to move in a given direction.
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        // System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.isReady() && rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }

    /**
     * Iterates through every direction closest to preferred direction to ensure a successful build
     */
    static boolean forceBuild(RobotType type, Direction dir) throws GameActionException {
        if(!rc.isReady()) return false;

        MapLocation location = rc.getLocation().add(dir);
        if(rc.canBuildRobot(type, dir) && !rc.senseFlooding(location)) {
            rc.buildRobot(type, dir);
            return true;
        }
        Direction direction;
        for(int i = 0; i < 4; i++) {
            direction = dir;
            for(int j = 0; j <= i; j++) {
                direction = direction.rotateLeft();
            }
            location = rc.getLocation().add(direction);
            if(rc.canBuildRobot(type, direction) && !rc.senseFlooding(location)) {
                rc.buildRobot(type, direction);
                return true;
            }

            direction = dir;
            for(int j = 0; j <= i; j++) {
                direction = direction.rotateRight();
            }
            location = rc.getLocation().add(direction);
            if(rc.canBuildRobot(type, direction) && !rc.senseFlooding(location)) {
                rc.buildRobot(type, direction);
                return true;
            }
        }
        return false;
    }

    static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        }
        //System.out.println("Build Failed");
        return false;
    }

    /**
     * Attempts to mine soup in a given direction.
     *
     * @param dir The intended direction of mining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMine(Direction dir) throws GameActionException {
        if(!rc.isReady()) {
            //System.out.println("Tried to mine " + dir.toString() + " While not ready");
            return false;
        }
        if (rc.canMineSoup(dir)) {
            rc.mineSoup(dir);
            return true;
        }
        //System.out.println("Mine Failed");
        return false;
    }

    /**
     * Attempts to refine soup in a given direction.
     *
     * @param dir The intended direction of refining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        }
        //System.out.println("Deposit Failed");
        return false;
    }

    /**
     * returns preferred spot to build design school
     */
    public static MapLocation designSchoolSpot() {
        MapLocation l = hqLocation;
        for(int i = 0; i < buildingSpace; i++) {
            l = l.add(rc.getLocation().directionTo(hqLocation).opposite().rotateRight());
        }
        return l;
    }

    public static MapLocation fulfillmentCenterSpot() {
        MapLocation l = hqLocation;
        for(int i = 0; i < buildingSpace; i++) {
            l = l.add(rc.getLocation().directionTo(hqLocation).opposite());
        }
        if(rc.onTheMap(l))
            return l;

        l = hqLocation;
        for(int i = 0; i < buildingSpace; i++) {
            l = l.add(hqLocation.directionTo(findCenterOfMap()));
        }
        return l;
    }


    /**
     * search every space within sensor radius for soup, will update local map with locations
     * Also finds elevation of every tile within radius
     */
    static void scanArea(boolean scanElevation, boolean scanRobots, boolean scanSoup) throws GameActionException {

        if(scanElevation) {
            int elevation;
            MapLocation scanLocation;
            boolean scanComplete, nextLocationFound;

            scanComplete = false;
            scanLocation = rc.getLocation();

            //Find starting location
            while(rc.canSenseLocation(scanLocation.add(NORTHEAST)) && rc.onTheMap(scanLocation.add(NORTHEAST))) {
                scanLocation = scanLocation.add(NORTHEAST);
            }
            while(rc.canSenseLocation(scanLocation.add(EAST)) && rc.onTheMap(scanLocation.add(EAST))) {
                scanLocation = scanLocation.add(EAST);
            }
            while(rc.canSenseLocation(scanLocation.add(NORTH)) && rc.onTheMap(scanLocation.add(NORTH))) {
                scanLocation = scanLocation.add(NORTH);
            }
            //System.out.println("Starting location: " + scanLocation.x + ", " + scanLocation.y);


            Direction[] algorythim = {SOUTH, SOUTHWEST, WEST, NORTHWEST, NORTH, NORTHWEST, WEST, SOUTHWEST};
            int index = 0;
            while(!scanComplete) {
                nextLocationFound = false;

                //check location for soup
                //System.out.println("Checking for soup at: " + scanLocation.x + ", " + scanLocation.y);
                try{
                    scanElevation(scanLocation);

                }catch(GameActionException exception){
                    //game exception
                    scanComplete = true;
                    System.out.println("ERR SoupScan - " + exception.getMessage());
                /*
                if (exception.getType().equals(GameActionExceptionType.OUT_OF_RANGE)){
                    scanComplete = true;
                    System.out.println("ERR SoupScan OutOfBounds - " + exception.getMessage());
                }else{
                    scanComplete = true;
                    System.out.println("ERR SoupScan - " + exception.getMessage());
                }
                 */
                }

                //Find next scan location
                for(int i = 0; i < 4; i++) {
                    if(rc.canSenseLocation(scanLocation.add(algorythim[index])) && rc.onTheMap(scanLocation.add(algorythim[index]))) {
                        scanLocation = scanLocation.add(algorythim[index]);
                        if(index != 0 && index != 4) index += 4-i;
                        nextLocationFound = true;
                        break;
                    }
                    index++;
                }

                if(index == 8) index = 0;
                if(!nextLocationFound) scanComplete = true;
            }
        }

        if(scanSoup) {
            findSoup(); //These things do not need a tile-by-tile scan. They can just use the built in functions
        }
        if(scanRobots) {
            findUnits();
        }
    }

    public static void scanElevation(MapLocation location) throws GameActionException {

        int elevation = rc.senseElevation(location); //get elevation data
        map.recordElevation(location, elevation); //store elevation data
    }

    public static void findSoup() throws GameActionException {
        //System.out.println("Qf: " + map.hasSoup(new MapLocation(32, 28)));
        MapLocation[] soups = rc.senseNearbySoup();
        for(int i = 0; i < soups.length; i++) {
            if(!map.hasSoup(soups[i])) {
                int amnt = rc.senseSoup(soups[i]);
                map.addSoup(soups[i], amnt);
                Tile temp = new Tile(soups[i].x, soups[i].y);
                temp.setLocationType('C'); //Soup
                temp.setSoupAmt(amnt/4);
                publishQueue.add(temp);
                System.out.println("Adding soup tile: " + temp.getX() + ", " + temp.getY() + ", Bytcodes left: " + Clock.getBytecodesLeft());

                if(goal == EXPLORE && rc.getType().equals(RobotType.MINER)) {
                    goal = TRAVEL_TO_SOUP;
                    preferredDeposit = toMapLocation(map.closestSoup(rc.getLocation()));
                }
            }
        }
    }

    public static void findUnits() {
        map.clearEnemies();
        RobotInfo[] robots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), enemyTeam);
        for(int i = 0; i < robots.length; i++) {
            map.recordEnemy(robots[i]);
        }
    }

    /**
     * Gets the center of the map, for exploration purposes
     */
    public static MapLocation findCenterOfMap() {
        return new MapLocation((int) (rc.getMapWidth()/2), (int) (rc.getMapHeight()/2));
    }

    /**
     * Convert robotType into char for storage in map
     * @param inRobotType robot type to be converted
     * @return
     */
    public static char convertRobotTypeToChar(RobotType inRobotType){
        switch(inRobotType){
            case HQ : return 'D';
            case COW: return 'E';
            case MINER: return 'F';
            case NET_GUN: return 'G';
            case REFINERY: return 'H';
            case VAPORATOR: return 'I';
            case LANDSCAPER: return 'J';
            case DESIGN_SCHOOL: return 'K';
            case DELIVERY_DRONE: return 'L';
            case FULFILLMENT_CENTER: return 'M';
            default: return 'A';
        }
    }

    //turns a tile into a MapLocation for easy processing
    public static MapLocation toMapLocation(Tile t) {
        if(t == null)
            return null;
        return new MapLocation(t.getX(), t.getY());
    }

    //Returns the distance squared between
    public static int distanceBetween(Tile t1, Tile t2) { return toMapLocation(t1).distanceSquaredTo(toMapLocation(t2)); }

    public static void printLocalMap(){
        for (int i = 0; i< rc.getMapHeight(); i++){
            for (int j=0; j<rc.getMapWidth(); j++){
                System.out.print(map.getLocationData(new MapLocation(j,i)));
            }
            System.out.println(); //ENTER key for new line
        }
    }
    public static void debugBlockchain(int turnNum) throws GameActionException {
        Transaction[] t = rc.getBlock(turnNum);
        for(int i = 0; i < t.length; i++) {
            Transaction message = t[i];
            for(int j = 0; j < message.getMessage().length; j++) {
                System.out.print(message.getMessage()[j]);
            }
        }
        System.out.println("");
    }

    public static boolean isPassible(MapLocation l) throws GameActionException {
        int h = rc.senseElevation(rc.getLocation());
        int g = rc.senseElevation(l);
        int k = Math.abs(h - g);
        if(rc.isLocationOccupied(l) || k > 3)
            return false;
        return true;
    }

    /**
     * A quick example to show how the new communication system can be called
     */
    public static void CommunicationExample(){
        int encodedMessage[];
        MapLocation mapLocation;
        DecodedMessage decodedMessage;
        Tile[] decodedTiles = new Tile[6];
        controller = new MessageController(); //call this at the beginning of the robot init stuff

        //creates a map message
        controller.createMapMessage(50, 35);
        mapLocation = new MapLocation(64, 1);
        controller.encodeLocation(map.getTile(mapLocation));
        mapLocation = new MapLocation(64, 2);
        controller.encodeLocation(map.getTile(mapLocation));
        encodedMessage = controller.getEncodedMessage();

        //decode message
        if (controller.validateMessage(encodedMessage, 50)){
            System.out.println("Message is valid, decoding...");
            decodedMessage = controller.decodeMessage(encodedMessage, 50);
            decodedMessage.getLastMessageTurn();
            //get tile information
            Tile tempTile;
            for (int i = 0; i < decodedMessage.getNumTiles(); i++){
                //move to map
                tempTile = decodedMessage.getTile(i);
                System.out.println("Tile location ( " + tempTile.getX() + " , " + tempTile.getY() + " )");
                System.out.println("Location Data - " + tempTile.getLocationType());
                System.out.println("Soup Amt - " + tempTile.getSoupAmt());
            }
        }
    }

    /**
     * Finds a safe location for the location of a building
     * @return
     */
    public MapLocation findSafeBuildLocation(boolean school) throws GameActionException {
        MapLocation testLocation, centerOfMap;
        Direction rayDirection;
        Direction[] testDirections;
        int currentElevation = rc.senseElevation(rc.getLocation());
        boolean isWithinSensor;
        boolean complete = false;
        //System.out.println("Finding safe location for building...");
        centerOfMap = findCenterOfMap(); //give Me CEntr
        testLocation = rc.getLocation();
        rayDirection = testLocation.directionTo(centerOfMap); //get the direction the ray will be cast
        testDirections = getTestDirections(rayDirection);

        //System.out.println("Beginning scan in direction " + rayDirection.toString());

        testLocation = testLocation.add(rayDirection);
        testLocation = testLocation.add(rayDirection);

        //cast the ray and test locations along its path
        while(complete) {
            //move test location along path of ray
            System.out.println("Moving scan out another layer");
            testLocation = testLocation.add(rayDirection);

            for (int i = 0; i < 4; i++){
                if ((i != 2)){
                    System.out.println("Current scan location - " + testLocation.x + " , " + testLocation.y);
                    if (rc.canSenseLocation(testLocation) && rc.onTheMap(testLocation)){
                        if(testBuildLocation(testLocation, currentElevation, school)){
                            complete = true;
                            break;
                        }
                    }else{
                        System.out.println("Tried to scan off map or out of sensor range, changing ray direciton...");
                        //out of sensor radius OR is off map -- change direction
                        testLocation = rc.getLocation();
                        rayDirection.rotateRight();
                        testLocation = testLocation.add(rayDirection);
                        testLocation = testLocation.add(rayDirection);
                    }
                }
                testLocation = testLocation.add(testDirections[i]);
            }
        }
        return testLocation;
    }

    private boolean testBuildLocation(MapLocation testLocation, int currentElevation, boolean school) throws GameActionException {

        int elevation = rc.senseElevation(testLocation);
        if ((elevation >= currentElevation - 3) && (elevation <= currentElevation + 3) && !rc.senseFlooding(testLocation)){
            if(map.locationClear(testLocation)){
                if (school){
                    return testBuildLocationSurroundings(testLocation, currentElevation);
                }else{
                    return true;
                }
            }
        }
        return false;
    }


    private boolean testBuildLocationSurroundings(MapLocation testLocation, int currentElevation) throws GameActionException{
        boolean complete = false;
        boolean valid = true;
        int loopCounter = 0;
        Direction[] directions = {WEST, WEST, SOUTH, SOUTH, EAST, EAST, NORTH};
        MapLocation currentLocation = testLocation.add(NORTHEAST);
        int elevation = rc.senseElevation(testLocation);

        while (!complete){
            //test target
            //ensure space is clear
            if (map.locationClear(currentLocation)){
                elevation = rc.senseElevation(currentLocation); //get the location of the target space
                if ((elevation >= currentElevation - 3) && (elevation <= currentElevation + 3)){
                    //middle is clear -- run further tests on surroundings
                    valid = true;
                }else{
                    valid = false;
                    complete = true;
                }
            }else{
                //location is not clear
                valid = false;
                complete = true;
            }

            //move the location
            if (loopCounter < 7){
                currentLocation = currentLocation.add(directions[loopCounter]);
                loopCounter++;
            }else{
                //completed spaces -- location is valid for construction
                valid = true;
                complete = true;
            }
        }

        return valid;
        }

    private Direction[] getTestDirections(Direction rayDirection){
        Direction[] testDirections = new Direction[4];
        switch(rayDirection) {
            case NORTH:
                testDirections[0] = EAST;
                testDirections[1] = WEST;
                testDirections[2] = WEST;
                testDirections[3] = EAST;
                break;
            case NORTHEAST:
                testDirections[0] = NORTHWEST;
                testDirections[1] = SOUTHEAST;
                testDirections[2] = SOUTHEAST;
                testDirections[3] = NORTHWEST;
                break;
            case SOUTHEAST:
                testDirections[0] = NORTHWEST;
                testDirections[1] = SOUTHEAST;
                testDirections[2] = SOUTHEAST;
                testDirections[3] = NORTHWEST;
                break;
            case SOUTH:
                testDirections[0] = EAST;
                testDirections[1] = WEST;
                testDirections[2] = WEST;
                testDirections[3] = EAST;
                break;
            case SOUTHWEST:
                testDirections[0] = NORTHWEST;
                testDirections[1] = SOUTHEAST;
                testDirections[2] = SOUTHEAST;
                testDirections[3] = NORTHWEST;
                break;
            case NORTHWEST:
                testDirections[0] = NORTHEAST;
                testDirections[1] = SOUTHWEST;
                testDirections[2] = SOUTHWEST;
                testDirections[3] = NORTHEAST;
                break;
            default:
                testDirections[0] = NORTH;
                testDirections[1] = SOUTH;
                testDirections[2] = SOUTH;
                testDirections[3] = NORTH;
                break;
        }
        return testDirections;
    }
}

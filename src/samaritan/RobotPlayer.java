package samaritan;
import battlecode.common.*;

import java.lang.reflect.MalformedParameterizedTypeException;
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

    //Commands
    static char GATHER_SOUP = 'S';
    static char MAKE_DSCHOOL = 'D';
    static char MAKE_FCENTER = 'F';
    static char CEXPLORE = 'E';

    static int minersNeeded = 2;
    static int landscapersNeeded = 8;
    static int dronesNeeded = 0;
    static int transactionPrice = 4;
    static int buildingSpace = 3;
    static int explorersNeeded = 1;

    static int wallBuildingTurn = 300;

    static int thisRound;

    //Miner variables
    static MapLocation motherRefinery;
    static MapLocation preferredDeposit;
    static MapLocation perch;
    static MapLocation buildDestination = null;
    static MapLocation motherBuilding;
    static MapLocation explorationDestination;
    static Direction buildDirection;

    static ArrayList<MapLocation> stink = new ArrayList<>();
    static int stinkLength = 1;

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
                while(turnCount > 10 && rc.getCooldownTurns() >= 1) { //If the robot wont be able to move anyway, just skip the turn
                    //System.out.println("Pollution too high, skipping turn: " + rc.getRoundNum());
                    Clock.yield();
                }
                thisRound = rc.getRoundNum();
                turnCount += 1;
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.

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
                if(rc.getRoundNum() != thisRound) { //Checks is more than 1 round was needed to run this turn
                    System.out.println("Bytecode Limit Exceeded - Round Started: " + thisRound + ". Round Finished: " + rc.getRoundNum());
                    //if(turnCount <= 10)
                        //System.out.print(" On Startup");
                }
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
            scanArea(true, false, true);
            publishHQLocation();
            //totalSoupNearby = map.totalSoup(rc.getLocation(), rc.getCurrentSensorRadiusSquared());
            goal = BUILD_MINER;
        }

        if(goal == BUILD_MINER) {
            closestSoup = map.nextSoup();
            Direction dir = rc.getLocation().directionTo(toMapLocation(closestSoup));
            if(rc.getTeamSoup() >= RobotType.MINER.cost + transactionPrice) {
                if(forceBuild(RobotType.MINER, dir)) {
                    commandMiner(GATHER_SOUP);
                    minersNeeded--;
                }
            }
            if(minersNeeded <= 0)
                goal = BUILD_BUILDER;
        }

        else if(goal == BUILD_BUILDER) {
            closestSoup = map.nextSoup();
            Direction dir = rc.getLocation().directionTo(toMapLocation(closestSoup)).opposite();
            if(rc.getTeamSoup() >= RobotType.MINER.cost + transactionPrice) {
                if(forceBuild(RobotType.MINER, dir)) {
                    commandMiner(MAKE_FCENTER);
                    goal = BUILD_EXPLORERS;
                }
            }

        } else if(goal == BUILD_EXPLORERS) {
            Direction dir = rc.getLocation().directionTo(findCenterOfMap());
            if(rc.getTeamSoup() >= RobotType.MINER.cost + transactionPrice) {
                if(forceBuild(RobotType.MINER, dir)) {
                    commandMiner(CEXPLORE);
                    explorersNeeded--;
                }
            }
            if(explorersNeeded <= 0) {
                goal = NONE;
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
            scanArea(true, true, true);
            findHQ();
            motherRefinery = hqLocation;
            preferredDeposit = toMapLocation(map.nextSoup());

        } else {
            scanArea(false, false, true); //Scan area before turn

        } if(goal == STANDBY) {


        } if(goal == TRAVEL_TO_SOUP) {
            if(rc.getLocation().equals(preferredDeposit)) {
                goal = MINE;
            } else if(rc.getLocation().distanceSquaredTo(preferredDeposit) <= 2 && !isPassible(preferredDeposit)) {
                goal = MINE;
            } else {
                Direction direction = rc.getLocation().directionTo(preferredDeposit);
                forceMove(direction);
            }

        } if(goal == MINE) {
            if(rc.senseSoup(preferredDeposit) <= 0) { //If soup deposit ran out
                map.removeSoup(preferredDeposit);
                //Announce the depletion of soup
                goal = TRAVEL_TO_SOUP;
                updateSoupDeposit();
                if(goal == TRAVEL_TO_SOUP)
                    forceMove(rc.getLocation().directionTo(preferredDeposit));
            }
            else if(rc.getSoupCarrying() >= RobotType.MINER.soupLimit - 2) { //If robot is full
                goal = DEPOSIT;
            }
            else {
                tryMine(rc.getLocation().directionTo(preferredDeposit));
            }

        } if(goal == DEPOSIT) {
            RobotInfo[] robots = rc.senseNearbyRobots(2, myTeam);
            for(int i = 0; i < robots.length; i++) {
                if(robots[i].getType() == RobotType.REFINERY || robots[i].getType() == RobotType.HQ) {
                    if(tryRefine(rc.getLocation().directionTo(robots[i].getLocation())))
                        goal = TRAVEL_TO_SOUP;
                    break;
                }
            }
            if(goal == DEPOSIT) {
                forceMove(rc.getLocation().directionTo(motherRefinery));
            }

        } if(goal == EXPLORE) {
            rc.disintegrate();

        } if(goal == BUILD_VAPORATOR) {
            forceBuild(RobotType.VAPORATOR, randomDirection());
            goal = EXPLORE;

        } if(goal == BUILD_DESIGN_SCHOOL) {

            if(!rc.getLocation().equals(buildDestination)) {
                forceMove(rc.getLocation().directionTo(buildDestination));
            } else {
                if(forceBuild(RobotType.DESIGN_SCHOOL, hqLocation.directionTo(buildDestination)))
                    goal = EXPLORE;
            }

        } if(goal == BUILD_FULFILLMENT_CENTER) {
            if(buildDestination == null) {
                buildDestination = fulfillmentCenterSpot();
            }

            if(!rc.getLocation().equals(buildDestination)) {
                forceMove(rc.getLocation().directionTo(buildDestination));
            } else {
                if(forceBuild(RobotType.FULFILLMENT_CENTER, hqLocation.directionTo(buildDestination))) {
                    goal = BUILD_DESIGN_SCHOOL;
                    buildDestination = designSchoolSpot();
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
        if(goal == STANDBY) {
            findHQ();
            buildDirection = rc.getLocation().directionTo(hqLocation);
            goal = BUILD_WALL_BUILDERS;

        } if(goal == BUILD_WALL_BUILDERS) {
            if(rc.getRoundNum() > wallBuildingTurn && landscapersNeeded > 0) {
                if(tryBuild(RobotType.LANDSCAPER, buildDirection)) {
                    buildDirection = buildDirection.rotateLeft();
                    landscapersNeeded--;
                }
            }
        }
    }

    static void runFulfillmentCenter() throws GameActionException {
        while(!rc.isReady()) Clock.yield();
        if(dronesNeeded > 0) {
            Direction dir = rc.getLocation().directionTo(findCenterOfMap());
            if(rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost + transactionPrice) {
                if(forceBuild(RobotType.DELIVERY_DRONE, dir))
                    commandMiner(CEXPLORE);
                dronesNeeded--;
            }
        }
    }

    static void runLandscaper() throws GameActionException {
        if(goal == STARTUP) {
            findHQ();
            perch = getPerch();
            goal = FIND_PERCH;

        } if(goal == FIND_PERCH) {
            MapLocation startingSpot = rc.getLocation();
            forceMove(startingSpot.directionTo(perch));
            addStink(startingSpot);

            if (rc.getLocation().equals(perch)) {
                goal = BUILD_DEFENSIVE_WALL;
            }

        } else if(goal == BUILD_DEFENSIVE_WALL) {
            if (rc.getDirtCarrying() >= 25) {
                tryDump(CENTER);
            } else {
                tryDig(hqLocation.directionTo(rc.getLocation()));
            }

        }
    }

    static void runDeliveryDrone() throws GameActionException {

        if(goal == STARTUP) {

            System.out.println("Drone Calibrating" + " TCount: " + rc.getRoundNum());
            int temp = findPurpose(); //Get its command form the HQ
            if (temp != -1)
                goal = temp;
            scanArea(false, true, true);
            findHQ();
            findMotherBuilding();
            map.initFog(hqLocation);
            explorationDestination = map.getFirstFog(rc.getLocation());

        } if(goal == EXPLORE) {
            if(rc.getLocation().equals(explorationDestination)) {
                scanArea(false, true, true);
                map.disperseFog(explorationDestination);
                explorationDestination = map.getClosestFog(rc.getLocation());
                if(explorationDestination == null)
                    goal = STANDBY;
            } else {
                forceMove(rc.getLocation().directionTo(explorationDestination));
            }
        }
    }

    static void runNetGun() throws GameActionException {

    }

    /**
     * method to keep track of stink to help with path finding
     * @param location
     */
    public static void addStink(MapLocation location) {
        stink.add(location);
        if(stink.size() > stinkLength)
            stink.remove(0);
    }

    public static void clearStink() {
        stink.clear();
    }

    public static boolean notStink(MapLocation location) {
        for(int i = 0; i < stink.size(); i++) {
            if(stink.get(i).equals(location))
                return false;
        }
        return true;
    }

    /**
     * Temporary method
     * Used for the HQ to tell a miner it just created what its purpose is
     * @param command
     */
    static void commandMiner(char command) throws GameActionException {
        if(rc.getTeamSoup() < transactionPrice) {
            System.out.println("ERR - commandMiner: called with not enough soup");
            return;
        }
        int[] message = new int[7];
        message[0] = command;
        rc.submitTransaction(message, transactionPrice);
        //System.out.println("Submitting message on turn " + rc.getRoundNum());
    }

    /**
     * Temporary method
     * Publishes location of hq at start of game
     * @throws GameActionException
     */
    static void publishHQLocation() throws GameActionException {
        int x = rc.getLocation().x;
        int y = rc.getLocation().y;
        int[] message = new int[7];
        message[0] = x;
        message[1] = y;
        rc.submitTransaction(message, transactionPrice);
    }

    /**
     * Used for the miner to determine its first goal
     * @return
     * @throws GameActionException
     */
    static int findPurpose() throws GameActionException {
        //debugBlockchain(rc.getRoundNum() - 1);
        Transaction[] t = rc.getBlock(birthday);
        int[] message = t[0].getMessage();
        System.out.println("Message: " + (char)message[0]);
        if(message[0] == GATHER_SOUP)
            return TRAVEL_TO_SOUP;
        if(message[0] == MAKE_DSCHOOL)
            return BUILD_DESIGN_SCHOOL;
        if(message[0] == MAKE_FCENTER)
            return BUILD_FULFILLMENT_CENTER;
        if(message[0] == CEXPLORE)
            return EXPLORE;
        return -1;
    }

    static void initializeRobot() throws GameActionException {
        //System.out.println("I'm a " + rc.getType() + ", my ID is " + rc.getID() + " and I just got created!"  + " TCount: " + rc.getRoundNum());
        char internRobotId;
        internRobotId = convertRobotTypeToChar(rc.getType());

        turnCount = 0;
        birthday = rc.getRoundNum() - 1;

        map = new LocalMap(rc.getLocation(), internRobotId, rc.getMapWidth(), rc.getMapHeight()); //create the local map
        myTeam = rc.getTeam();
        enemyTeam = rc.getTeam().opponent();
        goal = STARTUP;
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
        /*RobotInfo[] robots = rc.senseNearbyRobots(20, myTeam);
        for(int i = 0; i < robots.length; i++) {
            if(robots[i].getType() == RobotType.HQ) {
                hqLocation = robots[i].getLocation();
                break;
            }
        }
        return;*/

        Transaction[] t = rc.getBlock(1);
        int[] message = t[0].getMessage();
        hqLocation = new MapLocation(message[0], message[1]);
        //System.out.println("Location: " + message[0] + ", " + message[1]);
    }

    /**
     * Finds a new closest deposit one its preferredDeposit runs out
     */
    static void updateSoupDeposit() {
        //System.out.println("Changing preferredSDeposit");
        preferredDeposit = toMapLocation(map.nextSoup());
        if(preferredDeposit == null) {
            System.out.println("Self-Destruc");
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

        //int elevation = rc.senseElevation(location); //get elevation data
        //map.recordElevation(location, elevation); //store elevation data
    }

    public static void findSoup() throws GameActionException {
        MapLocation[] soups = rc.senseNearbySoup();
        for(int i = 0; i < soups.length; i++) {
            map.addSoup(soups[i], rc.senseSoup(soups[i]));
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
}

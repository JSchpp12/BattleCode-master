package samaritan;
import battlecode.common.*;

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
    static Team enemy;
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
    static int SCOOP = 9;
    static int PLOP = 10;
    static int STANDBY = 11;
    static int DSCHOOL = 12;

    //Commands
    static char GATHER_SOUP = 'S';

    static int minersNeeded = 2;


    //Miner variables
    static MapLocation motherRefinery;
    static MapLocation preferredDeposit;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        RobotPlayer.rc = rc;
        initializeRobot();

        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
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
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
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
            findSoup();
            //totalSoupNearby = map.totalSoup(rc.getLocation(), rc.getCurrentSensorRadiusSquared());
            System.out.println("Turns: " + rc.getRoundNum());
            closestSoup = map.closestSoup(rc.getLocation());

            System.out.println("Closest Soup: " + closestSoup.getX() + ", " + closestSoup.getY() + " TCount: " + rc.getRoundNum());
            Direction dir = rc.getLocation().directionTo(toMapLocation(closestSoup));
            //System.out.println("About to build miner"  + " TCount: " + rc.getRoundNum());
            forceBuild(RobotType.MINER, dir);
            commandMiner(GATHER_SOUP);
            minersNeeded--;
            goal = BUILD_MINER;
        }

        else if(goal == BUILD_MINER) {
            closestSoup = map.closestSoup(rc.getLocation());
            Direction dir = rc.getLocation().directionTo(toMapLocation(closestSoup));
            forceBuild(RobotType.MINER, dir);
            commandMiner(GATHER_SOUP);
            minersNeeded--;
            if(minersNeeded <= 0)
                goal = DSCHOOL;
        }

        else if(goal == DSCHOOL) {

        }
    }

    static void runMiner() throws GameActionException {

        if(goal == STARTUP) {

            System.out.println("Miner Calibrating" + " TCount: " + rc.getRoundNum());
            int temp = findPurpose(); //Get its command form the HQ
            if(temp != -1)
                goal = temp;
            else
                goal = MINE;
            findHQ();
            motherRefinery = hqLocation;
            findSoup();
            preferredDeposit = toMapLocation(map.closestSoup(rc.getLocation()));

        } if(goal == STANDBY) {


        } if(goal == TRAVEL_TO_SOUP) {
            //System.out.println("My goal is to travel to soup");
            if(rc.getLocation().equals(preferredDeposit)) {
                //System.out.println("Changing goal to MINE");
                goal = MINE;
            } else if(rc.getLocation().distanceSquaredTo(preferredDeposit) <= 2 && rc.isLocationOccupied(preferredDeposit)) {
                goal = MINE;
            }
            else {
                Direction direction = rc.getLocation().directionTo(preferredDeposit);
                tryMove(direction);
            }

        } if(goal == MINE) {
            if(rc.senseSoup(preferredDeposit) <= 0) {
                //map.clearLocation(rc.getLocation().x, rc.getLocation().y);
                //Announce the depletion of soup
                goal = TRAVEL_TO_SOUP;
                updateSoupDeposit();
                tryMove(rc.getLocation().directionTo(preferredDeposit));
                //System.out.println("Changing goal to DEPOSIT");
            }
            else if(rc.getSoupCarrying() >= RobotType.MINER.soupLimit - .5*(double)GameConstants.SOUP_MINING_RATE) {
                goal = DEPOSIT;
                //System.out.println("Changing goal to DEPOSIT");
            }
            else {
                tryMine(rc.getLocation().directionTo(preferredDeposit));
            }

        } if(goal == DEPOSIT) {
            RobotInfo[] robots = rc.senseNearbyRobots(2, myTeam);
            for(int i = 0; i < robots.length; i++) {
                if(robots[i].getType() == RobotType.REFINERY || robots[i].getType() == RobotType.HQ) {
                    tryRefine(rc.getLocation().directionTo(robots[i].getLocation()));
                    goal = TRAVEL_TO_SOUP;
                    //System.out.println("Changing goal to TRAVEL_TO_SOUP");
                    break;
                }
            }
            if(goal == DEPOSIT) {
                tryMove(rc.getLocation().directionTo(motherRefinery));
            }

        } if(goal == EXPLORE) {


        } if(goal == BUILD_VAPORATOR) {
            forceBuild(RobotType.VAPORATOR, randomDirection());
            goal = EXPLORE;

        } if(goal == BUILD_DESIGN_SCHOOL) {
            if(turnCount >= 12) {
                System.out.println("Attempting to BUild design school");
                if(tryBuild(RobotType.DESIGN_SCHOOL, rc.getLocation().directionTo(hqLocation).opposite()))
                    goal = EXPLORE;
            } else {
                tryMove(rc.getLocation().directionTo(hqLocation).opposite());
            }
        }
    }

    static void runRefinery() throws GameActionException {
        // System.out.println("Pollution: " + rc.sensePollution(rc.getLocation()));
    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {
        tryBuild(RobotType.LANDSCAPER, randomDirection());
    }

    static void runFulfillmentCenter() throws GameActionException {
        /*for (Direction dir : directions)
            tryBuild(RobotType.DELIVERY_DRONE, dir);*/
    }

    static void runLandscaper() throws GameActionException {
        if(goal == STARTUP) {
            findHQ();
            goal = SCOOP;

        } if(goal == SCOOP) {
            System.out.println("SCOOP");
            if(rc.getLocation().distanceSquaredTo(hqLocation) < 4) {
                tryMove(rc.getLocation().directionTo(hqLocation).opposite());
            } else {
                rc.digDirt(randomDirection());
            }
            if(rc.getDirtCarrying() >= 25) {
                goal = PLOP;
            }
        } else if(goal == PLOP) {

            System.out.println("PLOP");
            if(rc.getLocation().distanceSquaredTo(hqLocation) <= 2) {
                tryMove(rc.getLocation().directionTo(hqLocation).opposite());
            } else if(rc.getLocation().distanceSquaredTo(hqLocation) > 8) {
                tryMove(rc.getLocation().directionTo(hqLocation));
            } else {
                rc.depositDirt(rc.getLocation().directionTo(hqLocation));
            }
            if(rc.getDirtCarrying() <= 0) {
                goal = SCOOP;
            }

        }
    }

    static void runDeliveryDrone() throws GameActionException {
        /*
        if (!rc.isCurrentlyHoldingUnit()) {
            // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);

            if (robots.length > 0) {
                // Pick up a first robot within range
                rc.pickUpUnit(robots[0].getID());
                System.out.println("I picked up " + robots[0].getID() + "!");
            }
        } else {
            // No close robots, so search for robots within sight radius
            tryMove(randomDirection());
        }*/
    }

    static void runNetGun() throws GameActionException {

    }

    /**
     * Temporary method
     * Used for the HQ to tell a miner it just created what its purpose is
     * @param command
     */
    static void commandMiner(char command) throws GameActionException {
        int[] message = new int[7];
        message[0] = command;
        rc.submitTransaction(message, 4);
        System.out.println("Submitting message on turn " + rc.getRoundNum());
    }

    static int findPurpose() throws GameActionException {
        System.out.println("Getting message on turn " + rc.getRoundNum());
        debugBlockchain(rc.getRoundNum() - 1);
        Transaction[] t = rc.getBlock(birthday);
        int[] message = t[0].getMessage();
        System.out.println("Message: " + (char)message[0]);
        if(message[0] == GATHER_SOUP)
            return TRAVEL_TO_SOUP;
        return -1;
    }

    static void initializeRobot() throws GameActionException {
        System.out.println("I'm a " + rc.getType() + ", my ID is " + rc.getID() + " and I just got created!"  + " TCount: " + rc.getRoundNum());
        char internRobotId;
        internRobotId = convertRobotTypeToChar(rc.getType());

        turnCount = 0;
        birthday = rc.getRoundNum() - 1;

        map = new LocalMap(rc.getLocation(), internRobotId, rc.getMapWidth(), rc.getMapHeight()); //create the local map
        myTeam = rc.getTeam();
        enemy = rc.getTeam().opponent();
        goal = STARTUP;
    }

    /**
     * Saves the location of the HQ
     */
    static void findHQ() {
        RobotInfo[] robots = rc.senseNearbyRobots(20, myTeam);
        for(int i = 0; i < robots.length; i++) {
            if(robots[i].getType() == RobotType.HQ) {
                hqLocation = robots[i].getLocation();
                break;
            }
        }
        return;
    }

    /**
     * Finds a new closest deposit one its preferredDeposit runs out
     */
    static void updateSoupDeposit() {
        System.out.println("Changing preferredSDeposit");
        preferredDeposit = toMapLocation(map.closestSoup(rc.getLocation()));
        if(preferredDeposit == null) {
            goal = EXPLORE;
        }
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

    static boolean tryMove() throws GameActionException {
        for (Direction dir : directions)
            if (tryMove(dir))
                return true;
        return false;
        // MapLocation loc = rc.getLocation();
        // if (loc.x < 10 && loc.x < loc.y)
        //     return tryMove(Direction.EAST);
        // else if (loc.x < 10)
        //     return tryMove(Direction.SOUTH);
        // else if (loc.x > loc.y)
        //     return tryMove(Direction.WEST);
        // else
        //     return tryMove(Direction.NORTH);
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
            findSoup();
            return true;
        } else return false;
    }

    /**
     * Iterates through every direction closest to preferred direction to ensure a successful build
     */
    static void forceBuild(RobotType type, Direction dir) throws GameActionException {
        if(!rc.isReady()) return;

        MapLocation location = rc.getLocation().add(dir);
        if(rc.canBuildRobot(type, dir) && !rc.senseFlooding(location)) {
            rc.buildRobot(type, dir);
            return;
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
                return;
            }

            direction = dir;
            for(int j = 0; j <= i; j++) {
                direction = direction.rotateRight();
            }
            location = rc.getLocation().add(direction);
            if(rc.canBuildRobot(type, direction) && !rc.senseFlooding(location)) {
                rc.buildRobot(type, direction);
                return;
            }
        }
    }

    static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to mine soup in a given direction.
     *
     * @param dir The intended direction of mining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMineSoup(dir)) {
            rc.mineSoup(dir);
            return true;
        } else return false;
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
        } else return false;
    }


    static void tryBlockchain() throws GameActionException {
        if (turnCount < 3) {
            int[] message = new int[7];
            for (int i = 0; i < 7; i++) {
                message[i] = 123;
            }
            if (rc.canSubmitTransaction(message, 10))
                rc.submitTransaction(message, 10);
        }
        // System.out.println(rc.getRoundMessages(turnCount-1));
    }


    /**
     * search every space within sensor radius for soup, will update local map with locations
     * Also finds elevation of every tile within radius
     */
    static void findSoup() throws GameActionException {
        //System.out.println("Initializing Soup Scan...");
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
                if(rc.senseSoup(scanLocation) > 0){
                    //System.out.println("Found Soup");
                    map.addSoup(scanLocation, rc.senseSoup(scanLocation));
                }
                elevation = rc.senseElevation(scanLocation); //get elevation data
                map.recordElevation(scanLocation, elevation); //store elevation data

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

    public void printLocalMap(){
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
}

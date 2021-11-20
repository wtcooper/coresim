package coresim;

/*	Overview of Larvae Class Steps:
 * 
 *		1) loop through all colonies, determine if a colony will planulate, and if so, 
 *			calculate total number of larvae released
 * 		2) introduce larvae individually, have them "arrive" at a random
 * 			distance from adult based on dispersal distance (DD) parameter, pull 
 * 			from negative decreasing exponential distribution
 * 		3) have them search the bottom for appropriate location based on selectivity
 * 			Here, will need to define selectivity for seperate individuals
 * 		4) have them settle on bottom, and experience first-month mortality based
 * 			on where they settle; have this variable based on "unknown" parameter
 * 			offset weights for different substrates and orientation; i.e., parameters
 * 			which determine if an effect is present
 * 		* by experiencing mortality at end of method, will eliminate the need
 * 			to create unrealistically high number of agents (e.g., 10's of millions)
 * 			If mortality is 99% in first month, then number of agents will be on the order
 * 			of 100,000's
 * 		5) if survive, add a "recruit" agent
 */


import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;

import cern.jet.random.Beta;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;


public class Recruiter {

	HashMap<Point, ArrayList<Recruit>> recruitMap = new HashMap<Point, ArrayList<Recruit>>(); 
	ArrayList<Coral> coralList; 
	ArrayList<Recruit> recruitList = new ArrayList<Recruit>(); 

	private double fecundSA = .7;  // set to 70%; description of number calculation below
	private TimeKeeper tk; 
	private Uniform uniform; 
	private Normal normal;
	private Beta betaDist;
	private double alpha, beta; 
	private double[] alphaArray = {1.1, 1.1, 1.1, 1.1}; 
	private double[] betaArray = {100, 35, 10, 2}; 
	private int dispersalDist; // value from 0-3 to set alphaArray and betaArray values;
	//this will be variable input from batch processing; will determine which alpha/beta combination

	private int gridWidth, gridHeight; 

	private int [] numExternalCorals; 
	private int[][] coralArray;
	//	private float[][][][] subCoverArray; 
	private float[][] subCoverArraySimple; 
	private float[][] connectivityMatrix; 

	private float[] numExternalLarvae = new float[16]; // this array will hold the total number for 16 focal site cells (8x8m)from external supply

	private int subType;  // here, order is bare-cca-other-turf: 0-1-2-3
	private int reefNumber;
	private int month; 
	private int coralX, coralY;
	private int age, ageTemp;
	private int newX, newY; 
	private int larvaeX, larvaeY; 
	private double spatSize; 
	private double coralSurfArea; 
	private int numLarvae;
	private double probOfPlanulate, numLarvaeTemp; 
	private double dispersalDistance;
	private double settleMortality; 
	private int larvaeMortality; 
	private double larvaeMortalityDouble; 

	// probabilities and std dev of colonies releasing planulating in april, may, and june, from Moulding 2007 dissertation Table 1 of Ch6, pg 105-106
	private double probApril=68.8888889, probMay=82.6984127, probJune=61.2345679;
	private double probAprilStd=30.8485654, probMayStd=23.9409073, probJuneStd=29.3541323;
	private double numLarvaePerCM = 7.572923077, numLarvaePerCMStd = 8.573416474;

	private float[] survRatesArray;  
	private float[] survPropArray;  
	private float[] survRatesHillArray;  
	//	private float[][] survSDArray;  
	//private float[][] growRatesArray;  
	//private float[][] growSDArray;  


	ArrayList<Point> reefCoords; 
	//ArrayList<Point> coopersReefCoords;
	//ArrayList<Point> megansReefCoords;
	//ArrayList<Point> mk14ReefCoords;
	//ArrayList<Point> mk16ReefCoords;


	/* Here, define a preference parameter which is the likelihood that they'll settle on a particular substrate type
	 * This is calculated straight from field data where it is proportion settling on a particular substrate type divided
	 * 	by the proportion of that substrate type.  This value is then standardized as a proportion of all other substrate types
	 *	Use Erica' data processing for this of exact settlement locations
	 */

	private double baseProb1, baseProb2, baseProb3; 
	private double prob1, prob2, prob3; 

	private double autoCorr; // steps: 
	private double sumProb; 

	// substrate preference values from settlement experiment; see JuneSettlementCues08.xls for calculations
	private double [] substratePref= {0.055982143, 0.569444444}; // element 0 = exposed, element 1 = cryptic
	private double desperateLarvaOffset; 
	private int numMoves; 
	private double [] survSubOffset;  
	private int survType;  
	private double survSiteOffset;  
	private int survOffset; 
	private double survSD; 

	private double survRate; 
	//private double [] growSiteOffset;  

	//private int[] disturbTimes = new int[126]; 

	//Testing params
	//int totalCorals = 0; 
	//int totalNumLarvae = 0; 
	//int totalRecruits=0; 

	//int testCounter = 0; 

	//	private PrintWriter outFile=null;	


	Recruiter (TimeKeeper tk) {
		this.tk = tk; 
		gridWidth = RecruitmentMain.getGridWidth();
		gridHeight = RecruitmentMain.getGridHeight(); 
		uniform = tk.getUniform(); 
		normal = tk.getNormal();
		betaDist = tk.getBetaDist();

		// Parameters
		dispersalDist = tk.getDispersalDist();
		survType = tk.getSurvType(); 
		survSubOffset = tk.getSubSurvOffset();
		survSiteOffset = tk.getSiteSurvOffset();
		survSD = tk.getSurvSD();
		desperateLarvaOffset = tk.getDesperateLarvaOffset(); 
		//autoCorr = tk.getAutoCorr();
		settleMortality = tk.getSettleMortality(); 

		subCoverArraySimple = RecruitmentMain.getSubCoverArraySimple(); 
		survRatesArray = RecruitmentMain.getSurvRatesArray();
		survRatesHillArray = RecruitmentMain.getSurvRatesHillArray(); 
		survPropArray = RecruitmentMain.getSurvPropArray();



		//		subRepresent = tk.getSubRepresent(); 
		//growSiteOffset = tk.getSiteGrowOffset();

		larvaeMortality = tk.getLarvaeMortality(); 
		larvaeMortalityDouble = 1; 
		if (larvaeMortality == 0) larvaeMortalityDouble = 0.01; 
		else if (larvaeMortality == 1) larvaeMortalityDouble = 0.05; 
		else if (larvaeMortality == 2) larvaeMortalityDouble = 0.1; 
		else if (larvaeMortality == 3) larvaeMortalityDouble = 0.33; 
		else if (larvaeMortality == 4) larvaeMortalityDouble = 0.66; 
		else larvaeMortalityDouble = 1; 

		if (dispersalDist != 4){
			alpha = alphaArray[dispersalDist];
			beta = betaArray[dispersalDist];

			if (dispersalDist == 0) connectivityMatrix = RecruitmentMain.getConnectivityMatrix0();
			else if (dispersalDist == 1) connectivityMatrix = RecruitmentMain.getConnectivityMatrix1();
			else if (dispersalDist == 2) connectivityMatrix = RecruitmentMain.getConnectivityMatrix2();
			else if (dispersalDist == 3) connectivityMatrix = RecruitmentMain.getConnectivityMatrix3();
		}
	}


	/**
	 * This method sets the parameters at the initiation of another site in TimeKeeper setNextSupplyCell() method
	 */
	void setRecruitParams() {
		reefNumber = tk.getReefNumber();

		//**AUTOCORR data estimates from SubAutoCorrOptimizer.java
		//reefNum: 	0	avgAutoCorr:	0.7995321049138682	avgAutoCorrSD:	0.01702940357731214
		//reefNum: 	1	avgAutoCorr:	0.12193294380254027	avgAutoCorrSD:	0.28537464557013514
		//reefNum: 	2	avgAutoCorr:	0.8881563342701838	avgAutoCorrSD:	0.007065377521390045
		//reefNum: 	3	avgAutoCorr:	0.8745113715686903	avgAutoCorrSD:	0.008114847635394263
		
		if (reefNumber == 0) autoCorr = 0.7995321049138682; 
		else if (reefNumber == 1) autoCorr = 0.12193294380254027;
		else if (reefNumber == 2) autoCorr = 0.8881563342701838;
		else if (reefNumber == 3) autoCorr = 0.8745113715686903;


		if (reefNumber == 0) reefCoords = RecruitmentMain.getCoopersReefCoords();
		else if (reefNumber == 1) reefCoords = RecruitmentMain.getMegansReefCoords();
		else if (reefNumber == 2) reefCoords = RecruitmentMain.getMk14ReefCoords();
		else if (reefNumber == 3) reefCoords = RecruitmentMain.getMk16ReefCoords();

		numExternalCorals = tk.getNumExternalCorals(); 
		coralArray = tk.getCoralArray();
		coralList = tk.getCoralList(); 

		recruitMap = new HashMap<Point, ArrayList<Recruit>>(); 
		recruitList = new ArrayList<Recruit>(); 		


		// set the disturb times for a site, where 33% chance that any given month will have a disturbance
		// therefore, is likely that at least one month each year of larval release will have a disturbance affecting the 
		// youngest individuals
		/*		for (int i = 0; i < 126; i++){
			if (uniform.nextDoubleFromTo(0, 1) < 0.33) {
				disturbTimes[i] = 1; 
			}
			else disturbTimes[i] = 0; 
		}
		 */		
	}


	/**
	 * This is the start of the larval settlement phase which loops through all of the adult corals and calls the planulate method
	 * if they are appropriate surface area
	 */
	void recruit() {

		month = tk.getStepCounter()%12;

		//totalNumLarvae = 0; 
		//totalRecruits =0;
		//totalCorals = 0; 
		//testCounter = 0; 

		for (Coral coral: coralList) {
			coralX = coral.getX();
			coralY = coral.getY();

			coralSurfArea = coral.getSurfaceArea(); 
			setLocalSupply();

			//totalCorals++; 
		}

		//		System.out.println("Number of local larvae: \t" + totalNumLarvae); 
		/*  Need to verify this is correct */

		// if the dispersal distance is = 4, then do random without any external supply
		if (dispersalDist != 4){
			setExternalSupply();
		}


		//System.out.println("Number of total corals: \t" + totalCorals);
		//System.out.println("Number of total larvae: \t" + totalNumLarvae);
		//System.out.println("Number of total recruits: \t" + totalRecruits);
		//System.out.println("Number of times move() step called in local supply: \t" + testCounter);

		//System.out.println(); 


	}



	/** This method determines if a coral will or will not planulate based on the month.  Here, probabilities of planulation 
	 * as a function of month are drawn from Alison Moulding's dissertation (2007) at University of Miami.  
	 * 
	 * If it does planulate, it will planulate a random number of larvae based on it's surface area and the number of larvae per cm as 
	 * measured by Moulding (2007), using normal distribution from the average number per cm and the std deviation
	 * 
	 * The method then loops through the total number of larvae per colony, sets the dispersal point of the larvae 
	 * by calling the setDispersalPoint(), and if the dispersal point if a reef habitat, the larvae will then move in the move() method
	 * 
	 * Note: if the larvae ends up on non-reef habitat, it is assummed lost and will not recruit
	 */

	void setLocalSupply() {

		if(month == 3) probOfPlanulate = normal.nextDouble(probApril, probAprilStd);
		if(month == 4) probOfPlanulate = normal.nextDouble(probMay, probMayStd);
		if(month == 5) probOfPlanulate = normal.nextDouble(probJune, probJuneStd);

		// if a colony has larvae that month, planulate larvae
		if(uniform.nextIntFromTo(0,100) < probOfPlanulate){
			// below, only consider 70% of colony fecund to account for margins without larvae; this produced numLarvae values which most closely matched
			// actual data collected on num larvae per colony in 2006 (unpublished data), when calculating using Moulding's fecundity data 
			numLarvaeTemp = normal.nextDouble(numLarvaePerCM, numLarvaePerCMStd)*(coralSurfArea*fecundSA*larvaeMortalityDouble);
			if (numLarvaeTemp < 0) numLarvaeTemp = 0; 
			numLarvae = (int) numLarvaeTemp; 

			//totalNumLarvae = totalNumLarvae + numLarvae; 


			int loopCounter1 = 0;

			while (loopCounter1 < numLarvae){

				if (dispersalDist != 4){

					setDispersalPoint();
					if((larvaeX >= 0) && (larvaeX < gridWidth) && (larvaeY >= 0) && (larvaeY < gridHeight) ){

						move(); 
						//testCounter++; 
					}
				}

				// if the dispersal distance = 4, is random distribution
				else if (dispersalDist == 4){
					larvaeX = uniform.nextIntFromTo(0, gridWidth);
					larvaeY = uniform.nextIntFromTo(0, gridHeight);
					
					move(); 
				}

					
				loopCounter1++;
			} // end of while loop to release larvae
		}
	}


	/** This method is where the larvae move across the surface in a random direction.  Larvae have 4 possible moves when following
	 * the surface: forward, backward, left, or right.  Note, they do not free swim in the water column once they arrive on the benthos
	 * 
	 */

	void move(){


		boolean notSettled = true;  // boolean flag to see if a larvae is still alive and capable of moving

		int tempX=0, tempY=0; 
		newX = larvaeX*10; 
		newY = larvaeY*10; 

		subType = uniform.nextIntFromTo(0, 2); 
		numMoves = 0; 

		while (notSettled){
			//System.out.println("Not settled while loop" );

			//***************do movement here*************
			int location = uniform.nextIntFromTo(1, 8);     // this chooses randomly one of the eight directions the bug can go

			switch (location) {

			case 1: 
				tempX = newX-1;
				tempY = newY;
				break;

			case 2: 
				tempX = newX-1;
				tempY = newY+1;
				break;

			case 3: 
				tempX = newX;
				tempY = newY+1;
				break;

			case 4: 
				tempX = newX+1;
				tempY = newY+1;
				break;

			case 5: 
				tempX = newX+1;
				tempY = newY;
				break;

			case 6: 
				tempX = newX+1;
				tempY = newY-1;
				break;

			case 7: 
				tempX = newX;
				tempY = newY-1;
				break;

			case 8: 
				tempX = newX-1;
				tempY = newY-1;
			default:
				break;
			} // end switch 

			newX = tempX; 
			newY = tempY; 
			larvaeX = getWrapAround((int) newX/10);
			larvaeY = getWrapAround((int) newY/10);

			// check to see if larvae will settle
			if (checkSettlement()){

				// if settle, set age to 0months, and set initial spatSize to .1244cm which was average size from 2008 field chips
				age = 0; 

				// set size to average larvae size (.106cm) with SD (.02cm)
				spatSize = normal.nextDouble(.1062525346, .0231413207); 
				if (spatSize < 0.05)	spatSize = 0.05; 


				// check to see if spat will survive until end of 10year simulation, and if so, have them grow
				// Note: if spat enter simulation during 10year run, will only survive and die until the set 10year end
				// therefore, will get a mix of sizes representing realistic size-frequency distribution
				if (checkPostSettlement()){

					// if spat survive until end of 10year run, add a Recruit to reef grid and set properties
					// Note: these recruits are simple data storage with no scheduled methods
					// they simply need a location so they can be "sampled" with quadrats during RecordPatterns class at end of 10year simulation run
					Point point = new Point(larvaeX,larvaeY);

					Recruit recruit = new Recruit(); 
					recruit.setX(larvaeX);
					recruit.setY(larvaeY);
					recruit.setDiameter(spatSize);
					recruit.setSubType(subType);

					// this maps out the recruit locations to recruitMap for finding them in PatternRecorder
					ArrayList<Recruit> list = (ArrayList<Recruit>) recruitMap.get(point);
					if (list == null) {
						list = new ArrayList<Recruit>();
						recruitMap.put(point, list);
					}
					list.add(recruit);
					/**/					
					recruitList.add(recruit); 


					//	totalRecruits++; 

				} // end of checkPostSettlement() if statement

				notSettled = false;

			} // end of checkSettlement() if statement

			else if(uniform.nextIntFromTo(0, 100) < (settleMortality*100)){ 
				// do out of 1000 because can be very low mortality (.5%)
				notSettled = false; 
			}

			numMoves++; 

		} // end of while loop "notSettled"
	}



	/**	This is the heart of the larvae class which checks the new location to see if it's appropriate for settlement, based on:
	 * 		1) Substrate
	 * 		2) Light Intensity
	 * 
	 * 	as per settlement cue lab experiments.  
	 * 
	 * 	First checks substrate as that is strongest factor.  Does 
	 */


	boolean checkSettlement(){
		boolean checkSettlement = false;

		double subProb; 


		// check to see if coral is at that location; won't settle on coral (could also do this below in if-else with other 4 subTypes, but did now to quicken )
		if (coralArray[larvaeX][larvaeY] != 4) {


			// this section below sets the substrate type.  If autoCorr = 0.5, this would be random distribution, based on the % cover
			// of each substrate type.  If autoCorr > 0.5, then the value will the likelihood that another one of that type of substrate
			// will be placed next to the last substrate as a larvae moves.  When a different type of substrate get's picked, then that 
			// substrate's probability will be set to the autoCorr value.  E.g., if 0.7, then is a 70% that the next substrate will be the 
			// same, but as soon as a different substrate get's picked, the new substrate will be set to 0.7

			baseProb1 = subCoverArraySimple[reefNumber][0];
			baseProb2 = subCoverArraySimple[reefNumber][1];
			baseProb3 = 1-baseProb1-baseProb2;

			/*				if ((autoCorr > 0.45) && (autoCorr < 0.55)){
					prob1=baseProb1;
					prob2=baseProb2;
					prob3=baseProb3;
					sumProb = prob1+prob2+prob3; 

				}
				else {
			 */				
			if (subType ==0){
				prob1 = autoCorr;
				prob2 = (1-autoCorr)*(baseProb2/(baseProb1+baseProb2+baseProb3)); 
				prob3 = (1-autoCorr)*(baseProb3/(baseProb1+baseProb2+baseProb3)); 
				sumProb = prob1+prob2+prob3; 
			}
			else if (subType ==1){
				prob2 = autoCorr;
				prob1 = (1-autoCorr)*(baseProb1/(baseProb1+baseProb2+baseProb3)); 
				prob3 = (1-autoCorr)*(baseProb3/(baseProb1+baseProb2+baseProb3)); 
				sumProb = prob1+prob2+prob3; 
			}
			else if (subType ==2){
				prob3 = autoCorr;
				prob1 = (1-autoCorr)*(baseProb1/(baseProb1+baseProb2+baseProb3)); 
				prob2 = (1-autoCorr)*(baseProb2/(baseProb1+baseProb2+baseProb3)); 
				sumProb = prob1+prob2+prob3; 
			}


			// Explore potential ways to make rugosity via probability draws
			subProb = uniform.nextDoubleFromTo(0, sumProb);

			if (subProb < prob1) subType = 0; // exposed
			else if (subProb < (prob1 + prob2))subType= 1; // cryptic
			else subType= 2; // other


			if (subType != 2){

				// if a uniformly-drawn random number is less than the substrate preference, then they will settle on location
				// here, factor in a desperate larvae function, where will decrease their specificity (i.e., increase liklihood
				// of settlement) with the more moves they make. 
				if(uniform.nextDoubleFromTo(0, 1) < (substratePref[subType]+desperateLarvaOffset*numMoves)){
					checkSettlement = true; 
				}
			}
		} // end of check if coral at position


		return checkSettlement;
	}



	/** This method determines if a larvae settler survives the first month of settlement based on survival rates and SD
	 * Returns boolean true if it survives, false otherwise
	 * 
	 * @return
	 */
	boolean checkPostSettlement() {
		int loopCounter = 0; 
		boolean checkPostSettlement = false;
		boolean continueLoop = true; 
		//double baseIndivVar = 0;  // the base value for individual variability in mean survival rate curve
		//double indivVar = 0; 
		double[] survRatesArrayAdjust = new double[16]; 

		
		/** may have messed up stoichasticity here -- only introduce variability in first month and not subsequent months??
		 * 
		 */

		if (survType == 0){
			survRatesArrayAdjust[0] = normal.nextDouble((survRatesArray[0] + survSubOffset[subType] + survSiteOffset), 0.03); 
			
			// probably want this:
			// fullSurvOffset = survRatesArray[0] - survRatesArrayAdjust[0]; 

			for (int i = 1; i<16; i++){
				
				// correct version:
				// survRatesArrayAdjust[i]	= survRatesArray[i] + (fullSurvOffset*survPropArray[i]);
				survRatesArrayAdjust[i]	= survRatesArray[i] + ((survSubOffset[subType]+survSiteOffset)*survPropArray[i]);  
			}

		}
		else if (survType == 1){
			survRatesArrayAdjust[0] = normal.nextDouble((survRatesHillArray[0] + survSubOffset[subType] + survSiteOffset), 0.03);

			for (int i = 1; i<16; i++){
				survRatesArrayAdjust[i]	= survRatesHillArray[i] + ((survSubOffset[subType]+survSiteOffset)*survPropArray[i]);  
			}

		}




		// here, set an adjusted survival rates array for each individual.  This in effect sets a variable distance that each 
		// spat is from the "mean value" (i.e., SurvRatesArray), based on the survival Offset parameters.  Here, the mean value is 0.10xx,
		// and the offset parameters are from -.04 -> +.04, so can add up to -0.08 -> +0.08.  Then, you adjust each age group survival
		// by the survPropArray, which is the proportional decrease in the distance away from the mean value, so as the individual ages,
		// all are nearly the same value at the oldest individuals (0.96 monthly survival).  This is done so don't have survival over 1.0
		// at oldest individuals, but net change scales down appropriately 

		//baseIndivVar = normal.nextDouble(0, survSD);

		// set a variable to relect the remaining months until end of 9.5year run
		int remainingMonths = 126-tk.getStepCounter(); 

		while ((loopCounter < remainingMonths) && (continueLoop)) {


			if (age<=11) ageTemp = age;
			else if ((age>11) && (age<=17)) ageTemp= 12;
			else if ((age>17) && (age<=23)) ageTemp= 13;
			else if ((age>23) && (age<=29)) ageTemp= 14;
			else if (age>29) ageTemp= 15;


			// old version
			//survOffset = 4 + survSubOffset[subType] + survSiteOffset; 
			//survRate = survRatesArrayAdjust[ageTemp];

			// if survType = 0 (Michalis Menton), use appropriate survRatesArray; otherwise, if survType=1 (Hill equation), use survRatesHillArray
			/*			if (survType == 0 )  

			else if (survType == 1) survRate = normal.nextDouble((survRatesHillArray[ageTemp][survOffset] + indivVar), 0.01); 

			else if (survType == 2) {

				if (ageTemp < 1 && disturbTimes[tk.getStepCounter()+loopCounter]==1){

				survRate = normal.nextDouble(((survRatesArray[ageTemp][survOffset] + indivVar)*(0.5)), 0.01);
				}

				else survRate = normal.nextDouble((survRatesArray[ageTemp][survOffset] + indivVar), 0.01);
			}
			 */			

			// if they survive, then set checkPostSettlement to true
			if(uniform.nextDoubleFromTo(0, 1) < survRatesArrayAdjust[ageTemp]){
				checkPostSettlement= true; 
			}
			// if they die, set continue loop to false so will break out of while loop
			// also, re-set checkPostSettlement to false, in case they survived for a few months, and then died, so no larvae added above in move() method
			else {
				checkPostSettlement = false; 
				continueLoop = false; 
			}


			// Here, set the size 
			spatSize = spatSize + getNewGrowth(age); 

			age++; 

			loopCounter++; 

		} // end of while loop


		return checkPostSettlement;
	}



	double getNewGrowth(int element) {
		double newGrowth=0; 

		// for standard growth rate and SD
		newGrowth = normal.nextDouble(0.049166667, 0.040927);

		// testing:
		/*		if (element < 12){
			newGrowth = normal.nextDouble(0.049166667 + 0.02, 0.040927);
		} 
		else  newGrowth = normal.nextDouble(0.049166667, 0.040927);
		 */		

		// here, reset the element from the "age" variable, because elements for survival are 0-8, while 
		// elements for growth are only 0-6
		//if (element > 5) element = 6; 
		//double newGrowth = normal.nextDouble(growRatesArray[reefNumber][element] /*+ growSiteOffset[reefNumber]*/, growSDArray[reefNumber][element]);

		if (newGrowth<0) newGrowth = 0; 
		return newGrowth; 
	}



	/** This method sets the location of the as they disperse from the parent.  The functional relationships used to determine 
	 * dispersal distances are varied in the model batch runs to test the typical dispersal distances of this species.
	 * 
	 * These include: normal distribution, linear decrease, exponential decrease, others....
	 * 
	 */

	void setDispersalPoint(){
		double xDisplacement, yDisplacement; 
		double angle=0; 

		// here, set upper range of dispersal to 1.008KM (10,080 pixels) -- this corresponds to larger landscape that connectivity
		// matrix is derived from
		// use beta distribution with alpha = 1.1, so will never be 0-distance dispersal
		dispersalDistance = betaDist.nextDouble(alpha,beta)*10080; 

		angle = uniform.nextDoubleFromTo(0, 1)*(2*Math.PI); 

		xDisplacement = Math.cos(angle)*dispersalDistance;
		yDisplacement = Math.sin(angle)*dispersalDistance;

		//************************************
		// Could put in a larval water-column mortality function here -- longer the dispersal, higher prob of mortality



		larvaeX = (int) xDisplacement + coralX;
		larvaeY = (int) yDisplacement + coralY;
	}




	void setExternalSupply() {
		float coralDiamFloat =0f;
		double coralCircleSA, coralHemSA, coralSA;
		int matrixElement = 0; 
		int gridLocX = 0;
		int gridLocY = 0; 

		// reset the number of external larvae for each of the 16 focal site cells (8x8m cell size) to zero at start of method
		for (int i = 0; i < 16; i++) {
			numExternalLarvae[i] = 0; 			
		}

		// go through the numExternalCorals array which holds the total number of corals to distribute for each size class
		// this array is set in the CoralBuilder class from TimeKeeper
		for (int i = 0; i < numExternalCorals.length; i++) {
			for (int j = 0; j < numExternalCorals[i]; j++) {

				//**********************************************************************
				// **** Here, do planulation and set dispersal point equivalent methods below
				//**********************************************************************

				if(month == 3) probOfPlanulate = normal.nextDouble(probApril, probAprilStd);
				if(month == 4) probOfPlanulate = normal.nextDouble(probMay, probMayStd);
				if(month == 5) probOfPlanulate = normal.nextDouble(probJune, probJuneStd);

				// if a colony has larvae that month, planulate larvae
				if(uniform.nextIntFromTo(0,100) < probOfPlanulate){


					// get the random size of the coral surface area 
					if (i == 4) coralDiamFloat = uniform.nextIntFromTo(0, 9)+11;
					else if (i==3)coralDiamFloat  = uniform.nextIntFromTo(0, 9)+21;
					else if (i==2)coralDiamFloat = uniform.nextIntFromTo(0, 9)+31;
					else if (i==1)coralDiamFloat = uniform.nextIntFromTo(0, 9)+41;
					else if (i==0)coralDiamFloat = uniform.nextIntFromTo(0, 9)+51;

					coralCircleSA = Math.PI*(coralDiamFloat/2)*(coralDiamFloat/2); 
					coralHemSA = coralCircleSA*3; 
					coralSA = uniform.nextIntFromTo(0,(int)(coralHemSA-coralCircleSA))+ coralCircleSA;

					coralSurfArea = coralSA; 


					// distribute a coral to just reef cell habitat (based on habitat maps) in the area surrounding each focal study area
					int elements = reefCoords.size(); 
					Point point = reefCoords.get(uniform.nextIntFromTo(0, elements-1));
					coralX = (int) point.getX();
					coralY = (int) point.getY(); 


					// below, only consider 70% of colony fecund to account for margins without larvae; this produced numLarvae values which most closely matched
					// actual data collected on num larvae per colony in 2006 (unpublished data), when calculating using Moulding's fecundity data 
					numLarvaeTemp = normal.nextDouble(numLarvaePerCM, numLarvaePerCMStd)*(coralSurfArea*fecundSA*larvaeMortalityDouble);
					if (numLarvaeTemp < 0) numLarvaeTemp = 0; 
					numLarvae = (int) numLarvaeTemp; 


					// get the matrix element (0-15875; i.e., 126x126 pixels of greater 1008x1008m landscape) 
					// which corresponds to that corals location
					matrixElement = coralX + (coralY * 126); 

					// loop through the 16 focal study area cells (4x4 cells, each 8x8m, so 32x32m area)
					for (int k = 0; k < ((int)gridWidth/80)*((int)gridWidth/80); k++) {

						// Here, the connectivity matrix represents the proportion of larvae, on average over many runs, that will disperse
						// to a given 8x8m area within the focal simulation landscape (320x320pixels) from a 4x4m area outside of the focal area
						// but within a 1.008x1.008km total area
						// Here, these proportions are calculated from independent simulation runs of the ConnectivityMatrixBuilder Class, 
						// and are specific for the type of dispersal used in the model (beta distribution)

						// keep a running total of the larvae which arrive at each focal study area cell from external corals 
						numExternalLarvae[k]= numExternalLarvae[k]+ (numLarvae*connectivityMatrix[k][matrixElement]);

					} // end of "k" for loop to go through local site cells

				} // end of possibiilty to planulate

			} // end of "j" for loop going through all corals of of certain size
		} // end of "i" for loop going through all size classes

		// go through all 
		for (int i = 0; i < 16; i++) {

			//			totalExternalLarvae = totalExternalLarvae + (int) numExternalLarvae[i]; 

			int loopCounter2 = 0;
			while (loopCounter2 < (int) numExternalLarvae[i]){

				gridLocX = (int) i%4; 
				gridLocY = (int) i/4; 

				// randomly distributes the larvae with the 8x8meter area it lands in
				larvaeX = gridLocX*80 + uniform.nextIntFromTo(0, 79); 
				larvaeY = gridLocY*80 + uniform.nextIntFromTo(0, 79); 

				move(); 

				loopCounter2++;
				//totalNumLarvae++; 
			} // end of while loop to release larvae
		}// end of "i" for loop to go through all 16 focal site cells and settle larvae

		//		System.out.println("Number of external larvae: \t" + totalExternalLarvae);


	} // end of setExternalSupply() method





	/** This method returns a toriodal value
	 * 
	 * @param x
	 * @return
	 */

	int getWrapAround(int x){

		int returnValue = x; 
		if (x >= gridWidth) returnValue = x-gridWidth;
		if (x < 0) returnValue = gridWidth+x;
		return returnValue; 
	}



	public HashMap<Point, ArrayList<Recruit>> getRecruitMap() {
		return recruitMap;
	}



	public ArrayList<Recruit> getRecruitList() {
		return recruitList;
	}




}

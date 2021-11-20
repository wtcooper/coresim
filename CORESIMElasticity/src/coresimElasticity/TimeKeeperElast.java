/*
 * This is the class which does all of the scheduling for the model.  This class's step method
 * 	executes every tick at priority = 1.  The basic approach is this:
 * 
 * 		1. Run a single habitat for 5 years, with planulation every 3,4, and 5th month of the year
 * 			a. need to set so that is some local and some non-local supply each period
 * 			b. right now, will simply read in a number of non-local supply each period
 * 
 * 		2. once 5 years is reached, remove all larvae and coral agents from the context, collect garbage,
 * 			and build the next habitat which has some larval supply
 * 
 * 		** if figure out how to batch run the model, simply run a model in batch mode where the 
 * 			parameter I input is the LarvalSupplyCell # -- then i can scroll through all 10,000 in
 * 			batch mode, and this may be easier to do as distributed program using guidelines in
 * 			Repast mailing list email.  This will take some work figuring out the distributed part
 * 			since the email refers to RepastJ, but hopefully would be easy enough to adapt
 * 
 * 
 * 	IF want to extend this to full population cycle, will need to read / write the habitat (substrate 
 * 		ValueLayer) at the end of each planulation event.  That way, the planulation is tied directly 
 * 		to the coral abundances at all other sites.  To do this, will need to change approach so am
 * 		not doing ALL landscapes, but just a sample of 100x100m areas.  Possibly if had this running
 * 		on a super computer with threads, I could do all, but that will have to be in the future.  
 * 		
 * 		I could do the read/write easily enough with NetCDF files for the substrate value layer, this would
 * 		be quick and relatively small footprint if only doing a sample of landscapes.  To read/write the 
 * 		corals, that may be a little more difficult -- if only on the order of a few thousand per
 * 		100x100m area, may do export to text files, where each line has all of necessary info.
 */

package coresimElasticity;

import java.util.ArrayList;

import cern.jet.random.Beta;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;


public class TimeKeeperElast implements Runnable {

	RecruitmentMainElast rm;
	RecruiterElast recruiter;  
	//Semaphore semaphore; 

	private int seed;
	private MersenneTwister m;
	private Uniform uniform; 
	private Normal normal;
	private Beta betaDist;

	private int numReefs = 4; // number of reefs in the simulation; here, in order: coopers, megans, mk14, mk16 
	
	// below, variables for input file describing reefs under study

	// Arrays of rugosity and substrate set in SubstrateBuilder
//	private int[][] rugosityArray = new int[32][32];
//	private int[][] orientationArray = new int[320][320]; 

	private int[][] coralArray;
	ArrayList<CoralElast> coralList; 
	private int [] numExternalCorals;	
	

	private int reefNumber = 0; // reef Number, start at 0 and go to 3 (4 total)
	private boolean currentReefNumberFinished = false;  //logic check if loop through 10 years
	private int stepCounter = 12; // value to loop through each month of 12 years; start at 12 so can use modulus operator to check for 3,4,5th months


	private int dispersalDist; // value for setting dispersal probability distribution (from beta distribution); 0-6 different options
	private double expSurvOffset, crypSurvOffset, siteSurvOffset; 
	private double larvaeMortality; 


	// Offset parameters input during batch processing; here allows for variation in survivorship among substrate types, which is unknown
	private int survType, subRepresent, adultSpatial;
	private double survSD; 
	private double desperateLarvaOffset; 
	private double[] subSurvOffset = new double[2];  
	private double settleMortality; 
	private double autoCorr; 	// roughness for diamond-square algorithm.
	// **Note: this is NOT value of H; here, is what Merlin recommends where can be from 0-infinity, as increase gets more random (i.e., opposite of H) 

	private int paramID; 
	private long startTime; 

	
	private double[] elasticityProp = new double[100]; // = {-1.5, -1.25, -1.1, -1.05, 1.05, 1.1, 1.25, 1.5};

	// For elasticity analysis
	//private int siteSurvOffsetProp, expSurvOffsetProp, crypSurvOffsetProp, larvaeMortalityProp, dispersalDistanceProp, survTypeProp; 
	//private int expCoverProp, expPrefProp, crypCoverProp, crypPrefProp, adultCoverProp; 

	// For sensitivity analysis
	private double siteSurvOffsetProp, expSurvOffsetProp, crypSurvOffsetProp, larvaeMortalityProp, 
	adultCoverProp, expCoverProp, crypCoverProp, expPrefProp, crypPrefProp, desperateLarvaOffsetProp, settleMortalityProp; 
	private int dispersalDistanceProp, survTypeProp, adultSpatialProp; 

	
	// Need to set real values here before start -- optimal value for each of 4 reef sites
	private double[] siteSurvOffsetOpti = {0, 0, 0, 0}; // this was set to 0 during optimization
	private double[] expSurvOffsetOpti = {0.026808862, 0.034650754, 0.000220011, -0.018576587};
	private double[] crypSurvOffsetOpti = {0.028786139, -0.034187727, 0.016954716, 0.016473293};
	private double[] larvaeMortalityOpti = {0.012281581, 0.037393211, 0.006880139, 0.010261904};
	private int[] dispersalDistOpti = {2,2,2,2}; 

	
	
	private double[] adultCoverOpti = {2.686013211, 0.599365596, 1.91566761, 1.813485975}; 
	private double[] expCoverOpti = {0.386986301, 0.423758865, 0.362565445, 0.450320513};
	private double[] crypCoverOpti = {0.181506849, 0.242907801, 0.17408377, 0.091346154};
	private double expPrefOpti = 0.055982143;  
	private double crypPrefOpti = 0.569444444; 
	private int adultSpatialOpti = 0;
	private double desperateLarvaOpti = 0.01;
	private double settleMortalityOpti = 0.01; 
	
	private double[] substratePref = new double[2]; // substratePref= {0.055982143, 0.569444444}
	private double[][] subCoverArraySimple = new double[4][2]; 
	private double[] siteCoverArray = new double[4]; 






	TimeKeeperElast (RecruitmentMainElast rm, MersenneTwister m, int ID) {

		this.rm = rm; 
		this.m = m; 
		this.paramID = ID; 
		
		uniform = new Uniform(0, 100, m); 
		normal = new Normal(1,1,m);
		betaDist = new Beta(1, 1, m);

		siteSurvOffsetProp = RecruitmentMainElast.siteSurvOffsetProp;
		expSurvOffsetProp = RecruitmentMainElast.expSurvOffsetProp;
		crypSurvOffsetProp = RecruitmentMainElast.crypSurvOffsetProp;
		larvaeMortalityProp = RecruitmentMainElast.larvaeMortalityProp;
		dispersalDistanceProp = RecruitmentMainElast.dispersalDistanceProp;
		survTypeProp = RecruitmentMainElast.survTypeProp; 

		expCoverProp = RecruitmentMainElast.expCoverProp;
		crypCoverProp = RecruitmentMainElast.crypCoverProp;
		expPrefProp = RecruitmentMainElast.expPrefProp;
		crypPrefProp = RecruitmentMainElast.crypPrefProp; 
		
		adultCoverProp = RecruitmentMainElast.adultCoverProp;
		
		survSD = 0.04;

		adultSpatialProp = RecruitmentMainElast.adultSpatialProp;
		desperateLarvaOffsetProp = RecruitmentMainElast.desperateLarvaOffsetProp; 
		settleMortalityProp = RecruitmentMainElast.settleMortalityProp; 

		
		/*  **** for elasticity analysis
		adultSpatial = 0;
		desperateLarvaOffset = 0.01; 
		settleMortality = 0.01;

		// this will set the elasticityProp between -2
		elasticityProp[0] = -0.95;
		elasticityProp[19] = .05;

		for (int i = 1 ; i < 19; i++){
			elasticityProp[i] = elasticityProp[i-1] + 0.05;       
		}

		for (int i = 20; i < 100; i++){
			elasticityProp[i] = elasticityProp[i-1] + 0.05;       
		}
	*/
		
	}

	
	



	/** 
	 * Start method to set random number generators, read input files, and initialize landscape
	 */
	public void run() {
		//System.out.println("Model start time:\t" + System.currentTimeMillis());

		recruiter = new RecruiterElast(this);          
		setNextSupplyCell(); // initialized landscape: builds rugosity and allocates corals to landscape
		step(); 
		rm.releaseSemaphore(); 
	}


	/** Timekeeper step method, scheduled for each simulation tick with first priority.
	 * 	This method controls simulation scheduling, primarily how long to run simulation for a single reef, which months the
	 * corals planulate, re-initializing landscapes once simulation is finished for a single reef, and ending simulation run. 
	 * 
	 */
	public void step() {

		boolean simulationRunning = true; 

		// Simulation of one reef runs for 9yrs 7 months, beginning Jan and ending July
		// end in July since this is when recruit surveys were done
		while (simulationRunning){

			//double time = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
//			System.out.println("Reefnumber: \t" + reefNumber  + "\tYear: \t" + (int) stepCounter/12 + "\tMonth: \t" + stepCounter%12);

			// if stop at 126, this is equivalent to 10th year, at end of July (when actual recruit surveys were performed)
			if (stepCounter <= 126){

				// select the 4th, 5th, and 6th calendar month for planulation (below: 3,4,5 since start at month 0)
				if (stepCounter%12 == 3 || stepCounter%12 == 4 || stepCounter%12 == 5 ) {

					//long startTime = System.currentTimeMillis();
					recruiter.recruit(); 
//					System.out.println("recruit method time:\t" + (System.currentTimeMillis()-startTime));
				}
			}
			if (stepCounter == 126){ 
				finishSupplyCell();  

				// if finished with all reefs, exit model run
				if (reefNumber > 3){

					//System.out.println("Model end time:\t" + System.currentTimeMillis());
					simulationRunning = false; 
 
				}

				// if more reefs to do, re-initialize landscape, and start at year 1 for next reef
				else {
					setNextSupplyCell();
					stepCounter = 11; 
					currentReefNumberFinished = false;
				}
			}

			stepCounter++;
		}
	}




	/**	Finished a reef run by first recording recruit patterns from that run through PatternRecorder, second removing all agents from context, 
	 * 	and third incrementing the reefNumber counter so moves onto next reef.  
	 * 
	 */
	void finishSupplyCell(){

		PatternRecorderElast pr = new PatternRecorderElast(this, rm, recruiter);
		pr.recordPatterns(); 

		reefNumber++; 

		System.gc ();
	}


	/**	Initializes the landscape by first building the rugosity which is used to distribute substrate types, and second distributing corals
	 * 
	 */
	void setNextSupplyCell(){

		if (dispersalDistanceProp > 90) dispersalDist = dispersalDistOpti[reefNumber]; // set this so non-dispersalDist parameter elasticities will do dispersal Dist at optimal value
		else dispersalDist = dispersalDistanceProp; 
		
		if (survTypeProp > 90) survType = 1;
		else survType = survTypeProp; 
		
		if (expSurvOffsetProp > 90) expSurvOffset = expSurvOffsetOpti[reefNumber];
		else expSurvOffset = expSurvOffsetProp; 

		if (crypSurvOffsetProp > 90) crypSurvOffset = crypSurvOffsetOpti[reefNumber];
		else crypSurvOffset = crypSurvOffsetProp; 

		if (siteSurvOffsetProp > 90) siteSurvOffset = siteSurvOffsetOpti[reefNumber];
		else siteSurvOffset = siteSurvOffsetProp; 

		if (larvaeMortalityProp > 90) larvaeMortality = larvaeMortalityOpti[reefNumber];
		else larvaeMortality= larvaeMortalityProp; 

		if (expPrefProp > 90) substratePref[0] = expPrefOpti; 
		else substratePref[0] = expPrefProp; 
		
		if (crypPrefProp >90) substratePref[1] = crypPrefOpti; 
		else substratePref[1] = crypPrefProp; 

		if (expCoverProp > 90) subCoverArraySimple[reefNumber][0] = expCoverOpti[reefNumber]; 
		else subCoverArraySimple[reefNumber][0] = expCoverProp; 
		
		if (crypCoverProp > 90) subCoverArraySimple[reefNumber][1] = crypCoverOpti[reefNumber]; 
		else subCoverArraySimple[reefNumber][1] = crypCoverProp;

		if (adultCoverProp > 90) siteCoverArray[reefNumber] = adultCoverOpti[reefNumber];
		else siteCoverArray[reefNumber]  = adultCoverProp;  

		if (adultSpatialProp > 90) adultSpatial = adultSpatialOpti;
		else adultSpatial = adultSpatialProp;  
		
		if (desperateLarvaOffsetProp > 90) desperateLarvaOffset = desperateLarvaOpti;
		else desperateLarvaOffset = desperateLarvaOffsetProp; 
		

		if (settleMortalityProp > 90) settleMortality= settleMortalityOpti;
		else settleMortality = settleMortalityProp; 


		
		/* **** this is for Elasticity analysis
		if (dispersalDistanceProp == 0) dispersalDist = dispersalDistOpti[reefNumber]; // set this so non-dispersalDist parameter elasticities will do dispersal Dist at optimal value
		else dispersalDist = dispersalDistanceProp-1; 
		
		
		if (survTypeProp == 0) survType = 1;
		else survType = survTypeProp-1; 
		
		if (expSurvOffsetProp == 0) expSurvOffset = expSurvOffsetOpti[reefNumber];
		else expSurvOffset = expSurvOffsetOpti[reefNumber]+expSurvOffsetOpti[reefNumber]*elasticityProp[expSurvOffsetProp-1]; 

		if (crypSurvOffsetProp == 0) crypSurvOffset = crypSurvOffsetOpti[reefNumber];
		else crypSurvOffset = crypSurvOffsetOpti[reefNumber]+crypSurvOffsetOpti[reefNumber]*elasticityProp[crypSurvOffsetProp-1]; 

		if (siteSurvOffsetProp == 0) siteSurvOffset = siteSurvOffsetOpti[reefNumber];
		else siteSurvOffset = siteSurvOffsetOpti[reefNumber]+siteSurvOffsetOpti[reefNumber]*elasticityProp[siteSurvOffsetProp-1]; 

		if (larvaeMortalityProp == 0) larvaeMortality = larvaeMortalityOpti[reefNumber];
		else larvaeMortality= larvaeMortalityOpti[reefNumber]+larvaeMortalityOpti[reefNumber]*elasticityProp[larvaeMortalityProp-1]; 

		if (expPrefProp == 0) substratePref[0] = expPrefOpti; 
		else substratePref[0] = expPrefOpti + expPrefOpti*elasticityProp[expPrefProp-1];
		
		if (crypPrefProp == 0) substratePref[1] = crypPrefOpti; 
		else substratePref[1] = crypPrefOpti + crypPrefOpti*elasticityProp[crypPrefProp-1];

		if (expCoverProp == 0) subCoverArraySimple[reefNumber][0] = expCoverOpti[reefNumber]; 
		else subCoverArraySimple[reefNumber][0] = expCoverOpti[reefNumber] + expCoverOpti[reefNumber]*elasticityProp[expCoverProp-1];
		
		if (crypCoverProp == 0) subCoverArraySimple[reefNumber][1] = crypCoverOpti[reefNumber]; 
		else subCoverArraySimple[reefNumber][1] = crypCoverOpti[reefNumber] + crypCoverOpti[reefNumber]*elasticityProp[crypCoverProp-1];

		if (adultCoverProp == 0) siteCoverArray[reefNumber] = adultCoverOpti[reefNumber];
		else siteCoverArray[reefNumber]  = adultCoverOpti[reefNumber]+adultCoverOpti[reefNumber]*elasticityProp[adultCoverProp-1]; 
		*/
		
		
		subSurvOffset[0] = expSurvOffset;
		subSurvOffset[1] = crypSurvOffset;


		
		// class which populates the reef with corals of appropriate cover and size frequency
		CoralBuilderElast cBuilder = new CoralBuilderElast(this); 
		cBuilder.buildCorals();
		coralArray = cBuilder.getCoralArray(); 
		numExternalCorals = cBuilder.getExternalCoralNum();
		coralList = cBuilder.getCoralList(); 
		
		recruiter.setRecruitParams(); 
		
		//DrawHabitat drawer = new DrawHabitat(320, 320, 1, coralArray);
	   	//drawer.setVisible(true);
	}






	public int getStepCounter() {
		return stepCounter;
	}

	public int getNumReefs() {
		return numReefs;
	}

	public void setNumReefs(int numReefs) {
		this.numReefs = numReefs;
	}

	public int[] getNumExternalCorals() {
		return numExternalCorals;
	}

	public void setNumExternalCorals(int[] numExternalCorals) {
		this.numExternalCorals = numExternalCorals;
	}


/*	public int[][] getRugosityArray() {
		return rugosityArray;
	}

	public void setRugosityArray(int[][] rugosityArray) {
		this.rugosityArray = rugosityArray;
	}
*/

	public int getReefNumber() {
		return reefNumber;
	}

	public void setReefNumber(int reefNumber) {
		this.reefNumber = reefNumber;
	}

	public MersenneTwister getM() {
		return m;
	}

	public void setM(MersenneTwister m) {
		this.m = m;
	}


	public int getDispersalDist() {
		return dispersalDist;
	}

	public void setDispersalDist(int dispersalDist) {
		this.dispersalDist = dispersalDist;
	}



	public int[][] getCoralArray() {
		return coralArray;
	}

	public void setCoralArray(int[][] coralArray) {
		this.coralArray = coralArray;
	}




	public int getSeed() {
		return seed;
	}





	public void setSeed(int seed) {
		this.seed = seed;
	}





	public Uniform getUniform() {
		return uniform;
	}





	public void setUniform(Uniform uniform) {
		this.uniform = uniform;
	}





	public Normal getNormal() {
		return normal;
	}





	public void setNormal(Normal normal) {
		this.normal = normal;
	}





	public Beta getBetaDist() {
		return betaDist;
	}


	public void setBetaDist(Beta betaDist) {
		this.betaDist = betaDist;
	}


	public double getAutoCorr() {
		return autoCorr;
	}

	public void setAutoCorr(double autoCorr) {
		this.autoCorr= autoCorr;
	}



	public ArrayList<CoralElast> getCoralList() {
		return coralList;
	}

	public void setCoralList(ArrayList<CoralElast> coralList) {
		this.coralList = coralList;
	}

	public double getSettleMortality() {
		return settleMortality;
	}


	public int getParamID() {
		return paramID;
	}

	public void setParamID(int paramID) {
		this.paramID = paramID;
	}

	public int getSurvType() {
		return survType;
	}



	public double[] getSubSurvOffset() {
		return subSurvOffset;
	}

	public double getSiteSurvOffset() {
		return siteSurvOffset;
	}



/*	public int[][] getOrientationArray() {
		return orientationArray;
	}
*/


	public int getSubRepresent() {
		return subRepresent;
	}



	public int getAdultSpatial() {
		return adultSpatial;
	}


	public double getSurvSD() {
		return survSD;
	}






	public double getLarvaeMortality() {
		return larvaeMortality;
	}



	public long getStartTime() {
		return startTime;
	}






	public double getDesperateLarvaOffset() {
		return desperateLarvaOffset;
	}






	public double getExpSurvOffset() {
		return expSurvOffset;
	}






	public double getCrypSurvOffset() {
		return crypSurvOffset;
	}



	public double[] getSubstratePref() {
		return substratePref;
	}






	public double[][] getSubCoverArraySimple() {
		return subCoverArraySimple;
	}






	public double[] getSiteCoverArray() {
		return siteCoverArray;
	}





} // end of TimeKeeper class

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

package coresim;

import java.util.ArrayList;

import cern.jet.random.Beta;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;


public class TimeKeeper implements Runnable {

	RecruitmentMain rm;
	Recruiter recruiter;  
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
	ArrayList<Coral> coralList; 
	private int [] numExternalCorals;	
	

	private int reefNumber = 0; // reef Number, start at 0 and go to 3 (4 total)
	private boolean currentReefNumberFinished = false;  //logic check if loop through 10 years
	private int stepCounter = 12; // value to loop through each month of 12 years; start at 12 so can use modulus operator to check for 3,4,5th months


	private int dispersalDist; // value for setting dispersal probability distribution (from beta distribution); 0-6 different options


	// Offset parameters input during batch processing; here allows for variation in survivorship among substrate types, which is unknown
	private int survType, subRepresent, adultSpatial;
	private double survSD; 
	private double desperateLarvaOffset; 
	private double[] subSurvOffset = new double[2];  
	private double expSurvOffset, crypSurvOffset, siteSurvOffset; 
	private int larvaeMortality; 
	private double settleMortality; 
	private double autoCorr; 	// roughness for diamond-square algorithm.
	// **Note: this is NOT value of H; here, is what Merlin recommends where can be from 0-infinity, as increase gets more random (i.e., opposite of H) 

	private int paramID; 
	private long startTime; 


	TimeKeeper (RecruitmentMain rm, MersenneTwister m, int ID) {

		this.rm = rm; 
		this.m = m; 
		this.paramID = ID; 
		
		uniform = new Uniform(0, 100, m); 
		normal = new Normal(1,1,m);
		betaDist = new Beta(1, 1, m);

		dispersalDist	= RecruitmentMain.dispersalDist;
		survType = RecruitmentMain.survType; 
		expSurvOffset = RecruitmentMain.expSurvOffset;
		crypSurvOffset = RecruitmentMain.crypSurvOffset;
		siteSurvOffset = RecruitmentMain.siteSurvOffset;
		adultSpatial = RecruitmentMain.adultSpatial;
		survSD = RecruitmentMain.survSD;
		autoCorr = RecruitmentMain.autoCorr;
		settleMortality = RecruitmentMain.settleMortality; 
		larvaeMortality = RecruitmentMain.larvaeMortality; 
		desperateLarvaOffset = RecruitmentMain.desperateLarvaOffset; 
		
		// set these if conditions so that in parameter sweep, will do 0, 0.01, 0.05, versus 0, 0.01, 0.02
		if (settleMortality > 0.019 && settleMortality < 0.021){
			settleMortality = 0.05;
		}

		if (desperateLarvaOffset > 0.019 && desperateLarvaOffset < 0.021){
			desperateLarvaOffset = 0.05;
		}

		subSurvOffset[0] = expSurvOffset;
		subSurvOffset[1] = crypSurvOffset;

	}

	
	



	/** 
	 * Start method to set random number generators, read input files, and initialize landscape
	 */
	public void run() {
		//System.out.println("Model start time:\t" + System.currentTimeMillis());

		recruiter = new Recruiter(this);          
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

		PatternRecorder pr = new PatternRecorder(this, rm, recruiter);
		pr.recordPatterns(); 

		reefNumber++; 

		System.gc ();
	}


	/**	Initializes the landscape by first building the rugosity which is used to distribute substrate types, and second distributing corals
	 * 
	 */
	void setNextSupplyCell(){


		
		// class which populates the reef with corals of appropriate cover and size frequency
		CoralBuilder cBuilder = new CoralBuilder(this); 
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



	public ArrayList<Coral> getCoralList() {
		return coralList;
	}

	public void setCoralList(ArrayList<Coral> coralList) {
		this.coralList = coralList;
	}

	public double getSettleMortality() {
		return settleMortality;
	}

	public void setSettleMortality(int settleMortality) {
		this.settleMortality = settleMortality;
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






	public int getLarvaeMortality() {
		return larvaeMortality;
	}



	public long getStartTime() {
		return startTime;
	}






	public double getDesperateLarvaOffset() {
		return desperateLarvaOffset;
	}


} // end of TimeKeeper class

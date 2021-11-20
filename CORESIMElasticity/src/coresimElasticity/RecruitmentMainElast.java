package coresimElasticity;

import java.awt.Point;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import net.jcip.annotations.GuardedBy;
import cern.jet.random.engine.MersenneTwister;


/**
 * This is main class of CORESIM (COral REcruitment SIMulation)
 * @author Wade Cooper
 *
 */
public class RecruitmentMainElast {

	protected Object writeLock = new Object(); // lock for writing operations
	private static int N_CPUS; // = Runtime.getRuntime().availableProcessors(); 
	private ExecutorService exec; // = Executors.newFixedThreadPool(N_CPUS+1); // runs thread pool with #CPUs+1 threads
	private Semaphore semaphore; //= new Semaphore(N_CPUS+4); // blocks the task submission rate to work queue to #CPUs+4 tasks

	static int idNum = 0; 
	static long startTime; 
	private static int newSeed;
	private static final int seed = (int) System.currentTimeMillis();
	private static MersenneTwister m;
	private static final int numTransects = 2; 
	private static final int numQuadrats = 15; 

	// Synchronized write parameters
	@GuardedBy("writeLock") private static PrintWriter outFilePatterns=null;	
	@GuardedBy("writeLock") private static int reefNumber; 
	@GuardedBy("writeLock") private static double totalAbundance; 
	@GuardedBy("writeLock") private static double avgDensity; 
	@GuardedBy("writeLock") private static double avgDensitySD; 
	@GuardedBy("writeLock") private static int[] totalSizeFreq = new int[6];
	@GuardedBy("writeLock") private static int[] avgQuadratSizeFreq = new int[6];
	@GuardedBy("writeLock") private static int[][] quadratDensity = new int[numTransects][numQuadrats];
	@GuardedBy("writeLock") private static int[][] quadratCoralCover = new int[numTransects][numQuadrats];
	@GuardedBy("writeLock") private static int[][] twoMeterCoralCover = new int[numTransects][numQuadrats];
	@GuardedBy("writeLock") private static int[][][] quadratSizeFreq = new int[numTransects][numQuadrats][6];
	@GuardedBy("writeLock") private static double fitDensity; 
	@GuardedBy("writeLock") private static double fitTotal; 
	@GuardedBy("writeLock") private static double fitDensitySD; 
	@GuardedBy("writeLock") private static double fitSizeFreqSkew; 
	@GuardedBy("writeLock") private static double fitSizeFreqKurt; 
	@GuardedBy("writeLock") private static double fitSubExp; 
	@GuardedBy("writeLock") private static double fitSubCryp; 

	private static int[][] paramArray; 
	private static double[][] paramArrayDoubles; 
	private static String batchFileID; 
	// arraylists of points for where reef habitat is located
	private static ArrayList<Point> coopersReefCoords;
	private static ArrayList<Point> megansReefCoords;
	private static ArrayList<Point> mk14ReefCoords;
	private static ArrayList<Point> mk16ReefCoords; 

	// input data from ReefDataInput.txt
	private static int numReefs = 4; 
	private static float[] siteCoverArray;  
	private static float[][] sizeFreqArray;  
	private static float[][] rugosityCoverArray;
	private static float[][][] orientCoverArray;
	private static float[][][][] subCoverArray;
	private static float[][] subCoverArraySimple;
	private static float[] survRatesArray;  
	private static float[] survPropArray;  
	private static float[] survRatesHillArray;  
	private static float[][] growRatesArray;  
//	private static float[][] survSDArray;  
	private static float[][] growSDArray;  
	//private static final int[] id = new int[numReefs];
	//private static final float[] distanceToShore = new float[numReefs];
	//private static final float[] recruitDensityArray =  new float[numReefs];  

	private static int gridWidth=320, gridHeight=320; 
	private static int dispersalRange = 10080; // here, would be 10080 pixels, or 1.008km; this is total size of larger area around simulation landscape of 320x320
	private static int numDispDistributions = 4; 
	private static float[][] connectivityMatrix0; 
	private static float[][] connectivityMatrix1; 
	private static float[][] connectivityMatrix2; 
	private static float[][] connectivityMatrix3; 

	private static PrintWriter outFileID = null; 

	/**
	 * Default parameter values
	 */
	static int numSweeps; 
	static int dispersalDist; // dispersal ditribution setting, 0-3 corresponding to beta distributions values (see Recruiter.alphaArray and Recruiter.betaArray)
	static int survType; 
	static double expSurvOffset;
	static double crypSurvOffset;
	static double siteSurvOffset;
	static int subRepresent; // if 0, do simple substrate representation (just subtype at random); if 1, do complex (rugosity+orientation)
	static int adultSpatial; // do 3 levels of adult spatial distrib: 0=clumped (Brooke's values), 1=random, and 2=highly clumped
	static double survSD; // do 3 levels of adult spatial distrib: 0=clumped (Brooke's values), 1=random, and 2=highly clumped
	static double autoCorr;	// spatial autocorrelation value
	static double settleMortality; // mortality incurred by larvae while looking for settlment site (1% per step) 
	static double larvaeMortality; 
	static double desperateLarvaOffset; 
	static double siteCover, expPref, crypPref, expCover, crypCover;


	private static File fFile; // input file reader

	private static String paramFile; 

	// **** Below are for Elasticity Analysis
	//static int siteSurvOffsetProp=0, expSurvOffsetProp=0, crypSurvOffsetProp=0, larvaeMortalityProp=0, dispersalDistanceProp=0, survTypeProp=0; 
	//static int adultCoverProp=0, expCoverProp=0, crypCoverProp=0, expPrefProp=0, crypPrefProp=0;

	// **** Below are for Sensitivity Analysis
	static double siteSurvOffsetProp=99, expSurvOffsetProp=99, crypSurvOffsetProp=99, larvaeMortalityProp=99, 
		adultCoverProp=99, expCoverProp=99, crypCoverProp=99, expPrefProp=99, crypPrefProp=99, desperateLarvaOffsetProp=99, settleMortalityProp=99; 
	static int dispersalDistanceProp=99, survTypeProp=99, adultSpatialProp=99; 

	/**
	 * Main method
	 */
	public static void main(String[] args) {
		paramFile = args[0]; 
		//System.out.println("Model start time:\t" + System.currentTimeMillis());

		RecruitmentMainElast rm = new RecruitmentMainElast();
		rm.initialize(); 
		System.out.println("Input done");
		rm.batchRun();
		double runTime = ( (double) System.currentTimeMillis()-startTime)/(1000*60); 
		System.out.println("Model execution time:\t" + runTime + " minutes");
		//System.out.println("start time:\t" + startTime);
		//System.out.println("end time:\t" + System.currentTimeMillis());

	}


	/**
	 * Method for performing batch runs. 
	 * @throws InterruptedException 
	 */
	private void batchRun() {

		for(numSweeps = 0; numSweeps<paramArray[0][0]; numSweeps++){ 

			
			for (dispersalDistanceProp=0; dispersalDistanceProp<4; dispersalDistanceProp++){submitRun();}
			dispersalDistanceProp=99;			 

			for (survTypeProp=0; survTypeProp<2; survTypeProp++){submitRun();}
			survTypeProp=99;

			for (siteSurvOffsetProp=-0.06; siteSurvOffsetProp<0.065; siteSurvOffsetProp+=0.01){submitRun();}
			siteSurvOffsetProp=99;

			for (expSurvOffsetProp=-0.06; expSurvOffsetProp<0.065; expSurvOffsetProp+=0.01){submitRun();}
			expSurvOffsetProp=99;

			for (crypSurvOffsetProp=-0.06; crypSurvOffsetProp<0.065; crypSurvOffsetProp+=0.01){submitRun();}
			crypSurvOffsetProp=99;

			for (larvaeMortalityProp=0.001; larvaeMortalityProp<0.32; larvaeMortalityProp+=0.025){submitRun();}
			larvaeMortalityProp=99;

			for (adultCoverProp = 0.25; adultCoverProp<10.3; adultCoverProp+=0.5) {submitRun();}
			adultCoverProp =99; 

			for (expPrefProp = 0; expPrefProp <1; expPrefProp +=0.1) {submitRun();}
			expPrefProp =99; 
			for (crypPrefProp = 0; crypPrefProp <1; crypPrefProp +=0.1) {submitRun();}
			crypPrefProp =99; 

			for (expCoverProp = 0; expCoverProp <1; expCoverProp +=0.1) {submitRun();}
			expCoverProp =99; 
			
			for (crypCoverProp = 0; crypCoverProp <1; crypCoverProp +=0.1) {submitRun();}
			crypCoverProp =99; 

			for (adultSpatialProp = 0; adultSpatialProp < 3; adultSpatialProp++) {submitRun();}
			adultSpatialProp = 99;
			
			for (desperateLarvaOffsetProp = 0; desperateLarvaOffsetProp < 0.21; desperateLarvaOffsetProp+=0.02){submitRun();}
			desperateLarvaOffsetProp = 99; 
			
			for (settleMortalityProp = 0; settleMortalityProp < 0.21; settleMortalityProp+=0.02){submitRun();}
			settleMortalityProp = 99; 
			
			
			// set dispersal dist 1-5, then substract 1 from it if not = 0 so set as 1-4
			// reset to 0 after going through all 5 levels so is average for rest
			//***** Below is for Elasticity Analysis
			/*  
			for (dispersalDistanceProp=1; dispersalDistanceProp<6; dispersalDistanceProp++){submitRun();}
			dispersalDistanceProp=0;			 

			for (survTypeProp=1; survTypeProp<3; survTypeProp++){submitRun();}
			survTypeProp=0;

			for (siteSurvOffsetProp=1; siteSurvOffsetProp<39; siteSurvOffsetProp++){submitRun();}
			siteSurvOffsetProp=0;

			for (expSurvOffsetProp=1; expSurvOffsetProp<39; expSurvOffsetProp++){submitRun();}
			expSurvOffsetProp=0;
			for (crypSurvOffsetProp=1; crypSurvOffsetProp<39; crypSurvOffsetProp++){submitRun();}
			crypSurvOffsetProp=0;

			for (larvaeMortalityProp=1; larvaeMortalityProp<39; larvaeMortalityProp++){submitRun();}
			larvaeMortalityProp=0;

			for (adultCoverProp = 1; adultCoverProp<39; adultCoverProp++) {submitRun();}
			adultCoverProp =0; 

			for (expPrefProp = 1; expPrefProp <39; expPrefProp ++) {submitRun();}
			expPrefProp =0; 
			for (crypPrefProp = 1; crypPrefProp <39; crypPrefProp ++) {submitRun();}
			crypPrefProp =0; 

			for (expCoverProp = 5; expCoverProp <35; expCoverProp ++) {submitRun();}
			expCoverProp =0; 
			
			for (crypCoverProp = 5; crypCoverProp <35; crypCoverProp ++) {submitRun();}
			crypCoverProp =0; 
			
			*/
		}
		
		exec.shutdown(); // call shutdown of threads once work queue is finished

		try { 
			if (exec.awaitTermination(60, TimeUnit.MINUTES)){ // blocks until all tasks complete, or timeout is reached
				//writeStartStopTime("end", 0);
				outFilePatterns.close();
				outFileID.close();
			}
		} catch (InterruptedException ex) {ex.printStackTrace(); }
	} // end batchRun()


	public void submitRun() {
		
		  //**********************************
		  // Add a Semaphore here to throttle task submission: put N_CPUS + 5 so have extra tasks in queue
		  // need to do this so don't have too many tasks in work queue which could start eating into resources
		  // pass Semaphore reference to TimeKeeper so can do semaphore.release() at end of thread run
		  try {
			semaphore.acquire();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		} 
		 // System.out.println("semaphore permit aquired"); 

		  newSeed = seed + idNum*100;  // set new seed value so each run will have different seed
		  m = new MersenneTwister(newSeed); 
		  TimeKeeperElast tk = new TimeKeeperElast(this, m, idNum);
		  //tk.run(); 
		  writeIDTags(idNum, newSeed); 
		  idNum++; 
		  try { exec.execute(tk);} // add TimeKeeper objects to work queue; ExecutorService will assign to threads when available  
		  catch (RejectedExecutionException e){}  

	}
	
	
	
	/**
	 * Releases the semaphore; called from TimeKeeper at end of individual run so a new run is added to the queue  
	 */
	public void releaseSemaphore(){
		semaphore.release();  // release semaphore "permit" so another task can be added to work queue
		// System.out.println("semaphore permit released"); 
	}
	
	
	

	/**
	 * Initialization of model: read in static input data (reef info, reef locations, connectivity matrix)
	 */
	private void initialize() {

		startTime = System.currentTimeMillis(); 

		InputReaderElast ir = new InputReaderElast();

		ir.readParamFile(paramFile); 			
		batchFileID = InputReaderElast.getBatchFileID(); 
		N_CPUS = InputReaderElast.getN_CPUS(); 
		//larvaeMortality = InputReader.getLarvaeMortality(); 
		paramArray = InputReaderElast.getParamArray(); 
		paramArrayDoubles = InputReaderElast.getParamArrayDoubles(); 
		
		exec = Executors.newFixedThreadPool(N_CPUS+1); 
		semaphore = new Semaphore(N_CPUS+4); 
		
		ir.readInputFile();											 
		siteCoverArray = InputReaderElast.getSiteCoverArray();
		sizeFreqArray =  InputReaderElast.getSizeFreqArray();  
		rugosityCoverArray = InputReaderElast.getRugosityCoverArray();
		orientCoverArray = InputReaderElast.getOrientCoverArray(); 
		subCoverArray = InputReaderElast.getSubCoverArray(); 	
		subCoverArraySimple = InputReaderElast.getSubCoverArraySimple(); 
		growRatesArray =  InputReaderElast.getGrowRatesArray();  
		growSDArray =  InputReaderElast.getGrowSDArray();   
		
		ir.readSurvivalFile(); 
		survRatesArray = InputReaderElast.getSurvRatesArray(); 
		survPropArray = InputReaderElast.getSurvPropArray(); 
		survRatesHillArray = InputReaderElast.getSurvRatesHillArray(); 
//		survSDArray =  InputReader.getSurvSDArray();  

		for (int i=0; i<numReefs; i++){
			ir.readReefHabitatFile(i); // read in reef location data
			if (i ==0) coopersReefCoords = InputReaderElast.getCoopersReefCoords(); 
			else if (i==1) megansReefCoords = InputReaderElast.getMegansReefCoords();
			else if (i==2) mk14ReefCoords = InputReaderElast.getMk14ReefCoords();
			else if (i==3) mk16ReefCoords = InputReaderElast.getMk16ReefCoords();
		}

		for (int i=0; i<numDispDistributions; i++){
			ir.readConnectivityFile(i); // read in reef location data
			if (i ==0) connectivityMatrix0 = InputReaderElast.getConnectivityMatrix0();
			else if (i==1) connectivityMatrix1 = InputReaderElast.getConnectivityMatrix1();
			else if (i==2) connectivityMatrix2 = InputReaderElast.getConnectivityMatrix2();
			else if (i==3) connectivityMatrix3 = InputReaderElast.getConnectivityMatrix3();
		}
		
		// initialize output file
		try { outFilePatterns = new PrintWriter(new FileWriter("RecruitModelPatterns"+batchFileID+".txt", true));
		} catch (IOException e) {e.printStackTrace();}

		// initialize output file for ID parameter values 
		try { outFileID = new PrintWriter(new FileWriter("IDParameterValues"+batchFileID+".txt", true));
		} catch (IOException e) {e.printStackTrace();}

		//writeStartStopTime("start", startTime); 

	}










	/**
	 * Method to write the ID parameter values to seperate file when doing batch runs
	 */
	void writeIDTags(int id, int seed) {

//		System.out.println("write ID tags");

		outFileID.println(startTime + "\t" + id+"\t"+seed+"\t"+dispersalDistanceProp+"\t"+ survTypeProp +"\t"+ siteSurvOffsetProp+"\t"+
				expSurvOffsetProp+"\t"+crypSurvOffsetProp+"\t"+ larvaeMortalityProp +"\t"+ adultCoverProp+"\t"+
				expPrefProp+"\t"+ crypPrefProp +"\t"+ expCoverProp+"\t"+ crypCoverProp+"\t"+ adultSpatialProp+"\t"+ desperateLarvaOffsetProp
				+"\t"+ settleMortalityProp); 
				
	}


	/**
	 * Print out the start and stop time of the model to RecruitPatterns.txt
	 * @param time
	 */
	public void writeStartStopTime(String time, long startTime){
		if (time == "start") outFilePatterns.println("Model start time: \t" + startTime); 
		if (time == "end") outFilePatterns.println("Model end time: \t" + System.currentTimeMillis()); 
	}

	/**
	 * Synchroinized (to internal lock) method to write recruit patterns during individual runs. 
	 * @param pr
	 * @param id
	 */
	public  void writeOutput(PatternRecorderElast pr, int id) {

		synchronized (writeLock){

			Thread t = Thread.currentThread();
			String name = t.getName();

		//	System.out.println("pattern recording, thread#: \t" + name);

			reefNumber = pr.getReefNumber();

//			totalAbundance = pr.getTotalAbundance(); 
//			totalSizeFreq = pr.getTotalSizeFreq();
//			avgQuadratSizeFreq = pr.getAvgQuadratSizeFreq(); 
//			quadratDensity = pr.getQuadratDensity();
//			quadratCoralCover = pr.getQuadratCoralCover();
//			quadratSizeFreq = pr.getQuadratSizeFreq(); 
//			twoMeterCoralCover = pr.getTwoMeterCoralCover(); 
//			avgDensity= pr.getAvgDensity();
//			avgDensitySD = pr.getAvgDensitySD(); 

			fitTotal = pr.getFitTotal(); 
			fitDensity = pr.getFitDensity();
			fitDensitySD = pr.getFitDensitySD();
			fitSizeFreqSkew = pr.getFitSizeFreqSkew(); 
			fitSizeFreqKurt = pr.getFitSizeFreqKurt(); 
			fitSubExp = pr.getFitSubExp();
			fitSubCryp = pr.getFitSubCryp(); 
			
			dispersalDist = pr.getDispersalDist();
			survType = pr.getSurvType();
			expSurvOffset = pr.getExpSurvOffset();
			crypSurvOffset = pr.getCrypSurvOffset();
			siteSurvOffset = pr.getSiteSurvOffset();
			larvaeMortality = pr.getLarvaeMortality(); 
			siteCover = pr.getSiteCover(); 
			expPref = pr.getExpPref();
			crypPref = pr.getCrypPref();
			expCover = pr.getExpCover();
			crypCover = pr.getCrypCover();
			desperateLarvaOffset = pr.getDesperateLarvaOffset();
			settleMortality = pr.getSettleMortality();
			adultSpatial = pr.getAdultSpatial();
			
			double avgDensity = pr.getAvgDensity();
			double avgDensitySD = pr.getAvgDensitySD();
			double sizeFreqSkew = pr.getSizeFreqSkew();
			double sizeFreqKurt = pr.getSizeFreqKurt();
			double propExp = pr.getPropExp();
			double propCryp = pr.getPropCryp(); 
			

/*			System.out.println(startTime + "\t" + id + "\t" + reefNumber + "\t" 
					+ fitTotal + "\t" + fitDensity + "\t" + fitDensitySD + "\t" + fitSizeFreqSkew + "\t" + fitSizeFreqKurt + "\t" 
					+ fitSubExp + "\t" + fitSubCryp + "\t"
					+ dispersalDist+"\t"+ survType +"\t"+ expSurvOffset+"\t"+
					crypSurvOffset+"\t"+siteSurvOffset+"\t"+ adultSpatial+"\t"+larvaeMortality+"\t"+siteCover+"\t"+expPref+"\t"+crypPref
					+"\t"+expCover+"\t"+crypCover); 
*/
			
			outFilePatterns.println(startTime + "\t" +/*name + "\t" +*/ id + "\t" + reefNumber + "\t" 
					+ fitTotal + "\t" + fitDensity + "\t" + avgDensity + "\t" + fitDensitySD  + "\t" + avgDensitySD 
					+ "\t" + fitSizeFreqSkew  + "\t" + sizeFreqSkew + "\t" + fitSizeFreqKurt + "\t" + sizeFreqKurt
					+ "\t"+ fitSubExp  + "\t" + propExp+ "\t" + fitSubCryp  + "\t" + propCryp
					+ "\t"+ dispersalDist+"\t"+ survType +"\t"+ expSurvOffset
					+"\t"+crypSurvOffset+"\t"+siteSurvOffset+"\t"+ larvaeMortality+"\t"+siteCover+"\t"+expPref+"\t"+crypPref
					+"\t"+expCover+"\t"+crypCover+"\t"+adultSpatial+"\t"+desperateLarvaOffset+"\t"+settleMortality); 

/*			for (int i = 0; i < numTransects; i++) {
				for (int j = 1; j < numQuadrats; j++) {
			
					outFilePatterns.print(quadratDensity[i][j] 
					        + "\t" + quadratCoralCover[i][j]+ "\t" + twoMeterCoralCover[i][j]+ "\t" 
							+ quadratSizeFreq[i][j][0]+ "\t" 
							+ quadratSizeFreq[i][j][1]+ "\t" + quadratSizeFreq[i][j][2]+ "\t" + quadratSizeFreq[i][j][3]+ "\t" 
							+ quadratSizeFreq[i][j][4] + "\t" + quadratSizeFreq[i][j][5] + "\t");
				}
			}
			outFilePatterns.println(); 
*/			
		} // synchronized lock
	}








//	public static float[][] getSurvSDArray() {
//		return survSDArray;
//	}


	public static int getNumTransects() {
		return numTransects;
	}




	public static int getNumQuadrats() {
		return numQuadrats;
	}




	public static int getNumReefs() {
		return numReefs;
	}




	public static ArrayList<Point> getCoopersReefCoords() {
		return coopersReefCoords;
	}




	public static ArrayList<Point> getMegansReefCoords() {
		return megansReefCoords;
	}




	public static ArrayList<Point> getMk14ReefCoords() {
		return mk14ReefCoords;
	}




	public static ArrayList<Point> getMk16ReefCoords() {
		return mk16ReefCoords;
	}




	public static float[] getSiteCoverArray() {
		return siteCoverArray;
	}






	public static float[][] getSizeFreqArray() {
		return sizeFreqArray;
	}




	public static float[][] getRugosityCoverArray() {
		return rugosityCoverArray;
	}




	public static float[][][][] getSubCoverArray() {
		return subCoverArray;
	}




	public static float[][] getSubCoverArraySimple() {
		return subCoverArraySimple;
	}


	public static float[] getSurvRatesArray() {
		return survRatesArray;
	}

	public static float[] getSurvPropArray() {
		return survPropArray;
	}



	public static float[] getSurvRatesHillArray() {
		return survRatesHillArray;
	}


	public static float[][] getGrowRatesArray() {
		return growRatesArray;
	}




	public static float[][] getGrowSDArray() {
		return growSDArray;
	}




	public static int getGridWidth() {
		return gridWidth;
	}




	public static int getGridHeight() {
		return gridHeight;
	}




	public static int getDispersalRange() {
		return dispersalRange;
	}




	public static float[][] getConnectivityMatrix0() {
		return connectivityMatrix0;
	}




	public static float[][] getConnectivityMatrix1() {
		return connectivityMatrix1;
	}




	public static float[][] getConnectivityMatrix2() {
		return connectivityMatrix2;
	}




	public static float[][] getConnectivityMatrix3() {
		return connectivityMatrix3;
	}


	public static int[][] getParamArray() {
		return paramArray;
	}


	public static float[][][] getOrientCoverArray() {
		return orientCoverArray;
	}


	public static void setSiteCoverArray(float[] siteCoverArray) {
		RecruitmentMainElast.siteCoverArray = siteCoverArray;
	}


	public static void setSubCoverArraySimple(float[][] subCoverArraySimple) {
		RecruitmentMainElast.subCoverArraySimple = subCoverArraySimple;
	}

}

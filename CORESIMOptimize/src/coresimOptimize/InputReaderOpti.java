package coresimOptimize;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class InputReaderOpti {

	private static final int[][] paramArray = new int[5][3]; 
	private static final double[][] paramArrayDoubles = new double[6][3]; 
	private /*final*/ static int N_CPUS;
	private static double fecundity; 

	private static String batchFileID; 
	private static int reefNumber; 
	private static int popnSize; 
	private static int generations; 
	private static double desperateLarvae; 

	// input data from ReefDataInput.txt
	private static final int numReefs = 4; 
	private static final float[] siteCoverArray =  new float[numReefs];  
	private static final float[][] sizeFreqArray =  new float[numReefs][5];  
	
	private static final float[][] rugosityCoverArray = new float[numReefs][3]; // 4 reefs, 3 rugosities (high, mid, low)
	private static final float[][][] orientCoverArray = new float[numReefs][3][2]; // 4 reefs, 3 rugosities, 2 orientations (up, vert)
	private static final float[][][][] subCoverArray = new float[numReefs][3][2][2]; // 4 reefs, 3 rugosities, 2 orientations, 2 subTypes (exp, cryp)
	private static final float[][] subCoverArraySimple = new float[numReefs][2]; // 4 reefs, 2 subTypes (exp, cryp)
	
	private static final float[] survRatesArray =  new float[16];  // 9 total OFFSETS by 15 total time periods
	private static final float[] survPropArray =  new float[16];  // 9 total OFFSETS by 15 total time periods
	private static final float[] survRatesHillArray =  new float[16];  // 9 total OFFSETS by 15 total time periods
//	private static final float[][] survSDArray =  new float[8][15];  
	private static final float[][] growRatesArray =  new float[numReefs][6];  
	private static final float[][] growSDArray =  new float[numReefs][6];  

	
	// arraylists of points for where reef habitat is located
	private static final ArrayList<Point> coopersReefCoords = new ArrayList<Point>();
	private static final ArrayList<Point> megansReefCoords = new ArrayList<Point>();
	private static final ArrayList<Point> mk14ReefCoords = new ArrayList<Point>();
	private static final ArrayList<Point> mk16ReefCoords = new ArrayList<Point>();

	
	private static final int gridWidth=320, gridHeight=320; 
	private static final int dispersalRange = 10080; // here, would be 10080 pixels, or 1.008km; this is total size of larger area around simulation landscape of 320x320
	private static final int numDispDistributions = 4; 
	private static final float[][] connectivityMatrix0 = new float[((int)(gridWidth/80))*((int)(gridWidth/80))][((int)(dispersalRange/80))*((int)(dispersalRange/80))]; 
	private static final float[][] connectivityMatrix1 = new float[((int)(gridWidth/80))*((int)(gridWidth/80))][((int)(dispersalRange/80))*((int)(dispersalRange/80))]; 
	private static final float[][] connectivityMatrix2 = new float[((int)(gridWidth/80))*((int)(gridWidth/80))][((int)(dispersalRange/80))*((int)(dispersalRange/80))]; 
	private static final float[][] connectivityMatrix3 = new float[((int)(gridWidth/80))*((int)(gridWidth/80))][((int)(dispersalRange/80))*((int)(dispersalRange/80))]; 

	private static int loopCounter; 
	
	/**
	 * Default parameter values
	 */
	static final int dispersalDist=0; // dispersal ditribution setting, 0-3 corresponding to beta distributions values (see Recruiter.alphaArray and Recruiter.betaArray)
	static final double ccaSubPref=0.6;
	static final double bareSubPref=0.2;
	static final double turfSubPref=0.1;
	static final double bareSurvOffset=0.0;
	static final double ccaSurvOffset=0.0;
	static final double turfSurvOffset=0.0;
	static final double siteSurvOffset=0.0;
	static final double rugosityRoughness=1.0;
	static final double settleMortality = .01; // mortality incurred by larvae while looking for settlment site (1% per step) 


	private static File fFile; // input file reader

	
	
/**
 * Method to read in parameter file (ParamFileXX.txt), where XX is the batchFileID number
 * @param paramList
 */
	void readParamFile(String paramList){

		//System.out.println("Read batch parameter file");

		String aFileName = paramList;
		fFile = new File(aFileName);  

		try {
			//first use a Scanner to get each line
			loopCounter = 0; 
			Scanner scanner = new Scanner(fFile);
			while ( scanner.hasNextLine() ){
				processLineParameters(scanner.nextLine() );
				loopCounter++; 
			}
			scanner.close();
		}
		catch (IOException ex){
			System.out.println(" fail = "+ex);
		}

		// Test for param file input 
/*				for (int j = 0; j < 6; j++) {
					System.out.println(paramArray[j][0] +"\t" + paramArray[j][1]  +"\t" + paramArray[j][2]); 
				}
				for (int j = 0; j < 2; j++) {
					System.out.println(paramArrayDoubles[j][0] +"\t" + paramArrayDoubles[j][1]  +"\t" + paramArrayDoubles[j][2]); 
				}
*/				
				
	}

	void processLineParameters(String aLine){
		//use a second Scanner to parse the content of each line 
		Scanner scanner = new Scanner(aLine);
		scanner.useDelimiter("\t");
		if ( scanner.hasNext() ){

			if (loopCounter == 0){
				String temp1 = scanner.next(); // do nothing with this; is string for name
				reefNumber = scanner.nextInt();  
			}
			else if (loopCounter == 1) { 		// do nothing with these (column headers)
				String temp2 = scanner.next();
				popnSize = scanner.nextInt(); 
			}
			else if (loopCounter == 2) { 		// do nothing with these (column headers)
				String temp3 = scanner.next();
				generations = scanner.nextInt(); 
			}
			else if (loopCounter == 3) {
				String temp3 = scanner.next();
				desperateLarvae = scanner.nextFloat(); 
				
			}

			
		}// end 2nd scanner 
		scanner.close();

	} // end of processLine method

	

	/** 
	 * These methods input the reef habitat arrays which hold information on where reef habitat versus non-habitat are
	 */
	void readReefHabitatFile(int reefNumber2){

	//	System.out.println("Read reef habitat file");

		String aFileName = "ReefHabitat"+reefNumber2+".txt";
		fFile = new File(aFileName);  

		try {
			//first use a Scanner to get each line
			loopCounter = 0; 
			Scanner scanner = new Scanner(fFile);
			while ( scanner.hasNextLine() ){
				processLineReefHabitat(reefNumber2, scanner.nextLine() );
				loopCounter++; 

			}
			scanner.close();
		}
		catch (IOException ex){
			System.out.println(" fail = "+ex);
		}
	}

	void processLineReefHabitat(int reefNumber2, String aLine){
		//use a second Scanner to parse the content of each line 
		int temp = 0; 
		Scanner scanner = new Scanner(aLine);
		scanner.useDelimiter("\t");
		if ( scanner.hasNext() ){

			for (int j = 0; j < 126; j++) {

				temp = scanner.nextInt(); 

				if((j >= 61) && (j <= 64) && (loopCounter >= 61) && (loopCounter <= 64) ){} // do nothing -- we don't want these points
				else { // here, add all reef point locations (8x8m cell size) which are outside the focal landscape (32x32m) but within 1.08x1.08km area

					if(temp == 0 || temp == 1){
						if (reefNumber2 == 0) coopersReefCoords.add(new Point(j,loopCounter)); 
						if (reefNumber2 == 1) megansReefCoords.add(new Point(j,loopCounter)); 
						if (reefNumber2 == 2) mk14ReefCoords.add(new Point(j,loopCounter)); 
						if (reefNumber2 == 3) mk16ReefCoords.add(new Point(j,loopCounter)); 
					} 
				} // end "else" statement
			} // end loop through 126 elements in reef habitat input files
		}// end 2nd scanner 
		scanner.close();

	} // end of processLine method





	/**
	 * Methods to read in connectivity matrices
	 * @param connFileNumber
	 */
	void readConnectivityFile(int connFileNumber){

		//System.out.println("Read connectivity file");

		String aFileName = "ConnectivityMatrix"+connFileNumber+".txt";
		fFile = new File(aFileName);  

		try {
			//first use a Scanner to get each line
			loopCounter = 0; 
			Scanner scanner = new Scanner(fFile);
			while ( scanner.hasNextLine() ){
				processLineConnectivity(connFileNumber, scanner.nextLine() );
				loopCounter++; 

			}
			scanner.close();
		}
		catch (IOException ex){
			System.out.println(" fail = "+ex);
		}
	}

	void processLineConnectivity(int connFileNumber, String aLine){
		//use a second Scanner to parse the content of each line 
		Scanner scanner = new Scanner(aLine);
		scanner.useDelimiter("\t");
		if ( scanner.hasNext() ){

			for (int j = 0; j < 16; j++) {
				if (connFileNumber == 0)connectivityMatrix0[j][loopCounter] = scanner.nextFloat();  
				if (connFileNumber == 1)connectivityMatrix1[j][loopCounter] = scanner.nextFloat();
				if (connFileNumber == 2)connectivityMatrix2[j][loopCounter] = scanner.nextFloat();
				if (connFileNumber == 3)connectivityMatrix3[j][loopCounter] = scanner.nextFloat();

			}
		}
		scanner.close();
	} // end of processLine method




	void readSurvivalFile() {
	
		
		//System.out.println("Read survival rates matrix file");


		String aFileName = "SurvivalRates.txt";
		fFile = new File(aFileName);  

		try {
			//first use a Scanner to get each line
			Scanner scanner = new Scanner(fFile);
			while ( scanner.hasNextLine() ){
				processLineSurvival( scanner.nextLine() );

			}
			scanner.close();
		}
		catch (IOException ex){
			System.out.println(" fail = "+ex);
		}
		
		// Test for survival rates input 
/*		for (int j = 0; j < 16; j++) {
		
			System.out.println(survRatesArray[j][0] +"\t" + survRatesArray[j][1]  +"\t" + survRatesArray[j][2]  +"\t" + survRatesArray[j][3]
           +"\t" + survRatesArray[j][4]  +"\t" + survRatesArray[j][5] +"\t" + survRatesArray[j][6]  +"\t" + survRatesArray[j][7] 
            +"\t" + survRatesArray[j][8] ); 
		}
*/		
		
	}

	void processLineSurvival(String aLine){
		//use a second Scanner to parse the content of each line 
		Scanner scanner = new Scanner(aLine);
		scanner.useDelimiter("\t");
		if ( scanner.hasNext() ){

			int ID = scanner.nextInt();
			
			// Survival rates for Michalis Menten fit to data: (.993*age)/(Km+age)
			float survRateMM = scanner.nextFloat();
			float survRateHill = scanner.nextFloat();
			float survProp = scanner.nextFloat();
			
			survRatesArray[ID-1] = survRateMM; 
			survRatesHillArray[ID-1] = survRateHill; 
			survPropArray[ID-1] = survProp; 
		}

		scanner.close();
		

	} // end of processLine method

	

	
	
	/**
	 * Methods to read in reef data (cover, rugosity, etc)
	 */
	void readInputFile(){

		//System.out.println("Read reef input file");


		String aFileName = "ReefDataInput.txt";
		fFile = new File(aFileName);  

		try {
			//first use a Scanner to get each line
			Scanner scanner = new Scanner(fFile);
			while ( scanner.hasNextLine() ){
				processLineInput( scanner.nextLine() );

			}
			scanner.close();
		}
		catch (IOException ex){
			System.out.println(" fail = "+ex);
		}
	}

	void processLineInput(String aLine){
		//use a second Scanner to parse the content of each line 
		Scanner scanner = new Scanner(aLine);
		scanner.useDelimiter("\t");
		if ( scanner.hasNext() ){

			//Site	DistToShore	SiteCover	RecruitDensity	P500mArea	T500mArea	P1kmArea	
			// T1kmArea	SizeFreq10	SizeFreq20	SizeFreq30	SizeFreq40	SizeFreq50	SizeFreq60	RugAvg	RugSE	
			//BareHigh	CCAHigh	OtherHigh	TurfHigh	BareMid	CCAMid	OtherMid	TurfMid	BareLow	CCALow	OtherLow	TurfLow

			
			int ID = scanner.nextInt();
			float siteCover = scanner.nextFloat();
			float sizeFreq10 = scanner.nextFloat();
			float sizeFreq20 = scanner.nextFloat();
			float sizeFreq30 = scanner.nextFloat();
			float sizeFreq40 = scanner.nextFloat();
			float sizeFreq50 = scanner.nextFloat();
			
			float expCover = scanner.nextFloat();
			float crypCover = scanner.nextFloat();

			float rugHigh = scanner.nextFloat();
			float rugMid = scanner.nextFloat();
			float rugLow = scanner.nextFloat();

			float upHigh = scanner.nextFloat();
			float vertHigh = scanner.nextFloat();
			float upMid = scanner.nextFloat();
			float vertMid = scanner.nextFloat();
			float upLow = scanner.nextFloat();
			float vertLow = scanner.nextFloat();

			float expHighUp = scanner.nextFloat();
			float crypHighUp= scanner.nextFloat();
			float expHighVert = scanner.nextFloat();
			float crypHighVert = scanner.nextFloat();
			float expMidUp = scanner.nextFloat();
			float crypMidUp = scanner.nextFloat();
			float expMidVert = scanner.nextFloat();
			float crypMidVert = scanner.nextFloat();
			float expLowUp = scanner.nextFloat();
			float crypLowUp = scanner.nextFloat();
			float expLowVert = scanner.nextFloat();
			float crypLowVert = scanner.nextFloat();
			
			float growRate1 = scanner.nextFloat();
			float growRate2 = scanner.nextFloat();
			float growRate3 = scanner.nextFloat();
			float growRate4 = scanner.nextFloat();
			float growRate5 = scanner.nextFloat();
			float growRate6 = scanner.nextFloat();

			float growSD1 = scanner.nextFloat();
			float growSD2 = scanner.nextFloat();
			float growSD3 = scanner.nextFloat();
			float growSD4 = scanner.nextFloat();
			float growSD5 = scanner.nextFloat();
			float growSD6 = scanner.nextFloat();


			//id[ID-1] = ID;
			//distanceToShore[ID-1] =  distanceShore; 
			siteCoverArray[ID-1] =  siteCover; 
			//recruitDensityArray[ID-1] =  recruitDensity; 

			// For the sizeFreqArray below, add them up so is cumulative probabilities
			sizeFreqArray[ID-1][0] = sizeFreq10;
			sizeFreqArray[ID-1][1] = sizeFreq10 + sizeFreq20;
			sizeFreqArray[ID-1][2] = sizeFreq10 + sizeFreq20 + sizeFreq30;
			sizeFreqArray[ID-1][3] = sizeFreq10 + sizeFreq20 + sizeFreq30 + sizeFreq40;
			sizeFreqArray[ID-1][4] = sizeFreq10 + sizeFreq20 + sizeFreq30 + sizeFreq40 + sizeFreq50;

			rugosityCoverArray[ID-1][2] = rugHigh;
			rugosityCoverArray[ID-1][1] = rugMid;
			rugosityCoverArray[ID-1][0] = rugLow;

			orientCoverArray[ID-1][2][0] = upHigh; 
			orientCoverArray[ID-1][2][1] = vertHigh;
			orientCoverArray[ID-1][1][0] = upMid; 
			orientCoverArray[ID-1][1][1] = vertMid;
			orientCoverArray[ID-1][0][0] = upLow; 
			orientCoverArray[ID-1][0][1] = vertLow;
			
			subCoverArraySimple[ID-1][0] = expCover;
			subCoverArraySimple[ID-1][1] = crypCover;
			

			// For the subCoverArray below, add them up so is cumulative probabilities; this will be used to simplify allocation of substrates
			subCoverArray[ID-1][2][0][0] = expHighUp;
			subCoverArray[ID-1][2][0][1] = crypHighUp;
			subCoverArray[ID-1][2][1][0] = expHighVert;
			subCoverArray[ID-1][2][1][1] = crypHighVert;
			subCoverArray[ID-1][1][0][0] = expMidUp;
			subCoverArray[ID-1][1][0][1] = crypMidUp;
			subCoverArray[ID-1][1][1][0] = expMidVert;
			subCoverArray[ID-1][1][1][1] = crypMidVert;
			subCoverArray[ID-1][0][0][0] = expLowUp;
			subCoverArray[ID-1][0][0][1] = crypLowUp;
			subCoverArray[ID-1][0][1][0] = expLowVert;
			subCoverArray[ID-1][0][1][1] = crypLowVert;

			growRatesArray[ID-1][0] = growRate1; 
			growRatesArray[ID-1][1] = growRate2; 
			growRatesArray[ID-1][2] = growRate3; 
			growRatesArray[ID-1][3] = growRate4; 
			growRatesArray[ID-1][4] = growRate5; 
			growRatesArray[ID-1][5] = growRate6; 

			growSDArray[ID-1][0] = growSD1; 
			growSDArray[ID-1][1] = growSD2; 
			growSDArray[ID-1][2] = growSD3; 
			growSDArray[ID-1][3] = growSD4; 
			growSDArray[ID-1][4] = growSD5; 
			growSDArray[ID-1][5] = growSD6; 
		}

		scanner.close();
	} // end of processLine method


	
	public static float[] getSiteCoverArray() {
		return siteCoverArray;
	}


	public static float[][] getSizeFreqArray() {
		return sizeFreqArray;
	}

	public static float[][] getRugosityCoverArray() {
		return rugosityCoverArray;
	}

	public static float[][][] getOrientCoverArray() {
		return orientCoverArray;
	}

	public static float[][][][] getSubCoverArray() {
		return subCoverArray;
	}

	public static float[] getSurvRatesArray() {
		return survRatesArray;
	}

	public static float[] getSurvRatesHillArray() {
		return survRatesHillArray;
	}

	public static float[] getSurvPropArray() {
		return survPropArray;
	}

	public static float[][] getGrowRatesArray() {
		return growRatesArray;
	}

//	public static float[][] getSurvSDArray() {
//		return survSDArray;
//	}

	public static float[][] getGrowSDArray() {
		return growSDArray;
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

	public static int getDispersalDist() {
		return dispersalDist;
	}

	public static double getCcaSubPref() {
		return ccaSubPref;
	}

	public static double getBareSubPref() {
		return bareSubPref;
	}

	public static double getTurfSubPref() {
		return turfSubPref;
	}

	public static double getBareSurvOffset() {
		return bareSurvOffset;
	}

	public static double getCcaSurvOffset() {
		return ccaSurvOffset;
	}

	public static double getTurfSurvOffset() {
		return turfSurvOffset;
	}

	public static double getSiteSurvOffset() {
		return siteSurvOffset;
	}

	public static double getRugosityRoughness() {
		return rugosityRoughness;
	}

	public static double getSettleMortality() {
		return settleMortality;
	}

	public static int[][] getParamArray() {
		return paramArray;
	}

	public static String getBatchFileID() {
		return batchFileID;
	}
	public static double[][] getParamArrayDoubles() {
		return paramArrayDoubles;
	}

	public static float[][] getSubCoverArraySimple() {
		return subCoverArraySimple;
	}

	public static int getN_CPUS() {
		return N_CPUS;
	}

	public static double getFecundity() {
		return fecundity;
	}

	
	public static int getReefNumber() {
		return reefNumber;
	}

	public static int getPopnSize() {
		return popnSize;
	}

	public static int getGenerations() {
		return generations;
	}

	public static double getDesperateLarvae() {
		return desperateLarvae;
	}

}

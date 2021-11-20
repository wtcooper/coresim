package coresimElasticity;

/*	Notes:
 * 		(1) need to change RecruitmentMainElast and TimeKeeperElast back to elasticity instead of sensitivity analysis
 * 		(2) need to change elasticity back so is only positive values (0-95%) (so dont have weird non-linear functions
 * 		
 * 
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import org.apache.commons.math.stat.regression.SimpleRegression;

public class ElasticityCalculations {


	String fileName = "RecruitModelPatterns6.23.txt"; 
	int numRows = 118000; 


	int loopCounter = 0; 
	int[] reefNumber = new int [numRows]; 
	double[] fitTotal = new double[numRows]; 
	double[] fitDensity = new double[numRows]; 
	double[] fitDensitySD = new double[numRows]; 
	double[] fitSFSkew = new double[numRows]; 
	double[] fitSFKurt = new double[numRows]; 
	double[] fitSubExp = new double[numRows]; 
	double[] fitSubCryp = new double[numRows]; 
	String[] paramName = new String[numRows];
	double [] paramValue = new double[numRows];
	int[] replicate = new int[numRows];

	String paramNamePass = null; 

	int tempReefNum; 
	String tempParamName;
	double tempParamValue; 
	// map of all parameter values as key, with a list of all replicate values for that param value
	HashMap<Double, ArrayList<Double>> tempFitPatternMap = new HashMap<Double,ArrayList<Double>>(); 
	// map of all parameter values as key, with a double average value for that value
	HashMap<Double, Double> tempFitAverage = new HashMap<Double, Double>();
	// map of all parameter values as key, with a double different value for that value
	HashMap<Double, Double> tempFitDifference = new HashMap<Double, Double>(); 

	ArrayList<Double> paramValues = new ArrayList<Double>(); 

	Double[] maxFitValue = new Double[4]; 
	
	String patternName = null; 
	double maxFitIndex;
	double averageFitValue; 
	//double maxFitValue; 
	double totalFitValue; 
	double differenceValue; 

	double regressSlope; 

	private static File fFile; // input file reader
	public PrintWriter outFile = null; 



	public void run(){

		// open print writer
		try { outFile= new PrintWriter(new FileWriter("ElasticityCalcs.txt", true));
		} catch (IOException e) {e.printStackTrace();}

		for (int i = 0; i<7; i++){ // loop over 7 total patterns
			//System.out.println("beginning pattern number: " + i);

			for (int x = 0; x < 2; x++ ){
				
				for (int j = 0; j < 4; j++ ){ // loop over 4 total reefs
					tempReefNum = j; 
					maxFitValue[tempReefNum] = -100.0; // reset the maxFitValue here so get highest value per site overall 
					//System.out.println("beginning reef number: " + j);

					for (int k = 0; k < 10; k++) { // loop over 10 total parameters
						//					System.out.println("beginning parameter number: " + k);


						if (k == 0) paramNamePass = "crypCover";
						else if (k == 1) paramNamePass = "crypPref";
						else if (k == 2) paramNamePass = "crypSurvOff";
						else if (k == 3) paramNamePass = "dispDist";
						else if (k == 4) paramNamePass = "expCover";
						else if (k == 5) paramNamePass = "expPref";
						else if (k == 6) paramNamePass = "expSurvOff";
						else if (k == 7) paramNamePass = "larvMort";
						else if (k == 8) paramNamePass = "siteCover";
						else if (k == 9) paramNamePass = "survType";

						if (i==0) {
							patternName="fitTotal";
							setTempLists(fitTotal, j, paramNamePass);
							doCalculations(x); 
						}
						else if (i==1) {
							patternName="fitDensity";
							setTempLists(fitDensity, j, paramNamePass);
							doCalculations(x); 
						}
						else if (i==2) {
							patternName="fitDensitySD";
							setTempLists(fitDensitySD, j, paramNamePass);
							doCalculations(x); 
						}
						else if (i==3) {
							patternName="fitSFSkew";
							setTempLists(fitSFSkew, j, paramNamePass);
							doCalculations(x); 
						}
						else if (i==4) {
							patternName="fitSFKurt";
							setTempLists(fitSFKurt, j, paramNamePass);
							doCalculations(x); 
						}
						else if (i==5) {
							patternName="fitSubExp";
							setTempLists(fitSubExp, j, paramNamePass);
							doCalculations(x); 
						}
						else if (i==6) {
							patternName="fitSubCryp";
							setTempLists(fitSubCryp, j, paramNamePass);
							doCalculations(x); 
						}

						paramValues.clear();
						tempFitPatternMap.clear();
						tempFitAverage.clear();
						tempFitDifference.clear();
					}
				}
			}
		}
		outFile.close(); 
	}


	public void doCalculations(int x){


		if (x == 0) getAverages1(); // computes the averages and get highest value per each reef

		else{
			getAverages(); // computes the average again so can then compute difference against highest value per reef
			getDifferenceValues();
			calculateRegressionSlope(); 
			outputResults();
		}
	}

	/**	Will set the temporary hash map which holds all the replicates for a single single pattern at a single reef
	 * and at a single parameter type.  The key to the map will be the parameter value, and for each parameter value,
	 * will be the replicate number of values
	 * 
	 * Then for the averages, can take out a single 
	 * 
	 * @param fitPattern
	 * @param reefNum
	 * @param paramNamePass
	 */
	public void setTempLists(double[] fitPattern, int reefNum, String paramNamePass){

		for (int i=0; i<numRows; i++ ){

			if ((reefNumber[i] == reefNum) && (paramName[i].equals(paramNamePass)) && (paramValue[i] >=0)){
				//System.out.println("enter if statement in setTempLists"); 
				ArrayList<Double> list = (ArrayList<Double>) tempFitPatternMap.get(paramValue[i]	);
				if (list == null) {
					list = new ArrayList<Double>();
					tempFitPatternMap.put(paramValue[i], list);
					paramValues.add(paramValue[i]); 
				}
				list.add(fitPattern[i]);
				//System.out.println("fit pattern value: " + fitPattern[i]); 
			}
		}
	}


	public void getAverages(){

		for (int i=0; i<paramValues.size(); i++){
			//maxFitValue = -100; 
			totalFitValue = 0; 
			ArrayList<Double> tempValues = tempFitPatternMap.get(paramValues.get(i)); 
			for (int j=0; j<tempValues.size(); j++){
				totalFitValue += tempValues.get(j);
			}
			averageFitValue = totalFitValue/tempValues.size(); 
			tempFitAverage.put(paramValues.get(i), averageFitValue);

			//if (averageFitValue > maxFitValue) maxFitValue = averageFitValue; 

		}
	}

	public void getAverages1(){

		for (int i=0; i<paramValues.size(); i++){
			totalFitValue = 0; 
			ArrayList<Double> tempValues = tempFitPatternMap.get(paramValues.get(i)); 
			for (int j=0; j<tempValues.size(); j++){
				totalFitValue += tempValues.get(j);
			}
			averageFitValue = totalFitValue/tempValues.size(); 
			tempFitAverage.put(paramValues.get(i), averageFitValue);

			if (averageFitValue > maxFitValue[tempReefNum]) maxFitValue[tempReefNum] = averageFitValue; 

		}
	}

	public void getDifferenceValues(){
		for (int i=0; i<paramValues.size(); i++){
			differenceValue = maxFitValue[tempReefNum] - tempFitAverage.get(paramValues.get(i)) ; 
			tempFitDifference.put(paramValues.get(i), differenceValue); 
			//System.out.println(patternName + "\t" + tempReefNum + "\t" + paramNamePass + "\t" +tempFitAverage.get(paramValues.get(i)) + "\t" + tempFitDifference.get(paramValues.get(i)));  
		}
	}


	public void calculateRegressionSlope() {

		SimpleRegression regression = new SimpleRegression();

		for (int i=0; i<paramValues.size(); i++){
			regression.addData(paramValues.get(i), tempFitDifference.get(paramValues.get(i))); 
		}

		regressSlope = regression.getSlope(); 

	}


	public void outputResults(){
		outFile.println(patternName + "\t" + tempReefNum + "\t" + paramNamePass + "\t" + regressSlope); 
	}



	void readFile(){

		System.out.println("data input beginning..."); 
		fFile = new File(fileName);  

		try {
			Scanner scanner = new Scanner(fFile);
			while ( scanner.hasNextLine() ){
				processLine(scanner.nextLine() );
			}
			scanner.close();
		}
		catch (IOException ex){
			System.out.println(" fail = "+ex);
		}

		System.out.println("data input done.");
	}

	void processLine(String aLine){
		Scanner scanner = new Scanner(aLine);
		scanner.useDelimiter("\t");
		if ( scanner.hasNext() ){

			scanner.nextDouble();
			int id = scanner.nextInt(); // run number
			int reefNum = scanner.nextInt(); 
			double fTotal = scanner.nextDouble()-10000;
			double fDen = scanner.nextDouble()-10000;
			//scanner.nextDouble();
			double fDensitySD = scanner.nextDouble()-10000;
			//scanner.nextDouble();
			double fSFSkew = scanner.nextDouble()-10000;
			//scanner.nextDouble();
			double fSFKurt = scanner.nextDouble()-10000;
			//scanner.nextDouble();
			double fSubExp = scanner.nextDouble()-10000;
			//scanner.nextDouble();
			double fSubCryp = scanner.nextDouble()-10000;
			//scanner.nextDouble();
			scanner.nextInt();
			scanner.nextInt();
			scanner.nextDouble();
			scanner.nextDouble();
			scanner.nextInt();
			scanner.nextInt();
			scanner.nextDouble();
			scanner.nextDouble();
			scanner.nextDouble();
			scanner.nextDouble();
			scanner.nextDouble();
			scanner.nextDouble();
			//scanner.nextInt();
			//scanner.nextDouble();
			//scanner.nextDouble();


			// these will be the new variables to add in Elasticity output
			// add the replicated easily -- make a call to RecruitmentMainElasticity.numSweeps in TimeKeeper
			// and output through PatternRecorder
			// for the other ones, do two new variables in TimeKeeper -- paramType and paramValue.  Then in 
			// setNextSupplyCell() code, set these values if a parameter != 0. E.g., 
			//if (expSurvOffsetProp == 0) expSurvOffset = expSurvOffsetOpti[reefNumber];
			//else {
			//		expSurvOffset = expSurvOffsetOpti[reefNumber]+expSurvOffsetOpti[reefNumber]*elasticityProp[expSurvOffsetProp-1]; 
			// 		paramType = "expSurvOffset";
			// 		paramValue = elasticityProp[expSurvOffsetProp-1]; 
			//}
			int repl = scanner.nextInt();
			String paramType = scanner.next(); 
			double paramV = scanner.nextDouble();

			reefNumber[loopCounter] = reefNum; 
			fitTotal[loopCounter] = fTotal;
			fitDensity[loopCounter] = fDen;
			fitDensitySD[loopCounter] = fDensitySD; 
			fitSFSkew[loopCounter] = fSFSkew;
			fitSFKurt[loopCounter] = fSFKurt;
			fitSubExp[loopCounter] = fSubExp;
			fitSubCryp[loopCounter] = fSubCryp; 
			paramName[loopCounter] = paramType;
			paramValue[loopCounter] = paramV;
			replicate[loopCounter] = repl; 

			loopCounter++; 
		}// end 2nd scanner 
		scanner.close();

	} // end of processLine method







	public static void main(String[] args) {
		ElasticityCalculations ec = new ElasticityCalculations();
		ec.readFile();
		ec.run();
	}
}

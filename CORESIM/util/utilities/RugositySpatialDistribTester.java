package utilities;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;
import coresim.FractalTerrain;

public class RugositySpatialDistribTester {

	/**
	 * @param args
	 */
	private int seed = (int) System.currentTimeMillis();
	private MersenneTwister m = new MersenneTwister(seed); 
	private Uniform uniform  = new Uniform(0, 100, m);  


	private int lod = 5;  		// here,  gridHeight/gridWidth = (2^lod)
	private int gridWidth = 32; 
	private int gridHeight = 32;
	private int reefNumber; 

	private float[][] rugosityCoverArray = {{0.169491525f,0.474576271f,0.355932203f},
			{0.068965517f,0.327586207f,0.603448276f},
			{0.409090909f,0.53030303f,0.060606061f},
			{0.166666667f,0.555555556f,0.277777778f}}; 

	private int[][] rugosityArray = new int[gridWidth][gridHeight];

	private double topo[][] = new double[gridWidth][gridHeight]; 
	private double topoAdjust[][] = new double[gridWidth][gridHeight]; 
	private int topoInt[][] = new int[gridWidth][gridHeight]; //integer array which will be where processing takes place

	private double roughness; // this is NOT value of H; here, is what Merlin recommends where can be from 0-infinity.  As increase, gets more random (i.e., opposite of H) 
	private float highProp, midProp, lowProp; // this is proportion of each Orientation to use

	private double tran1Corr, tran2Corr; 
	private double tran1Average, tran2Average, tran1Sum, tran2Sum;
	private double tran1SumSquaresCorr, tran2SumSquaresCorr, tran1Gamma, tran2Gamma, tran1SumSquaresAvg, tran2SumSquaresAvg;
	private double tran1Variance, tran2Variance; 
	private int transectNumbers[][] = new int [2][18]; 
	private int tran1StartX, tran1StartY, tran2StartX, tran2StartY; 

	private PrintWriter outFile=null;	

	RugositySpatialDistribTester (){

	}



	void testDistrib() {

		try {  outFile = new PrintWriter(new FileWriter("RugSpatialTester.txt", true));
		} catch (IOException e) {e.printStackTrace();	}


		reefNumber = 0; 

		for (int i = 0; i < 4; i++) {


			roughness = .1; 
			for (int j =0; j < 10; j++) {

				if (j<9){
					for (int replicates = 0; replicates < 1; replicates++){

						buildRugosity(); 
						runTransects();				// run simulated transects and get values

						outFile.println("ReefNum"+reefNumber + "\t" + roughness + "\t" + tran1Corr + "\t" + tran2Corr);

					}
					roughness = roughness + .2;
				}

				else { // set last one to do random distribution based
					roughness = 99; 

					for (int replicates = 0; replicates < 1; replicates++){

						for (int x = 0; x < gridWidth; x++){
							for (int y = 0; y < gridHeight; y ++){

								highProp = rugosityCoverArray[reefNumber][0];
								midProp = rugosityCoverArray[reefNumber][1];
								lowProp = rugosityCoverArray[reefNumber][2];

								double prob = uniform.nextDoubleFromTo(0, 1) ; 
								if (prob < lowProp) rugosityArray[x][y] = 0; 
								else if (prob < lowProp+midProp) rugosityArray[x][y] = 1;
								else if (prob < lowProp+midProp+highProp) rugosityArray[x][y] = 2;
							}
						}

						runTransects(); 
						outFile.println("ReefNum"+reefNumber + "\t" + roughness + "\t" + tran1Corr + "\t" + tran2Corr);
						
						DrawHabitat drawer = new DrawHabitat(gridWidth, gridHeight, 4, rugosityArray);
					   	drawer.setVisible(true);

					}
				}
			}
			reefNumber++; 
		}
		outFile.println(); 
		outFile.close();
	}




	void runTransects() {

		tran1Sum=0; tran2Sum=0; tran1SumSquaresCorr=0; tran2SumSquaresCorr=0; tran1SumSquaresAvg=0; tran2SumSquaresAvg=0; 

		tran1StartX = uniform.nextIntFromTo(0,gridWidth-1); 
		tran1StartY = uniform.nextIntFromTo(0,gridHeight-1); 

		// loop through all meter positions on transect
		for (int k = 0; k < 18; k++){
			transectNumbers[0][k] =  rugosityArray[tran1StartX][getWrapAround(tran1StartY + k)]; 

			//outFile.println("transect1" + "\t" + tran1StartX + "\t" + getWrapAround(tran1StartY + k) + "\t" + transectNumbers[0][k]);

			tran1Sum = tran1Sum + transectNumbers[0][k]; 
		}

		tran2StartX = getWrapAround(tran1StartX + uniform.nextIntFromTo(1,10)); 
		tran2StartY = getWrapAround(tran1StartY + uniform.nextIntFromTo(0, 6) - 3); 

		for (int k = 0; k < 18; k++){
			transectNumbers[1][k] =  rugosityArray[tran2StartX][getWrapAround(tran2StartY + k)]; 

			//outFile.println("transect2" + "\t" + tran2StartX + "\t" + getWrapAround(tran2StartY + k) + "\t" + transectNumbers[1][k]);

			tran2Sum = tran2Sum + transectNumbers[1][k]; 
		}

		// get AutoCorrelation Value for each transect 
		tran1Average = tran1Sum/18f; 
		tran2Average = tran2Sum/18f; 

		for (int k = 0; k < 17; k++){
			tran1SumSquaresCorr = tran1SumSquaresCorr + (transectNumbers[0][k] - tran1Average)*(transectNumbers[0][k+1] - tran1Average); 
			tran2SumSquaresCorr = tran2SumSquaresCorr + (transectNumbers[1][k] - tran2Average)*(transectNumbers[1][k+1] - tran2Average); 
		}

		for (int k = 0; k < 18; k++){
			tran1SumSquaresAvg = tran1SumSquaresAvg + (transectNumbers[0][k] - tran1Average)*(transectNumbers[0][k] - tran1Average); 
			tran2SumSquaresAvg = tran2SumSquaresAvg + (transectNumbers[1][k] - tran2Average)*(transectNumbers[1][k] - tran2Average); 
		}

		tran1Gamma = tran1SumSquaresCorr/17f; 
		tran2Gamma = tran2SumSquaresCorr/17f; 
		tran1Variance = tran1SumSquaresAvg/18f;
		tran2Variance = tran2SumSquaresAvg/18f; 
		tran1Corr = tran1Gamma/tran1Variance;
		tran2Corr = tran2Gamma/tran2Variance; 

	}


	void buildRugosity() {

		// inputs the proportion of reef in different rugosity categories: high (0-0.55), mid (.55-.75), and low (.75-1), 
		// as measured via chain transects in substrate surveys
		highProp = rugosityCoverArray[reefNumber][0];
		midProp = rugosityCoverArray[reefNumber][1];
		lowProp = rugosityCoverArray[reefNumber][2];



		int counterArray[] = new int[500];  //an array which will count the total number of each integer height measurement in the landscape
		double minimum = 100000;
		double maximum = -100;
		double heightScale;
		double sum = 0;  // tracks the sum of the height values on the entire grid; i.e., counts the number of pixels at a particular height from 0-499
		int highCutOff = 0, lowCutOff = 0, midCutOff = counterArray.length;  // stores the cutoff height value based on the actual heights on the landscape and the proportion of each orientation


		// call to the terrain building classes to return an array of doubles with the topography numbers
		FractalTerrainTester ft = new FractalTerrainTester(lod, roughness, m, uniform);
		topo = ft.computeTerrain(); 


		//*************************************************************************************
		// Code below developed from Thorsten Wiegand Fortran code used in Wiegand et al. 1999
		//*************************************************************************************

		// sets the counter array to zero
		for (int i = 0; i<counterArray.length; i++) {
			counterArray[i] = 0;
		}

		// finds the minimum value in the height measurements
		for (int i = 0; i < gridWidth; i++) {
			for (int j = 0; j < gridHeight; j++) {
				if (topo[i][j] < minimum) { minimum = topo[i][j]; } 
			}
		}

		// shifts the range of the topo array to the mininum value being zero
		for (int i = 0; i < gridWidth; i++) {
			for (int j = 0; j < gridHeight; j++) {
				topoAdjust[i][j] = (topo[i][j]-minimum); 
			}
		}

		// finds the maximum value in the height measurements
		for (int i = 0; i < gridWidth; i++) {
			for (int j = 0; j < gridHeight; j++) {
				if (topoAdjust[i][j] > maximum) { maximum = topoAdjust[i][j]; } 
			}
		}

		// re-scales the range of the array between 0 and 499
		for (int i = 0; i < gridWidth; i++) {
			for (int j = 0; j < gridHeight; j++) {
				heightScale = (topoAdjust[i][j])/maximum; 
				topoInt[i][j] = (int) (499*heightScale);
				counterArray[topoInt[i][j]] = counterArray[topoInt[i][j]] + 1;
			}
		}

		// sets the cut-off height value for the different rugosity categories so have appropriate proportion of each on landscape
		for (int i = 0; i<counterArray.length; i++) {
			sum = (float) sum + counterArray[i];
			if (sum/((float)(gridWidth*gridHeight)) <= lowProp) {lowCutOff = i;}
			if (sum/((float)(gridWidth*gridHeight)) <= midProp + lowProp) {midCutOff = i;}
			if (sum/((float)(gridWidth*gridHeight)) <= highProp +midProp +lowProp) {highCutOff = i;}
		}

		// sets the orientation array to either 0, 1, or 2 (low, mid, and high rugosity, respectively)
		for (int i = 0; i < gridWidth; i++) {
			for (int j = 0; j < gridHeight; j++) {
				if (topoInt[i][j] <= lowCutOff) { rugosityArray[i][j] = 0; }
				else if (topoInt[i][j] <= midCutOff) { rugosityArray[i][j] = 1; }
				else if (topoInt[i][j] <= highCutOff) { rugosityArray[i][j] = 2; }
			}
		}



		// Call DrawHabitat to visualize the rugosity
			DrawHabitat drawer = new DrawHabitat(gridWidth, gridHeight, 4, rugosityArray);
		   	drawer.setVisible(true);

	}




	int getWrapAround(int x){

		int returnValue = x; 
		if (x >= gridWidth) returnValue = x-gridWidth;
		if (x < 0) returnValue = gridWidth+x;
		return returnValue; 
	}


	public static void main(String[] args) {
		// TODO Auto-generated method stub

		RugositySpatialDistribTester rt = new RugositySpatialDistribTester(); 
		rt.testDistrib(); 
	}

}

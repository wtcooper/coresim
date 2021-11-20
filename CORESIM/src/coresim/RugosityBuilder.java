package coresim;

import java.util.Random;

import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;


public class RugosityBuilder {

	private TimeKeeper tk; 
	private MersenneTwister m; 
	private Uniform uniform; 

	private long seed = System.currentTimeMillis();

	private int numReefs = 4; 

	private int lod = 5;  		// here,  gridHeight/gridWidth = (2^lod)
	private int gridWidth = 32; 
	private int gridHeight = 32;
	private int reefNumber; 

	private float[][] rugosityCoverArray;
	private float[][][] orientationCoverArray; 
	private int[][] rugosityArray = new int[gridWidth][gridHeight]; // 0=low, 1=mid, 2=high
	private int[][] orientationArray = new int[gridWidth*10][gridHeight*10];  // 0=up, 1=vert, 2=other (i.e., down)

	private double topo[][] = new double[gridWidth][gridHeight]; 
	private double topoAdjust[][] = new double[gridWidth][gridHeight]; 
	private int topoInt[][] = new int[gridWidth][gridHeight]; //integer array which will be where processing takes place

	private double roughness; // this is NOT value of H; here, is what Merlin recommends where can be from 0-infinity.  As increase, gets more random (i.e., opposite of H) 
	private float highProp, midProp, lowProp; // this is proportion of each Orientation to use


	RugosityBuilder(TimeKeeper tk) {
		this.tk = tk;
		m = tk.getM(); 
		reefNumber = tk.getReefNumber(); 
		rugosityCoverArray = RecruitmentMain.getRugosityCoverArray();
		orientationCoverArray = RecruitmentMain.getOrientCoverArray(); 
		roughness = tk.getRoughness();

		uniform = tk.getUniform();


	}


	/**	Distributes categorical rugosity measurements onto the landscape; here, three categories (high, mid, low).
	 * 	Methods are based on Thorsten Wiegand's 2D landscape generating approach (Wiegand et al. 1999; The American Naturalist 154(6), pg 605)
	 * 	However, instead of using his Gaussian function approach, use the diamond-square algorithm to create 3D landscape (FractalTerrain.class).
	 * 	A java version of his Guassian approach is available, but this produce less-realistic results (very "circular") compared to diamond-square.
	 * 	After diamond-square, use his methods (interpreted by obtaining his Fortran code) to allocate categories by appropriate proportions for each
	 * 
	 */
	void buildRugosity() {

		// inputs the proportion of reef in different rugosity categories: high (0-0.55), mid (.55-.75), and low (.75-1), 
		// as measured via chain transects in substrate surveys
		highProp = rugosityCoverArray[reefNumber][2];
		midProp = rugosityCoverArray[reefNumber][1];
		lowProp = rugosityCoverArray[reefNumber][0];


		// If roughness is 0, do as fully random distribution;
		// this is actually inverse of roughness value, where higher values of roughness approach randomness
		if (roughness == 0) {

			for (int x = 0; x < gridWidth; x++){
				for (int y = 0; y < gridHeight; y ++){

					double prob = uniform.nextDoubleFromTo(0, 1) ; 
					if (prob < lowProp) rugosityArray[x][y] = 0; 
					else if (prob < lowProp+midProp) rugosityArray[x][y] = 1;
					else if (prob < lowProp+midProp+highProp) rugosityArray[x][y] = 2;
				}
			}
			
			setOrientation(); 

		}

		// When roughness is not zero, do as 
		else {
			int counterArray[] = new int[500];  //an array which will count the total number of each integer height measurement in the landscape
			double minimum = 100000;
			double maximum = -100;
			double heightScale;
			double sum = 0;  // tracks the sum of the height values on the entire grid; i.e., counts the number of pixels at a particular height from 0-499
			int highCutOff = 0, lowCutOff = 0, midCutOff = counterArray.length;  // stores the cutoff height value based on the actual heights on the landscape and the proportion of each orientation


			// call to the terrain building classes to return an array of doubles with the topography numbers
			FractalTerrain ft = new FractalTerrain(tk, lod, roughness, m);
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
					if (topoInt[i][j] <= lowCutOff) { rugosityArray[i][j] = 0; }  // low 
					else if (topoInt[i][j] <= midCutOff) { rugosityArray[i][j] = 1; }  // mid
					else if (topoInt[i][j] <= highCutOff) { rugosityArray[i][j] = 2; } // high
				}
			}
			
			setOrientation(); 

		}

		// Call DrawHabitat to visualize the rugosity
		//		DrawHabitat drawer = new DrawHabitat(gridWidth, gridHeight, scaleFactor, rugosityArray);
		//    	drawer.setVisible(true);

	}

	void setOrientation() {
		for (int i = 0; i < gridWidth; i++) {
			for (int j = 0; j < gridHeight; j++) {
				int startx = i*10; 
				int starty = j*10; 

				int rug = rugosityArray[i][j] + 1; 
				switch (rug) {

				case 1: // low rugosity
					for (int k = 0; k<10; k++){
						for (int l = 0; l<10; l++){
							double prob = uniform.nextDoubleFromTo(0, 1) ; 
							if (prob < orientationCoverArray[reefNumber][0][0]) orientationArray[startx+k][starty+l] = 0; // orient = UP
							else if (prob < orientationCoverArray[reefNumber][0][0]+orientationCoverArray[reefNumber][0][1]) orientationArray[startx+k][starty+l]= 1; // orient = VERT
							else orientationArray[startx+k][starty+l] = 2; // orient = OTHER
						}
					}
							
				case 2: // mid rugosity
					for (int k = 0; k<10; k++){
						for (int l = 0; l<10; l++){
							double prob = uniform.nextDoubleFromTo(0, 1) ; 
							if (prob < orientationCoverArray[reefNumber][1][0]) orientationArray[startx+k][starty+l] = 0; // orient = UP
							else if (prob < orientationCoverArray[reefNumber][1][0]+orientationCoverArray[reefNumber][0][1]) orientationArray[startx+k][starty+l]= 1; // orient = VERT
							else orientationArray[startx+k][starty+l] = 2; // orient = OTHER
						}
					}
							
				case 3: // high rugosity
					for (int k = 0; k<10; k++){
						for (int l = 0; l<10; l++){
							double prob = uniform.nextDoubleFromTo(0, 1) ; 
							if (prob < orientationCoverArray[reefNumber][2][0]) orientationArray[startx+k][starty+l] = 0; // orient = UP
							else if (prob < orientationCoverArray[reefNumber][2][0]+orientationCoverArray[reefNumber][0][1]) orientationArray[startx+k][starty+l]= 1; // orient = VERT
							else orientationArray[startx+k][starty+l] = 2; // orient = OTHER
						}
					}
					
				default:
				break;
				} // end switch 
				
				
			}
		}
		
		
	}

	public int[][] getRugosityArray() {
		return rugosityArray;
	}


	public int[][] getOrientationArray() {
		return orientationArray;
	}



}// end of SubstrateBuilder class

package coresim;

import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import java.awt.Point;

import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.Normal;


public class CoralBuilder {

	private Uniform uniform; 
	private Normal normal; 

	private int numReefs; // number of reefs in the simulation; here, in order: coopers, megans, mk14, mk16 

	// Here, sizeFreqArray holds the proportion in each size class.  We do not consider size 0-10cm, so we have
	// a total of 5 size classes: 10-20, 20-30, 30-40, 40-50, 50-60cm
	private float[][] sizeFreqArray;  
	private float siteCover; 
	private double totalCoralCells;
	private int[] numPoritesCorals = new int[5];
	private int[] numSeedsArray = new int[5];
	private int[] numBetasArray = new int[5];
	private int[] size = {36, 25, 16, 9, 4};  
	private int[] diameter = {6, 5, 4, 3, 2};  

	private int reefNumber; 
	private float[] reefAreaBuffer = {.34f, .39f, .05f, .03f}; // proportion of 1008x1008m area surrounding each central site that is reef 

	private int locX;
	private int locY;
	private boolean check; 
	private int loopCounter; 
	private int coralDiameter;
	private float coralDiamFloat; 
	private double coralSA, coralCircleSA, coralHemSA;


	//private int reefArea; 
	private int gridWidth, gridHeight; 

	ArrayList<Point> reefCoords = new ArrayList<Point>();
	ArrayList<Point> seedCoords = new ArrayList<Point>();

	ArrayList<Coral> coralList = new ArrayList<Coral>();   
	private int[][] coralArray = new int[320][320];

	/* 	NOTE: this method distributes corals with spatial aggregation, using alpha and beta parameters below
	 * 	The method used is taken from Lundquist and Botsford (2004) and additionally used in Brant's (2007) dissertation
	 * 	The level of spatial aggregation was checked against measured values for Nearest Neighbor clustering index 
	 * 	(index = avg measured NN distance / avg NN distance under random distribution) measured from P. astreoides mapping by 
	 *  Brooke Gintert and colleagues and U. Miami RSMAS, where she found values of 0.62-0.599
	 */
	
	// Here, these values were chosen based on a parameter sweep across alpha, beta, and betaStd, and set based on the % cover of Porites
	// at each of the four sites.  Here, the elements correspond to the 4 sites, in order coopers-megans-mk14-mk16
	private double[] alphaArray = {.5, .7, .5, .5}; // percent of individuals first distributed randomly 
	private int[] betaArray = {5, 10, 10, 10}; // average Guassian distance in which remaining individuals are distributed from seeded individuals
	private int[] betaStdArray = {5, 1, 3, 3}; 

	private double alpha; // percent of individuals first distributed randomly 
	private int beta; // average Guassian distance in which remaining individuals are distributed from seeded individuals
	private int betaStd; 

	private double tempNumSeeded=0; 
	private double distance;
	private int[] newPoint = new int[3]; 

	private int adultSpatial; 
	
	CoralBuilder(TimeKeeper tk) {

		reefNumber = tk.getReefNumber();
		siteCover = RecruitmentMain.getSiteCoverArray()[reefNumber]; 
		sizeFreqArray = RecruitmentMain.getSizeFreqArray();
		gridWidth = RecruitmentMain.getGridWidth();
		gridHeight = RecruitmentMain.getGridHeight(); 
	
		uniform = tk.getUniform(); 
		normal = tk.getNormal(); 
		
		adultSpatial=tk.getAdultSpatial(); 
		
		//Brooke's values
		if (adultSpatial == 0){
		alpha = alphaArray[reefNumber];
		beta = betaArray[reefNumber];
		betaStd = betaStdArray[reefNumber];
		}
		
		// set adult spatial distribution to fully random 
		else if (adultSpatial ==1){
			alpha = 1.0;
			beta = 0;
			betaStd = 0;
		}

		// set adult spatial distribution to more clumped than data supports
		else if (adultSpatial == 2){
			alpha = alphaArray[reefNumber]-.2;
			beta = betaArray[reefNumber];
			betaStd = betaStdArray[reefNumber];
		}
		
	}


	/**
	 * Method called to set the number of corals for the habitat area and distribute them accordingly corals
	 */
	void buildCorals() {

		// store points; here, only useful if not solid landscape; i.e., reading in habitat layer with distince patch edges and non-reef area
		for (int i = 0; i < gridWidth; i++) {
			for (int j = 0; j < gridHeight; j++) {

				coralArray[i][j] = 0; 
				reefCoords.add(new Point(i,j)); 
				//reefArea++;
			}
		}

		totalCoralCells = (gridWidth*gridHeight)*(siteCover/100);
		numPoritesCorals = setTotalColonies(totalCoralCells); 
		// set the number of seeded corals and beta corals for each size category
		// here, equal number of seeded individuals for each size category
		for (int i = 0; i < 5; i++) {
			tempNumSeeded = (numPoritesCorals[i])*alpha; 
			numSeedsArray[i] = (int) tempNumSeeded;
			numBetasArray[i] = numPoritesCorals[i] - numSeedsArray[i];
		}

		distributeCorals();
		//DrawHabitat drawer = new DrawHabitat(gridWidth, gridHeight, 1, coralArray);
		//drawer.setVisible(true);

	}

	/**
	 * Distributes the corals onto the substrate array based on size frequency
	 * distribution and spatial configuration (alpha and beta parameters)
	 */

	void distributeCorals() {


		/////////////////////////////////////////////
		// Seed corals distribution
		/////////////////////////////////////////////


		for (int i = 0; i < size.length; i++ ) {
			for (int j = 0; j < numSeedsArray[i]; j++) {

				//check locations to make sure they're open
				check = true; 
				loopCounter = 0;  

				while (check) {

					//System.out.println("While loop size36 to find appropriate coords accessed");

					// get a random location from patch coordinates
					int elements = reefCoords.size(); 
					Point point = reefCoords.get(uniform.nextIntFromTo(0, elements-1));
					locX = (int) point.getX();
					locY = (int) point.getY();

					if (checkPoint(locX, locY, size[i])){
						check = false; 
						seedCoords.add(new Point(locX, locY));

						Coral coral = new Coral();
						coral.setX(locX);
						coral.setY(locY);
						coralDiameter = uniform.nextIntFromTo(0,9)+((diameter[i]-1)*10 + 1);
						coral.setDiameter(coralDiameter);
						coralDiamFloat=coralDiameter; 
						coralCircleSA = Math.PI*(coralDiamFloat/2)*(coralDiamFloat/2); 
						coralHemSA = coralCircleSA*3; 
						coralSA = uniform.nextIntFromTo(0, (int)(coralHemSA-coralCircleSA)) + coralCircleSA;
						coral.setSurfaceArea(coralSA);

						coralList.add(coral);  

						setCoralArray(locX, locY, size[i]); 
					}

					else loopCounter += 1; 

					// break out of while loop if have had 100 unsuccessful tries to find a suitable location
					if (loopCounter > 100) {
						check = false;  
					}

				} // end of while loop
			}

		}


		////////////////////////////////////////////////////////////
		// loop through and distribute all the Beta's 
		////////////////////////////////////////////////////////////


		if (seedCoords.size() > 0){

			for (int i = 0; i < size.length; i++ ) {
				for (int j = 0; j < numBetasArray[i]; j++) {


					//check locations to make sure they're open
					check = true; 
					loopCounter = 0;  

					while (check) {

						newPoint = getBetaPoint(); 
						locX = getWrapAround(newPoint[0]);
						locY = getWrapAround(newPoint[1]); 
						int elements = newPoint[2];

						if (checkPoint(locX, locY, size[i])){
							check = false; 

							Coral coral = new Coral();
							coral.setX(locX);
							coral.setY(locY);
							coralDiameter = uniform.nextIntFromTo(0,9)+((diameter[i]-1)*10 + 1);
							coral.setDiameter(coralDiameter);
							coralDiamFloat=coralDiameter; 
							coralCircleSA = Math.PI*(coralDiamFloat/2)*(coralDiamFloat/2); 
							coralHemSA = coralCircleSA*3; 
							coralSA = uniform.nextIntFromTo(0, (int)(coralHemSA-coralCircleSA)) + coralCircleSA;
							coral.setSurfaceArea(coralSA);

							coralList.add(coral);  

							setCoralArray(locX, locY, size[i]); 
						}

						else loopCounter += 1; 

						// break out of while loop if have had 100 unsuccessful tries to find a suitable location
						if (loopCounter > 100) {
							check = false;  
						}

					} // end of while loop
				}

			}
		}
	}



	/**	Checks to see if the location if devoid of corals for the full size of the coral; i.e., if 3x3pixels (or 30x30cm size), need to check 
	 * 9 total pixels to make sure no coral is already there 
	 * 
	 * @param x
	 * @param y
	 * @param size
	 * @return
	 */
	boolean checkPoint(int x, int y, int size){
		boolean checker = true;


		if (size == 36) {
			for (int i = x-3; i <= x+2; i++) {
				for (int j = y-3; j <= y+2; j++) {
					if (coralArray[getWrapAround(i)][getWrapAround(j)] == 4) {
						checker = false;
					}}}
		}
		else if (size == 25) {
			for (int i = x-2; i <= x+2; i++) {
				for (int j = y-2; j <= y+2; j++) {
					if (coralArray[getWrapAround(i)][getWrapAround(j)] == 4) {
						checker = false;
					}}}

		}
		else if (size == 16) {
			for (int i = x-2; i <= x+1; i++) {
				for (int j = y-2; j <= y+1; j++) {
					if (coralArray[getWrapAround(i)][getWrapAround(j)] == 4) {
						checker = false;
					}}}
		}
		else if (size == 9) {
			for (int i = x-1; i <= x+1; i++) {
				for (int j = y-1; j <= y+1; j++) {
					if (coralArray[getWrapAround(i)][getWrapAround(j)] == 4) {
						checker = false;
					}}}
		}
		else if (size == 4) {
			for (int i = x-1; i <= x; i++) {
				for (int j = y-1; j <= y; j++) {
					if (coralArray[getWrapAround(i)][getWrapAround(j)] == 4) {
						checker = false;
					}}}
		}
		else if (size == 1) {
			if (coralArray[getWrapAround(x)][getWrapAround(y)] == 4) {
				checker = false;
			}
		}

		return checker;

	}


	/** Once coral object is created, add the coral substrate code value to coralArray so checkPoint method can assess the pixel locations
	 * 
	 * @param x
	 * @param y
	 * @param size
	 */
	void setCoralArray(int x, int y, int size){


		if (size == 36) {
			for (int i = x-3; i <= x+2; i++) {
				for (int j = y-3; j <= y+2; j++) {
					coralArray[getWrapAround(i)][getWrapAround(j)] = 4;					
				}}
		}
		else if (size == 25) {
			for (int i = x-2; i <= x+2; i++) {
				for (int j = y-2; j <= y+2; j++) {
					coralArray[getWrapAround(i)][getWrapAround(j)] = 4;
				}}

		}
		else if (size == 16) {
			for (int i = x-2; i <= x+1; i++) {
				for (int j = y-2; j <= y+1; j++) {
					coralArray[getWrapAround(i)][getWrapAround(j)] = 4;					
				}}
		}
		else if (size == 9) {
			for (int i = x-1; i <= x+1; i++) {
				for (int j = y-1; j <= y+1; j++) {
					coralArray[getWrapAround(i)][getWrapAround(j)] = 4;
				}}
		}
		else if (size == 4) {
			for (int i = x-1; i <= x; i++) {
				for (int j = y-1; j <= y; j++) {
					coralArray[getWrapAround(i)][getWrapAround(j)] = 4;
				}}
		}

	}



	/** Based on the total cover of coral at a site (numCoralCells), this method allocates the appropriate number of each size
	 * category and returns an array of total colony numbers for each size class
	 */ 

	int[] setTotalColonies(double numCoralCells) {
		int[] numCorals = new int[5];

		int numSize4=0, numSize9=0, numSize16=0, numSize25=0, numSize36=0;   
		int colonyCounter = 0;


		while (colonyCounter < numCoralCells) {
			int prob = uniform.nextIntFromTo(0, 100);

			// may want to randomize this order; right now, little corals get more of preference
			// however, will be difficult to do so...
			if (prob <= (sizeFreqArray[reefNumber][0])*100) {
				numSize4 +=1;
				colonyCounter += 4;
			}
			else if (prob > (sizeFreqArray[reefNumber][0])*100 && prob <= (sizeFreqArray[reefNumber][1])*100) {
				numSize9 +=1;
				colonyCounter += 9;
			}
			else if (prob > (sizeFreqArray[reefNumber][1])*100 && prob <= (sizeFreqArray[reefNumber][2])*100) {
				numSize16 +=1;
				colonyCounter += 16;
			}
			else if (prob > (sizeFreqArray[reefNumber][2])*100 && prob <= (sizeFreqArray[reefNumber][3])*100) {
				numSize25 += 1; 
				colonyCounter += 25;
			}
			else if (prob > (sizeFreqArray[reefNumber][3])*100) {
				numSize36 += 1; 
				colonyCounter += 36;
			}

		} // end of while loop

		// once go through while loop, set the numbers in the spp's arrays
		// here, reverse the sizes, so largest size is first element, because this is the way they're distributed
		numCorals[4] = numSize4;
		numCorals[3] = numSize9;
		numCorals[2] = numSize16;
		numCorals[1] = numSize25;
		numCorals[0] = numSize36;

		
//		for (int i=0; i < 5; i++){
//			System.out.println("Reef Number: \t" + reefNumber + "\tNumber corals size"+i+": \t" + numCorals[i]); 
//		}

		
		return numCorals;

		// Testing
		

	} // end of SetTotalColonies method


	/** Sets the numExternalCorals array which stores the total number of colonies in each size category for the surrounding reef area 
	 * in a 1km radius buffer around 
	 * 
	 */
	int[] getExternalCoralNum() {
		double numTempCells;
		int [] numExternalCorals = new int[5];

		/* This will calculate the total number of reef cells within a 1x1km area around reef point.
		 * Will then multiply by site cover (%) and proportion of reef area around reef (relative to total potential),
		 * and then substract the center simulated site of (640x640) because this is taken care of above in the 
		 * coral builder method.
		 * 
		 * This external supply will then be used in the larvae method to get locations of settlers from outside		 * 
		 */

		numTempCells = (((10080*10080)-(gridHeight*gridWidth))*(siteCover/100) * (reefAreaBuffer[reefNumber]));

		numExternalCorals = setTotalColonies(numTempCells);
		return numExternalCorals; 


		
	}

	// this method will check to make sure it's an appropriate habitat type, and will 
	// check the rugosity and derive a likelihood of using the cell if it falls within a 
	// particular probability



	// returns a value to make sure is wrap around toroidal landscape
	int getWrapAround(int x){

		int returnValue = x; 
		if (x >= gridWidth) returnValue = x-gridWidth;
		if (x < 0) returnValue = gridWidth+x;
		return returnValue; 
	}






	int[] getBetaPoint(){
		int[] point = new int[3];
		double xDisplacement, yDisplacement; 
		int locXSeed, locYSeed;
		double angle; 

		int elements = seedCoords.size(); 
		Point seedPoint = seedCoords.get(uniform.nextIntFromTo(0, elements-1));

		locXSeed = (int) seedPoint.getX();
		locYSeed = (int) seedPoint.getY();

		// this will pick a random distance, uniformly distributed, from the seeded colony to a random max distance away
		// here, the max distance is normally distributed around beta with a std dev around beta
		// this will produce an effect where the "circles" defining the spatial clumping aren't distinct but variable to 
		// different degrees (beta, beta stdev)
		distance = uniform.nextDoubleFromTo(0,1) * normal.nextDouble(beta, betaStd); 
		angle =  uniform.nextDoubleFromTo(0,1) *(2*Math.PI); 

		xDisplacement = Math.cos(angle)*distance;
		yDisplacement = Math.sin(angle)*distance;

		point[0] = (int) xDisplacement + locXSeed;
		point[1] = (int) yDisplacement + locYSeed;
		point[2] = elements; 

		return point; 
	}


	public int[][] getCoralArray() {
		return coralArray;
	}


	public void setCoralArray(int[][] coralArray) {
		this.coralArray = coralArray;
	}


	public ArrayList<Coral> getCoralList() {
		return coralList;
	}


	public void setCoralList(ArrayList<Coral> coralList) {
		this.coralList = coralList;
	}

}

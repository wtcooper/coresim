package utilities;

import java.awt.Point;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import cern.jet.random.Beta;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;


public class SpatialAggregationTester {


	int compare; // Number of compare made to find the nearest
	int maxradius;  // Maximum radius of search in the array
	int ndata;      // Number of data in the arrays
	Point data[];   // Point array


	private int seed = 46546546; //(int) System.currentTimeMillis();
	private MersenneTwister m = new MersenneTwister(seed); 
	private Uniform uniform  = new Uniform(0, 100, m);  
	private Normal normal = new Normal(1,1,m); 
	private Beta betaDist = new Beta(1, 1, m);


	private int numReefs; // number of reefs in the simulation; here, in order: coopers, megans, mk14, mk16 

	// Here, sizeFreqArray holds the proportion in each size class.  We do not consider size 0-10cm, so we have
	// a total of 5 size classes: 10-20, 20-30, 30-40, 40-50, 50-60cm
	private float[] sizeFreqArray = {.7f, .9f, 1f, 0f, 0f};
	private int[][] coralArray = new int[320][320];

	private float siteCover = 2.68f; // set coral cover to 5% 

	private double totalCoralCells;
	private int[] numPoritesCorals = new int[5];
	private int[] numSeedsArray = new int[5];
	private int[] numBetasArray = new int[5];
	private int[] size = {36, 25, 16, 9, 4};  

	private int reefNumber; 

	private int locX;
	private int locY;
	private boolean check; 
	private int loopCounter; 

	//private int reefArea; 
	private int gridWidth=320, gridHeight=320; 

	ArrayList<Point> reefCoords;
	ArrayList<Point> seedCoords;
	ArrayList<Point> coralCoords;


	/* 	NOTE: this method distributes corals with spatial aggregation, using alpha and beta parameters below
	 * 	The method used is taken from Lundquist and Botsford (2004) and additionally used in Brant's (2007) dissertation
	 * 	The level of spatial aggregation was checked against measured values for Nearest Neighbor clustering index 
	 * 	(index = avg measured NN distance / avg NN distance under random distribution) measured from P. astreoides mapping by 
	 *  Brooke Gintert and colleagues and U. Miami RSMAS, where she found values of 0.62-0.599
	 */
	private double[] alphaArray = {1, .01, .05, .1, .3, .5, .7, .9}; // percent of individuals first distributed randomly 
	private int[] betaArray = {5, 10, 25, 50, 75, 100}; // average Guassian distance in which remaining individuals are distributed from seeded individuals
	private int[] betaStdArray = {1, 3, 5, 10}; 

	private double alpha;
	private int beta;
	private int betaStd; 

	private double[] nnDistances; 

	private double randomNND = 0;
	private double[] spatialNND; 

	private double tempNumSeeded=0; 
	private double distance;
	private int[] newPoint = new int[3]; 

	private int counter; 
	private PrintWriter outFile=null;	

	int totalNumCorals; 
	int reefArea = 0; 

	public void testNND() {
		counter = 0; 
		spatialNND = new double[alphaArray.length*betaArray.length*betaStdArray.length]; 
		
		// do first run to get randomNND (i.e., alphaArray[0], where alpha = 1)
		alpha = alphaArray[0]; 
		beta = 0; betaStd = 0; 
		
		// GOOD PARAMETER VALUES FOR 5%
		// alpha beta betastd
		//0.1	10	5
		//0.05	10	10
		//0.05	5	10
		alpha = .5; beta = 5; betaStd = 5; 

		buildCorals();
		computeNND(); 

		//***********TESTING**********
				DrawHabitat drawer = new DrawHabitat(gridWidth, gridHeight, 3, coralArray);
		drawer.setVisible(true);
		for (int i = 0; i < gridWidth; i++) {
			for (int j = 0; j < gridHeight; j++) {
				if (coralArray[i][j] == 4) 	reefArea++;
			}
		}
		System.out.println("total num corals: \t" + totalNumCorals); 
		System.out.println("total reef cover: \t" + ((double) reefArea)/((double)gridWidth*gridHeight)); 
		/**/
		
		//loop through the remainder of alpha, beta, and betaStd combinations		
		for (int i = 1; i < alphaArray.length; i++) {
			alpha = alphaArray[i]; 
			for (int j=0; j < betaArray.length; j++){
				beta = betaArray[j];
				for (int k=0; k < betaStdArray.length; k++){
					betaStd = betaStdArray[k]; 

					buildCorals();
					
					//*****TESTING*******
				//	DrawHabitat drawer2 = new DrawHabitat(gridWidth, gridHeight, 2, coralArray);
				//	drawer2.setVisible(true);
				//	for (int m = 0; m < gridWidth; m++) {
				//		for (int n = 0; n < gridHeight; n++) {
				//			if (coralArray[m][n] == 4) 	reefArea++;
				//		}
				//	}
					//System.out.println("total num corals: \t" + totalNumCorals); 
					//System.out.println("total reef cover: \t" + ((double) reefArea)/((double)gridWidth*gridHeight)); 

					computeNND();

				}
			}
		}
	}


	public void computeNND() {

		double totalDistances = 0; 
		int elements = coralCoords.size();
		nnDistances = new double[elements]; 

		for (int i = 0; i<coralCoords.size(); i++){

			Point coralPoint = coralCoords.get(i);
			Point nearestPoint = findNearestNeighborCrude(coralPoint, i); 

			nnDistances[i] = computeDistance(coralPoint, nearestPoint); 
			//System.out.println("nnDistances: \t" + nnDistances[i]); 
			
		}

		for (int i = 0; i<elements; i++)   { 
			totalDistances = totalDistances + nnDistances[i]; 
		}
		
		spatialNND[counter] = totalDistances / (double) (elements-1); 
		
		//System.out.println("spatial NND: \t" + spatialNND[counter]); 
				
		if(alpha == 1){
			randomNND = spatialNND[counter]; 
			//System.out.println("random NND: \t" + randomNND); 

		}

		counter++; 

	}




	// A crude way to find the nearest neighbor
	public Point findNearestNeighborCrude(Point p, int coralElement)
	{
		//System.out.println("coral element #: \t" + coralElement); 
		double mindist,dist;
		int mini = 0;
		// Init compare
		compare = 0;
		int elements = coralCoords.size();

		mindist = computeDistance(p, coralCoords.get(mini));
		for(int j=0; j < elements; j++)
		{

			if (j != coralElement){
				dist = computeDistance(p, coralCoords.get(j));
				compare++;
				if(dist < mindist)
				{
					mini = j;
					mindist = dist;
				}
			}
		}
		return coralCoords.get(mini);
	}

	// Compute the SQUARED distance between to points
	public double computeDistance(Point p1, Point p2)
	{
		double dx,dy;
		dx = p2.x - p1.x;
		dy = p2.y - p1.y;
		
		return Math.sqrt(dx*dx + dy*dy);
	}



	/**
	 * Method called to set the number of corals for the habitat area and distribute them accordingly corals
	 */
	void buildCorals() {

		// reinitialize these at start of each new alpha/beta/betaStd combination
		reefCoords = new ArrayList<Point>();
		seedCoords = new ArrayList<Point>();
		coralCoords = new ArrayList<Point>();
		totalNumCorals = 0 ;

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

						// add coral to point array
						coralCoords.add(new Point(locX, locY)); 
						totalNumCorals++; 
						// set the coral array so the checkPoint() method works 
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

						if (checkPoint(locX, locY, size[i])){
							check = false; 

							// add coral to point array
							coralCoords.add(new Point(locX, locY)); 
							totalNumCorals++; 
							// set the coral array so the checkPoint() method works 
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
			if (prob <= (sizeFreqArray[0])*100) {
				numSize4 +=1;
				colonyCounter += 4;
			}
			else if (prob > (sizeFreqArray[0])*100 && prob <= (sizeFreqArray[1])*100) {
				numSize9 +=1;
				colonyCounter += 9;
			}
			else if (prob > (sizeFreqArray[1])*100 && prob <= (sizeFreqArray[2])*100) {
				numSize16 +=1;
				colonyCounter += 16;
			}
			else if (prob > (sizeFreqArray[2])*100 && prob <= (sizeFreqArray[3])*100) {
				numSize25 += 1; 
				colonyCounter += 25;
			}
			else if (prob > (sizeFreqArray[3])*100) {
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


	public void writeOutput() {

		try {  outFile = new PrintWriter(new FileWriter("SpatialAggregtaion.txt", true));
		} catch (IOException e) {e.printStackTrace();	}

		// print both the spatialNND and the index of spatial/random
		for (int i = 0; i < counter; i++) {
				outFile.println(spatialNND[i] + "\t" + spatialNND[i]/randomNND);
			}

		outFile.println(); 
		outFile.close();


	}

	
	public static void main(String[] args) {

		SpatialAggregationTester sat = new SpatialAggregationTester(); 
		sat.testNND();
		sat.writeOutput(); 

	}

}

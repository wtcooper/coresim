package coralSampling;

import java.util.List;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.quadtree.Quadtree;

import javolution.util.FastMap;
import javolution.util.FastTable;

import cern.jet.random.Uniform;
import cern.jet.random.Normal;


public class CoralBuilder {

	private GeometryFactory gf = new GeometryFactory();
	private SpatialIndex spatialIndex = new Quadtree();

	private Uniform uniform; 
	private Normal normal; 

	private int numSpecies;  
	private int numSizeFreqBins; //10 cm bins
	private double binSize; 

	FastMap<Integer, FastTable<Coral>> coralsForDistribution = new FastMap<Integer, FastTable<Coral>>(); //here, map is species number, FastList is collection of corals
	FastMap<Integer, FastTable<Coral>> coralsForSeeding = new FastMap<Integer, FastTable<Coral>> (); 
	FastMap<Integer, FastTable<Coral>> seededCorals = new FastMap<Integer, FastTable<Coral>> (); 
	FastMap<Integer, FastTable<Coral>>  betaCorals = new FastMap<Integer, FastTable<Coral>> (); 

	FastTable <Coordinate> seedCoords = new FastTable<Coordinate>();


	private double[][] sizeFreqArray;  
	private double[][] sizeFreqArrayAdjust;  
	private double[] siteCover; 
	private double[] alpha; 
	private double[] beta; 
	private double[] betaStd; 

	private int[] numSeedsArray;
	private int[] numBetasArray;

	private boolean check; 
	private int loopCounter; 

	private int numSeedCorals=0;
	private int numBetaCorals=0; 

	private double tempNumSeeded=0; 
	private double distance;
	private double reefWidth, reefHeight, reefArea; 

	private Sampler sampler;

	CoralBuilder(Sampler sampler) {

		this.sampler = sampler; 

		reefWidth = sampler.getReefWidth();
		reefHeight = sampler.getReefHeight();
		reefArea = sampler.getReefArea(); 
		uniform = sampler.getUniform(); 
		normal = sampler.getNormal(); 
		numSpecies = sampler.getNumSpecies();
		numSizeFreqBins = sampler.getNumSizeFreqBins(); 
		binSize = sampler.getSizeFreqBinSize(); 
		siteCover = sampler.getSiteCover(); 
		sizeFreqArray = sampler.getSizeFreqArray();
		alpha = sampler.getAlpha();
		beta = sampler.getBeta();
		betaStd = sampler.getBetaStd(); 

		numSeedsArray = new int[numSpecies]; 
		numBetasArray = new int[numSpecies]; 

		for (int i = 0; i<numSpecies; i++){
			for (int j=0; j<numSizeFreqBins; j++){
				if (j==0) sizeFreqArrayAdjust[i][j] = sizeFreqArray[i][j];
				else sizeFreqArrayAdjust[i][j] = sizeFreqArray[i][j] + sizeFreqArrayAdjust[i][j-1]; // this rescales so that values represent proportions along number line from 0-1
			}
		}

	}


	/**
	 * Method called to set the number of corals for the habitat area and distribute them accordingly corals
	 */
	void buildCorals() {

		// loop through each species, and continue to build corals until have reached the appropriate total cover sum for that species
		for (int i = 0; i < numSpecies; i++){

			double coverSum = 0; 
			while (coverSum < siteCover[i]*reefArea){  // here, multiple this by reef area so get the total area of coral tissue on the reef in m^2
				double binMaxSize = getSizeFreqBinMaxSize(i); 

				double diameter = (uniform.nextDoubleFromTo(binMaxSize-binSize,binMaxSize))/100;  // convert diameter to meters for calculations since area is in m^2  
				Coral coral = new Coral(); 
				coral.setDiameter(diameter); 
				coral.setSpeciesIndex(i); 
				//TODO -- set whatever othe coral properies here

				FastTable<Coral> list = (FastTable<Coral>) coralsForDistribution.get(i);
				if (list == null) coralsForDistribution.put(i, (new FastTable<Coral>()));
				list.add(coral);

				coverSum += Math.PI*Math.pow((diameter/2),2); // here, sum up the coral cover assumming a circular shape.  If want different shapes, then will have to also assign a shape here and sum up assumming that shape 
			}
		}

		// set the number of seeded corals and beta corals for each species
		for (int i = 0; i < numSpecies; i++) {
			tempNumSeeded = (coralsForDistribution.get(i).size())*alpha[i]; 
			numSeedsArray[i] = (int) tempNumSeeded;
			numBetasArray[i] = coralsForDistribution.get(i).size() - numSeedsArray[i];

			// create a new Coral temp list which will be the value in the seedCorals map, then add corals to this
			FastTable<Coral> tempSeedList = new FastTable<Coral>(); 
			coralsForSeeding.put(i, tempSeedList); 
			FastTable<Coral> tempBetaList = new FastTable<Coral>(); 
			betaCorals.put(i, tempBetaList); 

			// add all seed individuals to coralSeeds list -- here, do this randomly
			for (int j=0; j < numSeedsArray[i]; j++){
				tempSeedList.add(coralsForDistribution.get(i).remove(uniform.nextIntFromTo(0, coralsForDistribution.get(i).size()))); 
				numSeedCorals++; 
			}

			System.out.println("checking numBetasArray[i] versus num remaining in corals for distribution;  numBetasArray[i]: " + numBetasArray[i] + "\tremaining num in coralsForDistribution: " + coralsForDistribution.get(i).size());

			// add all remaining beta individuals to coralBetas list
			for (int j=0; j < coralsForDistribution.get(i).size(); j++){
				tempBetaList.add(coralsForDistribution.get(i).remove(0)); 
				numBetaCorals++; 
			}
		}

		distributeCorals();
	}



	/** Randomly selects a size bin based on size frequency proportions for a given species, and returns the maximum size for that size bin (e.g., 10cm for the 0-10cm size bin) 
	 * 
	 * @param speciesNum
	 * @return
	 */
	private double getSizeFreqBinMaxSize(int speciesNum) {

		double randProb = uniform.nextDoubleFromTo(0, 1); 
		for (int i = 0; i < numSizeFreqBins; i++){
			double lowerBounds=0, upperBounds=0; 
			if (i == 0) lowerBounds = 0;
			else lowerBounds = sizeFreqArrayAdjust[speciesNum][i-1];
			upperBounds = sizeFreqArrayAdjust[speciesNum][i-1];

			if ( (randProb >= lowerBounds) && (randProb < upperBounds) ) return (i+1)*binSize; 
		}
		System.out.println("***Didn't select a size!***");
		System.exit(1); 
		return 999; 
	}





	/**
	 * Distributes the corals onto the substrate array based on size frequency
	 * distribution and spatial configuration (alpha and beta parameters)
	 */

	void distributeCorals() {

		/////////////////////////////////////////////
		// Seed corals distribution
		/////////////////////////////////////////////

		for (int i = 0; i < numSeedCorals; i++ ) {

			Coral coral = getRandomCoral("seed"); //coralSeeds.remove(uniform.nextIntFromTo(0, coralSeeds.size())); 

			//check locations to make sure they're open
			check = true; 
			loopCounter = 0;  

			while (check) {

				// get a random location from patch coordinates
				Coordinate midCoord = new Coordinate(); 
				midCoord.x = uniform.nextDoubleFromTo(0, reefWidth); 
				midCoord.y = uniform.nextDoubleFromTo(0, reefHeight); 

				Geometry newGeom = getCoralGeometry(midCoord, coral); 

				if (checkPoint(newGeom)){
					check = false; 
					coral.setGeometry(newGeom); 
					coral.setMidpoint(midCoord); 

					spatialIndex.insert(newGeom.getEnvelopeInternal(), coral); 

					//add to map of all seeded corals so can distribute beta's against
					FastTable<Coral> list = (FastTable<Coral>) seededCorals.get(coral.getSpeciesIndex());
					if (list == null) seededCorals.put(coral.getSpeciesIndex(), (new FastTable<Coral>()));
					list.add(coral);

				}

				else loopCounter += 1; 

				// break out of while loop if have had 100 unsuccessful tries to find a suitable location
				if (loopCounter > 100) {
					check = false;  
				}

			} // end of while loop
		}



		////////////////////////////////////////////////////////////
		// loop through and distribute all the Beta's 
		////////////////////////////////////////////////////////////



		for (int i = 0; i < numBetaCorals; i++ ) {

			Coral coral = getRandomCoral("beta"); //coralBetas.remove(uniform.nextIntFromTo(0, coralBetas.size())); 
			int speciesIndex = coral.getSpeciesIndex(); 

			if (seededCorals.get(speciesIndex).size() > 0){

				//check locations to make sure they're open
				check = true; 
				loopCounter = 0;  

				while (check) {

					Coordinate newCoord = getBetaPoint(speciesIndex); 

					Geometry newGeom = getCoralGeometry(newCoord, coral); 

					if (checkPoint(newGeom)){
						check = false; 
						coral.setGeometry(newGeom); 
						spatialIndex.insert(newGeom.getEnvelopeInternal(), coral); 
					}

					else loopCounter += 1; 

					// break out of while loop if have had 100 unsuccessful tries to find a suitable location
					if (loopCounter > 100) {
						check = false;  
					}

				} // end of while loop
			}
			else {
				//TODO - distribute all coralBetas randomly across full landscape


			}


		}
	}



	public Coral getRandomCoral(String type){
		FastMap<Integer, FastTable<Coral>> tempMap = null; 
		//		Coral coral = null;
		if (type.equals("seed")) tempMap = coralsForSeeding;
		else tempMap = betaCorals; 

		int element = uniform.nextIntFromTo(0, numSpecies-1); 
		return 	tempMap.get(element).remove(uniform.nextIntFromTo(0, tempMap.get(element).size()-1)); 

		/*
		if (type.equals("seed")){

			int element = uniform.nextIntFromTo(0, numSpecies-1); 
			coral = seedCorals.get(element).remove(uniform.nextIntFromTo(0, seedCorals.get(element).size()-1)); 
		}

		else if (type.equals("beta")){

		}
		return coral;
		 */
	}


	public Geometry getCoralGeometry(Coordinate midCoord, Coral coral){

		double radius = coral.getDiameter()/2; 
		Coordinate[] coords = new Coordinate[sampler.getNumCirclePoints()]; 
		for (int i=0; i<coords.length; i++){
			coords[i] = new Coordinate(0,0,0); 
		}
		// set the first coordinate in the coords array to the point at 0 angle
		coords[0].x = midCoord.x+radius;
		coords[0].y = midCoord.y; 

		double angleIncrement = (Math.PI*2)/coords.length; // in radians, this is angle between each polygon point of circle, based on the total number of circle points defined in Sampler
		double angle = angleIncrement; // start at first increment

		for (int i=1; i<coords.length; i++){
			coords[i].x = midCoord.x + (Math.cos(angle)*radius); 
			coords[i].y = midCoord.y + (Math.sin(angle)*radius); 
			angle += angleIncrement; 
		}

		Geometry geom = (Geometry) gf.createPolygon(new LinearRing(new CoordinateArraySequence(coords), gf), null); 
		return geom; 
	}




	/**	Checks to see if the location if devoid of corals for the full size of the coral; i.e., if 3x3pixels (or 30x30cm size), need to check 
	 * 9 total pixels to make sure no coral is already there 
	 * 
	 * @param x
	 * @param y
	 * @param size
	 * @return
	 */
	@SuppressWarnings("unchecked")
	boolean checkPoint(Geometry newGeometry){
		List<Coral> hits = spatialIndex.query(newGeometry.getEnvelopeInternal());

		if (hits.size() == 0) {
			return true; 
		}
		Coral coral; 
		for (int i = 0; i < hits.size(); i++) {
			coral = hits.get(i);
			Geometry geom = (Geometry) coral.getGeometry();
			if (newGeometry.intersects(geom)) { 
				return false;
			}
		}		
		return true; 
	}




	public Coordinate getBetaPoint(int speciesIndex){

		Coordinate point = new Coordinate();
		double xDisplacement, yDisplacement; 
		double angle; 

		int elements = seededCorals.get(speciesIndex).size(); 
		Coordinate seedPoint = seededCorals.get(speciesIndex).get(uniform.nextIntFromTo(0, elements-1)).getMidpoint();

		// this will pick a random distance, uniformly distributed, from the seeded colony to a random max distance away
		// here, the max distance is normally distributed around beta with a std dev around beta
		// this will produce an effect where the "circles" defining the spatial clumping aren't distinct but variable to 
		// different degrees (beta, beta stdev)
		distance = uniform.nextDoubleFromTo(0,1) * normal.nextDouble(beta[speciesIndex], betaStd[speciesIndex]); 
		angle =  uniform.nextDoubleFromTo(0,1) *(2*Math.PI); 

		xDisplacement = Math.cos(angle)*distance;
		yDisplacement = Math.sin(angle)*distance;

		point.x = xDisplacement + seedPoint.x;
		point.y = yDisplacement + seedPoint.y;
		return point; 
	}





}

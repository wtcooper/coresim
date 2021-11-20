package coresimOptimize;

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

import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.FitnessFunction;
import org.jgap.Gene;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.Population;
import org.jgap.impl.CrossoverOperator;
import org.jgap.impl.DefaultConfiguration;
import org.jgap.impl.DoubleGene;
import org.jgap.impl.IntegerGene;
import org.jgap.impl.MutationOperator;

import net.jcip.annotations.GuardedBy;
import cern.jet.random.engine.MersenneTwister;



/**
 * This is main class of CORESIM (COral REcruitment SIMulation)
 * @author Wade Cooper
 *
 */
public class RecruitmentMainOpti extends FitnessFunction {

	private static int reefNumber; 
	
	private static int MAX_ALLOWED_EVOLUTIONS; 
	private static int POPN_SIZE; 
//	private static double desperateLarvae; 

	private static double endFitness= 0;

	static int idNum = 0; 
	static long startTime; 
	private static int newSeed;
	private static final int seed = (int) System.currentTimeMillis();
	private static MersenneTwister m;
	private static final int numTransects = 2; 
	private static final int numQuadrats = 15; 

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

	private static int loopCounter = 1; 

	private static File fFile; // input file reader

	private static String paramFile; 



	/**
	 * Main method
	 * @throws InvalidConfigurationException 
	 */
	public static void main(String[] args) throws InvalidConfigurationException {
		paramFile = args[0]; 
		//System.out.println("Model start time:\t" + System.currentTimeMillis());

		startTime = System.currentTimeMillis(); 


		{	// **** INITIALIZE CODE BLOCK
			InputReaderOpti ir = new InputReaderOpti();

			// if need to read in paramFile as input
			ir.readParamFile(paramFile); 			
			reefNumber = InputReaderOpti.getReefNumber(); 
			MAX_ALLOWED_EVOLUTIONS = InputReaderOpti.getGenerations();
			POPN_SIZE = InputReaderOpti.getPopnSize(); 
			//desperateLarvae = InputReaderOpti.getDesperateLarvae(); 
			
			ir.readInputFile();											 
			siteCoverArray = InputReaderOpti.getSiteCoverArray();
			sizeFreqArray =  InputReaderOpti.getSizeFreqArray();  
			rugosityCoverArray = InputReaderOpti.getRugosityCoverArray();
			orientCoverArray = InputReaderOpti.getOrientCoverArray(); 
			subCoverArray = InputReaderOpti.getSubCoverArray(); 	
			subCoverArraySimple = InputReaderOpti.getSubCoverArraySimple(); 
			growRatesArray =  InputReaderOpti.getGrowRatesArray();  
			growSDArray =  InputReaderOpti.getGrowSDArray();   

			ir.readSurvivalFile(); 
			survRatesArray = InputReaderOpti.getSurvRatesArray(); 
			survPropArray = InputReaderOpti.getSurvPropArray(); 
			survRatesHillArray = InputReaderOpti.getSurvRatesHillArray(); 
			//		survSDArray =  InputReader.getSurvSDArray();  

			//for (int i=0; i<numReefs; i++){
			ir.readReefHabitatFile(reefNumber); // read in reef location data
			if (reefNumber ==0) coopersReefCoords = InputReaderOpti.getCoopersReefCoords(); 
			else if (reefNumber ==1) megansReefCoords = InputReaderOpti.getMegansReefCoords();
			else if (reefNumber ==2) mk14ReefCoords = InputReaderOpti.getMk14ReefCoords();
			else if (reefNumber ==3) mk16ReefCoords = InputReaderOpti.getMk16ReefCoords();
			//}

			for (int i=0; i<numDispDistributions; i++){
				ir.readConnectivityFile(i); // read in reef location data
				if (i ==0) connectivityMatrix0 = InputReaderOpti.getConnectivityMatrix0();
				else if (i==1) connectivityMatrix1 = InputReaderOpti.getConnectivityMatrix1();
				else if (i==2) connectivityMatrix2 = InputReaderOpti.getConnectivityMatrix2();
				else if (i==3) connectivityMatrix3 = InputReaderOpti.getConnectivityMatrix3();
			}

			System.out.println("Reef Number: \t" + reefNumber);

		}//***** END INITIALZE CODE BLOCK




		{ // **** JGAP CODE BLOCK
			Configuration conf = new DefaultConfiguration();

            conf.setPreservFittestIndividual(true);
            conf.setKeepPopulationSizeConstant(true);


            // For setting to non-default settings -- this had very rapid evoltion to avg fitness (~10 gen's)
/*            conf.getGeneticOperators().clear();
            CrossoverOperator _xOver = new CrossoverOperator(conf,2,true);
            MutationOperator _mutation = new MutationOperator(conf,5); 
            conf.addGeneticOperator(_mutation);
            conf.addGeneticOperator(_xOver);
*/
           // conf.setAlwaysCaculateFitness(true); 

			FitnessFunction myFunc = new RecruitmentMainOpti();

			conf.setFitnessFunction( myFunc );


			Gene[] sampleGenes = new Gene[ 3 ];

			/** For first optimization run to get which best disp dist was, used expOffset, crypOffset, larMortality, and dispDist
			 *  Did not use siteOffset because this co-varied with larvMortality
			 *  
			 *  For 2nd optimzation run to get best fit at single disp distance, just did expOffset, crypOffset, and larvMortality
			 */
			
			//sampleGenes[0] = new DoubleGene(conf, -0.04, 0.04 );  // Site offset
			sampleGenes[0] = new DoubleGene(conf, -0.04, 0.04 );  // Exp offset
			sampleGenes[1] = new DoubleGene(conf, -0.04, 0.04 );  // Cryp offset
			sampleGenes[2] = new DoubleGene(conf, 0.001, .1);  // Larval mortality range 1-70%
			//sampleGenes[3] = new IntegerGene(conf, 1, 3 );  // Dispersal distance
			//sampleGenes[4] = new IntegerGene(conf, 0, 1);  // SurvType
			//sampleGenes[5] = new IntegerGene(conf, 0, 1);  // DesperateLarvae
			//sampleGenes[6] = new DoubleGene(conf, 0.5, 1);  // autoCorr
			//sampleGenes[6] = new IntegerGene(conf, 0, 1);  // Settle Mortality

			
			
			Chromosome sampleChromosome = new Chromosome(conf, sampleGenes );

			conf.setSampleChromosome( sampleChromosome );

			// Finally, we need to tell the Configuration object how many
			// Chromosomes we want in our population. The more Chromosomes,
			// the larger the number of potential solutions (which is good
			// for finding the answer), but the longer it will take to evolve
			// the population each round. We'll set the population size to
			// 500 here.
			// --------------------------------------------------------------
			conf.setPopulationSize( POPN_SIZE );

			Genotype population = Genotype.randomInitialGenotype( conf );

			for( int i = 0; i < MAX_ALLOWED_EVOLUTIONS; i++ ) {

				double avgFitness = 0; 

				population.evolve(); // method to do evolution
				
				// below code to get average fitness values for each generation to see how it increases
				Population pop = population.getPopulation(); 
				int size = pop.size(); 
				
				for (int j=0; j < size; j++){
					IChromosome eachChromosome = pop.getChromosome(j);
					avgFitness += eachChromosome.getFitnessValue(); 
				}
				
				avgFitness = avgFitness/((double) pop.size()); 
				
				IChromosome bestSolutionSoFar = population.getFittestChromosome();

				//Double siteOffsetClass = (Double) a_subject.getGene(0).getAllele();
				//double siteOffset = siteOffsetClass.doubleValue(); 

				Double expOffsetClass = (Double) bestSolutionSoFar.getGene(0).getAllele();
				double expOffset = expOffsetClass.doubleValue(); 

				Double crypOffsetClass = (Double) bestSolutionSoFar.getGene(1).getAllele();
				double crypOffset = crypOffsetClass.doubleValue(); 

				Double larvalMortalityClass = (Double) bestSolutionSoFar.getGene(2).getAllele();
				double larvalMortality = larvalMortalityClass.doubleValue(); 

				//Integer dispDistClass = (Integer) bestSolutionSoFar.getGene(3).getAllele();
				//int dispersalDistance = dispDistClass.intValue(); 

/*				Integer survTypeClass = (Integer) bestSolutionSoFar.getGene(4).getAllele();
				int survType = survTypeClass.intValue(); 
				
				Integer desLarvaClass = (Integer) bestSolutionSoFar.getGene(5).getAllele();
				int desperateLarvae = desLarvaClass.intValue(); 

				Integer settleMorClass = (Integer) bestSolutionSoFar.getGene(6).getAllele();
				int settleMortality = settleMorClass.intValue(); 
*/				
				
				double runTime = ( (double) System.currentTimeMillis()-startTime)/(1000*60); //runtime in minutes
				
				System.out.println("gen:\t" + loopCounter + "\tbest fitness so far: \t" + bestSolutionSoFar.getFitnessValue()
						 +"\t"+ "avg fitness: \t" + avgFitness +"\t"+ "runTime:\t" + runTime
						/*+"\t"+ siteOffset */+"\t"+ expOffset +"\t"+ crypOffset 
						+"\t"+ larvalMortality /*+"\t"+dispersalDistance +"\t"+ survType +"\t"+ desperateLarvae +"\t"+ settleMortality*/);
				
				loopCounter++; 
			
			} // END EVOLVE loop through all generations

			


		} // **** END JGAP CODE BLOCK



	}


	/**
	 * Method for performing fitness evaluation. 
	 * 
	 */
	public double evaluate( IChromosome a_subject ) {

		double fitness = 0; 

		/*newSeed = seed + idNum*100;*/  
		m = new MersenneTwister(seed); 

		// need to define parameters and pass to TimeKeeper

		//Double siteOffsetClass = (Double) a_subject.getGene(0).getAllele();
		//double siteOffset = siteOffsetClass.doubleValue(); 

		Double expOffsetClass = (Double) a_subject.getGene(0).getAllele();
		double expOffset = expOffsetClass.doubleValue(); 

//		System.out.println("expOffset:\t" + expOffset);

		Double crypOffsetClass = (Double) a_subject.getGene(1).getAllele();
		double crypOffset = crypOffsetClass.doubleValue(); 

//		System.out.println("crypOffset :\t" + crypOffset );

		Double larvalMortalityClass = (Double) a_subject.getGene(2).getAllele();
		double larvalMortality = larvalMortalityClass.doubleValue(); 

//		System.out.println("larvalMortality :\t" + larvalMortality );

//		Integer dispDistClass = (Integer) a_subject.getGene(3).getAllele();
//		int dispersalDistance = dispDistClass.intValue(); 

//		System.out.println("dispersalDistance:\t" + dispersalDistance);
		
//		Integer survTypeClass = (Integer) a_subject.getGene(4).getAllele();
//		int survType = survTypeClass.intValue(); 
		
//		System.out.println("survType :\t" + survType );

//		Integer desLarvaClass = (Integer) a_subject.getGene(5).getAllele();
//		int desperateLarvae = desLarvaClass.intValue(); 

//		System.out.println("desp Larvae:\t" + desperateLarvae );

//		Integer settleMorClass = (Integer) a_subject.getGene(6).getAllele();
//		int settleMortality = settleMorClass.intValue(); 

//		System.out.println("settleMortality :\t" + settleMortality );
		
		//Double autoCorrClass = (Double) a_subject.getGene(6).getAllele();
		//double autoCorr = autoCorrClass.doubleValue(); 



		TimeKeeperOpti tk = new TimeKeeperOpti(this, m, reefNumber, /*siteOffset, */expOffset, crypOffset, 
				larvalMortality /*, dispersalDistance , survType , desperateLarvae, settleMortality*/);
		fitness = tk.run();


		return fitness; 

	} // end evaluate()











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

}

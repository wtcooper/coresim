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
import org.jgap.impl.DefaultConfiguration;
import org.jgap.impl.DoubleGene;
import org.jgap.impl.IntegerGene;

import net.jcip.annotations.GuardedBy;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;



/**
 * This is main class of CORESIM (COral REcruitment SIMulation)
 * @author Wade Cooper
 *
 */
public class SubAutoCorrOptimizer extends FitnessFunction {

	private static int MAX_ALLOWED_EVOLUTIONS = 50; 
	private static int POPN_SIZE = 100; 

	private static int reefNumber; // = 0;

	private static int loopCounter = 0; 

	private static int newSeed;
	private static final int seed = (int) System.currentTimeMillis();
	private static MersenneTwister m = new MersenneTwister(seed);  
	private Uniform uniform = new Uniform(0, 100, m);  

	private static int numReplicates = 10; 

	private static double[][]finalAutoCorrValues = new double[4][numReplicates]; 
	private static double[] avgFinalAutoCorrValues = new double[4]; 


	/**
	 * Main method
	 * @throws InvalidConfigurationException 
	 */
	public static void main(String[] args) throws InvalidConfigurationException {

		for (int k=0; k<4; k++){
			
		
			reefNumber = k; 
		
			int batchLoopCounter = 0; 
			
			for (int l=0; l<numReplicates; l++){


				{ // **** GPAP CODE BLOCK
					Configuration conf = new DefaultConfiguration();
					conf.setAlwaysCaculateFitness(true); 


					
					FitnessFunction myFunc = new SubAutoCorrOptimizer();

					conf.setFitnessFunction( myFunc );


					Gene[] sampleGenes = new Gene[ 1];

					// here, have anything <0 be random; therefore, 1/6th of starting individuals should be random
					// anything from 0-.5 would be more uniform (with lowest positive numbers most uniform)
					// anything from .5-1 would be more aggregated (with highest positive values most aggregated)
					sampleGenes[0] = new DoubleGene(conf, -0.2, .999);  // Site offset


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

							//Double autoCorrIndClass = (Double) eachChromosome.getGene(0).getAllele();
							//double autoCorrIndivid = autoCorrIndClass.doubleValue(); 
							//System.out.println(autoCorrIndivid); 
						}

						avgFitness = avgFitness/((double) pop.size()); 

						IChromosome bestSolutionSoFar = population.getFittestChromosome();

						Double autoCorrClass = (Double) bestSolutionSoFar.getGene(0).getAllele();
						double autoCorrX = autoCorrClass.doubleValue(); 

						//System.out.println("gen:\t" + loopCounter + "\tbest fitness: \t" + bestSolutionSoFar.getFitnessValue()
						//		+"\t"+ "avg fitness: \t" + avgFitness +"\t"+ "AutoCorr value:\t" + autoCorrX);


						finalAutoCorrValues [k][l] = autoCorrX; 

						loopCounter++; 

					} // END EVOLVE loop through all generations




				} // **** END GGAP CODE BLOCK

				//if (batchLoopCounter%10 == 0) {
					System.out.println("reefNum:\t" + k + "\tNumIterations: \t" + batchLoopCounter); 
				//}

				batchLoopCounter++; 
				Configuration.reset(); 

			}

		}
		
		
		for (int k=0; k<4; k++){
			double sum = 0;
			double mean = 0; 
			double sumSquares = 0; 
			double stDev = 0; 
			
			for (int l=0; l<numReplicates; l++){
				sum+=finalAutoCorrValues[k][l];
			}
			
			mean = sum/(double) numReplicates; 
			
			for (int l=0; l<numReplicates; l++){
				sumSquares += (finalAutoCorrValues[k][l] - mean)*(finalAutoCorrValues[k][l] - mean);
			}
			
			stDev = Math.sqrt(sumSquares/((double)numReplicates-1)); 
			System.out.println("reefNum: \t" + k + "\tavgAutoCorr:\t" + mean + "\tavgAutoCorrSD:\t" + stDev); 
		}

		
		

	}


	/**
	 * Method for performing batch runs. 
	 * @throws InterruptedException 
	 */
	public double evaluate( IChromosome a_subject ) {

		int subType; 
		double subProb; 

		double baseProb1, baseProb2, baseProb3; 
		double prob1=0, prob2=0, prob3=0; 
		double sumProb = 0; 

		
			//w/ bare as crypt/preferred:		
			//COOPERS	0.369863014	0.198630137
			//MEGANS	0.379432624	0.287234043
			//MK14	0.342931937	0.193717277
			//MK16	0.435897436	0.105769231
			//MK9	0.192307692	0.173076923

		// array of the observed cover of the "exposed" and "cryptic" substrate types from field substrate surveys
		double[][] subCoverArraySimple = 	{{0.369863014, 0.198630137},
				{0.379432624, 0.287234043},
				{0.342931937, 0.193717277}, 
				{0.435897436, 0.105769231}}; 

		//avg aggregation	
		//Coopers	0.089711429
		//Megans	-0.04255269
		//Marker14	0.288489115
		//Marker16	0.241764395

		// array of the observed autocorrelation coefficients from field substrate surveys
		double[] autoCorrObs = {0.089711429, -0.04255269, 0.288489115, 0.241764395}; 

		int[] substrate = new int[300]; // array to hold generated substrates

		double autoCorrEst; 
		double sumSquaresCorr = 0, sumSquaresReg = 0, gamma, variance; 


		double fitness = 0; 
		double subMean = 0; 

		// need to define parameters and pass to TimeKeeper

		Double autoCorrClass = (Double) a_subject.getGene(0).getAllele();
		double autoCorr = autoCorrClass.doubleValue(); 

		//double autoCorr = 0.9; 
		
		//System.out.println("autoCorrInput:\t" + autoCorr);
		
		
		subType = uniform.nextIntFromTo(0,2); 
		substrate[0] = subType; 
		subMean += subType; 

		int loop =0;

		for (int i = 0; i<3000; i++) {

			baseProb1 = subCoverArraySimple[reefNumber][0];
			baseProb2 = subCoverArraySimple[reefNumber][1];
			baseProb3 = 1-baseProb1-baseProb2;

			if (autoCorr <= 0 ){
				prob1=baseProb1;
				prob2=baseProb2;
				prob3=baseProb3;
				sumProb = prob1+prob2+prob3; 

			}
			else {
				if (subType ==0){
					prob1 = autoCorr;
					prob2 = (1-autoCorr)*(baseProb2/(baseProb1+baseProb2+baseProb3)); 
					prob3 = (1-autoCorr)*(baseProb3/(baseProb1+baseProb2+baseProb3)); 
					sumProb = prob1+prob2+prob3; 
				}
				else if (subType ==1){
					prob2 = autoCorr;
					prob1 = (1-autoCorr)*(baseProb1/(baseProb1+baseProb2+baseProb3)); 
					prob3 = (1-autoCorr)*(baseProb3/(baseProb1+baseProb2+baseProb3)); 
					sumProb = prob1+prob2+prob3; 
				}
				else if (subType ==2){
					prob3 = autoCorr;
					prob1 = (1-autoCorr)*(baseProb1/(baseProb1+baseProb2+baseProb3)); 
					prob2 = (1-autoCorr)*(baseProb2/(baseProb1+baseProb2+baseProb3)); 
					sumProb = prob1+prob2+prob3; 
				}
			}

			// Explore potential ways to make rugosity via probability draws
			subProb = uniform.nextDoubleFromTo(0, sumProb);

			if (subProb < prob1) subType = 0; // exposed
			else if (subProb < (prob1 + prob2))subType= 1; // cryptic
			else subType= 2; // other


			if (loop%10 == 0){
				substrate[(int) i/10] = subType; 
				subMean += subType; 
			}
			loop++; 
		}

		//System.out.println("subMean sum: \t" + subMean); 
		subMean = subMean/300; 
		//System.out.println("subMean mean: \t" + subMean); 

		for (int i=0; i<300; i++){

			if (i<299) sumSquaresCorr += (substrate[i]-subMean)*(substrate[i+1]-subMean);
			sumSquaresReg += (substrate[i]-subMean)*(substrate[i]-subMean);
			
			//System.out.println(substrate[i]); 

		}

		gamma = sumSquaresCorr/299;
		variance = sumSquaresReg/300; 
		autoCorrEst = gamma/variance; 

		//System.out.println("autoCorrOutput:\t" + autoCorrEst); 

		fitness = 1 - (Math.abs((autoCorrEst-autoCorrObs[reefNumber])/autoCorrObs[reefNumber])) + 10000;


		return fitness; 

	} // end evaluate()





}

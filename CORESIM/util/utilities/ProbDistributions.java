package utilities;

import java.awt.Point;
import java.util.Random;

import cern.jet.random.Beta;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;

public class ProbDistributions {

	/**
	 * @param args
	 */
	public ProbDistributions(){

	}



	public static void main(String[] args) {
		long seed = System.currentTimeMillis();
		Random rng = new Random(seed); 


		double lightValue;
		double lightMax = 2500; 
		double lightMean = 100; 
		double lightStdev = 300; 
		double prob, prob2, prob3; 
		double lightValDec;

		MersenneTwister m = new MersenneTwister();
		Normal normal = new Normal(lightMean, lightStdev, m); 
//		Beta beta = new Beta(2, 4, m);
		
		lightValue = 0;
		
		for (int i = 0; i<40; i++){
			prob = normal.pdf(lightValue);
			System.out.println("Light Value: \t" + lightValue +  "\tprob: \t" + prob);
			prob3 = (Math.abs(normal.pdf(lightValue)))/(Math.abs(normal.pdf(lightMean)));
			System.out.println("\tprob3: \t" + prob3);
			System.out.println();
			
			if (rng.nextDouble() < prob3){
				System.out.println("build on this light intensity");

			}
			System.out.println();

//			prob2 = normal.nextDouble(); 
//			System.out.println("\tRandom Number generator: \t" + prob2);
//			System.out.println();
			
			lightValue = lightValue + 10; 


		}

	}

}

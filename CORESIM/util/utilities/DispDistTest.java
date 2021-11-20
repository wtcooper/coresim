package utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cern.jet.random.Beta;
import cern.jet.random.Normal;
import cern.jet.random.engine.MersenneTwister;

public class DispDistTest {

	static long seed = System.currentTimeMillis();
	static MersenneTwister m = new MersenneTwister((int) seed);
	static Beta betaDist = new Beta(2, 4, m);
	static double dispersalDistance; 
	static private double alpha, beta; 
	static private double[] alphaArray = {1.1, 1.1, 1.1, 1.1}; 
	static private double[] betaArray = {100, 35, 10, 2}; 
	static List<Double> list = new ArrayList<Double>(); 
	
	public static void main(String[] args) {

		int i = 3; 
		alpha = alphaArray[i];
		beta = betaArray[i]; 
		
		int numLarva = 1000000;
		double numLarvaDouble = numLarva;
		double average, median; 
		double total = 0; 
		int medianIndex = (int) (numLarvaDouble/2); 
		//System.out.println("median index: " + medianIndex);
		
		for (int j = 0; j<numLarva; j++){
			dispersalDistance = betaDist.nextDouble(alpha,beta)*1000;
			list.add(dispersalDistance);
			total += dispersalDistance;
		}
		
		Collections.sort(list); 
		
		//for (int j = 0; j < 50; j++){
		//	System.out.println(list.get(j));
		//}
		
		System.out.println("min for dispersal dist " + i + ": " + list.get(0));		
		System.out.println("median for dispersal dist " + i + ": " + list.get(medianIndex));		
		System.out.println("max for dispersal dist " + i + ": " + list.get(numLarva-1));		
		System.out.println(); 
		average = total/((double)numLarva);
		System.out.println("average for dispersal dist " + i + ": " + average);		
	}

}

package utilities;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

import cern.jet.random.Beta;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;


public class ConnectivityMatrix {

	int seed = (int) System.currentTimeMillis();
	MersenneTwister m = new MersenneTwister(seed);
	Normal normal = new Normal(1,1,m);
	Beta betaDist = new Beta(1, 1, m);
	Uniform uniform = new Uniform(0, 100, m); 

	int gridWidth = 320, gridHeight = 320; 

	double totalLarvaePerCell = 10000000; 
	
	private double[] alphaArray = {1.1, 1.1, 1.1, 1.1}; 
	private double[] betaArray = {100, 35, 10, 2}; 
	private double alpha;
	private double beta; 
	private int dispersalDist; 
	double xDisplacement, yDisplacement, dispersalDistance; 
	double angle=0; 		

	int larvaeX, larvaeY, coralX, coralY; 

	int sourceMatrixElement; 
	int sinkMatrixElement; 
	int newSinkElement;
	int tempElement1; 
	int tempElement2; 
	int tempElement3; 


	// here, total larvae released per cell will be 1,000,000
	private double[][] settledLarvae= new double[16][15876]; 

	private double[][] connectivityMatrix = new double[16][15876]; 
	private PrintWriter outFile=null;	


	public void buildMatrix(){


		// loop through the 4 possible combinations of dispersal types
		for (int i = 0; i < 4; i++) {

			dispersalDist = i; 
			alpha = alphaArray[i];
			beta = betaArray[i]; 

			for (int j = 0; j < 16; j++) {
				for (int k = 0; k < 15876; k++) {
					connectivityMatrix[j][k] = 0; 
					settledLarvae[j][k] = 0; 
				}
			}

			for (int x = 0; x < 126; x++) {
				for (int y = 0; y < 126; y++) {


					// only do for areas outside focal simulation landscape
					if((x >= 61) && (x <= 64) && (y >= 61) && (y <= 64) ){}
					else {
					
//						System.out.println("x: \t" + x + "\ty: \t" + y);
//						System.out.println("cell outside focal area");
						coralX = x; //this would put corner of simulation landscape at 0,0
						coralY = y; 
						sourceMatrixElement = coralX + (coralY * 126);

//						System.out.println("sourceMatrix\t" + sourceMatrixElement);

						
						for (int l = 0; l < totalLarvaePerCell; l++) {
							dispersalDistance = betaDist.nextDouble(alpha,beta)*126; 

//							System.out.println("disp distance \t" + dispersalDistance);

							angle = uniform.nextDoubleFromTo(0, 1)*(2*Math.PI); 

							xDisplacement = Math.cos(angle)*dispersalDistance;
							yDisplacement = Math.sin(angle)*dispersalDistance;

//							System.out.println("xDisplace: \t" + xDisplacement + "\tyDisplament: \t" + yDisplacement);

							larvaeX = (int) xDisplacement + coralX;
							larvaeY = (int) yDisplacement + coralY;

							if((larvaeX >= 61) && (larvaeX <= 64) && (larvaeY >= 61) && (larvaeY <= 64) ){

								sinkMatrixElement = larvaeX + (larvaeY * 126); 

								// rescale the sinkMatrixElement so the values are in order from 0-63 to correspond to settledLarvae array
								tempElement1 = sinkMatrixElement - 7747; 
								tempElement2 = (int) tempElement1/126;
								tempElement3 = tempElement1%126; 
								newSinkElement = (tempElement2*4) + tempElement3; 

//								System.out.println("sinkMatrix\t" + newSinkElement);

								settledLarvae[newSinkElement][sourceMatrixElement] = settledLarvae[newSinkElement][sourceMatrixElement]+1; 
								
							}

							
						} // end "l" for loop to do 1000000 larvae release

					} // end if loop to check and make sure is only for outside area


				} // end "y" for loop going over 250 cells
			} // end "x" for loop going over 250 cells

			
			for (int j = 0; j < 16; j++) {
				for (int k = 0; k < 15876; k++) {
					connectivityMatrix[j][k] = (settledLarvae[j][k])/totalLarvaePerCell; 
				}
			}

			writeOutput(); 
			
		} // end "i" for loop going over 4 total dispersal distributions

	} // end buildMatrix() method



	public void writeOutput() {

		try {  outFile = new PrintWriter(new FileWriter("ConnectivityMatrix"+dispersalDist+".txt", true));
		} catch (IOException e) {e.printStackTrace();	}

		for (int i = 0; i < 15876; i++) {
			for (int j = 0; j < 16; j++) {

				outFile.print(connectivityMatrix[j][i] + "\t" );
			}
			outFile.println(); 
		}

		outFile.println(); 
		outFile.close();


	}


	public static void main(String[] args) {
		System.out.println("Model start time:\t" + System.currentTimeMillis());

		ConnectivityMatrix connect = new ConnectivityMatrix(); 
		connect.buildMatrix(); 

		System.out.println("Model end time:\t" + System.currentTimeMillis());

	}

}

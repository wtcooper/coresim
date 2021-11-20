package coresim;

import java.awt.Point;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import cern.jet.random.Uniform;


public class PatternRecorder {

	private Uniform uniform; 
	private RecruitmentMain rm; 

	HashMap<Point, ArrayList<Recruit>> recruitMap; 
	ArrayList<Recruit> recruitList; 

	private double size; 
	private double totalAbundance; 
	// size frequency array for recruits, with categories: <.2cm, .2-1cm, 1-2cm, 2-3cm, 3-4cm, 4-5cm
	private int[] totalSizeFreq = new int[6];
	private int[] avgQuadratSizeFreq = new int[6];

	private int numTransects =2;
	private int numQuadrats = 15;
	private int [][][] quadratMidPoints = new int [numTransects][numQuadrats][2]; //here, 2 transects, fifteen quadrats, 2 points (x,y)
	private int[][] quadratDensity = new int[numTransects][numQuadrats];
	private int[][] quadratCoralCover = new int[numTransects][numQuadrats];
	private int[][] twoMeterCoralCover = new int[numTransects][numQuadrats];
	
	private int[][][] quadratSizeFreq = new int[numTransects][numQuadrats][6];
	private int[][] coralArray;
	private int coralCover; 

	private double avgDensity; // avgDensity1Tran, avgDensity2Tran, avgDensity3Tran, avgDensity4Tran ; 
	private double avgDensitySD; //, avgDensitySD1Tran, avgDensitySD2Tran, avgDensitySD3Tran, avgDensitySD4Tran; 

	private int quadratAbundance; 
	private int gridWidth, gridHeight; 
	private int locX, locY; 
	private int reefNumber; 
	private int paramID; 

	//Calibration variables
	private double 	fitDensity=0, fitDensitySD=0, fitTotal = 0;  
	private double fitSizeFreqSkew=0, fitSizeFreqKurt=0;
	private double sizeFreqSkew=0, sizeFreqKurt=0; 
	private double meanSize=0, sumSize=0; 
	private double sumSquares=0, sumCubes=0, sumForths=0, stDev=0; 
	
	// Calibration for substrate
	private double sumExp = 0, sumCryp=0, fitSubExp, fitSubCryp, propExp, propCryp; 
	private int subType; 
	
	// actual data
	private double[] density = {5.2, 1.73333333, 1.142857143, 0.6666666666};
	private double[] densitySD = {5.054189113, 3.433439347, 2.634729387, 2.122674522};
	private double skewness = 0.930874273, kurtosis = -0.07630168; 

	//private double[] subExp = {0.569444444, 0.567084079, 0.729938272, 0.794410151}; 
	//private double[] subCryp = {0.430555556, 0.432915921, 0.270061728, 0.205589849}; 

	private double subExpMeasured = 0.416267943; 
	private double subCrypMeasured = 0.583732057;
	
	private PrintWriter outFile = null;
	
	public PatternRecorder(TimeKeeper tk, RecruitmentMain rm, Recruiter recruiter) {
		this.rm = rm; 
		gridHeight = RecruitmentMain.getGridHeight();
		gridWidth = RecruitmentMain.getGridWidth(); 
		coralArray = tk.getCoralArray();
		reefNumber = tk.getReefNumber(); 
		uniform = tk.getUniform();
		paramID = tk.getParamID(); 
		recruitMap = recruiter.getRecruitMap();
		recruitList = recruiter.getRecruitList(); 
		
	}


	void recordPatterns() {

		recordTotalSitePatterns(); 
		recordQuadratPatterns(); 
		recordFitness(); 
		//writeOutput(); 
		rm.writeOutput(this, paramID); 
	}


	/** Method to record the size frequency and total abundance for all recruits at a site
	 * 
	 */
	void recordTotalSitePatterns() {

		totalAbundance = 0; 

		for (int i=0; i<6; i++) {
			totalSizeFreq[i] = 0; 
		}

		// Routine to record 
		for (int i = 0; i < totalSizeFreq.length; i++) {
			totalSizeFreq[i] = 0; 
		}


		for (Recruit recruit: recruitList) {
			size = recruit.getDiameter(); 
			subType = recruit.getSubType(); 
			
			if(size < 5){ // if size is less than 5cm

				sumSize = sumSize + size; 
				totalAbundance++; 

				if (subType == 0) sumExp++;
				if (subType == 1) sumCryp++;
				
				
				if ((size >= 0) && (size <.2)) 			totalSizeFreq[0]++; 
				else if ((size >= .2) && (size <1)) 	totalSizeFreq[1]++; 
				else if ((size >= 1) && (size <2)) 		totalSizeFreq[2]++; 
				else if ((size >= 2) && (size <3)) 		totalSizeFreq[3]++; 
				else if ((size >= 3) && (size <4)) 		totalSizeFreq[4]++; 
				else if ((size >= 4) && (size <5)) 		totalSizeFreq[5]++; 
			}
		}
		
		//private double fitDensity, fitDensitySD, fitSizeFreqSkew, fitSizeFreqKurt;
		//private double meanSize, sumSize=0; 
		//private double sumSquares=0, sumCubes=0, sumForths=0; 

		meanSize = sumSize/(totalAbundance); 
		
		propExp = sumExp/(sumExp+sumCryp);
		propCryp = sumCryp/(sumExp+sumCryp);
		
		
		for (Recruit recruit: recruitList) {
			size = recruit.getDiameter(); 

			if(size < 5){ // if size is less than 5cm
				sumSquares = sumSquares + Math.pow((size-meanSize), 2);
				sumCubes = sumCubes + Math.pow((size-meanSize), 3);
				sumForths = sumForths + Math.pow((size-meanSize), 4);
			}
		}
		// calculate standard deviation of sizes
		stDev = Math.sqrt(sumSquares/((totalAbundance-1)));
		// calculate skewness of sizes
		sizeFreqSkew = (totalAbundance*sumCubes)/((totalAbundance-1)*(totalAbundance-2)*Math.pow(stDev,3));
		// calculate kurtosis of sizes
		sizeFreqKurt = (totalAbundance*(totalAbundance+1)*sumForths)/((totalAbundance-1)*(totalAbundance-2)*(totalAbundance-3)*Math.pow(stDev,4))
			- ((3*(totalAbundance-1)*(totalAbundance-1))/((totalAbundance-2)*(totalAbundance-3)));

		//System.out.println("meanSize: \t" + meanSize + "\tstDev: \t" + stDev + "\tskew: \t" + sizeFreqSkew + "\tkurt: \t" + sizeFreqKurt);
		
	} // end of recordTotalSitePatterns


	
	/** Method which records patterns at sample quadrats
	 * 
	 */
	void recordQuadratPatterns() {


		// sets the midpoint of the first quadrat on the first transect
		quadratMidPoints[0][0][0] = uniform.nextIntFromTo(0,gridWidth); 
		quadratMidPoints[0][0][1] = uniform.nextIntFromTo(0,gridHeight); 

		// sets the midpoint of the first quadrat on subsequent transects, each 8-12m away, with starting point +/- 5m up or down
		for (int i = 1; i < numTransects; i++) {
			quadratMidPoints[i][0][0] = getWrapAround(quadratMidPoints[i-1][0][0] + uniform.nextIntFromTo(80,120)); 
			quadratMidPoints[i][0][1] = getWrapAround(quadratMidPoints[i-1][0][1] + uniform.nextIntFromTo(-25,25)); 
		}

		// sets the midpoint of each quadrat along a transect and initializes the quadratSizeFreq array

		for (int i = 0; i < numTransects; i++) {
			for (int j = 1; j < numQuadrats; j++) {

				quadratSizeFreq[i][j][0]=0;
				quadratSizeFreq[i][j][1]=0;
				quadratSizeFreq[i][j][2]=0;
				quadratSizeFreq[i][j][3]=0;
				quadratSizeFreq[i][j][4]=0;
				quadratSizeFreq[i][j][5]=0;
				avgQuadratSizeFreq[0]=0;
				avgQuadratSizeFreq[1]=0;
				avgQuadratSizeFreq[2]=0;
				avgQuadratSizeFreq[3]=0;
				avgQuadratSizeFreq[4]=0;
				avgQuadratSizeFreq[5]=0;

				// if even # quadrat
				if (j%2 == 0){
					quadratMidPoints[i][j][0] = quadratMidPoints[i][0][0];  
					quadratMidPoints[i][j][1] = getWrapAround(quadratMidPoints[i][0][1] + j*20); 
				}
				// if odd # quadrat
				if (j%2 != 0){
					quadratMidPoints[i][j][0] = getWrapAround(quadratMidPoints[i][0][0] + 5);  
					quadratMidPoints[i][j][1] = getWrapAround(quadratMidPoints[i][0][1] + j*20); 
				}
			}
		} // end of for loop over numQuadrats


		
		
		// loop through each quadrat and record recruit properties
		for (int i = 0; i < numTransects; i++) {
			for (int j = 0; j < numQuadrats; j++) {

				locX = quadratMidPoints[i][j][0]; 
				locY = quadratMidPoints[i][j][1]; 

				quadratAbundance = 0; 
				coralCover = 0; 
				for (int k = locX-2; k <= locX+2; k++) {
					for (int l = locY-2; l <= locY+2; l++) {


						ArrayList<Recruit> list = getRecruits(k, l);
						if (list != null) {
							for (Recruit recruit: list) {
								size = recruit.getDiameter(); 
								if(size < 5){ // if size is less than 5cm
									quadratAbundance++; 

									if ((size >= 0) && (size <.2)) 	{
										quadratSizeFreq[i][j][0]++;
										avgQuadratSizeFreq[0]++;
									}
									else if ((size >= .2) && (size <1))  {
										quadratSizeFreq[i][j][1]++;
										avgQuadratSizeFreq[1]++;
									}
									else if ((size >= 1) && (size <2)) {
										quadratSizeFreq[i][j][2]++;
										avgQuadratSizeFreq[2]++;
									}
									else if ((size >= 2) && (size <3)) {
										quadratSizeFreq[i][j][3]++;
										avgQuadratSizeFreq[3]++;
									}
									else if ((size >= 3) && (size <4))  {
										quadratSizeFreq[i][j][4]++;
										avgQuadratSizeFreq[4]++;
									}
									else if ((size >= 4) && (size <5))  {
										quadratSizeFreq[i][j][5]++;
										avgQuadratSizeFreq[5]++;
									}
								}
							}
						}

						if (coralArray[getWrapAround(k)][getWrapAround(l)] == 4) {
							coralCover++; 
						}


					} // for "l" loop
				} // for "k" loop

				// get density per m squared
				quadratDensity[i][j] = quadratAbundance*4; 
				quadratCoralCover[i][j] = coralCover; 


				coralCover = 0; 
				// get twoMeterCoralCover for each quadrat
				// if even number quadrat
				if (j%2 == 0){
					for (int k = locX-2; k <= locX+7; k++) {
						for (int l = locY-4; l <= locY+5; l++) {
							if (coralArray[getWrapAround(k)][getWrapAround(l)] == 4) {
								coralCover++; 
							}
						}
					}
				}
				// if odd # quadrat
				if (j%2 != 0){
					for (int k = locX-7; k <= locX+2; k++) {
						for (int l = locY-4; l <= locY+5; l++) {
							if (coralArray[getWrapAround(k)][getWrapAround(l)] == 4) {
								coralCover++; 
							}
						}
					}
				}

				twoMeterCoralCover[i][j] = coralCover; 

			} // for "j" loop over quadrats
		} // for "i" loop over transects


		
		
		// record avg Density 
		avgDensity=0; //avgDensity1Tran = 0; avgDensity2Tran=0; avgDensity3Tran=0; avgDensity4Tran=0; 
		
		for (int i = 0; i < numTransects; i++) {
			for (int j = 0; j < numQuadrats; j++) {
				avgDensity = avgDensity + quadratDensity[i][j];
			}
			//if (i==0)avgDensity1Tran = avgDensity;
			//if (i==1)avgDensity2Tran = avgDensity;
			//if (i==2)avgDensity3Tran = avgDensity;
			//if (i==3)avgDensity4Tran = avgDensity;
		}		
		
		//avgDensity1Tran = avgDensity1Tran/(numQuadrats);
		//avgDensity2Tran = avgDensity2Tran/(2*numQuadrats);
		//avgDensity3Tran = avgDensity3Tran/(3*numQuadrats);
		//avgDensity4Tran = avgDensity4Tran/(4*numQuadrats);

		avgDensity= avgDensity/(numTransects*numQuadrats);
		

		// record avg Density standard deviation
		avgDensitySD = 0; //avgDensitySD1Tran=0; avgDensitySD2Tran=0; avgDensitySD3Tran=0; avgDensitySD4Tran=0; 
		
		for (int i = 0; i < numTransects; i++) {
			for (int j = 0; j < numQuadrats; j++) {

				//if (i<=0)avgDensitySD1Tran = avgDensitySD1Tran + (quadratDensity[i][j]-avgDensity1Tran)*(quadratDensity[i][j]-avgDensity1Tran);
				//if (i<=1)avgDensitySD2Tran = avgDensitySD2Tran + (quadratDensity[i][j]-avgDensity2Tran)*(quadratDensity[i][j]-avgDensity2Tran);
				//if (i<=2)avgDensitySD3Tran = avgDensitySD3Tran + (quadratDensity[i][j]-avgDensity3Tran)*(quadratDensity[i][j]-avgDensity3Tran);
				//if (i<=3)avgDensitySD4Tran = avgDensitySD4Tran + (quadratDensity[i][j]-avgDensity4Tran)*(quadratDensity[i][j]-avgDensity4Tran);
				
				avgDensitySD = avgDensitySD + (quadratDensity[i][j]-avgDensity)*(quadratDensity[i][j]-avgDensity);  // sum of squared deviation
			}
		}		
		//avgDensitySD1Tran = Math.sqrt(avgDensitySD1Tran /(1*numQuadrats)); // take sqare root of average squared deviation 
		//avgDensitySD2Tran = Math.sqrt(avgDensitySD2Tran /(2*numQuadrats)); // take sqare root of average squared deviation 
		//avgDensitySD3Tran = Math.sqrt(avgDensitySD3Tran /(3*numQuadrats)); // take sqare root of average squared deviation 
		//avgDensitySD4Tran = Math.sqrt(avgDensitySD4Tran /(4*numQuadrats)); // take sqare root of average squared deviation 
		avgDensitySD = Math.sqrt(avgDensitySD /(numTransects*numQuadrats-1)); // take sqare root of average squared deviation
	
	} // end of recordQuadratPatterns method


	
	
	
	void recordFitness() {
		
		// calculate the density fitness value, old value accounting for both 
		//if ((avgDensity >= (density[reefNumber]-densitySD[reefNumber])) && (avgDensity <= (density[reefNumber]+densitySD[reefNumber]))) {
		//	fitDensity = 1 - Math.abs(((avgDensity-density[reefNumber])/density[reefNumber])); // if within SD range, set closer to 1  
		//}
		//else {
		//	fitDensity = 0.5 - Math.abs(((avgDensity-density[reefNumber])/density[reefNumber])); // if outside SD range, set lower value so less likely 
		//}
		
		
		fitDensity = 1 - Math.abs(((avgDensity-density[reefNumber])/density[reefNumber])) + 10000; 
		
		fitDensitySD = 1 - Math.abs(((avgDensitySD-densitySD[reefNumber])/densitySD[reefNumber])) + 10000;
		
		
		fitSizeFreqSkew = 1 - Math.abs(((sizeFreqSkew-skewness)/skewness)) + 10000;
		// need to divide by additional number for kurtosis because measured value of kurtosis is very small (-0.07); therefore, this
		// will decrease the magnitude of the fitness variability by an order of magnitude, and therefore this value won't have such a 
		// large effect on overall fitness since is usually a large negative number (e.g., -30 in best case) while other values range around 0
		fitSizeFreqKurt = 1 - (Math.abs(((sizeFreqKurt-kurtosis)/kurtosis)))/50 + 10000;	
		
		fitSubExp = 1 - (Math.abs(((propExp-subExpMeasured)/subExpMeasured))) + 10000;
		fitSubCryp = 1 - (Math.abs(((propCryp-subCrypMeasured)/subCrypMeasured))) + 10000;

		fitTotal = (fitDensity+(0.5*fitDensitySD) + (0.25*fitSizeFreqSkew) + (0.25*fitSizeFreqKurt) 
				+ (0.25*fitSubExp) + (0.25*fitSubCryp))/2.5; 
		
	}
	

	void writeOutput() {

	try {

		outFile = new PrintWriter(new FileWriter("RecruitSizes"+reefNumber+".txt", true));
	} catch (IOException e) {
		e.printStackTrace();
	}

	for (Recruit recruit: recruitList) {
		size = recruit.getDiameter(); 

		if(size < 5){ // if size is less than 5cm

			outFile.println(size); 
		}
	}

	outFile.println(); 
	outFile.close();
}

	
	
	/**
	 * This method returns the recruits at a given x,y location
	 * @param x
	 * @param y
	 * @return
	 */
	ArrayList<Recruit> getRecruits(int x, int y){
		Point point = new Point(x,y);
		ArrayList<Recruit> list = (ArrayList<Recruit>) recruitMap.get(point);
		return list; 
	}

	
	
	int getWrapAround(int x){

		int returnValue = x; 
		if (x >= gridWidth) returnValue = x-gridWidth;
		if (x < 0) returnValue = gridWidth+x;
		return returnValue; 
	}


	public double getTotalAbundance() {
		return totalAbundance;
	}

	public int[] getTotalSizeFreq() {
		return totalSizeFreq;
	}

	public int[][] getQuadratDensity() {
		return quadratDensity;
	}

	public int[][] getQuadratCoralCover() {
		return quadratCoralCover;
	}


	public int[][][] getQuadratSizeFreq() {
		return quadratSizeFreq;
	}

	public int getReefNumber() {
		return reefNumber;
	}


	public int[][] getTwoMeterCoralCover() {
		return twoMeterCoralCover;
	}


	public double getAvgDensity() {
		return avgDensity;
	}


	public double getAvgDensitySD() {
		return avgDensitySD;
	}




	public int[] getAvgQuadratSizeFreq() {
		return avgQuadratSizeFreq;
	}


	public double getFitDensity() {
		return fitDensity;
	}


	public double getFitDensitySD() {
		return fitDensitySD;
	}


	public double getFitSizeFreqSkew() {
		return fitSizeFreqSkew;
	}


	public double getFitSizeFreqKurt() {
		return fitSizeFreqKurt;
	}


	public double getFitTotal() {
		return fitTotal;
	}


	public double getFitSubExp() {
		return fitSubExp;
	}


	public double getFitSubCryp() {
		return fitSubCryp;
	}

	public double getSizeFreqSkew() {
		return sizeFreqSkew;
	}



	public double getSizeFreqKurt() {
		return sizeFreqKurt;
	}



	public double getPropExp() {
		return propExp;
	}



	public double getPropCryp() {
		return propCryp;
	}

}

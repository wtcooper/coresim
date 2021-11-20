package coralSampling;

import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;

public class Sampler {

	private static final int seed = (int) System.currentTimeMillis();
	private static MersenneTwister m = new MersenneTwister(seed);
	Uniform uniform = new Uniform(m); 
	Normal normal = new Normal(0,1,m);
	
	private int numSpecies = 5; 
	private int numSizeFreqBins = 10; //10 cm bins; up to 
	private double sizeFreqBinSize = 10; // do this in cm

	private double[][] sizeFreqArray = new double[numSpecies][numSizeFreqBins]; // species x sizeFreqBins  
	private double[] siteCover = new double[numSpecies]; // site cover for each species 
	private double[] alpha = new double[numSpecies]; 
	private double[] beta = new double[numSpecies]; 
	private double[] betaStd = new double[numSpecies]; 

	private double reefWidth = 30; //width of reef in meters
	private double reefHeight = 30; //height of reef in meters
	private double reefArea = reefWidth*reefHeight; 
	
	private int numCirclePoints = 12; 
	
	public void run(){
		CoralBuilder builder = new CoralBuilder(this);
		builder.buildCorals();
		
		// TODO - run transects here
		
	}
	
	public static void main(String[] args) {
		Sampler sampler = new Sampler(); 
		sampler.run();
	}

	public Uniform getUniform() {
		return uniform;
	}

	public Normal getNormal() {
		return normal;
	}

	public int getNumSpecies() {
		return numSpecies;
	}

	public int getNumSizeFreqBins() {
		return numSizeFreqBins;
	}

	public double getSizeFreqBinSize() {
		return sizeFreqBinSize;
	}

	public double[][] getSizeFreqArray() {
		return sizeFreqArray;
	}

	public double[] getSiteCover() {
		return siteCover;
	}

	public double[] getAlpha() {
		return alpha;
	}

	public double[] getBeta() {
		return beta;
	}

	public double[] getBetaStd() {
		return betaStd;
	}

	public double getReefWidth() {
		return reefWidth;
	}

	public double getReefHeight() {
		return reefHeight;
	}

	public double getReefArea() {
		return reefArea;
	}

	public int getNumCirclePoints() {
		return numCirclePoints;
	}

	


	
}

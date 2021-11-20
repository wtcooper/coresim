package coresimElasticity;

import java.util.Random;

import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;

public class FractalTerrainElast  {

	private TimeKeeperElast tk; 

	//	lod		gridwidth/height
	//  5			32
	//	6			64
	//	7			128
	//	8			256
	//	9			512
	//	10			1024

	private MersenneTwister m; 
	private Uniform uniform; 

	private double[][] terrain;
	private double[][] terrainAdjust;
	private double roughness, min, max;
	private int divisions, lod;
	private Random rng;



	public FractalTerrainElast (TimeKeeperElast tk, int lod, double roughness, MersenneTwister m) {
		this.tk = tk; 
		this.roughness = roughness; // this is H, where H is between 0-1; 2^(-H) is the amount to multiply the random number by each time
		this.lod = lod;
		this.m = m;
		this.divisions = 1 << lod;  // this is equal to saying 1*2^(lod), so here if lod=7, then divisions (or cells) will be 128
		terrain = new double[divisions + 1][divisions + 1]; // here, sets the terrain array to appropriate size; lod=7 sets terrain to 129x129
		terrainAdjust = new double[divisions][divisions]; // this will be final version to pass to main method, which doesn't include the last elements which are same value as first elements for seemless wrapping
		uniform = tk.getUniform(); 
	
	}


	// this is the method that will be called from the main method 
	// will return the terrain array reference

	public double[][] computeTerrain() {

		double cornerHeight; 
		cornerHeight = rnd(); 


		terrain[0][0] = cornerHeight; // rnd (); Set this to rnd() if don't want wrappable surface
		terrain[0][divisions] = cornerHeight; // rnd (); 
		terrain[divisions][divisions] = cornerHeight; // rnd (); 
		terrain[divisions][0] = cornerHeight; // rnd (); 


		double rangeReduce = 1; 
		double range = roughness; 


		// this following code (including square method) is slightly modified from Merlin's code; it is from http://www.java.sys-con.com/read/46231.htm
		// this loops go through the total number of iterations for the entire grid
		for (int i = 0; i < lod; ++ i) {

			int q = 1 << i;      	// here, q = 1*2^i; for i=0, this will equal 1 (2^0=1)   
			int r = 1 << (lod - i); // here, for the first loop, i=0, so r = divisions or length between points
			int s = r >> 1; 		// here, s = 1/2 of the r, where r>>1 is r/(2^1); so for each loop, r = 1/2 divisions because start with i=0


			// this loops through all of the points in the grid to do a diamond application to each point needed
			for (int j = 0; j < divisions; j += r)  // assume j=x coordinate, so will do 1 point on first loop (x=0, x=divisions-1)
				for (int k = 0; k < divisions; k += r)
					diamond (j, k, r, range);  	// this passes the point coordinate, the distance between the points, and the roughness value


			if (s > 0) //this says that as long as the lod hasn't been finished (i.e., on last loop), keep doing square
				for (int j = 0; j < divisions; j += s) // j is x coordinate; s is half the divisions

					// here k is y coordinate, and starts at 64 ((0+64)/r = 0 with 64 remainder)
					// the next loop will be when k = r, so will do a second point at (0, 128) assumming lod = 7
					for (int k = (j + s) % r; k < divisions; k += r)	// (j+s)%r for fraction is simply the numerator, i.e., for 1st loop is 64
						square (j, k, r, range);


			// These are different options for how to change the roughness constant.  
			// the range *= roughness seems to produce the most "random" landscapes with initial values between 1-2


			//	    rangeReduce = Math.pow(2, -roughness);  // this should be correct interpretation, so after the first loop, the scale will now be
			//	    range = range * rangeReduce; 
			//		  range = range/2.0f;	// this is another option as proposed by "fractal purists" 
			range *= roughness;	// this is option that Merlin and others recommend       

		}



		// This code checks to see if the edges are equal which is a necessity for seamless wrapping
		/*  	for (int j = 0; j <= divisions; j++) {

	    		if (terrain[0][j] == terrain[divisions][j]) {System.out.println("Y-value" + "\t" + j + "\t" + "TRUE");}
	    		else {System.out.println("Y-value" + "\t" + j + "\t" + "FALSE");}
	   	}
		 */  	

		// This code stores the terrain values into a new array, without the equal edges; thus, when passed to HabitatBuilder it is wrappable
		for (int i = 0; i < divisions; i++) {
			for (int j = 0; j < divisions; j++) {
				terrainAdjust[i][j] = terrain[i][j];
			}
		}
		return terrainAdjust; 
	}

	// Diamond step
	private void diamond (int x, int y, int side, double scale) {
		if (side > 1) {
			int half = side / 2;
			double avg = (terrain[x][y] + terrain[x + side][y] +
					terrain[x + side][y + side] + terrain[x][y + side]) * 0.25;

			terrain[x + half][y + half] = avg + rnd () * scale;

		}
	}


	// Square step
	private void square (int x, int y, int side, double scale) {
		int half = side / 2;
		double avg = 0.0, sum = 0.0;

		// computes middle point in a diamond accounting for seamless wrapping

		// West side
		if (x-half >= 0) 	{ avg += terrain[x-half][y]; sum += 1.0; }
		else 				{ avg += terrain[x-half + divisions][y]; sum += 1.0; }

		// South corner
		if (y-half >= 0) 	{ avg += terrain[x][y-half]; sum += 1.0; }
		else 				{ avg += terrain[x][y-half+divisions]; sum += 1.0; }

		// East corner
		if (x + half <= divisions)	{ avg += terrain[x + half][y]; sum += 1.0; }
		else						{ avg += terrain[x + half - divisions][y]; sum += 1.0; }

		// North corner
		if (y + half <= divisions)	{ avg += terrain[x][y + half]; sum += 1.0; }
		else						{ avg += terrain[x][y + half - divisions]; sum +=1.0; }


		// this will make the edges the same 
		if (x == 0) 					{ 	terrain[x][y] = avg/sum + rnd () * scale;
											terrain[divisions][y] = terrain[x][y];}

		else if (y == 0) 				{ 	terrain[x][y] = avg/sum + rnd () * scale;
											terrain[x][divisions] = terrain[x][y];}

		else 							{	terrain[x][y] = avg/sum + rnd () * scale;}
	}


	private double rnd () {
		return uniform.nextDoubleFromTo(0, 2) - 1.0;
	}

}

package utilities;

import java.awt.*;

public class DrawHabitat extends Frame {
	
	public int gridWidth, gridHeight, scaleFactor; 
    public int orientation[][];
    public int orientationTemp[][];

    public int orientationScaled[][]; 

    
    // constructor, sets the gridWidth and the gridHeight to the main simulation
	public DrawHabitat (int xdim, int ydim, int scale, int[][] orient) {
		this.gridWidth = xdim; 
		this.gridHeight = ydim;
		this.scaleFactor = scale;
		this.orientation = orient; 
		orientationScaled = new int[gridWidth*scaleFactor][gridHeight*scaleFactor];
		orientationTemp = new int[gridWidth][gridHeight];
		
		for (int i = 0; i < gridWidth; i++) {
	    	for (int j = 0; j < gridHeight; j++) {
	    		orientationTemp[i][gridHeight-1-j] = orientation[i][j];
	    	}
		}

		for (int i = 0; i < gridWidth; i++) {
	    	for (int j = 0; j < gridHeight; j++) {
	    		orientationScaled[i*scaleFactor][j*scaleFactor] = orientationTemp[i][j];
	    	}
		}

		setSize(gridWidth*scaleFactor, gridHeight*scaleFactor);
		
	}
	
	
	public void paint(Graphics g) {

		for (int i = 0; i < gridWidth*scaleFactor; i+=scaleFactor) {
	    	for (int j = 0; j < gridHeight*scaleFactor; j+=scaleFactor) {

	    		
	    		////////////////////////////////////////////////////////////////
	    		// bare-ccaBad-ccaGood-macro-turf-other-coral == 1-2-3-4-5-6-7//
	    		////////////////////////////////////////////////////////////////

	    		
	    		if (orientationScaled[i][j] == 0) {
	    			g.setColor(Color.WHITE);
	    			g.fillRect(i, j, i+(scaleFactor-1), j+(scaleFactor-1)); 
	    		}
	    		else if (orientationScaled[i][j] == 1) {			// bare
	    			g.setColor(Color.GRAY);
	    			g.fillRect(i, j, i+(scaleFactor-1), j+(scaleFactor-1)); 
	    		}
	    		else if (orientationScaled[i][j] == 2) {			// ccaBad
	    			g.setColor(Color.BLACK);
	    			g.fillRect(i, j, i+(scaleFactor-1), j+(scaleFactor-1)); 
	    		}

	    	}
		}
	}
	
	public int getWrapAround(int x){

		int returnValue = x; 
		if (x >= 1000) returnValue = x-1000;
		if (x < 0) returnValue = 1000+x;
		return returnValue; 
	}


}

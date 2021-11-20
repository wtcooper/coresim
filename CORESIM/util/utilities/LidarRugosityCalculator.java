
/*
 * Important: as the lidar data is set up, will need to extract the last x and y rows for each file from the subsequent
 * tiles, and add it to the text file with the data prior to running this Calculator method on it.  Reason being -- the 
 * lidar tile data are in even numbers, but need to be odd to do the surface area calculation 
 * 
 * Overall Steps:
 * 1) Open .tif of a tile in ImageJ; select all, go to Analyze -> Tools -> Save x-y data as text file
 * 		- do this for all tiles, so have x-y text file for each
 * 2) Will need to build 2nd program that exports the appropriate extra x & y rows from a text file, then adds that to the appropriate files
 * 2) Open in Excel (hopefully it holds it...) -- sort to get last row of x's and last row of y's data; add to appropriate 
 * 		tiles that match; if no data, leave blank and program will fill in with -100
 * 3) Run program, get rugosity measure of each
 * 4) Make sure program is exporting appropriate x-y coordinates for each value as UTM
 * 
 * 
 * 
 */



package utilities;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

public class LidarRugosityCalculator {

	int x, y; 
	double height; 
	double area;
	// will store data from original file; represents 2x2km, at 1m resolution
	double[][] reefZoneTemp = new double[101][101]; 
	// will convert original data to a rugosity measurement; represents 2x2km at 2m resolution
	double[][] reefZone = new double[50][50];
	private PrintWriter outFile=null;	
	private File fFile;
	boolean check; 

	
	public void calculate() {

	    for (int i = 0; i < reefZone.length; i++) {
	    	for (int j = 0; j < reefZone.length; j++) {
	    		reefZone[i][j] = -100;
	    		}
	    	} 
	    
	    for (int i = 0; i < reefZoneTemp.length; i++) {
	    	for (int j = 0; j < reefZoneTemp.length; j++) {
	    		reefZoneTemp[i][j] = -100;
	    		}
	    	} 
	    
		getFile();
		
		double x1,x2,x3,y1,y2,y3,z1,z2,z3, dist1, dist2, dist3; 

		for (int i = 1; i < reefZoneTemp.length; i=i+2) {
	    	for (int j = 1; j < reefZoneTemp.length; j=j+2) {

	    		check = true; 
	    		for (int k = 0; k < 3; k++){
	    			for (int l = 0; l < 3; l++){
		    			if (reefZoneTemp[i-1+k][j-1+l] == -100) {check = false;}
		    			else {check = true;}
	    				
	    			}
	    		}
	    		
	    		if (check == true){
	    			area = 0;

	    			// for first triangle section
	    			x1=i-1; y1=j-1; z1 = reefZoneTemp[i-1][j-1];
	    			x2=i-1; y2=j; z2 = reefZoneTemp[i-1][j];
	    			x3=i; y3=j-1; z3 = reefZoneTemp[i][j-1]; 
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			area = area + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// for 2nd triangle section
	    			x1=i-1; y1=j; z1 = reefZoneTemp[i-1][j];
	    			x2=i; y2=j-1; z2 = reefZoneTemp[i][j-1];
	    			x3=i; y3=j; z3 = reefZoneTemp[i][j]; 
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			area = area + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// for 3rd triangle section
	    			x1=i-1; y1=j+1; z1 = reefZoneTemp[i-1][j+1];
	    			x2=i-1; y2=j; z2 = reefZoneTemp[i-1][j];
	    			x3=i; y3=j; z3 = reefZoneTemp[i][j]; 
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			area = area + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// for 4th triangle section
	    			x1=i-1; y1=j+1; z1 = reefZoneTemp[i-1][j+1];
	    			x2=i; y2=j+1; z2 = reefZoneTemp[i][j+1];
	    			x3=i; y3=j; z3 = reefZoneTemp[i][j]; 
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			area = area + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// for 5th triangle section
	    			x1=i+1; y1=j-1; z1 = reefZoneTemp[i+1][j-1];
	    			x2=i; y2=j-1; z2 = reefZoneTemp[i][j-1];
	    			x3=i; y3=j; z3 = reefZoneTemp[i][j]; 
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			area = area + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// for 6th triangle section
	    			x1=i+1; y1=j-1; z1 = reefZoneTemp[i+1][j-1];
	    			x2=i+1; y2=j; z2 = reefZoneTemp[i+1][j];
	    			x3=i; y3=j; z3 = reefZoneTemp[i][j]; 
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			area = area + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// for 7th triangle section
	    			x1=i; y1=j+1; z1 = reefZoneTemp[i][j+1];
	    			x2=i+1; y2=j; z2 = reefZoneTemp[i+1][j];
	    			x3=i; y3=j; z3 = reefZoneTemp[i][j]; 
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			area = area + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// for 8th triangle section
	    			x1=i; y1=j+1; z1 = reefZoneTemp[i][j+1];
	    			x2=i+1; y2=j; z2 = reefZoneTemp[i+1][j];
	    			x3=i+1; y3=j+1; z3 = reefZoneTemp[i+1][j+1]; 
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			area = area + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			reefZone[(i-1)/2][(j-1)/2] = area/4;
	    		}
	    	}
	    	System.out.println("Calculate Loop Number " + "\t" + i );
		} 

		writeOutput();
	}
	
	public void getFile() {
		
		fFile = new File("C:\\RepastS\\workspace\\Recruitment\\data\\SampleData0.txt");
		System.out.println("getFile method working");
		processLineByLine();

	}
	
	public final void processLineByLine(){


		
		try {
			//first use a Scanner to get each line
			Scanner scanner = new Scanner(fFile);
			while ( scanner.hasNextLine() ){
				processLine( scanner.nextLine() );
			}
			scanner.close();
			System.out.println("File Input to reefZoneTemp completed");
		}
		catch (IOException ex){
//			log(ex.getMessage());
		}
	}


	protected void processLine(String aLine){
		//use a second Scanner to parse the content of each line 
		Scanner scanner = new Scanner(aLine);
		scanner.useDelimiter("\t");
		if ( scanner.hasNext() ){
			x = scanner.nextInt();
			y = scanner.nextInt();
			height = scanner.nextDouble();

			reefZoneTemp[x][y] = height;
			System.out.println("File Input to reefZoneTemp working"+ "\t" + x+ "\t" + y + "\t" + height);
		}

		scanner.close();
	}
	
	
	public void writeOutput() {

		try {
        	outFile = new PrintWriter(new FileWriter("SampleData0Output.dat", true));
        } catch (IOException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        }

        for (int i = 0; i < reefZone.length; i++) {
        	for (int j = 0; j < reefZone.length; j++) {
        		outFile.println(i + "\t" + j + "\t" + reefZone[i][j]);
        		}
        	} 
        outFile.close();
	}	
	
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		LidarRugosityCalculator ldc = new LidarRugosityCalculator();
		ldc.calculate();
		
		
	}

}

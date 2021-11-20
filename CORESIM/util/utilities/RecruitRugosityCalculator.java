package utilities;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

public class RecruitRugosityCalculator {

	double[][] data = new double[101][101];
	private PrintWriter outFile=null;
	private File fFileInput;
	String filenameInput; 
	private File fFileOutput;
	String filenameOutput; 
	
	int ID, transectx, quadratx, point1x, point2x, point3x, point4x, point5x, point6x, point7x, point8x, point9x;
	int[] id = new int[249];	
	int[] transect = new int[249];	
	int[] quadrat = new int[249];	
	int[] point1 = new int[249];	
	int[] point2 = new int[249];	
	int[] point3 = new int[249];	
	int[] point4 = new int[249];	
	int[] point5 = new int[249];	
	int[] point6 = new int[249];	
	int[] point7 = new int[249];	
	int[] point8 = new int[249];	
	int[] point9 = new int[249];	
	float[] rugosity = new float[249];

	boolean check; 	
	double area;
	double areaTemp;
	double areaTempA;
	double areaTempB;
	

	public void calculate(){

	    for (int i = 0; i < rugosity.length; i++) {
	    		rugosity[i] = -999;
	    		
	    	} 
		
		
		
		double x1,x2,x3,y1,y2,y3,z1,z2,z3, dist1, dist2, dist3; 

		for (int i = 0; i < rugosity.length; i++) {

	    		check = true; 
		    	if (point1[i] == 999) {check = false;}
		    	if (point2[i] == 999) {check = false;}
		    	if (point3[i] == 999) {check = false;}
		    	if (point4[i] == 999) {check = false;}
		    	if (point5[i] == 999) {check = false;}
		    	if (point6[i] == 999) {check = false;}
		    	if (point7[i] == 999) {check = false;}
		    	if (point8[i] == 999) {check = false;}
		    	if (point9[i] == 999) {check = false;}
		    	
	    		if (check == true){
	    			area = 0;
	    			areaTempA = 0; areaTempB = 0;

	    			/*  To calculate rugosity, use a modified approach of Brock et al..  Here, for each 1x1m quadrat in the
	    			 * 2x2meter grid, calculate 2 triangles for each of two possible ways (so have 4 triangles per 1x1meter).  
	    			 * Then take the average of the two area measurements for each 1x1meter quadrat and use this as a more
	    			 * robust measurement, versus subjectively or randomly choosing which possible way to divide a square into 
	    			 * two triangles.  
	    			 * 
	    			 */
	    			
	    			// First Quadrat (lower left), triangle 1 of 1st possible split 
	    			x1=-1; y1=-1; z1 = point3[i];
	    			x2=-1; y2=0; z2 = point2[i];
	    			x3=0; y3=-1; z3 = point6[i]; 
	    			z1 = z1/100; z2 = z2/100; z3 = z3/100;
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			areaTempA = areaTempA + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// First Quadrat (lower left), triangle 2 of 1st possible split 
	    			x1=-1; y1=0; z1 = point2[i];
	    			x2=0; y2=-1; z2 = point6[i];
	    			x3=0; y3=0; z3 = point5[i]; 
	    			z1 = z1/100; z2 = z2/100; z3 = z3/100;
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			areaTempA = areaTempA + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// First Quadrat (lower left), triangle 1 of 2nd possible split 
	    			x1=-1; y1=-1; z1 = point3[i];
	    			x2=-1; y2=0; z2 = point2[i];
	    			x3=0; y3=0; z3 = point5[i]; 
	    			z1 = z1/100; z2 = z2/100; z3 = z3/100;
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			areaTempB = areaTempB + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// First Quadrat (lower left), triangle 2 of 2nd possible split 
	    			x1=-1; y1=-1; z1 = point3[i];
	    			x2=0; y2=-1; z2 = point6[i];
	    			x3=0; y3=0; z3 = point5[i]; 
	    			z1 = z1/100; z2 = z2/100; z3 = z3/100;
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			areaTempB = areaTempB + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			
	    			area = area + (areaTempA+areaTempB)/2; 
	    			
	    			areaTempA = 0; areaTempB = 0;
	    			

	    			
	    			// Second Quadrat (upper left), triangle 1 of 1st possible split 
	    			x2=-1; y2=1; z2 = point1[i];
	    			x3=0; y3=0; z3 = point5[i]; 
	    			x3=0; y3=1; z3 = point4[i]; 
	    			z1 = z1/100; z2 = z2/100; z3 = z3/100;
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			areaTempA = areaTempA + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// Second Quadrat (upper left), triangle 2 of 1st possible split 
	    			x1=-1; y1=0; z1 = point2[i];
	    			x2=-1; y2=1; z2 = point1[i];
	    			x3=0; y3=0; z3 = point5[i]; 
	    			z1 = z1/100; z2 = z2/100; z3 = z3/100;
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			areaTempA = areaTempA + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// Second Quadrat (upper left), triangle 1 of 2nd possible split 
	    			x2=-1; y2=1; z2 = point1[i];
	    			x3=0; y3=1; z3 = point4[i]; 
	    			x2=-1; y2=0; z2 = point2[i];
	    			z1 = z1/100; z2 = z2/100; z3 = z3/100;
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			areaTempB = areaTempB + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// Second Quadrat (upper left), triangle 2 of 2nd possible split 
	    			x3=0; y3=1; z3 = point4[i]; 
	    			x2=-1; y2=0; z2 = point2[i];
	    			x3=0; y3=0; z3 = point5[i]; 
	    			z1 = z1/100; z2 = z2/100; z3 = z3/100;
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			areaTempB = areaTempB + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			area = area + (areaTempA+areaTempB)/2; 
	    			
	    			areaTempA = 0; areaTempB = 0;
	    			
	    			
	    			// Third Quadrat (upper right), triangle 1 of 1st possible split 
	    			x2=1; y2=0; z2 = point8[i];
	    			x3=0; y3=0; z3 = point5[i]; 
	    			x3=0; y3=1; z3 = point4[i]; 
	    			z1 = z1/100; z2 = z2/100; z3 = z3/100;
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			areaTempA = areaTempA + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// Third Quadrat (upper right), triangle 2 of 1st possible split 
	    			x2=1; y2=0; z2 = point8[i];
	    			x3=1; y3=1; z3 = point7[i]; 
	    			x3=0; y3=1; z3 = point4[i]; 
	    			z1 = z1/100; z2 = z2/100; z3 = z3/100;
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			areaTempA = areaTempA + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// Third Quadrat (upper right), triangle 1 of 2nd possible split 
	    			x3=0; y3=0; z3 = point5[i]; 
	    			x3=0; y3=1; z3 = point4[i]; 
	    			x3=1; y3=1; z3 = point7[i]; 
	    			z1 = z1/100; z2 = z2/100; z3 = z3/100;
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			areaTempB = areaTempB + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// Third Quadrat (upper right), triangle 2 of 2nd possible split 
	    			x2=1; y2=0; z2 = point8[i];
	    			x3=0; y3=0; z3 = point5[i]; 
	    			x3=1; y3=1; z3 = point7[i]; 
	    			z1 = z1/100; z2 = z2/100; z3 = z3/100;
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			areaTempB = areaTempB + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			area = area + (areaTempA+areaTempB)/2; 
	    			
	    			areaTempA = 0; areaTempB = 0;
	    			
	    			
	    			// Forth Quadrat (lower right), triangle 1 of 1st possible split 
	    			x2=1; y2=0; z2 = point8[i];
	    			x3=0; y3=0; z3 = point5[i]; 
	    			x2=0; y2=-1; z2 = point6[i];
	    			z1 = z1/100; z2 = z2/100; z3 = z3/100;
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			areaTempA = areaTempA + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// Forth Quadrat (lower right), triangle 2 of 1st possible split 
	    			x2=1; y2=0; z2 = point8[i];
	    			x3=1; y3=-1; z3 = point9[i]; 
	    			x2=0; y2=-1; z2 = point6[i];
	    			z1 = z1/100; z2 = z2/100; z3 = z3/100;
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			areaTempA = areaTempA + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// Forth Quadrat (lower right), triangle 1 of 2nd possible split 
	    			x3=0; y3=0; z3 = point5[i]; 
	    			x2=0; y2=-1; z2 = point6[i];
	    			x3=1; y3=-1; z3 = point9[i]; 
	    			z1 = z1/100; z2 = z2/100; z3 = z3/100;
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			areaTempB = areaTempB + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// Forth Quadrat (lower right), triangle 2 of 2nd possible split 
	    			x2=1; y2=0; z2 = point8[i];
	    			x3=0; y3=0; z3 = point5[i]; 
	    			x3=1; y3=-1; z3 = point9[i]; 
	    			z1 = z1/100; z2 = z2/100; z3 = z3/100;
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			areaTempB = areaTempB + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			area = area + (areaTempA+areaTempB)/2; 
	    			
	    			areaTempA = 0; areaTempB = 0;

	    			
	    			
	    			rugosity[i] = (float) area/4;
	    		} // end of if (check == true)
	    	} // end of for loop going through 249 entries

			// Name the sample data files 
			filenameOutput = "RecruitRugosityOut.txt"; 
			fFileOutput = new File(filenameOutput); 
			writeOutput(); 
			
	} // end of calculate method
	

public void readFile(){
	String aFileName = "RecruitRugosity.dat";
	fFileInput = new File(aFileName);  

	processLineByLine();
	

}
    
public void processLineByLine(){
	
	try {
		//first use a Scanner to get each line
		Scanner scanner = new Scanner(fFileInput);
		while ( scanner.hasNextLine() ){
			processLine( scanner.nextLine() );
			
		}
		scanner.close();
	}
	catch (IOException ex){
          System.out.println(" fail = "+ex);
	}
}



public void processLine(String aLine){
	//use a second Scanner to parse the content of each line 
	Scanner scanner = new Scanner(aLine);
	scanner.useDelimiter("\t");
	if ( scanner.hasNext() ){
		ID = scanner.nextInt();
		transectx = scanner.nextInt();
		quadratx = scanner.nextInt();
		point1x = scanner.nextInt();
		point2x = scanner.nextInt();
		point3x = scanner.nextInt();
		point4x = scanner.nextInt();
		point5x = scanner.nextInt();
		point6x = scanner.nextInt();
		point7x = scanner.nextInt();
		point8x = scanner.nextInt();
		point9x = scanner.nextInt();

		id[ID-1] = ID;
		transect[ID-1] = transectx;
		quadrat[ID-1] = quadratx;
		point1[ID-1] = point1x;
		point2[ID-1] = point2x;
		point3[ID-1] = point3x;
		point4[ID-1] = point4x;
		point5[ID-1] = point5x;
		point6[ID-1] = point6x;
		point7[ID-1] = point7x;
		point8[ID-1] = point8x;
		point9[ID-1] = point9x;
	}

	scanner.close();
} // end of processLine method



	public void writeOutput() {


		try {
        	outFile = new PrintWriter(new FileWriter(fFileOutput, true));
        } catch (IOException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        }

        for (int i = 0; i < rugosity.length; i++) {
        		outFile.println(i+1 + "\t" + rugosity[i]);
        		}
        outFile.close();
	}

	
	public static void main(String[] args) {

		RecruitRugosityCalculator rrc = new RecruitRugosityCalculator();
		rrc.readFile();
		rrc.calculate();

		
	}

}

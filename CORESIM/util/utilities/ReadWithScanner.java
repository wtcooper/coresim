// code adapted from http://www.javapractices.com/topic/TopicAction.do?Id=42; see for original

package utilities;

import java.io.*;
import java.util.Scanner;

public class ReadWithScanner {
	
	int x, y; 
	double height; 
	double[][] reefZoneTemp = new double[2000][2000]; 
	double[][] reefZone = new double[1000][1000];
	private PrintWriter outFile=null;	

	// PRIVATE //
	private final File fFile;

	
	public ReadWithScanner(String aFileName){
		fFile = new File(aFileName);  
	}
	

	public double[][] ReadFile() {
		
		processLineByLine();
//		log("Done.");
		
		return reefZone;
	}



	/** Template method that calls {@link #processLine(String)}.  */
	public final void processLineByLine(){

	    for (int i = 0; i < reefZone.length; i++) {
	    	for (int j = 0; j < reefZone.length; j++) {
	    		reefZone[i][j] = -100;
	    		}
	    	} 
		
		try {
			//first use a Scanner to get each line
			Scanner scanner = new Scanner(fFile);
			while ( scanner.hasNextLine() ){
				processLine( scanner.nextLine() );
			}
			scanner.close();
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

			reefZone[x][y] = height;

		}

		scanner.close();
	}

	
	public void writeOutput() {

		try {
        	outFile = new PrintWriter(new FileWriter("bisc_e582n2810edit1_rugosity.dat", true));
        } catch (IOException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        }

        for (int i = 0; i < 1000; i++) {
        	for (int j = 0; j < 1000; j++) {
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


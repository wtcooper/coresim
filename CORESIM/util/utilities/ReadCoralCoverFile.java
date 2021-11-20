// code adapted from http://www.javapractices.com/topic/TopicAction.do?Id=42; see for original

package utilities;

import java.io.*;
import java.util.Scanner;

public class ReadCoralCoverFile {
	
	// format of txt file: ID	BankArea	PatchArea	CoralCov	MacroCov	BareCov	CCACov	ReefArea	lat	lon
	int ID, lat, lon, coralCov, macroCov, bareCov, ccaCov; 
	int sup1,sup2,sup3,sup4,sup5,sup6,sup7,sup8,sup9,sup10,sup11,sup12,sup13,sup14,sup15,totSupply; 

	float bankA, patchA, reefA; 
	float[] bankArea = new float[10000]; 
	float[] patchArea = new float[10000]; 
	float[] reefArea = new float[10000]; 
	int[] latitude = new int[10000];
	int[] longitude = new int[10000];
	int[] coralCover = new int[10000];
	int[] macroCover = new int[10000];
	int[] bareCover = new int[10000];
	int[] ccaCover = new int[10000];
	int[] id = new int[10000];
	int[] totalSupply = new int[10000]; 
	int[][] larvaeSupply = new int[10000][15];


	String request; 
	private PrintWriter outFile=null;	

	// PRIVATE //
	File fFile;

	
	public void ReadFile(){
		String aFileName = "ReefCover.txt";
		fFile = new File(aFileName);  

		processLineByLine();
		
	    for (int i = 0; i < 20; i++) {
		System.out.println("ID:" + id[i] + "\t" + "lat & lon:"+ "\t" + latitude[i] + "\t" + longitude[i] + 
				"\t" + "Total Supply"+ "\t" + totalSupply[i]);
	    }
	}
	    
	public final void processLineByLine(){
		
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
			ID = scanner.nextInt();
			bankA = scanner.nextFloat();
			patchA = scanner.nextFloat();
			coralCov = scanner.nextInt();
			macroCov = scanner.nextInt();
			bareCov = scanner.nextInt();
			ccaCov = scanner.nextInt();
			reefA = scanner.nextFloat();
			lon = scanner.nextInt();
			lat = scanner.nextInt();
			sup1 = scanner.nextInt();
			sup2 = scanner.nextInt();
			sup3 = scanner.nextInt();
			sup4 = scanner.nextInt();
			sup5 = scanner.nextInt();
			sup6 = scanner.nextInt();
			sup7 = scanner.nextInt();
			sup8 = scanner.nextInt();
			sup9 = scanner.nextInt();
			sup10 = scanner.nextInt();
			sup11 = scanner.nextInt();
			sup12 = scanner.nextInt();
			sup13 = scanner.nextInt();
			sup14 = scanner.nextInt();
			sup15 = scanner.nextInt();
			totSupply = scanner.nextInt();

			id[ID-1] = ID;
			bankArea[ID-1] = bankA;
			patchArea[ID-1] = patchA;
			reefArea[ID-1] = reefA;
			latitude[ID-1] = lat;
			longitude[ID-1] = lon;
			coralCover[ID-1] = coralCov;
			macroCover[ID-1] = macroCov;
			bareCover[ID-1] = bareCov;
			ccaCover[ID-1] = ccaCov;

			larvaeSupply[ID-1][0] = sup1;
			larvaeSupply[ID-1][1] = sup2;
			larvaeSupply[ID-1][2] = sup3;
			larvaeSupply[ID-1][3] = sup4;
			larvaeSupply[ID-1][4] = sup5;
			larvaeSupply[ID-1][5] = sup6;
			larvaeSupply[ID-1][6] = sup7;
			larvaeSupply[ID-1][7] = sup8;
			larvaeSupply[ID-1][8] = sup9;
			larvaeSupply[ID-1][9] = sup10;
			larvaeSupply[ID-1][10] = sup11;
			larvaeSupply[ID-1][11] = sup12;
			larvaeSupply[ID-1][12] = sup13;
			larvaeSupply[ID-1][13] = sup14;
			larvaeSupply[ID-1][14] = sup15;
			
			totalSupply[ID-1] = totSupply; 

		}

		scanner.close();
	}

	

	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		ReadCoralCoverFile rcc = new ReadCoralCoverFile(); 
		rcc.ReadFile();
		
		
	}	


}


package utilities;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class SampleDataMaker {

	double[][] data = new double[101][101];
	private PrintWriter outFile=null;
	private File fFile;
	String filename; 
	
	
	public void makeData(){

		// loop 10 times to make 10 sample data files
		for (int w = 0; w < 10; w++) {

			for (int i = 0; i < data.length; i++) {
				for (int j = 0; j < data.length; j++) {
					data[i][j] = Math.random()*-8; 
				}
			} 
		
			// Name the sample data files 
			filename = "SampleData" + w + ".txt"; 
			fFile = new File(filename); 
			writeOutput(w); 
		}
	}
	
	public void writeOutput(int w) {


		try {
        	outFile = new PrintWriter(new FileWriter(fFile, true));
        } catch (IOException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        }

        for (int i = 0; i < data.length; i++) {
        	for (int j = 0; j < data.length; j++) {
        		outFile.println(i + "\t" + j + "\t" + data[i][j]);
    			System.out.println("Sample Data" + w + "\t" + i + "\t" + j + "\t" + data[i][j]);
        		}
        	} 
        outFile.close();
	}

	
	public static void main(String[] args) {

		SampleDataMaker sd = new SampleDataMaker();
		sd.makeData();

		
	}

}

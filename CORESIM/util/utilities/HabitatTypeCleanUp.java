
/*
 * This program cleans up the habitatType.nc file after it is exported from ArcGIS -- need to do this because Arc
 * exports some of the files as bytes and some as shorts, and some with missing values of -128 and some with missing
 * values of 0, depending on the circumstances.  Also, the last row of longitudes (588e)
 * were exported as 2000x1549, versus 2000x2000, so need to account for that
 * 
 * 
 */



package utilities;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import ucar.ma2.ArrayFloat;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayByte;
import ucar.ma2.ArrayShort;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileCache;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;


import java.util.ArrayList;
import java.io.IOException;

public class HabitatTypeCleanUp {

	int x, y; 
	double height; 
	double area;

	// lat is y, lon is x; dimensions of variable are lat,lon or y,x
   	public int NLAT = 2000;
   	// is 1549 for the 588 longitude's
   	public int NLON = 2000;
   	public int latStart = 2818;
   	public int lonStart = 588;

   	
	// will convert original data to a rugosity measurement; represents 2x2km at 2m resolution
	float[][] habitat = new float[2000][2000];
	int[][] habitatTemp = new int[NLAT][NLON];

	private PrintWriter outFile=null;	
	private File fFile;
	boolean check; 
	

   	
   	
    // only need to add +2 to the depth array, because this is what the rugosity calculator will be based on
    // don't need to add to lats and lons because we just want 2000x2000 for rugosity array
    int [] latsIn = new int[NLAT];
    int [] lonsIn = new int[NLON];

	
	public void calculate() {

		
		// ** for 588e, will set the missing values to -100
	    for (int i = 0; i < 2000; i++) {
	    	for (int j = 0; j < 2000; j++) {
	    		habitat[i][j] = -100;
	    		}
	    	} 
		
		getFile();
		
		
		// clean up for only 0's and 0's and 1's; here, 0 = patch, 1 = bank
		/*		for (int i = 0; i < NLAT; i++) {
	    	for (int j = 0; j < NLON; j++) {
	    		if (habitatTemp[i][j] == -128) {
	    			habitat[i][j] = -100; 
	    		}
	    		else {
	    			habitat[i][j] = (float) habitatTemp[i][j];
	    		}
	    	}
	    }
*/
		
		// clean up for only 1's 
		for (int i = 0; i < NLAT; i++) {
	    	for (int j = 0; j < NLON; j++) {
	    		if (habitatTemp[i][j] == 0) {
	    			habitat[i][j] = -100; 
	    		}
	    		else {
	    			habitat[i][j] = (float) habitatTemp[i][j];
	    		}
	    		
	    	}
	    }
		
		
		//	    	System.out.println("Calculate Loop Number " + "\t" + i );


		writeOutput();
	}
	
	
	
	
	public void getFile() {
		
	    	
	        // Open the file and check to make sure it's valid.
	        String filename = "Habitat"+lonStart+"_"+latStart+".nc";
	        NetcdfFile dataFile = null;

	        try {

	            dataFile = NetcdfFile.open(filename, null);
//                System.out.println("Success opening main file");
                
	            Variable latVar = dataFile.findVariable("y");
	            if (latVar == null) {
	                System.out.println("Cant find Variable y");
	                return;
	            }

	            Variable lonVar= dataFile.findVariable("x");
	            if (lonVar == null) {
	                System.out.println("Cant find Variable x");
	                return;
	            }

	            Variable habitatVar= dataFile.findVariable("HabitatType");
	            if (habitatVar == null) {
	                System.out.println("Cant find Variable depth");
	                return;
	            }


	            
	            if(latVar.getDimensions().size() != 1) {
	              System.out.println(" fail to get the dimensions of variable y");
	                return;
	            }
	            if(habitatVar.getDimensions().size() != 2) {
	               System.out.println(" fail to get the dimensions of variable depth");
	                return;
	            }

	            
	            // Read the latitude and longitude coordinate variables into arrays
	            // latsIn and lonsIn.

	            ArrayDouble.D1 latArray;
	            ArrayDouble.D1 lonArray;

	            latArray = (ArrayDouble.D1)latVar.read();
	            lonArray = (ArrayDouble.D1)lonVar.read();

	            // sets the latitude to the latitude array for later export; 
	            int[] shape = latArray.getShape();
	            for (int i=0; i<shape[0]; i++) {
	                latsIn[i] = (int) latArray.get(i);
	            }

	            shape = lonArray.getShape();
	            for (int j=0; j<shape[0]; j++) {
	                lonsIn[j] = (int) lonArray.get(j);
	            }



	            // Read the data. Since we know the contents of the file we know
	            // that the data arrays in this program are the correct size to
	            // hold all the data.
	            
	            // need to change to ArrayByte for the 1's only
	            //ArrayShort.D2 habitatArray;
	            ArrayByte.D2 habitatArray;
	            
	            //habitatArray = (ArrayShort.D2)habitatVar.read();
	            habitatArray = (ArrayByte.D2)habitatVar.read();
	            
	            int [] shape1 = habitatArray.getShape();

	            for (int i=0; i<shape1[0]; i++) {
	              for (int j=0; j<shape1[1]; j++) {
	                	  habitatTemp[i][j] = (int) habitatArray.get(i,j);
	                  }
	              }


	        } catch (java.io.IOException e) {
	              System.out.println(" fail = "+e);
	              e.printStackTrace();
	        } finally {
	           if (dataFile != null)
	           try {
	             dataFile.close();
	           } catch (IOException ioe) {
	             ioe.printStackTrace();
	           }
	        }
//	        System.out.println("*** SUCCESS reading example file sfc_pres_temp.nc!");
		
	}
	
	public void writeOutput() {

		
   
	    
        // Create the file.
        String filename = "HabitatType"+lonStart+"_"+latStart+".nc";
        NetcdfFileWriteable dataFile = null;

        try {
            //Create new netcdf-3 file with the given filename
            dataFile = NetcdfFileWriteable.createNew(filename, false);

            // In addition to the latitude and longitude dimensions, we will
            // also create latitude and longitude netCDF variables which will
            // hold the actual latitudes and longitudes. Since they hold data
            // about the coordinate system, the netCDF term for these is:
            // "coordinate variables."
            Dimension latDim = dataFile.addDimension("latitude", 2000);
            Dimension lonDim = dataFile.addDimension("longitude", 2000);
            ArrayList dims =  null;


            dataFile.addVariable("latitude", DataType.FLOAT, new Dimension[] {latDim});
            dataFile.addVariable("longitude", DataType.FLOAT, new Dimension[] {lonDim});

            // Define units attributes for coordinate vars. This attaches a
            // text attribute to each of the coordinate variables, containing
            // the units.

            dataFile.addVariableAttribute("longitude", "units", "UTM_east");
            dataFile.addVariableAttribute("latitude", "units", "UTM_north");

            // Define the netCDF data variables.
            dims =  new ArrayList();
            dims.add(latDim);
            dims.add(lonDim);
            dataFile.addVariable("habitatType", DataType.FLOAT, dims);

            // Define units attributes for variables.
            dataFile.addVariableAttribute("habitatType", "units", "0=patch,1=bank");

            // Write the coordinate variable data. This will put the latitudes
            // and longitudes of our data grid into the netCDF file.
            dataFile.create();


            ArrayFloat.D1 dataLat = new ArrayFloat.D1(latDim.getLength());
            ArrayFloat.D1 dataLon = new ArrayFloat.D1(lonDim.getLength());

            int i,j;
            for (i=0; i<latDim.getLength(); i++) {
                dataLat.set(i, (float) latsIn[i]);
            }

            for (j=0; j<lonDim.getLength(); j++) {
               dataLon.set(j, (float) lonsIn[j]);
            }


            dataFile.write("latitude", dataLat);
            dataFile.write("longitude", dataLon);

            ArrayFloat.D2 dataHabitat = new ArrayFloat.D2(latDim.getLength(), lonDim.getLength());
            
            for (i=0; i<latDim.getLength(); i++) {
                for (j=0; j<lonDim.getLength(); j++) {
                   dataHabitat.set(i,j, habitat[i][j]);
                }
            }

            int[] origin = new int[2];

            dataFile.write("habitatType", origin, dataHabitat);


        } catch (IOException e) {
              e.printStackTrace();
        } catch (InvalidRangeException e) {
              e.printStackTrace();
        } finally {
            if (null != dataFile)
            try {
                dataFile.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
       }
//       System.out.println( "*** SUCCESS writing example file sfc_pres_temp.nc!" );

	}	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		HabitatTypeCleanUp ldc = new HabitatTypeCleanUp();
		ldc.calculate();
		
		
	}

}

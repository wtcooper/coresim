
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
import java.util.ArrayList;
import java.util.Scanner;

import ucar.ma2.ArrayFloat;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.ArrayInt;
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

public class LidarRugosityCalculator2 {

	int x, y; 
	double height; 
	double area;

	// will convert original data to a rugosity measurement; represents 2x2km at 2m resolution
	float[][] rugosity = new float[2000][2000];
	private PrintWriter outFile=null;	
	private File fFile;
	boolean check; 
	
   	public int NLAT = 2000;
   	public int NLON = 2000;
   	public int latStart = 2808;
   	public int lonStart = 584;

    // only need to add +2 to the depth array, because this is what the rugosity calculator will be based on
    // don't need to add to lats and lons because we just want 2000x2000 for rugosity array
    float [][] depth = new float[NLAT+2][NLON+2];
    int [] latsIn = new int[NLAT];
    int [] lonsIn = new int[NLON];

	
	public void calculate() {

	    for (int i = 0; i < rugosity.length; i++) {
	    	for (int j = 0; j < rugosity.length; j++) {
	    		rugosity[i][j] = -100;
	    		}
	    	} 
	    
		
		
		getFile();
		
		
		double x1,x2,x3,y1,y2,y3,z1,z2,z3, dist1, dist2, dist3; 

		for (int i = 1; i < depth.length-1; i++) {
	    	for (int j = 1; j < depth.length-1; j++) {

	    		check = true; 
	    		for (int k = 0; k < 3; k++){
	    			for (int l = 0; l < 3; l++){
		    			if (depth[i-1+k][j-1+l] == -100) {check = false;}
	    			}
	    		}
	    		
	    		if (check == true){
	    			area = 0;

	    			// for first triangle section
	    			x1=i-1; y1=j-1; z1 = depth[i-1][j-1];
	    			x2=i-1; y2=j; z2 = depth[i-1][j];
	    			x3=i; y3=j-1; z3 = depth[i][j-1]; 
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			area = area + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// for 2nd triangle section
	    			x1=i-1; y1=j; z1 = depth[i-1][j];
	    			x2=i; y2=j-1; z2 = depth[i][j-1];
	    			x3=i; y3=j; z3 = depth[i][j]; 
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			area = area + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// for 3rd triangle section
	    			x1=i-1; y1=j+1; z1 = depth[i-1][j+1];
	    			x2=i-1; y2=j; z2 = depth[i-1][j];
	    			x3=i; y3=j; z3 = depth[i][j]; 
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			area = area + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// for 4th triangle section
	    			x1=i-1; y1=j+1; z1 = depth[i-1][j+1];
	    			x2=i; y2=j+1; z2 = depth[i][j+1];
	    			x3=i; y3=j; z3 = depth[i][j]; 
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			area = area + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// for 5th triangle section
	    			x1=i+1; y1=j-1; z1 = depth[i+1][j-1];
	    			x2=i; y2=j-1; z2 = depth[i][j-1];
	    			x3=i; y3=j; z3 = depth[i][j]; 
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			area = area + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// for 6th triangle section
	    			x1=i+1; y1=j-1; z1 = depth[i+1][j-1];
	    			x2=i+1; y2=j; z2 = depth[i+1][j];
	    			x3=i; y3=j; z3 = depth[i][j]; 
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			area = area + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// for 7th triangle section
	    			x1=i; y1=j+1; z1 = depth[i][j+1];
	    			x2=i+1; y2=j; z2 = depth[i+1][j];
	    			x3=i; y3=j; z3 = depth[i][j]; 
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			area = area + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// for 8th triangle section
	    			x1=i; y1=j+1; z1 = depth[i][j+1];
	    			x2=i+1; y2=j; z2 = depth[i+1][j];
	    			x3=i+1; y3=j+1; z3 = depth[i+1][j+1]; 
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			area = area + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;
	    		
	    			
	    			rugosity[i-1][j-1] = (float) area/4;
	    		}
	    	}
//	    	System.out.println("Calculate Loop Number " + "\t" + i );
		} 

		writeOutput();
	}
	
	public void getFile() {
		
	        
	        boolean isNorthNeighbor, isEastNeighbor, isSouthNeighbor, isWestNeighbor; 
	        int [][] neighbors = 	{{1,1,1,0,0}, //2802; x's are 580, 582, 584, 586, 588  * this will give neighbors[lat][lon], or [9][5]
	        						{1,1,1,0,0},  //2804
	        						{1,1,1,1,0},  //2806
	        						{0,1,1,1,0},  //2808
	        						{0,1,1,1,0},  //2810
	        						{0,0,1,1,0},  //2812
	        						{0,0,1,1,1},  //2814
	        						{0,0,1,1,1},  //2816
	        						{0,0,0,0,1}}; //2818
	        int [] lonCoords = {580, 582, 584, 586, 588};
	        int [] latCoords = {2802, 2804, 2806, 2808, 2810, 2812, 2814, 2816, 2818}; 
	        


	        
	        // procedure to check neighborhood to see which tiles have neighboring tiles
	        int latIndex = 0; int lonIndex = 0; 

	        for (int i=0; i < lonCoords.length; i++){
	        	if (lonCoords[i] == lonStart) { lonIndex = i;}
	        }

	        for (int i=0; i < latCoords.length; i++){
	        	if (latCoords[i] == latStart) { latIndex = i;}
	        }
	
	        isNorthNeighbor = true; isEastNeighbor = true; isSouthNeighbor = true; isWestNeighbor = true; 
	        
	        if (lonIndex == 0 || neighbors[latIndex][lonIndex-1] == 0) {isWestNeighbor = false;}
	        if (lonIndex == (lonCoords.length - 1) || neighbors[latIndex][lonIndex+1] == 0) {isEastNeighbor = false;}
	        if (latIndex == 0 || neighbors[latIndex-1][lonIndex] == 0) {isSouthNeighbor = false;}
	        if (latIndex == (latCoords.length - 1) || neighbors[latIndex+1][lonIndex] == 0) {isNorthNeighbor = false;}
	        

	        // Initialize depth array to missing value of -100
	        for (int i=0; i < depth.length; i++){
	        	for (int j=0; j < depth[i].length; j++){
	        		depth[i][j] = -100; 
	        	}
	        }
	        
	    	
	        // Open the file and check to make sure it's valid.
	        String filename = "Lidar"+lonStart+"_"+latStart+".nc";
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

	            Variable depthVar= dataFile.findVariable("depth");
	            if (depthVar == null) {
	                System.out.println("Cant find Variable depth");
	                return;
	            }


	            
	            if(latVar.getDimensions().size() != 1) {
	              System.out.println(" fail to get the dimensions of variable y");
	                return;
	            }
	            if(depthVar.getDimensions().size() != 2) {
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
	            ArrayFloat.D2 depthArray;

	            depthArray = (ArrayFloat.D2)depthVar.read();

	            int [] shape1 = depthArray.getShape();

	            for (int i=0; i<shape1[0]; i++) {
	              for (int j=0; j<shape1[1]; j++) {
	                	  depth[i+1][j+1] = depthArray.get(i,j);
	                  }
	              }


	            
	            // Get the adjoining rows to this tile from the neighboring tiles
	            if (isNorthNeighbor == true) {
	            	try {
	            		String northFilename = "Lidar"+lonStart+"_"+latCoords[latIndex+1]+".nc";
	            		NetcdfFile northDataFile = null;
	            		northDataFile = NetcdfFile.open(northFilename, null);
	            		Variable ndepthVar = northDataFile.findVariable("depth");

	            		Array ndepthArray;
	            		ndepthArray = (Array)ndepthVar.read("0:0:1,0:1999:1");
	            		Array nreduce = ndepthArray.reduce();
	            		  Index index = nreduce.getIndex();
	            		for (int i=0; i<shape1[0]; i++) {
	            			depth[2001][i+1] = nreduce.getFloat(index.set(i));
	            		}
	            	} catch (InvalidRangeException e) {
	            		e.printStackTrace();
	            	} catch (IOException ioe) {
	            		ioe.printStackTrace();
	            	}
	            }

	            if (isEastNeighbor == true) {
	            	try {
	        	        String eastFilename = "Lidar"+lonCoords[lonIndex+1]+"_"+latStart+".nc";
	        	        NetcdfFile eastDataFile = null;
	                	eastDataFile = NetcdfFile.open(eastFilename, null);
	                	Variable edepthVar = eastDataFile.findVariable("depth");

		            	Array edepthArray;
		            	edepthArray = (Array)edepthVar.read("0:1999:1,0:0:1");
		            	Array ereduce = edepthArray.reduce();
		            	Index index = ereduce.getIndex();
		            	for (int i=0; i<shape1[0]; i++) {
		            		depth[i+1][2001] = ereduce.getFloat(index.set(i));
		            	}
		            	} catch (InvalidRangeException e) {
			   	             e.printStackTrace();
			 	        } catch (IOException ioe) {
		   	             ioe.printStackTrace();
		 	           }	            
	            }

	            if (isSouthNeighbor == true) {
	            	try {
	        	        String southFilename = "Lidar"+lonStart+"_"+latCoords[latIndex-1]+".nc";
	        	        NetcdfFile southDataFile = null;
	                	southDataFile = NetcdfFile.open(southFilename, null);
	                	Variable sdepthVar = southDataFile.findVariable("depth");

		            	Array sdepthArray;
		            	sdepthArray = (Array)sdepthVar.read("1999:1999:1,0:1999:1");
		            	Array sreduce = sdepthArray.reduce();
		            	Index index = sreduce.getIndex();
		            	for (int i=0; i<shape1[0]; i++) {
		            		depth[0][i+1] = sreduce.getFloat(index.set(i));
		            	}
		            	} catch (InvalidRangeException e) {
			   	             e.printStackTrace();
			 	        } catch (IOException ioe) {
		   	             ioe.printStackTrace();
		 	           }	            

	            }

	            if (isWestNeighbor == true) {
	            	try {
	        	        String westFilename = "Lidar"+lonCoords[lonIndex-1]+"_"+latStart+".nc";
	        	        NetcdfFile westDataFile = null;
	                	westDataFile = NetcdfFile.open(westFilename, null);
	                	Variable wdepthVar = westDataFile.findVariable("depth");

		            	Array wdepthArray;
		            	wdepthArray = (Array)wdepthVar.read("0:1999:1,1999:1999:1");
		            	Array wreduce = wdepthArray.reduce();
		            	Index index = wreduce.getIndex();
		            	for (int i=0; i<shape1[0]; i++) {
		            		depth[i+1][0] = wreduce.getFloat(index.set(i));
		            	}
		            	} catch (InvalidRangeException e) {
			   	             e.printStackTrace();
			 	        } catch (IOException ioe) {
		   	             ioe.printStackTrace();
		 	           }	            
	            }

	            
/*	            for (int i=0; i<shape1[0]; i++) {
	                for (int j=0; j<shape1[1]; j++) {
	                	System.out.println("Lat: " + latsIn[i] + "\t\t" + "Lon: " + lonsIn[j] + "\t\t" + "Depth: " + depth[i][j]);
	                }
	            }
*/

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

		
		//set all no-data points to 0
	    for (int i = 0; i < rugosity.length; i++) {
	    	for (int j = 0; j < rugosity.length; j++) {
	    		if( rugosity[i][j] == -100) {
	    			rugosity[i][j] = 1;
	    		}
	    	}
	    } 
	    
	    
        // Create the file.
        String filename = "LidarRugosity"+lonStart+"_"+latStart+"_NoData0.nc";
        NetcdfFileWriteable dataFile = null;

        try {
            //Create new netcdf-3 file with the given filename
            dataFile = NetcdfFileWriteable.createNew(filename, false);

            // In addition to the latitude and longitude dimensions, we will
            // also create latitude and longitude netCDF variables which will
            // hold the actual latitudes and longitudes. Since they hold data
            // about the coordinate system, the netCDF term for these is:
            // "coordinate variables."
            Dimension latDim = dataFile.addDimension("latitude", NLAT);
            Dimension lonDim = dataFile.addDimension("longitude", NLON);
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
            dataFile.addVariable("rugosity", DataType.FLOAT, dims);

            // Define units attributes for variables.
            dataFile.addVariableAttribute("rugosity", "units", "m");

            // Write the coordinate variable data. This will put the latitudes
            // and longitudes of our data grid into the netCDF file.
            dataFile.create();


            ArrayFloat.D1 dataLat = new ArrayFloat.D1(latDim.getLength());
            ArrayFloat.D1 dataLon = new ArrayFloat.D1(lonDim.getLength());

            // Create some pretend data. If this wasn't an example program, we
            // would have some real data to write, for example, model
            // output.
            int i,j;


            for (i=0; i<latDim.getLength(); i++) {
                dataLat.set(i, (float) latsIn[i]);
            }

            for (j=0; j<lonDim.getLength(); j++) {
               dataLon.set(j, (float) lonsIn[j]);
            }


            dataFile.write("latitude", dataLat);
            dataFile.write("longitude", dataLon);

            // Create the pretend data. This will write our surface pressure and
            // surface temperature data.

            ArrayFloat.D2 dataRugosity = new ArrayFloat.D2(latDim.getLength(), lonDim.getLength());

            
            for (i=0; i<latDim.getLength(); i++) {
                for (j=0; j<lonDim.getLength(); j++) {
                   dataRugosity.set(i,j, rugosity[i][j]);
                }
            }

            int[] origin = new int[2];

            dataFile.write("rugosity", origin, dataRugosity);


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

		LidarRugosityCalculator2 ldc = new LidarRugosityCalculator2();
		ldc.calculate();
		
		
	}

}

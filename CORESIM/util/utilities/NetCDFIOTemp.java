package utilities;

import java.io.IOException;
import java.util.ArrayList;

import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

public class NetCDFIOTemp {

	String fileType; 
	float[][]data = new float[100][100];
	static float[][]rugosity = new float[100][100];
	
	int lonStart, latStart, xdim, ydim;
	int ymax, xmax;

	int [] latsIn = new int[2000];
	int [] lonsIn = new int[2000];


	/*	METHOD getFile
	 *  pass in the fileType (LidarRugosity or HabitatType), the starting lon and lat of 2000x2000
	 *  and the xdim and ydim that needs slice out -- will always be a 100x100 piece, so just need the 
	 *  starting x and y slice spot
	 */

	public float[][] getFile(String fileType, int lonStart, int latStart, int xdim, int ydim) {
		
		this.lonStart = lonStart;
		this.latStart = latStart;
		this.xdim = xdim;
		this.ydim = ydim;
		this.ymax = ydim+99;
		this.xmax = xdim+99;
		
		if (fileType == "rugosity") {
			getRugosity();
		}
		else {
			getHabitatType();
		}

		return data; 
	}
	
/*	public void getFile(String fileType, int lonStart, int latStart, int xdim, int ydim) {

		this.lonStart = lonStart;
		this.latStart = latStart;
		this.xdim = xdim;
		this.ydim = ydim;
		this.ymax = ydim+99;
		this.xmax = xdim+99;


		if (fileType == "rugosity") {
			getRugosity();
		}
		else {
			getHabitatType();
		}

		for (int i=0; i<data.length; i++) {
			for (int j=0; j<data.length; j++) {
				System.out.println(i + "\t" + j + "\t" + data[i][j]);
			}
		}


	}
*/

	/* METHOD getRugosity
	 * returns the rugosity data array 
	 * 
	 */

	public void getRugosity() {

		// Open the file and check to make sure it's valid.
		String filename = "LidarRugosity"+lonStart+"_"+latStart+".nc";
		NetcdfFile dataFile = null;

		try {

			dataFile = NetcdfFile.open(filename, null);
//			System.out.println("Success opening main file");

			// shouldn't need to pass the lats and lons back

			/*            Variable latVar = dataFile.findVariable("latitude");
            if (latVar == null) {
                System.out.println("Cant find Variable latitude");
                return;
            }

            Variable lonVar= dataFile.findVariable("longitude");
            if (lonVar == null) {
                System.out.println("Cant find Variable longitude");
                return;
            }
			 */
			Variable dataVar= dataFile.findVariable("rugosity");
			if (dataVar == null) {
				System.out.println("Cant find Variable rugosity");
				return;
			}


			/*          
            if(latVar.getDimensions().size() != 1) {
              System.out.println(" fail to get the dimensions of variable latitude");
                return;
            }
			 */            
			if(dataVar.getDimensions().size() != 2) {
				System.out.println(" fail to get the dimensions of variable rugosity");
				return;
			}


			// Read the latitude and longitude coordinate variables into arrays
			// latsIn and lonsIn.

			/*          ArrayFloat.D1 latArray;
            ArrayFloat.D1 lonArray;

            latArray = (ArrayFloat.D1)latVar.read();
            lonArray = (ArrayFloat.D1)lonVar.read();

            int[] shape = latArray.getShape();
            for (int i=0; i<shape[0]; i++) {
                latsIn[i] = (int) latArray.get(i);
            }

            shape = lonArray.getShape();
            for (int j=0; j<shape[0]; j++) {
                lonsIn[j] = (int) lonArray.get(j);
            }

			 */

			try {

				ArrayFloat.D2 dataArray;
				dataArray = (ArrayFloat.D2)dataVar.read(ydim+":"+ymax+":1,"+xdim+":"+xmax+":1");

				int [] shape1 = dataArray.getShape();

				Index index = dataArray.getIndex();

				for (int i=0; i<shape1[0]; i++) {
					for (int j=0; j<shape1[1]; j++) {
						data[i][j] = dataArray.getFloat(index.set(i,j));
					}
				}

			} catch (InvalidRangeException e) {
				e.printStackTrace();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}


			// Read the data. Since we know the contents of the file we know
			// that the data arrays in this program are the correct size to
			// hold all the data.
			/*           ArrayFloat.D2 dataArray;

            dataArray = (ArrayFloat.D2)dataVar.read();

            int [] shape1 = dataArray.getShape();

            for (int i=0; i<shape1[0]; i++) {
              for (int j=0; j<shape1[1]; j++) {
                	  data[i][j] = dataArray.get(i,j);
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

	}


	/* METHOD getHabitatType
	 * returns the habitatType data array 
	 * 
	 */

	public void getHabitatType(){
		// Open the file and check to make sure it's valid.
		String filename = "HabitatType"+lonStart+"_"+latStart+".nc";
		NetcdfFile dataFile = null;

		try {

			dataFile = NetcdfFile.open(filename, null);

			Variable dataVar= dataFile.findVariable("habitatType");
			if (dataVar == null) {
				System.out.println("Cant find Variable habitatType");
				return;
			}


			if(dataVar.getDimensions().size() != 2) {
				System.out.println(" fail to get the dimensions of variable habitatType");
				return;
			}


			// Read the data. Since we know the contents of the file we know
			// that the data arrays in this program are the correct size to
			// hold all the data.
			ArrayFloat.D2 dataArray;

			dataArray = (ArrayFloat.D2)dataVar.read();

			int [] shape1 = dataArray.getShape();

			for (int i=0; i<shape1[0]; i++) {
				for (int j=0; j<shape1[1]; j++) {
					data[i][j] = dataArray.get(i,j);
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
	}




	// this would Output a netCDF file of values (here, rugosity) based on lat's and lon's
	public void writeOutput(String filename, float latsIn[], float lonsIn[], float dataIn[][]) {

		int NLAT = 1000, NLON = 1000;  // dimensions of array


		// Create the file.  Change to pass in coding based on what is needed

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
			dataFile.addVariable("data", DataType.FLOAT, dims);

			// Define units attributes for variables.
			dataFile.addVariableAttribute("data", "units", "m");

			// Write the coordinate variable data. This will put the latitudes
			// and longitudes of our data grid into the netCDF file.
			dataFile.create();


			ArrayFloat.D1 dataLat = new ArrayFloat.D1(latDim.getLength());
			ArrayFloat.D1 dataLon = new ArrayFloat.D1(lonDim.getLength());

			// Create some pretend data. If this wasn't an example program, we
			// would have some real data to write, for example, model
			// output.
			int i,j;

			// here, would set data based on whatever is being passed into "write output"
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

			ArrayFloat.D2 dataWrite = new ArrayFloat.D2(latDim.getLength(), lonDim.getLength());


			for (i=0; i<latDim.getLength(); i++) {
				for (j=0; j<lonDim.getLength(); j++) {
					dataWrite.set(i,j, dataIn[i][j]);
				}
			}

			int[] origin = new int[2];

			dataFile.write("data", origin, dataWrite);


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


	}	

/*	public static void main(String[] args) {

		NetCDFIOTemp ntemp = new NetCDFIOTemp();
		rugosity = ntemp.getFile("rugosity", 580, 2802, 0, 0);

		for (int i=0; i<rugosity.length; i++) {
			for (int j=0; j<rugosity.length; j++) {
				System.out.println(i + "\t" + j + "\t" + rugosity[i][j]);
			}
		}


	}
	*/
}

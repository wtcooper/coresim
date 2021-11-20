package coralSampling;

/* Base coral survival and growth on just a few parameters:
 * 
 *	Mortality = b0 + b1*age + b2*substrate + b3*orientation
 *	Growth = b0 + b1*age + b2*substrate + b3*orientation   			 
 * 
 * 	* need to calculate a monthly mortality (versus daily) for my data
 * 
 * 	Here, don't have growth as function of substrate, so will have to inverse model this
 * 
 * 	Vary these in model and see which ones predict recruit distribution patterns in field; do this for 
 * 		model runs on just a couple of landscapes for high numbers of larvae and recuits (100,000-millions)
 * 		* use this as model validation steps
 * 
 * 	* see Wikipedia on logistic regression for calculating DSR estimates on my mortality and growth eq's
 */


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;


public class Coral {

	private Coordinate midpoint;
	private String species;
	private int speciesIndex; //the number index of the species used in CoralBuilder
	private Geometry geometry; 
	private double diameter; 

	
	public void setDiameter(double diameter) {
		this.diameter = diameter;
	}

	public double getDiameter() {
		return diameter; 
		//return 	geometry.getBoundaryDimension();
	}

	public double getSurfaceArea() {
		return geometry.getArea();
	}

	public Coordinate getMidpoint() {
		return midpoint;
	}

	public void setMidpoint(Coordinate midpoint) {
		this.midpoint = midpoint;
	}

	public String getSpecies() {
		return species;
	}

	public void setSpecies(String species) {
		this.species = species;
	}

	public Geometry getGeometry() {
		return geometry;
	}

	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

	public int getSpeciesIndex() {
		return speciesIndex;
	}

	public void setSpeciesIndex(int speciesIndex) {
		this.speciesIndex = speciesIndex;
	}


}

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

package coresimElasticity;


public class CoralElast {

	private int x;
	private int y;
//	private String name;
//	private String species;
//	private int colonyNum;
	private int diameter;
	private double surfaceArea;

	CoralElast(){
		
	}
	
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	public int getDiameter() {
		return diameter;
	}
	public void setDiameter(int diameter) {
		this.diameter = diameter;
	}
	public double getSurfaceArea() {
		return surfaceArea;
	}
	public void setSurfaceArea(double surfaceArea) {
		this.surfaceArea = surfaceArea;
	}




}

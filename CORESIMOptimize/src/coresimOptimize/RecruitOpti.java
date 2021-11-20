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

package coresimOptimize;


public class RecruitOpti {


	private int x;
	private int y;
	private int surfaceX;
	private int surfaceY;
	private double diameter;
	private int subType; 


	RecruitOpti() {

	}




	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public double getDiameter() {
		return diameter;
	}

	public void setDiameter(double diameter) {
		this.diameter = diameter;
	}



	public void setX(int x) {
		this.x = x;
	}


	public void setY(int y) {
		this.y = y;
	}


	public int getSurfaceX() {
		return surfaceX;
	}


	public void setSurfaceX(int surfaceX) {
		this.surfaceX = surfaceX;
	}


	public int getSurfaceY() {
		return surfaceY;
	}


	public void setSurfaceY(int surfaceY) {
		this.surfaceY = surfaceY;
	}



	public int getSubType() {
		return subType;
	}

	public void setSubType(int subType) {
		this.subType = subType;
	}


}

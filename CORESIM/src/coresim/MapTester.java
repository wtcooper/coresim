package coresim;

import java.util.*;
import java.awt.Point;
import java.util.Random;


public class MapTester {

	long seed = System.currentTimeMillis();
	Random rng = new Random(seed); 

	HashMap<Point, ArrayList<Recruit>> hm = new HashMap<Point, ArrayList<Recruit>>(); 

	int x, y; 

	void test(){

		// add recruits to hash map
		for (int i = 0; i<10; i++){
			x = i;
			y = i; 
			Point point = new Point(x,y);

			Recruit recruit = new Recruit(); 
			recruit.setDiameter(i); 
			ArrayList<Recruit> list = (ArrayList<Recruit>) hm.get(point);
			if (list == null) {
				list = new ArrayList<Recruit>();
				hm.put(point, list);
			}
			list.add(recruit);
		}

		for (int i = 0; i<10; i++){

			ArrayList<Recruit> list = getRecruits(i, i);

			if (list != null) {
				int looperCounter=0; 
				for (Recruit recruit: list) {
					double diameter = recruit.getDiameter(); 
					System.out.println("x: \t" + i + "\ty: \t" + i + "\tdiameter: \t" + diameter);
					looperCounter++;
				}
				System.out.println("number recruits at one location: \t" + looperCounter);  
			}		
		}

	}

	ArrayList<Recruit> getRecruits(int x, int y){
		Point point = new Point(x,y);
		ArrayList<Recruit> list = (ArrayList<Recruit>) hm.get(point);
		return list; 
	}


	public static void main(String[] args) {

		MapTester mp = new MapTester();
		mp.test();

	}

}






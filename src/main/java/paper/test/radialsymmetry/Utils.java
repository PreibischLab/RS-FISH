package paper.test.radialsymmetry;

import java.util.ArrayList;

import fit.Spot;

public class Utils {
	public static double dist(double[] e1, double[] e2){
		return Math.sqrt(dist2(e1, e2));
	}
	
	public static double dist2(double[] e1, double[] e2){
		double dist = 0;
		for (int j = 0; j < e1.length; j++)
			dist += (e1[j] - e2[j])*(e1[j] - e2[j]);
		return dist;
	}
	
	public static double rmse(ArrayList <double[] > positions, ArrayList <Spot> spots, int [] pos2spot) {
		double res = 0;
		long totalSpots = 0;
		for (int j = 0; j < positions.size(); j++) {
			if (pos2spot[j] != -1) { // there is a corresponding point 
				Spot spot = spots.get(pos2spot[j]);
				int numDimensions = spot.numDimensions();
				final double[] location = new double [numDimensions];
				spot.localize(location);
				res += dist2(location, positions.get(j));
				totalSpots++;
			} 
		}
		res /= totalSpots;
		return Math.sqrt(res);
	}
}

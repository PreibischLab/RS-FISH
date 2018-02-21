package paper.test.radialsymmetry;

import java.util.Comparator;

import fit.Spot;

public class UtilComparators {
	// contains useful comparators 
	public class doubleComparator implements Comparator<double[]>{
		@Override
		public int compare(double[] e1, double[] e2) {
			int result = 0; // same 
			for (int j = 0; j < e1.length; j++) {
				if (e1[j] < e2[j]) {
					result = -1;
					break;
				} else if (e1[j] > e2[j]) {
					result = 1;
					break;
				}
			}
			return result;
		}
	}

	public class spotComparator implements Comparator<Spot>{
		@Override
		public int compare(Spot e1, Spot e2) {
			int result = 0; // same 
			for (int j = 0; j < e1.numDimensions(); j++) {
				if (e1.getDoublePosition(j) < e2.getDoublePosition(j)) {
					result = -1;
					break;
				} else if (e1.getDoublePosition(j) > e2.getDoublePosition(j)) {
					result = 1;
					break;
				}
			}
			return result;
		}
	}

	public class approxComparator implements Comparator<double[]>{
		private double eps = 0;
		public approxComparator(double eps) {
			this.eps = eps;
		}
		@Override
		public int compare(double[] e1, double[] e2) {
			int result = 0; // same 
			if (dist(e1, e2) > eps) 
				result = 1;
			return result;
		}
	}
	
	public double dist(double[] e1, double[] e2){
		double dist = 0;
		for (int j = 0; j < e1.length; j++)
			dist += (e1[j] - e2[j])*(e1[j] - e2[j]);
		return Math.sqrt(dist);
	}

}

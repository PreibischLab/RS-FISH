package paper.test.radialsymmetry;

import java.util.Comparator;

import fit.Spot;

public class UtilComparators {
	// contains useful comparators 
	
	// TODO: I have to write my own comparator
	
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
			// TODO Auto-generated method stub
			return 0;
		}

	}
	
}

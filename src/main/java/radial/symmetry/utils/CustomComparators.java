package radial.symmetry.utils;

import java.util.ArrayList;
import java.util.Comparator;

import net.imglib2.RealPoint;

public class CustomComparators {
	
	public class PosComparator implements Comparator<RealPoint> {

		@Override
		public int compare(RealPoint a, RealPoint b) {
			int numDimensions = a.numDimensions();
			int compareTo = 0;

			double az = a.getDoublePosition(numDimensions - 1);
			double bz = b.getDoublePosition(numDimensions - 1);

			if (az < bz)
				compareTo = -1;
			if (az > bz)
				compareTo = 1;
			return compareTo;
		}
	}

	public class IndexComparator implements Comparator<Integer> {
		private final ArrayList<double[]> peaks;

		public IndexComparator(ArrayList<double[]> peaks) {
			this.peaks = peaks;
		}

		public Integer[] createIndexArray() {
			Integer[] indexes = new Integer[peaks.size()];
			for (int i = 0; i < peaks.size(); i++)
				indexes[i] = i; // Autoboxing
			return indexes;
		}

		@Override
		public int compare(Integer index1, Integer index2) {
			// Autounbox from Integer to int to use as array indexes
			int compareTo = 0;
			int numDimensions = peaks.get(index1).length;
			
			double az = peaks.get(index1)[numDimensions - 1];
			double bz = peaks.get(index2)[numDimensions - 1];

			if (az < bz)
				compareTo = -1;
			if (az > bz)
				compareTo = 1;
			return compareTo;
		}
	}
}

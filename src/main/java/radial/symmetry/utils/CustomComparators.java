/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2023 Developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
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

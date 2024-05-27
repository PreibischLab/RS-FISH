/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2024 RS-FISH developers.
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
package visualization;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import fiji.tool.SliceObserver;
import fitting.Spot;
import gui.interactive.HelperFunctions;
import gui.vizualization.ImagePlusListener;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Overlay;

public class Detections {

	ImagePlus imp;
	ArrayList<double[]> peaks;
	ArrayList<Float> intensity;
	ArrayList<Long> timePointIndices;
	Integer[] sortedIndices; // sorting order in increasing z-dim

	int numDimensions;
	long[] dimensions;

	SliceObserver sliceObserver;

	double threshold; // show only corresponding spots from the histogram

	float sigma;
	float anisotropy;
	
	// TODO: create getters and setters
	boolean isComputing = false;
	boolean isStarted = false;

	public boolean isComputing() {
		return isComputing;
	}

	public boolean isStarted() {
		return isStarted;
	}

	public double getThreshold() {
		return threshold;
	}

	// TODO: the image might be xy t, switch to the imagePlus and adjust the
	// numDimensions accordingly
	public Detections(ImagePlus imp, ArrayList<Spot> spots, ArrayList<Long> timePoint, float sigma, float anisotropy) {
		this.dimensions = HelperFunctions.getDimensions(imp.getDimensions());
		this.numDimensions = dimensions.length; // stores x y (z) only

		this.threshold = 0; // we suppose that intensities are non-negative
		
		this.sigma = sigma;
		this.anisotropy = anisotropy;

		// sort over z ?
		// show the corresponding overlay

		// TODO: add the check for the 2-4D different cases

		this.imp = imp;
		// this.imp.setDimensions( 1, (int)img.dimension( 2 ), 1 );

		this.peaks = new ArrayList<>(spots.size());
		this.intensity = new ArrayList<>(spots.size());
		this.timePointIndices = new ArrayList<>(spots.size());

		HelperFunctions.copyToDouble(spots, peaks);

		// sorts the indices to be able to get the intensities
		IndexComparator comparator = new IndexComparator(peaks);
		sortedIndices = comparator.createIndexArray();
		Arrays.sort(sortedIndices, comparator);
		// sort by z in increasing order
		// TODO: Perfom for 3D images only ? 
		Collections.sort(peaks, new PosComparator());

		permuteIntensities(spots, this.intensity, sortedIndices);
		permuteTimePoints(timePoint, this.timePointIndices, sortedIndices);

	}

	public static void permuteIntensities(ArrayList<Spot> from, ArrayList<Float> to, Integer[] idx) {
		// from and to are of the same sizes
		for (int j = 0; j < from.size(); j++)
			to.add(from.get(idx[j]).getFloatIntensity());
	}

	public static void permuteTimePoints(ArrayList<Long> timePoint, ArrayList<Long> timePointIndices, Integer[] idx) {
		// from and to are NOT of the same sizes

		ArrayList<Long> tmp = new ArrayList<>(timePointIndices.size());

		// go over all indices
		int curTimePointIdx = 0;
		long totalSpots = 0;
		for (int j = 0; j < idx.length; j++) {
			if (totalSpots <= j) {
				totalSpots += timePoint.get(curTimePointIdx);
				curTimePointIdx++;
			}
			// timePointIndices.add((long) curTimePointIdx);
			tmp.add((long) curTimePointIdx);
		}

		for (int j = 0; j < idx.length; j++) 
			timePointIndices.add(tmp.get(idx[j]) - 1); // IMP! 0-notation
	}

	// shows the detected blobs
	// images can be 2,3,4D
	// in general x y c z t
	public void showDetections() {
		imp.show();

		sliceObserver = new SliceObserver(imp, new ImagePlusListener(this));
		updatePreview(threshold);
		isStarted = true;
	}

	public void close()
	{
		try
		{
			if ( sliceObserver != null )
				sliceObserver.unregister();
		}
		catch (Exception e) {}

		imp.setOverlay( new Overlay() );
	}

	// will be triggered by the movement of the slider
	// TODO: add the threshold value for the overlays that you want to show
	public void updatePreview(double threshold) {

		this.threshold = threshold;

		Overlay overlay = imp.getOverlay(); // contains marked spots

		if (overlay == null) {
			overlay = new Overlay();
			imp.setOverlay(overlay);
		}

		overlay.clear();

		int zSlice = imp.getZ();
		int tSlice = imp.getT();
		int[] indices = new int[2];

		// image hax z ?
		if (imp.getDimensions()[3] != 1) {
			// FIXME: should work in 2D + time, too
			indices = findIndices(zSlice, sigma, anisotropy); // contains indices of the lower and upper slices
		} else {
			indices[0] = 0;
			indices[1] = peaks.size();
		}

		if (indices[0] >= 0 && indices[1] >= 0) {
			for (int curPeakIdx = indices[0]; curPeakIdx < indices[1]; curPeakIdx++) {
				if (timePointIndices.get(curPeakIdx) + 1 == tSlice) { // filter corresponding time points
					if (intensity.get(curPeakIdx) > threshold) { // this one should be the comparison with the current
																	// peak intensity
						double[] peak = peaks.get(curPeakIdx);

						final double x = peak[0];
						final double y = peak[1];

						float initRadius = sigma + 1; // make radius a bit larger than DoG sigma
						float radius = initRadius;

						if (numDimensions == 3) {
							final long z = (long) peak[2];
							radius -= Math.abs(z - (zSlice - 1));
						}

						final OvalRoi or = new OvalRoi(x - radius + 0.5, y - radius + 0.5, radius * 2, radius * 2);
						or.setStrokeColor(new Color(255, 0, 0));
						overlay.add(or);
					}
				}
			}
		}

		imp.updateAndDraw();
		isComputing = false;
	}

	// TODO: maybe possible speed up and not necessary to sort in longs
	public class PosComparator implements Comparator<double[]> {

		@Override
		public int compare(double[] a, double[] b) {
			int compareTo = 0;

			double az = a[numDimensions - 1];
			double bz = b[numDimensions - 1];

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

			double az = peaks.get(index1)[numDimensions - 1];
			double bz = peaks.get(index2)[numDimensions - 1];

			if (az < bz)
				compareTo = -1;
			if (az > bz)
				compareTo = 1;
			return compareTo;
		}
	}

	// searches for the min[IDX_1] and max[IDX_2]
	public int[] findIndices(long zSlice, float sigma, float anisotropy) {
		int[] indices = new int[2]; //

		// TODO: MAKE THE THIS THE PARAMETER THAT IS PASSED
		// THIS IS THE SCALE THAT YOU COMPUTE
		int ds =  (int)(sigma*anisotropy) + 1;// 5; // how many extra slices to consider = ds*2

		// FIXME: this imageJ problem slices vs channels!
		double lowerBound = Math.max(zSlice - ds, 1);
		double upperBound = Math.min(zSlice + ds, imp.getNSlices()); // imp.getNSlices());

		double[] tmp = new double[numDimensions];
		tmp[numDimensions - 1] = lowerBound;
		int idxLower = Collections.binarySearch(peaks, tmp, new PosComparator());
		tmp[numDimensions - 1] = upperBound;
		int idxUpper = Collections.binarySearch(peaks, tmp, new PosComparator());

		// DEBUG:
		// System.out.println(idxLower + " " + idxUpper);

		if (idxLower < 0) {
			idxLower = -(idxLower + 1);
			if (idxLower == peaks.size())
				idxLower -= 1;
		}

		if (idxUpper < 0) {
			idxUpper = -(idxUpper + 1);
			if (idxUpper == peaks.size())
				idxUpper -= 1;
		}

		// TODO: Update this to have real O(lg n) complexity

		indices[0] = idxLower;
		indices[1] = idxUpper;

		if (idxLower >= 0 && idxUpper >= 0) {
			indices[0] = updateIdx(peaks, numDimensions, zSlice, idxLower, -1);
			indices[1] = updateIdx(peaks, numDimensions, zSlice, idxUpper, 1);
		}

		return indices;
	}

	public static int updateIdx(ArrayList<double[]> peaks, int numDimensions, long zSlice, int idx, int direction) {
		int newIdx = idx;

		int from = idx;

		long zPos = (long) peaks.get(idx)[numDimensions - 1]; // current zPosition

		int j = from;
		while (zSlice == zPos) {
			if (j < 0 || j == peaks.size())
				break;

			if ((long) peaks.get(j)[numDimensions - 1] != zPos)
				break;

			newIdx = j;
			j += direction;
		}

		// System.out.println("No infinite loop; Such success!");

		return newIdx;
	}

	public static void main(String[] args) {

	}
}

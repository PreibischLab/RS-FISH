package visualization;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import fiji.tool.SliceObserver;
import fit.Spot;
import gui.interactive.HelperFunctions;
import gui.vizualization.ImagePlusListener;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import util.ImgLib2Util;

public class Detections {

	ImagePlus imp;
	ArrayList<double []> peaks;
	int numDimensions;
	long [] dimensions;

	SliceObserver sliceObserver;

	double threshold; // show only corresponding spots from the histogram

	// TODO: create getters and setters
	boolean isComputing = false;
	boolean isStarted = false;

	public boolean isComputing(){
		return isComputing;
	}

	public boolean isStarted(){
		return isStarted;
	}

	public double getThreshold(){
		return threshold;
	}
	

	public Detections(RandomAccessibleInterval <FloatType> img, ArrayList<Spot> spots){
		this.numDimensions = img.numDimensions();
		this.dimensions = new long[numDimensions];

		this.threshold = 0; // we suppose that intensities are non-negative

		// sort over z ? 
		// show the corresponding overlay 

		// TODO: add the check for the 2-4D different cases

		this.imp = ImageJFunctions.wrap(img, "");
		this.imp.setDimensions( 1, (int)img.dimension( 2 ), 1 );
		this.peaks = new ArrayList<>();

		HelperFunctions.copyToDouble(spots, peaks);
		// sort by z in increasing order 
		Collections.sort(peaks, new PosComparator());		
	}


	// shows the detected blobs
	// images can be 2,3,4D
	// in general x y c z t
	public void showDetections(){
		imp.show();

		sliceObserver = new SliceObserver(imp, new ImagePlusListener( this ));
		updatePreview(threshold);
		isStarted = true;
	}

	// will be triggered by the movement of the slider
	// TODO: add the threshold value for the overlays that you want to show
	public void updatePreview(double threshold){		
		Overlay overlay = imp.getOverlay();

		if (overlay == null) {
			// System.out.println("If this message pops up probably something
			// went wrong.");
			overlay = new Overlay();
			imp.setOverlay(overlay);
		}

		overlay.clear();		

		int zSlice = imp.getCurrentSlice(); // getZ() ? 

		int[] indices = findIndices(zSlice); // contains indices of the lower and upper slices

		if (indices[0] >= 0 && indices[1] >= 0){
			for (int curPeak = indices[0]; curPeak <= indices[1]; curPeak++){

				System.out.println("th = " + threshold);
				
				
				if (threshold > -1){ // this one should be the comparison with the current peak intensity
					double [] peak = peaks.get(curPeak);

					final double x = peak[0];
					final double y = peak[1];
					final long z = (long)peak[2];

					int initRadius = 5;
					int radius = initRadius - (int)Math.abs(z - zSlice) ;

					final OvalRoi or = new OvalRoi(x - radius + 0.5, y - radius + 0.5, radius * 2, radius * 2);
					or.setStrokeColor(new Color(255, 0, 0));
					overlay.add(or);
				}
			}
		}


		imp.updateAndDraw();
		isComputing = false;
	}

	// TODO: maybe possible speed up and not necessary to sort in longs
	public class PosComparator implements Comparator<double []>{

		@Override
		public int compare(double[] a, double [] b) {
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

	// searches for the min[IDX_1] and max[IDX_2] 
	public int[] findIndices(long zSlice){
		int[] indices = new int[2]; // 

		// TODO: MAKE THE THIS THE PARAMETER THAT IS PASSED 
		// THIS IS THE SCALE THAT YOU COMPUTE
		int ds = 5; // how many extra slices to consider = ds*2

		// FIXME: this imageJ problem slices vs channels!
		double lowerBound = Math.max(zSlice - ds, 1);
		double upperBound = Math.min(zSlice + ds, imp.getNSlices()); // imp.getNSlices());

		double [] tmp = new double[numDimensions];
		tmp[numDimensions - 1] = lowerBound;
		int idxLower = Collections.binarySearch(peaks, tmp, new PosComparator());
		tmp[numDimensions - 1] = upperBound;
		int idxUpper = Collections.binarySearch(peaks, tmp, new PosComparator());

		// DEBUG: 
		// System.out.println(idxLower + " "  + idxUpper);		

		if (idxLower < 0){
			idxLower = -(idxLower + 1);
			if (idxLower == peaks.size())
				idxLower -= 1;
		}

		if (idxUpper < 0){
			idxUpper = -(idxUpper + 1);
			if (idxUpper == peaks.size())
				idxUpper -= 1;
		}

		//TODO: Update this to have real O(lg n) complexity

		indices[0] = idxLower;
		indices[1] = idxUpper;

		if (idxLower >= 0 && idxUpper >= 0){
			indices[0] = updateIdx(peaks, numDimensions, zSlice, idxLower, -1);
			indices[1] = updateIdx(peaks, numDimensions, zSlice, idxUpper, 1);
		}

		return indices;
	}

	public static int updateIdx(ArrayList<double[]> peaks, int numDimensions, long zSlice, int idx, int direction){
		int newIdx = idx;

		int from = idx;

		long zPos = (long) peaks.get(idx)[numDimensions - 1]; // current zPosition

		int j = from;
		while(zSlice == zPos){
			if (j < 0 || j == peaks.size())
				break;

			if((long)peaks.get(j)[numDimensions - 1] != zPos)
				break;

			newIdx = j;
			j += direction;
		}

		// System.out.println("No infinite loop; Such success!");

		return newIdx;
	}


	public static void main(String [] args){

	}
}

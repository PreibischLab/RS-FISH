package visualization;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import fiji.tool.SliceObserver;
import fit.Spot;
import gui.interactive.HelperFunctions;
import gui.vizualization.ImagePlusListener;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;

public class Detections {

	ImagePlus imp;
	ArrayList<double []> peaks;
	int numDimensions;
	long [] dimensions;


	SliceObserver sliceObserver;

	// TODO: create getters and setters
	boolean isComputing = false;
	boolean isStarted = false;

	public boolean isComputing(){
		return isComputing;
	}

	public boolean isStarted(){
		return isStarted;
	}


	public Detections(RandomAccessibleInterval <FloatType> img, ArrayList<Spot> spots){
		this.numDimensions = img.numDimensions();
		this.dimensions = new long[numDimensions];
		// sort over z ? 
		// show the corresponding overlay 

		// TODO: add the check for the 2-4D different cases

		this.imp = ImageJFunctions.wrap(img, "");
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
		updatePreview();
		isStarted = true;
	}

	// will be triggered by the movement of the slider
	public void updatePreview(){		
		Overlay overlay = imp.getOverlay();

		if (overlay == null) {
			// System.out.println("If this message pops up probably something
			// went wrong.");
			overlay = new Overlay();
			imp.setOverlay(overlay);
		}

		overlay.clear();		

		int zSlice = imp.getCurrentSlice(); // getZ() ? 

		//		int ds = 5; // how many extra slices to consider = ds*2
		//
		//		double lowerBound = Math.max(zSlice - ds, 1);
		//		double upperBound = Math.min(zSlice + ds, imp.getNChannels()); // imp.getNSlices());
		//
		//
		//		System.out.println(lowerBound + " " + zSlice + " " + upperBound);
		//
		//
		//		double [] tmp = new double[numDimensions];
		//		tmp[numDimensions - 1] = lowerBound;
		//
		//
		//		int peakLower = Collections.binarySearch(peaks, tmp, new PosComparator());
		//
		//
		//		tmp[numDimensions - 1] = upperBound;
		//
		//		int peakUpper = Collections.binarySearch(peaks, tmp, Collections.reverseOrder(new PosComparator()));
		//
		//		System.out.println(peakLower + " " + peakUpper);


		int[] indices = findIndices(zSlice); // contains indices of the lower and upper slices


		System.out.println(indices[0] + " " + zSlice + " " + indices[1]);

		if (indices[0] >= 0 && indices[1] >= 0){
			for (int curPeak = indices[0]; curPeak <= indices[1]; curPeak++){

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

		// TODO: Move this part to the listeners -> gui.vizualization 
		//		for (final long[] peak : peaks) {
		//			final float x = peak[0];
		//			final float y = peak[1];
		//			final float z = peak[2];
		//
		//			// TODO: make a loop here to show [-3 +3] slices too
		//			// TODO: make the search here more efficient.
		//			// maybe binary search to find the lowest and the highest index values 
		//			
		//			// defines how many slices to take
		//			
		//			
		//			
		//			
		////			for (int curSlice = Math.max(zSlice - ds, 1); curSlice <= Math.min(zSlice + ds, imp.getNSlices()); curSlice++){
		//				if (z == zSlice){
		//					
		//					// System.out.println(x + " " + y + " " + z);
		//		
		//					// TODO: Should be adaptive
		//					int radius = 3;
		//		
		//					final OvalRoi or = new OvalRoi(x - radius + 0.5, y - radius + 0.5, radius * 2, radius * 2);
		//					or.setStrokeColor(new Color(255, 0, 0));
		//					overlay.add(or);
		//				}
		////			}
		//		
		//		}		

		imp.updateAndDraw();
		isComputing = false;
	}

	public class PosComparator implements Comparator<double []>{

		@Override
		public int compare(double[] a, double [] b) {
			int compareTo = 0;
			if (a[numDimensions - 1] < b[numDimensions - 1])
				compareTo = -1;
			if (a[numDimensions - 1] > b[numDimensions - 1])
				compareTo = 1;
			return compareTo;
		}

	}

	// searches for the min[IDX_1] and max[IDX_2] 
	public int[] findIndices(long zSlice){
		int[] indices = new int[2]; // 

		int ds = 5; // how many extra slices to consider = ds*2

		// FIXME: this imageJ problem slices vs channels!
		double lowerBound = Math.max(zSlice - ds, 1);
		double upperBound = Math.min(zSlice + ds, imp.getNChannels()); // imp.getNSlices());
		
		double [] tmp = new double[numDimensions];
		tmp[numDimensions - 1] = lowerBound;
		int idxLower = Collections.binarySearch(peaks, tmp, new PosComparator());
		tmp[numDimensions - 1] = upperBound;
		int idxUpper = Collections.binarySearch(peaks, tmp, Collections.reverseOrder(new PosComparator()));

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

		long zPos = (long) peaks.get(idx)[numDimensions]; // current zPosition

		int j = from;
		while(zSlice == zPos){
			if (j < 0 || j == peaks.size())
				break;

			if((long)peaks.get(j)[numDimensions] != zPos)
				break;
			else
				newIdx = j;

			j += direction;
		}

		System.out.println("No infinite loop; Such success!");

		return newIdx;
	}


	public static void main(String [] args){

	}
}

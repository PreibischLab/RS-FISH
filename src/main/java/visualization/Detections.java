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
	ArrayList<long[]> peaks;
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

		HelperFunctions.copyToLong(spots, peaks);
		// sort by z in increasing order 
		Collections.sort(peaks, new Comparator<long[]>() {
			@Override
			public int compare(long[] a, long [] b) {
				int compareTo = 0;
				if (a[numDimensions - 1] < b[numDimensions - 1])
					compareTo = -1;
				if (a[numDimensions - 1] > b[numDimensions - 1])
					compareTo = 1;
				return compareTo;
			}
		});

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
		int ds = 5;

		long lowerBound = Math.max(zSlice - ds, 1);
		long upperBound = Math.min(zSlice + ds, imp.getNChannels()); // imp.getNSlices());


		System.out.println(lowerBound + " " + zSlice + " " + upperBound);


		long [] tmp = new long[numDimensions];
		tmp[numDimensions - 1] = lowerBound;


		int peakLower = Collections.binarySearch(peaks, tmp, new Comparator<long[]>() {
			@Override
			public int compare(long[] a, long [] b) {
				int compareTo = 0;
				if (a[numDimensions - 1] < b[numDimensions - 1])
					compareTo = -1;
				if (a[numDimensions - 1] > b[numDimensions - 1])
					compareTo = 1;
				return compareTo;
			}
		});


		tmp[numDimensions - 1] = upperBound;

		int peakUpper = Collections.binarySearch(peaks, tmp, new Comparator<long[]>() {
			@Override
			public int compare(long[] a, long [] b) {
				int compareTo = 0;
				if (a[numDimensions - 1] < b[numDimensions - 1])
					compareTo = -1;
				if (a[numDimensions - 1] > b[numDimensions - 1])
					compareTo = 1;
				return compareTo;
			}
		});

		System.out.println(peakLower + " " + peakUpper);


		if (peakLower >= 0 && peakUpper >= 0){

			for (int curPeak = peakLower; curPeak <= peakUpper; curPeak++){

				long [] peak = peaks.get(curPeak);

				final float x = peak[0];
				final float y = peak[1];
				final long z = peak[2];

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

	public static void main(String [] args){

	}
}

package visualization;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import fit.Spot;
import gui.interactive.HelperFunctions;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Roi;

import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;

public class Detections {

	// shows the detected blobs
	// images can be 2,3,4D
	// in general x y c z t
	public static void showDetections(RandomAccessibleInterval <FloatType> img, ArrayList<Spot> spots){
		// sort over z ? 
		// show the corresponding overlay 

		// TODO: add the check for the 2-4D different cases

		int numDimensions = img.numDimensions();
		long [] dimensions = new long[numDimensions];

		// Img<FloatType> rImg = new ArrayImgFactory<FloatType>().create(dimensions, new FloatType());
		ImagePlus imp = ImageJFunctions.wrap(img, "");

		ArrayList<long[]> peaks = new ArrayList<>();

		HelperFunctions.copyToLong(spots, peaks);

		Collections.sort(peaks, new Comparator<long[]>() {
			@Override
			public int compare(long[] a, long [] b) {
				return  (a[numDimensions - 1] >= b[numDimensions - 1]) ? 1 : -1;
			}
		});

		Overlay overlay = imp.getOverlay();

		if (overlay == null) {
			// System.out.println("If this message pops up probably something
			// went wrong.");
			overlay = new Overlay();
			imp.setOverlay(overlay);
		}

		// TODO: Move this part to the listeners -> gui.vizualization 
		for (final long[] peak : peaks) {
			final float x = peak[0];
			final float y = peak[1];
			
			System.out.println(x + " " + y);
			
			// TODO: Should be adaptive
			int radius = 3;
	
			// +0.5 is to center in on the middle of the detection pixel
			// cross roi
	//			final Roi lrv = new Roi(x - radius + 0.5, y + 0.5, radius * 2, 0);
	//			final Roi lrh = new Roi(x + 0.5, y - radius + 0.5, 0, radius * 2);
	//			
	//
	//			lrv.setStrokeColor(new Color(255, 0, 0));
	//			overlay.add(lrv);
	//			overlay.add(lrh);
			
			final OvalRoi or = new OvalRoi(x - radius + 0.5, y - radius + 0.5, radius * 2, radius * 2);
			or.setStrokeColor(new Color(255, 0, 0));
			overlay.add(or);
		}

		imp.show();
		imp.updateAndDraw();
	}

	public static void main(String [] args){

	}
}

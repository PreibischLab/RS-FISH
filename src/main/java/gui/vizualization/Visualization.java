package gui.vizualization;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.img.display.imagej.ImageJFunctions;

import fit.Spot;
import histogram.Histogram;
import ij.ImagePlus;
import visualization.Detections;
import visualization.Inliers;

public class Visualization {

	public static void showVisualization(ImagePlus imp, ArrayList<Spot> allSpots, ArrayList<Float> intensity, ArrayList<Long> timePoint, boolean showInliers, boolean showDetections){

		Detections detection;

		// show the initial images overlapped with ransac regions
		if (showInliers) {
			Inliers.showInliers(imp, allSpots, timePoint);
		}
		// show the detections + the histogram (doesn't make sense to show it without the detections)
		if (showDetections){
			detection = new Detections(imp, allSpots, intensity, timePoint);
			detection.showDetections();
			
			// do not show histogram if you have zero spots
			if (allSpots.size() != 0){
				final List< Double > values = new ArrayList< Double >(intensity.size());

				for (final Float i : intensity )
					values.add(i.doubleValue());
				// TODO: make this parameter dynamic ?
				int numBins = 100;

				final Histogram demo = new Histogram( values, numBins, "Intensity distribution", "", detection);
				demo.showHistogram();
			}
		}		
	}
}

package gui.radial.symmetry.vizualization;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.multithreading.SimpleMultiThreading;

import fitting.Spot;
import gui.radial.symmetry.histogram.Histogram;
import ij.ImagePlus;

public class Visualization {
	
	public static double histThreshold = 0;

	public static void showVisualization(ImagePlus imp, ArrayList<Spot> allSpots, ArrayList<Float> intensity, ArrayList<Long> timePoint, boolean showInliers, boolean showDetections, float sigma, float anisotropy){

		Detections detection;

		// show the initial images overlapped with ransac regions
		if (showInliers) {
			Inliers.showInliers(imp, allSpots, timePoint);
		}
		// show the detections + the histogram (doesn't make sense to show it without the detections)
		if (showDetections){
			detection = new Detections(imp, allSpots, intensity, timePoint, sigma, anisotropy);
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
				
				do {
					// TODO: change to something that is not deprecated
					SimpleMultiThreading.threadWait(100);
				} while (!demo.isFinished());
				
				histThreshold = demo.getHistThreshold();
			}
		}		
	}
	
	public static double getHistThreshold(){
		return histThreshold;
	}
	
}

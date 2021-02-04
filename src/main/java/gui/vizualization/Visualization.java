package gui.vizualization;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import fitting.Spot;
import histogram.Histogram;
import ij.ImagePlus;
import visualization.Detections;

public class Visualization {

	public static int numBins = 100;

	public static Detections showDetections(
			ImagePlus imp,
			ArrayList<Spot> allSpots,
			ArrayList<Long> timePoint,
			float sigma,
			double anisotropy )
	{
		Detections detection = new Detections(imp, allSpots, timePoint, sigma, (float)anisotropy);
		detection.showDetections();
		return detection;
	}

	public static double visuallyDefineThreshold(
			ImagePlus imp,
			ArrayList<Spot> allSpots,
			ArrayList<Long> timePoint,
			//boolean showInliers,
			float sigma,
			double anisotropy){

		// show the initial images overlapped with ransac regions
		//if (showInliers)
		//	Inliers.showInliers(imp, allSpots, timePoint);

		// do not show histogram if you have zero spots
		if (allSpots.size() != 0)
		{
			// show the detections + the histogram (doesn't make sense to show it without the detections)
			final Detections detection = showDetections(imp, allSpots, timePoint, sigma, anisotropy);

			final List< Double > values = allSpots.stream().map( s -> s.getIntensity() ).collect( Collectors.toList() );

			final Histogram demo = new Histogram( values, numBins, "Intensity distribution", "", detection);
			demo.showHistogram();

			do
			{
				try
				{
					Thread.sleep( 100 );
				}
				catch ( final InterruptedException e ) {}
			} while (!demo.isFinished());

			return demo.getHistThreshold();
		}
		else
			return 0.0;
	}
}

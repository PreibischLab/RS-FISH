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

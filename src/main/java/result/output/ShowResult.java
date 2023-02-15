/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2023 RS-FISH developers.
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
package result.output;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import benchmark.TextFileAccess;
import fitting.Spot;
import gui.interactive.HelperFunctions;
import ij.measure.ResultsTable;

public class ShowResult {
	public static void ransacResultCsv(
			final ArrayList<Spot> spots, final ArrayList<Long> timePoint,
			final ArrayList<Long> channelPoint, double histThreshold, final String csvFile)
	{
		if ( spots == null || spots.size() == 0 )
		{
			HelperFunctions.log( "No spots found, nothing to write.");
			return;
		}

		PrintWriter out = TextFileAccess.openFileWrite( csvFile );

		if ( spots.get( 0 ).numDimensions() == 3 )
			out.println("x,y,z,t,c,intensity");
		else
			out.println("x,y,t,c,intensity");

		int currentTimePoint = 0;
		int totalSpotsPerTimePoint = 0;

		int currentChannelPoint = 0;
		int totalSpotsPerChannelPoint = 0;

		int idx = 0;
		for (Spot spot : spots) {
			// if spot was not discarded
					if (spot.getIntensity() >= histThreshold) {

						idx++;

						for (int d = 0; d < spot.numDimensions(); ++d)
							out.print( String.format(java.util.Locale.US, "%.4f", spot.getDoublePosition(d)) + "," );

						totalSpotsPerTimePoint++;
						if (totalSpotsPerTimePoint > timePoint.get(currentTimePoint)) {
							currentTimePoint++;
							totalSpotsPerTimePoint = 1;
						}
						out.print( (currentTimePoint + 1) + "," );

						totalSpotsPerChannelPoint++;
						if (totalSpotsPerChannelPoint > channelPoint.get(currentChannelPoint)) {
							currentChannelPoint++;
							totalSpotsPerChannelPoint = 1;
						}
						out.print( (currentChannelPoint + 1) + "," ); // user-friendly, starting the counting from 1

						out.println(String.format(java.util.Locale.US, "%.4f", spot.getIntensity()));

					}
		}
		HelperFunctions.log("Spots found = " + idx);
		out.close();
	}

	public static ArrayList<double[]> points( final ArrayList<Spot> spots, double histThreshold)
	{
		if ( spots == null || spots.size() == 0 )
		{
			HelperFunctions.log( "No spots found, nothing to write.");
			return new ArrayList<>();
		}

		ArrayList<double[]> points = new ArrayList<>();

		for (Spot spot : spots) {
			// if spot was not discarded
					if (spot.getIntensity() >= histThreshold) {

						final double[] l = new double[ spot.numDimensions() + 1 ];
						for (int d = 0; d < spot.numDimensions(); ++d)
							l[ d ] = spot.getDoublePosition(d);
						l[ spot.numDimensions() ] = spot.getIntensity();

						points.add( l );
					}
		}
		HelperFunctions.log("Spots found = " + points.size());

		return points;
	}

	// ineractive mode
	// this function will show the result of RANSAC
	// proper window -> dialog view with the columns
	public static ResultsTable ransacResultTable(final ArrayList<Spot> spots, final ArrayList<Long> timePoint,
												 final ArrayList<Long> channelPoint, double histThreshold) {
		// real output
		ResultsTable rt = new ResultsTable();
		String[] xyz = { "x", "y", "z" };
		int currentTimePoint = 0;
		int totalSpotsPerTimePoint = 0;

		int currentChannelPoint = 0;
		int totalSpotsPerChannelPoint = 0;

		int idx = 0;
		for (Spot spot : spots) {
			// if spot was not discarded
					if (spot.getIntensity() >= histThreshold) {
						rt.incrementCounter();
						for (int d = 0; d < spot.numDimensions(); ++d) {
							rt.addValue(xyz[d], String.format(java.util.Locale.US, "%.4f", spot.getDoublePosition(d)));
						}

						totalSpotsPerTimePoint++;
						if (totalSpotsPerTimePoint > timePoint.get(currentTimePoint)) {
							currentTimePoint++;
							totalSpotsPerTimePoint = 1;
						}
						rt.addValue("t", currentTimePoint + 1); // user-friendly, starting the counting from 1

						totalSpotsPerChannelPoint++;
						if (totalSpotsPerChannelPoint > channelPoint.get(currentChannelPoint)) {
							currentChannelPoint++;
							totalSpotsPerChannelPoint = 1;
						}
						rt.addValue("c", currentChannelPoint + 1); // user-friendly, starting the counting from 1

						rt.addValue("intensity", String.format(java.util.Locale.US, "%.4f", spot.getIntensity()));

					}
			idx++;
		}
		HelperFunctions.log("Spots found = " + rt.getCounter());

		return rt;
	}

	public static ResultsTable ransacResultTable(final List<double[]> spots ) {
		// real output
		ResultsTable rt = new ResultsTable();
		String[] xyz = { "x", "y", "z" };

		for (final double[] spot : spots) {
			rt.incrementCounter();

			for (int d = 0; d < spot.length - 1; ++d)
				rt.addValue(xyz[d], String.format(java.util.Locale.US, "%.4f", spot[ d ] ) );

			rt.addValue("t", 1 ); // user-friendly, starting the counting from 1
			rt.addValue("c", 1 ); // user-friendly, starting the counting from 1

			rt.addValue("intensity", String.format(java.util.Locale.US, "%.4f", spot[ spot.length - 1 ] ));
		}

		return rt;
	}

}

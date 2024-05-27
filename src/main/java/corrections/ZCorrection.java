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
package corrections;

import cmd.VisualizePointsBDV;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import static corrections.QuadraticFunctionAxisDifference.polyFunc;
import static corrections.QuadraticFunctionAxisDifference.quadraticFit;
import fit.PointFunctionMatch;
import fit.polynomial.QuadraticFunction;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import mpicbg.models.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import umontreal.ssj.probdist.GammaDist;

public class ZCorrection implements Callable<Void>
{
	// input file
	@Option(names = {"-i"}, required = true, description = "input CSV, e.g. -i input.csv")
	protected List< String > csvIn = null;

	@Option(names = {"-o"}, required = true, description = "output CSV, e.g. -o corrected.csv")
	protected List< String > csvOut = null;

	@Option(names = {"-m"}, required = false, description = "mask image (background = 0, foreground > 0), e.g. -m mask.tif")
	protected List< String > mask = null;

	public ZCorrection(List<String> csvIn, List<String> csvOut, List<String> mask) {
		this.csvIn = csvIn;
		this.csvOut = csvOut;
		this.mask = mask;
	}

	public ZCorrection() {
	}



	@Override
	public Void call() throws Exception {

		if ( csvIn.size() != csvOut.size() )
		{
			System.out.println( "Number of input and output CSVs does not match. stopping.");
			return null;
		}

		for ( int i = 0; i < csvIn.size(); ++i )
		{
			//
			// read CSV
			//
			System.out.println( "Reading " + csvIn.get( i ) );

			String[] nextLine;
			CSVReader reader = new CSVReader(new FileReader(csvIn.get( i )));
			ArrayList< InputSpot > spots = new ArrayList<>();

			// skip header
			reader.readNext();

			while ((nextLine = reader.readNext()) != null )
			{
				// x,y,z,t,c,intensity
				InputSpot s = new InputSpot();
				s.x = Double.parseDouble( nextLine[ 0 ] );
				s.y = Double.parseDouble( nextLine[ 1 ] );
				s.z = Double.parseDouble( nextLine[ 2 ] );
				s.t = Integer.parseInt( nextLine[ 3 ] );
				s.c = Integer.parseInt( nextLine[ 4 ] );
				s.intensity = Double.parseDouble( nextLine[ 5 ] ) ; // was unsigned short - 32768
				spots.add( s );
			}

			System.out.println( "Loaded: " + spots.size() + " spots.");
			reader.close();

			//
			// optionally filter the spots with the mask
			//
			if ( mask != null && mask.size() > 0 )
			{
				System.out.println( "Filtering locations using mask image: " + mask.get( i ) );

				final ArrayList< InputSpot > spotsTmp = new ArrayList<>();

				final RandomAccessibleInterval img = VisualizePointsBDV.open( mask.get( i ), null );

				if ( img.numDimensions() != 2 )
				{
					throw new IOException( "2D image required, but is " + img.numDimensions() );
				}
				else
				{
					System.out.println( "Image size=" + Util.printInterval( img ) );
				}

				final RealRandomAccess rra = Views.interpolate( Views.extendBorder( img ) , new NearestNeighborInterpolatorFactory<>() ).realRandomAccess();

				for ( final InputSpot s : spots )
				{
					rra.setPosition( s.x, 0 );
					rra.setPosition( s.y, 1 );

					if ( ((RealType)rra.get()).getRealDouble() > 0 )
						spotsTmp.add( s );
				}

				System.out.println( "Remaining spots=" + spotsTmp.size() + ", previously=" + spots.size() );

				spots = spotsTmp;
			}

			//
			// fit quadratic function
			//
			final double[] minMaxI = new double[] { Double.MAX_VALUE, -Double.MAX_VALUE };
			final double[] minMaxZ = new double[] { Double.MAX_VALUE, -Double.MAX_VALUE };

			final List< Point > points = spots.stream().map(s -> {
					minMaxI[ 0 ] = Math.min( minMaxI[ 0 ], s.intensity );
					minMaxI[ 1 ] = Math.max( minMaxI[ 1 ], s.intensity );
					minMaxZ[ 0 ] = Math.min( minMaxZ[ 0 ], s.z );
					minMaxZ[ 1 ] = Math.max( minMaxZ[ 1 ], s.z );
					return new Point( new double[]{ s.z, s.intensity} );
				} ).collect(Collectors.toList() );

			final double epsilon = ( minMaxI[ 1 ] - minMaxI[ 0 ] ) * 0.1;
			System.out.println("minI=" + minMaxI[ 0 ] + ", maxI=" + minMaxI[ 1 ] );

			final Pair<QuadraticFunction, ArrayList<PointFunctionMatch>> result = quadraticFit(points, epsilon, 0.3, 1000 );

			//
			// apply quadratic function
			//
			final double minZIntensity = polyFunc( minMaxZ[ 0 ], result.getA() );
			final double[] data = new double[ spots.size() ];
			int j = 0;

			
			for ( final InputSpot spot : spots )
			{
				spot.adjustedIntensity = data[ j++] = spot.intensity * minZIntensity / polyFunc( spot.z, result.getA() );
				//System.out.println( spot.z +"," + spot.intensity + "," + spot.adjustedIntensity + "," + polyFunc( spot.z, result.getA() ) );
			}

			//
			// normalize 0...1 using Gamma distribution
			//

			// new min/max after adjustment
			double minI = data[ 0 ];
			double maxI = data[ 0 ];

			for ( final double v : data )
			{
				minI = Math.min( minI, v );
				maxI = Math.max( maxI, v );
			}

			System.out.println("adjusted minI=" + minI + ", maxI=" + maxI );

			/*
			final double[] loghist = new double[ 25 ]; // 25 bins covering min to max
			final double[] loghistDist = new double[ data.length ];

			for ( j = 0; j < data.length; ++j )
			{
				final double f = ( Math.log10( data[ j ] ) - minI ) / ( maxI -minI );
				final int bin = Math.max( 0, Math.min( loghist.length - 1, (int)Math.round( f * ( loghist.length - 1 ) ) ) );
				++loghist[ bin ];
				loghistDist[ j ] = bin;
			}

			final NormalDist distHistGauss = NormalDist.getInstanceFromMLE( loghistDist, loghistDist.length );
			System.out.println( "gauss max gradient at: " + distHistGauss.getMean() );// maxGradient( distHistGauss, hist.length / 2 ) );
			System.out.println( ( distHistGauss.getMean() / ( loghist.length - 1 ) ) * ( maxI -minI ) + minI );
			System.out.println( Math.pow( 10, ( distHistGauss.getMean() / ( loghist.length - 1 ) ) * ( maxI -minI ) + minI ) );

			final GammaDist distHistGamma = GammaDist.getInstanceFromMLE( loghistDist, loghistDist.length );
			System.out.println( "GammaDist max gradient at: " + maxGradient( distHistGamma, loghist.length / 2 ) );

			final NormalDist distGauss = NormalDist.getInstanceFromMLE( data, data.length );
			System.out.println( "gauss max gradient at: " + distGauss.getMean() );
			*/

			// https://en.wikipedia.org/wiki/Gamma_distribution
			final GammaDist dist = GammaDist.getInstanceFromMLE(data, data.length);
			final double maxGradient = maxGradient( dist, (minI + maxI) / 2 );
			System.out.println( "gamma mode@I=" + maxGradient );
			System.out.println( "gamma stdev=" + dist.getStandardDeviation() );
			System.out.println( "gamma variance=" + dist.getVariance() );
			System.out.println( "gamma alpha=" + dist.getAlpha() );
			System.out.println( "gamma lambda=" + dist.getLambda() );
			System.out.println( "gamma mean=" + dist.getMean() );
			System.out.println( "gamma Xinf=" + dist.getXinf() );
			System.out.println( "gamma Xsup=" + dist.getXsup() );
			
			for (double x =minI; x<maxI; x+=100)
			{
				System.out.println( x + "\t" + dist.density(x));
			}

			for ( final InputSpot spot : spots )
			{
				//System.out.println( spot.z +"," + spot.intensity + "," + spot.adjustedIntensity + "," + spot.adjustedIntensity / maxGradient);
				spot.adjustedIntensity /= maxGradient;
			}

			// correct and write new CSV
			System.out.println( "Writing " + csvOut.get( i ) );

			final CSVWriter writer = new CSVWriter(new FileWriter(csvOut.get( i )));

			nextLine = new String[ 6 ];
			nextLine[ 0 ] = "x";
			nextLine[ 1 ] = "y";
			nextLine[ 2 ] = "z";
			nextLine[ 3 ] = "t";
			nextLine[ 4 ] = "c";
			nextLine[ 5 ] = "intensity";//x,y,z,t,c,intensity
			writer.writeNext(nextLine);

			for ( final InputSpot spot : spots )
			{
				nextLine[ 0 ] = Double.toString( spot.x );
				nextLine[ 1 ] = Double.toString( spot.y );
				nextLine[ 2 ] = Double.toString( spot.z );
				nextLine[ 3 ] = "1";
				nextLine[ 4 ] = "1";
				nextLine[ 5 ] = Double.toString( spot.adjustedIntensity );//x,y,z,t,c,intensity

				writer.writeNext(nextLine);
			}

			writer.close();
		}

		System.out.println( "done." );
		return null;
	}

	protected static double maxGradient( final GammaDist dist, double x )
	{
		double gradient = gradientAt( dist, x );
		double step = 10000;

		//System.out.println( x + "," + gradient );

		boolean foundBetter = false;

		do
		{
			do
			{
				double gradientA = Math.abs( gradientAt( dist, x + step ) );
				double gradientB = Math.abs( gradientAt( dist, x - step ) );
	
				double newGradient = Math.max( gradientA, gradientB );
				if ( newGradient > gradient )
				{
					foundBetter = true;
					if ( gradientA > gradientB )
					{
						x = x + step;
						gradient = gradientA;
						//System.out.println( x + "," + gradient + " (" + step + ")" );
					}
					else
					{
						x = x - step;
						gradient = gradientB;
						//System.out.println( x + "," + gradient + " (" + -step + ")" );
					}
					
				}
				else
				{
					foundBetter = false;
				}
			}
			while ( foundBetter );

			step /= 1.001;
		}
		while (step > 1E-4 );

		return x;
	}

	protected static double gradientAt( final GammaDist dist, double x )
	{
		final double z0 = dist.cdf( x );
		final double z1 = dist.cdf( x + 1E-6 );

		return (z1-z0)/1E-6;
	}

	public static void main( String[] args )
	{
		new CommandLine(new ZCorrection()).execute(args);
	}
}

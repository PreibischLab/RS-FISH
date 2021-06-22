package corrections;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import cmd.VisualizePointsBDV;
import fit.PointFunctionMatch;
import fit.polynomial.NewtonRaphson;
import fit.polynomial.QuadraticFunction;
import mpicbg.models.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.util.Util;
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import umontreal.ssj.probdist.GammaDist;
import umontreal.ssj.probdist.LognormalDist;
import umontreal.ssj.probdist.NormalDist;

public class ZCorrection implements Callable<Void>
{
	// input file
	@Option(names = {"-i"}, required = true, description = "input CSV, e.g. -i input.csv")
	private List< String > csvIn = null;

	@Option(names = {"-o"}, required = true, description = "output CSV, e.g. -o corrected.csv")
	private List< String > csvOut = null;

	@Option(names = {"-m"}, required = false, description = "mask image (background = 0, foreground > 0), e.g. -m mask.tif")
	private List< String > mask = null;

	public static Pair< QuadraticFunction, ArrayList<PointFunctionMatch> > quadraticFit(
			final List<Point> points,
			final double epsilon,
			final double minInlierRatio,
			final int nIterations )
	{
		//int nIterations = 1000;
		//double epsilon = 0.1;
		//double minInlierRatio = 0.5;
		QuadraticFunction qf = new QuadraticFunctionAxisDifference();

		if ( points.size() < qf.getMinNumMatches() )
			throw new RuntimeException( "Not enough points for fitting a quadratic function. Candidates=" + points.size() );

		final ArrayList<PointFunctionMatch> candidates = new ArrayList<>();
		final ArrayList<PointFunctionMatch> inliers = new ArrayList<>();

		for (final Point p : points)
			candidates.add(new PointFunctionMatch(p));

		try {
			System.out.println( "nIterations=" + nIterations + ", epsilon=" + epsilon + ", minInlierRatio=" + minInlierRatio );
			
			qf.filterRansac(candidates, inliers, nIterations, epsilon, minInlierRatio, qf.getMinNumMatches() );

			if ( inliers.size() < qf.getMinNumMatches() )
				throw new RuntimeException( "Couldn't fix quadratic function. Candidates=" + candidates.size() + ", inliers=" +inliers.size() );

			//qf.fit(inliers);
		}
		catch (Exception exc) {
			exc.printStackTrace();
		}

		double zMin = Double.MAX_VALUE;
		double zMax = -Double.MAX_VALUE;

		double avgError = 0;
		double maxError = 0;

		for ( final PointFunctionMatch p : inliers )
		{
			p.apply( qf );
			final double distance = p.getDistance();

			// x is z, y is intensity
			zMin = Math.min( zMin, p.getP1().getL()[ 0 ] );
			zMax = Math.max( zMax, p.getP1().getL()[ 0 ] );

			avgError += distance;
			maxError = Math.max( maxError, distance );

			//System.out.println( p.getP1().getL()[ 0 ] + ", " +  p.getP1().getL()[ 1 ] + ", " + polyFunc(p.getP1().getL()[ 0 ], qf) );
		}

		System.out.println( "candidates=" + candidates.size() + ", inliers=" + inliers.size() + ", avg err=" + (avgError/inliers.size()) + ", max error=" + maxError + ", zMin=" + zMin + ", zMax=" + zMax + ", " + qf );

		return new ValuePair<>( qf, inliers );
	}

	// return y for y = a*x*x + b*x + c
	public static double polyFunc(final double x, final QuadraticFunction f ) {
		return f.getC() + x*f.getB() + x*x*f.getA();
	}
	
	public static class InputSpot
	{
		double x,y,z,intensity,adjustedIntensity;
		int t,c;
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
				s.intensity = Double.parseDouble( nextLine[ 5 ] ) - 32768; // was unsigned short
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
					System.out.println( "2D image required, but is " + img.numDimensions() );
					System.exit( 0 );
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

			final GammaDist dist = GammaDist.getInstanceFromMLE(data, data.length);
			final double maxGradient = maxGradient( dist, (minI + maxI) / 2 );
			System.out.println( "gamma mode@I=" + maxGradient );
	
			for ( final InputSpot spot : spots )
			{
				//System.out.println( spot.z +"," + spot.intensity + "," + spot.adjustedIntensity + "," + spot.adjustedIntensity / maxGradient);
				spot.adjustedIntensity /= maxGradient;
			}

			// correct and write new CSV
			System.out.println( "Writing " + csvOut.get( i ) );

			final CSVWriter writer = new CSVWriter(new FileWriter(csvOut.get( i )), ',', CSVWriter.NO_QUOTE_CHARACTER);

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

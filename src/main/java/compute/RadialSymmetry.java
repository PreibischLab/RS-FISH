package compute;

import java.awt.Rectangle;
import java.util.ArrayList;

import background.NormalizedGradient;
import background.NormalizedGradientAverage;
import background.NormalizedGradientMedian;
import background.NormalizedGradientRANSAC;
import fitting.Center.CenterMethod;
import fitting.Spot;
import fitting.SymmetryCenter3d;
import gradient.Gradient;
import gradient.GradientOnDemand;
import gradient.GradientPreCompute;
import gui.interactive.HelperFunctions;
import ij.IJ;
import intensity.Intensity;
import net.imglib2.Interval;
import net.imglib2.KDTree;
import net.imglib2.Point;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.dog.DogDetection;
import net.imglib2.neighborsearch.RadiusNeighborSearch;
import net.imglib2.neighborsearch.RadiusNeighborSearchOnKDTree;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import parameters.RadialSymParams;

public class RadialSymmetry {
	public enum Ransac { NONE, SIMPLE, MULTICONSENSU };

	public static int bsNumIterations = 500; // not a parameter, can be changed
												// through Beanshell
	public static int numIterations = 250; // not a parameter, can be changed
											// through Beanshell

	ArrayList<Point> peaks;
	ArrayList<Spot> spots;
	Gradient derivative;
	NormalizedGradient ng;

	final RandomAccessible<FloatType> img;
	final RadialSymParams params;
	final Interval globalInterval, computeInterval;

	// set all parameters in the constructor
	public RadialSymmetry(
			final RandomAccessible<FloatType> img,
			final Interval globalInterval, // we need to know where to cut off gradients at image borders
			final Interval computeInterval,
			final RadialSymParams params) {
		this.img = img;
		this.params = params;
		this.globalInterval = globalInterval;
		this.computeInterval = computeInterval;
	}

	public void compute() {
		compute(this,img, globalInterval, computeInterval, params);
	}

	public static void compute(
			final RadialSymmetry rs,
			final RandomAccessible<FloatType> pImg,
			final Interval globalInterval, // we need to know where to cut off gradients at image borders
			final Interval computeInterval,
			final RadialSymParams p ) {

		// perform DOG

		HelperFunctions.log( "Computing DoG..." );

		rs.peaks = computeDog(pImg, computeInterval, p.sigma, p.threshold, p.anisotropyCoefficient, p.useAnisotropyForDoG, p.numThreads );

		HelperFunctions.log("DoG pre-detected spots: " + rs.peaks.size() );//+ ", " + numIterations  + ", " + pMaxError );

		// calculate (normalized) derivatives
		rs.derivative = new GradientOnDemand(pImg);
		rs.ng = calculateNormalizedGradient(rs.derivative, RadialSymParams.bsMethods[ p.bsMethod ], p.bsMaxError, p.bsInlierRatio);

		// CODE FOR DEBUGGING DoG OUTPUT
		// FOR Poiss_30spots_bg_200_1_I_300_0_img0.loc ONE SPOT IS DOUBLE-DETECTED:
		// 60.155194, 148.78923, 21.915013 >> 60.0, 148.0, 23.0 && 60.0, 148.0, 21.0
		//
		// ONE IS MISSED WHERE THINGS ARE CLOSE TO EACH OTHER:
		// 77.757591, 194.33074, 7.099615399999999 -- only found: 79.0, 193.0, 7.0 closest to 79.97478, 192.38965, 6.092944
		//List< mpicbg.models.Point > gt = HelperFunctions.toPoints(
				//LoadSpotFile.loadSpotsDouble( new File("/Users/spreibi/Documents/BIMSB/Publications/radialsymmetry/Poiss_30spots_bg_200_0_I_10000_0_img0.loc" ) ));
				//LoadSpotFile.loadSpotsDouble( new File( "/Users/spreibi/Documents/BIMSB/Publications/radialsymmetry/Poiss_30spots_bg_200_1_I_300_0_img0.loc") ) );
				//LoadSpotFile.loadSpotsDouble( new File( "/Users/spreibi/Documents/BIMSB/Publications/radialsymmetry/Poiss_300spots_bg_200_0_I_10000_0_img0.loc") ) );
		//for ( final mpicbg.models.Point p : gt )
		//	System.out.println( Util.printCoordinates( p.getL() ) );
		//System.out.println( HelperFunctions.analyzePoints( gt, HelperFunctions.toPointsLong( simplifiedPeaks ), false ) );

		HelperFunctions.log( "Computing Radial Symmetry..." );

		rs.spots = computeRadialSymmetry(
				globalInterval,
				rs.ng,
				rs.derivative,
				rs.peaks,
				Util.getArrayFromValue( p.supportRadius, pImg.numDimensions() ),
				p.inlierRatio,
				p.maxError,
				(float)p.anisotropyCoefficient,
				p.RANSAC(),
				p.minNumInliers,
				p.nTimesStDev1,
				p.nTimesStDev2 );

		/*
		long[] dim = new long[ 3 ];
		pImg.dimensions( dim );
		RandomAccessibleInterval<FloatType> out = ArrayImgs.floats(dim);
		RandomAccess<FloatType> r = out.randomAccess();
		Random rnd = new Random( 12 );

		for ( final Spot spot : rs.spots )
		{
			final float v = rnd.nextFloat() * 5 + 1;

			for ( final PointFunctionMatch f : spot.inliers )
			{
				r.setPosition( Math.round( f.getP1().getL()[ 0 ] ) , 0 );
				r.setPosition( Math.round( f.getP1().getL()[ 1 ] ) , 1 );
				r.setPosition( Math.round( f.getP1().getL()[ 2 ] ) , 2 );
				r.get().set( v );
			}
		}

		RandomAccessibleInterval<FloatType> gtImg = ArrayImgs.floats(dim);

		for ( final mpicbg.models.Point point : gt )
			TestGauss3d.addGaussian( gtImg, point.getL(), Util.getArrayFromValue( 1.0, gtImg.numDimensions() ) );

		ImageJFunctions.show( out );
		ImageJFunctions.show( gtImg );

		SimpleMultiThreading.threadHaltUnClean();
		*/

		if ( p.RANSAC().ordinal() > 0 )
		{
			for ( int i = rs.spots.size() - 1; i >= 0; --i )
				if ( rs.spots.get( i ).inliers.size() == 0 )
					rs.spots.remove( i );
	
			HelperFunctions.log( "#detections (after RANSAC): " + rs.spots.size() );
		}

		// TODO: trash the one with lower intensity
		HelperFunctions.log( "Filtering double-detections (dist < 0.5 px)");
		filterDoubleDetections( rs.spots, 0.5 );

		HelperFunctions.log( "Final #detections (before intensity check): " + rs.spots.size() );

		//System.out.println( HelperFunctions.analyzePoints( gt, HelperFunctions.toPointsSpot( rs.spots ), false ) );
	}

	public static void filterDoubleDetections( final ArrayList< Spot > spots, final double threshold )
	{
		if ( spots.size() < 2 )
			return;

		boolean removed = false;
		int countRemoved = 0;

		do
		{
			removed = false;
	
			final KDTree< Spot > tree = new KDTree<>(spots, spots );
			final RadiusNeighborSearch< Spot > search = new RadiusNeighborSearchOnKDTree<>( tree );
	
			for ( int s = 0; s < spots.size(); ++s )
			{
				final Spot spot = spots.get( s );
	
				search.search( spot, threshold, true );
	
				if ( search.numNeighbors() > 1 )
				{
					System.out.println( "Removing: " + Util.printCoordinates( spot.getOriginalLocation() ) );
					for ( int i = 1; i < search.numNeighbors(); ++i )
						System.out.println( search.getDistance( i ) );
	
					// we do an expensive greedy strategy
					// if two or more points are too close, we this one and start over
					spots.remove( s );
					++countRemoved;
					removed = true;
					break;
				}
			}

		} while ( removed );

		System.out.println( "Removed " + countRemoved + " points." );
	}

	public static ArrayList<Point> computeDog(final RandomAccessible<FloatType> pImg, final Interval interval, float pSigma,
			float pThreshold, final double anisotropy, final boolean useAnisotropy, final int numThreads ) {

		float pSigma2 = HelperFunctions.computeSigma2(pSigma, RadialSymParams.defaultSensitivity);

		double[] calibration = new double[ pImg.numDimensions() ];
		calibration[ 0 ] = 1.0;
		calibration[ 1 ] = 1.0;
		if ( calibration.length == 3 )
			calibration[ 2 ] = useAnisotropy ? (1.0/anisotropy) : 1.0;

		final DogDetection<FloatType> dog2 = new DogDetection<>(pImg, interval, calibration, pSigma, pSigma2,
				DogDetection.ExtremaType.MINIMA, pThreshold, false);

		dog2.setNumThreads(numThreads);

		return new ArrayList<>( dog2.getPeaks() );
	}

	public static ArrayList<Spot> computeRadialSymmetry(final Interval interval, NormalizedGradient pNg,
			Gradient pDerivative, ArrayList<Point> peaks, int[] pSupportRadius, float pInlierRatio,
			float pMaxError, float pAnisotropy, Ransac ransac, final int minNumInliers, final double nTimesStDev1, final double nTimesStDev2 ) {
		int numDimensions = interval.numDimensions();

		// the size of the RANSAC area
		final long[] range = new long[numDimensions];
		for (int d = 0; d < numDimensions; ++d)
			range[d] = pSupportRadius[d]*2;

		ArrayList<Spot> pSpots = Spot.extractSpotsPoints(interval, peaks, pDerivative, pNg, range);

		// scale the z-component according to the anisotropy coefficient
		if (numDimensions == 3)
			fixAnisotropy(pSpots, pAnisotropy);

		if (ransac != Ransac.NONE )
		{
			final ArrayList< Spot > additionalSpots = 
					Spot.ransac(
							pSpots,
							numIterations,
							pMaxError,
							pInlierRatio,
							minNumInliers,
							ransac == Ransac.MULTICONSENSU,
							nTimesStDev1,
							nTimesStDev2,
							interval,
							pDerivative,
							pNg,
							range );

			if ( ransac == Ransac.MULTICONSENSU )
				pSpots.addAll( additionalSpots );
		}
		else
		{
			/*
			for ( final Spot s : pSpots )
			{
				final SymmetryCenter3d c = new SymmetryCenter3d();
				c.xc = s.getOriginalLocation()[ 0 ];
				c.yc = s.getOriginalLocation()[ 1 ];
				c.zc = s.getOriginalLocation()[ 2 ];

				((SymmetryCenter3d)s.center).set(c);
			}
			*/

			try {
				Spot.fitCandidates(pSpots);
				/*
				for ( final Spot spot : pSpots )
				{
					//boolean lookAt = false;
					//if ( spot.getOriginalLocation()[ 0 ] == 230 && spot.getOriginalLocation()[ 1 ] == 229 && spot.getOriginalLocation()[ 2 ] == 55 )
					//	lookAt = true;

					//if ( spot.getOriginalLocation()[ 0 ] == 219 && spot.getOriginalLocation()[ 1 ] == 213 && spot.getOriginalLocation()[ 2 ] == 59 )
					//	lookAt = true;

					spot.center.fit( spot.candidates );

					if ( lookAt )
					{
						// SINGLE: original location: (219, 213, 59) localized as (219.09322, 212.95442, 58.740772)
						// SPARK:  original location: (219, 213, 59) localized as (219.17636, 212.91121, 58.17321)
						// spark: when using different block boundaries it localizes right (50, 50, 50) instead of (32, 32, 32)
						System.out.println( "original location: " + Util.printCoordinates( spot.getOriginalLocation() ) + " localized as " + Util.printCoordinates( spot ));
						//System.exit( 0 );
					}

					// original location: (230, 229, 55) for (229.86337, 228.90987, 54.77498)
					// no match for: (229.8634, 228.9099, 54.775), d=0.27821317366364656

					// original location: (219, 213, 59) for (219.09322, 212.95442, 58.740772)
					// no match for: (219.0932, 212.9544, 58.7408), d=0.5752897009333628
					if ( Math.abs( spot.getDoublePosition(2) - 58.7408 ) < 0.0001 )
					{
						System.out.println( "original location: " + Util.printCoordinates( spot.getOriginalLocation() ) + " for " + Util.printCoordinates( spot ));
					}
				}*/

			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Something went wrong, please report the bug.");
			}

		}

		return pSpots;
	}

	public static void fixAnisotropy(ArrayList<Spot> spots, float pAnisotropy) {
		for (int j = 0; j < spots.size(); j++)
			spots.get(j).updateScale(new float[] { 1, 1, pAnisotropy });
	}

	public static NormalizedGradient calculateNormalizedGradient(Gradient pDerivative, String pBsMethod,
			float pBsMaxError, float pBsInlierRatio) {
		final NormalizedGradient ng;
		// "No background subtraction", "Mean", "Median", "RANSAC on Mean",
		// "RANSAC on Median"
		if (pBsMethod.equals("No background subtraction"))
			ng = null;
		else if (pBsMethod.equals("Mean"))
			ng = new NormalizedGradientAverage(pDerivative);
		else if (pBsMethod.equals("Median"))
			ng = new NormalizedGradientMedian(pDerivative);
		else if (pBsMethod.equals("RANSAC on Mean"))
			ng = new NormalizedGradientRANSAC(pDerivative, CenterMethod.MEAN, pBsMaxError, pBsInlierRatio);
		else if (pBsMethod.equals("RANSAC on Median"))
			ng = new NormalizedGradientRANSAC(pDerivative, CenterMethod.MEDIAN, pBsMaxError, pBsInlierRatio);
		else
			throw new RuntimeException("Unknown bsMethod: " + pBsMethod);
		return ng;
	}

	// process each 2D/3D slice of the image to search for the spots
	public static void process(
			RandomAccessible<FloatType> img,
			RandomAccessible<FloatType> rai,
			final Interval globalInterval, // we need to know where to cut off gradients at image borders
			final Interval computeInterval,
			RadialSymParams rsm,
			int[] impDim,
			ArrayList<Spot> allSpots,
			ArrayList<Long> timePoint,
			ArrayList<Long> channelPoint) {
		RandomAccessible<FloatType> timeFrameNormalized;
		RandomAccessible<FloatType> timeFrame;

		// impDim <- x y c z t
		for (int c = 0; c < impDim[2]; c++) {
			for (int t = 0; t < impDim[4]; t++) {
				// grab xy(z) part of the image
				timeFrameNormalized = HelperFunctions.reduceImg(rai, c, t, impDim);
				timeFrame 			= HelperFunctions.reduceImg(img, c, t, impDim);

				// TODO: finish double-points
				RadialSymmetry rs = new RadialSymmetry(timeFrameNormalized, globalInterval, computeInterval, rsm);
				rs.compute();

				int minNumInliers = rsm.ransacSelection == 0 ? 0 : 1; // TODO: horrible!
				ArrayList<Spot> filteredSpots = HelperFunctions.filterSpots(rs.getSpots(), minNumInliers);

				allSpots.addAll(filteredSpots);
				timePoint.add(new Long(filteredSpots.size()));

				if (rsm.getGaussFit()) {
					// WARNING: This does a full gaussian fit
					Intensity.calulateIntesitiesGF(
							timeFrame,
							timeFrame.numDimensions(),
							rsm.getAnisotropyCoefficient(),
							rsm.getSigmaDoG(),
							filteredSpots,
							rsm.supportRadius );
				} else {// iterate over all points and perform the linear
						// interpolation for each of the spots
					Intensity.calculateIntensitiesLinear(timeFrame, filteredSpots);
				}

				// FIXME: make this a parameter, not doing this by default, will crash on 2d
				if ( false )
					Intensity.fixIntensities(filteredSpots);
			}
			if (c != 0) // FIXME: formula is wrong
				channelPoint.add(new Long(allSpots.size() - channelPoint.get(c - 1)));
			else
				channelPoint.add(new Long(allSpots.size()));
		}

	}

	// getters
	public ArrayList<Point> getPeaks() {
		return peaks;
	}

	public ArrayList<Spot> getSpots() {
		return spots;
	}

	public Gradient getDerivative() {
		return derivative;
	}

	public NormalizedGradient getNormalizedGradient() {
		return ng;
	}
}

package compute;

import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import background.NormalizedGradient;
import background.NormalizedGradientAverage;
import background.NormalizedGradientMedian;
import background.NormalizedGradientRANSAC;
import benchmark.LoadSpotFile;
import fitting.Center.CenterMethod;
import fitting.PointFunctionMatch;
import fitting.Spot;
import gradient.Gradient;
import gradient.GradientPreCompute;
import gui.interactive.HelperFunctions;
import ij.IJ;
import intensity.Intensity;
import net.imglib2.KDTree;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.dog.DogDetection;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.neighborsearch.RadiusNeighborSearch;
import net.imglib2.neighborsearch.RadiusNeighborSearchOnKDTree;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import parameters.RadialSymParams;
import test.TestGauss3d;

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

	RandomAccessibleInterval<FloatType> img;
	RadialSymParams params;

	// set all parameters in the constructor
	public RadialSymmetry(final RandomAccessibleInterval<FloatType> img, final RadialSymParams params) {
		this.img = img;
		this.params = params;
	}

	public void compute() {
		compute(this,img, params);
	}

	public static void compute(
			final RadialSymmetry rs,
			final RandomAccessibleInterval<FloatType> pImg, 
			final RadialSymParams p ) {

		// perform DOG
		rs.peaks = computeDog(pImg, p.sigma, p.threshold, p.anisotropyCoefficient, p.useAnisotropyForDoG );

		// calculate (normalized) derivatives
		rs.derivative = new GradientPreCompute(pImg);
		rs.ng = calculateNormalizedGradient(rs.derivative, RadialSymParams.bsMethods[ p.bsMethod ], p.bsMaxError, p.bsInlierRatio);

		// use light weighted structure for the radial symmetry computations
		final ArrayList<long[]> simplifiedPeaks = new ArrayList<>();
		Rectangle rectangle = new Rectangle(0, 0, (int) pImg.dimension(0), (int) pImg.dimension(1));
		HelperFunctions.copySimplePeaks(rs.peaks, simplifiedPeaks, rectangle );

		// CODE FOR DEBUGGING DoG OUTPUT
		// FOR Poiss_30spots_bg_200_1_I_300_0_img0.loc ONE SPOT IS DOUBLE-DETECTED:
		// 60.155194, 148.78923, 21.915013 >> 60.0, 148.0, 23.0 && 60.0, 148.0, 21.0
		//
		// ONE IS MISSED WHERE THINGS ARE CLOSE TO EACH OTHER:
		// 77.757591, 194.33074, 7.099615399999999 -- only found: 79.0, 193.0, 7.0 closest to 79.97478, 192.38965, 6.092944
		List< mpicbg.models.Point > gt = HelperFunctions.toPoints(
				//LoadSpotFile.loadSpotsDouble( new File("/Users/spreibi/Documents/BIMSB/Publications/radialsymmetry/Poiss_30spots_bg_200_0_I_10000_0_img0.loc" ) ));
				LoadSpotFile.loadSpotsDouble( new File( "/Users/spreibi/Documents/BIMSB/Publications/radialsymmetry/Poiss_30spots_bg_200_1_I_300_0_img0.loc") ) );
				//LoadSpotFile.loadSpotsDouble( new File( "/Users/spreibi/Documents/BIMSB/Publications/radialsymmetry/Poiss_300spots_bg_200_0_I_10000_0_img0.loc") ) );

		//for ( final mpicbg.models.Point p : gt )
		//	System.out.println( Util.printCoordinates( p.getL() ) );

		//System.out.println( HelperFunctions.analyzePoints( gt, HelperFunctions.toPointsLong( simplifiedPeaks ), false ) );

		//p.maxError = 1.5f;
		//p.supportRadius = 3;

		rs.spots = computeRadialSymmetry(
				pImg,
				rs.ng,
				rs.derivative,
				simplifiedPeaks,
				Util.getArrayFromValue( p.supportRadius, pImg.numDimensions() ),
				p.inlierRatio,
				p.maxError,
				(float)p.anisotropyCoefficient,
				p.RANSAC,
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
		*/

		if ( p.RANSAC.ordinal() > 0 )
		{
			for ( int i = rs.spots.size() - 1; i >= 0; --i )
				if ( rs.spots.get( i ).inliers.size() == 0 )
					rs.spots.remove( i );
	
			IJ.log( "#detections (after RANSAC): " + rs.spots.size() );
		}

		IJ.log( "Filtering double-detections (dist < 0.5 px)");
		filterDoubleDetections( rs.spots, 0.5 );

		IJ.log( "Final #detections (before intensity check): " + rs.spots.size() );

		System.out.println( HelperFunctions.analyzePoints( gt, HelperFunctions.toPointsSpot( rs.spots ), false ) );

		//SimpleMultiThreading.threadHaltUnClean();
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

	public static ArrayList<Point> computeDog(final RandomAccessibleInterval<FloatType> pImg, float pSigma,
			float pThreshold, final double anisotropy, final boolean useAnisotropy ) {

		float pSigma2 = HelperFunctions.computeSigma2(pSigma, RadialSymParams.defaultSensitivity);

		double[] calibration = new double[ pImg.numDimensions() ];
		calibration[ 0 ] = 1.0;
		calibration[ 1 ] = 1.0;
		if ( calibration.length == 3 )
			calibration[ 2 ] = useAnisotropy ? (1.0/anisotropy) : 1.0;

		final DogDetection<FloatType> dog2 = new DogDetection<>(pImg, calibration, pSigma, pSigma2,
				DogDetection.ExtremaType.MINIMA, pThreshold, false);

		return dog2.getPeaks();
	}

	public static ArrayList<Spot> computeRadialSymmetry(final RandomAccessibleInterval<FloatType> pImg, NormalizedGradient pNg,
			Gradient pDerivative, ArrayList<long[]> simplifiedPeaks, int[] pSupportRadius, float pInlierRatio,
			float pMaxError, float pAnisotropy, Ransac ransac, final int minNumInliers, final double nTimesStDev1, final double nTimesStDev2 ) {
		int numDimensions = pImg.numDimensions();

		// the size of the RANSAC area
		final long[] range = new long[numDimensions];
		for (int d = 0; d < numDimensions; ++d)
			range[d] = pSupportRadius[d]*2;

		ArrayList<Spot> pSpots = Spot.extractSpots(pImg, simplifiedPeaks, pDerivative, pNg, range);

		// scale the z-component according to the anisotropy coefficient
		if (numDimensions == 3)
			fixAnisotropy(pSpots, pAnisotropy);

		IJ.log("DoG pre-detected spots: " + pSpots.size() + ", " + numIterations  + ", " + pMaxError );

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
							pImg,
							pDerivative,
							pNg,
							range );

			if ( ransac == Ransac.MULTICONSENSU )
				pSpots.addAll( additionalSpots );
		}
		else
			try {
				Spot.fitCandidates(pSpots);
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Something went wrong, please report the bug.");
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
	public static void processSliceBySlice(
			RandomAccessibleInterval<FloatType> img,
			RandomAccessibleInterval<FloatType> rai,
			RadialSymParams rsm,
			int[] impDim,
			ArrayList<Spot> allSpots,
			ArrayList<Long> timePoint,
			ArrayList<Long> channelPoint) {
		RandomAccessibleInterval<FloatType> timeFrameNormalized;
		RandomAccessibleInterval<FloatType> timeFrame;

		// impDim <- x y c z t
		for (int c = 0; c < impDim[2]; c++) {
			for (int t = 0; t < impDim[4]; t++) {
				// grab xy(z) part of the image
				timeFrameNormalized = HelperFunctions.copyImg(rai, c, t, impDim);
				timeFrame 			= HelperFunctions.copyImg(img, c, t, impDim);

				// TODO: finish double-points
				RadialSymmetry rs = new RadialSymmetry(timeFrameNormalized, rsm);
				rs.compute();

				int minNumInliers = 1;
				ArrayList<Spot> filteredSpots = HelperFunctions.filterSpots(rs.getSpots(), minNumInliers);
				allSpots.addAll(filteredSpots);
				timePoint.add(new Long(filteredSpots.size()));

				if (rsm.getGaussFit()) {
					// FIXME: fix the problem with the computations of this one
					// WARNING: This does a full gaussian fit, let's just not do this!
					Intensity.calulateIntesitiesGF(
							timeFrame,
							timeFrame.numDimensions(),
							rsm.getAnisotropyCoefficient(),
							rsm.getSigmaDoG(),
							filteredSpots);
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

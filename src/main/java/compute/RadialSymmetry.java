package compute;

import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

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
import net.imglib2.RealPoint;
import net.imglib2.algorithm.dog.DogDetection;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.neighborsearch.RadiusNeighborSearch;
import net.imglib2.neighborsearch.RadiusNeighborSearchOnKDTree;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Pair;
import net.imglib2.util.Util;
import net.imglib2.util.ValuePair;
import parameters.RadialSymmetryParameters;

public class RadialSymmetry {
	public static int bsNumIterations = 100; // not a parameter, can be changed
												// through Beanshell
	public static int numIterations = 500; // not a parameter, can be changed
											// through Beanshell
	// steps per octave for DoG
	private static int defaultSensitivity = 4;

	ArrayList<Point> peaks;
	ArrayList<Spot> spots;
	Gradient derivative;
	NormalizedGradient ng;

	// private static final boolean debug = true;
	// private static final long timingScale = 1000; // will result in seconds
	// DOG:
	float sigma;
	float threshold;
	// RANSAC:
	boolean ransac;
	int supportRadius;
	float inlierRatio;
	float maxError;
	// Background subtraction:
	String bsMethod;
	float bsMaxError;
	float bsInlierRatio;
	// General image params:
	RandomAccessibleInterval<FloatType> img;
	float anisotropy;
	double[] calibration;

	// set all parameters in the constructor
	public RadialSymmetry(final RandomAccessibleInterval<FloatType> img, final RadialSymmetryParameters params) {
		this.img = img;
		sigma = params.getParams().getSigmaDoG();
		threshold = params.getParams().getThresholdDoG();
		supportRadius = params.getParams().getSupportRadius();
		inlierRatio = params.getParams().getInlierRatio();
		maxError = params.getParams().getMaxError();
		bsMethod = params.getParams().getBsMethod();
		bsMaxError = params.getParams().getBsMaxError();
		bsInlierRatio = params.getParams().getBsInlierRatio();
		anisotropy = params.getParams().getAnisotropyCoefficient();
		ransac = params.getParams().getRANSAC();
		calibration = params.getCalibration();
	}

	public void compute() {
		compute(img, sigma, threshold, supportRadius, inlierRatio, maxError, bsMethod, bsMaxError, bsInlierRatio,
				anisotropy, ransac);
	}

	public void compute(final RandomAccessibleInterval<FloatType> pImg, float pSigma, float pThreshold,
			int pSupportRadius, float pInlierRatio, float pMaxError, String pBsMethod, float pBsMaxError,
			float pBsInlierRatio, float pAnisotropy, boolean pRansac) {

		// perform DOG
		peaks = computeDog(pImg, pSigma, pThreshold);

		// calculate (normalized) derivatives
		derivative = new GradientPreCompute(pImg);
		ng = calculateNormalizedGradient(derivative, pBsMethod, pBsMaxError, pBsInlierRatio);

		// use light weighted structure for the radial symmetry computations
		final ArrayList<long[]> simplifiedPeaks = new ArrayList<>();
		Rectangle rectangle = new Rectangle(0, 0, (int) pImg.dimension(0), (int) pImg.dimension(1));
		HelperFunctions.copySimplePeaks(peaks, simplifiedPeaks, rectangle );

		// CODE FOR DEBUGGING DoG OUTPUT
		// FOR Poiss_30spots_bg_200_1_I_300_0_img0.loc ONE SPOT IS DOUBLE-DETECTED:
		// 60.155194, 148.78923, 21.915013 >> 60.0, 148.0, 23.0 && 60.0, 148.0, 21.0
		//
		// ONE IS MISSED WHERE THINGS ARE CLOSE TO EACH OTHER:
		// 78.257591, 194.83074, 7.099615399999999 -- only found: 79.0, 193.0, 7.0 closest to 80.47478, 192.88965, 6.092944
		List< mpicbg.models.Point > gt = HelperFunctions.toPoints(
				//LoadSpotFile.loadSpotsDouble( new File("/Users/spreibi/Documents/BIMSB/Publications/radialsymmetry/Poiss_30spots_bg_200_0_I_10000_0_img0.loc" ) ));
				LoadSpotFile.loadSpotsDouble( new File( "/Users/spreibi/Documents/BIMSB/Publications/radialsymmetry/Poiss_30spots_bg_200_1_I_300_0_img0.loc") ) );

		//for ( final mpicbg.models.Point p : gt )
		//	System.out.println( Util.printCoordinates( p.getL() ) );

		System.out.println( HelperFunctions.analyzePoints( gt, HelperFunctions.toPointsLong( simplifiedPeaks ), false ) );

		pMaxError = 1.5f;
		pSupportRadius = 3;

		System.out.println( "" + pInlierRatio );

		// 31.140394, 60.66735, 5.0560045
		// 33.188287, 69.688289, 5.3686692

		// (60, 148, 21)
		// 77 --(59.76829853217439, 148.32047163604776, 21.981953154839633)

		// (60, 148, 23)
		// 76 --(59.7442836442797, 148.26202274101857, 22.364260862115284)
		
		// trigger radial symmetry
		//for ( pAnisotropy = 0.5f; pAnisotropy <= 2f; pAnisotropy += 0.05f )
		{
			pAnisotropy = 0.675f; //1.0f / 1.481481481481481f;

			pRansac = true;
			int[] supportRadius = new int[ pImg.numDimensions() ];
			supportRadius[ 0 ] = supportRadius[ 1 ] = pSupportRadius;
			supportRadius[ 2 ] = pSupportRadius - 1;

			spots = computeRadialSymmetry(pImg, ng, derivative, simplifiedPeaks, supportRadius, pInlierRatio, pMaxError,
				pAnisotropy, pRansac, false );

			long[] dim = new long[ 3 ];
			pImg.dimensions( dim );
			RandomAccessibleInterval<FloatType> out = ArrayImgs.floats(dim);
			RandomAccess<FloatType> r = out.randomAccess();

			int sum = 0;
			for ( final Spot spot : spots )
			{
				sum += spot.inliers.size();
				for ( final PointFunctionMatch f : spot.inliers )
				{
					r.setPosition( Math.round( f.getP1().getL()[ 0 ] ) , 0 );
					r.setPosition( Math.round( f.getP1().getL()[ 1 ] ) , 1 );
					r.setPosition( Math.round( f.getP1().getL()[ 2 ] ) , 2 );
					r.get().set( 1 );
				}
			}

			ImageJFunctions.show( out );

			System.out.print( sum + " -- ");
			System.out.println( pAnisotropy + ": " + HelperFunctions.analyzePoints( gt, HelperFunctions.toPointsSpot( spots ), true ) );

			filterDoubleDetections( spots, 0.5 );
		}
	}

	public static void filterDoubleDetections( final ArrayList< Spot > spots, final double threshold )
	{
		final KDTree< Spot > tree = new KDTree<>(spots, spots );
		final RadiusNeighborSearch< Spot > search = new RadiusNeighborSearchOnKDTree<>( tree );

		final ArrayList< Pair< Spot, Spot > > tooClose = new ArrayList<>();
		final HashMap< Spot, Integer > tooCloseNeighbors = new HashMap<>();
	
		for ( int s = 0; s < spots.size(); ++s )
		{
			final Spot spot = spots.get( s );

			search.search( spot, threshold, true );

			if ( search.numNeighbors() > 1 )
			{
				System.out.println( Util.printCoordinates( spot.getOriginalLocation() ) );
				for ( int i = 1; i < search.numNeighbors(); ++i )
				{
					System.out.println( search.getDistance( i ) );
					tooClose.add( new ValuePair<Spot, Spot>( spot, search.getSampler( i ).get() ) );

					if ( !tooCloseNeighbors.containsKey( spot ) )
						tooCloseNeighbors.put( spot, 1 );
					else
						tooCloseNeighbors.put( spot, tooCloseNeighbors.get( spot ) + 1 );

					if ( !tooCloseNeighbors.containsKey( search.getSampler( i ).get() ) )
						tooCloseNeighbors.put( search.getSampler( i ).get(), 1 );
					else
						tooCloseNeighbors.put( search.getSampler( i ).get(), tooCloseNeighbors.get( search.getSampler( i ).get() ) + 1 );
				}
			}
		}

		// TODO: REMOVE

	}

	public ArrayList<Point> computeDog(final RandomAccessibleInterval<FloatType> pImg, float pSigma,
			float pThreshold) {
		float pSigma2 = HelperFunctions.computeSigma2(pSigma, defaultSensitivity);

		final DogDetection<FloatType> dog2 = new DogDetection<>(pImg, calibration, pSigma, pSigma2,
				DogDetection.ExtremaType.MINIMA, pThreshold, false);

		return dog2.getPeaks();

		//ArrayList<RefinedPeak<Point>> pPeaks = dog2.getSubpixelPeaks();
		//return pPeaks;
	}

	public ArrayList<Spot> computeRadialSymmetry(final RandomAccessibleInterval<FloatType> pImg, NormalizedGradient pNg,
			Gradient pDerivative, ArrayList<long[]> simplifiedPeaks, int[] pSupportRadius, float pInlierRatio,
			float pMaxError, float pAnisotropy, boolean useRansac, final boolean multiConsenus) {
		int numDimensions = pImg.numDimensions();

		// the size of the RANSAC area
		final long[] range = new long[numDimensions];
		for (int d = 0; d < numDimensions; ++d)
			range[d] = pSupportRadius[d]*2;

		ArrayList<Spot> pSpots = Spot.extractSpots(pImg, simplifiedPeaks, pDerivative, pNg, range);
		// scale the z-component according to the anisotropy coefficient
		if (numDimensions == 3)
			fixAnisotropy(pSpots, pAnisotropy);

		IJ.log("DoG pre-detected spots: " + pSpots.size());

		if (useRansac)
			Spot.ransac(pSpots, numIterations, pMaxError, pInlierRatio, multiConsenus);
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
			RadialSymmetryParameters rsm,
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

				if (rsm.getParams().getGaussFit()) {
					// FIXME: fix the problem with the computations of this one
					// WARNING: This does a full gaussian fit, let's just not do this!
					Intensity.calulateIntesitiesGF(timeFrame, timeFrame.numDimensions(), rsm.getParams().getAnisotropyCoefficient(),
						rsm.getParams().getSigmaDoG(), filteredSpots);
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

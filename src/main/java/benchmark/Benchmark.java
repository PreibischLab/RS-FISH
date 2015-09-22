package benchmark;

import ij.ImageJ;

import java.io.File;
import java.util.ArrayList;

import localmaxima.LocalMaxima;
import localmaxima.LocalMaximaDoG;
import localmaxima.LocalMaximaSmoothNeighborhood;
import mpicbg.imglib.algorithm.math.MathLib;
import fit.OrientedPoint;
import fit.PointFunctionMatch;
import fit.Spot;
import gauss.GaussFit;
import gauss.GaussianMaskFit;
import gradient.Gradient;
import gradient.GradientPreCompute;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.KDTree;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.neighborsearch.NearestNeighborSearchOnKDTree;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class Benchmark 
{
	final String dir, file;
	final String imageFile = ".tif";
	final String textFile = ".loc";
	final String matlabFile = ".loc3";

	// the ground truth
	final ArrayList< GroundTruthPoint > groundtruth;
	
	// the initial detections
	ArrayList< int[] > peaks;
	
	// the spots after ransac
	ArrayList< Spot > spots, goodspots;
	
	// the image
	final Img< FloatType > img;
	final FloatType min, max;
	
	// size around the detection to use
	// we detect at 0.5, 0.5, 0.5 - so we need an even size
	final int[] range = new int[]{ 10, 10, 10 };
			
	public Benchmark( final String dir, final String file ) throws ImgIOException
	{
		this.dir = dir;
		this.file = file;

		this.groundtruth = LoadSpotFile.loadSpots( new File( dir + file + textFile ) );
		this.img = new ImgOpener().openImg( dir + file + imageFile, new ArrayImgFactory< FloatType >(), new FloatType() ).getImg();
		this.min = img.firstElement().createVariable();
		this.max = min.createVariable();
		
		norm( img, min, max );
		
		new ImageJ();
		ImageJFunctions.show( img );
		
		//for ( final GroundTruthPoint p : groundtruth )
		//	System.out.println( p );
	}
	
	protected void norm( final Img< FloatType > img, final FloatType min, final FloatType max )
	{
		min.set( img.firstElement().get() );
		max.set( min );
		
		for ( final FloatType t : img )
		{
			if ( t.get() > max.get() )
				max.set( t.get() );
			
			if ( t.get() < min.get() )
				min.set( t.get() );
		}
		
		for ( final FloatType t : img )
			t.set( ( t.get() - min.get() ) / ( max.get() - min.get() ) );
	}
	
	public int findPeaks()
	{
		// we need an initial candidate search	
		final LocalMaxima candiateSearch;
		//candiateSearch = new LocalMaximaDoG( img, 0.7, 1.2, 0.1 );
		//candiateSearch = new LocalMaximaNeighborhood( img );
		candiateSearch = new LocalMaximaSmoothNeighborhood( img, new double[]{ 1, 1, 1 }, 0.5 );
		//candiateSearch = new LocalMaximaAll( img );
		
		//ImageJFunctions.show( candiateSearch.getSource() );
		
		peaks = candiateSearch.estimateLocalMaxima();
		
		return peaks.size();
	}
	
	public void matlabSpots()
	{
		this.goodspots = LoadSpotFile.loadSpots2( new File( dir + file + matlabFile ) );	
		System.out.print( goodspots.size() + "\t" );
	}
	
	public void gaussfit()
	{
		final GaussFit f = new GaussFit( img );
		final int n = img.numDimensions();
		goodspots = new ArrayList<Spot>();
		
		for ( final int[] p : peaks )
		{
			final float[] loc = f.fit( p );
			
			if ( loc != null )
			{
				final Spot s = new Spot( n );
				
				for ( int d = 0; d < n; ++d )
					s.center.setSymmetryCenter( loc[ d ], d );
				
				goodspots.add( s );
			}
		}
		
		System.out.print( goodspots.size() + "\t" );
		
	}
	
	public void getRangeForFit( final long[] min, final long[] max, final long[] size, final int[] p )
	{
		for ( int d = 0; d < p.length; ++d )
		{
			min[ d ] = p[ d ] - range[ d ]/2;
			max[ d ] = p[ d ] + range[ d ]/2;
			
			if ( size != null )
				size[ d ] = max[ d ] - min[ d ] + 1;
		}		
	}
	
	public void gaussianMaskFit() { gaussianMaskFit( null ); }
	
	public void gaussianMaskFit( final ArrayList< Img< FloatType > > ransacWeights )
	{
		final int n = img.numDimensions();
		goodspots = new ArrayList<Spot>();
		
		final long[] min = new long[ n ];
		final long[] max = new long[ n ];
		final double[] loc = new double[ n ];
		final double[] sigma = new double[]{ 2, 2, 2 };
		
		int i = 0;
		
		for ( final int[] p : peaks )
		{
			getRangeForFit( min, max, null, p );

			for ( int d = 0; d < n; ++d )
				loc[ d ] = p[ d ];
		
			if ( ransacWeights == null )
				GaussianMaskFit.gaussianMaskFit( Views.interval( img, min, max ), loc, sigma, null );
			else
				GaussianMaskFit.gaussianMaskFit( Views.interval( img, min, max ), loc, sigma, ransacWeights.get( i ) );
			
			final Spot s = new Spot( n );
			
			for ( int d = 0; d < n; ++d )
				s.center.setSymmetryCenter( loc[ d ], d );
			
			goodspots.add( s );
			
			i++;
		}
		
		System.out.print( goodspots.size() + "\t" );
	}
	
	protected ArrayList< Spot > extractSpotsRANSAC( final double ransacError )
	{
		// we need something to compute the derivatives
		final Gradient derivative;
		
		//derivative = new DerivativeOnDemand( image );
		derivative = new GradientPreCompute( img );
		
		final ArrayList< Spot > spots = Spot.extractSpots( img, peaks, derivative, range );
		
		//GradientDescent.testGradientDescent( spots, new boolean[]{ false, false, true } );
		
		// ransac on all spots
		Spot.ransac( spots, 1000, ransacError, 1.0/100.0 );
		
		return spots;
	}
	
	public ArrayList< Img< FloatType > > extractMasksByRANSAC( final double ransacError )
	{
		this.spots = extractSpotsRANSAC( ransacError );
		
		final int n = img.numDimensions();
		final ArrayList< Img< FloatType > > masks = new ArrayList<Img<FloatType>>();
		final long[] min = new long[ n ];
		final long[] max = new long[ n ];
		final long[] size = new long[ n ];
		final long[] tmp = new long[ n ];
		final FloatType one = new FloatType( 1 );
		
		// to display all weights of the current image as sum for debug (might overlap)
		//final Img< FloatType > tmpImg = img.factory().create( img, img.firstElement() );
		
		for ( final Spot spot : spots )
		{
			spot.computeAverageCostInliers();
			
			if ( spot.inliers.size() > spot.center.getMinNumPoints() * 3 )
			{
				// get the center location of the image used to compute the radial symmetry
				final int[] p = spot.getOriginalLocation();
				
				//System.out.println( "p: " + Util.printCoordinates( p ) );

				// set the range we will use for the fit
				getRangeForFit( min, max, size, p );

				//System.out.println( "min: " + Util.printCoordinates( min ) );
				//System.out.println( "max: " + Util.printCoordinates( max ) );
				//System.out.println( "size: " + Util.printCoordinates( size ) );

				// create the mask image
				final Img< FloatType > ransacMask = new ArrayImgFactory< FloatType >().create( size, img.firstElement() );
				
				// set the mask image to the same location as the interval we fit on and make it iterable
				final RandomAccessibleInterval<FloatType> translatedMask = Views.translate( ransacMask, min );				
				final RandomAccess< FloatType > r = Views.extendZero( translatedMask ).randomAccess();
				
				//final RandomAccess< FloatType > r = tmpImg.randomAccess();
				
				//System.out.println( "inliers: " + spot.inliers.size() );

				// add weight for every pixel involved in the inliers
				for ( final PointFunctionMatch pfm : spot.inliers )
				{
					final OrientedPoint op = (OrientedPoint)pfm.getP1();
					
					// get the top left front location
					for ( int d = 0; d < n; ++d )
						tmp[ d ] = Math.round( op.getL()[ d ] - 0.5 );
					
					//System.out.println( "op: " + Util.printCoordinates( op.getL() ) );
					//System.out.println( "round(op): " + Util.printCoordinates( tmp ) );
					
					r.setPosition( tmp );
					r.get().add( one );
					r.fwd( 0 );
					r.get().add( one );
					
					if ( n > 1 )
					{
						r.fwd( 1 );
						r.get().add( one );
						r.bck( 0 );
						r.get().add( one );
						
						if ( n == 3 )
						{
							r.fwd( 2 );
							r.get().add( one );
							r.fwd( 0 );
							r.get().add( one );
							r.bck( 1 );
							r.get().add( one );
							r.bck( 0 );							
							r.get().add( one );
						}
					}					
				}

				// copy and show the spot itself
				/*
				final Img< FloatType > tmpImg = new ArrayImgFactory< FloatType >().create( size, img.firstElement() );
				final RandomAccessibleInterval<FloatType> translatedTmpImg = Views.translate( tmpImg, min );
				final RandomAccess< FloatType > rImg = Views.extendZero( img ).randomAccess();
				final Cursor< FloatType > cTmpImg = Views.iterable( translatedTmpImg ).localizingCursor();

				while ( cTmpImg.hasNext() )
				{
					cTmpImg.fwd();
					rImg.setPosition( cTmpImg );
					cTmpImg.get().set( rImg.get() );
				}
				
				ImageJFunctions.show( translatedTmpImg ).setTitle( "img_spot" );
				
				
				ImageJFunctions.show( translatedMask ).setTitle( "ransac_mask" );
				SimpleMultiThreading.threadHaltUnClean();
				*/
				masks.add( ransacMask );
			}
			else
			{
				masks.add( null );
			}
		}
		
		//ImageJFunctions.show( tmpImg ).setTitle( "img_spot" );
		//SimpleMultiThreading.threadHaltUnClean();
		
		return masks;
	}
	
	public void fitPointsRANSAC( final double ransacError )
	{
		this.spots = extractSpotsRANSAC( ransacError );
		
		// some statistics on inliers
		int minNumInliers = Integer.MAX_VALUE;
		int maxNumInliers = Integer.MIN_VALUE;
		double avgNumInliers = 0;
		
		double minInlierRatio = 1;
		double maxInlierRatio = 0;
		double avgInlierRatio = 0;
		
		goodspots = new ArrayList<Spot>();
		
		for ( final Spot spot : spots )
		{
			spot.computeAverageCostInliers();
			
			if ( spot.inliers.size() > spot.center.getMinNumPoints() * 3 ) //&& dist( spot.getCenter(), spot.getOriginalLocation() ) < 0.7 )
			{
				//System.out.println( spot + " " + spot.inliers.size() + " " + spot.candidates.size() );
				goodspots.add( spot );
				
				final double inlierRatio = (double)spot.inliers.size() / (double)spot.candidates.size();
				
				minInlierRatio = Math.min( minInlierRatio, inlierRatio );
				maxInlierRatio = Math.max( maxInlierRatio, inlierRatio );
				avgInlierRatio += inlierRatio;
				
				minNumInliers = Math.min( minNumInliers, spot.inliers.size() );
				maxNumInliers = Math.max( maxNumInliers, spot.inliers.size() );
				avgNumInliers += spot.inliers.size();
			}
		}
		
		if ( goodspots.size() == 0 )
		{
			avgNumInliers = minNumInliers = maxNumInliers = 0;
			avgInlierRatio = minInlierRatio = maxInlierRatio = 0;
		}
		else
		{
			avgNumInliers /= (double)goodspots.size();
			avgInlierRatio /= (double)goodspots.size();
		}
		
		System.out.print( ransacError + "\t" + goodspots.size() + "\t" + avgInlierRatio + "\t" + minInlierRatio + "\t" + maxInlierRatio + "\t" );
		
		//Img< FloatType > ransacarea = img.factory().create( img, img.firstElement() );
		//Spot.drawRANSACArea( goodspots, ransacarea );
		//ImageJFunctions.show( ransacarea );
	}
	
	public void analyzePoints()
	{
		if ( goodspots.size() == 0 )
		{
			System.out.println( "0\t0\t0"  );
			return;
		}
		
		// compare the found maxima to the known list
		final KDTree< Spot > kdTreeSpots = new KDTree<Spot>( goodspots, goodspots );
		final NearestNeighborSearchOnKDTree< Spot > searchSpot = new NearestNeighborSearchOnKDTree<Spot>( kdTreeSpots );
		
		final KDTree< GroundTruthPoint > kdTreeGroundTruth = new KDTree< GroundTruthPoint >( groundtruth, groundtruth );
		final NearestNeighborSearchOnKDTree< GroundTruthPoint > searchGroundTruth = new NearestNeighborSearchOnKDTree< GroundTruthPoint >( kdTreeGroundTruth );

		// search spots in ground truth
		for ( final GroundTruthPoint p : groundtruth )
			p.setNumAssigned( 0 );
	
		double avgError = 0;
		double minError = Double.MAX_VALUE;
		double maxError = -Double.MAX_VALUE;
		final double[] median = new double[ goodspots.size() ];
		
		int i = 0;
		for ( final Spot spot : goodspots )
		{
			searchGroundTruth.search( spot );
			final GroundTruthPoint p = searchGroundTruth.getSampler().get();
			
			final double dist = dist( p, spot );
			avgError += dist;
			median[ i++ ] = dist;
			minError = Math.min( minError, dist );
			maxError = Math.max( maxError, dist );
			//System.out.println( dist );
		}
		
		avgError /= (double)goodspots.size();
		double stdev = 0;
		
		for ( i = 0; i < median.length; ++i )
			stdev += ( median[ i ] - avgError ) * ( median[ i ] - avgError ); 

		stdev /= (double)goodspots.size();
		stdev = Math.sqrt( stdev );
		
		double medianError = mpicbg.imglib.util.Util.computeMedian( median );
		
		System.out.println( avgError + "\t" + stdev + "\t" + medianError + "\t" + minError + "\t" + maxError );
		
	}
	
	public static double dist( final RealLocalizable p1, final RealLocalizable p2 )
	{
		double dist = 0;
		
		for ( int d = 0; d < p1.numDimensions(); ++d )
		{
			final double t = p1.getDoublePosition( d ) - p2.getDoublePosition( d );
			dist += t*t;
		}
		
		return Math.sqrt( dist );
	}

	/**
	 * @param args
	 * @throws ImgIOException 
	 */
	public static void main(String[] args) throws ImgIOException 
	{
		//final String dir = "documents/Images For Stephan/Tests/";
		final String dir = "documents/Images For Stephan/Empty Bg Density Range Sigxy 2 SigZ 2/";
		//final String dir = "documents/Images For Stephan/Infinite SNR Density Range Sigxy 1pt35 SigZ 2/";		
		final String file = "Poiss_30spots_bg_200_2_I_300_0_img1";
		
		final Benchmark b = new Benchmark( dir, file );
		
		System.out.println( "peaks: " + b.findPeaks() );
		
		b.gaussianMaskFit( null );
		b.analyzePoints();
		//SimpleMultiThreading.threadHaltUnClean();
		System.out.println();
		
		for ( double error = 0.0625/2; error < 65; error = error * Math.sqrt( Math.sqrt( Math.sqrt( 2 ) ) ) )
		{
			System.out.print( error + "\t" );
			
			ArrayList< Img< FloatType > > masks = b.extractMasksByRANSAC( error );
			b.gaussianMaskFit( masks );
			b.analyzePoints();
		}
		SimpleMultiThreading.threadHaltUnClean();
		
		// analyze Tim's matlab results
		//b.matlabSpots();
		//b.analyzePoints();
		//SimpleMultiThreading.threadHaltUnClean();
		
		/*
		for ( double s = 0.5; s <= 5; s = s + 0.1 )
		{
			System.out.print( s + "\t" );
			GaussFit.s = s;//4.4;
			b.gaussfit();
			b.analyzePoints();
		}
		System.exit( 0 );
		*/
		
		double error = 2.19;
		//for ( double error = 0.0625/2; error < 65; error = error * Math.sqrt( Math.sqrt( Math.sqrt( 2 ) ) ) )
		{
			b.fitPointsRANSAC( error );
			b.analyzePoints();
		}
	}

}

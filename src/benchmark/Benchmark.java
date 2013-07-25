package benchmark;

import ij.ImageJ;

import java.io.File;
import java.util.ArrayList;

import localmaxima.LocalMaxima;
import localmaxima.LocalMaximaDoG;
import localmaxima.LocalMaximaSmoothNeighborhood;
import mpicbg.imglib.algorithm.math.MathLib;
import fit.Spot;
import gauss.GaussFit;
import gradient.Gradient;
import gradient.GradientPreCompute;

import net.imglib2.RealLocalizable;
import net.imglib2.collection.KDTree;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.io.ImgIOException;
import net.imglib2.io.ImgOpener;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.neighborsearch.NearestNeighborSearchOnKDTree;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;

public class Benchmark 
{
	final String dir, file;
	final String imageFile = ".tif";
	final String textFile = ".loc";

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
		candiateSearch = new LocalMaximaSmoothNeighborhood( img, new double[]{ 1, 1, 1 }, 0.4 );
		//candiateSearch = new LocalMaximaAll( img );
		
		//ImageJFunctions.show( candiateSearch.getSource() );
		
		peaks = candiateSearch.estimateLocalMaxima();
		
		return peaks.size();
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
	
	public void fitPoints( final double ransacError )
	{
		// we need something to compute the derivatives
		final Gradient derivative;
		
		//derivative = new DerivativeOnDemand( image );
		derivative = new GradientPreCompute( img );
		
		spots = Spot.extractSpots( img, peaks, derivative, range );
		
		//GradientDescent.testGradientDescent( spots, new boolean[]{ false, false, true } );
		//SimpleMultiThreading.threadHaltUnClean();
		
		// ransac on all spots
		Spot.ransac( spots, 1000, ransacError, 1.0/100.0 );
		
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
		final String dir = "documents/Images For Stephan/Tests/";
		final String file = "Poiss_30spots_bg_200_2_I_1000_0_img0";
		
		final Benchmark b = new Benchmark( dir, file );
		
		System.out.println( "peaks: " + b.findPeaks() );
		
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
		
		//double error = 2.19;
		for ( double error = 0.0625/2; error < 65; error = error * Math.sqrt( Math.sqrt( Math.sqrt( 2 ) ) ) )
		{
			b.fitPoints( error );
			b.analyzePoints();
		}
	}

}

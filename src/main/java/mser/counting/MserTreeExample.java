/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2016 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
 * John Bogovic, Albert Cardona, Barry DeZonia, Christian Dietz, Jan Funke,
 * Aivar Grislis, Jonathan Hale, Grant Harris, Stefan Helfrich, Mark Hiner,
 * Martin Horn, Steffen Jaensch, Lee Kamentsky, Larry Lindsey, Melissa Linkert,
 * Mark Longair, Brian Northan, Nick Perry, Curtis Rueden, Johannes Schindelin,
 * Jean-Yves Tinevez and Michael Zinsmaier.
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

package mser.counting;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.EllipseRoi;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.process.ByteProcessor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.componenttree.mser.Mser;
import net.imglib2.algorithm.componenttree.mser.MserTree;
import net.imglib2.algorithm.dog.DogDetection;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;

import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealMatrixFormat;

import gui.interactive.HelperFunctions;

/**
 * Example of computing and visualizing the {@link MserTree} of an image.
 *
 *
 * @author Tobias Pietzsch
 */
public class MserTreeExample< T extends IntegerType< T > >
{
	final ImagePlus imp;
	final Overlay ov;
	final ImageStack stack;
	final int w;
	final int h;
	
//steps per octave for DoG
	private static int defaultSensitivity = 4;

	public MserTreeExample( final ImagePlus imp, final ImageStack stack )
	{
		this.imp = imp;
		if (imp.getOverlay() == null) {
			ov = new Overlay();
			imp.setOverlay( ov );
		}
		else
			ov = imp.getOverlay();
		
		this.stack = stack;
		this.w = imp.getWidth();
		this.h = imp.getHeight();
	}
	
	/**
	 * find the ellipsoid around smfish image
	 */
	public void visualise3D( final MserTree< T > tree)
	{
		double [] sigmas = new double [3];
		for ( final Mser< T > mser : tree ){
			sigmas = getSigmas( mser.mean(), mser.cov(), 3 );
		}
		// TODO: how do we adjust threshold values?
		float pThreshold = 0.0027f;
		// TODO: which sigmas do we take? maybe adjust through
		ArrayList<RefinedPeak<Point>> peaks = computeDog(ImageJFunctions.wrap(imp), (float) sigmas[0], pThreshold);
		ArrayList<RefinedPeak<Point>> filteredPeaks = filterPeaks(peaks, imp);
		// TODO: save peaks to the folder
		printPeaks(filteredPeaks);
	}
	
	public static ArrayList<RefinedPeak<Point>> filterPeaks(ArrayList<RefinedPeak<Point>> peaks, ImagePlus imp){
		ArrayList<RefinedPeak<Point>> filteredPeaks = new ArrayList<>();
		
		if (imp.getRoi() != null){
			Roi roi = imp.getRoi();
			for (RefinedPeak<Point> peak : peaks) {
				if ( roi.contains((int)peak.getFloatPosition(0), (int)peak.getFloatPosition(1)))
					filteredPeaks.add(peak);
			}
		} 
		else {
			// DEBUG: 
			System.out.println("No roi found!");
			filteredPeaks = peaks;
		}
		return filteredPeaks;
	}
	
	public static void printPeaks(ArrayList<RefinedPeak<Point>> peaks) {
		for (RefinedPeak<Point> peak : peaks) {
			System.out.println(createPosString(peak));
		}
	}
	
	public static String createPosString(RefinedPeak<Point> peak) {
		String delimeter = " ";
		String res = peak.getFloatPosition(0) + delimeter + peak.getFloatPosition(1) + delimeter + peak.getFloatPosition(2);
		return res;
	}
	
	public static void savePeaksToFile(ArrayList<RefinedPeak<Point>> peaks) {
		// TODO: 
	}
	
	// TODO: FINISH THE MATHS FORMULAS
	public static double [] getSigmas(final double [] mean, final double[] cov, final double nsigmas) {
		double [] sigmas = new double [3];
		
		// upper triangular matrix
		RealMatrix matrix = MatrixUtils.createRealMatrix(new double [][] {{cov[0], cov[1], cov[2]},
																																	{cov[1], cov[3], cov[4]}, 
																																	{cov[2], cov[4], cov[5]} });
		
		EigenDecomposition ed = new EigenDecomposition(matrix);
		double [] eigenvalues = ed.getRealEigenvalues();
		
		if (eigenvalues.length != 3) System.out.println("Something is wrong; real eigenvalues count is < 3");

//		DEBUG:
//		for (double val : eigenvalues)
//			System.out.println(val);
		
		// TODO: this one is still not finished 
		// for ()
		// find the rotation angle 
		// find sigma 
		// pass these values to the DOG to detect spots
		sigmas[0] = sigmas[1] = sigmas[2] = 10;
		return sigmas;
	}
	
	public static ArrayList<RefinedPeak<Point>> computeDog(final RandomAccessibleInterval<FloatType> pImg, float pSigma,
		float pThreshold) {
		float pSigma2 = HelperFunctions.computeSigma2(pSigma, defaultSensitivity);
		final float tFactor = pImg.numDimensions() == 3 ? 0.5f : 1.0f;
		
		// TODO: adjust the calibration for the z-axis
		double [] calibration = new double [] {1, 1, 1};
		
		final DogDetection<FloatType> dog2 = new DogDetection<>(pImg, calibration, pSigma, pSigma2,
				DogDetection.ExtremaType.MINIMA, tFactor * pThreshold / 2, false);
		ArrayList<RefinedPeak<Point>> pPeaks = dog2.getSubpixelPeaks();
		return pPeaks;
}

	public static < T extends IntegerType< T > > long[] getMserSize(final MserTree< T > tree) {
		long mserSize[] = new long[] {0, 0}; // mean, median
		ArrayList <Long> mserSizes = new ArrayList<>(tree.size());
		
		for(final Mser< T > mser : tree) {
			mserSize[0] += mser.size();
			mserSizes.add(mser.size());
			// DEBUG:
			// break;
			// System.out.println("connected component size: " + mser.size() + ", size:" + mser.cov()[0] + " " + mser.cov()[1] + " " + mser.cov()[2]);
		}

		if (mserSizes.size() > 0) {
			// mean
			mserSize[0] /= tree.size();
			// median
			Collections.sort(mserSizes);
			mserSize[1] = mserSizes.get(mserSizes.size() / 2);
		}

		// DEBUG: 
		// System.out.println("mean: " + mserSize[0]);
		// System.out.println("median: " + mserSize[1]);
		return mserSize;
	}
	
	
	public static void main( final String[] args )
	{
		// ref: http://www.vlfeat.org/overview/mser.html
		final int delta = 1; // "steps" between the stable regions
		final long minSize = 1000; // min size of the connected component 
		final long maxSize = 100 * 100 * 1; // max size of the connected component
		final double maxVar = 0.8; // describes the stability of the region, smaller the value less regions you get 
		final double minDiversity = 1; // describes the similarity with the parent, larger value less regions you get 

		final Img< UnsignedByteType > img;
		try
		{
			new ImageJ();
			IJ.open("/Users/kkolyva/Desktop/2018-06-11-10-09-02-test-mser/it=495-2-cropped-8bit-median-roi.tif");
			img = ImagePlusAdapter.wrapByte( IJ.getImage() );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
			return;
		}

		final ImagePlus impImg = IJ.getImage();
		final ImageStack stack = new ImageStack( (int) img.dimension( 0 ), (int) img.dimension( 1 ) );

		final MserTree< UnsignedByteType > treeBrightToDark = MserTree.buildMserTree( img, new UnsignedByteType( delta ), minSize, maxSize, maxVar, minDiversity, false );
		final MserTreeExample< UnsignedByteType > vis = new MserTreeExample<>( impImg, stack );

		ImageJFunctions.show( projectRGB( treeBrightToDark, img ) );
		System.out.println("Approximate number of spots: " + treeBrightToDark.size());
		
		// TODO: 
		// [ ] add the size check 
		long[] mserSize = getMserSize(treeBrightToDark);
		System.out.println("Mser: mean: " + mserSize[0] + ", median: " + mserSize[1]);
		
		vis.visualise3D(treeBrightToDark);
		
		// [ ] add the DoG on top
		// [ ] 
		// [ ]
		System.out.println("Done!");
	}

	public static Img< FloatType > project( final MserTree< UnsignedByteType > tree, final Interval image )
	{
		final long[] dim = new long[ image.numDimensions() ];

		for ( int d = 0; d < image.numDimensions(); ++d )
			dim[ d ] = image.dimension( d );
		Img< FloatType > vis = ImagePlusImgs.floats( dim );
		final Random rnd = new Random( 353 );
		final RandomAccess< FloatType > ra = vis.randomAccess();

		for ( final Mser< UnsignedByteType > mser : tree )
		{
			final float value = rnd.nextFloat() + 0.1f;

			for ( final Localizable l : mser )
			{
				ra.setPosition( l );
				if ( ra.get().get() != 0.0 )
					ra.get().set( ( value + ra.get().get() ) / 2.0f );
				else
					ra.get().set( value );
			}
		}

		return vis;
	}

	public static Img< ARGBType > projectRGB( final MserTree< UnsignedByteType > tree, final Interval image )
	{
		final long[] dim = new long[ image.numDimensions() ];

		for ( int d = 0; d < image.numDimensions(); ++d )
			dim[ d ] = image.dimension( d );

		final Img< ARGBType > vis = ImagePlusImgs.argbs( dim );
		final Random rnd = new Random( 353 );
		final RandomAccess< ARGBType > ra = vis.randomAccess();

		for ( final Mser< UnsignedByteType > mser : tree )
		{
			final int r = rnd.nextInt( 128 ) + 128; // 128 ... 255
			final int g = rnd.nextInt( 128 ) + 128; // 128 ... 255
			final int b = rnd.nextInt( 128 ) + 128; // 128 ... 255

			for ( final Localizable l : mser )
			{
				ra.setPosition( l );
				final ARGBType t = ra.get();
				int r1 = ARGBType.red( t.get() );
				int g1 = ARGBType.green( t.get() );
				int b1 = ARGBType.blue( t.get() );
	
				if ( r1 + g1 + b1 == 0 )
				{
					r1 = r;
					g1 = g;
					b1 = b;
				}
				else
				{
					r1 = ( r1 + r ) / 2;
					g1 = ( g1 + g ) / 2;
					b1 = ( b1 + b ) / 2;
				}

				ra.get().set( ARGBType.rgba( r1, g1, b1, 0 ) );
			}
		}

		return vis;
	}
	
}

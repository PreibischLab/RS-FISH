package radial.symmetry.utils;
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
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.dog.DifferenceOfGaussian;
import net.imglib2.algorithm.localextrema.LocalExtrema;
import net.imglib2.algorithm.localextrema.LocalExtrema.LocalNeighborhoodCheck;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.algorithm.localextrema.SubpixelLocalization;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

/**
 * @author Stephan Preibisch
 * @author Tobias Pietzsch
 */
public class DoGDetection< T extends RealType< T > & NativeType< T > >
{
	public static enum ExtremaType
	{
		/**
		 * Bright blobs on dark background.
		 */
		MINIMA,

		/**
		 * Dark blobs on bright background.
		 */
		MAXIMA
	}

	/**
	 * {@link ExecutorService} used for Multi-Threading. If null, a new one will
	 * be created on demand
	 **/
	private ExecutorService executorService;

	public DoGDetection(
			final RandomAccessibleInterval< T > input,
			final double[] calibration,
			final double sigmaSmaller,
			final double sigmaLarger,
			final ExtremaType extremaType,
			final double minPeakValue,
			final boolean normalizeMinPeakValue )
	{
		this( Views.extendMirrorSingle( input ), input, calibration, sigmaSmaller, sigmaLarger, extremaType, minPeakValue, normalizeMinPeakValue );
	}

	/**
	 * Sets up a {@link DogDetection} with the specified parameters (does not do
	 * any computation yet). If the input image is of type {@link DoubleType},
	 * {@link DoubleType} will be used for computing the Difference-of-Gaussian.
	 * In all other cases, {@link FloatType} will be used).
	 *
	 * @param input
	 *            the input image.
	 * @param interval
	 *            which interval of the input image to process
	 * @param calibration
	 *            The calibration, i.e., the voxel sizes in some unit for the
	 *            input image.
	 * @param sigmaSmaller
	 *            sigma for the smaller scale in the same units as calibration.
	 * @param sigmaLarger
	 *            sigma for the larger scale in the same units as calibration.
	 * @param extremaType
	 *            which type of extrema (minima, maxima) to detect. Note that
	 *            minima in the Difference-of-Gaussian correspond to bright
	 *            blobs on dark background. Maxima correspond to dark blobs on
	 *            bright background.
	 * @param minPeakValue
	 *            threshold value for detected extrema. Maxima below
	 *            {@code minPeakValue} or minima above {@code -minPeakValue}
	 *            will be disregarded.
	 * @param normalizeMinPeakValue
	 *            Whether the peak value should be normalized. The
	 *            Difference-of-Gaussian is an approximation of the
	 *            scale-normalized Laplacian-of-Gaussian, with a factor of
	 *            <em>f = sigmaSmaller / (sigmaLarger - sigmaSmaller)</em>. If
	 *            {@code normalizeMinPeakValue=true}, the {@code minPeakValue}
	 *            will be divided by <em>f</em> (which is equivalent to scaling
	 *            the DoG by <em>f</em>).
	 */
	public DoGDetection(
			final RandomAccessible< T > input,
			final Interval interval,
			final double[] calibration,
			final double sigmaSmaller,
			final double sigmaLarger,
			final ExtremaType extremaType,
			final double minPeakValue,
			final boolean normalizeMinPeakValue )
	{
		this( input, interval, calibration, sigmaSmaller, sigmaLarger, extremaType, minPeakValue, normalizeMinPeakValue, new DogComputationType<>( input, interval ).getType() );
	}

	/**
	 * Sets up a {@link DogDetection} with the specified parameters (does not do
	 * any computation yet).
	 *
	 * @param input
	 *            the input image.
	 * @param interval
	 *            which interval of the input image to process
	 * @param calibration
	 *            The calibration, i.e., the voxel sizes in some unit for the
	 *            input image.
	 * @param sigmaSmaller
	 *            sigma for the smaller scale in the same units as calibration.
	 * @param sigmaLarger
	 *            sigma for the larger scale in the same units as calibration.
	 * @param extremaType
	 *            which type of extrema (minima, maxima) to detect. Note that
	 *            minima in the Difference-of-Gaussian correspond to bright
	 *            blobs on dark background. Maxima correspond to dark blobs on
	 *            bright background.
	 * @param minPeakValue
	 *            threshold value for detected extrema. Maxima below
	 *            {@code minPeakValue} or minima above {@code -minPeakValue}
	 *            will be disregarded.
	 * @param normalizeMinPeakValue
	 *            Whether the peak value should be normalized. The
	 *            Difference-of-Gaussian is an approximation of the
	 *            scale-normalized Laplacian-of-Gaussian, with a factor of
	 *            <em>f = sigmaSmaller / (sigmaLarger - sigmaSmaller)</em>. If
	 *            {@code normalizeMinPeakValue=true}, the {@code minPeakValue}
	 *            will be divided by <em>f</em> (which is equivalent to scaling
	 *            the DoG by <em>f</em>).
	 * @param computationType
	 *            The type to use for computing the Difference-of-Gaussian.
	 */
	public < F extends RealType< F > & NativeType< F > > DoGDetection(
			final RandomAccessible< T > input,
			final Interval interval,
			final double[] calibration,
			final double sigmaSmaller,
			final double sigmaLarger,
			final ExtremaType extremaType,
			final double minPeakValue,
			final boolean normalizeMinPeakValue,
			final F computationType )
	{
		this.input = input;
		this.interval = interval;
		this.sigmaSmaller = sigmaSmaller;
		this.sigmaLarger = sigmaLarger;
		this.pixelSize = calibration;
		this.typedDogDetection = new TypedDogDetection<>( computationType );
		this.imageSigma = 0.5;
		this.minf = 2;
		this.extremaType = extremaType;
		this.minPeakValue = minPeakValue;
		this.normalizeMinPeakValue = normalizeMinPeakValue;
		this.keepDoGImg = true;
		this.numThreads = Runtime.getRuntime().availableProcessors();
	}

	/**
	 * If you want to get subpixel-localized peaks, call
	 * {@link #getSubpixelPeaks()} directly.
	 */
	public ArrayList< Point > getPeaks()
	{
		return typedDogDetection.getPeaks();
	}

	public ArrayList< RefinedPeak< Point > > getSubpixelPeaks()
	{
		return typedDogDetection.getSubpixelPeaks();
	}

	public RandomAccessibleInterval< ? > getDogImage() { return typedDogDetection.dogImg; }

	protected final RandomAccessible< T > input;

	protected final Interval interval;

	protected final double sigmaSmaller;

	protected final double sigmaLarger;

	protected final double[] pixelSize;

	protected final TypedDogDetection< ? > typedDogDetection;

	protected double imageSigma;

	protected double minf;

	protected ExtremaType extremaType;

	protected double minPeakValue;

	protected boolean normalizeMinPeakValue;

	protected boolean keepDoGImg;

	protected int numThreads;

	public void setImageSigma( final double imageSigma )
	{
		this.imageSigma = imageSigma;
	}

	public void setMinf( final double minf )
	{
		this.minf = minf;
	}

	public void setMinPeakValue( final double minPeakValue )
	{
		this.minPeakValue = minPeakValue;
	}

	public void setNormalizeMinPeakValue( final boolean normalizeMinPeakValue )
	{
		this.normalizeMinPeakValue = normalizeMinPeakValue;
	}

	public void setKeepDoGImg( final boolean keepDoGImg )
	{
		this.keepDoGImg = keepDoGImg;
	}

	public void setNumThreads( final int numThreads )
	{
		this.numThreads = numThreads;
	}

	public double getImageSigma()
	{
		return imageSigma;
	}

	public double getMinf()
	{
		return minf;
	}

	public double getMinPeakValue()
	{
		return minPeakValue;
	}

	public boolean getNormalizeMinPeakValue()
	{
		return normalizeMinPeakValue;
	}

	public boolean getKeepDoGImg()
	{
		return keepDoGImg;
	}

	public int getNumThreads()
	{
		return numThreads;
	}

	public void setExecutorService( final ExecutorService service )
	{
		this.executorService = service;
	}

	private static class DogComputationType< F extends RealType< F > & NativeType< F > >
	{
		private final F type;

		@SuppressWarnings( "unchecked" )
		public DogComputationType(
				final RandomAccessible< ? > input,
				final Interval interval )
		{
			final Object t = Util.getTypeFromInterval( Views.interval( input, interval ) );
			if ( t instanceof DoubleType )
				type = ( F ) new DoubleType();
			else
				type = ( F ) new FloatType();
		}

		public F getType()
		{
			return type;
		}
	}

	protected class TypedDogDetection< F extends RealType< F > & NativeType< F > >
	{
		protected final F type;

		protected RandomAccessibleInterval< F > dogImg;

		public TypedDogDetection( final F type )
		{
			this.type = type;
		}

		public ArrayList< Point > getPeaks()
		{
			final ExecutorService service;
			if ( executorService == null )
				service = Executors.newFixedThreadPool( numThreads );
			else
				service = executorService;

			dogImg = Util.getArrayOrCellImgFactory( interval, type ).create( interval );
			final long[] translation = new long[ interval.numDimensions() ];
			interval.min( translation );
			dogImg = Views.translate( dogImg, translation );

			final double[][] sigmas = DifferenceOfGaussian.computeSigmas( imageSigma, minf, pixelSize, sigmaSmaller, sigmaLarger );
			DifferenceOfGaussian.DoG( sigmas[ 0 ], sigmas[ 1 ], input, dogImg, service );
			final F val = type.createVariable();
			final double minValueT = type.getMinValue();
			final double maxValueT = type.getMaxValue();
			final LocalNeighborhoodCheck< Point, F > localNeighborhoodCheck;
			final double normalization = normalizeMinPeakValue ? ( sigmaLarger / sigmaSmaller - 1.0 ) : 1.0;
			switch ( extremaType )
			{
			case MINIMA:
				val.setReal( Math.max( Math.min( -minPeakValue * normalization, maxValueT ), minValueT ) );
				localNeighborhoodCheck = new LocalExtrema.MinimumCheck<>( val );
				break;
			case MAXIMA:
			default:
				val.setReal( Math.max( Math.min( minPeakValue * normalization, maxValueT ), minValueT ) );
				localNeighborhoodCheck = new LocalExtrema.MaximumCheck<>( val );
			}
			final ArrayList< Point > peaks = LocalExtrema.findLocalExtrema( dogImg, localNeighborhoodCheck, service );
			if ( !keepDoGImg )
				dogImg = null;

			if ( executorService == null )
				service.shutdown();

			return peaks;
		}

		public ArrayList< RefinedPeak< Point > > getSubpixelPeaks()
		{
			final boolean savedKeepDoGImg = keepDoGImg;
			keepDoGImg = true;
			final ArrayList< Point > peaks = getPeaks();
			final SubpixelLocalization< Point, F > spl = new SubpixelLocalization<>( dogImg.numDimensions() );
			spl.setAllowMaximaTolerance( true );
			spl.setMaxNumMoves( 10 );
			final ArrayList< RefinedPeak< Point > > refined = spl.process( peaks, dogImg, dogImg );
			keepDoGImg = savedKeepDoGImg;
			if ( !keepDoGImg )
				dogImg = null;
			return refined;
		}
	}
}

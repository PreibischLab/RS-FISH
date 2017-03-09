package gui.interactive;

import java.awt.Rectangle;
import java.util.ArrayList;

import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import mpicbg.imglib.util.Util;

public class HelperFunctions {

	public static Img< FloatType > toImg( final ImagePlus imagePlus, final long[] dim, final int type )
	{
		final ImageProcessor ip = imagePlus.getStack().getProcessor( imagePlus.getCurrentSlice() );
		final Object pixels = ip.getPixels();

		final Img< FloatType > imgTmp;

		if ( type == 0 )
		{
			imgTmp = ArrayImgs.floats( dim );
			final byte[] p = (byte[])pixels;

			int i = 0;
			for ( final FloatType t : imgTmp )
				t.set( UnsignedByteType.getUnsignedByte( p[ i++ ] ) );
				
		}
		else if ( type == 1 )
		{
			imgTmp = ArrayImgs.floats( dim );
			final short[] p = (short[])pixels;

			int i = 0;
			for ( final FloatType t : imgTmp )
				t.set( UnsignedShortType.getUnsignedShort( p[ i++ ] ) );
		}
		else
		{
			imgTmp = ArrayImgs.floats( (float[])pixels, dim );
		}

		return imgTmp;
	}

	// APPROVED: 
	public static float computeSigma2(final float sigma1, final int stepsPerOctave) {
		final float k = (float) Math.pow( 2f, 1f / stepsPerOctave );
		return sigma1 * k;
	}

	public static float computeValueFromScrollbarPosition(final int scrollbarPosition, final float min,
			final float max, final int scrollbarSize) {
		return min + (scrollbarPosition / (float) scrollbarSize) * (max - min);
	}

	public static int computeScrollbarPositionFromValue(final float sigma, final float min, final float max,
			final int scrollbarSize) {
		return Util.round(((sigma - min) / (max - min)) * scrollbarSize);
	}

	/**
	 * sets the calibration for the initial image. Only the relative value matters.
	 * normalize everything with respect to the 1-st coordinate.
	 * */
	public static double [] setCalibration(ImagePlus imagePlus, int numDimensions){
		 double [] calibration = new double[numDimensions]; // should always be 2 for the interactive mode
		// if there is something reasonable in x-axis calibration use this value
		if ((imagePlus.getCalibration().pixelWidth >= 1e-13) && imagePlus.getCalibration().pixelWidth != Double.NaN){
			calibration[0] = imagePlus.getCalibration().pixelWidth/imagePlus.getCalibration().pixelWidth;
			calibration[1] = imagePlus.getCalibration().pixelHeight/imagePlus.getCalibration().pixelWidth;		
			if (numDimensions == 3)
				calibration[2] = imagePlus.getCalibration().pixelDepth/imagePlus.getCalibration().pixelWidth;
		}
		else{
			// otherwise set everything to 1.0 trying to fix calibration
			for (int i = 0; i < numDimensions; ++i)
				calibration[i] = 1.0;
		}
		return calibration;
	}
	

	public static <T extends RealType<T>> void printCoordinates(RandomAccessibleInterval<T> img) {
		for (int d = 0; d < img.numDimensions(); ++d) {
			System.out.println("[" + img.min(d) + " " + img.max(d) + "] ");
		}
	}
	
	// check if peak is inside of the rectangle
	protected static boolean isInside( final RefinedPeak<Point> peak, final Rectangle rectangle )
	{
		if ( rectangle == null )
			return true;

		final float x = peak.getFloatPosition(0);
		final float y = peak.getFloatPosition(1);

		boolean res = (x >= (rectangle.x) && y >= (rectangle.y) && 
				x < (rectangle.width + rectangle.x - 1) && y < (rectangle.height + rectangle.y - 1));

		return res;
	}
	
	/**
	 * Copy peaks found by DoG to lighter ArrayList (!imglib2)
	 */
	public static void copyPeaks(
			final ArrayList<RefinedPeak<Point>> peaks,
			final ArrayList<long[]> simplifiedPeaks,
			final int numDimensions,
			final Rectangle rectangle,
			final double threshold ) {
		// TODO: here should be the threshold for the peak values
		for (final RefinedPeak<Point> peak : peaks){
			// TODO: add threshold value
			if (HelperFunctions.isInside( peak, rectangle ) && (-peak.getValue() > threshold))
			{
				final long[] coordinates = new long[numDimensions];
				for (int d = 0; d < peak.numDimensions(); ++d)
					coordinates[d] = Util.round(peak.getDoublePosition(d));

				simplifiedPeaks.add(coordinates);
			}
		}
	}
	
	
	/**
	 * used by background subtraction to calculate
	 * the boundaries of the spot 
	 * */
	// FIXME: Actually wrong! check 0 should interval.min(d)
	public static void getBoundaries(long[] peak, long[] min, long [] max, long [] fullImgMax, int supportRadius){
		for (int d = 0; d < peak.length; ++d){
			// check that it does not exceed bounds of the underlying image
			min[d] = Math.max(peak[d] - supportRadius, 0);
			max[d] = Math.min(peak[d] + supportRadius, fullImgMax[d]);

		}
	}
	
}

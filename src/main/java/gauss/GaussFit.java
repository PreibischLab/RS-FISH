package gauss;

import mpicbg.imglib.algorithm.math.LocalizablePoint;
import mpicbg.imglib.algorithm.peak.GaussianPeakFitterND;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.real.FloatType;
import net.imglib2.img.Img;

public class GaussFit 
{
	final Image< FloatType > image;
	final GaussianPeakFitterND<FloatType> fitter;
	final float[] tmp;
	final double[] typicalSigma;
	final int n;
	
	public static double s = 1;
	
	public GaussFit( final Img< net.imglib2.type.numeric.real.FloatType > img )
	{
		this.image = mpicbg.imglib.wrapper.ImgLib2.wrapArrayFloatToImgLib1( img );
		this.fitter = new GaussianPeakFitterND< FloatType >( image );
		this.n = img.numDimensions();
		this.tmp = new float[ n ];
		this.typicalSigma = new double[ n ];
		
		for ( int d = 0; d < n; ++d )
			typicalSigma[ d ] = s;
		
	}
	
	public float[] fit( final int[] loc )
	{
		for ( int d = 0; d < n; ++d )
			tmp[ d ] = loc[ d ];
    	    	
		final double[] results = fitter.process( new LocalizablePoint( tmp ), typicalSigma );
		
		//double a = results[ 0 ];
		double x = results[ 1 ];
		double y = results[ 2 ];
		double z = results[ 3 ];
		double sx = 1/Math.sqrt( results[ 4 ] );
		double sy = 1/Math.sqrt( results[ 5 ] );
		double sz = 1/Math.sqrt( results[ 6 ] );
		
		final float[] result = new float[]{ (float)x, (float)y, (float)z };
		
		//System.out.println( x + " " + y + " " + z + " s: " + sx + " " + sy + " " + sz );
		if ( ! (Double.isNaN( sx ) || Double.isNaN( sy ) || Double.isNaN( sz ) ) )
			return result;
		else
			return null;
	}
}

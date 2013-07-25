package localmaxima;

import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss.Gauss;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class LocalMaximaSmoothNeighborhood extends LocalMaximaNeighborhood
{
	public LocalMaximaSmoothNeighborhood( final RandomAccessibleInterval<FloatType> source, final double[] sigma )
	{
		super( smooth( source, sigma ) );	
	}

	public LocalMaximaSmoothNeighborhood( final RandomAccessibleInterval<FloatType> source, final double[] sigma, final double threshold )
	{
		super( smooth( source, sigma ), threshold );	
	}
	
	private static final Img< FloatType > smooth( final RandomAccessibleInterval<FloatType> source, final double[] sigma )
	{
		final Img< FloatType > smoothed = new ArrayImgFactory<FloatType>().create( source, new FloatType() );
		Gauss.inFloat( sigma, Views.extendMirrorSingle( source ), source, smoothed, new Point( source.numDimensions() ), new ArrayImgFactory< FloatType >() );
		return smoothed;
	}
}

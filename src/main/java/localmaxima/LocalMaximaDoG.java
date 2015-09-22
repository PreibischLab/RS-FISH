package localmaxima;

import java.util.ArrayList;

import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussianPeak;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussianReal1;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.wrapper.ImgLib2;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;

public class LocalMaximaDoG extends LocalMaxima
{
	final double sigma1, sigma2, minPeakValue;
	
	public LocalMaximaDoG( final Img< FloatType > source, final double sigma1, final double sigma2, final double minPeakValue )
	{
		super( source );
		this.sigma1 = sigma1;
		this.sigma2 = sigma2;
		this.minPeakValue = minPeakValue;
	}
	
	@Override
	public ArrayList< int[] > estimateLocalMaxima() 
	{
		DifferenceOfGaussianReal1< mpicbg.imglib.type.numeric.real.FloatType > dog = new DifferenceOfGaussianReal1< mpicbg.imglib.type.numeric.real.FloatType >(
				ImgLib2.wrapFloatToImgLib1( (Img< FloatType >)source ),
				new OutOfBoundsStrategyMirrorFactory< mpicbg.imglib.type.numeric.real.FloatType >(),
				sigma1, 
				sigma2, 
				minPeakValue, 
				1 );
		
		dog.setKeepDoGImage( false );
		dog.process();
				
		final ArrayList< int[] > peakList = new ArrayList<int[]>( dog.getPeaks().size() );
		
		for ( final DifferenceOfGaussianPeak< mpicbg.imglib.type.numeric.real.FloatType > peak : dog.getPeaks() )
		{
			final int[] tmp = new int[ numDimensions ];
			
			for ( int d = 0; d < numDimensions; ++d )
				tmp[ d ] = peak.getPosition( d );
			
			peakList.add( tmp );
		}

		return peakList;
	}
}

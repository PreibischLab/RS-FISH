package localmaxima;

import java.util.ArrayList;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.legacy.scalespace.DifferenceOfGaussian;
import net.imglib2.algorithm.legacy.scalespace.DifferenceOfGaussianPeak;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory.Boundary;
import net.imglib2.type.numeric.real.FloatType;

public class LocalMaximaDoG extends LocalMaximaCandidates
{
	final RandomAccessibleInterval< FloatType > img;
	final int numDimensions;
	final double sigma1, sigma2, minPeakValue;
	
	public LocalMaximaDoG( final RandomAccessibleInterval< FloatType > img, final double sigma1, final double sigma2, final double minPeakValue )
	{
		this.img = img;
		this.numDimensions = img.numDimensions();
		this.sigma1 = sigma1;
		this.sigma2 = sigma2;
		this.minPeakValue = minPeakValue;
	}
	
	@Override
	public ArrayList<int[]> estimateLocalMaxima() 
	{
		DifferenceOfGaussian< FloatType > dog = new DifferenceOfGaussian<FloatType>( 
				img, 
				new ArrayImgFactory<FloatType>(), 
				new OutOfBoundsMirrorFactory<FloatType, RandomAccessibleInterval<FloatType>>( Boundary.SINGLE ), 
				sigma1, 
				sigma2, 
				minPeakValue, 
				1 );
		
		dog.setKeepDoGImg( false );
		dog.process();
				
		final ArrayList< int[] > peakList = new ArrayList<int[]>( dog.getPeaks().size() );
		
		for ( final DifferenceOfGaussianPeak< FloatType > peak : dog.getPeaks() )
		{
			final int[] tmp = new int[ numDimensions ];
			
			for ( int d = 0; d < numDimensions; ++d )
				tmp[ d ] = peak.getIntPosition( d );
			
			peakList.add( tmp );
		}

		return peakList;
	}

	@Override
	public int numDimensions() { return numDimensions; }
}

package localmaxima;

import java.util.ArrayList;

import net.imglib2.EuclideanSpace;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

public abstract class LocalMaxima implements EuclideanSpace
{
	final protected RandomAccessibleInterval< FloatType > source;
	final protected int numDimensions;

	public LocalMaxima( final RandomAccessibleInterval< FloatType > source )
	{
		this.source = source;
		this.numDimensions = source.numDimensions();
	}

	public abstract ArrayList< int[] > estimateLocalMaxima();

	@Override
	public int numDimensions() { return numDimensions; }
}

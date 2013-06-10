package localmaxima;

import java.util.ArrayList;

import net.imglib2.EuclideanSpace;

public abstract class LocalMaximaCandidates implements EuclideanSpace
{
	public abstract ArrayList< int[] > estimateLocalMaxima();
}

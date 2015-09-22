package gradient;

import net.imglib2.Localizable;

public interface ComputeGradient 
{
	/**
	 * Computes the n-dimensional 1st derivative vector in center of a 2x2x2...x2 environment for a certain location
	 * defined by the position of the RandomAccess
	 * 
	 * @param location - the top-left-front position for which to compute the derivative
	 * @param derivativeVector - where to put the derivative vector [3]
	 */
	public void gradientAt( final Localizable location, final double[] derivativeVector );
}

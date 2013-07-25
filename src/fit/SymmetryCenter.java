package fit;

import net.imglib2.EuclideanSpace;
import mpicbg.models.Model;
import mpicbg.models.Point;

public interface SymmetryCenter< M extends AbstractFunction< M > > extends Model< M >, Function< Point >, EuclideanSpace
{
	/**
	 * @param center - will write the symmetry center into the array, dimensionality must match
	 */
	public void getSymmetryCenter( final double center[] );
	
	/**
	 * @param center - will write the symmetry center into the array, dimensionality must match
	 */
	public void getSymmetryCenter( final float center[] );
	
	/**
	 * @param d - dimension
	 * @return the center in dimension d
	 */
	public double getSymmetryCenter( final int d );
	
	public void setSymmetryCenter( final double center, final int d );
}

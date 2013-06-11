package fit;

import mpicbg.models.Model;
import mpicbg.models.Point;

public interface SymmetryCenter< M extends AbstractFunction< M > > extends Model< M >, Function< Point >
{
	public void getSymmetryCenter( final double center[] );
	public void getSymmetryCenter( final float center[] );
	
	public double getSymmetryCenter( final int d );
}

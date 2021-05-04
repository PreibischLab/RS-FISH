package util.st.render;

import net.imglib2.EuclideanSpace;
import net.imglib2.RealLocalizable;
import net.imglib2.Sampler;

/**
 * Integrating Nearest-neighbor search in an Euclidean space. The interface describes
 * implementations that perform the search for a specified value and provide
 * access to that value.
 * 
 * @author Stephan Preibisch
 */
public interface IntegratingNeighborSearch< T > extends EuclideanSpace
{
	/**
	 * Perform intgrating-neighbor search for a reference coordinate.
	 * 
	 * @param reference
	 */
	public void search( final RealLocalizable reference );

	/**
	 * Access the data of the nearest neighbor. Data is accessed through a
	 * {@link Sampler} that guarantees write access if the underlying data set
	 * is writable.
	 */
	public Sampler< T > getSampler();

	/**
	 * Create a copy.
	 */
	public IntegratingNeighborSearch< T > copy();
}

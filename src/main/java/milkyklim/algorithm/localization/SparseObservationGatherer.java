package milkyklim.algorithm.localization;

import fit.PointFunctionMatch;
import intensity.Intensity.WrappedSpot;
import net.imglib2.RandomAccessible;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.algorithm.localization.Observation;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class SparseObservationGatherer < T extends RealType< T > > implements ObservationGatherer< WrappedSpot > 
{
	RandomAccessible <T> img; // initial image
	RealRandomAccessible<T> interpolant; // interpolated image for non-integer intensities
	
	public SparseObservationGatherer(RandomAccessible< T > img)
	{
		// set the lookup image; necessary to get the intensities
		// TODO: we need a lookup from peak >> which pixels to use (inliers)
		// spot.getInliers();
		
		this.img = img;
		NLinearInterpolatorFactory<T> factory = new NLinearInterpolatorFactory<>();
		interpolant = Views.interpolate(img, factory);
	}

	@Override
	public Observation gatherObservationData( final WrappedSpot peak )
	{
		int n_pixels = peak.getSpot().getInliers().size();
		int numDimensions = peak.numDimensions();
		
		double [] I = new double[n_pixels];
		double [][] x = new double[n_pixels][numDimensions];

		int idx = 0;
		RealRandomAccess<T> rra = interpolant.realRandomAccess();
		
		// FIXME: Is there a better way to prevent this dirty cast ?
		for (PointFunctionMatch pm : peak.getSpot().getInliers()){
			double [] pos = pm.getP1().getL(); 
			for (int d = 0; d < numDimensions; d++)
				x[idx][d] = pos[d];
			rra.setPosition(pos);
			I[idx] = rra.get().getRealDouble();
			idx++;
		}
		
		Observation obs = new Observation();
		obs.I = I;
		obs.X = x;
		return obs;
	}
}

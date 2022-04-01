package milkyklim.algorithm.localization;

import java.util.List;

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
	final RandomAccessible <T> img; // initial image
	final RealRandomAccessible<T> interpolant; // interpolated image for non-integer intensities
	final int ransacSelection;

	public SparseObservationGatherer( final RandomAccessible< T > img, final int ransacSelection )
	{
		// set the lookup image; necessary to get the intensities
		// TODO: we need a lookup from peak >> which pixels to use (inliers)
		// spot.getInliers();
		
		this.img = img;
		NLinearInterpolatorFactory<T> factory = new NLinearInterpolatorFactory<>();
		this.interpolant = Views.interpolate(img, factory);
		this.ransacSelection = ransacSelection;
	}

	@Override
	public Observation gatherObservationData( final WrappedSpot peak )
	{
		final List< PointFunctionMatch > pms = ransacSelection == 0 ? peak.getSpot().getCandidates() : peak.getSpot().getInliers();

		final int n_pixels = pms.size();
		final int numDimensions = peak.numDimensions();
		
		final double [] I = new double[n_pixels];
		final double [][] x = new double[n_pixels][numDimensions];

		int idx = 0;
		final RealRandomAccess<T> rra = interpolant.realRandomAccess();

		for (PointFunctionMatch pm : pms){
			double [] pos = pm.getP1().getL(); 
			for (int d = 0; d < numDimensions; d++)
				x[idx][d] = pos[d];
			rra.setPosition(pos);
			I[idx] = rra.get().getRealDouble();
			idx++;
		}

		final Observation obs = new Observation();
		obs.I = I;
		obs.X = x;
		return obs;
	}
}

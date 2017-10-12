package net.imglib2.algorithm.localization;

import net.imglib2.Cursor;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.algorithm.region.localneighborhood.RectangleNeighborhoodGPL;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import fit.PointFunctionMatch;
import fit.Spot;

public class SparseObservationGatherer < T extends RealType< T >> implements ObservationGatherer 
{
	
	RandomAccessibleInterval <T> img;
	RealRandomAccessible<T> interpolant;
	
	public SparseObservationGatherer(RandomAccessibleInterval< T > img)
	{
		// set the lookup image; necessary to get the intensities
		// TODO: we need a lookup from peak >> which pixels to use (inliers)
		// spot.getInliers();
		
		this.img = img;
		NLinearInterpolatorFactory<T> factory = new NLinearInterpolatorFactory<>();
		interpolant = Views.interpolate(Views.extendZero(img), factory);
	}

	@Override
	public Observation gatherObservationData( final Localizable peak )
	{
		int n_pixels = ((Spot) peak).getInliers().size();
		int numDimensions = peak.numDimensions();
		
		double [] I = new double[n_pixels];
		double [][] x = new double[n_pixels][numDimensions];

		int idx = 0;
		RealRandomAccess<T> rra = interpolant.realRandomAccess();
		
		// FIXME: Is there a better way to prevent this dirty cast ?
		for (PointFunctionMatch pm : ((Spot) peak).getInliers()){
			double [] pos = pm.getP1().getL(); 
			// System.out.print(pos[0] + " "  + pos[1]);
			for (int d = 0; d < numDimensions; d++){
				x[idx][d] = pos[d];
			}
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

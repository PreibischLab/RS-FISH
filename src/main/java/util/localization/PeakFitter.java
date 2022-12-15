package util.localization;

import java.util.Collection;

import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

public class PeakFitter<T extends RealType<T> > extends GenericPeakFitter< T, Localizable >
{
	// TODO: trash this class once we make a major version bump
	public PeakFitter(RandomAccessibleInterval<T> image, Collection<Localizable> peaks, FunctionFitter fitter,
			FitFunction peakFunction, StartPointEstimator estimator)
	{
		super(image, peaks, fitter, peakFunction, estimator);
	}
}

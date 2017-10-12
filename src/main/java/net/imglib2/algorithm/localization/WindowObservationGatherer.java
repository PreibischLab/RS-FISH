package net.imglib2.algorithm.localization;

import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

public class WindowObservationGatherer<T extends RealType<T>> implements ObservationGatherer
{
	final RandomAccessibleInterval<T> image;
	final long[] padSize;

	public WindowObservationGatherer( final RandomAccessibleInterval<T> image, final long[] padSize )
	{
		this.image = image;
		this.padSize = padSize;
	}

	@Override
	public Observation gatherObservationData( final Localizable peak)
	{
		return LocalizationUtils.gatherObservationData( image, peak, padSize );
	}

}

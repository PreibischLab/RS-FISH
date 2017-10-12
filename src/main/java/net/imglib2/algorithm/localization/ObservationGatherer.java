package net.imglib2.algorithm.localization;

import net.imglib2.Localizable;

public interface ObservationGatherer
{
	Observation gatherObservationData( final Localizable peak );
}

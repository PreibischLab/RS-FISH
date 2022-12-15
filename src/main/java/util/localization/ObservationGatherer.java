package util.localization;

import net.imglib2.Localizable;

public interface ObservationGatherer< L extends Localizable >
{
	Observation gatherObservationData( final L peak );
}

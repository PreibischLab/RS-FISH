package milkyklim.algorithm.localization;

import net.imglib2.Localizable;
import net.imglib2.algorithm.localization.Observation;

public interface ObservationGatherer< L extends Localizable >
{
	Observation gatherObservationData( final L peak );
}

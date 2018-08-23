package gradient.normalized.computation;

import java.util.ArrayList;
import java.util.LinkedList;

import net.imglib2.util.Util;

import gradient.computation.Gradient;

public class NormalizedGradientAverage extends NormalizedGradient {
	public NormalizedGradientAverage(final Gradient gradient) {
		super(gradient);
	}

	@Override
	protected void computeBackground(final ArrayList<LinkedList<Double>> gradientsPerDim, final double[] bkgrnd) {
		for (int d = 0; d < n; ++d)
			bkgrnd[d] = Util.average(NormalizedGradientMedian.collection2Array(gradientsPerDim.get(d)));
	}
}
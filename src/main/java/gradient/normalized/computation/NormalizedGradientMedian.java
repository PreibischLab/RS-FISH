package gradient.normalized.computation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import net.imglib2.util.Util;

import gradient.computation.Gradient;

public class NormalizedGradientMedian extends NormalizedGradient {
	public NormalizedGradientMedian(final Gradient gradient) {
		super(gradient);
	}

	@Override
	protected void computeBackground(final ArrayList<LinkedList<Double>> gradientsPerDim, final double[] bkgrnd) {
		for (int d = 0; d < n; ++d)
			bkgrnd[d] = Util.median(collection2Array(gradientsPerDim.get(d)));
	}

	public static double[] collection2Array(final Collection<Double> c) {
		final double[] l = new double[c.size()];

		int i = 0;
		for (final double v : c)
			l[i++] = v;

		return l;
	}
}

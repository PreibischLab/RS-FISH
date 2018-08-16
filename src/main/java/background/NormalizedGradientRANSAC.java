package background;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import fitting.Center;
import fitting.PointFunctionMatch;
import fitting.Center.CenterMethod;
import gradient.Gradient;
import mpicbg.models.Point;

public class NormalizedGradientRANSAC extends NormalizedGradient {
	double maxError, minInlierRatio;
	CenterMethod centerMethod;

	public NormalizedGradientRANSAC(final Gradient gradient, final CenterMethod centerMethod, final double maxError,
			final double minInlierRatio) {
		super(gradient);

		this.maxError = maxError;
		this.minInlierRatio = minInlierRatio;
		this.centerMethod = centerMethod;
	}

	public NormalizedGradientRANSAC(final Gradient gradient, final CenterMethod centerMethod) {
		this(gradient, centerMethod, 0.05, 0.3);
	}

	public void setCenterMethod(final CenterMethod method) {
		this.centerMethod = method;
	}

	public void setMaxError(final double maxError) {
		this.maxError = maxError;
	}

	public void setMinInlierRatio(final double minInlierRatio) {
		this.minInlierRatio = minInlierRatio;
	}

	public CenterMethod getCenterMethod() {
		return centerMethod;
	}

	public double getMaxError() {
		return maxError;
	}

	public double getMinInlierRatio() {
		return minInlierRatio;
	}

	@Override
	protected void computeBackground(final ArrayList<LinkedList<Double>> gradientsPerDim, final double[] bkgrnd) {
		// TODO: Parameters in GUI: 0.05, 0.3
		for (int d = 0; d < n; ++d)
			bkgrnd[d] = runRANSAC(gradientsPerDim.get(d), centerMethod, maxError, minInlierRatio);
	}

	public static double runRANSAC(final Collection<Double> values, final CenterMethod centerMethod,
			final double maxError, final double minInlierRatio) {
		final ArrayList<PointFunctionMatch> candidates = new ArrayList<>();
		final ArrayList<PointFunctionMatch> inliers = new ArrayList<>();

		for (final double d : values)
			candidates.add(new PointFunctionMatch(new Point(new double[] { d })));

		final Center l = new Center(centerMethod);

		try {
			l.ransac(candidates, inliers, 500, maxError, minInlierRatio);
			l.fit(inliers);

			return l.getP();
		} catch (Exception e) {
			return 0;
		}
	}

}

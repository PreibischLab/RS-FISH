/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2025 RS-FISH developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package corrections;

import fit.PointFunctionMatch;
import fit.polynomial.QuadraticFunction;
import java.util.ArrayList;
import java.util.List;
import mpicbg.models.Point;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

public class QuadraticFunctionAxisDifference extends QuadraticFunction {
    /**
     *
     */
    private static final long serialVersionUID = -3873461779139311393L;

    public QuadraticFunctionAxisDifference() {
        this(0, 0, 0);
    }

    public QuadraticFunctionAxisDifference(final double a, final double b, final double c) {
        super(a, b, c);
    }

    @Override
    public double distanceTo(Point point) {
        final double x1 = point.getW()[0];
        final double y1 = point.getW()[1];

        return Math.abs(y1 - evaluateAt(x1));
    }


    @Override
    public QuadraticFunctionAxisDifference copy() {
        final QuadraticFunctionAxisDifference c = new QuadraticFunctionAxisDifference(getA(), getB(), getC());

        c.setCost(getCost());

        return c;
    }

    public double evaluateAt(final double x) {
        return getC() + x * getB() + x * x * getA();
    }

    public static Pair<QuadraticFunction, ArrayList<PointFunctionMatch>> quadraticFit(
            final List<Point> points,
            final double epsilon,
            final double minInlierRatio,
            final int nIterations) {
        //int nIterations = 1000;
        //double epsilon = 0.1;
        //double minInlierRatio = 0.5;
        QuadraticFunction qf = new QuadraticFunctionAxisDifference();

        if (points.size() < qf.getMinNumMatches())
            throw new RuntimeException("Not enough points for fitting a quadratic function. Candidates=" + points.size());

        final ArrayList<PointFunctionMatch> candidates = new ArrayList<>();
        final ArrayList<PointFunctionMatch> inliers = new ArrayList<>();

        for (final Point p : points)
            candidates.add(new PointFunctionMatch(p));

        try {
            System.out.println("nIterations=" + nIterations + ", epsilon=" + epsilon + ", minInlierRatio=" + minInlierRatio);

            qf.filterRansac(candidates, inliers, nIterations, epsilon, minInlierRatio, qf.getMinNumMatches());

            if (inliers.size() < qf.getMinNumMatches())
                throw new RuntimeException("Couldn't fix quadratic function. Candidates=" + candidates.size() + ", inliers=" + inliers.size());

            //qf.fit(inliers);
        } catch (Exception exc) {
            exc.printStackTrace();
        }

        double zMin = Double.MAX_VALUE;
        double zMax = -Double.MAX_VALUE;

        double avgError = 0;
        double maxError = 0;

        for (final PointFunctionMatch p : inliers) {
            p.apply(qf);
            final double distance = p.getDistance();

            // x is z, y is intensity
            zMin = Math.min(zMin, p.getP1().getL()[0]);
            zMax = Math.max(zMax, p.getP1().getL()[0]);

            avgError += distance;
            maxError = Math.max(maxError, distance);

            //System.out.println( p.getP1().getL()[ 0 ] + ", " +  p.getP1().getL()[ 1 ] + ", " + polyFunc(p.getP1().getL()[ 0 ], qf) );
        }

        System.out.println("candidates=" + candidates.size() + ", inliers=" + inliers.size() + ", avg err=" + (avgError / inliers.size()) + ", max error=" + maxError + ", zMin=" + zMin + ", zMax=" + zMax + ", " + qf);

        return new ValuePair<>(qf, inliers);
    }

    // return y for y = a*x*x + b*x + c
    public static double polyFunc(final double x, final QuadraticFunction f) {
        return f.getC() + x * f.getB() + x * x * f.getA();
    }

}

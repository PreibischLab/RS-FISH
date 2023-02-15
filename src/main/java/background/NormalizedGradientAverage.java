/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2023 RS-FISH developers.
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
package background;

import java.util.ArrayList;
import java.util.LinkedList;

import net.imglib2.util.Util;

import gradient.Gradient;

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

/*
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2024 RS-FISH developers.
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
package util.localization;

/**
 * A dummy {@link FunctionFitter} that simply skips the fitting process and leaves
 * the starting estimate untouched. 
 * <p>
 * Use this when you want to rely solely on 
 * {@link StartPointEstimator} results and skip the extra curve fitting step. 
 * 
 * @author Jean-Yves Tinevez - 2013
 */
public class DummySolver implements FunctionFitter {

	@Override
	public void fit(double[][] x, double[] y, double[] a, FitFunction f) throws Exception {
		return;
	}
	
	@Override
	public String toString() {
		return "Dummy curve fitting algorithm";
	}

}

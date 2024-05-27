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
 * Interface for fitting-target functions.
 * <p>
 * Functions defined here are scalar, multi-dimensional ones. 
 * We impose that they can return a gradient, so as to use various curve-fitting scheme.
 * </p>
 * 
 *
 */
public interface FitFunction {

	/**
	 * Evaluates this function at point <code>x</code>. The function is
	 * otherwise defined over an array of parameters <code>a</code>, that
	 * is the target of the fitting procedure.
	 * @param x  the multidimensional to evaluate the function at
	 * @param a  the set of parameters that defines the function
	 * @return  a double value, the function evaluated at <code>x</code>
	 *  
	 */
	public double val(double[] x, double[] a);

	/**
	 * Evaluates the gradient value of the function, taken with respect to the 
	 * <code>ak</code><sup>th</sup> parameter, evaluated at point <code>x</code>.
	 * @param x  the point to evaluate the gradient at
	 * @param a  the set of parameters that defines the function
	 * @param ak the index of the parameter to compute the gradient 
	 * @return the kth component of the gradient <code>df(x,a)/da_k</code>
	 * @see #val(double[], double[])
	 */
	public double grad(double[] x, double[] a, int ak);

	/**
	 * Evaluates the hessian value of the function, taken with respect to the 
	 * <code>r</code><sup>th</sup> and <code>c</code><sup>th</sup> parameters, 
	 * evaluated at point <code>x</code>.
	 * @param x  the point to evaluate the gradient at
	 * @param a  the set of parameters that defines the function
	 * @param r the index of the first parameter to compute the gradient 
	 * @param c the index of the second parameter to compute the gradient 
	 * @return the <code>(r, c)</code> element of the hessian matrix <code>d²f(x,a)/(da_r da_c)</code>
	 * @see #val(double[], double[])
	 */
	public double hessian(double[] x, double[] a, int r, int c);

}

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
package anisotropy.parameters;

public class AParams {
	public static float defaultSigma = 1.5f;
	public static float defaultThreshold = 0.0050f;

	public static float defaultAnisotropy = 1.0f;

	// RANSAC parameters
	// current value
	boolean RANSAC;
	float maxError, inlierRatio;
	int supportRadius;

	// DoG parameters
	// current
	float sigma, threshold;

	//
	float anisotropy;

	public AParams() {
		setSigmaDog(defaultSigma);
		setThresholdDoG(defaultThreshold);
		setAnisotropy(defaultAnisotropy);
	}

	public void printParams() {
		System.out.println("SigmaDoG      : " + sigma);
		System.out.println("ThresholdDoG  : " + threshold);
		System.out.println("Anisotropy    : " + anisotropy);
	}

	public void printDefaultParams() {
		System.out.println("DSigmaDoG      : " + defaultSigma);
		System.out.println("DThresholdDoG  : " + defaultThreshold);
		System.out.println("DAnisotropy  : " + defaultAnisotropy);
	}

	// getters
	// Difference of Gaussians
	public float getSigmaDoG() {
		return sigma;
	}

	public float getThresholdDoG() {
		return threshold;
	}

	// RANSAC
	public boolean getRANSAC() {
		return RANSAC;
	}

	public float getMaxError() {
		return maxError;
	}

	public float getInlierRatio() {
		return inlierRatio;
	}

	public int getSupportRadius() {
		return supportRadius;
	}

	//
	public float getAnisotropy() {
		return anisotropy;
	}

	/*
	 * back up the default values
	 */
	public void setDefaultValues() {
		defaultSigma = sigma;
		defaultThreshold = threshold;
		defaultAnisotropy = anisotropy;
	}

	// to be used by the listeners
	public void setSigmaDog(float sigmaDog) {
		this.sigma = sigmaDog;
	}

	public void setThresholdDoG(float threshold) {
		this.threshold = threshold;
	}

	// RANSAC
	public void setRANSAC(boolean RANSAC) {
		this.RANSAC = RANSAC;
	}

	public void setMaxError(float maxError) {
		this.maxError = maxError;
	}

	public void setInlierRatio(float inlierRatio) {
		this.inlierRatio = inlierRatio;
	}

	public void setSupportRadius(int supportRadius) {
		this.supportRadius = supportRadius;
	}

	//
	public void setAnisotropy(float anisotropy) {
		this.anisotropy = anisotropy;
	}

}

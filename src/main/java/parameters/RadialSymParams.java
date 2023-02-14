/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2023 Developers.
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
package parameters;

import java.io.Serializable;

import compute.RadialSymmetry.Ransac;
import gui.interactive.HelperFunctions;

public class RadialSymParams implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9045686244206165151L;

	public static String[] modeChoice = new String[] { "Interactive", "Advanced" };
	public static String[] ransacChoice = new String[] { "No RANSAC", "RANSAC", "Multiconsensus RANSAC" };

	final public static String[] bsMethods = new String[] { "No background subtraction", "Mean", "Median", "RANSAC on Mean", "RANSAC on Median" };
	final public static String[] intensityMethods = new String[] { "Linear Interpolation", "Gaussian fit (on inlier pixels)", "Integrate spot intensities (on candidate pixels)" };

	public static boolean defaultAutoMinMax = true;
	public static double defaultMin = 0;
	public static double defaultMax = 255;

	public static double defaultIntensityThreshold = 0.0;
	public static float defaultSigma = 1.5f;
	public static float defaultThreshold = 0.007f;

	public static float defaultMaxError = 1.5f;
	public static float defaultInlierRatio = (float) (10.0 / 100.0);
	public static int defaultSupportRadius = 3;

	public static float defaultBsInlierRatio = (float) (75.0 / 100.0);
	public static float defaultBsMaxError = 0.05f;
	public static int defaultBsMethodChoice = 0;

	public static int defaultRANSACChoice = 1;
	public static double defaultAnisotropy = 1.0f;
	public static boolean defaultUseAnisotropyForDoG = true;

	public static int defaultIntensityMethod = 0;
	//public static boolean defaultGaussFitLocation = false;
	public static int defaultMode = 0;
	public static int defaultImg = 0;

	public static boolean defaultAddToROIManager = false;

	public static String defaultResultsFilePath = "";

	// only used for DoG
	public int numThreads = Runtime.getRuntime().availableProcessors();

	// steps per octave for DoG
	public static int defaultSensitivity = 4;

	// multiconsensus options
	public static int defaultMinNumInliers = 20;
	public static double defaultNTimesStDev1 = 8.0;
	public static double defaultNTimesStDev2 = 6.0;

	// min/max
	public boolean autoMinMax = defaultAutoMinMax;
	public double min = Double.NaN;
	public double max = Double.NaN;

	// RANSAC parameters
	// current value
	public Ransac RANSAC() { return Ransac.values()[ ransacSelection ]; }
	public int ransacSelection = defaultRANSACChoice;
	public float maxError = defaultMaxError, inlierRatio = defaultInlierRatio;
	public int supportRadius = defaultSupportRadius;

	// Background Subtraction parameters
	// current values
	public float bsMaxError = defaultBsMaxError, bsInlierRatio = defaultInlierRatio;
	public int bsMethod = defaultBsMethodChoice;

	// DoG parameters
	// current
	public float sigma = defaultSigma, threshold = defaultThreshold;

	// intensity threshold
	public double intensityThreshold = defaultIntensityThreshold;

	// Z-scaling anisotropy calculation
	public double anisotropyCoefficient = defaultAnisotropy;
	public boolean useAnisotropyForDoG = defaultUseAnisotropyForDoG;

	// use gauss fit
	public int intensityMethod = defaultIntensityMethod;
	//public boolean gaussFitLocation = defaultGaussFitLocation;

	// multiconsensus options
	public int minNumInliers = defaultMinNumInliers;
	public double nTimesStDev1 = defaultNTimesStDev1, nTimesStDev2 = defaultNTimesStDev2;

	// ROI manager
	public boolean addToROIManager = defaultAddToROIManager;

	// advanced output
	public String resultsFilePath = defaultResultsFilePath;

	public void printParams() { printParams(true); }
	public void printParams( final boolean printIntensityThreshold ) {
		HelperFunctions.log("SigmaDoG               : " + sigma);
		HelperFunctions.log("ThresholdDoG           : " + threshold);
		HelperFunctions.log("anisotropyCoefficient  : " + anisotropyCoefficient);
		HelperFunctions.log("useAnisotropyForDoG    : " + useAnisotropyForDoG);
		HelperFunctions.log("RANSAC                 : " + RANSAC() );
		HelperFunctions.log("MaxError               : " + maxError);
		HelperFunctions.log("InlierRatio            : " + inlierRatio);
		HelperFunctions.log("supportRadius          : " + supportRadius);
		HelperFunctions.log("Intensity computation  : " + intensityMethods[ intensityMethod ]);
		//HelperFunctions.log("GaussFitLocation       : " + gaussFitLocation);
		if ( printIntensityThreshold )
			HelperFunctions.log("intensityThreshold     : " + intensityThreshold);
		HelperFunctions.log("min intensity          : " + min);
		HelperFunctions.log("max intensity          : " + max);
		HelperFunctions.log("autoMinMax             : " + autoMinMax);
		HelperFunctions.log("resultsFilePath        : " + resultsFilePath);

		if ( ransacSelection == 2 )
		{
			HelperFunctions.log("minNumInliers          : " + minNumInliers);
			HelperFunctions.log("nTimesStDev1           : " + nTimesStDev1);
			HelperFunctions.log("nTimesStDe             : " + nTimesStDev2);
		}

		HelperFunctions.log("bsMethod               : " + bsMethods[ bsMethod ]);
		HelperFunctions.log("bsMaxError             : " + bsMaxError);
		HelperFunctions.log("bsInlierRatio          : " + bsInlierRatio);

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
	public Ransac getRANSAC() {
		return RANSAC();
	}

	public int getRANSACIndex() {
		return RANSAC().ordinal();
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

	public double getAnisotropyCoefficient() {
		return anisotropyCoefficient;
	}
	
	public int getIntensityMethod() {
		return intensityMethod;
	}

//	public boolean getGaussFitLocation() {
//		return gaussFitLocation;
//	}

	// background subtraction
	// "No background subtraction", "Mean", "Median", "RANSAC on Mean", "RANSAC
	// on Median" };
	public int getBsMethod() {
		return bsMethod;
	}

	public float getBsMaxError() {
		return bsMaxError;
	}

	public float getBsInlierRatio() {
		return bsInlierRatio;
	}

	/**
	 * set the default values
	 */
	public void setDefaultValuesFromInteractive() {
		defaultSigma = sigma;
		defaultThreshold = threshold;

		defaultRANSACChoice = RANSAC().ordinal();
		defaultMaxError = maxError;
		defaultInlierRatio = inlierRatio;
		defaultSupportRadius = supportRadius;

		defaultBsInlierRatio = bsInlierRatio;
		defaultBsMaxError = bsMaxError;
		defaultBsMethodChoice = bsMethod;
		
		defaultAnisotropy = anisotropyCoefficient;
	}

	// to be used by the listeners
	public void setSigmaDog(float sigmaDog) {
		this.sigma = sigmaDog;
	}

	public void setThresholdDog(float threshold) {
		this.threshold = threshold;
	}

	public void setRANSAC(int ransacChoice) {
		this.ransacSelection = ransacChoice;
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

	public void setAnisotropyCoefficient(double anisotropyCoefficient) {
		this.anisotropyCoefficient = anisotropyCoefficient;
	}

	// background subtraction
	// "No background subtraction", "Mean", "Median", "RANSAC on Mean", "RANSAC
	// on Median" };
	public void setBsMethod(final int bsMethod) {
		this.bsMethod = bsMethod;
	}

	public void setBsMaxError(float bsMaxError) {
		this.bsMaxError = bsMaxError;
	}

	public void setBsInlierRatio(float bsInlierRatio) {
		this.bsInlierRatio = bsInlierRatio;
	}

}

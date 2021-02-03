package parameters;

import compute.RadialSymmetry.Ransac;

public class RadialSymParams {

	public static String[] modeChoice = new String[] { "Interactive", "Advanced" };
	public static String[] ransacChoice = new String[] { "No RANSAC", "RANSAC", "Multiconsensus RANSAC" };

	final public static String[] bsMethods = new String[] { "No background subtraction", "Mean", "Median", "RANSAC on Mean", "RANSAC on Median" };

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

	public static boolean defaultGaussFit = false;
	public static int defaultMode = 0;
	public static int defaultImg = 0;

	public static boolean defaultVisualizeDetections = true;
	public static boolean defaultVisualizeInliers = true;

	public static String defaultResultsFilePath = "";

	// steps per octave for DoG
	public static int defaultSensitivity = 4;

	// multiconsensus options
	public static int defaultMinNumInliers = 20;
	public static double defaultNTimesStDev1 = 8.0;
	public static double defaultNTimesStDev2 = 6.0;

	// RANSAC parameters
	// current value
	public Ransac RANSAC = Ransac.values()[ defaultRANSACChoice ];
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
	boolean gaussFit = defaultGaussFit;

	// multiconsensus options
	public int minNumInliers = defaultMinNumInliers;
	public double nTimesStDev1 = defaultNTimesStDev1, nTimesStDev2 = defaultNTimesStDev2;

	// advanced output
	public String resultsFilePath = defaultResultsFilePath;

	public void printParams() {
		System.out.println("SigmaDoG      : " + sigma);
		System.out.println("ThresholdDoG  : " + threshold);
		System.out.println("RANSAC        : " + RANSAC );
		System.out.println("MaxError      : " + maxError);
		System.out.println("InlierRatio   : " + inlierRatio);
		System.out.println("supportRadius : " + supportRadius);
		System.out.println("GaussFit      : " + gaussFit);
	}

	public void printDefaultParams() {
		System.out.println("DSigmaDoG      : " + defaultSigma);
		System.out.println("DThresholdDoG  : " + defaultThreshold);
		System.out.println("DRANSAC        : " + Ransac.values()[ defaultRANSACChoice ]);
		System.out.println("DMaxError      : " + defaultMaxError);
		System.out.println("DInlierRatio   : " + defaultInlierRatio);
		System.out.println("DSupportRadius : " + defaultSupportRadius);
		System.out.println("DGaussFit      : " + defaultGaussFit);
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
		return RANSAC;
	}

	public int getRANSACIndex() {
		return RANSAC.ordinal();
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
	
	public boolean getGaussFit() {
		return gaussFit;
	}

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

		defaultRANSACChoice = RANSAC.ordinal();
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

	// RANSAC
	public void setRANSAC(Ransac ransac) {
		this.RANSAC = ransac;
	}

	public void setRANSAC(int ransacChoice) {
		this.RANSAC = Ransac.values()[ ransacChoice ];
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
	
	public void setGaussFit(boolean gaussFit) {
		this.gaussFit = gaussFit;
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

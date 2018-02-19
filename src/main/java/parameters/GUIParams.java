package parameters;

public class GUIParams {
	final public static String[] bsMethods = new String[] { "No background subtraction", "Mean", "Median",
			"RANSAC on Mean", "RANSAC on Median" };

	public static float defaultSigma = 1.5f;
	public static float defaultThreshold = 0.0033f;

	public static float defaultMaxError = 0.5034f;
	public static float defaultInlierRatio = (float) (37.0 / 100.0);
	public static int defaultSupportRadius = 3;

	public static float defaultBsInlierRatio = (float) (75.0 / 100.0);
	public static float defaultBsMaxError = 0.05f;
	public static String defaultBsMethod = "No background subtraction";

	public static boolean defaultRANSAC = true;
	public static float defaultAnisotropy = 1.0f;

	public static String defaultRoiFolder = "";

	// RANSAC parameters
	// current value
	boolean RANSAC;
	float maxError, inlierRatio;
	int supportRadius;

	// Background Subtraction parameters
	// current values
	float bsMaxError, bsInlierRatio;
	String bsMethod;

	// DoG parameters
	// current
	float sigma, threshold;

	// Z-scaling anisotropy calculation
	float anisotropyCoefficient;

	// path to the folder with ROI's for images
	String roiFolder;

	public GUIParams() {
		setSigmaDog(defaultSigma);
		setThresholdDoG(defaultThreshold);
		setRANSAC(defaultRANSAC);
		setMaxError(defaultMaxError);
		setInlierRatio(defaultInlierRatio);
		setSupportRadius(defaultSupportRadius);
		// anisotropy in z
		setAnisotropyCoefficient(defaultAnisotropy);
		// what do you do with this values here
		setBsMethod(defaultBsMethod);
		setBsMaxError(defaultBsMaxError);
		setBsInlierRatio(defaultBsInlierRatio);
		//
		setRoiFolder(defaultRoiFolder);
	}

	public void printParams() {
		System.out.println("SigmaDoG      : " + sigma);
		System.out.println("ThresholdDoG  : " + threshold);
		System.out.println("RANSAC        : " + RANSAC);
		System.out.println("MaxError      : " + maxError);
		System.out.println("InlierRatio   : " + inlierRatio);
		System.out.println("supportRadius : " + supportRadius);
		System.out.println("RoiFolder     : " + roiFolder);
	}

	public void printDefaultParams() {
		System.out.println("DSigmaDoG      : " + defaultSigma);
		System.out.println("DThresholdDoG  : " + defaultThreshold);
		System.out.println("DRANSAC 	   : " + defaultRANSAC);
		System.out.println("DMaxError      : " + defaultMaxError);
		System.out.println("DInlierRatio   : " + defaultInlierRatio);
		System.out.println("DSupportRadius : " + defaultSupportRadius);
		System.out.println("DRoiFolder     : " + defaultRoiFolder);
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

	public float getAnisotropyCoefficient() {
		return anisotropyCoefficient;

	}

	// background subtraction
	// "No background subtraction", "Mean", "Median", "RANSAC on Mean", "RANSAC
	// on Median" };
	public String getBsMethod() {
		return bsMethod;
	}

	public float getBsMaxError() {
		return bsMaxError;
	}

	public float getBsInlierRatio() {
		return bsInlierRatio;
	}

	public String getRoiFolder() {
		return roiFolder;
	}

	/**
	 * back up the default values
	 */
	public void setDefaultValues() {
		defaultSigma = sigma;
		defaultThreshold = threshold;

		defaultRANSAC = RANSAC;
		defaultMaxError = maxError;
		defaultInlierRatio = inlierRatio;
		defaultSupportRadius = supportRadius;

		defaultBsInlierRatio = bsInlierRatio;
		defaultBsMaxError = bsMaxError;
		defaultBsMethod = bsMethod;

		defaultRoiFolder = roiFolder;
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

	public void setAnisotropyCoefficient(float anisotropyCoefficient) {
		this.anisotropyCoefficient = anisotropyCoefficient;

	}

	// background subtraction
	// "No background subtraction", "Mean", "Median", "RANSAC on Mean", "RANSAC
	// on Median" };
	public void setBsMethod(final String bsMethod) {
		this.bsMethod = bsMethod;
	}

	public void setBsMaxError(float bsMaxError) {
		this.bsMaxError = bsMaxError;
	}

	public void setBsInlierRatio(float bsInlierRatio) {
		this.bsInlierRatio = bsInlierRatio;
	}

	public void setRoiFolder(String roiFolder) {
		this.roiFolder = roiFolder;
	}

}

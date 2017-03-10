package parameters;

public abstract class GUIParams
{
	final public static String [] bsMethods = new String []{ "No background subtraction", "Mean", "Median", "RANSAC on Mean", "RANSAC on Median" };

	public static float defaultSigma = 5.0f;
	public static float defaultThreshold = 0.03f;
	
	public static float defaultMaxError = 3.0f;
	public static float defaultInlierRatio = (float) (75.0 / 100.0);
	public static int defaultSupportRadius = 5;
	
	public static float defaultBSInlierRatio = (float) (75.0 / 100.0);
	public static float defaultBSMaxError = 0.05f;
	public static int defaultBSMethod = 0;

	public float sigmaDoG();
	public float thresholdDoG();

	public int bsMethod(); //  "No background subtraction", "Mean", "Median", "RANSAC on Mean", "RANSAC on Median" };
	public float bsMaxErrorRANSAC();
	public float bsInlierRatioRANSAC();

	public float maxErrorRANSAC();
	public float inlierRationRANSAC();
	public int supportRadiusRANSAC();
}

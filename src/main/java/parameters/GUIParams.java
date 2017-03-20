package parameters;

public class GUIParams
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

	// RANSAC parameters
	// current value
	float maxError, inlierRatio;
	int supportRadius;

	// Background Subtraction parameters 
	// current values 
	float bsMaxError, bsInlierRatio;
	int bsMethod;

	// DoG parameters
	// current
	float sigma, threshold;

	public GUIParams()
	{
		//setSigmaDoG( defaultSigma );
		// ...
	}

	public float sigmaDoG();
	public float thresholdDoG();

	public int bsMethod(); //  "No background subtraction", "Mean", "Median", "RANSAC on Mean", "RANSAC on Median" };
	public float bsMaxErrorRANSAC();
	public float bsInlierRatioRANSAC();

	public float maxErrorRANSAC();
	public float inlierRationRANSAC();
	public int supportRadiusRANSAC();

	public void setDefaultValues() {
		// TODO Auto-generated method stub
		defaultSigma = sigma;
		//...
	}

	// set methods (to be used by the listeners)
	// public void setSigmaDog()
	// ...
}

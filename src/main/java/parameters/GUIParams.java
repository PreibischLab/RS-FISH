package parameters;

public class GUIParams
{
	final public static String [] bsMethods = new String []{ "No background subtraction", "Mean", "Median", "RANSAC on Mean", "RANSAC on Median" };

	public static float defaultSigma = 5.0f;
	public static float defaultThreshold = 0.03f;
	
	public static float defaultMaxError = 3.0f;
	public static float defaultInlierRatio = (float) (75.0 / 100.0);
	public static int defaultSupportRadius = 5;
	
	public static float defaultBsInlierRatio = (float) (75.0 / 100.0);
	public static float defaultBsMaxError = 0.05f;
	public static int defaultBsMethod = 0;

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
		setSigmaDog(sigma);
		setThresholdDoG(threshold);
		setMaxError(maxError);		
		setInlierRatio(inlierRatio);
		setSupportRadius(supportRadius);
		// what do you do with this values here
		setBsMethod(bsMethod);
		setBsMaxError(bsMaxError);
		setBsInlierRatio(bsInlierRatio);
	}

	
	// getters
	// Difference of Gaussians
	public float getSigmaDoG(){
		return sigma;
	}

	public float getThresholdDoG(){
		return threshold;
	}

	// RANSAC 
	public float getMaxError(){
		return maxError;
	}
	public float getInlierRatio(){
		return inlierRatio;
	}
	public int getSupportRadius(){
		return supportRadius;
	}
	
	// background subtraction 
	// "No background subtraction", "Mean", "Median", "RANSAC on Mean", "RANSAC on Median" };
	public int getBsMethod(){
		return bsMethod;
	}
	
	public float getBsMaxError(){
		return bsMaxError;
	}
	
	public float getBsInlierRatio(){
		return bsInlierRatio;	
	}
	
	public void setDefaultValues() {
		defaultSigma = sigma;	
		defaultThreshold = threshold;
		
		defaultMaxError = maxError;
		defaultInlierRatio = inlierRatio;
		defaultSupportRadius = supportRadius;
		
		defaultBsInlierRatio = bsInlierRatio;
		defaultBsMaxError = bsMaxError;
		defaultBsMethod = bsMethod;
	}

	// to be used by the listeners
	public void setSigmaDog(float sigmaDog){
		this.sigma = sigmaDog;
	}
	
	public void setThresholdDoG(float threshold){
		this.threshold = threshold;
	}

	// RANSAC 
	public void setMaxError(float maxError){
		this.maxError = maxError;
	}
	
	public void setInlierRatio(float inlierRatio){
		this.inlierRatio = inlierRatio;
	}
	public void setSupportRadius(int supportRadius){
		this.supportRadius = supportRadius;
	}
	
	// background subtraction 
	// "No background subtraction", "Mean", "Median", "RANSAC on Mean", "RANSAC on Median" };
	public void setBsMethod(int bsMethod){
		this.bsMethod = bsMethod;
	}
	
	public void setBsMaxError(float bsMaxError){
		this.bsMaxError = bsMaxError;
	}
	
	public void setBsInlierRatio(float bsInlierRatio){
		this.bsInlierRatio = bsInlierRatio;	
	}
	
	
	
	
}

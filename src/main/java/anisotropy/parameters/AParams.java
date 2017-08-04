package anisotropy.parameters;

public class AParams {

	public static float defaultSigma = 1.5f;
	public static float defaultThreshold = 0.0001f;
	
	// RANSAC parameters
	// current value
	boolean RANSAC;
	float maxError, inlierRatio;
	int supportRadius;
	
	// DoG parameters
	// current
	float sigma, threshold;
	
	public AParams() {
		setSigmaDog(defaultSigma);
		setThresholdDoG(defaultThreshold);
	}
	
	public void printParams(){
		System.out.println("SigmaDoG      : " + sigma);
		System.out.println("ThresholdDoG  : " + threshold);
	}
	
	public void printDefaultParams(){
		System.out.println("DSigmaDoG      : " + defaultSigma);
		System.out.println("DThresholdDoG  : " + defaultThreshold);
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
	public boolean getRANSAC(){
		return RANSAC;
	}
	public float getMaxError(){
		return maxError;
	}
	public float getInlierRatio(){
		return inlierRatio;
	}
	public int getSupportRadius(){
		return supportRadius;
	}
	
	/*
	 * back up the default values
	 * */
	public void setDefaultValues() {
		defaultSigma = sigma;	
		defaultThreshold = threshold;
	}
	
	// to be used by the listeners
	public void setSigmaDog(float sigmaDog){
		this.sigma = sigmaDog;
	}
	
	public void setThresholdDoG(float threshold){
		this.threshold = threshold;
	}
	
	// RANSAC 
	public void setRANSAC(boolean RANSAC){
		this.RANSAC = RANSAC;
	}
	
	public void setMaxError(float maxError){
		this.maxError = maxError;
	}
	
	public void setInlierRatio(float inlierRatio){
		this.inlierRatio = inlierRatio;
	}
	public void setSupportRadius(int supportRadius){
		this.supportRadius = supportRadius;
	}
	
	
	
	
	
}

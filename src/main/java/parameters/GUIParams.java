package parameters;

public interface GUIParams
{
	public float sigmaDoG();
	public float thresholdDoG();

	public int bsMethod(); //  "No background subtraction", "Mean", "Median", "RANSAC on Mean", "RANSAC on Median" };
	public float bsMaxErrorRANSAC();
	public float bsInlierRatioRANSAC();

	public float maxErrorRANSAC();
	public float inlierRationRANSAC();
	public int supportRadiusRANSAC();
}

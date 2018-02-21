package paper.test.radialsymmetry;

import parameters.GUIParams;
import parameters.RadialSymmetryParameters;

public class InputParams {
	// stores input parameters for images and csv files
	public static String path = "/Users/kkolyva/Desktop/2018-02-21-paper-radial-symmetry-test/";
	public static String imgName = "test-2D-image";
	public static String posName = "test-2D-pos";
	public static long [] dims = new long[] {512, 512};
	public static double [] sigma = new double[] {3, 3}; 
	public static int numDimensions = dims.length;
	public static long numSpots = 800;
	public static int seed = 42;
	public static boolean padding = false;
	
	// generated 2D image test
	public static RadialSymmetryParameters setParameters(int numDimensions) {
		// TODO: move the inits to the separate file
		// this parameters should come from the manual adjustment
		// DoG parameters
		float sigmaDog = 1.5f; 
		float threshold = 0.015f;
		
		// RANSAC parameters
		boolean RANSAC = true;
		int supportRadius = 3; // this one I know
		float maxError = 0.3f; 
		float inlierRatio = 0.60f;

		// Background Subtraction parameters
		float bsMaxError = 0.05f;
		float bsInlierRatio = 0.75f;
		String bsMethod = "No background subtraction";

		// Gauss Fit over intensities
		boolean gaussFit = false;
		
		// set the parameters from the defaults
		final GUIParams params = new GUIParams();
		
		params.setRANSAC(RANSAC);
		params.setMaxError(maxError);
		params.setInlierRatio(inlierRatio);
		params.setSupportRadius(supportRadius);
		params.setBsMaxError(bsMaxError);
		params.setBsInlierRatio(bsInlierRatio);
		params.setBsMethod(bsMethod);
		params.setSigmaDog(sigmaDog);
		params.setThresholdDog(threshold);
		params.setGaussFit(gaussFit);
		
		// back up the parameter values to the default variables
		params.setDefaultValues();

		double [] calibration  = new double[numDimensions];
		for (int d = 0; d < numDimensions; d++)
			calibration[d] = 1;
		RadialSymmetryParameters rsm = new RadialSymmetryParameters(params, calibration);
		return rsm;
	}
}

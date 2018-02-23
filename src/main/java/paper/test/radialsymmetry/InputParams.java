package paper.test.radialsymmetry;

import parameters.GUIParams;
import parameters.RadialSymmetryParameters;

public class InputParams {
	// stores input parameters for images and csv files
	public static String path;
	public static String imgName;
	public static String posName;
	public static long [] dims;
	public static double [] sigma; 
	public static int numDimensions;
	public static long numSpots;
	public static int seed;
	public static boolean padding;

	// DoG parameters
	public static float sigmaDog; 
	public static float threshold;

	// RANSAC parameters
	public static boolean RANSAC;
	public static int supportRadius; // this one I know
	public static float maxError; 
	public static float inlierRatio;

	// Background Subtraction parameters
	public static float bsMaxError;
	public static float bsInlierRatio;
	public static String bsMethod;

	// Gauss Fit over intensities
	public static boolean gaussFit;

	// TODO: 
	// 1. make 4 different params initializers

	// TODO: 
	// changes the parameters to corresponding one
	public static void setParameters3D() {
		// input data params
		path = "/Users/kkolyva/Desktop/2018-02-21-paper-radial-symmetry-test/";
		imgName = "test-2D-image";
		posName = "test-2D-pos";
		dims = new long[] {512, 512};
		sigma = new double[] {3, 3}; 
		numDimensions = dims.length;
		numSpots = 800; // ? 
		seed = 42; 
		padding = false;
		// radial symmetry params
	} 

	// changes the parameters to corresponding one
	public static RadialSymmetryParameters setParametersMax2D() {
		// input data params
		path = "/Users/kkolyva/Desktop/2018-02-21-paper-radial-symmetry-test/";
		imgName = "test-2D-image";
		posName = "test-2D-pos";
		// TODO: doesb;t 
		// dims = new long[] {1024, 1024};
		// sigma = new double[] {3, 3}; 
		// numDimensions = dims.length;
		// numSpots = 800; // ? 
		// seed = 42; 
		// padding = false;
		
		// radial symmetry params
		// this parameters should come from the manual adjustment
		// DoG parameters
		sigmaDog = 1.0f; 
		threshold = 0.0120f;

		// RANSAC parameters
		RANSAC = true;
		supportRadius = 2; // this one I know
		maxError = 0.6f; 
		inlierRatio = 0.5f;

		// Background Subtraction parameters
		bsMaxError = 0.05f;
		bsInlierRatio = 0.75f;
		bsMethod = "No background subtraction";

		// Gauss Fit over intensities
		gaussFit = false;
		
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


	public static RadialSymmetryParameters setParameters2D() {
		// input data params
		path = "/Users/kkolyva/Desktop/2018-02-21-paper-radial-symmetry-test/";
		imgName = "test-2D-image";
		posName = "test-2D-pos";
		dims = new long[] {512, 512};
		sigma = new double[] {3, 3}; 
		numDimensions = dims.length;
		numSpots = 800;
		seed = 42;
		padding = false;

		// radial symmetry params
		// this parameters should come from the manual adjustment
		// DoG parameters
		sigmaDog = 1.5f; 
		threshold = 0.015f;

		// RANSAC parameters
		RANSAC = true;
		supportRadius = 3; // this one I know
		maxError = 0.3f; 
		inlierRatio = 0.60f;

		// Background Subtraction parameters
		bsMaxError = 0.05f;
		bsInlierRatio = 0.75f;
		bsMethod = "No background subtraction";

		// Gauss Fit over intensities
		gaussFit = false;
		
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

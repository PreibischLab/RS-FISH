package paper.test.radialsymmetry;


public class InputParams {
	// stores input parameters for images and csv files
	public static String path = "/Users/kkolyva/Desktop/";
	public static String imgName = "test-2D-image";
	public static String posName = "test-2D-pos";
	public static long [] dims = new long[] {512, 512};
	public static double [] sigma = new double[] {3, 3}; 
	public static int numDimensions = dims.length;
	public static long numSpots = 800;
	public static int seed = 42;
	public static boolean padding = false;
}

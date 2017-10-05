package test;

import java.io.File;

import gui.Radial_Symmetry;
import ij.ImageJ;
import ij.ImagePlus;
import ij.io.Opener;


public class RadialSymmetryTests {
	public static void testRSGaussianFit(){
		File path = new File("/media/milkyklim/Samsung_T3/2017-06-26-radial-symmetry-test/Simulated_3D.tif");

		if (!path.exists())
			throw new RuntimeException("'" + path.getAbsolutePath() + "' doesn't exist.");

		new ImageJ();
		System.out.println("Opening '" + path + "'");

		ImagePlus imp = new Opener().openImage(path.getAbsolutePath());

		if (imp == null)
			throw new RuntimeException("image was not loaded");

		imp.show();
		imp.setSlice(121);

		// new Radial_Symmetry().run(new String());
	}
	
	
	public static void main(String[] args){
		testRSGaussianFit();
		System.out.println("Doge!");
	}
	
}



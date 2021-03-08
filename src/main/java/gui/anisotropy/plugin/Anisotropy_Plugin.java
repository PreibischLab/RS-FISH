package gui.anisotropy.plugin;

import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import anisotropy.parameters.AParams;
import gui.anisotropy.AnisitropyCoefficient;
import gui.interactive.HelperFunctions;
import ij.ImagePlus;
import parameters.RadialSymParams;

@Plugin(type = Command.class, menuPath = "Plugins>RS-FISH>Tools>Calculate Anisotropy Coefficient")
public class Anisotropy_Plugin implements Command {

	public static String[] paramChoice = new String[] { "Gauss Fit", "Radial Symmetry" };
	public static int defaultParam = 0;
	public static int defaultImg = 0;

	@Parameter(autoFill=false, label="Image")
	ImagePlus imagePlus;

	@Parameter(choices={ "Gauss Fit", "Radial Symmetry" }, label="Detection method")
	String paramType = paramChoice[defaultParam];
	
	@Parameter(visibility=ItemVisibility.INVISIBLE)
	LogService logService;

	@Override
	public void run() {
		float bestScale = 1.0f;
		AParams ap = new AParams();
		double [] minmax = HelperFunctions.calculateMinMax(imagePlus);
		AnisitropyCoefficient ac = new AnisitropyCoefficient(imagePlus, ap, paramType, minmax[0], minmax[1]);
		bestScale = (float) ac.calculateAnisotropyCoefficient();	
		// write bestScale somewhere
		ap.setAnisotropy(bestScale);
		// save default and calculated anisotropy coefficient
		RadialSymParams.defaultAnisotropy = bestScale; 
		ap.setDefaultValues();
		logService.info("Anisotropy coefficient: " + bestScale);
	}

	public static void main(String[] args)
	{
		net.imagej.ImageJ ij = new net.imagej.ImageJ();
		ij.launch( "/Users/spreibi/Documents/BIMSB/Publications/radialsymmetry/Poiss_300spots_bg_200_0_I_10000_0_img0.tif" );
		ij.command().run(Anisotropy_Plugin.class, true);

		// for the historical reasons
		System.out.println("DOGE!");
	}


}

package gui.anisotropy.plugin;

import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import anisotropy.parameters.AParams;
import gui.anisotropy.AnisitropyCoefficient;
import gui.radial.symmetry.interactive.HelperFunctions;
import ij.ImagePlus;
import radial.symmetry.parameters.GUIParams;

@Plugin(type = Command.class, menuPath = "Plugins>Radial Symmetry Localization>Calculate Anisotropy Coefficient")
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
		GUIParams.defaultAnisotropy = bestScale; 
		ap.setDefaultValues();
		logService.info("Anisotropy coefficient: " + bestScale);
	}

	public static void main(String[] args)
	{
		// for the historical reasons
		System.out.println("DOGE!");
	}


}

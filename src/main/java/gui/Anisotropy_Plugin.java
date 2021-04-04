package gui;

import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import anisotropy.parameters.AParams;
import gui.anisotropy.AnisotropyCoefficient;
import gui.interactive.HelperFunctions;
import ij.IJ;
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

		if ( imagePlus.getNFrames() > 1 )
		{
			IJ.log( "More than one frame detected, need a plain 3D image for Anisotropy detection (check Image>Properties)." );
			return;
		}

		if ( imagePlus.getNChannels() > 1 )
		{
			IJ.log( "More than one channel detected, need a plain 3D image for Anisotropy detection (check Image>Properties)." );
			return;
		}

		if ( imagePlus.getNSlices() <= 1 )
		{
			IJ.log( "Only one slice detected, need a 3D image for Anisotropy detection (check Image>Properties)." );
			return;
		}

		float bestScale = 1.0f;
		AParams ap = new AParams();
		double [] minmax = HelperFunctions.calculateMinMax(imagePlus);
		AnisotropyCoefficient ac = new AnisotropyCoefficient(imagePlus, ap, paramType, minmax[0], minmax[1]);
		if ( ac.wasCanceled() )
			return;
		bestScale = (float) ac.calculateAnisotropyCoefficient();

		// save default and calculated anisotropy coefficient
		RadialSymParams.defaultAnisotropy = bestScale;
		RadialSymParams.defaultSigma = ac.params.getSigmaDoG(); 
		RadialSymParams.defaultThreshold = ac.params.getThresholdDoG();

		IJ.log("Anisotropy coefficient: " + bestScale);
	}

	public static void main(String[] args)
	{
		net.imagej.ImageJ ij = new net.imagej.ImageJ();
		//ij.launch( "/Users/spreibi/Documents/BIMSB/Publications/radialsymmetry/Poiss_300spots_bg_200_0_I_10000_0_img0.tif" );
		ij.launch( "/Users/spreibi/Documents/BIMSB/Publications/radialsymmetry/N2_702_cropped_1620 (high SNR)_ch0.tif" );
		//ij.launch( "/Users/spreibi/Downloads/in_situ_HCR_tekt2_514_otogl_488_itln_546_spdf_647_zoom_2_no_DAPI_stage_13_embryo_1_cell_1.tif" );
		ij.command().run(Anisotropy_Plugin.class, true);

		// for the historical reasons
		System.out.println("DOGE!");
	}


}

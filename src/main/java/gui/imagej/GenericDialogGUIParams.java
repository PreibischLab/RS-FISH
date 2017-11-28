package gui.imagej;

import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import parameters.GUIParams;

// used for the advanced option of the initial dialog
@Plugin(type = Command.class, name = "Set parameters")
public class GenericDialogGUIParams extends ContextCommand {
	// TODO: This one use useless
	@Parameter(type = ItemIO.INPUT, visibility = ItemVisibility.INVISIBLE)
	private GUIParams guiParams;

	@Parameter(label = "Sigma")
	float sigma = GUIParams.defaultSigma;
	@Parameter(label = "Threshold")
	float threshold = GUIParams.defaultThreshold;

	@Parameter(label = "Support region radius")
	int supportRadius = GUIParams.defaultSupportRadius;

	@Parameter(label = "Inlier ratio")
	float inlierRatio = GUIParams.defaultInlierRatio;
	@Parameter(label = "Max error")
	float maxError = GUIParams.defaultMaxError;

	@Parameter(choices = { "No background subtraction", "Mean", "Median", "RANSAC on Mean",                      
			"RANSAC on Median" }, label = "Local background subtraction")
	String bsMethod = GUIParams.defaultBsMethod;

	@Parameter(label = "ROI folder")
	String roiFolder = GUIParams.defaultRoiFolder; // by default no roi folder
													// is used

	@Override
	public void run() {
		guiParams.setSigmaDog(sigma);
		guiParams.setThresholdDoG(threshold);
		guiParams.setSupportRadius(supportRadius);

		guiParams.setInlierRatio(inlierRatio);
		guiParams.setMaxError(maxError);
		guiParams.setBsMethod(bsMethod);

		guiParams.setRoiFolder(roiFolder);
		
		// also back up the defaults
		guiParams.setDefaultValues();
	}
}

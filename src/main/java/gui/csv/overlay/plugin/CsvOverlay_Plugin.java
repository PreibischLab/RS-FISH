package gui.csv.overlay.plugin;

import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ij.ImagePlus;

@Plugin(type = Command.class, menuPath = "Plugins>Radial Symmetry Localization>Show Overlay")
public class CsvOverlay_Plugin implements Command {
	
	/* >>> Pipeline:
	 * - load the image (grab the available one)
	 * - load the csv file
	 * - show the slider with the z value
	 * */
	
	@Parameter(autoFill=true, label="Image")
	ImagePlus imagePlus;
	
	@Parameter(label = "Path to the overlay file")
	String csvPath = "";
	
	@Parameter(visibility=ItemVisibility.INVISIBLE)
	LogService logService;
	
	@Override
	public void run() {
		System.out.println("DOGE!");
	}

}

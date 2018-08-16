package gui.csv.overlay.plugin;

import java.io.File;

import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import gui.csv.overlay.CsvOverlay;
import ij.ImagePlus;

@Plugin(type = Command.class, menuPath = "Plugins>Radial Symmetry Localization>Show Overlay")
public class CsvOverlay_Plugin implements Command {
	
	/* >>> Pipeline:
	 * - load the image (grab the available one)
	 * - load the csv file
	 * - show the slider with the z value
	 * */
	
	@Parameter(autoFill=true, label="Image")
	ImagePlus imp;
	
	@Parameter(label = "Path to the overlay file")
	File csvFile = new File("/Users/kkolyva/Desktop/2018-07-31-09-53-32-N2-all-results-together/csv-2/C1-N2_16.csv");
	
	@Parameter(visibility=ItemVisibility.INVISIBLE)
	LogService logService;
	
	@Override
	public void run() {
		
		// trigger only functions here
		CsvOverlay dummy= new CsvOverlay(imp, csvFile);
		dummy.test();
		
		System.out.println("DOGE!");
	}

}

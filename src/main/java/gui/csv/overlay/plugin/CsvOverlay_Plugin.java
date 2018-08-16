package gui.csv.overlay.plugin;

import java.io.File;

import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import gui.csv.overlay.CsvOverlay;
import ij.ImagePlus;

@Plugin(type = Command.class, menuPath = "Plugins>Radial Symmetry Localization>Show Overlay")
public class CsvOverlay_Plugin implements Command {
	
	@Parameter(label="Image", autoFill=false)
	ImagePlus imp;
	
	@Parameter(label = "Path to the overlay file", type=ItemIO.INPUT, autoFill=false)
	File csvFile = new File("/Users/kkolyva/Desktop/2018-08-16-12-20-11-csv-overlay-plugin-test/C1-N2_279.csv");
	
	@Parameter(visibility=ItemVisibility.INVISIBLE)
	LogService logService;
	
	@Override
	public void run() {
		
		// trigger only functions here
		CsvOverlay dummy = new CsvOverlay(imp, csvFile);
		// dummy.test();
		
		System.out.println("DOGE!");
	}

}

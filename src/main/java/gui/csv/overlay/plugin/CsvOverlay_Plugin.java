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

@Plugin(type = Command.class, menuPath = "Plugins>Radial Symmetry Localization>[Exp] Show Detections")
public class CsvOverlay_Plugin implements Command {

	@Parameter(label="Image", autoFill=false)
	ImagePlus imp;

	@Parameter(label = "Path to the overlay file", type=ItemIO.INPUT, autoFill=false)
	File csvFile = new File("");

	@Parameter(visibility=ItemVisibility.INVISIBLE)
	LogService logService;

	@Override
	public void run() {
		new CsvOverlay(imp, csvFile);
	}
}

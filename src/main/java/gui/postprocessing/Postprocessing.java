package gui.postprocessing;

import java.io.File;

import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.FileWidget;

import ij.ImagePlus;

@Plugin(type = Command.class, menuPath = "Plugins>Radial Symmetry Localization>Postprocessing")
public class Postprocessing extends ContextCommand{

	@Parameter(autoFill = false, label = "Nuclei mask:")
	ImagePlus imp;
	
	@Parameter(label = "Intron results:", style=FileWidget.OPEN_STYLE)
	File intronPath;
	
	@Parameter(label = "Exon results:", style=FileWidget.OPEN_STYLE)
	File exonPath;
	
	@Override
	public void run() {
		
	}

}

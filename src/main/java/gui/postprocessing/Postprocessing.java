package gui.postprocessing;

import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ij.ImagePlus;

@Plugin(type = Command.class, menuPath = "Plugins>Radial Symmetry Localization>Postprocessing")
public class Postprocessing extends ContextCommand{

	@Parameter(autoFill = false, label = "Nuclei mask:")
	ImagePlus imp;
	
	@Parameter(label = "Intron results:")
	String intronPath;
	
	@Parameter(label = "Exon results:")
	String exonPath;
	
	@Override
	public void run() {
		
	}

}

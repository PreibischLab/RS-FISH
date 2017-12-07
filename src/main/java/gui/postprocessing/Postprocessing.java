package gui.postprocessing;

import java.io.File;
import java.util.ArrayList;

import net.imglib2.img.Img;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.FileWidget;

import fit.Spot;
import gui.interactive.HelperFunctions;
import postprocessing.Colocalization;

@Plugin(type = Command.class, menuPath = "Plugins>Radial Symmetry Localization>Postprocessing")
public class Postprocessing extends ContextCommand {

	@Parameter(autoFill = false, label = "Nuclei mask:")
	Img<UnsignedByteType> img;

	@Parameter(label = "Intron results:", style = FileWidget.OPEN_STYLE)
	File intronPath = new File("/Users/kkolyva/Desktop/05.nd2 - C=2-1.csv");

	@Parameter(label = "Exon results:", style = FileWidget.OPEN_STYLE)
	File exonPath = new File("/Users/kkolyva/Desktop/05.nd2 - C=0-1.csv");

	@Parameter(label = "Error (px):", min = "0.0")
	float error = 0.5f;

	@Override
	public void run() {
		// read exonic probe
		ArrayList<Spot> exSpots = new ArrayList<>();
		ArrayList<Long> exTimePoint = new ArrayList<>();
		ArrayList<Long> exChannelPoint = new ArrayList<>();
		ArrayList<Float> exIntensity = new ArrayList<>();

		HelperFunctions.readCSV(exonPath.getAbsolutePath(), exSpots, exTimePoint, exChannelPoint, exIntensity);

		ArrayList<Spot> inSpots = new ArrayList<>();
		ArrayList<Long> inTimePoint = new ArrayList<>();
		ArrayList<Long> inChannelPoint = new ArrayList<>();
		ArrayList<Float> inIntensity = new ArrayList<>();

		HelperFunctions.readCSV(intronPath.getAbsolutePath(), inSpots, inTimePoint, inChannelPoint, inIntensity);

		System.out.println("in: " + inSpots.size());

		ArrayList<int[]> overlayedIndices = Colocalization.findColocalization(img, exSpots, inSpots, error);

		// for (int [] indices : overlayedIndices)
		// System.out.println(exSpots.get(indices[0]).getDoublePosition(0) + " "
		// + inSpots.get(indices[1]).getDoublePosition(0));

		System.out.println("The plugin triggered!");
	}
}

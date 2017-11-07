package roi.process;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import fit.Spot;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.Opener;
import ij.io.RoiDecoder;

public class RoiProcess {

	// returns spots in the given roi
	// takes one roi and one image
	public static ArrayList<Spot> processImage(ArrayList<Spot> allSpots, Roi roi) {
		ArrayList<Spot> returnSpots = new ArrayList<>();
		for (final Spot spot : allSpots) {
			double[] pos = spot.getCenter();
			if (roi.contains((int) pos[0], (int) pos[1])) {
				returnSpots.add(spot);
			}
		}
		return returnSpots;
	}

	public static void processRoi(ArrayList<Spot> allSpots, String roiFoder) {
		ArrayList<String> listRoiPath = new ArrayList<>();

		for (final File roiFile : new File(roiFoder).listFiles()) {
			if (roiFile.isFile()) {
				System.out.println(roiFile.getName());
				// TODO: grab the file - roi
				listRoiPath.add(roiFile.getAbsolutePath());
				final RoiDecoder roiDecoder = new RoiDecoder(roiFile.getAbsolutePath());
				// TODO: processImage()
				Roi roi = null;
				try {
					roi = roiDecoder.getRoi();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (roi != null)
					processImage(allSpots, roi);
				else {
					System.out.println("There was a problem reading roi: " + roiFile.getAbsolutePath());
					System.out.println("Spots were not filtered!");
				}
				// TODO: return spots as the result

			}
		}
	}

	// roi format is name as image + (-0, -1, -2, -3 )
	public static ArrayList<String> readRoi(String roiFoder) {
		ArrayList<String> listRoiPath = new ArrayList<>();
		for (final File roiFile : new File(roiFoder).listFiles()) {
			if (roiFile.isFile()) {
				System.out.println(roiFile.getName());
				listRoiPath.add(roiFile.getAbsolutePath());
			}
		}
		return listRoiPath;
	}

	public static void main(String[] args) {
		new ImageJ();

		ImagePlus imp = new Opener().openImage("/Users/kkolyva/Desktop/image.jpg");

		imp.show();

		// ImgLib2Util.openAs32Bit(new
		// File("/Users/kkolyva/Desktop/IMG_1458.tif"));
		final RoiDecoder roiDecoder = new RoiDecoder("/Users/kkolyva/Desktop/1281-1722.roi");

		Roi roi = null;
		try {
			roi = roiDecoder.getRoi();
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		// if(roi != null)
		// imp.setRoi(roi);

		// test the point
		System.out.print(roi.contains(1, 1) + " " + roi.contains(imp.getWidth() / 2, imp.getHeight() / 2));

	}
}
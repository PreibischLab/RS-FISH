package util;

import java.io.File;
import java.util.Collection;

import cluster.radial.symmetry.process.ImageData;
import cluster.radial.symmetry.process.ImageDataFull;

public class NotSoUsefulOutput {
	public static String toProgressString(long current, long total, String name) {
		return String.format("%d / %d: %s", current, total, name);
	}
	
	public static String toComplaintString(String className, String name) {
		return String.format("%s: %s is missing", className, name);
	}
	
	public static void printFiles(File [] files) {
		for (File f : files)
			System.out.println(f);
	}
	
	public static void printImageDataParameters(Collection<ImageData> imageData) {
		for (ImageData id : imageData) {
			String tmp = id.getDefects() ? "" : "out"; 
			System.out.println(String.format("Wavelength: %d, Type: %s, filename %s with%s defects", id.getLambda(), id.getType(), id.getFilename(), tmp));
		}
	}
	
	public static void printImageDataFullParameters(Collection<ImageDataFull> imageDataFull) {
		for (ImageDataFull id : imageDataFull) {
			
			String sChannels = "";
			for (String channel :id.getChannels().keySet()) 
				sChannels += channel + " ";
			String tmp = id.getDefects() ? "" : "out"; 
			System.out.println(String.format("Filename %s with%s defects and %stypes", id.getFilename(), tmp, sChannels));
		}
	}
}

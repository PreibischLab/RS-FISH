package util;


public class Utils {
	public static String getFullName(String channel, String filename, String ext) {
		return String.format("%s-%s%s", channel, filename, ext);
	}

	public static String getNameWithoutExt(String channel, String filename) {
		return String.format("%s-%s", channel, filename);
	}

}

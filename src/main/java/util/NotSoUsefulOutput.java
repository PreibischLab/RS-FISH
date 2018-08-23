package util;


public class NotSoUsefulOutput {
	public static String toProgressString(long current, long total, String name) {
		return String.format("%d / %d: %s", current, total, name);
	}
	
	public static String toComplaintString(String className, String name) {
		return String.format("%s: %s is missing", className, name);
	}
}

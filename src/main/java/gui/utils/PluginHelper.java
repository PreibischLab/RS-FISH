/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2024 RS-FISH developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package gui.utils;

import ij.gui.GenericDialog;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import net.imagej.patcher.HeadlessGenericDialog;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

public class PluginHelper {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public static final String[] GLOB_SPECIAL_CHARS = new String[]{"{", "}", "[", "]", "*", "?"};

    public static boolean isHeadless() {
        return GenericDialog.class.getSuperclass().equals(HeadlessGenericDialog.class);
    }

    public static List<File> getFilesFromPattern(String pattern, final long fileMinSize) {
        Pair<String, String> pAndp = splitIntoPathAndPattern(pattern, GLOB_SPECIAL_CHARS);

        String path = pAndp.getA();
        String justPattern = pAndp.getB();

        PathMatcher pm;
        try {
            pm = FileSystems.getDefault().getPathMatcher("glob:" +
                    ((justPattern.length() == 0) ? path : String.join("/", path, justPattern)));
        } catch (PatternSyntaxException e) {
            // malformed pattern, return empty list (for now)
            // if we do not catch this, we will keep logging exceptions e.g. while user is typing something like [0-9]
            return new ArrayList<>();
        }

        List<File> paths = new ArrayList<>();

        if (!new File(path).exists())
            return paths;

        int numLevels = justPattern.split("/").length;

        try {
            Files.walk(Paths.get(path), numLevels).filter(p -> pm.matches(p)).filter(t -> {
                // ignore directories
                if (Files.isDirectory(t))
                    return false;

                try {
                    return Files.size(t) > fileMinSize;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return false;
            })
                    .forEach(p -> paths.add(new File(p.toString())));

        } catch (IOException e) {

        }

        Collections.sort(paths);
        return paths;
    }

    public static boolean containsAny(String s, String... templates) {
        for (int i = 0; i < templates.length; i++)
            if (s.contains(templates[i]))
                return true;
        return false;
    }

    public static Pair<String, String> splitIntoPathAndPattern(String s, String... templates) {
        String[] subpaths = s.split(Pattern.quote(File.separator));
        ArrayList<String> path = new ArrayList<>();
        ArrayList<String> pattern = new ArrayList<>();
        boolean noPatternFound = true;

        for (int i = 0; i < subpaths.length; i++) {
            if (noPatternFound && !containsAny(subpaths[i], templates)) {
                path.add(subpaths[i]);
            } else {
                noPatternFound = false;
                pattern.add(subpaths[i]);
            }
        }
        String sPath = String.join("/", path);
        String sPattern = String.join("/", pattern);

        return new ValuePair<String, String>(sPath, sPattern);
    }

    public static List<String> getPattern(File file) {
        List<String> patterns = Arrays.asList(file.getName().split("[_-]")).stream().filter(s ->
                isNumeric(s)).collect(Collectors.toList());
        return patterns;
    }


    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public static String getLabelPattern(File file) {
        String file_name = file.getName();
        List<String> pattern = getPattern(file);
        for (int i = 0; i < pattern.size(); i++) {
            char c = (i < ALPHABET.length()) ? ALPHABET.charAt(i) : ALPHABET.charAt(i % ALPHABET.length());
            file_name = file_name.replaceFirst(pattern.get(i), "{" + c + "}");
        }
        return "Pattern: " + file_name;
    }
}

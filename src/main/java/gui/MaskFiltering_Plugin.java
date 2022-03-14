package gui;

import corrections.MaskFiltering;
import ij.ImageJ;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MaskFiltering_Plugin implements PlugIn {
    @Override
    public void run(String s) {

        GenericDialog gd = new GenericDialog("Anisotropy");
        gd.addDirectoryField("Input CSV Folder:     ", "", 50);
        gd.addDirectoryField("Output CSV Folder:    ", "", 50);
        gd.addDirectoryField("Mask Folder:          ", "", 50);

        gd.showDialog();

        if (gd.wasCanceled())
            return;

        String csvInFolder = gd.getNextString();
        String csvOutFolder = gd.getNextString();
        String maskFolder = gd.getNextString();

        System.out.println("CSV in: " + csvInFolder);
        System.out.println("CSV out: " + csvOutFolder);
        System.out.println("Mask: " + maskFolder);

        List<String> csvIn = getListSubFiles(csvInFolder, ".csv");
        List<String> csvOut = getListSubFiles(csvOutFolder, ".csv");
        List<String> mask = getListSubFiles(maskFolder, ".tif");

        try {
            new MaskFiltering(csvIn,csvOut,mask).call();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<String> getListSubFiles(String folder, String filter) {
        return Arrays.asList(new File(folder).listFiles((dir, name) -> name.toLowerCase().endsWith(filter))).stream()
                .map(s -> s.getAbsolutePath())
                .collect(Collectors.toList());
    }

    public static void main(String[] args) {
        new ImageJ();
        new MaskFiltering_Plugin().run(null);
    }
}

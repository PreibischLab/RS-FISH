package gui;

import corrections.MaskFiltering;
import gui.utils.MultiWildcardFileListChooser;
import ij.ImageJ;
import ij.plugin.PlugIn;
import java.util.List;

public class MaskFiltering_Plugin implements PlugIn {
    @Override
    public void run(String s) {

        MultiWildcardFileListChooser chooser = new MultiWildcardFileListChooser();

        if (!chooser.getFileList())
            return;

        List<String> csvIn = chooser.getInputFilesFiltered();
        List<String> csvOut = chooser.getOutputFilesFiltered();
        List<String> mask = chooser.getMaskFilesFiltered();

        try {
            new MaskFiltering(csvIn, csvOut, mask).call();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new ImageJ();
        new MaskFiltering_Plugin().run(null);
    }
}

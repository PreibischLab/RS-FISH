package gui;

import corrections.MaskFiltering;
import corrections.ZCorrection;
import gui.utils.MaskFilteringFileListChooser;
import gui.utils.MaskFilteringOptionChooserGui;
import gui.utils.ZCorrectionFileChooser;
import ij.ImageJ;
import ij.plugin.PlugIn;

import java.util.List;

public class MaskFiltering_Plugin implements PlugIn {
    @Override
    public void run(String s) {

        MaskFilteringOptionChooserGui optionChooserGui = new MaskFilteringOptionChooserGui();

        if (!optionChooserGui.getOptions())
            return;

        boolean zCorrection = optionChooserGui.iszCorrection();
        boolean maskFiltering = optionChooserGui.isMaskFiltering();
        boolean intensityCorrection = optionChooserGui.isIntensityCorrection();

        // TODO intensity correction
        if (maskFiltering) {
            MaskFilteringFileListChooser chooser = new MaskFilteringFileListChooser();

            if (!chooser.getFileList())
                return;

            List<String> csvIn = chooser.getInputFilesFiltered();
            List<String> csvOut = chooser.getOutputFilesFiltered();
            List<String> mask = chooser.getMaskFilesFiltered();

            try {
                // ZCorrelation with MaskFiltering
                if (zCorrection) {
                    new MaskFiltering(csvIn, csvOut, mask).call();
                } else {
                    // just MaskFiltering
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (zCorrection) {
                ZCorrectionFileChooser chooser = new ZCorrectionFileChooser();
                if (!chooser.getFileList())
                    return;

                List<String> csvIn = chooser.getInputFiles();
                List<String> csvOut = chooser.getOutputFiles();

                try {
                    // just ZCorrelation
                    if (zCorrection) {
//                    TODO apply z correction with condition
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


    }

    public static void main(String[] args) {
        new ImageJ();
        new MaskFiltering_Plugin().run(null);
    }
}

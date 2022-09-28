package gui;

import corrections.MaskFilter;
import gui.utils.MultiWildcardFileListChooser;
import ij.ImageJ;
import ij.plugin.PlugIn;

import javax.swing.*;
import java.util.List;

public class MaskFiltering_Plugin implements PlugIn {
    @Override
    public void run(String s) {

        MultiWildcardFileListChooser chooser = new MultiWildcardFileListChooser();

        if (!chooser.getFileList())
            return;

        List<String> csvIn = chooser.getInputFilesFiltered();
        String csvOut = chooser.getOutputFolder();
        String mask = chooser.getMaskFile();

        try {
            boolean result = MaskFilter.run(csvIn, csvOut, mask);
            if (result){
                int n = csvIn.size();

                JOptionPane.showMessageDialog(null, n + " files filtered successfully !","Success", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new ImageJ();
        new MaskFiltering_Plugin().run(null);
    }
}

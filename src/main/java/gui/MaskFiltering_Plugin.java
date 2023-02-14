/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2023 Developers.
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

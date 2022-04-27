package gui.utils;

import fiji.util.gui.GenericDialogPlus;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class MaskFilteringOptionChooserGui {
    private boolean zCorrection;
    private boolean maskFiltering;
    private boolean intensityCorrection;
//    TODO: Laura: if you want message add this
//    private final static String info = "<html> Select Options to  via wildcard expression <br /> "
//            + "Use the path field to specify a file or directory to process or click 'Browse...' to select one. <br /> <br />"
//            + "Wildcard (*) expressions are allowed. <br />"
//            + "e.g. '/Users/spim/data/img_TL*_param*.csv' <br /><br />"
//            + "</html>";

    public boolean getOptions() {
        {
            GenericDialogPlus gdp = new GenericDialogPlus("Pick Options:");
            // TODO: and this
            // GuiUtils.addMessageAsJLabel(info, gdp);

            gdp.addCheckboxGroup(2, 2, new String[]{"Z Correction using Gamma", "Mask Filtering", "Intensity correction using Quadratic"}, new boolean[]{true, true, true});

            gdp.showDialog();

            if (gdp.wasCanceled())
                return false;

            this.zCorrection = gdp.getNextBoolean();
            this.maskFiltering = gdp.getNextBoolean();
            this.intensityCorrection = gdp.getNextBoolean();
            return true;
        }
    }

    public boolean iszCorrection() {
        return zCorrection;
    }

    public boolean isMaskFiltering() {
        return maskFiltering;
    }

    public boolean isIntensityCorrection() {
        return intensityCorrection;
    }
}

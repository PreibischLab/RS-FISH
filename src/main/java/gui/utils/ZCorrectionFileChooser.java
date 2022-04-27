package gui.utils;

import fiji.util.gui.GenericDialogPlus;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class ZCorrectionFileChooser {
    public static boolean windowsHack = true;


    private final static long KB_FACTOR = 1024;

    private final static String info = "<html> <h1> Select files via wildcard expression </h1> <br /> "
            + "Use the path field to specify a file or directory to process or click 'Browse...' to select one. <br /> <br />"
            + "Wildcard (*) expressions are allowed. <br />"
            + "e.g. '/Users/spim/data/img_TL*_param*.csv' <br /><br />"
            + "</html>";

    private List<String> inputFiles;
    private List<String> outputFiles;


    public boolean getFileList() {

        GenericDialogPlus gdp = new GenericDialogPlus("Pick files to include");

        GuiUtils.addMessageAsJLabel(info, gdp);

        gdp.addDirectoryOrFileField("Inputs path", "/", 65);
//        gdp.addDirectoryOrFileField("Masks Path", "/", 65);
        gdp.addDirectoryField("Output Path", "/", 65);
        gdp.addNumericField("exclude files smaller than (KB)", 10, 0);

        // preview selected files - not possible in headless
        if (!PluginHelper.isHeadless()) {
            // add empty preview
            GuiUtils.addMessageAsJLabel(GuiUtils.previewFiles(new ArrayList<>()), gdp, GuiUtils.smallStatusFont);
            JLabel lab = (JLabel) gdp.getComponent(7);
            TextField num = (TextField) gdp.getComponent(6);
            TextField inputField = (TextField) ((Panel) gdp.getComponent(2)).getComponent(0);
//            TextField maskField = (TextField) ((Panel) gdp.getComponent(4)).getComponent(0);

            num.addTextListener(e -> {
                String inputPath = inputField.getText();
//                String maskPath = maskField.getText();
                updateGui(inputPath, lab, num.getText(), gdp);
            });

            final AtomicBoolean autoset = new AtomicBoolean(false);
            inputField.addTextListener(e -> PathUpdated(gdp, lab, num.getText(), inputField.getText(), autoset));
        }

        if (windowsHack && ij.plugin.frame.Recorder.record && System.getProperty("os.name").toLowerCase().contains("win")) {
            gdp.addMessage("Warning: we are on Windows and the Macro Recorder is on, replacing all instances of '\\' with '/'\n"
                    + "   Disable it by opening the script editor, language beanshell, call:\n"
                    + "   net.preibisch.mvrecon.fiji.datasetmanager.FileListDatasetDefinition.windowsHack = false;", GuiUtils.smallStatusFont, Color.RED);
        }

        GuiUtils.addScrollBars(gdp);
        gdp.showDialog();

        if (gdp.wasCanceled())
            return false;

        String fileInput = gdp.getNextString();
        String outputPath = gdp.getNextString();
        long numa = (long) gdp.getNextNumber();

        this.inputFiles = getFiles(fileInput, numa * KB_FACTOR).stream().map(f -> f.getAbsolutePath()).collect(Collectors.toList());
        this.outputFiles = this.inputFiles.stream().map(f -> new File(outputPath, new File(f).getName()).getAbsolutePath()).collect(Collectors.toList());
        return true;
    }

    public List<String> getInputFiles() {
        return inputFiles;
    }


    public List<String> getOutputFiles() {
        return outputFiles;
    }

    private void PathUpdated(GenericDialogPlus gdp, JLabel lab, String numParam, String inputPath, AtomicBoolean autoset) {
        if (autoset.get()) {
            autoset.set(false);
            return;
        }
        // if macro recorder is running and we are on windows
        if (windowsHack && ij.plugin.frame.Recorder.record && System.getProperty("os.name").toLowerCase().contains("win")) {
            while (inputPath.contains("\\"))
                inputPath = inputPath.replace("\\", "/");
            autoset.set(true);
        }
        updateGui(inputPath, lab, numParam, gdp);
    }

    private static void updateGui(String inputPath, JLabel lab, String num, GenericDialogPlus gdp) {

        List<String> inputFiles = getFiles(inputPath, Long.parseLong(num) * KB_FACTOR).stream().map(s -> s.getAbsolutePath()).collect(Collectors.toList());


        if (!inputFiles.isEmpty()) {
//            String patternLabel = GuiUtils.getColoredHtmlFromPattern(PluginHelper.getLabelPattern(inputFiles.get(inputFiles.size() - 1)), false);
//            files.add(0, patternLabel);
            lab.setText(GuiUtils.previewStrings(inputFiles));
            lab.setSize(lab.getPreferredSize());
            gdp.setSize(gdp.getPreferredSize());
            gdp.validate();
        }
    }

    private static List<File> getFiles(String path, long factorFilter) {
        if (path.endsWith(File.separator))
            path = path.substring(0, path.length() - File.separator.length());

        if (new File(path).isDirectory())
            path = String.join(File.separator, path, "*");

        return PluginHelper.getFilesFromPattern(path, factorFilter);
    }

    public static void main(String[] args) {
        new ZCorrectionFileChooser().getFileList();
    }

}

package gui.utils;

import fiji.util.gui.GenericDialogPlus;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.swing.*;


public class MultiWildcardFileListChooser {
    public static boolean windowsHack = true;


    private final static long KB_FACTOR = 1024;

    private final static String info = "<html> <h1> Select files via wildcard expression </h1> <br /> "
            + "Use the path field to specify a file or directory to process or click 'Browse...' to select one. <br /> <br />"
            + "Wildcard (*) expressions are allowed. <br />"
            + "e.g. '/Users/spim/data/img_TL*_param*.csv' <br /><br />"
            + "</html>";
    private List<String> inputFilesFiltered;
    private List<String> maskFilesFiltered;
    private List<String> outputFilesFiltered;


    public boolean getFileList() {

        GenericDialogPlus gdp = new GenericDialogPlus("Pick files to include");

        GuiUtils.addMessageAsJLabel(info, gdp);

        gdp.addDirectoryOrFileField("Inputs path", "/", 65);
        gdp.addDirectoryOrFileField("Masks Path", "/", 65);
        gdp.addDirectoryField("Output Path", "/", 65);
        gdp.addNumericField("exclude files smaller than (KB)", 10, 0);

        // preview selected files - not possible in headless
        if (!PluginHelper.isHeadless()) {
            // add empty preview
            GuiUtils.addMessageAsJLabel(GuiUtils.previewFiles(new ArrayList<>()), gdp, GuiUtils.smallStatusFont);
            JLabel lab = (JLabel) gdp.getComponent(9);
            TextField num = (TextField) gdp.getComponent(8);
            TextField inputField = (TextField) ((Panel) gdp.getComponent(2)).getComponent(0);
            TextField maskField = (TextField) ((Panel) gdp.getComponent(4)).getComponent(0);

            num.addTextListener(e -> {
                String inputPath = inputField.getText();
                String maskPath = maskField.getText();
                updateGui(inputPath, maskPath, lab, num.getText(), gdp);
            });

            final AtomicBoolean autoset = new AtomicBoolean(false);

            inputField.addTextListener(e -> PathUpdated(gdp, lab, num.getText(), inputField.getText(), maskField.getText(), autoset));
            maskField.addTextListener(e -> PathUpdated(gdp, lab, num.getText(), inputField.getText(), maskField.getText(), autoset));
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
        String maskInput = gdp.getNextString();
        String outputPath = gdp.getNextString();
        long numa = (long) gdp.getNextNumber();

        List<File> inputFiles = getFiles(fileInput, numa * KB_FACTOR).stream().sorted().collect(Collectors.toList());
        List<File> maskFiles = getFiles(maskInput, numa * KB_FACTOR).stream().sorted().collect(Collectors.toList());
        List<String> inputPatterns = inputFiles.stream().map(f -> String.join("_", PluginHelper.getPattern(f))).collect(Collectors.toList());
        List<String> maskPatterns = maskFiles.stream().map(f -> String.join("_", PluginHelper.getPattern(f))).collect(Collectors.toList());
        List<String> commonPatterns = inputPatterns.stream().distinct().filter(maskPatterns::contains).collect(Collectors.toList());

        this.inputFilesFiltered = commonPatterns.stream().map(p -> inputFiles.get(inputPatterns.indexOf(p)).getAbsolutePath()).collect(Collectors.toList());
        this.maskFilesFiltered = commonPatterns.stream().map(p -> maskFiles.get(maskPatterns.indexOf(p)).getAbsolutePath()).collect(Collectors.toList());
        this.outputFilesFiltered = this.inputFilesFiltered.stream().map(f -> new File(outputPath, new File(f).getName()).getAbsolutePath()).collect(Collectors.toList());


        if (fileInput.endsWith(File.separator))
            fileInput = fileInput.substring(0, fileInput.length() - File.separator.length());

        if (new File(fileInput).isDirectory())
            fileInput = String.join(File.separator, fileInput, "*");

        List<File> files = PluginHelper.getFilesFromPattern(fileInput, numa * KB_FACTOR);

        files.forEach(f -> System.out.println("Including file " + f + " in dataset."));
        return true;
    }

    public List<String> getInputFilesFiltered() {
        return inputFilesFiltered;
    }

    public List<String> getMaskFilesFiltered() {
        return maskFilesFiltered;
    }

    public List<String> getOutputFilesFiltered() {
        return outputFilesFiltered;
    }

    private void PathUpdated(GenericDialogPlus gdp, JLabel lab, String numParam, String inputPath, String maskPath, AtomicBoolean autoset) {
        if (autoset.get()) {
            autoset.set(false);
            return;
        }
        // if macro recorder is running and we are on windows
        if (windowsHack && ij.plugin.frame.Recorder.record && System.getProperty("os.name").toLowerCase().contains("win")) {
            while (inputPath.contains("\\"))
                inputPath = inputPath.replace("\\", "/");
            while (maskPath.contains("\\"))
                maskPath = maskPath.replace("\\", "/");
            autoset.set(true);
        }
        updateGui(inputPath, maskPath, lab, numParam, gdp);
    }

    private static void updateGui(String inputPath, String maskPath, JLabel lab, String num, GenericDialogPlus gdp) {

        List<File> inputFiles = getFiles(inputPath, Long.parseLong(num) * KB_FACTOR).stream().sorted().collect(Collectors.toList());
        List<File> maskFiles = getFiles(maskPath, Long.parseLong(num) * KB_FACTOR).stream().sorted().collect(Collectors.toList());
        List<String> inputPatterns = inputFiles.stream().map(f -> String.join("_", PluginHelper.getPattern(f))).collect(Collectors.toList());
        List<String> maskPatterns = maskFiles.stream().map(f -> String.join("_", PluginHelper.getPattern(f))).collect(Collectors.toList());
        List<String> commonPatterns = inputPatterns.stream()
                .distinct()
                .filter(maskPatterns::contains)
                .collect(Collectors.toList());

        List<String> files = commonPatterns.stream().map(p ->
                inputFiles.get(inputPatterns.indexOf(p)).getName() + GuiUtils.colorHTML(" -> ") + maskFiles.get(maskPatterns.indexOf(p)).getName()
        ).collect(Collectors.toList());

        if (!inputFiles.isEmpty()) {
            String patternLabel = GuiUtils.getColoredHtmlFromPattern(PluginHelper.getLabelPattern(inputFiles.get(inputFiles.size() - 1)), false);
            files.add(0, patternLabel);
            lab.setText(GuiUtils.previewStrings(files));
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
        new MultiWildcardFileListChooser().getFileList();
    }

}
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
            + "<h3> You can drag-and-drop files or folder </h3> <br /> "
            + "OR, use the path field to specify a file or directory to process or click 'Browse...' to select one. <br /> <br />"
            + "Wildcard (*) expressions are allowed. <br />"
            + "e.g. '/Users/spim/data/img_TL*_param*.csv' <br /><br />"
            + "</html>";
    private List<String> inputFilesFiltered;
    private String maskFile;
    private String outputFolder;


    public boolean getFileList() {

        GenericDialogPlus gdp = new GenericDialogPlus("Pick files to include");

        GuiUtils.addMessageAsJLabel(info, gdp);

        gdp.addDirectoryOrFileField("Inputs File/Folder", "/", 65);
        gdp.addDirectoryOrFileField("Mask File", "/", 65);
        gdp.addDirectoryField("Output Folder", "/", 65);
        gdp.addNumericField("exclude files smaller than (KB)", 10, 0);

        // preview selected files - not possible in headless
        if (!PluginHelper.isHeadless()) {
            // add empty preview
            GuiUtils.addMessageAsJLabel(GuiUtils.previewFiles(new ArrayList<>()), gdp, GuiUtils.smallStatusFont);
            GuiUtils.addMessageAsJLabel("", gdp, GuiUtils.smallStatusFont,Color.RED);
            JLabel lab = (JLabel) gdp.getComponent(9);
            JLabel warn = (JLabel) gdp.getComponent(10);
            TextField num = (TextField) gdp.getComponent(8);
            TextField inputField = (TextField) ((Panel) gdp.getComponent(2)).getComponent(0);
            TextField maskField = (TextField) ((Panel) gdp.getComponent(4)).getComponent(0);
            TextField outputField = (TextField) ((Panel) gdp.getComponent(6)).getComponent(0);

            num.addTextListener(e -> {
                String inputPath = inputField.getText();
                String maskPath = maskField.getText();
                updateGui(inputPath, lab, num.getText(), gdp);
            });

            final AtomicBoolean autoset = new AtomicBoolean(false);

            inputField.addTextListener(e -> PathUpdated(gdp, lab,warn, num.getText(), inputField.getText(), maskField.getText(),outputField.getText(), autoset));
            maskField.addTextListener(e -> PathUpdated(gdp, lab,warn, num.getText(), inputField.getText(), maskField.getText(), outputField.getText(),autoset));
            outputField.addTextListener(e -> PathUpdated(gdp, lab,warn, num.getText(), inputField.getText(), maskField.getText(), outputField.getText(),autoset));

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

        List<String> inputFiles = getFiles(fileInput, numa * KB_FACTOR).stream().map(p -> p.getAbsolutePath()).collect(Collectors.toList());

        this.inputFilesFiltered = inputFiles;
        this.maskFile = maskInput;
        this.outputFolder = outputPath;


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

    public String getMaskFile() {
        return maskFile;
    }

    public String getOutputFolder() {
        return outputFolder;
    }

    private void PathUpdated(GenericDialogPlus gdp, JLabel lab,JLabel warn, String numParam, String inputPath, String maskPath, String outputPath, AtomicBoolean autoset) {
        if (autoset.get()) {
            autoset.set(false);
            return;
        }
        boolean overwrite = checkOutputOverwrite(inputPath,outputPath);
        if (overwrite)
            warn.setText("WARNING: same path input/output, CSV will be overwrite !");
        else
            warn.setText("");
        // if macro recorder is running and we are on windows
        if (windowsHack && ij.plugin.frame.Recorder.record && System.getProperty("os.name").toLowerCase().contains("win")) {
            while (inputPath.contains("\\"))
                inputPath = inputPath.replace("\\", "/");
            while (maskPath.contains("\\"))
                maskPath = maskPath.replace("\\", "/");
            autoset.set(true);
        }
        updateGui(inputPath, lab, numParam, gdp);
    }

    private boolean checkOutputOverwrite(String inputPath, String outputPath) {
        if (inputPath.length()< 3 ||  outputPath.length() <3)
            return false;
        File input = new File(inputPath);
        File output = new File(outputPath);

        if (input.isFile()){
            if(input.getParentFile().equals(output))
                return true;

        }else if (input.equals(output))
            return true;
        return false;
    }


    private static void updateGui(String inputPath, JLabel lab, String num, GenericDialogPlus gdp) {

        List<File> inputFiles = getFiles(inputPath, Long.parseLong(num) * KB_FACTOR);
        List<String> guiFiles = inputFiles.stream().map(p ->  p.getName()).collect(Collectors.toList());

        if (!inputFiles.isEmpty()) {
//            String patternLabel = GuiUtils.getColoredHtmlFromPattern(PluginHelper.getLabelPattern(inputFiles.get(inputFiles.size() - 1)), false);
//            guiFiles.add(0, patternLabel);
            lab.setText(GuiUtils.previewStrings(guiFiles));
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
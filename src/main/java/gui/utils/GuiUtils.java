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
package gui.utils;

import ij.gui.GenericDialog;
import java.awt.*;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import javax.swing.*;
import net.imglib2.type.numeric.ARGBType;

public class GuiUtils {
    public static Font smallStatusFont = new Font(Font.SANS_SERIF, Font.ITALIC, 11);
    public static int minNumLines = 10;

    /**
     * A copy of Curtis's method
     * <p>
     * https://github.com/openmicroscopy/bioformats/blob/v4.4.8/components/loci-plugins/src/loci/plugins/util/WindowTools.java#L72
     *
     * @param obj - the Container to add the scroll bar to
     */
    public static void addScrollBars(Object obj) {
//        * <dependency>
//        * <groupId>${bio-formats.groupId}</groupId>
//        * <artifactId>loci_plugins</artifactId>
//        * <version>${bio-formats.version}</version>
//        * </dependency>

        if (!(obj instanceof Container)) {
            System.out.println("Cannot add scrollbars, it's not a Container but a " + obj.getClass().getSimpleName());
            return;
        }

        final Container pane = (Container) obj;

        if (!(pane.getLayout() instanceof GridBagLayout)) {
            System.out.println("Cannot add scrollbars, it's not a GridBagLayout but a " + pane.getLayout().getClass().getSimpleName());
            return;
        }

        GridBagLayout layout = (GridBagLayout) pane.getLayout();

        // extract components
        int count = pane.getComponentCount();
        Component[] c = new Component[count];
        GridBagConstraints[] gbc = new GridBagConstraints[count];
        for (int i = 0; i < count; i++) {
            c[i] = pane.getComponent(i);
            gbc[i] = layout.getConstraints(c[i]);
        }

        // clear components
        pane.removeAll();
        layout.invalidateLayout(pane);

        // create new container panel
        Panel newPane = new Panel();
        GridBagLayout newLayout = new GridBagLayout();
        newPane.setLayout(newLayout);
        for (int i = 0; i < count; i++) {
            newLayout.setConstraints(c[i], gbc[i]);
            newPane.add(c[i]);
        }

        // HACK - get preferred size for container panel
        // NB: don't know a better way:
        // - newPane.getPreferredSize() doesn't work
        // - newLayout.preferredLayoutSize(newPane) doesn't work
        Frame f = new Frame();
        f.setLayout(new BorderLayout());
        f.add(newPane, BorderLayout.CENTER);
        f.pack();
        final Dimension size = newPane.getSize();
        f.remove(newPane);
        f.dispose();

        // compute best size for scrollable viewport
        size.width += 25;
        size.height += 15;
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int maxWidth = 7 * screen.width / 8;
        int maxHeight = 3 * screen.height / 4;
        if (size.width > maxWidth)
            size.width = maxWidth;
        if (size.height > maxHeight)
            size.height = maxHeight;

        // create scroll pane
        ScrollPane scroll = new ScrollPane() {
            private static final long serialVersionUID = 1L;

            public Dimension getPreferredSize() {
                return size;
            }
        };
        scroll.add(newPane);

        // add scroll pane to original container
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        layout.setConstraints(scroll, constraints);
        pane.add(scroll);
    }

    public static String getColoredHtmlFromPattern(String pattern, boolean withRootTag) {
        final StringBuilder sb = new StringBuilder();
        if (withRootTag)
            sb.append("<html>");
        int n = 0;
        for (int i = 0; i < pattern.length(); i++) {
            if (pattern.charAt(i) == '{') {
                Color col = getColorN(n++);
                sb.append("<span style=\"color: rgb(" + col.getRed() + "," + col.getGreen() + "," + col.getBlue() + ")\">{");
            } else if (pattern.charAt(i) == '}')
                sb.append("}</span>");
            else
                sb.append(pattern.charAt(i));
        }
        if (withRootTag)
            sb.append("</html>");
        return sb.toString();
    }


    public static String colorHTML(String str) {
        final StringBuilder sb = new StringBuilder();
        Color col = getColorN(1);
        sb.append("<span style=\"color: rgb(" + col.getRed() + "," + col.getGreen() + "," + col.getBlue() + ")\">");
        sb.append(str);
        sb.append("</span>");
        return sb.toString();
    }

    public static String previewStrings(List<String> files) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><h2> selected files </h2>");
        for (String f : files)
            sb.append("<br />" + f);
        for (int i = 0; i < minNumLines - files.size(); i++)
            sb.append("<br />");
        sb.append("</html>");
        return sb.toString();
    }

    public static String previewFiles(List<File> files) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><h2> selected files </h2>");
        for (File f : files)
            sb.append("<br />" + f.getAbsolutePath());
        for (int i = 0; i < minNumLines - files.size(); i++)
            sb.append("<br />");
        sb.append("</html>");
        return sb.toString();
    }

    public static Color getColorN(long n) {
        Iterator<ARGBType> iterator = ColorStream.iterator();
        ARGBType c = new ARGBType();
        for (int i = 0; i < n + 43; i++)
            for (int j = 0; j < 3; j++)
                c = iterator.next();
        return new Color(ARGBType.red(c.get()), ARGBType.green(c.get()), ARGBType.blue(c.get()));
    }

    public static void addMessageAsJLabel(String msg, GenericDialog gd) {
        addMessageAsJLabel(msg, gd, null);
    }

    public static void addMessageAsJLabel(String msg, GenericDialog gd, Font font) {
        addMessageAsJLabel(msg, gd, font, null);
    }

    public static void addMessageAsJLabel(String msg, GenericDialog gd, Font font, Color color) {
        gd.addMessage(msg);
        if (!PluginHelper.isHeadless()) {
            final Component msgC = gd.getComponent(gd.getComponentCount() - 1);
            final JLabel msgLabel = new JLabel(msg);

            if (font != null)
                msgLabel.setFont(font);
            if (color != null)
                msgLabel.setForeground(color);

            gd.add(msgLabel);
            GridBagConstraints constraints = ((GridBagLayout) gd.getLayout()).getConstraints(msgC);
            ((GridBagLayout) gd.getLayout()).setConstraints(msgLabel, constraints);

            gd.remove(msgC);
        }
    }


    public static void main(String[] args) {
        //new FileListDatasetDefinition().createDataset();
        //new WildcardFileListChooser().getFileList().forEach( f -> System.out.println( f.getAbsolutePath() ) );
        GenericDialog gd = new GenericDialog("A");
        gd.addMessage(getColoredHtmlFromPattern("a{b}c{d}e{aaaaaaaaaa}aa{bbbbbbbbbbbb}ccccc{ddddddd}", true));
        System.out.println(getColoredHtmlFromPattern("a{b}c{d}e", false));
        gd.showDialog();
    }


}

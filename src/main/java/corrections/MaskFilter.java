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
package corrections;

import cmd.VisualizePointsBDV;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MaskFilter {

    public static boolean run(List<String> csvIn, String csvOutFolder, String mask) throws IOException, CsvValidationException {

        if (mask == null)
            throw new IOException("Invalid Mask ! ");

        System.out.println("Filtering locations using mask image: " + mask);

        final RandomAccessibleInterval img = VisualizePointsBDV.open(mask, null);
        double[] values = checkMaskValues(img);
        if (values.length == 0)
            throw new IOException("Empty invalid Mask! " + mask);
        for (int v = 0; v < values.length; v++) {
            double maskValue = values[v];
            String extra = "";
            if (values.length > 1)
                extra = "_mask_" + v;
            List<String> csvOut = getCSVOutputNames(csvOutFolder, csvIn, extra);
            for (int i = 0; i < csvIn.size(); ++i) {
                // read CSV
                List<InputSpot> currentSpot = readSpot(csvIn.get(i));
                System.out.println("Image size=" + Util.printInterval(img));
                if (img.numDimensions() == 2 || img.numDimensions() == 3) {
                    final ArrayList<InputSpot> spotsTmp = new ArrayList<>();
                    final RealRandomAccess rra = Views.interpolate(Views.extendBorder(img), new NearestNeighborInterpolatorFactory<>()).realRandomAccess();

                    for (final InputSpot s : currentSpot) {
                        rra.setPosition(s.x, 0);
                        rra.setPosition(s.y, 1);
                        if (img.numDimensions() == 3)
                            rra.setPosition(s.z, 2);

                        if (((RealType) rra.get()).getRealDouble() == maskValue)
                            spotsTmp.add(s);
                    }
                    System.out.println("Remaining spots=" + spotsTmp.size() + ", previously=" + currentSpot.size());

                    currentSpot = spotsTmp;
                } else {
                    throw new IOException("2D/3D image required, but is " + img.numDimensions());
                }

                // correct and write new CSV
                writeSpots(currentSpot, csvOut.get(i));
            }
        }

        System.out.println("done.");
        return true;
    }

    private static List<String> getCSVOutputNames(String csvOutFolder, List<String> csvIn, String extra) {
        List<String> result = new ArrayList<>();
        for (String in : csvIn) {
            String basename = FilenameUtils.getBaseName(in);
            String extension = FilenameUtils.getExtension(in);
            String outputName = new StringBuilder()
                    .append(basename)
                    .append("_filtered")
                    .append(extra)
                    .append(".")
                    .append(extension)
                    .toString();
            File outputFile = new File(csvOutFolder, outputName);
            result.add(outputFile.getAbsolutePath());
        }
        return result;
    }

    private static double[] checkMaskValues(RandomAccessibleInterval img) {
        List<Double> result = new ArrayList<>();
        IterableInterval iterableTarget = Views.iterable(img);
        Cursor cursor = iterableTarget.localizingCursor();

        while ( cursor.hasNext())
        {
            cursor.fwd();
            double val = ((RealType) cursor.get()).getRealDouble();
            if (val>0)
                if (!result.contains(Double.valueOf(val)))
                    result.add(Double.valueOf(val));
        }

        return result.stream().mapToDouble(d->d).toArray();
    }

    private static List<InputSpot> readSpot(String csv) throws IOException, CsvValidationException {

        System.out.println("Reading " + csv);

        String[] nextLine;
        CSVReader reader = new CSVReader(new FileReader(csv));
        ArrayList<InputSpot> currentSpots = new ArrayList<>();

        // skip header
        reader.readNext();

        while ((nextLine = reader.readNext()) != null) {
            // x,y,z,t,c,intensity
            InputSpot s = new InputSpot();
            s.x = Double.parseDouble(nextLine[0]);
            s.y = Double.parseDouble(nextLine[1]);
            s.z = Double.parseDouble(nextLine[2]);
            s.t = Integer.parseInt(nextLine[3]);
            s.c = Integer.parseInt(nextLine[4]);
            s.intensity = Double.parseDouble(nextLine[5]); //- 32768; // was unsigned short
            currentSpots.add(s);
        }

        System.out.println("Loaded: " + currentSpots.size() + " spots.");
        reader.close();
        return currentSpots;
    }

    private static void writeSpots(List<InputSpot> spots, String csv) throws IOException {
        System.out.println("Writing " + csv);

        final CSVWriter writer = new CSVWriter(new FileWriter(csv));
        String[] nextLine = new String[6];
        nextLine[0] = "x";
        nextLine[1] = "y";
        nextLine[2] = "z";
        nextLine[3] = "t";
        nextLine[4] = "c";
        nextLine[5] = "intensity";//x,y,z,t,c,intensity
        writer.writeNext(nextLine);

        for (final InputSpot spot : spots) {
            nextLine[0] = Double.toString(spot.x);
            nextLine[1] = Double.toString(spot.y);
            nextLine[2] = Double.toString(spot.z);
            nextLine[3] = "1";
            nextLine[4] = "1";
            nextLine[5] = Double.toString(spot.intensity);//x,y,z,t,c,intensity

            writer.writeNext(nextLine);
        }

        writer.close();
    }

}


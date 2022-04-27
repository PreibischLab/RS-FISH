package corrections;

import cmd.VisualizePointsBDV;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class MaskFilter {

    public static boolean run(List<String> csvIn, List<String> csvOut, List<String> mask) throws Exception {

        if (csvIn.size() != csvOut.size()) {
            System.out.println("Number of input and output CSVs does not match. stopping.");
            return false;
        }

        for (int i = 0; i < csvIn.size(); ++i) {
            //
            // read CSV
            //

            List<InputSpot> currentSpot = readSpot(csvIn.get(i));
            //
            // optionally filter the spots with the mask
            //
//            TODO mask filtering
            if (mask != null && mask.size() > 0) {
                System.out.println("Filtering locations using mask image: " + mask.get(i));

                final ArrayList<InputSpot> spotsTmp = new ArrayList<>();

                final RandomAccessibleInterval img = VisualizePointsBDV.open(mask.get(i), null);

                if (img.numDimensions() != 2) {
                    System.out.println("2D image required, but is " + img.numDimensions());
                    System.exit(0);
                } else {
                    System.out.println("Image size=" + Util.printInterval(img));
                }

                final RealRandomAccess rra = Views.interpolate(Views.extendBorder(img), new NearestNeighborInterpolatorFactory<>()).realRandomAccess();

                for (final InputSpot s : currentSpot) {
                    rra.setPosition(s.x, 0);
                    rra.setPosition(s.y, 1);

                    if (((RealType) rra.get()).getRealDouble() > 0)
                        spotsTmp.add(s);
                }

                System.out.println("Remaining spots=" + spotsTmp.size() + ", previously=" + currentSpot.size());

                currentSpot = spotsTmp;
            }

            // correct and write new CSV
            writeSpots(currentSpot, csvOut.get(i));
        }

        System.out.println("done.");
        return true;
    }

    private static List<InputSpot> readSpot(String csv) throws IOException {

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

        final CSVWriter writer = new CSVWriter(new FileWriter(csv), ',', CSVWriter.NO_QUOTE_CHARACTER);
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
            nextLine[5] = Double.toString(spot.adjustedIntensity);//x,y,z,t,c,intensity

            writer.writeNext(nextLine);
        }

        writer.close();
    }
}


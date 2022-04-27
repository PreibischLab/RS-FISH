package corrections;

import cmd.VisualizePointsBDV;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import static corrections.QuadraticFunctionAxisDifference.polyFunc;
import static corrections.QuadraticFunctionAxisDifference.quadraticFit;
import fit.PointFunctionMatch;
import fit.polynomial.QuadraticFunction;
import mpicbg.models.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import picocli.CommandLine;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class MaskFiltering extends ZCorrection implements Callable<Void>
{

    public MaskFiltering(List<String> csvIn, List<String> csvOut, List<String> mask) {
        super(csvIn, csvOut, mask);
    }

    public MaskFiltering() {
    }

    @Override
    public Void call() throws Exception {

        if ( csvIn.size() != csvOut.size() )
        {
            System.out.println( "Number of input and output CSVs does not match. stopping.");
            return null;
        }

        for ( int i = 0; i < csvIn.size(); ++i )
        {
            //
            // read CSV
            //
            System.out.println( "Reading " + csvIn.get( i ) );

            String[] nextLine;
            CSVReader reader = new CSVReader(new FileReader(csvIn.get( i )));
            ArrayList< InputSpot > spots = new ArrayList<>();

            // skip header
            reader.readNext();

            while ((nextLine = reader.readNext()) != null )
            {
                // x,y,z,t,c,intensity
                InputSpot s = new InputSpot();
                s.x = Double.parseDouble( nextLine[ 0 ] );
                s.y = Double.parseDouble( nextLine[ 1 ] );
                s.z = Double.parseDouble( nextLine[ 2 ] );
                s.t = Integer.parseInt( nextLine[ 3 ] );
                s.c = Integer.parseInt( nextLine[ 4 ] );
                s.intensity = Double.parseDouble( nextLine[ 5 ] ); //- 32768; // was unsigned short
                spots.add( s );
            }

            System.out.println( "Loaded: " + spots.size() + " spots.");
            reader.close();

            //
            // optionally filter the spots with the mask
            //
//            TODO mask filtering
            if ( mask != null && mask.size() > 0 )
            {
                System.out.println( "Filtering locations using mask image: " + mask.get( i ) );

                final ArrayList< InputSpot > spotsTmp = new ArrayList<>();

                final RandomAccessibleInterval img = VisualizePointsBDV.open( mask.get( i ), null );

                if ( img.numDimensions() != 2 )
                {
                    System.out.println( "2D image required, but is " + img.numDimensions() );
                    System.exit( 0 );
                }
                else
                {
                    System.out.println( "Image size=" + Util.printInterval( img ) );
                }

                final RealRandomAccess rra = Views.interpolate( Views.extendBorder( img ) , new NearestNeighborInterpolatorFactory<>() ).realRandomAccess();

                for ( final InputSpot s : spots )
                {
                    rra.setPosition( s.x, 0 );
                    rra.setPosition( s.y, 1 );

                    if ( ((RealType)rra.get()).getRealDouble() > 0 )
                        spotsTmp.add( s );
                }

                System.out.println( "Remaining spots=" + spotsTmp.size() + ", previously=" + spots.size() );

                spots = spotsTmp;
            }

            //
            // fit quadratic function
            //
            final double[] minMaxI = new double[] { Double.MAX_VALUE, -Double.MAX_VALUE };
            final double[] minMaxZ = new double[] { Double.MAX_VALUE, -Double.MAX_VALUE };

            final List< Point > points = spots.stream().map(s -> {
                minMaxI[ 0 ] = Math.min( minMaxI[ 0 ], s.intensity );
                minMaxI[ 1 ] = Math.max( minMaxI[ 1 ], s.intensity );
                minMaxZ[ 0 ] = Math.min( minMaxZ[ 0 ], s.z );
                minMaxZ[ 1 ] = Math.max( minMaxZ[ 1 ], s.z );
                return new Point( new double[]{ s.z, s.intensity} );
            } ).collect(Collectors.toList() );

            final double epsilon = ( minMaxI[ 1 ] - minMaxI[ 0 ] ) * 0.1;
            System.out.println("minI=" + minMaxI[ 0 ] + ", maxI=" + minMaxI[ 1 ] );

            final Pair<QuadraticFunction, ArrayList<PointFunctionMatch>> result = quadraticFit(points, epsilon, 0.3, 1000 );

            //
            // apply quadratic function
            //
            final double minZIntensity = polyFunc( minMaxZ[ 0 ], result.getA() );
            final double[] data = new double[ spots.size() ];
            int j = 0;


            for ( final InputSpot spot : spots )
            {
                spot.adjustedIntensity = data[ j++] = spot.intensity * minZIntensity / polyFunc( spot.z, result.getA() );
            }

            // new min/max after adjustment
            double minI = data[ 0 ];
            double maxI = data[ 0 ];

            for ( final double v : data )
            {
                minI = Math.min( minI, v );
                maxI = Math.max( maxI, v );
            }

            System.out.println("adjusted minI=" + minI + ", maxI=" + maxI );

            // correct and write new CSV
            System.out.println( "Writing " + csvOut.get( i ) );

            final CSVWriter writer = new CSVWriter(new FileWriter(csvOut.get( i )), ',', CSVWriter.NO_QUOTE_CHARACTER);

            nextLine = new String[ 6 ];
            nextLine[ 0 ] = "x";
            nextLine[ 1 ] = "y";
            nextLine[ 2 ] = "z";
            nextLine[ 3 ] = "t";
            nextLine[ 4 ] = "c";
            nextLine[ 5 ] = "intensity";//x,y,z,t,c,intensity
            writer.writeNext(nextLine);

            for ( final InputSpot spot : spots )
            {
                nextLine[ 0 ] = Double.toString( spot.x );
                nextLine[ 1 ] = Double.toString( spot.y );
                nextLine[ 2 ] = Double.toString( spot.z );
                nextLine[ 3 ] = "1";
                nextLine[ 4 ] = "1";
                nextLine[ 5 ] = Double.toString( spot.adjustedIntensity );//x,y,z,t,c,intensity

                writer.writeNext(nextLine);
            }

            writer.close();
        }

        System.out.println( "done." );
        return null;
    }

    public static void main( String[] args )
    {
        new CommandLine(new MaskFiltering()).execute(args);
    }
}


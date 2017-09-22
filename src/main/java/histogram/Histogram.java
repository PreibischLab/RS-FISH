package histogram;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.imglib2.util.ValuePair;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import ij.ImageJ;
import visualization.Detections;

public class Histogram extends ApplicationFrame
{
	private static final long serialVersionUID = 1L;
	protected double min, max;
	
	final Detections detection; // contains the image with overlays 

	public Histogram( final List< Double > values, final int numBins, final String title, final String units )
	{
		super( title );

		final IntervalXYDataset dataset = createDataset( values, numBins, title );
		final JFreeChart chart = createChart( dataset, title, units );
		final ChartPanel chartPanel = new ChartPanel( chart );
		
		chartPanel.addChartMouseListener( new MouseListenerValue( chartPanel, getMin() + ( getMax() - getMin() ) / 2 ));

		chartPanel.setPreferredSize( new Dimension( 600, 270 ) );
		setContentPane( chartPanel );
		
		detection = null;
	}
	
	public Histogram( final List< Double > values, final int numBins, final String title, final String units, final Detections detection)
	{
		super( title );

		final IntervalXYDataset dataset = createDataset( values, numBins, title );
		final JFreeChart chart = createChart( dataset, title, units );
		final ChartPanel chartPanel = new ChartPanel( chart );
		
		chartPanel.addChartMouseListener( new MouseListenerValue( chartPanel, getMin() + ( getMax() - getMin() ) / 2, detection));

		chartPanel.setPreferredSize( new Dimension( 600, 270 ) );
		setContentPane( chartPanel );
		
		this.detection = detection;
	}
	
	
	public void showHistogram()
	{
		this.pack();
		RefineryUtilities.centerFrameOnScreen( this );
		this.setVisible( true );
	}

	public double getMin() { return min; }
	public double getMax() { return max; }

	public static ValuePair< Double, Double > getMinMax( final List< Double > data )
	{
		// compute min/max/size
		double min = data.get( 0 );
		double max = data.get( 0 );

		for ( final double v : data )
		{
			min = Math.min( min, v );
			max = Math.max( max, v );
		}

		return new ValuePair< Double, Double >( min, max );
	}

	public static List< ValuePair< Double, Integer > > binData( final List< Double > data, final double min, final double max, final int numBins )
	{
		// avoid the one value that is exactly 100%
		final double size = max - min + 0.000001;

		// bin and count the entries
		final int[] bins = new int[ numBins ];

		for ( final double v : data )
			++bins[ (int)Math.floor( ( ( v - min ) / size ) * numBins ) ];

		// make the list of bins
		final ArrayList< ValuePair< Double, Integer > > hist = new ArrayList< ValuePair< Double, Integer > >();

		final double binSize = size / numBins;
		for ( int bin = 0; bin < numBins; ++bin )
			hist.add( new ValuePair< Double, Integer >( min + binSize/2 + binSize * bin, bins[ bin ] ) );

		return hist;
	}
	
	protected IntervalXYDataset createDataset( final List< Double > values, final int numBins, final String title )
	{
		final XYSeries series = new XYSeries( title );

		final ValuePair< Double, Double > minmax = getMinMax( values );
		this.min = minmax.getA();
		this.max = minmax.getB();

		final List< ValuePair< Double, Integer > > hist = binData( values, min, max, numBins );
		
		for ( final ValuePair< Double, Integer > pair : hist )
			series.add( pair.getA(), pair.getB() );

		final XYSeriesCollection dataset = new XYSeriesCollection( series );
		dataset.setAutoWidth( true );

		return dataset;
	}

	protected JFreeChart createChart( final IntervalXYDataset dataset, final String title, final String units )
	{
		final JFreeChart chart = ChartFactory.createXYBarChart(
			title,
			"Intensity", // + " [" + units + "]", 
			false,
			"# of spots", 
			dataset,
			PlotOrientation.VERTICAL,
			false, // legend
			false,
			false );

		NumberAxis range = (NumberAxis) chart.getXYPlot().getDomainAxis();
		range.setRange( getMin(), getMax() );

		XYPlot plot = chart.getXYPlot();
		XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
		
		renderer.setSeriesPaint( 0, Color.red );
		renderer.setDrawBarOutline( true );
		renderer.setSeriesOutlinePaint( 0, Color.black );
		renderer.setBarPainter( new StandardXYBarPainter() );

		return chart;
	}

	@Override
	public void windowClosing( final WindowEvent evt )
	{
		if( evt.getWindow() == this )
			dispose();
	}
	
	public static void main( final String[] args )
	{
		new ImageJ();

		final List< Double > values = new ArrayList< Double >();
		final Random rnd = new Random();
		
		for ( int i = 0; i < 10000; ++i )
			values.add( rnd.nextGaussian() );

		final Histogram demo = new Histogram( values, 100, "Histogram for ...", "pixels" );
		demo.showHistogram();
		
	}
}

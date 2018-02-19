package histogram;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.TextAnchor;

import mpicbg.imglib.multithreading.SimpleMultiThreading;
import visualization.Detections;

public class MouseListenerValue implements ChartMouseListener
{
	Histogram histogram;
	final ChartPanel panel;
	ValueMarker valueMarker;
	
	final Detections detection; // contains the image with overlays 	
	
	// this constructor is used to sync the histogram and the overlay image
	MouseListenerValue( Histogram histogram, final ChartPanel panel, final double startValue, final Detections detection)
	{
		this.histogram = histogram;
		this.panel = panel;
		this.valueMarker = makeMarker( startValue );
		this.detection = detection;
		((XYPlot)panel.getChart().getPlot()).addDomainMarker( valueMarker );
		
		// first "update" so that the histogram and the overlays are consistent in values
		if (detection != null)
			if (detection.isStarted()) {
				while (detection.isComputing()) {
					SimpleMultiThreading.threadWait(10);
				}
				detection.updatePreview(startValue);
			}
		
	}

	protected ValueMarker makeMarker( final double value )
	{
		final ValueMarker valueMarker = new ValueMarker( value );
		valueMarker.setStroke( new BasicStroke ( 2f ) );
		valueMarker.setPaint( new Color( 0f/255f, 0f/255f, 255f/255f ) );
		valueMarker.setLabel( " I = " + String.format(java.util.Locale.US,"%.2f", value) );
		valueMarker.setLabelPaint( Color.BLUE );
		valueMarker.setLabelAnchor( RectangleAnchor.TOP );
		valueMarker.setLabelTextAnchor( TextAnchor.TOP_LEFT );
		
		return valueMarker;
	}

	@Override
	public void chartMouseClicked( final ChartMouseEvent e )
	{
		// left mouse click
		if ( e.getTrigger().getButton() == MouseEvent.BUTTON1 )
		{
			double value = getChartXLocation( e.getTrigger().getPoint(), panel );
			// System.out.println("value = " + value);
			histogram.histThresold = value;
			
			valueMarker.setValue( value );
			valueMarker.setLabel( " I = " + String.format(java.util.Locale.US,"%.2f", value) );
			
			// TODO: Use the 'value' to update the overlays in the corresponding image 
			if (detection != null)
				if (detection.isStarted()) {
					while (detection.isComputing()) {
						SimpleMultiThreading.threadWait(10);
					}
					detection.updatePreview(value);
				}
		}
	}
	
	public static double /*int*/ getChartXLocation( final Point point, final ChartPanel panel )
	{
		final Point2D p = panel.translateScreenToJava2D( point );
		final Rectangle2D plotArea = panel.getScreenDataArea();
		final XYPlot plot = (XYPlot) panel.getChart().getPlot();
		final double chartX = plot.getDomainAxis().java2DToValue( p.getX(), plotArea, plot.getDomainAxisEdge() );
		//final double chartY = plot.getRangeAxis().java2DToValue( p.getY(), plotArea, plot.getRangeAxisEdge() );
	
		return chartX; // (int)Math.round( chartX );			
	}

	@Override
	public void chartMouseMoved( ChartMouseEvent e )
	{
	}
}

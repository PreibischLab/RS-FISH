package gui.interactive;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import parameters.GUIParams;

public class BackgroundRANSACListener implements ItemListener
{
	final InteractiveRadialSymmetry parent;

	public BackgroundRANSACListener( final InteractiveRadialSymmetry parent )
	{
		this.parent = parent;
	}

	@Override
	public void itemStateChanged( ItemEvent e )
	{
		final String c = e.getItem().toString();

		parent.params.setBsMethod(-1);
		for ( int i = 0; i < GUIParams.bsMethods.length; ++i )
			if ( c.equals( GUIParams.bsMethods[ i ] ) )
				parent.params.setBsMethod(i);

		if ( parent.params.getBsMethod() == -1 )
			throw new RuntimeException( "Unkown method: " + c  );
		
		// "No background subtraction", "Mean", "Median", "RANSAC on Mean", "RANSAC on Median" ;
		if ( parent.params.getBsMethod() == 3 || parent.params.getBsMethod() == 4 )
		{
			// RANSAC based, open window?
			if ( parent.bkWindow == null )
			{
				parent.bkWindow = new BackgroundRANSACWindow( parent );
				// System.out.println( parent.bkWindow.getFrame().isVisible() );
			}

			if ( !parent.bkWindow.getFrame().isVisible() )
				parent.bkWindow.getFrame().setVisible( true );
		}
		else
		{
			// System.out.println( "hiding frame" );
			// not RANSAC based, close window?
			if ( parent.bkWindow != null && parent.bkWindow.getFrame().isVisible() )
				parent.bkWindow.getFrame().setVisible( false );
		}
	}
}

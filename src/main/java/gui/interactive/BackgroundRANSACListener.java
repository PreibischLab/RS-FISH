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
package gui.interactive;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import parameters.RadialSymParams;

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

		// no background by defaut
		parent.params.setBsMethod( 0 );	
		for ( int i = 0; i < RadialSymParams.bsMethods.length; ++i )
			if ( c.equals( RadialSymParams.bsMethods[ i ] ) )
				parent.params.setBsMethod( i );

		// FIXME: Adjust this check		
//		if ( parent.params.getBsMethod() == -1 )
//			throw new RuntimeException( "Unkown method: " + c  );
		
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

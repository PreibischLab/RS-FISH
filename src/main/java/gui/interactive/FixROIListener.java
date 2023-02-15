/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2023 RS-FISH developers.
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

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import ij.ImagePlus;
import ij.gui.Roi;

/**
 * keeps the target ROI same as source ROI
 * @author spreibi
 *
 */
public class FixROIListener implements MouseListener {
	final ImagePlus source, target;

	public FixROIListener( final ImagePlus s, final ImagePlus t){
		this.source = s;
		this.target = t;
	}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(final MouseEvent e) {
		// here the ROI might have been modified, let's test for that
		final Roi roi = source.getRoi();

		if ( roi == null )
			target.deleteRoi();
		else
			target.setRoi( roi );
	}
}

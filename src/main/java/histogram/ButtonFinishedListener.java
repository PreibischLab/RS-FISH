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
package histogram;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ButtonFinishedListener implements ActionListener {

	Histogram parent;
	
	public ButtonFinishedListener(final Histogram parent) {
		this.parent = parent;
	}

	@Override
	public void actionPerformed(final ActionEvent arg0) {
		parent.isFinished = true;
		parent.dispose();
	}
}

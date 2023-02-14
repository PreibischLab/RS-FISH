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
package gui;

import java.io.File;

import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import gui.csv.overlay.CsvOverlay;
import ij.ImagePlus;

@Plugin(type = Command.class, menuPath = "Plugins>RS-FISH>Tools>Show Detections (ImageJ/Fiji)")
public class CsvOverlay_Plugin implements Command {

	@Parameter(label="Image", autoFill=false)
	ImagePlus imp;

	@Parameter(label = "Path to the overlay file", type=ItemIO.INPUT, autoFill=false)
	File csvFile = new File("");

	@Parameter(visibility=ItemVisibility.INVISIBLE)
	LogService logService;

	@Override
	public void run() {
		new CsvOverlay(imp, csvFile);
	}
}

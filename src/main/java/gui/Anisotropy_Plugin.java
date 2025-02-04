/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2025 RS-FISH developers.
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

import java.util.Arrays;

import anisotropy.parameters.AParams;
import gui.anisotropy.AnisotropyCoefficient;
import gui.interactive.HelperFunctions;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import parameters.RadialSymParams;

public class Anisotropy_Plugin implements PlugIn {

	public static String[] paramChoice = new String[] { "Gauss Fit", "Radial Symmetry" };
	public static int defaultParam = 0;
	public static int defaultImg = 0;

	@Override
	public void run( String arg ) {


		// get list of open image stacks
		final int[] idList = WindowManager.getIDList();

		if ( idList == null || idList.length == 0 )
		{
			IJ.error( "You need at least one open image." );
			return;
		}

		// map all id's to image title for those who are 3d stacks
		final String[] imgList =
				Arrays.stream( idList ).
						mapToObj( id -> WindowManager.getImage( id ).getTitle() ).
							toArray( String[]::new );

		if ( RadialSymParams.defaultImg >= imgList.length )
			RadialSymParams.defaultImg = 0;

		GenericDialog gd = new GenericDialog("Anisotropy");

		gd.addChoice( "Image", imgList, imgList[ RadialSymParams.defaultImg ] );
		gd.addChoice( "Detection Mode", paramChoice, paramChoice[ defaultParam ] );

		gd.showDialog();

		if ( gd.wasCanceled() )
			return;

		// don't do it by name as often multiple images have the same name
		ImagePlus imagePlus = WindowManager.getImage( idList[ RadialSymParams.defaultImg = gd.getNextChoiceIndex() ] );
		int mode = RadialSymParams.defaultMode = gd.getNextChoiceIndex();

		if ( imagePlus.getNFrames() > 1 )
		{
			HelperFunctions.log( "More than one frame detected, need a plain 3D image for Anisotropy detection (check Image>Properties)." );
			return;
		}

		if ( imagePlus.getNChannels() > 1 )
		{
			HelperFunctions.log( "More than one channel detected, need a plain 3D image for Anisotropy detection (check Image>Properties)." );
			return;
		}

		if ( imagePlus.getNSlices() <= 1 )
		{
			HelperFunctions.log( "Only one slice detected, need a 3D image for Anisotropy detection (check Image>Properties)." );
			return;
		}

		float bestScale = 1.0f;
		AParams ap = new AParams();
		double [] minmax = HelperFunctions.calculateMinMax(imagePlus);
		AnisotropyCoefficient ac = new AnisotropyCoefficient(imagePlus, ap, mode, minmax[0], minmax[1]);
		if ( ac.wasCanceled() )
			return;
		bestScale = (float) ac.calculateAnisotropyCoefficient();

		// save default and calculated anisotropy coefficient
		RadialSymParams.defaultAnisotropy = bestScale;
		RadialSymParams.defaultSigma = ac.params.getSigmaDoG(); 
		RadialSymParams.defaultThreshold = ac.params.getThresholdDoG();

		HelperFunctions.log("Anisotropy coefficient: " + bestScale);
	}

	public static void main(String[] args)
	{
		new ImageJ();
		//ij.launch( "/Users/spreibi/Documents/BIMSB/Publications/radialsymmetry/Poiss_300spots_bg_200_0_I_10000_0_img0.tif" );
		//ij.launch( "/Users/spreibi/Documents/BIMSB/Publications/radialsymmetry/N2_702_cropped_1620 (high SNR)_ch0.tif" );
		//ij.launch( "/Users/spreibi/Downloads/in_situ_HCR_tekt2_514_otogl_488_itln_546_spdf_647_zoom_2_no_DAPI_stage_13_embryo_1_cell_1.tif" );

		new ImagePlus( "/Users/spreibi/Documents/BIMSB/Publications/radialsymmetry/N2_702_cropped_1620 (high SNR)_ch0.tif" ).show();
		new Anisotropy_Plugin().run( null );

		// for the historical reasons
		System.out.println("DOGE!");
	}


}

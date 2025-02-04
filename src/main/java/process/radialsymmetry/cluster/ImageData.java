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
package process.radialsymmetry.cluster;


public class ImageData {
	// contains the image parameters 
	
	public int lambda; // wave length
	public boolean defects; // is this a good image or bad
	public String filename; // without extension
	public float center; // used for the second run to store the center of the fitted peak
	
	ImageData(){
		lambda = 0;
		defects = false;
		filename = "";
		center = 1;
	}
	
	ImageData(int lambda, boolean defects, String filename){
		this.lambda = lambda;
		this.defects = defects;
		this.filename = filename;
		this.center = 1;
	}
	
	ImageData(int lambda, boolean defects, String filename, float center){
		this.lambda = lambda;
		this.defects = defects;
		this.filename = filename;
		this.center = center;
	}
	
	ImageData(String filename, float center){
		this.lambda = 0;
		this.defects = false;
		this.filename = filename;
		this.center = center;
	}
	
	int getLambda() {
		return lambda;
	}
	
	boolean getDefects() {
		return defects;
	}
	
	String getFilename() {
		return filename;
	}
	
	float getCenter() {
		return center;
	}
}

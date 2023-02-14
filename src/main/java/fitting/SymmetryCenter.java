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
package fitting;

import fit.AbstractFunction;
import fit.Function;
import mpicbg.models.Model;
import mpicbg.models.Point;
import net.imglib2.EuclideanSpace;

/**
 * Radial Symmetry Package
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.  If not, see http://www.gnu.org/licenses/.
 * 
 * @author Stephan Preibisch (stephan.preibisch@gmx.de) and Timothee Lionnet
 */
public interface SymmetryCenter< M extends AbstractFunction< M > > extends Model< M >, Function< M, Point >, EuclideanSpace
{
	/**
	 * WARNING! This value still needs to be divided by the scale set it Spot!
	 * 
	 * @param center - will write the symmetry center into the array, dimensionality must match
	 */
	public void getSymmetryCenter( final double center[] );
	
	/**
	 * WARNING! This value still needs to be divided by the scale set it Spot!
	 * 
	 * @param center - will write the symmetry center into the array, dimensionality must match
	 */
	public void getSymmetryCenter( final float center[] );
	
	/**
	 * WARNING! This value still needs to be divided by the scale set it Spot!
	 * 
	 * @param d - dimension
	 * @return the center in dimension d
	 */
	public double getSymmetryCenter( final int d );
	
	public void setSymmetryCenter( final double center, final int d );
}

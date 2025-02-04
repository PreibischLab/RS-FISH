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
package util.st.render;

/** 
 * A common parameter class so maxDistance can be changed externally
 */
public class MaxDistanceParam
{
	private double maxSqDistance, maxDistance;

	public MaxDistanceParam( final double maxDistance ) { setMaxDistance( maxDistance ); }

	public void setMaxDistance( final double maxDistance )
	{
		this.maxDistance = maxDistance;
		this.maxSqDistance = maxDistance * maxDistance;
	}

	public double maxDistance() { return maxDistance; }
	public double maxSqDistance() { return maxSqDistance; }
}

/*
 * HA-JDBC: High-Availability JDBC
 * Copyright 2004-2009 Paul Ferraro
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.hajdbc.durability.coarse;

import java.io.Serializable;

import net.sf.hajdbc.Database;
import net.sf.hajdbc.DatabaseCluster;
import net.sf.hajdbc.durability.Durability;
import net.sf.hajdbc.durability.DurabilityFactory;

/**
 * Factory for creating a {@link CoarseDurability}.
 * @author Paul Ferraro
 */
public class CoarseDurabilityFactory implements DurabilityFactory, Serializable
{
	private static final long serialVersionUID = -24045976334856435L;

	/**
	 * {@inheritDoc}
	 * @see net.sf.hajdbc.durability.DurabilityFactory#createDurability(net.sf.hajdbc.durability.DurabilityListener)
	 */
	@Override
	public <Z, D extends Database<Z>> Durability<Z, D> createDurability(DatabaseCluster<Z, D> cluster)
	{
		return new CoarseDurability<Z, D>(cluster);
	}
}

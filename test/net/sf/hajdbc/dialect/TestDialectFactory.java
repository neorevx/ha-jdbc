/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (c) 2004-2007 Paul Ferraro
 * 
 * This library is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by the 
 * Free Software Foundation; either version 2.1 of the License, or (at your 
 * option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, 
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * Contact: ferraro@users.sourceforge.net
 */
package net.sf.hajdbc.dialect;

import net.sf.hajdbc.Dialect;

import org.easymock.EasyMock;
import org.testng.annotations.Test;

/**
 * @author Paul Ferraro
 *
 */
public class TestDialectFactory
{
	@Test
	public void testSerialize()
	{
		Dialect dialect = EasyMock.createStrictMock(Dialect.class);
		
		EasyMock.replay(dialect);
		
		String id = DialectFactory.serialize(dialect);
		
		EasyMock.verify(dialect);
		
		assert id.equals(dialect.getClass().getName()) : id;
	}
	
	@Test
	public void testDeserialize()
	{
		this.assertDialect(null, StandardDialect.class);
		this.assertDialect("net.sf.hajdbc.dialect.StandardDialect", StandardDialect.class);

		this.assertDialect("standard", StandardDialect.class);
		this.assertDialect("db2", DB2Dialect.class);
		this.assertDialect("derby", DerbyDialect.class);
		this.assertDialect("firebird", StandardDialect.class);
		this.assertDialect("h2", HSQLDBDialect.class);
		this.assertDialect("hsqldb", HSQLDBDialect.class);
		this.assertDialect("ingres", IngresDialect.class);
		this.assertDialect("maxdb", MaxDBDialect.class);
		this.assertDialect("mckoi", MckoiDialect.class);
		this.assertDialect("mysql", MySQLDialect.class);
		this.assertDialect("oracle", MaxDBDialect.class);
		this.assertDialect("postgresql", PostgreSQLDialect.class);

		this.assertDialect("PostgreSQL", PostgreSQLDialect.class);
		this.assertDialect("POSTGRESQL", PostgreSQLDialect.class);

		try
		{
			Dialect dialect = DialectFactory.deserialize("invalid");
			
			assert false : dialect.getClass().getName();
		}
		catch (Exception e)
		{
			assert true;
		}
	}
	
	private void assertDialect(String id, Class<? extends Dialect> dialectClass)
	{
		try
		{
			Dialect dialect = DialectFactory.deserialize(id);
			
			assert dialectClass.isInstance(dialect) : dialect.getClass().getName();
		}
		catch (Exception e)
		{
			assert false : e;
		}
	}
}

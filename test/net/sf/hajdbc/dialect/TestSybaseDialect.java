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

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Collection;

import net.sf.hajdbc.ColumnProperties;
import net.sf.hajdbc.ForeignKeyConstraint;
import net.sf.hajdbc.QualifiedName;
import net.sf.hajdbc.TableProperties;
import net.sf.hajdbc.cache.ForeignKeyConstraintImpl;

import org.easymock.EasyMock;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Paul Ferraro
 */
@SuppressWarnings("nls")
@Test
public class TestSybaseDialect extends TestStandardDialect
{
	public TestSybaseDialect()
	{
		super(new SybaseDialect());
	}
	
	/**
	 * @see net.sf.hajdbc.dialect.TestStandardDialect#testGetSimpleSQL()
	 */
	@Override
	public void testGetSimpleSQL() throws SQLException
	{
		String result = this.getSimpleSQL();
		
		assert result.equals("SELECT GETDATE()") : result;
	}

	/**
	 * @see net.sf.hajdbc.dialect.TestStandardDialect#testGetTruncateTableSQL()
	 */
	@Override
	public void testGetTruncateTableSQL() throws SQLException
	{
		TableProperties table = EasyMock.createStrictMock(TableProperties.class);
		
		EasyMock.expect(table.getName()).andReturn("table");
		
		EasyMock.replay(table);
		
		String result = this.getTruncateTableSQL(table);
		
		EasyMock.verify(table);
		
		assert result.equals("TRUNCATE TABLE table") : result;
	}

	/**
	 * @see net.sf.hajdbc.dialect.TestStandardDialect#testGetCreateForeignKeyConstraintSQL()
	 */
	@Override
	public void testGetCreateForeignKeyConstraintSQL() throws SQLException
	{
		ForeignKeyConstraint key = new ForeignKeyConstraintImpl("name", "table");
		key.getColumnList().add("column1");
		key.getColumnList().add("column2");
		key.setForeignTable("foreign_table");
		key.getForeignColumnList().add("foreign_column1");
		key.getForeignColumnList().add("foreign_column2");
		key.setDeferrability(DatabaseMetaData.importedKeyInitiallyDeferred);
		key.setDeleteRule(DatabaseMetaData.importedKeyCascade);
		key.setUpdateRule(DatabaseMetaData.importedKeyRestrict);
		
		String result = this.getCreateForeignKeyConstraintSQL(key);
		
		assert result.equals("ALTER TABLE table ADD CONSTRAINT name FOREIGN KEY (column1, column2) REFERENCES foreign_table (foreign_column1, foreign_column2) ON DELETE CASCADE ON UPDATE RESTRICT") : result;
	}

	/**
	 * @see net.sf.hajdbc.dialect.TestStandardDialect#testIsIdentity()
	 */
	@Override
	public void testIsIdentity() throws SQLException
	{
		ColumnProperties column = EasyMock.createStrictMock(ColumnProperties.class);
		
		EasyMock.expect(column.getDefaultValue()).andReturn("AUTOINCREMENT");
		
		EasyMock.replay(column);
		
		boolean result = this.isIdentity(column);

		EasyMock.verify(column);
		
		assert result;
		
		EasyMock.reset(column);
		
		EasyMock.expect(column.getDefaultValue()).andReturn("IDENTITY");
		
		EasyMock.replay(column);
		
		result = this.isIdentity(column);

		EasyMock.verify(column);
		
		assert result;
		
		EasyMock.reset(column);
		
		EasyMock.expect(column.getDefaultValue()).andReturn(null);
		
		EasyMock.replay(column);
		
		result = this.isIdentity(column);
		
		EasyMock.verify(column);
		
		assert !result;
	}
	
	/**
	 * @see net.sf.hajdbc.dialect.TestStandardDialect#testParseSequence(java.lang.String)
	 */
	@Override
	@Test(dataProvider = "sequence-sql")
	public void testParseSequence(String sql) throws SQLException
	{
		String result = this.parseSequence(sql);
		
		assert (result == null) : result;
	}

	/**
	 * @see net.sf.hajdbc.dialect.TestStandardDialect#testGetSequences()
	 */
	@Override
	public void testGetSequences() throws SQLException
	{
		DatabaseMetaData metaData = EasyMock.createStrictMock(DatabaseMetaData.class);
		
		EasyMock.replay(metaData);
		
		Collection<QualifiedName> result = this.getSequences(metaData);
		
		EasyMock.verify(metaData);
		
		assert result.isEmpty() : result;
	}

	@Override
	@DataProvider(name = "current-date")
	Object[][] currentDateProvider()
	{
		java.sql.Date date = new java.sql.Date(System.currentTimeMillis());
		
		return new Object[][] {
			new Object[] { "SELECT CURRENT DATE FROM success", date },
			new Object[] { "SELECT TODAY(*) FROM success", date },
			new Object[] { "SELECT TODAY ( * ) FROM success", date },
			new Object[] { "SELECT CURRENT DATES FROM failure", date },
			new Object[] { "SELECT CCURRENT DATE FROM failure", date },
			new Object[] { "SELECT NOTTODAY(*) FROM failure", date },
			new Object[] { "SELECT 1 FROM failure", date },
		};
	}
	
	@Override
	@Test(dataProvider = "current-date")
	public void testEvaluateCurrentDate(String sql, java.sql.Date date) throws SQLException
	{
		String expected = sql.contains("success") ? String.format("SELECT '%s' FROM success", date.toString()) : sql;
		
		String evaluated = this.evaluateCurrentDate(sql, date);

		assert evaluated.equals(expected) : evaluated;
	}

	@Override
	@DataProvider(name = "current-time")
	Object[][] currentTimeProvider()
	{
		java.sql.Time date = new java.sql.Time(System.currentTimeMillis());
		
		return new Object[][] {
			new Object[] { "SELECT CURRENT TIME FROM success", date },
			new Object[] { "SELECT CCURRENT TIME FROM failure", date },
			new Object[] { "SELECT CURRENT TIMESTAMP FROM failure", date },
			new Object[] { "SELECT 1 FROM failure", date },
		};
	}
	
	@Override
	@Test(dataProvider = "current-time")
	public void testEvaluateCurrentTime(String sql, java.sql.Time date) throws SQLException
	{
		String expected = sql.contains("success") ? String.format("SELECT '%s' FROM success", date.toString()) : sql;
		
		String evaluated = this.evaluateCurrentTime(sql, date);

		assert evaluated.equals(expected) : evaluated;
	}

	@Override
	@DataProvider(name = "current-timestamp")
	Object[][] currentTimestampProvider()
	{
		java.sql.Timestamp date = new java.sql.Timestamp(System.currentTimeMillis());
		
		return new Object[][] {
			new Object[] { "SELECT CURRENT TIMESTAMP FROM success", date },
			new Object[] { "SELECT GETDATE() FROM success", date },
			new Object[] { "SELECT GETDATE ( ) FROM success", date },
			new Object[] { "SELECT NOW(*) FROM success", date },
			new Object[] { "SELECT NOW ( * ) FROM success", date },
			new Object[] { "SELECT CCURRENT TIMESTAMP FROM failure", date },
			new Object[] { "SELECT CURRENT TIMESTAMPS FROM failure", date },
			new Object[] { "SELECT FORGETDATE() FROM failure", date },
			new Object[] { "SELECT NNOW(*) FROM failure", date },
			new Object[] { "SELECT 1 FROM failure", date },
		};
	}
	
	@Override
	@Test(dataProvider = "current-timestamp")
	public void testEvaluateCurrentTimestamp(String sql, java.sql.Timestamp date) throws SQLException
	{
		String expected = sql.contains("success") ? String.format("SELECT '%s' FROM success", date.toString()) : sql;
		
		String evaluated = this.evaluateCurrentTimestamp(sql, date);

		assert evaluated.equals(expected) : evaluated;
	}
}

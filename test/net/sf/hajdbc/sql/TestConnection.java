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
package net.sf.hajdbc.sql;

import java.lang.reflect.Proxy;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.sf.hajdbc.Balancer;
import net.sf.hajdbc.ColumnProperties;
import net.sf.hajdbc.Database;
import net.sf.hajdbc.DatabaseCluster;
import net.sf.hajdbc.DatabaseMetaDataCache;
import net.sf.hajdbc.DatabaseProperties;
import net.sf.hajdbc.Dialect;
import net.sf.hajdbc.MockDatabase;
import net.sf.hajdbc.TableProperties;
import net.sf.hajdbc.util.reflect.ProxyFactory;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Unit test for {@link Connection}
 * @author Paul Ferraro
 */
@SuppressWarnings({ "unchecked", "nls" })
@Test
public class TestConnection implements Connection
{
	private TransactionContext transactionContext = EasyMock.createStrictMock(TransactionContext.class);
	private Balancer balancer = EasyMock.createStrictMock(Balancer.class);
	private DatabaseCluster cluster = EasyMock.createStrictMock(DatabaseCluster.class);
	private FileSupport fileSupport = EasyMock.createStrictMock(FileSupport.class);
	private Dialect dialect = EasyMock.createStrictMock(Dialect.class);
	private DatabaseMetaDataCache metaData = EasyMock.createStrictMock(DatabaseMetaDataCache.class);
	private DatabaseProperties databaseProperties = EasyMock.createStrictMock(DatabaseProperties.class);
	private TableProperties tableProperties = EasyMock.createStrictMock(TableProperties.class);
	private ColumnProperties columnProperties = EasyMock.createStrictMock(ColumnProperties.class);
	private Connection connection1 = EasyMock.createStrictMock(java.sql.Connection.class);
	private Connection connection2 = EasyMock.createStrictMock(java.sql.Connection.class);
	private SQLProxy parent = EasyMock.createStrictMock(SQLProxy.class);
	private SQLProxy root = EasyMock.createStrictMock(SQLProxy.class);
	private Savepoint savepoint1 = EasyMock.createStrictMock(Savepoint.class);
	private Savepoint savepoint2 = EasyMock.createStrictMock(Savepoint.class);
	
	private Database database1 = new MockDatabase("1");
	private Database database2 = new MockDatabase("2");
	private Set<Database> databaseSet;
	private ExecutorService executor = Executors.newSingleThreadExecutor();
	private Connection connection;
	private ConnectionInvocationHandler handler;
	private IAnswer<InvocationStrategy> anwser = new IAnswer<InvocationStrategy>()
	{
		@Override
		public InvocationStrategy answer() throws Throwable
		{
			return (InvocationStrategy) EasyMock.getCurrentArguments()[0];
		}		
	};
	
	@BeforeClass
	void init() throws Exception
	{
		Map<Database, Connection> map = new TreeMap<Database, Connection>();
		map.put(this.database1, this.connection1);
		map.put(this.database2, this.connection2);
		
		this.databaseSet = map.keySet();
		
		EasyMock.expect(this.parent.getDatabaseCluster()).andReturn(this.cluster);

		this.parent.addChild(EasyMock.isA(ConnectionInvocationHandler.class));

		this.replay();
		
		this.handler = new ConnectionInvocationHandler(new Object(), this.parent, EasyMock.createMock(Invoker.class), map, this.transactionContext, this.fileSupport);
		this.connection = ProxyFactory.createProxy(Connection.class, this.handler);
		
		this.verify();
		this.reset();
	}
	
	private Object[] objects()
	{
		return new Object[] { this.cluster, this.balancer, this.connection1, this.connection2, this.fileSupport, this.parent, this.root, this.savepoint1, this.savepoint2, this.dialect, this.metaData, this.databaseProperties, this.tableProperties, this.columnProperties, this.transactionContext };
	}
	
	void replay()
	{
		EasyMock.replay(this.objects());
	}
	
	void verify()
	{
		EasyMock.verify(this.objects());
	}
	
	@AfterMethod
	void reset()
	{
		EasyMock.reset(this.objects());
	}
	
	/**
	 * @see java.sql.Connection#clearWarnings()
	 */
	@Override
	public void clearWarnings() throws SQLException
	{
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		this.connection1.clearWarnings();
		this.connection2.clearWarnings();
		
		this.replay();
		
		this.connection.clearWarnings();
		
		this.verify();
	}
	
	/**
	 * @see java.sql.Connection#close()
	 */
	@Override
	public void close() throws SQLException
	{
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		this.connection1.close();
		this.connection2.close();
		
		this.transactionContext.close();
		
		this.fileSupport.close();

		this.parent.removeChild(this.handler);
		
		this.replay();
		
		this.connection.close();
		
		this.verify();
	}
	
	/**
	 * @see java.sql.Connection#commit()
	 */
	@Override
	public void commit() throws SQLException
	{
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.cluster.getTransactionalExecutor()).andReturn(this.executor);
		
		EasyMock.expect(this.transactionContext.end(EasyMock.isA(DatabaseWriteInvocationStrategy.class))).andAnswer(this.anwser);
		
		EasyMock.expect(this.cluster.getBalancer()).andReturn(this.balancer);
		EasyMock.expect(this.balancer.all()).andReturn(this.databaseSet);
		
		EasyMock.expect(this.parent.getRoot()).andReturn(this.root);
		
		this.root.retain(this.databaseSet);
		
		this.connection1.commit();
		this.connection2.commit();
		
		this.replay();
		
		this.connection.commit();
		
		this.verify();
	}
	
	public void testCreateStatement() throws SQLException
	{
		Statement statement1 = EasyMock.createMock(Statement.class);
		Statement statement2 = EasyMock.createMock(Statement.class);
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.connection1.createStatement()).andReturn(statement1);
		EasyMock.expect(this.connection2.createStatement()).andReturn(statement2);
		
		this.replay();
		
		Statement result = this.createStatement();

		this.verify();
		
		assert Proxy.isProxyClass(result.getClass());
		
		SQLProxy proxy = SQLProxy.class.cast(Proxy.getInvocationHandler(result));
		
		assert proxy.getObject(this.database1) == statement1;
		assert proxy.getObject(this.database2) == statement2;
	}
	
	/**
	 * @see java.sql.Connection#createStatement()
	 */
	@Override
	public Statement createStatement() throws SQLException
	{
		return this.connection.createStatement();
	}
	
	@DataProvider(name = "int-int")
	Object[][] intIntProvider()
	{
		return new Object[][] {
			new Object[] { ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY },
			new Object[] { ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY },
			new Object[] { ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY },
			new Object[] { ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE },
			new Object[] { ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE },
			new Object[] { ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE },
		};
	}
	
	@Test(dataProvider = "int-int")
	public void testCreateStatement(int type, int concurrency) throws SQLException
	{
		Statement statement1 = EasyMock.createMock(Statement.class);
		Statement statement2 = EasyMock.createMock(Statement.class);
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.connection1.createStatement(type, concurrency)).andReturn(statement1);
		EasyMock.expect(this.connection2.createStatement(type, concurrency)).andReturn(statement2);
		
		this.replay();
		
		Statement result = this.createStatement(type, concurrency);

		this.verify();
		
		assert Proxy.isProxyClass(result.getClass());
		
		SQLProxy proxy = SQLProxy.class.cast(Proxy.getInvocationHandler(result));
		
		assert proxy.getObject(this.database1) == statement1;
		assert proxy.getObject(this.database2) == statement2;
	}
	
	/**
	 * @see java.sql.Connection#createStatement(int, int)
	 */
	@Override
	public Statement createStatement(int type, int concurrency) throws SQLException
	{
		return this.connection.createStatement(type, concurrency);
	}
	
	@DataProvider(name = "int-int-int")
	Object[][] intIntIntProvider()
	{
		return new Object[][] {
			new Object[] { ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT },
			new Object[] { ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT },
			new Object[] { ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT },
			new Object[] { ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT },
			new Object[] { ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT },
			new Object[] { ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT },
			new Object[] { ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT },
			new Object[] { ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT },
			new Object[] { ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT },
			new Object[] { ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, ResultSet.HOLD_CURSORS_OVER_COMMIT },
			new Object[] { ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE, ResultSet.HOLD_CURSORS_OVER_COMMIT },
			new Object[] { ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE, ResultSet.HOLD_CURSORS_OVER_COMMIT },
		};
	}
	
	@Test(dataProvider = "int-int-int")
	public void testCreateStatement(int type, int concurrency, int holdability) throws SQLException
	{
		Statement statement1 = EasyMock.createMock(Statement.class);
		Statement statement2 = EasyMock.createMock(Statement.class);
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.connection1.createStatement(type, concurrency, holdability)).andReturn(statement1);
		EasyMock.expect(this.connection2.createStatement(type, concurrency, holdability)).andReturn(statement2);
		
		this.replay();
		
		Statement result = this.createStatement(type, concurrency, holdability);

		this.verify();
		
		assert Proxy.isProxyClass(result.getClass());
		
		SQLProxy proxy = SQLProxy.class.cast(Proxy.getInvocationHandler(result));
		
		assert proxy.getObject(this.database1) == statement1;
		assert proxy.getObject(this.database2) == statement2;
	}
	
	/**
	 * @see java.sql.Connection#createStatement(int, int, int)
	 */
	@Override
	public Statement createStatement(int type, int concurrency, int holdability) throws SQLException
	{		
		return this.connection.createStatement(type, concurrency, holdability);
	}
	
	public void testGetAutoCommit() throws SQLException
	{
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.connection1.getAutoCommit()).andReturn(true);
		
		this.replay();
		
		boolean autoCommit = this.getAutoCommit();
		
		this.verify();
		
		assert autoCommit;
	}
	
	/**
	 * @see java.sql.Connection#getAutoCommit()
	 */
	@Override
	public boolean getAutoCommit() throws SQLException
	{
		return this.connection.getAutoCommit();
	}

	public void testGetCatalog() throws SQLException
	{
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.connection1.getCatalog()).andReturn("catalog");
		
		this.replay();
		
		String catalog = this.getCatalog();
		
		this.verify();
		
		assert catalog.equals("catalog") : catalog;
	}
	
	/**
	 * @see java.sql.Connection#getCatalog()
	 */
	@Override
	public String getCatalog() throws SQLException
	{
		return this.connection.getCatalog();
	}

	public void testGetHoldability() throws SQLException
	{
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.connection1.getHoldability()).andReturn(ResultSet.HOLD_CURSORS_OVER_COMMIT);
		
		this.replay();
		
		int holdability = this.getHoldability();
		
		this.verify();
		
		assert holdability == ResultSet.HOLD_CURSORS_OVER_COMMIT : holdability;
	}
	
	/**
	 * @see java.sql.Connection#getHoldability()
	 */
	@Override
	public int getHoldability() throws SQLException
	{
		return this.connection.getHoldability();
	}

	public void testGetMetaData() throws SQLException
	{
		DatabaseMetaData metaData = EasyMock.createMock(DatabaseMetaData.class);
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.cluster.getBalancer()).andReturn(this.balancer);
		EasyMock.expect(this.balancer.next()).andReturn(this.database2);

		this.balancer.beforeInvocation(this.database2);
		
		EasyMock.expect(this.connection2.getMetaData()).andReturn(metaData);
		
		this.balancer.afterInvocation(this.database2);
		
		this.replay();
		
		DatabaseMetaData result = this.getMetaData();
		
		this.verify();
		
		assert result == metaData;
	}
	
	/**
	 * @see java.sql.Connection#getMetaData()
	 */
	@Override
	public DatabaseMetaData getMetaData() throws SQLException
	{
		return this.connection.getMetaData();
	}
	
	public void testGetTransactionIsolation() throws SQLException
	{
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.cluster.getBalancer()).andReturn(this.balancer);
		EasyMock.expect(this.balancer.next()).andReturn(this.database2);
		
		this.balancer.beforeInvocation(this.database2);
		
		EasyMock.expect(this.connection2.getTransactionIsolation()).andReturn(java.sql.Connection.TRANSACTION_NONE);

		this.balancer.afterInvocation(this.database2);
		
		this.replay();
		
		int isolation = this.getTransactionIsolation();
		
		this.verify();
		
		assert isolation == java.sql.Connection.TRANSACTION_NONE : isolation;
	}
	
	/**
	 * @see java.sql.Connection#getTransactionIsolation()
	 */
	@Override
	public int getTransactionIsolation() throws SQLException
	{
		return this.connection.getTransactionIsolation();
	}
	
	public void testGetTypeMap() throws SQLException
	{
		Map<String, Class<?>> map = Collections.emptyMap();
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.connection1.getTypeMap()).andReturn(map);
		
		this.replay();
		
		Map<String, Class<?>> result = this.getTypeMap();
		
		this.verify();
		
		assert result == map;
	}
	
	/**
	 * @see java.sql.Connection#getTypeMap()
	 */
	@Override
	public Map<String, Class<?>> getTypeMap() throws SQLException
	{
		return this.connection.getTypeMap();
	}
	
	public void testGetWarnings() throws SQLException
	{
		SQLWarning warning = new SQLWarning();
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.connection1.getWarnings()).andReturn(warning);
		
		this.replay();
		
		SQLWarning result = this.getWarnings();
		
		this.verify();
		
		assert result == warning;
	}
	
	/**
	 * @see java.sql.Connection#getWarnings()
	 */
	@Override
	public SQLWarning getWarnings() throws SQLException
	{
		return this.connection.getWarnings();
	}

	public void testIsClosed() throws SQLException
	{
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.connection1.isClosed()).andReturn(true);
		
		this.replay();
		
		boolean closed = this.isClosed();
		
		this.verify();
		
		assert closed;
	}
	
	/**
	 * @see java.sql.Connection#isClosed()
	 */
	@Override
	public boolean isClosed() throws SQLException
	{
		return this.connection.isClosed();
	}
	
	public void testIsReadOnly() throws SQLException
	{
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.connection1.isReadOnly()).andReturn(true);
		
		this.replay();
		
		boolean readOnly = this.isReadOnly();
		
		this.verify();
		
		assert readOnly;
	}
	
	/**
	 * @see java.sql.Connection#isReadOnly()
	 */
	@Override
	public boolean isReadOnly() throws SQLException
	{
		return this.connection.isReadOnly();
	}
	
	public void testNativeSQL() throws SQLException
	{
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.connection1.nativeSQL("sql")).andReturn("native-sql");
		
		this.replay();
		
		String nativeSQL = this.nativeSQL("sql");
		
		this.verify();
		
		assert nativeSQL.equals("native-sql") : nativeSQL;
	}
	
	/**
	 * @see java.sql.Connection#nativeSQL(java.lang.String)
	 */
	@Override
	public String nativeSQL(String sql) throws SQLException
	{
		return this.connection.nativeSQL(sql);
	}
	
	
	@DataProvider(name = "string")
	Object[][] stringProvider()
	{
		return new Object[][] { new Object[] { "sql" } };
	}
	
	@Test(dataProvider = "string")
	public void testPrepareCall(String sql) throws SQLException
	{
		CallableStatement statement1 = EasyMock.createMock(CallableStatement.class);
		CallableStatement statement2 = EasyMock.createMock(CallableStatement.class);
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.cluster.getNonTransactionalExecutor()).andReturn(this.executor);
		
		EasyMock.expect(this.cluster.getBalancer()).andReturn(this.balancer);
		EasyMock.expect(this.balancer.all()).andReturn(this.databaseSet);
		
		EasyMock.expect(this.parent.getRoot()).andReturn(this.root);
		
		this.root.retain(this.databaseSet);
		
		EasyMock.expect(this.connection1.prepareCall(sql)).andReturn(statement1);
		EasyMock.expect(this.connection2.prepareCall(sql)).andReturn(statement2);

		this.replay();
		
		CallableStatement result = this.prepareCall(sql);

		this.verify();
		
		assert Proxy.isProxyClass(result.getClass());
		
		SQLProxy proxy = SQLProxy.class.cast(Proxy.getInvocationHandler(result));
		
		assert proxy.getObject(this.database1) == statement1;
		assert proxy.getObject(this.database2) == statement2;
	}
	
	/**
	 * @see java.sql.Connection#prepareCall(java.lang.String)
	 */
	@Override
	public CallableStatement prepareCall(String sql) throws SQLException
	{
		return this.connection.prepareCall(sql);
	}
	
	@DataProvider(name = "string-int-int")
	Object[][] stringIntIntProvider()
	{
		return new Object[][] {
			new Object[] { "sql", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY },
			new Object[] { "sql", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY },
			new Object[] { "sql", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY },
			new Object[] { "sql", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE },
			new Object[] { "sql", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE },
			new Object[] { "sql", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE },
		};
	}
	
	@Test(dataProvider = "string-int-int")
	public void testPrepareCall(String sql, int type, int concurrency) throws SQLException
	{
		CallableStatement statement1 = EasyMock.createMock(CallableStatement.class);
		CallableStatement statement2 = EasyMock.createMock(CallableStatement.class);
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.cluster.getNonTransactionalExecutor()).andReturn(this.executor);
		
		EasyMock.expect(this.cluster.getBalancer()).andReturn(this.balancer);
		EasyMock.expect(this.balancer.all()).andReturn(this.databaseSet);

		EasyMock.expect(this.parent.getRoot()).andReturn(this.root);
		
		this.root.retain(this.databaseSet);
		
		EasyMock.expect(this.connection1.prepareCall(sql, type, concurrency)).andReturn(statement1);
		EasyMock.expect(this.connection2.prepareCall(sql, type, concurrency)).andReturn(statement2);
		
		this.replay();
		
		CallableStatement result = this.prepareCall(sql, type, concurrency);

		this.verify();
		
		assert Proxy.isProxyClass(result.getClass());
		
		SQLProxy proxy = SQLProxy.class.cast(Proxy.getInvocationHandler(result));
		
		assert proxy.getObject(this.database1) == statement1;
		assert proxy.getObject(this.database2) == statement2;
	}
	
	/**
	 * @see java.sql.Connection#prepareCall(java.lang.String, int, int)
	 */
	@Override
	public CallableStatement prepareCall(String sql, int type, int concurrency) throws SQLException
	{
		return this.connection.prepareCall(sql, type, concurrency);
	}
	
	@DataProvider(name = "string-int-int-int")
	Object[][] stringIntIntIntProvider()
	{
		return new Object[][] {
			new Object[] { "sql", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT },
			new Object[] { "sql", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT },
			new Object[] { "sql", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT },
			new Object[] { "sql", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT },
			new Object[] { "sql", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT },
			new Object[] { "sql", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT },
			new Object[] { "sql", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT },
			new Object[] { "sql", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT },
			new Object[] { "sql", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT },
			new Object[] { "sql", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, ResultSet.HOLD_CURSORS_OVER_COMMIT },
			new Object[] { "sql", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE, ResultSet.HOLD_CURSORS_OVER_COMMIT },
			new Object[] { "sql", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE, ResultSet.HOLD_CURSORS_OVER_COMMIT },
		};
	}
	
	@Test(dataProvider = "string-int-int-int")
	public void testPrepareCall(String sql, int type, int concurrency, int holdability) throws SQLException
	{
		CallableStatement statement1 = EasyMock.createMock(CallableStatement.class);
		CallableStatement statement2 = EasyMock.createMock(CallableStatement.class);
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.cluster.getNonTransactionalExecutor()).andReturn(this.executor);
		
		EasyMock.expect(this.cluster.getBalancer()).andReturn(this.balancer);
		EasyMock.expect(this.balancer.all()).andReturn(this.databaseSet);
		
		EasyMock.expect(this.parent.getRoot()).andReturn(this.root);
		
		this.root.retain(this.databaseSet);
		
		EasyMock.expect(this.connection1.prepareCall(sql, type, concurrency, holdability)).andReturn(statement1);
		EasyMock.expect(this.connection2.prepareCall(sql, type, concurrency, holdability)).andReturn(statement2);

		this.replay();
		
		CallableStatement result = this.prepareCall(sql, type, concurrency, holdability);

		this.verify();
		
		assert Proxy.isProxyClass(result.getClass());
		
		SQLProxy proxy = SQLProxy.class.cast(Proxy.getInvocationHandler(result));
		
		assert proxy.getObject(this.database1) == statement1;
		assert proxy.getObject(this.database2) == statement2;
	}
	
	/**
	 * @see java.sql.Connection#prepareCall(java.lang.String, int, int, int)
	 */
	@Override
	public CallableStatement prepareCall(String sql, int type, int concurrency, int holdability) throws SQLException
	{
		return this.connection.prepareCall(sql, type, concurrency, holdability);
	}
	
	@Test(dataProvider = "string")
	public void testPrepareStatement(String sql) throws SQLException
	{
		PreparedStatement statement1 = EasyMock.createMock(PreparedStatement.class);
		PreparedStatement statement2 = EasyMock.createMock(PreparedStatement.class);
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.cluster.getNonTransactionalExecutor()).andReturn(this.executor);		
		
		EasyMock.expect(this.cluster.isCurrentTimestampEvaluationEnabled()).andReturn(false);
		EasyMock.expect(this.cluster.isCurrentDateEvaluationEnabled()).andReturn(false);
		EasyMock.expect(this.cluster.isCurrentTimeEvaluationEnabled()).andReturn(false);
		EasyMock.expect(this.cluster.isRandEvaluationEnabled()).andReturn(false);
		
		EasyMock.expect(this.cluster.getBalancer()).andReturn(this.balancer);
		EasyMock.expect(this.balancer.all()).andReturn(this.databaseSet);
		
		EasyMock.expect(this.parent.getRoot()).andReturn(this.root);
		
		this.root.retain(this.databaseSet);
		
		EasyMock.expect(this.connection1.prepareStatement(sql)).andReturn(statement1);
		EasyMock.expect(this.connection2.prepareStatement(sql)).andReturn(statement2);
		
		this.extractIdentifiers(sql);
		
		this.replay();
		
		PreparedStatement result = this.prepareStatement(sql);

		this.verify();
		
		assert Proxy.isProxyClass(result.getClass());
		
		SQLProxy proxy = SQLProxy.class.cast(Proxy.getInvocationHandler(result));
		
		assert proxy.getObject(this.database1) == statement1;
		assert proxy.getObject(this.database2) == statement2;
	}
	
	/**
	 * @see java.sql.Connection#prepareStatement(java.lang.String)
	 */
	@Override
	public PreparedStatement prepareStatement(String sql) throws SQLException
	{
		return this.connection.prepareStatement(sql);
	}
	
	@DataProvider(name = "string-int")
	Object[][] stringIntProvider()
	{
		return new Object[][] {
			new Object[] { "sql", Statement.NO_GENERATED_KEYS },
			new Object[] { "sql", Statement.RETURN_GENERATED_KEYS },
		};
	}

	@Test(dataProvider = "string-int")
	public void testPrepareStatement(String sql, int autoGeneratedKeys) throws SQLException
	{
		PreparedStatement statement1 = EasyMock.createMock(PreparedStatement.class);
		PreparedStatement statement2 = EasyMock.createMock(PreparedStatement.class);
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.cluster.getNonTransactionalExecutor()).andReturn(this.executor);
		
		EasyMock.expect(this.cluster.isCurrentTimestampEvaluationEnabled()).andReturn(false);
		EasyMock.expect(this.cluster.isCurrentDateEvaluationEnabled()).andReturn(false);
		EasyMock.expect(this.cluster.isCurrentTimeEvaluationEnabled()).andReturn(false);
		EasyMock.expect(this.cluster.isRandEvaluationEnabled()).andReturn(false);
		
		EasyMock.expect(this.cluster.getBalancer()).andReturn(this.balancer);
		EasyMock.expect(this.balancer.all()).andReturn(this.databaseSet);
		
		EasyMock.expect(this.parent.getRoot()).andReturn(this.root);
		
		this.root.retain(this.databaseSet);
		
		EasyMock.expect(this.connection1.prepareStatement(sql, autoGeneratedKeys)).andReturn(statement1);
		EasyMock.expect(this.connection2.prepareStatement(sql, autoGeneratedKeys)).andReturn(statement2);

		this.extractIdentifiers(sql);
		
		this.replay();
		
		PreparedStatement result = this.prepareStatement(sql, autoGeneratedKeys);

		this.verify();
		
		assert Proxy.isProxyClass(result.getClass());
		
		SQLProxy proxy = SQLProxy.class.cast(Proxy.getInvocationHandler(result));
		
		assert proxy.getObject(this.database1) == statement1;
		assert proxy.getObject(this.database2) == statement2;
	}
	
	/**
	 * @see java.sql.Connection#prepareStatement(java.lang.String, int)
	 */
	@Override
	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
	{
		return this.connection.prepareStatement(sql, autoGeneratedKeys);
	}
	
	@Test(dataProvider = "string-int-int")
	public void testPrepareStatement(String sql, int type, int concurrency) throws SQLException
	{
		PreparedStatement statement1 = EasyMock.createMock(PreparedStatement.class);
		PreparedStatement statement2 = EasyMock.createMock(PreparedStatement.class);
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.cluster.getNonTransactionalExecutor()).andReturn(this.executor);
		
		EasyMock.expect(this.cluster.isCurrentTimestampEvaluationEnabled()).andReturn(false);
		EasyMock.expect(this.cluster.isCurrentDateEvaluationEnabled()).andReturn(false);
		EasyMock.expect(this.cluster.isCurrentTimeEvaluationEnabled()).andReturn(false);
		EasyMock.expect(this.cluster.isRandEvaluationEnabled()).andReturn(false);
		
		EasyMock.expect(this.cluster.getBalancer()).andReturn(this.balancer);
		EasyMock.expect(this.balancer.all()).andReturn(this.databaseSet);
		
		EasyMock.expect(this.parent.getRoot()).andReturn(this.root);
		
		this.root.retain(this.databaseSet);
		
		EasyMock.expect(this.connection1.prepareStatement(sql, type, concurrency)).andReturn(statement1);
		EasyMock.expect(this.connection2.prepareStatement(sql, type, concurrency)).andReturn(statement2);

		this.extractIdentifiers(sql);
		
		this.replay();
		
		PreparedStatement result = this.prepareStatement(sql, type, concurrency);

		this.verify();
		
		assert Proxy.isProxyClass(result.getClass());
		
		SQLProxy proxy = SQLProxy.class.cast(Proxy.getInvocationHandler(result));
		
		assert proxy.getObject(this.database1) == statement1;
		assert proxy.getObject(this.database2) == statement2;
	}
	
	/**
	 * @see java.sql.Connection#prepareStatement(java.lang.String, int, int)
	 */
	@Override
	public PreparedStatement prepareStatement(String sql, int type, int concurrency) throws SQLException
	{
		return this.connection.prepareStatement(sql, type, concurrency);
	}
	
	@Test(dataProvider = "string-int-int-int")
	public void testPrepareStatement(String sql, int type, int concurrency, int holdability) throws SQLException
	{
		PreparedStatement statement1 = EasyMock.createMock(PreparedStatement.class);
		PreparedStatement statement2 = EasyMock.createMock(PreparedStatement.class);
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.cluster.getNonTransactionalExecutor()).andReturn(this.executor);
		
		EasyMock.expect(this.cluster.isCurrentTimestampEvaluationEnabled()).andReturn(false);
		EasyMock.expect(this.cluster.isCurrentDateEvaluationEnabled()).andReturn(false);
		EasyMock.expect(this.cluster.isCurrentTimeEvaluationEnabled()).andReturn(false);
		EasyMock.expect(this.cluster.isRandEvaluationEnabled()).andReturn(false);
		
		EasyMock.expect(this.cluster.getBalancer()).andReturn(this.balancer);
		EasyMock.expect(this.balancer.all()).andReturn(this.databaseSet);
		
		EasyMock.expect(this.parent.getRoot()).andReturn(this.root);
		
		this.root.retain(this.databaseSet);
		
		EasyMock.expect(this.connection1.prepareStatement(sql, type, concurrency, holdability)).andReturn(statement1);
		EasyMock.expect(this.connection2.prepareStatement(sql, type, concurrency, holdability)).andReturn(statement2);
		
		this.extractIdentifiers(sql);
		
		this.replay();
		
		PreparedStatement result = this.prepareStatement(sql, type, concurrency, holdability);

		this.verify();
		
		assert Proxy.isProxyClass(result.getClass());
		
		SQLProxy proxy = SQLProxy.class.cast(Proxy.getInvocationHandler(result));
		
		assert proxy.getObject(this.database1) == statement1;
		assert proxy.getObject(this.database2) == statement2;
	}
	
	/**
	 * @see java.sql.Connection#prepareStatement(java.lang.String, int, int, int)
	 */
	@Override
	public PreparedStatement prepareStatement(String sql, int type, int concurrency, int holdability) throws SQLException
	{
		return this.connection.prepareStatement(sql, type, concurrency, holdability);
	}
	
	@DataProvider(name = "string-ints")
	Object[][] stringIntsProvider()
	{
		return new Object[][] { new Object[] { "sql", new int[] { 1 } } };
	}
	
	@Test(dataProvider = "string-ints")
	public void testPrepareStatement(String sql, int[] columnIndexes) throws SQLException
	{
		PreparedStatement statement1 = EasyMock.createMock(PreparedStatement.class);
		PreparedStatement statement2 = EasyMock.createMock(PreparedStatement.class);
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.cluster.getNonTransactionalExecutor()).andReturn(this.executor);
		
		EasyMock.expect(this.cluster.isCurrentTimestampEvaluationEnabled()).andReturn(false);
		EasyMock.expect(this.cluster.isCurrentDateEvaluationEnabled()).andReturn(false);
		EasyMock.expect(this.cluster.isCurrentTimeEvaluationEnabled()).andReturn(false);
		EasyMock.expect(this.cluster.isRandEvaluationEnabled()).andReturn(false);
		
		EasyMock.expect(this.cluster.getBalancer()).andReturn(this.balancer);
		EasyMock.expect(this.balancer.all()).andReturn(this.databaseSet);
		
		EasyMock.expect(this.parent.getRoot()).andReturn(this.root);
		
		this.root.retain(this.databaseSet);
		
		EasyMock.expect(this.connection1.prepareStatement(sql, columnIndexes)).andReturn(statement1);
		EasyMock.expect(this.connection2.prepareStatement(sql, columnIndexes)).andReturn(statement2);

		this.extractIdentifiers(sql);
		
		this.replay();
		
		PreparedStatement result = this.prepareStatement(sql, columnIndexes);

		this.verify();
		
		assert Proxy.isProxyClass(result.getClass());
		
		SQLProxy proxy = SQLProxy.class.cast(Proxy.getInvocationHandler(result));
		
		assert proxy.getObject(this.database1) == statement1;
		assert proxy.getObject(this.database2) == statement2;
	}
	
	/**
	 * @see java.sql.Connection#prepareStatement(java.lang.String, int[])
	 */
	@Override
	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
	{
		return this.connection.prepareStatement(sql, columnIndexes);
	}
	
	@DataProvider(name = "string-strings")
	Object[][] stringStringsProvider()
	{
		return new Object[][] { new Object[] { "sql", new String[] { "col1" } } };
	}
	
	@Test(dataProvider = "string-strings")
	public void testPrepareStatement(String sql, String[] columnNames) throws SQLException
	{
		PreparedStatement statement1 = EasyMock.createMock(PreparedStatement.class);
		PreparedStatement statement2 = EasyMock.createMock(PreparedStatement.class);
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.cluster.getNonTransactionalExecutor()).andReturn(this.executor);
		
		EasyMock.expect(this.cluster.isCurrentTimestampEvaluationEnabled()).andReturn(false);
		EasyMock.expect(this.cluster.isCurrentDateEvaluationEnabled()).andReturn(false);
		EasyMock.expect(this.cluster.isCurrentTimeEvaluationEnabled()).andReturn(false);
		EasyMock.expect(this.cluster.isRandEvaluationEnabled()).andReturn(false);
		
		EasyMock.expect(this.cluster.getBalancer()).andReturn(this.balancer);
		EasyMock.expect(this.balancer.all()).andReturn(this.databaseSet);
		
		EasyMock.expect(this.parent.getRoot()).andReturn(this.root);
		
		this.root.retain(this.databaseSet);
		
		EasyMock.expect(this.connection1.prepareStatement(sql, columnNames)).andReturn(statement1);
		EasyMock.expect(this.connection2.prepareStatement(sql, columnNames)).andReturn(statement2);

		this.extractIdentifiers(sql);
		
		this.replay();
		
		PreparedStatement result = this.connection.prepareStatement(sql, columnNames);

		this.verify();
		
		assert Proxy.isProxyClass(result.getClass());
		
		SQLProxy proxy = SQLProxy.class.cast(Proxy.getInvocationHandler(result));
		
		assert proxy.getObject(this.database1) == statement1;
		assert proxy.getObject(this.database2) == statement2;
	}
	
	/**
	 * @see java.sql.Connection#prepareStatement(java.lang.String, java.lang.String[])
	 */
	@Override
	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
	{
		return this.connection.prepareStatement(sql, columnNames);
	}
	
	@DataProvider(name = "savepoint")
	Object[][] savepointProvider() throws Exception
	{
		Map<Database, Savepoint> map = new TreeMap<Database, Savepoint>();
		map.put(this.database1, this.savepoint1);
		map.put(this.database2, this.savepoint2);
		
		return new Object[][] { new Object[] { ProxyFactory.createProxy(Savepoint.class, new SavepointInvocationHandler(this.connection, this.handler, EasyMock.createMock(Invoker.class), map)) } };
	}
	
	/**
	 * @see java.sql.Connection#releaseSavepoint(Savepoint)
	 */
	@Test(dataProvider = "savepoint")
	public void releaseSavepoint(Savepoint savepoint) throws SQLException
	{
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.cluster.getTransactionalExecutor()).andReturn(this.executor);
		
		EasyMock.expect(this.cluster.getBalancer()).andReturn(this.balancer);
		EasyMock.expect(this.balancer.all()).andReturn(this.databaseSet);
		
		EasyMock.expect(this.parent.getRoot()).andReturn(this.root);
		
		this.root.retain(this.databaseSet);
		
		this.connection1.releaseSavepoint(this.savepoint1);
		this.connection2.releaseSavepoint(this.savepoint2);
		
		this.replay();
		
		this.connection.releaseSavepoint(savepoint);
		
		this.verify();
	}
	
	/**
	 * @see java.sql.Connection#rollback()
	 */
	@Test
	public void rollback() throws SQLException
	{
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.cluster.getTransactionalExecutor()).andReturn(this.executor);
		
		EasyMock.expect(this.transactionContext.end(EasyMock.isA(DatabaseWriteInvocationStrategy.class))).andAnswer(this.anwser);

		EasyMock.expect(this.cluster.getBalancer()).andReturn(this.balancer);
		EasyMock.expect(this.balancer.all()).andReturn(this.databaseSet);

		EasyMock.expect(this.parent.getRoot()).andReturn(this.root);
		
		this.root.retain(this.databaseSet);
		
		this.connection1.rollback();
		this.connection2.rollback();
		
		this.replay();
		
		this.connection.rollback();
		
		this.verify();
	}
	
	/**
	 * @see java.sql.Connection#rollback(Savepoint)
	 */
	@Test(dataProvider = "savepoint")
	public void rollback(Savepoint savepoint) throws SQLException
	{
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.cluster.getTransactionalExecutor()).andReturn(this.executor);
		
		EasyMock.expect(this.transactionContext.end(EasyMock.isA(DatabaseWriteInvocationStrategy.class))).andAnswer(this.anwser);
		
		EasyMock.expect(this.cluster.getBalancer()).andReturn(this.balancer);
		EasyMock.expect(this.balancer.all()).andReturn(this.databaseSet);

		EasyMock.expect(this.parent.getRoot()).andReturn(this.root);
		
		this.root.retain(this.databaseSet);
		
		this.connection1.rollback(this.savepoint1);
		this.connection2.rollback(this.savepoint2);
		
		this.replay();
		
		this.connection.rollback(savepoint);
		
		this.verify();
	}
	
	@DataProvider(name = "boolean")
	Object[][] booleanProvider()
	{
		return new Object[][] { new Object[] { true } };
	}
	
	/**
	 * @see java.sql.Connection#setAutoCommit(boolean)
	 */
	@Test(dataProvider = "boolean")
	public void setAutoCommit(boolean autoCommit) throws SQLException
	{
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		this.connection1.setAutoCommit(autoCommit);
		this.connection2.setAutoCommit(autoCommit);
		
		this.replay();
		
		this.connection.setAutoCommit(autoCommit);
		
		this.verify();
	}
	
	/**
	 * @see java.sql.Connection#setCatalog(java.lang.String)
	 */
	@Test(dataProvider = "string")
	public void setCatalog(String catalog) throws SQLException
	{
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.cluster.getNonTransactionalExecutor()).andReturn(this.executor);
		
		EasyMock.expect(this.cluster.getBalancer()).andReturn(this.balancer);
		EasyMock.expect(this.balancer.all()).andReturn(this.databaseSet);

		EasyMock.expect(this.parent.getRoot()).andReturn(this.root);
		
		this.root.retain(this.databaseSet);
		
		this.connection1.setCatalog(catalog);
		this.connection2.setCatalog(catalog);
		
		this.replay();
		
		this.connection.setCatalog(catalog);
		
		this.verify();
	}
	
	@DataProvider(name = "holdability")
	Object[][] holdabilityProvider()
	{
		return new Object[][] {
			new Object[] { ResultSet.CLOSE_CURSORS_AT_COMMIT },
			new Object[] { ResultSet.HOLD_CURSORS_OVER_COMMIT },
		};
	}
	
	/**
	 * @see java.sql.Connection#setHoldability(int)
	 */
	@Test(dataProvider = "holdability")
	public void setHoldability(int holdability) throws SQLException
	{
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		this.connection1.setHoldability(holdability);
		this.connection2.setHoldability(holdability);
		
		this.replay();
		
		this.connection.setHoldability(holdability);
		
		this.verify();
	}
	
	/**
	 * @see java.sql.Connection#setReadOnly(boolean)
	 */
	@Test(dataProvider = "boolean")
	public void setReadOnly(boolean readOnly) throws SQLException
	{
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.cluster.getNonTransactionalExecutor()).andReturn(this.executor);
		
		EasyMock.expect(this.cluster.getBalancer()).andReturn(this.balancer);
		EasyMock.expect(this.balancer.all()).andReturn(this.databaseSet);

		EasyMock.expect(this.parent.getRoot()).andReturn(this.root);
		
		this.root.retain(this.databaseSet);
		
		this.connection1.setReadOnly(readOnly);
		this.connection2.setReadOnly(readOnly);
		
		this.replay();
		
		this.connection.setReadOnly(readOnly);
		
		this.verify();
	}
	
	public void testSetSavepoint() throws SQLException
	{
		Savepoint savepoint1 = EasyMock.createMock(Savepoint.class);
		Savepoint savepoint2 = EasyMock.createMock(Savepoint.class);
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.cluster.getTransactionalExecutor()).andReturn(this.executor);
		
		EasyMock.expect(this.cluster.getBalancer()).andReturn(this.balancer);
		EasyMock.expect(this.balancer.all()).andReturn(this.databaseSet);

		EasyMock.expect(this.parent.getRoot()).andReturn(this.root);
		
		this.root.retain(this.databaseSet);
		
		EasyMock.expect(this.connection1.setSavepoint()).andReturn(savepoint1);
		EasyMock.expect(this.connection2.setSavepoint()).andReturn(savepoint2);
		
		this.replay();
		
		Savepoint result = this.setSavepoint();
		
		this.verify();

		assert Proxy.isProxyClass(result.getClass());
		
		SQLProxy proxy = SQLProxy.class.cast(Proxy.getInvocationHandler(result));
		
		assert proxy.getObject(this.database1) == savepoint1;
		assert proxy.getObject(this.database2) == savepoint2;
	}
	
	/**
	 * @see java.sql.Connection#setSavepoint()
	 */
	@Override
	public Savepoint setSavepoint() throws SQLException
	{
		return this.connection.setSavepoint();
	}
	
	@Test(dataProvider = "string")
	public void testSetSavepoint(String name) throws SQLException
	{
		Savepoint savepoint1 = EasyMock.createMock(Savepoint.class);
		Savepoint savepoint2 = EasyMock.createMock(Savepoint.class);
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.cluster.getTransactionalExecutor()).andReturn(this.executor);
		
		EasyMock.expect(this.cluster.getBalancer()).andReturn(this.balancer);
		EasyMock.expect(this.balancer.all()).andReturn(this.databaseSet);
		
		EasyMock.expect(this.parent.getRoot()).andReturn(this.root);
		
		this.root.retain(this.databaseSet);
		
		EasyMock.expect(this.connection1.setSavepoint(name)).andReturn(savepoint1);
		EasyMock.expect(this.connection2.setSavepoint(name)).andReturn(savepoint2);
		
		this.replay();
		
		Savepoint result = this.setSavepoint(name);
		
		this.verify();

		assert Proxy.isProxyClass(result.getClass());
		
		SQLProxy proxy = SQLProxy.class.cast(Proxy.getInvocationHandler(result));
		
		assert proxy.getObject(this.database1) == savepoint1;
		assert proxy.getObject(this.database2) == savepoint2;
	}
	
	/**
	 * @see java.sql.Connection#setSavepoint(java.lang.String)
	 */
	@Override
	public Savepoint setSavepoint(String name) throws SQLException
	{
		return this.connection.setSavepoint(name);
	}
	
	@DataProvider(name = "isolation")
	Object[][] isolationProvider()
	{
		return new Object[][] { new Object[] { java.sql.Connection.TRANSACTION_NONE } };
	}
	
	/**
	 * @see java.sql.Connection#setTransactionIsolation(int)
	 */
	@Test(dataProvider = "isolation")
	public void setTransactionIsolation(int level) throws SQLException
	{
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.cluster.getNonTransactionalExecutor()).andReturn(this.executor);
		
		EasyMock.expect(this.cluster.getBalancer()).andReturn(this.balancer);
		EasyMock.expect(this.balancer.all()).andReturn(this.databaseSet);
		
		EasyMock.expect(this.parent.getRoot()).andReturn(this.root);
		
		this.root.retain(this.databaseSet);
		
		this.connection1.setTransactionIsolation(level);
		this.connection2.setTransactionIsolation(level);
		
		this.replay();
		
		this.connection.setTransactionIsolation(level);
		
		this.verify();
	}
	
	@DataProvider(name = "map")
	Object[][] mapProvider()
	{
		return new Object[][] { new Object[] { Collections.EMPTY_MAP } };
	}
	
	/**
	 * @see java.sql.Connection#setTypeMap(java.util.Map)
	 */
	@Test(dataProvider = "map")
	public void setTypeMap(Map<String, Class<?>> map) throws SQLException
	{
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		this.connection1.setTypeMap(map);
		this.connection2.setTypeMap(map);
		
		this.replay();
		
		this.connection.setTypeMap(map);
		
		this.verify();
	}

	public void testCreateArrayOf() throws SQLException
	{
		Object[] objects = new Object[0];
		
		Array array = EasyMock.createMock(Array.class);
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.connection1.createArrayOf("", objects)).andReturn(array);
		
		this.replay();
		
		Array result = this.createArrayOf("", objects);
		
		this.verify();
		
		assert result == array;
	}
	
	/**
	 * @see java.sql.Connection#createArrayOf(java.lang.String, java.lang.Object[])
	 */
	@Override
	public Array createArrayOf(String typeName, Object[] elements) throws SQLException
	{
		return this.connection.createArrayOf(typeName, elements);
	}

	public void testCreateBlob() throws SQLException
	{
		Blob blob = EasyMock.createMock(Blob.class);
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.connection1.createBlob()).andReturn(blob);
		
		this.replay();
		
		Blob result = this.createBlob();
		
		this.verify();
		
		assert result == blob;
	}
	
	/**
	 * @see java.sql.Connection#createBlob()
	 */
	@Override
	public Blob createBlob() throws SQLException
	{
		return this.connection.createBlob();
	}

	public void testCreateClob() throws SQLException
	{
		Clob clob = EasyMock.createMock(Clob.class);
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.connection1.createClob()).andReturn(clob);
		
		this.replay();
		
		Clob result = this.createClob();
		
		this.verify();
		
		assert result == clob;
	}
	
	/**
	 * @see java.sql.Connection#createClob()
	 */
	@Override
	public Clob createClob() throws SQLException
	{
		return this.connection.createClob();
	}

	public void testCreateNClob() throws SQLException
	{
		NClob clob = EasyMock.createMock(NClob.class);
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.connection1.createNClob()).andReturn(clob);
		
		this.replay();
		
		NClob result = this.createNClob();
		
		this.verify();
		
		assert result == clob;
	}
	
	/**
	 * @see java.sql.Connection#createNClob()
	 */
	@Override
	public NClob createNClob() throws SQLException
	{
		return this.connection.createNClob();
	}

	public void testCreateSQLXML() throws SQLException
	{
		SQLXML xml = EasyMock.createMock(SQLXML.class);
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.connection1.createSQLXML()).andReturn(xml);
		
		this.replay();
		
		SQLXML result = this.createSQLXML();
		
		this.verify();
		
		assert result == xml;
	}
	
	/**
	 * @see java.sql.Connection#createSQLXML()
	 */
	@Override
	public SQLXML createSQLXML() throws SQLException
	{
		return this.connection.createSQLXML();
	}
	
	public void testCreateStruct() throws SQLException
	{
		Object[] elements = new Object[0];
		
		Struct struct = EasyMock.createMock(Struct.class);
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.connection1.createStruct("", elements)).andReturn(struct);
		
		this.replay();
		
		Struct result = this.createStruct("", elements);
		
		this.verify();
		
		assert result == struct;
	}
	
	/**
	 * @see java.sql.Connection#createStruct(java.lang.String, java.lang.Object[])
	 */
	@Override
	public Struct createStruct(String typeName, Object[] elements) throws SQLException
	{
		return this.connection.createStruct(typeName, elements);
	}

	public void testGetClientInfo() throws SQLException
	{
		Properties properties = new Properties();
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.connection1.getClientInfo()).andReturn(properties);
		
		this.replay();
		
		Properties result = this.getClientInfo();
		
		this.verify();
		
		assert result == properties;
	}
	
	/**
	 * @see java.sql.Connection#getClientInfo()
	 */
	@Test
	public Properties getClientInfo() throws SQLException
	{
		return this.connection.getClientInfo();
	}

	@Test(dataProvider = "string")
	public void testGetClientInfo(String property) throws SQLException
	{
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.connection1.getClientInfo(property)).andReturn("value");
		
		this.replay();
		
		String result = this.getClientInfo(property);
		
		this.verify();
		
		assert result.equals("value");
	}
	
	/**
	 * @see java.sql.Connection#getClientInfo(java.lang.String)
	 */
	@Override
	public String getClientInfo(String property) throws SQLException
	{
		return this.connection.getClientInfo(property);
	}

	public void testIsValid() throws SQLException
	{
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.cluster.getBalancer()).andReturn(this.balancer);
		EasyMock.expect(this.balancer.next()).andReturn(this.database2);
		
		this.balancer.beforeInvocation(this.database2);
		
		EasyMock.expect(this.connection2.isValid(1)).andReturn(true);
		
		this.balancer.afterInvocation(this.database2);
		
		this.replay();
		
		boolean result = this.isValid(1);
		
		this.verify();
		
		assert result;
	}
	
	/**
	 * @see java.sql.Connection#isValid(int)
	 */
	@Test(dataProvider = "int")
	public boolean isValid(int timeout) throws SQLException
	{
		return this.connection.isValid(timeout);
	}

	@DataProvider(name = "properties")
	Object[][] propertiesProvider()
	{
		return new Object[][] { new Object[] { new Properties() } };
	}
	
	/**
	 * @see java.sql.Connection#setClientInfo(java.util.Properties)
	 */
	@Test(dataProvider = "properties")
	public void setClientInfo(Properties properties) throws SQLClientInfoException
	{
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		this.connection1.setClientInfo(properties);
		this.connection2.setClientInfo(properties);
		
		this.replay();
		
		this.connection.setClientInfo(properties);
		
		this.verify();
	}

	@DataProvider(name = "string-string")
	Object[][] stringStringProvider()
	{
		return new Object[][] { new Object[] { "name", "value" } };
	}
	
	/**
	 * @see java.sql.Connection#setClientInfo(java.lang.String, java.lang.String)
	 */
	@Test(dataProvider = "string-string")
	public void setClientInfo(String property, String value) throws SQLClientInfoException
	{
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		this.connection1.setClientInfo(property, value);
		this.connection2.setClientInfo(property, value);
		
		this.replay();
		
		this.connection.setClientInfo(property, value);
		
		this.verify();
	}
	
	public void testIsWrapperFor() throws SQLException
	{
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.connection1.isWrapperFor(Connection.class)).andReturn(true);
		
		this.replay();
		
		boolean result = this.isWrapperFor(Connection.class);
		
		this.verify();
		
		assert result;
	}
	
	/**
	 * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
	 */
	@Override
	public boolean isWrapperFor(Class<?> targetClass) throws SQLException
	{
		return this.connection.isWrapperFor(targetClass);
	}

	public void testUnwrap() throws SQLException
	{
		Connection connection = EasyMock.createMock(Connection.class);
		
		EasyMock.expect(this.cluster.isActive()).andReturn(true);
		
		EasyMock.expect(this.connection1.unwrap(Connection.class)).andReturn(connection);
		
		this.replay();
		
		Connection result = this.connection.unwrap(Connection.class);
		
		this.verify();
		
		assert result == connection;
	}
	
	/**
	 * @see java.sql.Wrapper#unwrap(java.lang.Class)
	 */
	@Override
	public <T> T unwrap(Class<T> targetClass) throws SQLException
	{
		return this.connection.unwrap(targetClass);
	}
	
	protected void extractIdentifiers(String sql) throws SQLException
	{	
		EasyMock.expect(this.cluster.isSequenceDetectionEnabled()).andReturn(false);
		EasyMock.expect(this.cluster.isIdentityColumnDetectionEnabled()).andReturn(false);
		
		EasyMock.expect(this.cluster.getDatabaseMetaDataCache()).andReturn(this.metaData);
		EasyMock.expect(this.metaData.getDatabaseProperties(EasyMock.same(this.connection))).andReturn(this.databaseProperties);
		EasyMock.expect(this.databaseProperties.supportsSelectForUpdate()).andReturn(false);
	}
}

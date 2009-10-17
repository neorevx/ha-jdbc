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
package net.sf.hajdbc;

import java.util.concurrent.ExecutorService;

import net.sf.hajdbc.balancer.Balancer;
import net.sf.hajdbc.cache.DatabaseMetaDataCache;
import net.sf.hajdbc.durability.Durability;
import net.sf.hajdbc.lock.LockManager;
import net.sf.hajdbc.state.StateManager;

/**
 * @author Paul Ferraro
 * @param <D> either java.sql.Driver or javax.sql.DataSource
 */
public interface DatabaseCluster<Z, D extends Database<Z>> extends Lifecycle
{
	/**
	 * Returns the identifier of this cluster.
	 * @return an identifier
	 */
	String getId();
	
	/**
	 * Activates the specified database
	 * @param database a database descriptor
	 * @param manager a state manager
	 * @return true, if the database was activated, false it was already active
	 */
	boolean activate(D database, StateManager manager);
	
	/**
	 * Deactivates the specified database
	 * @param database a database descriptor
	 * @param manager a state manager
	 * @return true, if the database was deactivated, false it was already inactive
	 */
	boolean deactivate(D database, StateManager manager);
	
	/**
	 * Returns the database identified by the specified id
	 * @param id a database identifier
	 * @return a database descriptor
	 * @throws IllegalArgumentException if no database exists with the specified identifier
	 */
	D getDatabase(String id);
	
	/**
	 * Returns the Balancer implementation used by this database cluster.
	 * @return an implementation of <code>Balancer</code>
	 */
	Balancer<Z, D> getBalancer();
	
	/**
	 * Returns an executor service used to execute transactional database writes.
	 * @return an implementation of <code>ExecutorService</code>
	 */
	ExecutorService getTransactionalExecutor();
	
	/**
	 * Returns an executor service used to execute non-transactional database writes.
	 * @return an implementation of <code>ExecutorService</code>
	 */
	ExecutorService getNonTransactionalExecutor();
	
	/**
	 * Returns a dialect capable of returning database vendor specific values.
	 * @return an implementation of <code>Dialect</code>
	 */
	Dialect getDialect();
	
	/**
	 * Returns a LockManager capable of acquiring named read/write locks on the specific objects in this database cluster.
	 * @return a LockManager implementation
	 */
	LockManager getLockManager();
	
	/**
	 * Returns a StateManager for persisting database cluster state.
	 * @return a StateManager implementation
	 */
	StateManager getStateManager();
	
	/**
	 * Returns a DatabaseMetaData cache.
	 * @return a <code>DatabaseMetaDataCache</code> implementation
	 */
	DatabaseMetaDataCache<Z, D> getDatabaseMetaDataCache();
	
	/**
	 * Indicates whether or not sequence detection is enabled for this cluster.
	 * @return true, if sequence detection is enabled, false otherwise.
	 */
	boolean isSequenceDetectionEnabled();
	
	/**
	 * Indicates whether or not identity column detection is enabled for this cluster.
	 * @return true, if identity column detection is enabled, false otherwise.
	 */
	boolean isIdentityColumnDetectionEnabled();
	
	/**
	 * Indicates whether or not non-deterministic CURRENT_DATE SQL functions will be evaluated to deterministic static values.
	 * @return true, if temporal SQL replacement is enabled, false otherwise.
	 */
	boolean isCurrentDateEvaluationEnabled();
	
	/**
	 * Indicates whether or not non-deterministic CURRENT_TIME functions will be evaluated to deterministic static values.
	 * @return true, if temporal SQL replacement is enabled, false otherwise.
	 */
	boolean isCurrentTimeEvaluationEnabled();
	
	/**
	 * Indicates whether or not non-deterministic CURRENT_TIMESTAMP functions will be evaluated to deterministic static values.
	 * @return true, if temporal SQL replacement is enabled, false otherwise.
	 */
	boolean isCurrentTimestampEvaluationEnabled();
	
	/**
	 * Indicates whether or not non-deterministic RAND() functions will be replaced by evaluated to static values.
	 * @return true, if temporal SQL replacement is enabled, false otherwise.
	 */
	boolean isRandEvaluationEnabled();
	
	/**
	 * Indicates whether or not this cluster is active, i.e. started, but not yet stopped.
	 * @return true, if this cluster is active, false otherwise.
	 */
	boolean isActive();

	void addListener(DatabaseClusterListener listener);
	
	void removeListener(DatabaseClusterListener listener);
	
	void addSynchronizationListener(SynchronizationListener listener);
	
	void removeSynchronizationListener(SynchronizationListener listener);
	
	void addConfigurationListener(DatabaseClusterConfigurationListener<Z, D> listener);
	
	void removeConfigurationListener(DatabaseClusterConfigurationListener<Z, D> listener);
	
	Durability<Z, D> getDurability();
}

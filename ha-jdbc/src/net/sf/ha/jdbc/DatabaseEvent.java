package net.sf.ha.jdbc;

import java.util.EventObject;

/**
 * @author  Paul Ferraro
 * @version $Revision$
 * @since   1.0
 */
public class DatabaseEvent extends EventObject
{
	/**
	 * Constructs a new DatabaseEvent.
	 * @param source
	 */
	public DatabaseEvent(String databaseId)
	{
		super(databaseId);
	}
	
	public String getDatabaseId()
	{
		return (String) this.getSource();
	}
}

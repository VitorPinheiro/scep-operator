/**
 * 
 */
package lac.inf.puc.rio.br.scep.database;

import java.util.ArrayList;

import lac.inf.puc.rio.br.scep.model.Query;

/**
 * This is the interface to communicate to the query database.
 * 
 * @author vitor
 *
 */
public interface IQueryDatabase 
{
	public Query getQuery(int queryID);
	
	public void addQuery(Query query);
	
	public ArrayList<Query> getAllQueries();

}
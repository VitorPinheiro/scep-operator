/**
 * 
 */
package lac.inf.puc.rio.br.scep.operator;

import java.util.ArrayList;
import java.util.List;

import eu.larkc.csparql.cep.api.RdfQuadruple;
import lac.inf.puc.rio.br.scep.model.Query;

/**
 * @author vitor
 *
 */
public interface ISCEPoperator 
{
	/**
	 * Add a triple (+ timestamp) to the stream of this operator.
	 * 
	 * @param subject
	 * @param predicate
	 * @param object
	 * @param timestamp (in milliseconds)
	 */
	//public void appendData(Integer queryID, String subject, String predicate, String object, long timestamp);
	
	//public void appendData(Integer queryID, List<RdfQuadruple> triples);
	
	
	/**
	 * Start running a query in the stream of the operator.
	 * 
	 * @param queryID The ID of the query to start.
	 * @return
	 */
	//public boolean startQuery(int queryID);
	
	
	/**
	 * Completely removes the query from the operator (stop if it is not stopped and delete it from the operator)
	 * 
	 * @param queryID
	 * @return
	 */
	//public boolean removeAndStopQuery(int queryID);
	
}
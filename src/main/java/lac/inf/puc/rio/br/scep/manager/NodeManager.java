package lac.inf.puc.rio.br.scep.manager;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lac.inf.puc.rio.br.scep.aggregator.Aggregator;
import lac.inf.puc.rio.br.scep.database.IQueryDatabase;
import lac.inf.puc.rio.br.scep.operator.SCEPoperator;
import lac.inf.puc.rio.br.scep.utils.Utils;

/**
 * This class is responsible for managing the operator logic.
 * To instantiate operators.
 * To receive the stream input from the aggregator.
 * 
 * This class is responsible for starting the node jar.
 * 
 * 
 *
 */
public class NodeManager 
{
	private static Aggregator _aggregator;
	private static SCEPoperator _scepOperator;
	private static Utils _utils;
	private static int _nodeID;
	
	private static Logger logger = LogManager.getLogger(NodeManager.class);
	
	/**
	 * Usage: java -jar app.jar num
	 * 
	 * num is the number of the query
	 * 
	 * This will start an operator that runs query num.
	 * 
	 * @param args
	 */
    public static void main( String[] args )
    {
    	int queryID = 1;
    	logger.info("------------------------ Execution Starting --------------------------");
    	System.out.println("------------------------ Execution Starting --------------------------");
    	logger.info("NodeManager for query {} initiating", queryID);
    	System.out.println("NodeManager for query "+queryID+" initiating");
    	
    	/* Preciso escolher quais queries esse operator vai ter. É no nome do topico Kafka.*/
    	_nodeID = Integer.valueOf(_utils.getProperty("node.properties","id"));
    	
    	ArrayList<Integer> queries = new ArrayList<Integer>();
    	queries.add(queryID);
    	
    	_aggregator = new Aggregator(_nodeID, queries);
    	_scepOperator = new SCEPoperator(_nodeID, "csparql1", queryID);
    	
    	logger.info("Aggregator and SCEPoperator for query {} initiated.", queryID);
    	System.out.println("Aggregator and SCEPoperator for query "+queryID+" initiated.");
    }    
}

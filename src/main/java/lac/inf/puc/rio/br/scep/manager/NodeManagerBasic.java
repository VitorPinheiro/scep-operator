/**
 * 
 */
package lac.inf.puc.rio.br.scep.manager;

import java.io.File;
import java.util.ArrayList;

import lac.inf.puc.rio.br.scep.aggregator.AggregatorGen;
import lac.inf.puc.rio.br.scep.operator.BasicOperator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lac.inf.puc.rio.br.scep.aggregator.Aggregator;
import lac.inf.puc.rio.br.scep.operator.SCEPoperator;
import lac.inf.puc.rio.br.scep.utils.Utils;

/**
 * @author vitor
 *
 */
public class NodeManagerBasic
{
	private static BasicOperator _basicOperator;
	private static BasicOperator _basicOperator2;
	private static BasicOperator _basicOperator3;
	private static BasicOperator _basicOperator4;
	private static BasicOperator _basicOperator5;
	private static BasicOperator _basicOperator6;
	private static BasicOperator _basicOperator7;
	private static BasicOperator _basicOperator8;
	private static AggregatorGen _aggregator;
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
    	int queryID = 12;
    	logger.info("------------------------ Execution Starting --------------------------");
    	System.out.println("------------------------ Execution Starting --------------------------");
    	logger.info("NodeManagerBasic for query {} initiating", queryID);
    	System.out.println("NodeManagerBasic for query "+queryID+" initiating");
    	
    	/* Preciso escolher quais queries esse operator vai ter. É no nome do topico Kafka.*/
    	_nodeID = Integer.valueOf(_utils.getProperty("nodebasic.properties","id"));

		_aggregator = new AggregatorGen(_nodeID, "nodebasic.properties");
		_basicOperator = new BasicOperator(_nodeID, "basicOp1", queryID);
		//_basicOperator2 = new BasicOperator(_nodeID, "basicOp2", queryID);
		//_basicOperator3 = new BasicOperator(_nodeID, "basicOp3", queryID);
		//_basicOperator4 = new BasicOperator(_nodeID, "basicOp4", queryID);
		//_basicOperator5 = new BasicOperator(_nodeID, "basicOp5", queryID);
		//_basicOperator6 = new BasicOperator(_nodeID, "basicOp6", queryID);
		//_basicOperator7 = new BasicOperator(_nodeID, "basicOp7", queryID);
		//_basicOperator8 = new BasicOperator(_nodeID, "basicOp8", queryID);

		//_basicOperator.prepareQuery(queryID);
    	
    	logger.info("Aggregator and BasicOperator for query {} initiated.", queryID);
    	System.out.println("Aggregator and BasicOperator for query "+queryID+" initiated.");
    }  

}

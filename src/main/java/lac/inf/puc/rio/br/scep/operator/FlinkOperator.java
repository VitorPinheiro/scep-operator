/**
 * 
 */
package lac.inf.puc.rio.br.scep.operator;

import java.util.List;
import java.util.Properties;

import eu.larkc.csparql.cep.api.RdfQuadruple;

/**
 * @author vitor
 *
 */
public class FlinkOperator extends AbstractSCEPoperator 
{

	protected FlinkOperator(int nodeID) {
		super(nodeID);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see lac.inf.puc.rio.br.scep.operator.AbstractSCEPoperator#appendData(java.lang.Integer, java.util.List)
	 */
	public void appendData(Integer queryID, List<RdfQuadruple> triples) {
		// TODO Auto-generated method stub

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		// TODO Auto-generated method stub
		
		//final StreamExecutionEnvironment see = StreamExecutionEnvironment.getExecutionEnvironment();
		Properties properties = new Properties();
		properties.setProperty("bootstrap.servers", "localhost:9092");
		properties.setProperty("group.id", "customerAnalytics");
		//FlinkKafkaConsumer<String> kafkaSource = new FlinkKafkaConsumer<>("customer.create", new SimpleStringSche
	}

}

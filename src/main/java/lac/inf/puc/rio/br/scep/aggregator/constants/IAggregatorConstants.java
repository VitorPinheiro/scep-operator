/**
 * 
 */
package lac.inf.puc.rio.br.scep.aggregator.constants;

/**
 * @author vitor
 *
 */
public interface IAggregatorConstants 
{
	public static String KAFKA_BROKERS = "localhost:9092";
	
	public static String CLIENT_ID="agg_client";
	
	public static Integer NUMBER_OF_TRIPLES_PER_MSG=71;
	
	public static Integer MAX_NO_MESSAGE_FOUND_COUNT=1000;
	
	public static String OFFSET_RESET_LATEST="latest";
	
	public static String OFFSET_RESET_EARLIER="earliest";
	
	public static Integer MAX_POLL_RECORDS=1;
}

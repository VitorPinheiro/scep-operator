/**
 * 
 */
package lac.inf.puc.rio.br.scep.utils;

/**
 * @author vitor
 *
 */
public interface IKafkaConstants {
	public static String KAFKA_BROKERS = "localhost:9092";
		
	public static String CLIENT_ID="client1";
	
	public static Integer NUMBER_OF_TRIPLES_PER_MSG=71;
	
	public static String TOPIC_NAME="outputQuery4";	
	
	public static String GROUP_ID_CONFIG="consumerGroup1";
	
	public static Integer MAX_NO_MESSAGE_FOUND_COUNT=1000;
	
	public static String OFFSET_RESET_LATEST="latest";
	
	public static String OFFSET_RESET_EARLIER="earliest";

	public static String OFFSET_RESET_SMALLEST="smallest";
	
	public static Integer MAX_POLL_RECORDS=1;
}

/**
 * 
 */
package lac.inf.puc.rio.br.scep.operator.constants;

/**
 * @author vitor
 *
 */
public interface IOperatorConstants {
	public static String KAFKA_BROKERS = "localhost:9092";
	
	public static String OFFSET_RESET_EARLIER="earliest";

	public static String OFFSET_RESET_SMALLEST="smallest";
	
	public static Integer MAX_POLL_RECORDS=1;	
	
	public static Integer MAX_NO_MESSAGE_FOUND_COUNT=100000000;
	
	public static Integer NUMBER_OF_TRIPLES_PER_MSG=null;
}

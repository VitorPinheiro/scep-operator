/**
 * 
 */
package lac.inf.puc.rio.br.scep.aggregator.constants;

import lac.inf.puc.rio.br.scep.utils.Utils;

/**
 * @author vitor
 *
 */
public class IAggregatorConstants
{
	private static Utils _utils;

	public static String KAFKA_BROKERS = _utils.getProperty("scepconfig.properties","KAFKA_BROKER"); // "localhost:9092";
	
	public static String CLIENT_ID="agg_client";
	
	public static Integer NUMBER_OF_TRIPLES_PER_MSG=71;
	
	public static Integer MAX_NO_MESSAGE_FOUND_COUNT=1000;
	
	public static String OFFSET_RESET_LATEST="latest";
	
	public static String OFFSET_RESET_EARLIER= _utils.getProperty("scepconfig.properties","OFFSET_RESET_EARLIER"); //"earliest";
	
	public static Integer MAX_POLL_RECORDS= Integer.valueOf(_utils.getProperty("scepconfig.properties","MAX_POLL_RECORDS")); //1;
}

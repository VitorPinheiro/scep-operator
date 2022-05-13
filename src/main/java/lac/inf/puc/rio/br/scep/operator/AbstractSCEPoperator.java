/**
 * 
 */
package lac.inf.puc.rio.br.scep.operator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.larkc.csparql.cep.api.RdfQuadruple;
import lac.inf.puc.rio.br.scep.database.IQueryDatabase;
import lac.inf.puc.rio.br.scep.database.QueryDatabase;
import lac.inf.puc.rio.br.scep.utils.Utils;
import lac.inf.puc.rio.br.scep.utils.WriterToFile;

/**
 * @author vitor
 *
 * Esta classe abstrata é o molde de como um SCEPoperator deve ser criado. Ela só força que sua classe SCEPoperator tenha um
 * método de appendData. O appendData deve receber uma lista de triplas RDF e uma query que deve ser aplicada a estas triplas.
 * 
 * Os dados devem ser retirados de um topico Kafka.
 * Os dados produzidos pela query devem ser publicados em um tópico kafka criado pelo SCEPoperator.
 * 
 * O objeto query, possui metodos que podem ser utilizados para gerarem os nomes dos topicos tanto de recebmento dos dados quanto
 * de publicacao dos dados gerados pela query.
 * 
 * O metodo Query.get_outputStreamID() retorna o nome do topico que deve ser utilizado para publicar os resultados retornados pela query.
 * 
 * O método Query.get_inputStreamIDFromAggregator() retorna o nome do topico para ser consumido. Este método só deve ser usado para
 * definir o topico de consumo dos dados, caso este SCEPoperator utilize o aggregator do DSCEP. 
 * 
 * O Aggregator do DSCEP só deve ser utilizado se o RSP engine utilizado não faça window management e/ou não possua suporte a multiplas 
 * streams.
 * 
 * Caso não precise do Aggregator, use o método Query.get_inputStreamsIDs(), ele vai retornar uma lista de topicos que devem ser consumidos
 * para a query em questão.
 *
 */
public abstract class AbstractSCEPoperator 
{
	protected IQueryDatabase _queryDb;
	protected Utils _utils;	
	protected WriterToFile _writerToFile;	
	protected Map<Integer, Integer> _queriesWindowCount;
	
	
	protected int _nodeID;
	protected long _startTime;
	protected boolean _resetStartTime = false;
	protected int _numTriplesIn = 0;
	
	protected AbstractSCEPoperator(int nodeID)
	{
		if(_queryDb == null) {
			//_queryDb = new QueryDatabase();
			_queryDb = QueryDatabase.getInstance();
		}
		
		_nodeID = nodeID;
		_queriesWindowCount = new HashMap<Integer, Integer>();
	}
	
	/**
	 * Utilizada por terceiros para inserir dados no SCEP engine.
	 * 
	 * @param queryID: A query que deve processar as triplas
	 * @param triples: As triplas de entrada.
	 */
	//abstract public void appendData(Integer queryID, List<Object> triples);
	
	
	// Functions to manage and record processing time into log files.
	// Function to serialize an json message
	
	/**
	 * Função responsavel por inicializar a classe que escreve em arquivo. É opcional, mas o suporte já é oferecido aqui.
	 * @param queryID
	 */
	protected void initializeOutputFiles(int queryID, String opName)
	{
		_writerToFile = new WriterToFile("SOp"+opName+"_node_"+_nodeID+"_query_"+queryID); // Cada query vai ter seu arquivo de performance.
	}
	
	public Long getStartTime()
	{
		return _startTime;
	}
	
	public void resetStartTime()
	{
		_resetStartTime = true;
	}
	
	public void writeToFile(String time, int executionNum, int numTriplesOut)
	{
		_writerToFile.writeToSpeedFileNewLine(time, executionNum, _numTriplesIn, numTriplesOut);
	}
	
	public void writeJsonOutToFile(String json, int executionNum)
	{
		_writerToFile.writeToJsonOutFile(json, executionNum);
	}
}

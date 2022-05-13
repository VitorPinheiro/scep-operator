/**
 * 
 */
package lac.inf.puc.rio.br.scep.aggregator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.OffsetCommitCallback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import eu.larkc.csparql.cep.api.RdfQuadruple;
import lac.inf.puc.rio.br.scep.aggregator.constants.IAggregatorConstants;
import lac.inf.puc.rio.br.scep.communication.ConsumerCreator;
import lac.inf.puc.rio.br.scep.communication.ProducerCreator;
import lac.inf.puc.rio.br.scep.database.IQueryDatabase;
import lac.inf.puc.rio.br.scep.database.QueryDatabase;
import lac.inf.puc.rio.br.scep.model.Query;
import lac.inf.puc.rio.br.scep.model.StreamSnip;
import lac.inf.puc.rio.br.scep.model.StreamSnipBlocks;
import lac.inf.puc.rio.br.scep.model.TriplesBlock;
import lac.inf.puc.rio.br.scep.model.WindowType;
import lac.inf.puc.rio.br.scep.utils.ShutdownableThread;
import lac.inf.puc.rio.br.scep.utils.Utils;
import lac.inf.puc.rio.br.scep.utils.WriterToFile;

/**
 * @author vitor
 *
 *	This class is resposible for receiving all stream snips from all inputs of the queries registered in its database.
 *	It will receive all inputs and create one only input stream for each query that it is responsible for.
 *
 *	Each query can have only one aggregator within all the distribute infrastructure environment. And each Aggregator can be
 *	responsible for more than one query.
 */
public class Aggregator 
{
	private Utils _utils;	
	
	private IQueryDatabase _queryDb;
	private Map<Integer, StreamSnip> _streamSnipToQuery;
	private Map<Integer, StreamSnipBlocks> _streamSnipBlocksToQuery;
	private Map<Integer, WriterToFile> _writerToFileToQuery;
	private Map<Integer, Long> _startTime;
	private Map<Integer, Long> _idleTime;
	private Map<Integer, Boolean> _dataSent;
		
	private ArrayList<Query> _queriesOfThisAgg;
	private int _nodeID;
	//private boolean _extraTripleSent = false;
	private Map<Integer, Boolean> _extraTripleSent;
	
	private int _msgsSentCount;
	private boolean _isTwitterDB;
	private int _numTriplesIn = 0;
	
	// Aggregator ID.
	private final String _aggregatorID;
	
	private static Logger _logger = LogManager.getLogger(Aggregator.class);
	
	public Aggregator(int nodeID, List<Integer> queriesID)
	{
		_logger.debug("Aggregator of node {} is being created...", nodeID);
		
		_nodeID = nodeID;
		_aggregatorID = "AGG_"+ nodeID;
		_queryDb = QueryDatabase.getInstance();
		//_queryDb = new QueryDatabase();
		_streamSnipToQuery = new HashMap<Integer, StreamSnip>();
		_streamSnipBlocksToQuery = new HashMap<Integer, StreamSnipBlocks>();
		_extraTripleSent = new HashMap<Integer, Boolean>();
		_queriesOfThisAgg = new ArrayList<Query>();
		_writerToFileToQuery = new HashMap<Integer, WriterToFile>();
		_startTime = new HashMap<Integer, Long>();
		_idleTime = new HashMap<Integer, Long>();
		_dataSent = new HashMap<Integer, Boolean>(); 
		_msgsSentCount = 1;
		
		// Start Kafka consumers for each query
		for(int i=0; i<queriesID.size();i++)
		{
			Query query = _queryDb.getQuery(queriesID.get(i));
			_queriesOfThisAgg.add(query);
			
			addQueryToMapping(query.get_queryID());
			
			String consumerName = "AggConsumer_"+_nodeID+"_"+query.get_queryID();
			
			_writerToFileToQuery.put(query.get_queryID(), new WriterToFile("Agg_node_"+_nodeID+"_query_"+query.get_queryID())); 
			_startTime.put(query.get_queryID(), null);
			_idleTime.put(query.get_queryID(), null);
			_dataSent.put(query.get_queryID(), true);
			
			//System.out.println("Aggregator: Starting consumer for query "+query.get_queryID()+" with consumer group name: "+consumerName);
			
			// WARINNG: O METODO PARA AQUI. ESSE runConsumer PUXA O PROCESSAMENTO.
			// TODO: O programa para aqui, preciso fazer com que esse consumer seja uma thread separada.
			// Pq se nao ele nao continua o processamento e o operador nao starta.
			// Cada consumer diferente precisa ser uma thread diferente, acho eu.
			runConsumer(query.get_inputStreamsIDs(), consumerName); 
			//System.out.println("Aggregator consumer started.");			
		}
	}
	
	/**
	 * Whenever the Aggregator receives a new stream snip it will send to this method.
	 * 
	 * @param jsonStreamSnip
	 */
	public void addNewStreamSnip(String jsonStreamSnip)
	{		
		System.out.println("New stream snip arrived for aggregator of node: "+_nodeID);
		// Ver quem produziu esse pedaço de stream
		String producer = _utils.getAttributeFromMsg(jsonStreamSnip, "Producer");	
		
		if(_utils.getAttributeFromMsg(jsonStreamSnip, "isTwitterDB") != null && 
				_utils.getAttributeFromMsg(jsonStreamSnip, "isTwitterDB").equals("true"))
			_isTwitterDB = true;
		else
			_isTwitterDB = false;				
		
		for(int i=0;i<_queriesOfThisAgg.size();i++)
		{						
			if(producer == null)
			{
				_logger.info("O atributo producer da seguinte stream snip é nulo "+jsonStreamSnip);
				_utils.error("O atributo producer da seguinte stream snip é nulo "+jsonStreamSnip);
				return;
			}				
			
			String inputStreamID;
			Integer queryNum = _utils.parseToIntegerIfPossible(producer);
			if(queryNum != null)
			{
				inputStreamID = "Q"+queryNum+"O";
			}	
			else
			{
				inputStreamID = producer;
			}
			
			System.out.println("Chegou no aggregator inputStreamID = "+inputStreamID);
			
			if(_queriesOfThisAgg.get(i).get_inputStreamsIDs().contains(inputStreamID))
			{		
				// Inicia a contagem de tempo de processar
				// Aqui chega o primeiro snip, o tempo só reseta quando uma msg é enviada
				int queryID = _queriesOfThisAgg.get(i).get_queryID();
				if(_dataSent.get(queryID))
				{
					_startTime.put(queryID, System.currentTimeMillis());
					_idleTime.put(queryID, 0L);
					_dataSent.put(queryID, false); // resetei o tempo, espera enviar de novo
					
					_numTriplesIn = 0;
				}
				
				/**
				 * Pode-se perceber aqui que o snip de cada query, já vai conter todos os dados de todas as
				 * input streams dela em um timeframe. Por isso que eu guardo todos os snips
				 * de todas as input streams no mesmo lugar. Quando eu tiver o windowSize certo eu envio pra algum
				 * nó que tenha essa query.
				 */
				
				if(_isTwitterDB)
				{ 					
					//System.out.println("queries.get(i).get_queryID() = "+_queriesOfThisAgg.get(i).get_queryID());
					
					ArrayList<TriplesBlock> triplesBlock = _utils.getTriplesBlockFromJSONSnip(jsonStreamSnip);										
					
					_streamSnipBlocksToQuery.get(_queriesOfThisAgg.get(i).get_queryID()).addBlocks(triplesBlock);

					// Preciso contar o triplesIn agora pq eu tiro as triplas ja lidas desta estrutura: _streamSnipBlocksToQuery
					_numTriplesIn = _numTriplesIn + _streamSnipBlocksToQuery.get(_queriesOfThisAgg.get(i).get_queryID()).getTotalNumberOfTriples();
					
					synchronized(_streamSnipBlocksToQuery.get(_queriesOfThisAgg.get(i).get_queryID()))
					{
						//System.out.println("checkAndSendWindowToOperator will begin");
						checkAndSendBlockWindowToOperator(_queriesOfThisAgg.get(i));
					}
				}
				else
				{
					StreamSnip snip = new StreamSnip();				
					snip.addTriples(_utils.getTriplesFromJSONSnip(jsonStreamSnip));					
					
					_numTriplesIn = _numTriplesIn + snip.getNumberOfTriples();
					
					_streamSnipToQuery.get(_queriesOfThisAgg.get(i).get_queryID()).addSnip(snip);
					
					// Verificar se já da para enviar alguma window para seu operador (operador é uma query)
					//System.out.println("Entering synchromized area: checkAndSendWindowToOperator");
					synchronized(_streamSnipToQuery.get(_queriesOfThisAgg.get(i).get_queryID()))
					{
						//System.out.println("checkAndSendWindowToOperator will begin");
						checkAndSendWindowToOperator(_queriesOfThisAgg.get(i));
					}
				}
			}
		}
	}
	
	private StreamSnipBlocks addDummyBlock(StreamSnipBlocks streamBlocks, int numOfDummies)
	{
		if(numOfDummies == 0)
			return streamBlocks;
		
		if(streamBlocks == null)
			return null;
		
		TriplesBlock dummyBlock = new TriplesBlock();
		
		int lastBlockIndex = streamBlocks.getNumberOfBlocks() - 1;
		long lastTimestamp = streamBlocks.getBlockWithOutDeleting(lastBlockIndex).getTimestamp();
				
		for(int i=0; i<numOfDummies; i++)
		{
			RdfQuadruple dummyTriple = new RdfQuadruple("http://example.org/sss", "http://example.org/ppp", "http://example.org/ooo", lastTimestamp+1);
			dummyBlock.addTriple(dummyTriple);
		}
		
		streamBlocks.addBlock(dummyBlock);
		
		return streamBlocks;
	}
	
	private List<RdfQuadruple> addDummyTriple(List<RdfQuadruple> list)
	{
		int lastIndex = list.size()-1;
		RdfQuadruple lastTriple = list.get(lastIndex);
		RdfQuadruple dummyTriple = new RdfQuadruple("http://example.org/sss", "http://example.org/ppp", "http://example.org/ooo", lastTriple.getTimestamp()+1);
		list.add(dummyTriple);		
		return list;
	}
	
	/**
	 * Check the query in the _streamSnipToQuery mapping. If in the stream snips associated to it there is at least one window
	 * so it will send to the query, which is here in this same node.
	 * In the future, this routing will be separated of its corresponding query. 
	 * @param query
	 */
	private void checkAndSendBlockWindowToOperator(Query query)
	{
		int numerOfBlocks = _streamSnipBlocksToQuery.get(query.get_queryID()).getNumberOfBlocks();
		if(numerOfBlocks == 0)
		{
			//System.out.println("A query "+query.get_queryID()+" não tem nenhum block como seu input (tamanho 0)");
			return; // vai pra proxima query
		}
		
		if(query.getWindowType() == WindowType.NUMBER_OF_TRIPLES)
		{
			//System.out.println("Query que precisa receber a snip é do tipo: "+query.getWindowType());
			
			
			int windowSize = query.getWindowSize();
			
			StreamSnipBlocks blocksStream = _streamSnipBlocksToQuery.get(query.get_queryID());
			
			// calcular se ja tem blocos suficientes para enviar ao operador.
			//System.out.println("blocksStream.getNumberOfBlocks() = "+blocksStream.getNumberOfBlocks());
			
			int totalNumberOfTriples = 0;
			boolean sendData = false;
			int numberOfBlocksToSend = 0;
			for(int i=0; i<blocksStream.getNumberOfBlocks(); i++)
			{
				totalNumberOfTriples = totalNumberOfTriples + blocksStream.getBlockWithOutDeleting(i).size();
				//System.out.println("totalNumberOfTriples ("+i+") = "+totalNumberOfTriples);
				
				if(totalNumberOfTriples == (windowSize-2))
				{					
					sendData = true;					
					numberOfBlocksToSend = i+1;
					
					//System.out.println("1totalNumberOfTriples = "+totalNumberOfTriples);
					//System.out.println("1numberOfBlocksToSend = "+numberOfBlocksToSend);
					break;
				}
				else if(totalNumberOfTriples > (windowSize-2))
				{
					sendData = true;
					numberOfBlocksToSend = i;

					//System.out.println("2totalNumberOfTriples = "+totalNumberOfTriples);
					//System.out.println("2numberOfBlocksToSend = "+numberOfBlocksToSend);
					break;
				}
			}
			
			if(!sendData)
				// AQUI: if(totalNumberOfTriples >= (windowSize - blocksStream.getBlockWithOutDeleting(0).size()))
				{
					// Vou enviar as triplas assim que elas chegarem e completo com dummy
				// é só descomenta o "AQUI" ali emcima desse comentario que ele volta a nao enviar assim que recebe.
				// Se descomentar ele envia só se tiver faltando o tamanho de um bloco
					sendData = true;
					numberOfBlocksToSend = blocksStream.getNumberOfBlocks();
					totalNumberOfTriples = blocksStream.getTotalNumberOfTriples();
					
					//System.out.println("3totalNumberOfTriples = "+totalNumberOfTriples);
					//System.out.println("3numberOfBlocksToSend = "+numberOfBlocksToSend);
				}
						
			if(sendData)
			{
				//System.out.println("Data will be sent.");
				
				StreamSnipBlocks snipBlocksToSend = new StreamSnipBlocks();
				
				for(int i=0; i<numberOfBlocksToSend; i++)
				{
					snipBlocksToSend.addBlock(blocksStream.getBlock());
				}
				
				
				
				int numOfTriplesToSend = snipBlocksToSend.getTotalNumberOfTriples();
				
				//System.out.println("triples to be sent = "+numOfTriplesToSend);
				//System.out.println("windowSize = "+windowSize);
				
				int diff = windowSize - numOfTriplesToSend;
				if(_extraTripleSent.get(query.get_queryID()))
				{
					diff--;
				}
				
				_logger.debug("snip (before adding dummy) = "+numOfTriplesToSend);
				//System.out.println("snip (before adding dummy) = "+numOfTriplesToSend);

				snipBlocksToSend = addDummyBlock(snipBlocksToSend, diff);
				
				_logger.debug("snip (after adding dummy) = "+snipBlocksToSend.getTotalNumberOfTriples());
				//System.out.println("snip (after adding dummy) = "+snipBlocksToSend.getTotalNumberOfTriples());
				
				runProducer(snipBlocksToSend, query);
				
				// Já enviei para o operador, pode esvaziar a fila.
				_streamSnipBlocksToQuery.get(query.get_queryID()).clear();
			}			
			
			// Depois do primeiro envio sempre vai ter uma tripla extra no operador, pode ser a dummy ou nao.
			_extraTripleSent.put(query.get_queryID(), true);
		}
		
		if(query.getWindowType() == WindowType.FIXED_TIME)
		{
			_logger.info("WindowType.TIME not implemented yet");
			_utils.error("WindowType.TIME not implemented yet");		
		}
		
		if(query.getWindowType() == WindowType.SLIDING_TIME)
		{			
			_logger.info("WindowType.SLIDING_TIME not implemented yet");
			_utils.error("WindowType.SLIDING_TIME not implemented yet");
		}
	}
	
	/**
	 * Check the query in the _streamSnipToQuery mapping. If in the stream snips associated to it there is at least one window
	 * so it will send to the query, which is here in this same node.
	 * In the future, this routing will be separated of its corresponding query. 
	 * @param query
	 */
	private void checkAndSendWindowToOperator(Query query)
	{
		int numerOfTriples = _streamSnipToQuery.get(query.get_queryID()).getNumberOfTriples();
		if(numerOfTriples == 0)
		{
			//System.out.println("A query "+query.get_queryID()+" não tem nenhuma snip como seu input (tamanho 0)");
			return; // vai pra proxima query
		}
		
		if(query.getWindowType() == WindowType.NUMBER_OF_TRIPLES)
		{
			//System.out.println("Query que precisa receber a snip é do tipo: "+query.getWindowType());
			
			int snipSize = numerOfTriples;
			int windowSize = query.getWindowSize();
			
			if(snipSize >= windowSize)
			{
				int diff = snipSize - windowSize;	
				List<RdfQuadruple> triplesToSendToOperator; 
				
				// send all the snip as a window.
				//System.out.println("diff (snipSize - windowSize) = "+diff);
				if(diff == 0)
				{
					// adiciona uma tripla dummy e envia.
					//System.out.println("Temos exatamente 1 janela nesse snip.");
					//System.out.println("SnipSize = "+snipSize);
					//System.out.println("WindowSize = "+windowSize);
					triplesToSendToOperator = _streamSnipToQuery.get(query.get_queryID()).getTriples();
					
					if(!_extraTripleSent.get(query.get_queryID()))
					{
						triplesToSendToOperator = addDummyTriple(triplesToSendToOperator);
						
						//System.out.println("snip (after adding dummy) = "+triplesToSendToOperator.size());
					}
					else
						//System.out.println("snip (not adding dummy) = "+triplesToSendToOperator.size());
					
					//System.out.println("Triple extra foi = "+triplesToSendToOperator.get(triplesToSendToOperator.size()-1));					
					//_scepOpInstance.appendData(triplesToSendToOperator);
					// TODO: SEND DATA TO OPERATOR
					runProducer(triplesToSendToOperator, query);
					
					// Já enviei para o operador, pode esvaziar a fila.
					_streamSnipToQuery.get(query.get_queryID()).clearSnip();
				}
				else
				{
					// dif > 0 então pode ser que tenha mais de uma janela pra enviar
					int numWindowsInSnip = (int) snipSize / windowSize;
					//System.out.println("Temos "+numWindowsInSnip+" janelas dentro deste snip.");
					int numTriplesToSend = (numWindowsInSnip*windowSize);	
					
					if( (numWindowsInSnip*windowSize) - snipSize == 0)
					{ // Snip size é multiplo do tamanho da janela. Temos o numero de triplas certinho.
						triplesToSendToOperator = _streamSnipToQuery.get(query.get_queryID()).getTriples(numTriplesToSend); // Já deleta as triplas que ele pega.
						
						if(!_extraTripleSent.get(query.get_queryID()))
						{
							triplesToSendToOperator = addDummyTriple(triplesToSendToOperator);							
						}
						
					}
					else
					{ // snip size maior do que o numero de triplas da janela
						if(_extraTripleSent.get(query.get_queryID()))
						{
							//System.out.println("Já adicionei dummy, nao preciso enviar mais do que o tamanho da janela.");
							triplesToSendToOperator = _streamSnipToQuery.get(query.get_queryID()).getTriples(numTriplesToSend); // Já deleta as triplas que ele pega.
						}
						else
						{
							//System.out.println("Não adicionei dummy, mas peguei uma triple extra da stream e mandei.");
							triplesToSendToOperator = _streamSnipToQuery.get(query.get_queryID()).getTriples(numTriplesToSend+1); // Já deleta as triplas que ele pega.
						}
					}
					
					//System.out.println("Triple extra foi = "+triplesToSendToOperator.get(triplesToSendToOperator.size()-1));
					//_scepOpInstance.appendData(triplesToSendToOperator);		
					// TODO: SEND DATA TO OPERATOR
					runProducer(triplesToSendToOperator, query);
					
					List<RdfQuadruple> triplesNotSent = _streamSnipToQuery.get(query.get_queryID()).getTriples();
					//System.out.println("Triplas que não foram enviadas ao operador ("+triplesNotSent.size()+"): "+triplesNotSent);
				}
			}
			else
			{
				// snipSize < windowSize
				// Melhor esperar mais dado pra fechar a janela.
				
				// Se for o isTweetDB, eu completo com dummies e envio.
				// Streams do isTweetDB só vao cair aqui, elas sempre vao ser enviadas sendo menor do que o tamanho da janela.
				// WARNING: Se o tamanho da janela na query for alterado, é preciso alterar no gerador de triplas tb, caso contrario,
				// não vai funcionar corretamente.
				
				List<RdfQuadruple> triplesNotSent = _streamSnipToQuery.get(query.get_queryID()).getTriples();
				//System.out.println("SnipSize = "+numerOfTriples);
				//System.out.println("Triplas que não foram enviadas ao operador ("+triplesNotSent.size()+"): "+triplesNotSent);
			}
			
			// Depois do primeiro envio sempre vai ter uma tripla extra no operador, pode ser a dummy ou nao.
			_extraTripleSent.put(query.get_queryID(), true);
		}
		
		if(query.getWindowType() == WindowType.FIXED_TIME)
		{
			/**
			 * Eu posso também fazer as sliding windows aqui nessa classe.
			 * Na query eu usaria sempre TUMBLING, e aqui eu aplicaria o STEP de cada query.
			 * Por exemplo, se uma query é [RANGE 30m STEP 10m], na query estaria [RANGE 30m TUMBLING]
			 * E nessa classe, a StreamSnip correspondente a essa query ia ser enviada com um shift de 10m.
			 * 
			 * E nesse caso o CSPARQL só processa quando recebe o dado depois do 30m pra fechar a janela. 
			 * Preciso sempre ter certeza que a snip que eu mandar vai ser processada pela janela.
			 * 
			 * Para garantir que uma janela seria processada eu sempre precisaria mandar pelo menos 1 evento a mais
			 * da janela pra ela ser processada. Seria um evento que eu sempre iria perder.
			 * 
			 * https://www.evernote.com/l/AStmerOXJalC87kdF5G_tegtrrB_-r05IiU
			 * Aqui explica q eu posso fazer a gestao do STEP da query CSPARQL de fora dela. Teria que fazer isso pra
			 * fazer possivel esse tipo de query aqui ter o placement da Henriette.
			 * 
			 */
			List<RdfQuadruple> triplesToSendToOperator;
			
			triplesToSendToOperator = _streamSnipToQuery.get(query.get_queryID()).getTriples();
			
			//System.out.println("=======================================");
			//System.out.println(triplesToSendToOperator);
			//System.out.println("=======================================");
			
			runProducer(triplesToSendToOperator, query);
			
			//_utils.error("WindowType.TIME not implemented yet");		
		}
		
		if(query.getWindowType() == WindowType.SLIDING_TIME)
		{
			_logger.info("WindowType.SLIDING_TIME not implemented yet");
			_utils.error("WindowType.SLIDING_TIME not implemented yet");
		}
	}
	
	private void addQueryToMapping(int queryID)
	{
		if(!_streamSnipToQuery.containsKey(queryID))
		{
			_streamSnipToQuery.put(queryID, new StreamSnip());
		}
		
		if(!_streamSnipBlocksToQuery.containsKey(queryID))
		{
			_streamSnipBlocksToQuery.put(queryID, new StreamSnipBlocks());
		}
		
		if(!_extraTripleSent.containsKey(queryID))
		{
			_extraTripleSent.put(queryID, false);
		}
	}
	
	public void runConsumer(List<String> topicName, String consumerGroupName)
    {
		//System.out.println("Aggregator: runConsumer");
		
		_logger.debug("Consumer(listening) on topic = {} - ConsumerGroup = {}", topicName, consumerGroupName );
		
		KafkaConsumer<Integer, String> consumer = ConsumerCreator.createConsumer(topicName, 
				IAggregatorConstants.KAFKA_BROKERS, 
				consumerGroupName, 
				IAggregatorConstants.MAX_POLL_RECORDS, 
				IAggregatorConstants.OFFSET_RESET_EARLIER);

    	AggregatorConsumer aggConsumer = new AggregatorConsumer(consumer, consumerGroupName);
    	aggConsumer.start();
    }
		
	private class AggregatorConsumer extends ShutdownableThread {
		private final KafkaConsumer<Integer, String> _consumer;
		
	    public AggregatorConsumer(KafkaConsumer<Integer, String> consumer, String consumerName)
	    {
	    	super(consumerName, false);
	    	
	    	_consumer = consumer;
	    }

		@Override
		public void doWork() {
			// TODO Auto-generated method stub
			_logger.debug("AggregatorName = {} - Consumer started on topics = {}", _aggregatorID, _consumer.listTopics() );
			while (true) {
				final ConsumerRecords<Integer, String> consumerRecords = _consumer.poll(1000);
				/*if (consumerRecords.count() == 0) {
					noMessageToFetch++;
					if (noMessageToFetch > IAggregatorConstants.MAX_NO_MESSAGE_FOUND_COUNT)
						break;
					else
						continue;
				}*/
				
				for (ConsumerRecord<Integer, String> record : consumerRecords) {
					//System.out.println("Record Key " + record.key());
					//System.out.println("Aggregator Consumer: Record value " + record.value());
					//System.out.println("Record partition " + record.partition());
					//System.out.println("Record offset " + record.offset());
					
					addNewStreamSnip(record.value());
		        }
				
				//consumer.commitAsync();
				
				// We send the commit and carry on, but if the commit fails, the failure and the offsets will be logged.
				_consumer.commitAsync(new OffsetCommitCallback() {
			        public void onComplete(Map<TopicPartition,
			        OffsetAndMetadata> offsets, Exception e) {
			            if (e != null)
			            {
			            	_logger.info("Commit failed for offsets: "+ offsets+". Exeption = "+ e);
			                _utils.error("Commit failed for offsets: "+ offsets+". Exeption = "+ e);
			            }
			        }
			    });
			}
		}

		@Override
		public void onShutDown() 
		{
			_consumer.close();
		}
		
	}
	
	private void runProducer(StreamSnipBlocks triples, Query query) 
	{
		System.out.println("AggregatorBlocks: runProducer");
		
		if(triples == null || triples.getNumberOfBlocks() == 0)
		{
			System.out.println("Query "+query.get_queryID()+" produziu um conjunto vazio.");
			return;
		}
		
		Producer<Long, String> producer = ProducerCreator.createProducer(IAggregatorConstants.KAFKA_BROKERS, _aggregatorID);	
		
		String topicName = query.get_inputStreamIDFromAggregator();
		
		_logger.debug("AggregatorBlocksName {} - Publishing on topic {}", _aggregatorID, topicName );
		System.out.println("AggregatorBlocksName "+_aggregatorID+" - Publishing on topic "+topicName );
		
		JsonObject jsonStream = _utils.streamBlocksToJsonObject(triples, 
													Integer.toString(_nodeID),   // The ID of the node which this snip was produced.
													Integer.toString(_msgsSentCount), // It is only a counter that says how many messages the sender on the nodeID generated.
													_aggregatorID, // The query/aggregator/initialStream that produced this snip.		
													true );	
		
		final ProducerRecord<Long, String> record = new ProducerRecord<Long, String>(topicName, jsonStream.toString());
		
		try 
		{
			RecordMetadata metadata = producer.send(record).get();
			
			//System.out.println("Aggregator: Record sent with key " + _msgsSentCount + " to partition " + metadata.partition()
			//		+ " with offset " + metadata.offset());
			
			Long timeToProcess = System.currentTimeMillis() - _startTime.get(query.get_queryID());			
			
			_writerToFileToQuery.get(query.get_queryID()).writeToSpeedFileNewLine(timeToProcess.toString(), _msgsSentCount, _numTriplesIn, triples.getTotalNumberOfTriples());			
			_msgsSentCount++;			
			_dataSent.put(query.get_queryID(), true);			
		
			_logger.debug("AggregatorBlocksName {} - TimeToProcess {}", _aggregatorID, timeToProcess );
			System.out.println("Aggregator: Enviando stream para query "+query.get_queryID());
			System.out.println("Aggregator: Stream tem "+triples.getTotalNumberOfTriples()+" triplas");
			
		} catch (ExecutionException e) {
			_logger.info("Aggregator: Error in sending record");
			_logger.info(e);
			System.out.println("Aggregator: Error in sending record");
			System.out.println(e);
		} catch (InterruptedException e) {
			_logger.info("Aggregator: Error in sending record");
			_logger.info(e);
			System.out.println("Aggregator: Error in sending record");
			System.out.println(e);
		}
	}
	
	public void runProducer(List<RdfQuadruple> triples, Query query)
    {		
		//System.out.println("Aggregator: runProducer");
		
		if(triples == null || triples.size() == 0)
		{
			System.out.println("Query "+query.get_queryID()+" produziu um conjunto vazio.");
			return;
		}
	      
		Producer<Long, String> producer = ProducerCreator.createProducer(IAggregatorConstants.KAFKA_BROKERS, _aggregatorID);
		
		String topicName = query.get_inputStreamIDFromAggregator();
		
		_logger.debug("AggregatorName {} - Publishing on topic {}", _aggregatorID, topicName );
		System.out.println("AggregatorName "+_aggregatorID+" - Publishing on topic {}"+topicName );
		
		JsonObject jsonStream = _utils.toJsonObject(triples, // Triples to send
														Integer.toString(_nodeID),   // The ID of the node which this snip was produced.
														Integer.toString(_msgsSentCount), // It is only a counter that says how many messages the sender on the nodeID generated.
														_aggregatorID,
														false); // The query/aggregator/initialStream that produced this snip.
		
		final ProducerRecord<Long, String> record = new ProducerRecord<Long, String>(topicName, jsonStream.toString());
		
		try 
		{
			RecordMetadata metadata = producer.send(record).get();
			System.out.println("Aggregator: Record sent with key " + _msgsSentCount + " to partition " + metadata.partition()
					+ " with offset " + metadata.offset());
			_msgsSentCount++;
			System.out.println("Aggregator: Enviando stream para query "+query.get_queryID());
			System.out.println("Aggregator: Stream tem "+triples.size()+" triplas");
			
		} catch (ExecutionException e) {
			_logger.info("Aggregator: Error in sending record");
			_logger.info(e);
			System.out.println("Aggregator: Error in sending record");
			System.out.println(e);
		} catch (InterruptedException e) {
			_logger.info("Aggregator: Error in sending record");
			_logger.info(e);
			System.out.println("Aggregator: Error in sending record");
			System.out.println(e);
		}
    }
	
	
	/*private void runProducer(List<RdfQuadruple> triples, Query query) 
	{
		System.out.println("Aggregator: runProducer");
		if(triples == null || triples.size() == 0)
		{
			System.out.println("Query "+query.get_queryID()+" produziu um conjunto vazio.");
			return;
		}
	      
		Producer<Long, String> producer = ProducerCreator.createProducer(IOperatorConstants.KAFKA_BROKERS, IOperatorConstants.CLIENT_ID);
		
		int toIndex = 0;
		for (int index = 0; index < triples.size(); index = index + IAggregatorConstants.NUMBER_OF_TRIPLES_PER_MSG) 
		{
			toIndex = index + IAggregatorConstants.NUMBER_OF_TRIPLES_PER_MSG;
			if(toIndex >= triples.size() )
				toIndex = triples.size();

			String aggregatorName = "AGG"+_nodeID;
			String topicName = query.get_inputStreamIDFromAggregator();
			System.out.println("Aggregator: producer with topic name = "+topicName);
			JsonObject jsonStream = _utils.toJsonObject(triples.subList(index, toIndex), // Triples to send
															Integer.toString(_nodeID),   // The ID of the node which this snip was produced.
															Integer.toString(_msgsSentCount), // It is only a counter that says how many messages the sender on the nodeID generated.
															aggregatorName); // The query/aggregator/initialStream that produced this snip.
			
			final ProducerRecord<Long, String> record = new ProducerRecord<Long, String>(topicName, jsonStream.toString());
			
			try 
			{
				RecordMetadata metadata = producer.send(record).get();
				System.out.println("Aggregator: Record sent with key " + _msgsSentCount + " to partition " + metadata.partition()
						+ " with offset " + metadata.offset());
				_msgsSentCount++;
				System.out.println("Aggregator: Enviando stream da query "+query.get_queryID());
				System.out.println("Aggregator: Stream tem "+triples.size()+" triplas");
				System.out.println("Aggregator: Stream: "+jsonStream);
				
			} catch (ExecutionException e) {
				System.out.println("Aggregator: Error in sending record");
				System.out.println(e);
			} catch (InterruptedException e) {
				System.out.println("Aggregator: Error in sending record");
				System.out.println(e);
			}
		}
	}*/
}

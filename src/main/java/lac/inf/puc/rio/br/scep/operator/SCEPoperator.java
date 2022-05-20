/**
 * 
 */
package lac.inf.puc.rio.br.scep.operator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.OffsetCommitCallback;
import org.apache.kafka.common.TopicPartition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.ParseException;

import eu.larkc.csparql.cep.api.RdfQuadruple;
import eu.larkc.csparql.cep.api.RdfStream;
import eu.larkc.csparql.core.engine.CsparqlEngine;
import eu.larkc.csparql.core.engine.CsparqlEngineImpl;
import eu.larkc.csparql.core.engine.CsparqlQueryResultProxy;
import lac.inf.puc.rio.br.scep.aggregator.Aggregator;
import lac.inf.puc.rio.br.scep.communication.ConsumerCreator;
import lac.inf.puc.rio.br.scep.database.IQueryDatabase;
import lac.inf.puc.rio.br.scep.database.QueryDatabase;
import lac.inf.puc.rio.br.scep.model.Query;
import lac.inf.puc.rio.br.scep.model.TriplesBlock;
import lac.inf.puc.rio.br.scep.operator.constants.IOperatorConstants;
import lac.inf.puc.rio.br.scep.operator.observer.SCEPOperatorObserver;
import lac.inf.puc.rio.br.scep.utils.ShutdownableThread;
import lac.inf.puc.rio.br.scep.utils.Utils;
import lac.inf.puc.rio.br.scep.utils.WriterToFile;

/**
 * @author vitor
 *
 */
public class SCEPoperator extends AbstractSCEPoperator 
{
	/**
	 * CSPARQL engine specifics
	 */
	private final CsparqlEngine _engine;	
	private final Map<Integer, CsparqlQueryResultProxy> _queries;
	private Map<Integer, String> _queriesStreamIri;
	
	/**
	 * Logger para esta classe
	 */
	private static Logger _logger = LogManager.getLogger(SCEPoperator.class);

	private String _name;
	
	/**
	 * Cria um SCEP operator.
	 * Faz a inicialização do engine correspondente.
	 * Cria o arquivo para logs do DSCEP
	 * @param nodeID
	 */
	public SCEPoperator(int nodeID, String name)
	{
		super(nodeID);
		_logger.debug("SCEPoperator of node {} is being created...", nodeID);

		_name = name;
		_engine = new CsparqlEngineImpl();		
		_engine.initialize();
		
		_queries = new HashMap<Integer, CsparqlQueryResultProxy>();
		_queriesStreamIri = new HashMap<Integer, String>();
				
		System.out.println("SCEPoperator is initialized.");
	}
	
	/**
	 * Cria um SCEP operator.
	 * Faz a inicialização do engine correspondente.
	 * Cria o arquivo para logs do DSCEP
	 * Já starta a query indicada no engine.
	 * @param nodeID
	 * @param queryID
	 */
	public SCEPoperator(int nodeID, String name, int queryID)
	{
		super(nodeID);
		_logger.debug("SCEPoperator of node {} and query {} is being created...", nodeID, queryID);

		_name = name;
		_engine = new CsparqlEngineImpl();		
		_engine.initialize();
		
		_queries = new HashMap<Integer, CsparqlQueryResultProxy>();		
		_queriesStreamIri = new HashMap<Integer, String>();
				
		System.out.println("SCEPoperator is initialized and query "+queryID+" started.");		
		startQuery(queryID);
	}
	
	
	/**
	 * CSPARQL engine needs to know the stream Iri from the query file.
	 * @param query
	 */
	private void setStreamIriFromQuery(Query query)
	{
		String queryText = query.get_query();
    	int indexBegin = queryText.indexOf("STREAM");
    	
    	if(indexBegin == -1)
    	{
    		_utils.error("A query "+query.get_queryID()+" nao tem a palavra reservada STREAM para identificar o nome  da sua stream.");
    		return;
    	}
    	
    	String subString = queryText.substring(indexBegin+6);
    	
    	int parentesisBegin = subString.indexOf("<");
    	int parentesisEnd = subString.indexOf(">");
    	
    	//String streamIri = subString.substring(parentesisBegin+1, parentesisEnd-1);
		String streamIri = subString.substring(parentesisBegin+1, parentesisEnd);


    	
    	if(streamIri == null)
    	{
    		_utils.error("A query "+query.get_queryID()+" nao tem uma Iri para sua STREAM.");
    		return;
    	}

		System.out.println("VITOR 3: "+streamIri);
    	_queriesStreamIri.put(query.get_queryID(), streamIri);
	}
	
	/**
	 * Assume que a lista de triplas esta ordenada por timestamp.
	 * @param triples
	 */
	public void appendData(Integer queryID, List<RdfQuadruple> triples)
	{
		if(_resetStartTime)
		{
			_startTime = System.currentTimeMillis();
			_resetStartTime = false;
			_numTriplesIn = 0;
		}
		
		//System.out.println("Number of triples to be appended: "+triples.size());
		for(int i=0; i<triples.size();i++)
		{			
			//System.out.println("data "+i+" will be appended.");
			
			String streamIri = _queriesStreamIri.get(queryID);
			_engine.getStreamByIri(streamIri).put(triples.get(i));
			
			//System.out.println("data "+i+" appended.");
		}
		//System.out.println("Done");
	}
	
	/**
	 * CSPARQL needs to register each static RDF file that the query will use.
	 * @param query
	 * @return
	 */
	private boolean addStaticDBsToCS_Engine(Query query) 
	{
		ArrayList<String> staticDBs = query.get_staticDatabases();
		
		if(staticDBs != null)
		{
			for(int i=0;i<staticDBs.size();i++)
			{
				try {
					System.out.println("Adding static model: "+staticDBs.get(i));
					_engine.putStaticNamedModel(staticDBs.get(i), staticDBs.get(i));
					//_engine.putStaticNamedModel(staticDBs.get(i), staticDBs.get(i));
				} catch (Exception e) {
					_utils.error("bad static model: " + staticDBs.get(i));
				}
			}
		}
		
		return true;
	}
		
	
	public boolean startQuery(int queryID) 
	{
		Query query = _queryDb.getQuery(queryID).getClone();

		//System.out.println("Query started:"+query.get_query() );
		
		if(query == null)
		{
			_utils.error("Bad query or queryID do not exists.");
			return false;
		}
			
			
		try 
		{	
			if(!_queries.containsKey(queryID))
			{
				addStaticDBsToCS_Engine(query);
				System.out.println("VITOR 1");
				setStreamIriFromQuery(query);
				System.out.println("VITOR 2");
				initializeOutputFiles(queryID, _name);
				
				RdfStream _stream = new RdfStream(_queriesStreamIri.get(queryID));
				_engine.registerStream(_stream);	
			
				CsparqlQueryResultProxy proxy = _engine.registerQuery(query.get_query().replaceAll("\r", " ").replaceAll("\n", " "), false);		
				
				 if (proxy != null) 
				 {
					 System.out.println("Query "+query.get_queryID()+" is appended. Try to append Observer");
					 proxy.addObserver(new SCEPOperatorObserver(query, _nodeID, this));
					 System.out.println("Observer appended");
					 
					 _queries.put(queryID, proxy);
					 _queriesWindowCount.put(queryID, 0);
				 }
				 else
				 {
					 return false;
				 }				 
			}
			else
				return true; //  Já estava startada.
			        
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			_utils.error("bad query: " + e.getMessage());
			return false;
		}
		
		// Initiate query consumer (Kafka Consumer)
		// Each query must receive all inputstream from a topic, because of this each consumer for each query must
		// have its own group.
		//System.out.println("Starting consumer for query "+query.get_queryID()+" with consumer group name: "+consumerGroupName);
		//System.out.println("Consumer of topic name = "+query.get_inputStreamIDFromAggregator());				
		runConsumer(query);
		//System.out.println("Consumer for query "+query.get_queryID()+" started.");
		
		return true;
	}

	/**
	 * Metodo para remover e parar uma query no engine do CSPARQL
	 * @param queryID
	 * @return
	 */
	public boolean removeAndStopQuery(int queryID) 
	{
		if (_queries.containsKey(queryID)) 
		{
			System.out.println("Stopping query");
			_engine.unregisterQuery(_queries.get(queryID).getId());
			_queries.remove(queryID);
		}
		else 
		{
			System.out.println("Query not found!");
			return false;
		}
		return true;
	}
	
	
	public void runConsumer(Query query)
    {
		String consumerGroupName = "OpConsumer_"+_nodeID+"_"+query.get_queryID();
		String topicName = query.get_inputStreamIDFromAggregator();
		_logger.debug("Consumer(listening) on topic = {} - ConsumerGroup = {}", topicName, consumerGroupName );
		
		List<String> topics = new ArrayList<String>();
		topics.add(topicName);
		KafkaConsumer<Integer, String> consumer = ConsumerCreator.createConsumer(topics, 
				IOperatorConstants.KAFKA_BROKERS, 
				consumerGroupName, 
				IOperatorConstants.MAX_POLL_RECORDS, 
				IOperatorConstants.OFFSET_RESET_EARLIER);

		OperatorConsumer aggConsumer = new OperatorConsumer(consumer, consumerGroupName, query);
    	aggConsumer.start();	
    }
	
	private class OperatorConsumer extends ShutdownableThread 
	{
		private final KafkaConsumer<Integer, String> _consumer;
		private final Query _query;
		private final String _consumerName;
		
	    public OperatorConsumer(KafkaConsumer<Integer, String> consumer, String consumerName, Query query)
	    {
	    	super(consumerName, false);
	    	
	    	_consumer = consumer;
	    	_query = query;
	    	_consumerName = consumerName;
	    }

		@Override
		public void doWork() {
			// TODO Auto-generated method stub
			boolean isTwitterDB = false;
			while (true) {
				final ConsumerRecords<Integer, String> consumerRecords = _consumer.poll(100000);				
				
				for (ConsumerRecord<Integer, String> record : consumerRecords) {
					//System.out.println("Record Key " + record.key());
					//System.out.println("Consumer: Record value " + record.value());
					_logger.debug("Consumer received: Record value " + record.value());
					//System.out.println("Record partition " + record.partition());
					//System.out.println("Record offset " + record.offset());
					
					// Todas as queries que tiverem query.get_inputStreamIDFromAggregator() == topicName
					// essas sao as queries que precisam receber o dado.
					_logger.debug("Consumer (received) for query = {} - ConsumerGroup = {}", _query.get_queryID().toString(), _consumerName );
					
					if(_utils.getAttributeFromMsg(record.value(), "isTwitterDB") != null && 
							_utils.getAttributeFromMsg(record.value(), "isTwitterDB").equals("true"))
						isTwitterDB = true;
					else
						isTwitterDB = false;
					
					ArrayList<RdfQuadruple> triples = null;
					if(isTwitterDB)
					{
						ArrayList<TriplesBlock> triplesBlock = _utils.getTriplesBlockFromJSONSnip(record.value());						
						
						//System.out.println("Number of blocks: "+triplesBlock.size());
						
						triples = new ArrayList<RdfQuadruple>();
						for(int i=0; i<triplesBlock.size(); i++)
						{
							//System.out.println(triplesBlock.get(i).getTriples());							
							_numTriplesIn = _numTriplesIn + triplesBlock.get(i).size();		
							//System.out.println("NumTriples = "+triplesBlock.get(i).getTriples().size());
							//System.out.println("NumTriples = "+triplesBlock.get(i).size());
							
							triples.addAll(triplesBlock.get(i).getTriples());
							
							//System.out.println("NumTriplesToT = "+triples.size());
						}
					}
					else
					{		
						triples = new ArrayList<RdfQuadruple>();
						triples = _utils.getTriplesFromJSONSnip(record.value());
						_numTriplesIn = _numTriplesIn + triples.size();
					}					
					
					System.out.println("Number of triples received from agg: "+triples.size());
					appendData(_query.get_queryID(), triples);
		        }
				
				// We send the commit and carry on, but if the commit fails, the failure and the offsets will be logged.
				_consumer.commitAsync(new OffsetCommitCallback() {
			        public void onComplete(Map<TopicPartition,
			        OffsetAndMetadata> offsets, Exception e) {
			            if (e != null)
			                _utils.error("Commit failed for offsets: "+ offsets+". Exeption = "+ e);
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
}

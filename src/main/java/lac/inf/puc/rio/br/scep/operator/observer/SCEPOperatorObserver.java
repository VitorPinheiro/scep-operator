/**
 * 
 */
package lac.inf.puc.rio.br.scep.operator.observer;

import java.io.BufferedReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutionException;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.impl.LiteralLabelFactory;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.BindingFactory;
import com.hp.hpl.jena.sparql.engine.binding.BindingMap;

import eu.larkc.csparql.cep.api.RdfQuadruple;
import eu.larkc.csparql.common.RDFTable;
import lac.inf.puc.rio.br.scep.aggregator.constants.IAggregatorConstants;
import lac.inf.puc.rio.br.scep.communication.ProducerCreator;
import lac.inf.puc.rio.br.scep.model.Query;
import lac.inf.puc.rio.br.scep.model.StreamSnipBlocks;
import lac.inf.puc.rio.br.scep.model.TriplesBlock;
import lac.inf.puc.rio.br.scep.operator.SCEPoperator;
import lac.inf.puc.rio.br.scep.operator.constants.IOperatorConstants;
import lac.inf.puc.rio.br.scep.utils.Utils;
import lac.inf.puc.rio.br.scep.utils.IKafkaConstants;

/**
 * @author vitor
 *
 */
public class SCEPOperatorObserver implements Observer 
{
	/**
	 * Varaibles
	 */
	private Query _query;	
	private int _nodeID;
	private int _msgsSent;
	private int _executionNum;
	private final String _producerKafkaID;
	private ArrayList<String> _plainLiteralProperties; // http://rdfs.org/sioc/ns#id / http://www.ics.forth.gr/isl/oae/core#detectedAs / http://www.w3.org/2000/01/rdf-schema#label / http://rdfs.org/sioc/ns#name
	private ArrayList<String> _dateTimeProperties; // http://purl.org/dc/terms/created
	private ArrayList<String> _doubleProperties; // http://www.gsi.dit.upm.es/ontologies/onyx/ns#hasEmotionIntensity / http://www.ics.forth.gr/isl/oae/core#confidence
	private ArrayList<String> _integerProperties; // http://schema.org/userInteractionCount
	private ArrayList<String> _urlValues; // http://www.gsi.dit.upm.es/ontologies/onyx/ns#hasEmotion / sioc:has_creator / onyx:hasEmotionSet / schema:interactionStatistic
	
	/**
	 * Classes
	 */
	private Utils _utils;
	private SCEPoperator _operator;
	
	/**
	 * Constants
	 */
	private static final String QUOTE = "\"";
	private static final String GT = ">";
	private static final String LT = "<";	
	
	/**
	 * Logger
	 */
	private static Logger _logger = LogManager.getLogger(SCEPOperatorObserver.class);
	
	/**
	 * Constructor
	 * @param query : The query which this observer is related.
	 * @param nodeID : The ID of the node of which this query is running.
	 */
	public SCEPOperatorObserver(Query query, int nodeID, SCEPoperator operator)
	{
		_logger.debug("SCEPOperatorObserver of node {} and query {} is being created...", nodeID, query.get_queryID());		
		
		_msgsSent = 1;
		_executionNum = 1;
		_query = query;
		_nodeID = nodeID;
		_producerKafkaID = "OP_"+nodeID+"_"+query.get_queryID();
		initializeProperties();
		
		_operator = operator;
		
		System.out.println("SCEPOperatorObserver of node "+nodeID+" and query "+query.get_queryID()+" is created");
	}
	
	/**
	 * _plainLiteralProperties
	 * _dateTimeProperties
	 * _doubleProperties
	 * _integerProperties
	 * 
	 */
	private void initializeProperties()
	{ 
		_urlValues = new ArrayList<String>();
		_urlValues.add("http://www.ics.forth.gr/isl/oae/core#hasMatchedURI");
		
		_plainLiteralProperties = new ArrayList<String>();
		_plainLiteralProperties.add("http://rdfs.org/sioc/ns#name");
		_plainLiteralProperties.add("http://rdfs.org/sioc/ns#id");
		_plainLiteralProperties.add("http://www.ics.forth.gr/isl/oae/core#detectedAs");
		_plainLiteralProperties.add("http://www.w3.org/2000/01/rdf-schema#label");
		_plainLiteralProperties.add("http://example.org/hasName");
		
		_dateTimeProperties = new ArrayList<String>();
		_dateTimeProperties.add("http://purl.org/dc/terms/created");
		
		_doubleProperties = new ArrayList<String>();
		_doubleProperties.add("http://www.gsi.dit.upm.es/ontologies/onyx/ns#hasEmotionIntensity");
		_doubleProperties.add("http://www.ics.forth.gr/isl/oae/core#confidence");
		
		_integerProperties = new ArrayList<String>();
		_integerProperties.add("http://schema.org/userInteractionCount");
	}
	
	
	
	@Override
	/**
	 * A resposta que sai do CSPARQL operator é uma lista de triplas.
	 * Primeiro é preciso converter essa lista 
	 */
    public void update(Observable o, Object arg) 
    {				
	  System.out.println("Update! Query "+_query.get_queryID()+" produziu uma resposta.");
      final RDFTable rdfTable = (RDFTable) arg;		      
      
      final List<Binding> bindings = new ArrayList<>();
      final String[] vars = rdfTable.getNames().toArray(new String[]{});
      
      rdfTable.stream().forEach((t) -> {
          bindings.add(toBinding(vars, t.toString(), "\t"));
        });
      
      //System.out.println("----------------Query "+_query.get_queryID()+" msgs sent = "+_msgsSent);
      
      int numTriplesOut = 0;
      if(_query.getProducesTriplesInBlock())
      {
    	  Model rdfAnswer = generateModel(vars, bindings);    	
    	  
    	  //System.out.println(rdfAnswer);
    	  
    	  
    	  StreamSnipBlocks blockTriples = convertAnswerToTripleBlocks(rdfAnswer, _query);
    	  
    	  /*
    	  if(_query.get_queryID() == 1)
    		  blockTriples = convertAnswerToTripleBlocks(rdfAnswer, _query);
    		  //blockTriples = convertAnswerToTripleBlocks_type(rdfAnswer, _query, "dbo:MusicalArtist", "ex:isNotMusicalArtists");
    	  else if (_query.get_queryID() == 4)
    		  blockTriples = convertAnswerToTripleBlocks_type(rdfAnswer, _query, "dbo:TelevisionShow", "ex:isNotTelevisionShow");
    	  */
    	  
    	  //System.out.println("Answer for query "+_query.get_queryID()+": \n"+ blockTriples);
    	  _logger.debug("Answer for query"+_query.get_queryID()+": "+blockTriples);
    	  
    	  // Send with kafka
    	  if(blockTriples!= null)
    	  {
    		  numTriplesOut = blockTriples.getTotalNumberOfTriples();
    		  runProducer(blockTriples);    		  
    	  }
      }
      else
      {
    	  ArrayList<RdfQuadruple> triples = generateStream(vars, bindings);
          //System.out.println("Answer for query "+_query.get_queryID()+": \n"+ triples);	
          
          _logger.debug("Answer for query"+_query.get_queryID()+": "+triples);
          
          // Send with kafka
          numTriplesOut = triples.size();
       	  runProducer(triples);  
      }
      
      Long procTime = System.currentTimeMillis() - _operator.getStartTime();
	  _operator.writeToFile(procTime.toString(), _executionNum, numTriplesOut);
	  _operator.resetStartTime();
	  _executionNum++;
	  //System.out.println("procTime = "+procTime);
      				          
      
    }
	
	/**
	 * Está função converte uma lista de triplas retornado pelo operador CSPARQL em uma lista de RDF graphs.
	 * 
	 * @param answerTriples: O modelo jena que representa a lista de triplas retornada pela query.
	 * @param query: A querry que produziu as triplas
	 * @return Retorna: A lista de RDF graphs identificados pela propriedade sioc:id
	 */
	private StreamSnipBlocks convertAnswerToTripleBlocks(Model answerTriples, Query query)
	{
		String queryPostIds = "select distinct ?postID WHERE { ?tweet sioc:id ?postID  } ";

		ResultSet rs1 = QueryExecutionFactory.create( query.getQueryPREFIX()+queryPostIds, answerTriples ).execSelect();
		
		///////////////////////////////////////////////////////////////////////////////////////////
		// STEP 2: Para cada um dos posts eu pego todas as triplas dele crio os RDFQuadruple, 
		// usando o System.currentTimeMillis() como timestamp.
		///////////////////////////////////////////////////////////////////////////////////////////
		StreamSnipBlocks allTriples = new StreamSnipBlocks();	
		
		while(rs1.hasNext())
		{		
			QuerySolution postResult = rs1.next();
			String postID = postResult.get("postID").asLiteral().toString();
			
			// Replace postID number with the postID number to extract.
			String constructContentTemplate = query.getConstructContent();				    	
	    	String temp = constructContentTemplate.replaceFirst("(sioc:id)(\\s*)((\\S+)[^.])", "sioc:id \""+postID+"\" .");
	    	String constructContent = temp.replaceFirst("[.][.]", ".");
			
	    	// TODO: Precisa colocar um OPTIONAL clause por tripla! Não é o bloco que é opcional, é cada tripla.
	    	// Vai ficar lento a query, mas é o jeito.
	    	String whereContent = query.getWhereBlock(constructContent);
			
			String queryToGetRdfGraphs = "construct { "+constructContent+" }  where { "+whereContent+" }";
			
			//System.out.println("------------------");
			//System.out.println(query.getQueryPREFIX()+queryToGetRdfGraphs);
			//System.out.println("------------------");
			
			Model model2 = QueryExecutionFactory.create( query.getQueryPREFIX()+queryToGetRdfGraphs, answerTriples).execConstruct();
			
			TriplesBlock postTriplesBlock = new TriplesBlock();			
			StmtIterator it = model2.listStatements();
			
			//System.out.println("postID = "+postID);
			//System.out.println("it.hasNext() = "+it.hasNext());
			while(it.hasNext())
			{						
				Statement stmt = it.nextStatement();
				
				Triple t = stmt.asTriple();
			    
				RdfQuadruple postTriple = new RdfQuadruple(t.getSubject().toString(), 
														t.getPredicate().toString(), 
														t.getObject().toString(), 
														System.currentTimeMillis());
				postTriplesBlock.addTriple(postTriple);
			}
			
			allTriples.addBlock(postTriplesBlock);
			postTriplesBlock.clear();
		}
		
		return allTriples;
	}
	
	/**
	 * Criada para pegar a resposta da query7 (getMusicalArtists) e agrupar ela em blocos de triplas.
	 * @param model
	 * @param query
	 * @return
	 */
	private StreamSnipBlocks convertAnswerToTripleBlocks_type(Model model, Query query, String searchType, String isNotSearchType)
	{
		// Write a model in Turtle syntax, default style (pretty printed)
	    //RDFDataMgr.write(System.out, model, Lang.N3) ;
		///////////////////////////////////////////////////////////////////////////////////////////
		// STEP 1: Defino quais X posts eu vou enviar. (OK) : Faço isso para ordenar os posts mais 
		// antigos aos mais novos.
		///////////////////////////////////////////////////////////////////////////////////////////
		String prefixs = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
				+ "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
				+ "prefix owl: <https://www.w3.org/2002/07/owl#> "
				+ "prefix xsd: <http://www.w3.org/2001/XMLSchema#> "
				+ "prefix nee: <http://www.ics.forth.gr/isl/oae/core#> "
				+ "prefix ex: <http://example.org/> "
				+ "prefix schema: <http://schema.org/> "
				+ "prefix dc: <http://purl.org/dc/terms/> "
				+ "prefix dbo: <http://dbpedia.org/ontology/> "
				+ "prefix dbp: <http://dbpedia.org/property/> "
				+ "prefix dbr: <http://dbpedia.org/resource/>"
				+ "prefix sioc: <http://rdfs.org/sioc/ns#> "
				+ "prefix sioc_t: <http://rdfs.org/sioc/types#> "
				+ "prefix onyx: <http://www.gsi.dit.upm.es/ontologies/onyx/ns#> "
				+ "prefix wna: <http://www.gsi.dit.upm.es/ontologies/wnaffect/ns#> \n";
		
		String queryPostIds = "select distinct ?postID WHERE { ?tweet rdf:type sioc:Post ."
															+ "?tweet sioc:id ?postID  } ";
				
		ResultSet rs1 = QueryExecutionFactory.create( prefixs+queryPostIds, model ).execSelect();
		
		///////////////////////////////////////////////////////////////////////////////////////////
		// STEP 2: Para cada um dos posts eu pego todas as triplas dele crio os RDFQuadruple, 
		// usando o System.currentTimeMillis() como timestamp.
		///////////////////////////////////////////////////////////////////////////////////////////
		StreamSnipBlocks allTriples = new StreamSnipBlocks();	
		
		while(rs1.hasNext())
		{		
			QuerySolution postResult = rs1.next();
			String postID = postResult.get("postID").asLiteral().toString();

			//System.out.println("=== Results for postID = "+postID+" ===");			
			
			String queryMusicalArtists = "construct { ?tweet rdf:type sioc:Post ."
										+ "?tweet sioc:id \""+postID+"\" . "
										+ "?tweet dc:created ?datetime ."
										+ "?tweet sioc:has_creator ?postCreator ."
										+ "?tweet onyx:hasEmotionSet ?emotionSet ."
										+ "?tweet schema:interactionStatistic ?interactionSet ."
										+ "?tweet schema:interactionStatistic ?interactionSet2 ."	
										+ "?interactionSet rdf:type schema:InteractionCounter ."
										+ "?interactionSet schema:interactionType schema:LikeAction ."
										+ "?interactionSet schema:userInteractionCount ?likeCount ."
										+ "?interactionSet2 rdf:type schema:InteractionCounter ."
										+ "?interactionSet2 schema:interactionType schema:ShareAction ."
										+ "?interactionSet2 schema:userInteractionCount ?shareCount ."	
										+ "?tweet schema:mentions ?entity ."
										+ "?entity nee:hasMatchedURI ?artistURI ."	
										+ "?artistURI dbo:genre ?p ."
										+ "?artistURI rdf:type "+searchType+" ."
										+ "?entity nee:detectedAs ?name ."	
										+ "?tweet schema:mentions ?entity2 ."
										+ "?entity2 nee:hasMatchedURI ?uri ."
										+ "?entity2 ex:hasName ?nameEntity ."
										+ "?uri ex:type "+isNotSearchType+" ."											
										+ "?emotionSet rdf:type onyx:EmotionSet ."
										+ "?emotionSet onyx:hasEmotion ?negative ."
										+ "?negative onyx:hasEmotionCategory wna:negative-emotion . "
										+ "?negative onyx:hasEmotionIntensity ?negNum ."										
										+ "?emotionSet onyx:hasEmotion ?positive ."
										+ "?positive onyx:hasEmotionCategory wna:positive-emotion ."
										+ "?positive onyx:hasEmotionIntensity ?posNum ."
										+ "?tweet schema:mentions ?TagClass ."
										+ "?TagClass rdf:type sioc_t:Tag ."
										+ "?TagClass rdfs:label ?tag ."
										+ "?tweet schema:mentions ?UserAcc ."
										+ "?UserAcc rdf:type sioc:UserAccount ."
										+ "?UserAcc sioc:name ?userName .}"										
						+ "where { ?tweet rdf:type sioc:Post ."
						+ "?tweet sioc:id \""+postID+"\" ."
						+ "?tweet dc:created ?datetime ."						
						+ "?tweet sioc:has_creator ?postCreator ."
						+ "?tweet onyx:hasEmotionSet ?emotionSet ."
						+ "?tweet schema:interactionStatistic ?interactionSet ."
						+ "?tweet schema:interactionStatistic ?interactionSet2 ."		
						+ "?interactionSet rdf:type schema:InteractionCounter ."
						+ "?interactionSet schema:interactionType schema:LikeAction ."
						+ "?interactionSet schema:userInteractionCount ?likeCount ."
						+ "?interactionSet2 rdf:type schema:InteractionCounter ."
						+ "?interactionSet2 schema:interactionType schema:ShareAction ."
						+ "?interactionSet2 schema:userInteractionCount ?shareCount ."						
						+ "?emotionSet rdf:type onyx:EmotionSet ."
						+ "?emotionSet onyx:hasEmotion ?positive ."
						+ "?positive onyx:hasEmotionCategory wna:positive-emotion ."
						+ "?positive onyx:hasEmotionIntensity ?posNum ."
						+ "?emotionSet onyx:hasEmotion ?negative ."
						+ "?negative onyx:hasEmotionCategory wna:negative-emotion . "
						+ "?negative onyx:hasEmotionIntensity ?negNum ."			
						+ "?tweet schema:mentions ?entity ."
						+ "?entity nee:hasMatchedURI ?artistURI ."		
						+ "?artistURI dbo:genre ?p ."
						+ "?artistURI rdf:type "+searchType+" ."
						+ "?entity ex:hasName ?name ."		
						+ "?tweet schema:mentions ?entity2 ."
						+ "?entity2 nee:hasMatchedURI ?uri ."
						+ "?entity2 ex:hasName ?nameEntity ."
						+ "?uri ex:type "+isNotSearchType+" ."				
						+ "OPTIONAL { ?tweet schema:mentions ?TagClass }"
						+ "OPTIONAL { ?TagClass rdf:type sioc_t:Tag }"
						+ "OPTIONAL { ?TagClass rdfs:label ?tag }"
						+ "OPTIONAL { ?tweet schema:mentions ?UserAcc }"
						+ "OPTIONAL { ?UserAcc rdf:type sioc:UserAccount }"
						+ "OPTIONAL { ?UserAcc sioc:name ?userName }"						
						+ " } ";
			
			Model model2 = QueryExecutionFactory.create( prefixs+queryMusicalArtists, model ).execConstruct();						 
			
			TriplesBlock postTriplesBlock = new TriplesBlock();
			
			StmtIterator it = model2.listStatements();
			//System.out.println("model2 has "+model2.listStatements().toList().size()+ "of triples.");
			
			//RDFDataMgr.write(System.out, model2, Lang.N3) ;
			
			while(it.hasNext())
			{
				Statement stmt = it.nextStatement();
				
				Triple t = stmt.asTriple();
			    
				RdfQuadruple postTriple = new RdfQuadruple(t.getSubject().toString(), 
														t.getPredicate().toString(), 
														t.getObject().toString(), 
														System.currentTimeMillis());
				
				// Preciso garantir que eu envie até 248 triplas e que nenhum tweet seja dividido.
				// Esse triples deve ter no maximo 248, na hora de criar ele eu preciso monitorar isso, cada vez que der 248 eu envio pelo runProducer.
				postTriplesBlock.addTriple(postTriple);
			}
			
			allTriples.addBlock(postTriplesBlock);
			postTriplesBlock.clear();
		}

		return allTriples;
	}
	
	
	private void runProducer(StreamSnipBlocks triples) 
	{
		//System.out.println("AggregatorBlocks: runProducer");
		
		if(triples == null || triples.getNumberOfBlocks() == 0)
		{
			//System.out.println("Query "+_query.get_queryID()+" produziu um conjunto vazio.");
			return;
		}
		
		Producer<Long, String> producer = ProducerCreator.createProducer(IAggregatorConstants.KAFKA_BROKERS, _producerKafkaID);	
		
		String topicName = _query.get_outputStreamID();
		_logger.debug("AggregatorBlocksName {} - Publishing on topic {} - Number of blocks = {} - Number of triples = {}", _producerKafkaID, topicName, triples.getNumberOfBlocks(), triples.getTotalNumberOfTriples());
		
		JsonObject jsonStream = _utils.streamBlocksToJsonObject(triples, 
													Integer.toString(_nodeID),   // The ID of the node which this snip was produced.
													Integer.toString(_msgsSent), // It is only a counter that says how many messages the sender on the nodeID generated.
													Integer.toString(_query.get_queryID()), // The query/aggregator/initialStream that produced this snip.		
													true );	
		
		final ProducerRecord<Long, String> record = new ProducerRecord<Long, String>(topicName, jsonStream.toString());		
		
		try 
		{
			RecordMetadata metadata = producer.send(record).get();
			//System.out.println("Aggregator: Record sent with key " + _msgsSent + " to partition " + metadata.partition()
			//		+ " with offset " + metadata.offset());
			_msgsSent++;
			//System.out.println("Aggregator: Enviando stream para query "+_query.get_queryID());
			//System.out.println("Aggregator: Stream tem "+triples.getTotalNumberOfTriples()+" triplas");
			//System.out.println("Stream: "+jsonStream);
			
		} catch (ExecutionException e) {
			System.out.println("Aggregator: Error in sending record");
			System.out.println(e);
		} catch (InterruptedException e) {
			System.out.println("Aggregator: Error in sending record");
			System.out.println(e);
		}
		
		_operator.writeJsonOutToFile(jsonStream.toString(), _executionNum);
	}
	
	
	private void runProducer(ArrayList<RdfQuadruple> triples) 
	{
		if(triples == null || triples.size() == 0)
		{
			//System.out.println("Query "+_query.get_queryID()+" produziu um conjunto vazio.");
			return;
		}
		  
		Producer<Long, String> producer = ProducerCreator.createProducer(IOperatorConstants.KAFKA_BROKERS, _producerKafkaID);
					
		JsonObject jsonStream = _utils.toJsonObject(triples, 
														Integer.toString(_nodeID), // The ID of the node which this snip was produced.
														Integer.toString(_msgsSent), // It is only a counter that says how many messages the sender on the nodeID generated.
														Integer.toString(_query.get_queryID()),
														false); // The query/aggregator/initialStream that produced this snip.
		
		String topic_name = _query.get_outputStreamID();
		_logger.debug("Publishing result of query {} on topic {} - ProducerClientID = {}", _query.get_queryID(), topic_name, _producerKafkaID );
		
		final ProducerRecord<Long, String> record = new ProducerRecord<Long, String>(topic_name, jsonStream.toString());
		
		try 
		{
			RecordMetadata metadata = producer.send(record).get();
			//System.out.println("Record sent with key " + _msgsSent + " to partition " + metadata.partition()
			//		+ " with offset " + metadata.offset());
			_msgsSent++;
			//System.out.println("Enviando stream da query "+_query.get_queryID());
			//System.out.println("Stream tem "+triples.size()+" triplas");
			//System.out.println("Stream: "+jsonStream);
			
		} catch (ExecutionException e) {
			System.out.println("Error in sending record");
			System.out.println(e);
		} catch (InterruptedException e) {
			System.out.println("Error in sending record");
			System.out.println(e);
		}
		
		_operator.writeJsonOutToFile(jsonStream.toString(), _executionNum);
	}
	
	/**
	 * Create a Jena model from a result of a CSPARQL query.
	 * @param vars
	 * @param bindings
	 * @return
	 */
	private Model generateModel(String[] vars, List<Binding> bindings)
	{
		Model model = ModelFactory.createDefaultModel() ;
		
		for (Binding binding : bindings) 
		{						
			if(vars.length != 3)
			{
				_utils.error("Bad answer to generate triple stream. Must have size 3. Vars: "+vars.toString());
				return null;
			}		  
			
			Resource subject = ResourceFactory.createResource(binding.get(Var.alloc(vars[0])).toString());
			Property predicate = ResourceFactory.createProperty(binding.get(Var.alloc(vars[1])).toString());
			
			Statement stmt;
			
			String prop = binding.get(Var.alloc(vars[1])).toString();
			String obj ; 
			if(_plainLiteralProperties.contains(prop))
			{		
				obj = binding.get(Var.alloc(vars[2])).getLiteral().toString();
				obj = obj.substring(2, obj.length()-2);
				Literal literal = ResourceFactory.createPlainLiteral(obj);
				stmt = ResourceFactory.createStatement(subject, predicate, literal);				
			}		
			else if(_dateTimeProperties.contains(prop))
			{
				obj = binding.get(Var.alloc(vars[2])).toString();
				String finalDate = obj.replaceAll(binding.get(Var.alloc(vars[2])).getLiteralDatatypeURI(), "");
				
				finalDate = finalDate.substring(3, finalDate.length()-5);
				
				stmt = ResourceFactory.createStatement(subject, predicate, ResourceFactory.createTypedLiteral(finalDate, XSDDatatype.XSDdate));	
			}
			else if(_doubleProperties.contains(prop))
			{
				obj = binding.get(Var.alloc(vars[2])).toString();
				String finalDouble = obj.replaceAll(binding.get(Var.alloc(vars[2])).getLiteralDatatypeURI(), "");
				
				finalDouble = finalDouble.substring(3, finalDouble.length()-5);
				
				double finalDoubleNum = Double.parseDouble(finalDouble);

				stmt = ResourceFactory.createStatement(subject, predicate, ResourceFactory.createTypedLiteral(finalDoubleNum));				
			}
			else if(_integerProperties.contains(prop))
			{
				obj = binding.get(Var.alloc(vars[2])).toString();
				String finalInt = obj.replaceAll(binding.get(Var.alloc(vars[2])).getLiteralDatatypeURI(), "");
				
				finalInt = finalInt.substring(3, finalInt.length()-5);				
				
				int finalIntNum = Integer.parseInt(finalInt);
				
				stmt = ResourceFactory.createStatement(subject, predicate, ResourceFactory.createTypedLiteral(finalIntNum));
			}
			/*else if (_urlValues.contains(prop))
			{
				obj = binding.get(Var.alloc(vars[2])).toString();
				
				String result = obj;
				try {
					result = URLDecoder.decode(obj, StandardCharsets.UTF_8.toString());
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				Resource object = ResourceFactory.createResource(result);	
				stmt = ResourceFactory.createStatement(subject, predicate, object);				
			}*/
			else
			{
				obj = binding.get(Var.alloc(vars[2])).toString();
				Resource object = ResourceFactory.createResource(binding.get(Var.alloc(vars[2])).toString());
				stmt = ResourceFactory.createStatement(subject, predicate, object);
			}
			
			
			
			model.add(stmt);
		}
		
		return model;
	}
	
	/**
	 * Generate a stream, and put a timestamp in every produced triple.
	 * @param vars
	 * @param bindings
	 * @return
	 */
	private ArrayList<RdfQuadruple> generateStream(String[] vars, List<Binding> bindings)
	{
		ArrayList<RdfQuadruple> triples = new ArrayList<RdfQuadruple>();
					
		for (Binding binding : bindings) 
		{			
			if(vars.length != 3)
			{
				_utils.error("Bad answer to generate triple stream. Must have size 3. Vars: "+vars.toString());
				return null;
			}		  
			
			RdfQuadruple triple = new RdfQuadruple(binding.get(Var.alloc(vars[0])).toString(), 
					  								binding.get(Var.alloc(vars[1])).toString(), 
					  								binding.get(Var.alloc(vars[2])).toString(), 
					  								System.currentTimeMillis());		        
			triples.add(triple);
		}
		
		return triples;
	}
	
	private static Binding toBinding(String[] vars, String string, String separator) 
	{
		//System.out.println("string = "+string);
		String[] values = string.split(separator);
		final BindingMap binding = BindingFactory.create();
		for (int i = 0; i < vars.length; i++) 
		{
			binding.add(Var.alloc(vars[i]), toNode(values[i]));
		}
		return binding;
	}
	  
	private static Node toNode(String value) 
	{
		if (value.startsWith("http://") || value.startsWith("https://")) 
		{
			return NodeFactory.createURI(value);
		} 
		else if (value.contains("^^")) 
		{
			String[] parts = value.split("\\^\\^");
			RDFDatatype dtype = NodeFactory.getType(toUri(parts[1]));
			return NodeFactory.createLiteral(unquoting(parts[0]), dtype);
		} 		
		else 
		{
			return NodeFactory.createLiteral(value);
		}
	}
	  
	private static String unquoting(final String string) 
	{
		final StringBuilder builder = new StringBuilder(string);
		if (builder.indexOf(QUOTE) == 0) 
		{
			builder.deleteCharAt(0);
		}
		if (builder.lastIndexOf(QUOTE) == builder.length() - 1) 
		{
			builder.deleteCharAt(builder.length() - 1);
		}
		return builder.toString();
	}

	private static String toUri(final String string) 
	{
		final StringBuilder builder = new StringBuilder(string);
		if (builder.indexOf(LT) == 0) 
		{
			builder.deleteCharAt(0);
		}
		if (builder.lastIndexOf(GT) == builder.length() - 1) 
		{
			builder.deleteCharAt(builder.length() - 1);
		}
		return builder.toString();
	}

}

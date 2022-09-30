package lac.inf.puc.rio.br.scep.operator;

import com.google.gson.JsonObject;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.*;
import eu.larkc.csparql.cep.api.RdfQuadruple;
import lac.inf.puc.rio.br.scep.aggregator.constants.IAggregatorConstants;
import lac.inf.puc.rio.br.scep.communication.ConsumerCreator;
import lac.inf.puc.rio.br.scep.communication.ProducerCreator;
import lac.inf.puc.rio.br.scep.model.Query;
import lac.inf.puc.rio.br.scep.model.StreamSnipBlocks;
import lac.inf.puc.rio.br.scep.model.TriplesBlock;
import lac.inf.puc.rio.br.scep.operator.constants.IOperatorConstants;
import lac.inf.puc.rio.br.scep.utils.ShutdownableThread;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class BasicOperator extends AbstractSCEPoperator
{
    private final Map<Integer, String> _queries;
    private Boolean _isCONSTRUCT;
    private Query _query;
    private Model _model;

    private String _name;

    private int _numTriplesOut;
    private int _msgsSent;
    private int _executionNum;
    private String _producerKafkaID;

    /**
     * Logger para esta classe
     */
    private static Logger _logger = LogManager.getLogger(BasicOperator.class);

    /**
     * Cria um SCEP operator.
     * Faz a inicialização do engine correspondente.
     * Cria o arquivo para logs do DSCEP
     * @param nodeID
     */
    public BasicOperator(int nodeID, String name)
    {
        super(nodeID);
        _logger.debug("BasicOperator of node {} is being created...", nodeID);

        _name = name;
        _queries = new HashMap<Integer, String>();
        _isCONSTRUCT = false;
        //_query = new Query();// null;
        _model = null;
        _numTriplesOut = 0;

        _nodeID = nodeID;
        _producerKafkaID = "OP_"+nodeID+"_"+_query.get_queryID();
        _msgsSent = 1;
        _executionNum = 1;

        System.out.println("BasicOperator is initialized.");
    }

    public BasicOperator(int nodeID, String name, int queryID)
    {
        super(nodeID);
        _logger.debug("BasicOperator of node {} is being created...", nodeID);

        _name = name;
        _queries = new HashMap<Integer, String>();
        _isCONSTRUCT = false;
        _numTriplesOut = 0;

        _nodeID = nodeID;
        _msgsSent = 1;
        _executionNum = 1;

        System.out.println("BasicOperator"+_name+" is initialized.");

        prepareQuery(queryID);
    }

    public void appendData(Integer queryID, ArrayList<TriplesBlock> triplesBlock)
    {
        System.out.println("BasicOperator"+_name+": appendData triplesBlock initialized");
        if(_resetStartTime)
        {
            _startTime = System.currentTimeMillis();
            _resetStartTime = false;
            _numTriplesIn = 0;
        }

        /**
         * Adiciona a janela do Model, para realizar a query.
         */
        ArrayList<Statement> windowStatments = new ArrayList<Statement>();
        for(int i=0; i<triplesBlock.size(); i++) {
            for (int k = 0; k < triplesBlock.get(i).getTriples().size(); k++) {
                Resource sub = ResourceFactory.createResource(triplesBlock.get(i).getTriples().get(k).getSubject());
                Property pred = ResourceFactory.createProperty(triplesBlock.get(i).getTriples().get(k).getPredicate());
                // Tem o problema do post, que tem o \\ no inicio e no fim do ID do post, tem que tirar pq ta errado e a sparql nao pega.

                Statement st;
                Resource obj;
                Literal lit;
                if(pred.toString().equalsIgnoreCase("http://rdfs.org/sioc/ns#id"))
                {
                //    Literal lit = ResourceFactory.createPlainLiteral(triplesBlock.get(i).getTriples().get(k).getObject());
                //    st = _model.createStatement(sub,pred,lit);
                    lit = ResourceFactory.createPlainLiteral(triplesBlock.get(i).getTriples().get(k).getObject().replace("\\", ""));
                    st = _model.createStatement(sub,pred,lit);
                    //System.out.println("POST_ID = "+lit);
                }
                else
                {
                    obj = ResourceFactory.createResource(triplesBlock.get(i).getTriples().get(k).getObject());
                    st = _model.createStatement(sub,pred,obj);
                }

                _model.add(st);
                windowStatments.add(st);
            }
        }

        System.out.println("BasicOperator"+_name+": model object (KB + Window): created");
        /**
         * Degug: Testar se adicionou tudo da janela na KB
         */
        /*try{
            File outputFile = new File("operator"+_name+"_modelTestAll"+_msgsSent+".ttl");
            OutputStream outStream = new FileOutputStream(outputFile);
            Writer writer = new OutputStreamWriter(outStream, StandardCharsets.UTF_8);
            _model.write(writer, "TTL");
        }
        catch (Exception e)
        {
            System.out.println("BasicOperator-"+_name+" Deu algo errado ai: "+e);
        }

        System.out.println("BasicOperator-"+_name+": model object (KB + Window: persisted on file)");*/

        /**
         * Realiza a query no Model que contém o static database (KB) + a janela.
         */
        if(_isCONSTRUCT) {
            // https://stackify.com/heres-how-to-calculate-elapsed-time-in-java/
            Instant start = Instant.now();
            Model model2 = QueryExecutionFactory.create( _query.get_query(), _model ).execConstruct();
            Instant end = Instant.now();
            Duration timeElapsed = Duration.between(start, end);
            System.out.println("Query executed by basic operator "+_name+" in "+timeElapsed.getSeconds()+ " secs.");
            _logger.debug("Query executed by basic operator "+_name+" in "+timeElapsed.getSeconds()+ " secs.");

            // DEBUG: Só para conferir a resposta do processamento de cada janela em um arquivo
            /*System.out.println("Writing results to file...");
            try {
                File outputFile = new File("queryAnswer.ttl");
                OutputStream outStream = new FileOutputStream(outputFile);
                Writer writer = new OutputStreamWriter(outStream, StandardCharsets.UTF_8);
                model2.write(writer, "TTL");
            }
            catch (Exception e)
            {
                System.out.println("Deu erro ai na resposta da query (construct): "+e);
            }*/

            /**
             * DEBUG: Just for printing the results - INICIO
             */
            /*System.out.println("BasicOperator "+_name+": Writing results to file...");
            try {
                File outputFile = new File("operator"+_name+"QueryAnswer"+_msgsSent+".ttl");
                OutputStream outStream = new FileOutputStream(outputFile);
                Writer writer = new OutputStreamWriter(outStream, StandardCharsets.UTF_8);
                model2.write(writer, "TTL");
            }
            catch (Exception e)
            {
                System.out.println("BasicOperator "+_name+": Deu erro ai na resposta da query (construct): "+e);
            }*/
            /**
             * DEBUG: Just for printing the results - FIM
             */

            StreamSnipBlocks blockTriples = convertAnswerToTripleBlocks(model2,_query);
            _logger.debug("BasicOperator: Answer for query"+_query.get_queryID()+": "+blockTriples);


            // Send with kafka
            if(blockTriples!= null)
            {
                _numTriplesOut = blockTriples.getTotalNumberOfTriples();
                runProducer(blockTriples);

                Long procTime = System.currentTimeMillis() - getStartTime();
                //writeToFile(procTime.toString(), _executionNum, _numTriplesOut);
                resetStartTime();
                _executionNum++;
            }
        }
        else
        {
            ResultSet rs1 = QueryExecutionFactory.create(_query.get_query(), _model).execSelect();

            while(rs1.hasNext()) {
                QuerySolution postResult = rs1.next();
                //System.out.println(postResult);
            }

            // TODO: Criar a conversao para TripleBlock caso a query deste operador seja um SELECT. Por enquanto o SELECT nao esta suportado.
        }

        /**
         * Remove a janela do Model, para nao interferir na query da proxima janela.
         */
        for(int i=0;i<windowStatments.size();i++)
        {
            _model.remove(windowStatments.get(i));
        }

        System.out.println("BasicOperator "+_name+": appendData done");
    }


    public void appendData(Integer queryID, List<RdfQuadruple> triples)
    {
        System.out.println("BasicOperator"+_name+": appendData triples initialized");
        if(_resetStartTime)
        {
            _startTime = System.currentTimeMillis();
            _resetStartTime = false;
            _numTriplesIn = 0;
        }

        /**
         * Adiciona a janela do Model, para realizar a query.
         */
        ArrayList<Statement> windowStatments = new ArrayList<Statement>();



        for(int i=0; i<triples.size(); i++) {

            Resource sub = ResourceFactory.createResource(triples.get(i).getSubject());
            Property pred = ResourceFactory.createProperty(triples.get(i).getPredicate());

            Statement st;
            Resource obj;
            Literal lit;
            if(pred.toString().equalsIgnoreCase("http://rdfs.org/sioc/ns#id"))
            {
                //    Literal lit = ResourceFactory.createPlainLiteral(triplesBlock.get(i).getTriples().get(k).getObject());
                //    st = _model.createStatement(sub,pred,lit);
                lit = ResourceFactory.createPlainLiteral(triples.get(i).getObject().replace("\\", ""));
                st = _model.createStatement(sub,pred,lit);
                //System.out.println("POST_ID = "+lit);
            }
            else
            {
                obj = ResourceFactory.createResource(triples.get(i).getObject());
                st = _model.createStatement(sub,pred,obj);
            }

            _model.add(st);
            windowStatments.add(st);

        }

        System.out.println("BasicOperator"+_name+": model object (KB + Window): created");
        /**
         * Degug: Testar se adicionou tudo da janela na KB
         */
        if(_query.getIsDebugModeOn())
        {
            try{
                File outputFile = new File("operator"+_name+"_modelTestAll"+_msgsSent+".ttl");
                OutputStream outStream = new FileOutputStream(outputFile);
                Writer writer = new OutputStreamWriter(outStream, StandardCharsets.UTF_8);
                _model.write(writer, "TTL");
            }
            catch (Exception e)
            {
                System.out.println("BasicOperator-"+_name+" Deu algo errado ai: "+e);
            }

            System.out.println("BasicOperator-"+_name+": model object (KB + Window: persisted on file)");
        }

        /**
         * Realiza a query no Model que contém o static database (KB) + a janela.
         */
        if(_isCONSTRUCT) {
            // https://stackify.com/heres-how-to-calculate-elapsed-time-in-java/
            Instant start = Instant.now();

            //System.out.println("_query.get_query() = "+_query.get_query());
            Model model2 = QueryExecutionFactory.create( _query.get_query(), _model ).execConstruct();

            //model2.write(System.out, "TTL") ;

            Instant end = Instant.now();
            Duration timeElapsed = Duration.between(start, end);
            System.out.println("Query executed by basic operator "+_name+" in "+timeElapsed.getSeconds()+ " secs.");
            _logger.debug("Query executed by basic operator "+_name+" in "+timeElapsed.getSeconds()+ " secs.");


            /**
             * DEBUG: Just for printing the results of each window in a different file
             */
            if(_query.getIsDebugModeOn())
            {
                System.out.println("BasicOperator "+_name+": Writing results to file...");
                try {
                    File outputFile = new File("operator"+_name+"QueryAnswer"+_msgsSent+".ttl");
                    OutputStream outStream = new FileOutputStream(outputFile);
                    Writer writer = new OutputStreamWriter(outStream, StandardCharsets.UTF_8);
                    model2.write(writer, "TTL");
                }
                catch (Exception e)
                {
                    System.out.println("BasicOperator "+_name+": Deu erro ai na resposta da query (construct): "+e);
                }
            }

            StreamSnipBlocks blockTriples = convertAnswerToTripleBlocks(model2,_query);
            _logger.debug("BasicOperator: Answer for query"+_query.get_queryID()+": "+blockTriples);

            /**
             * DEBUG: Print the query results, but now from the StreamSnipBlocks, which are generated from the model2 (SPARQL query answer)
             */
            if(_query.getIsDebugModeOn())
            {
                for(int i=0;i<blockTriples.getNumberOfBlocks();i++)
                {
                    List<RdfQuadruple> block = blockTriples.getBlockWithOutDeleting(i).getTriples();
                    for(int j=0;j<block.size();j++)
                    {
                        _writerToFile.writeToTxtFile(block.get(j).toString());
                    }
                    _writerToFile.writeToTxtFile("-------------- Next Answer -------------");
                }
            }


            // Send with kafka
            if(blockTriples!= null)
            {
                _numTriplesOut = blockTriples.getTotalNumberOfTriples();
                runProducer(blockTriples);

                Long procTime = System.currentTimeMillis() - getStartTime();
                //writeToFile(procTime.toString(), _executionNum, _numTriplesOut);
                resetStartTime();
                _executionNum++;
            }
        }
        else
        {
            ResultSet rs1 = QueryExecutionFactory.create(_query.get_query(), _model).execSelect();

            while(rs1.hasNext()) {
                QuerySolution postResult = rs1.next();
                //System.out.println(postResult);
            }

            // TODO: Criar a conversao para TripleBlock caso a query deste operador seja um SELECT. Por enquanto o SELECT nao esta suportado.
        }

        /**
         * Remove a janela do Model, para nao interferir na query da proxima janela.
         */
        for(int i=0;i<windowStatments.size();i++)
        {
            _model.remove(windowStatments.get(i));
        }

        System.out.println("BasicOperator"+_name+": appendData done");
        System.out.println("---------------");
    }

    /**
     * Ele recebe uma query em CSPARQL e prepara para ser executada como um SPARQL em um modelo jena.
     *
     * @param queryID
     * @return
     */
    public boolean prepareQuery(int queryID) {
        _query = _queryDb.getQuery(queryID).getClone();

        // TODO Operator precisa fazer essas duas linhas:
        _producerKafkaID = "OP_"+_nodeID+"_"+_query.get_queryID();
        initializeOutputFiles(queryID, _name);

        if (_query == null) {
            _utils.error("Bad query or queryID do not exists.");
        }

        // Prepare query for this basic operator
        // Remove the FROM statement and identify if it is an CONSTRUCT query or a SELECT query
        String querySPARQL = _query.get_query();

        int indexCONSTRUCT = querySPARQL.indexOf("CONSTRUCT");
        int indexSELECT = querySPARQL.indexOf("SELECT");

        int indexOfQuery = 0;
        if (indexCONSTRUCT == -1 && indexSELECT == -1)
        { // Query com CONSTRUCT
            _utils.error("ERROR: A query sparql precisa ser um construct ou um select.");
        }
        else if (indexCONSTRUCT == -1)
        {
            indexOfQuery = indexSELECT;
            _isCONSTRUCT = false;
        }
        else if (indexSELECT == -1)
        {
            indexOfQuery = indexCONSTRUCT;
            _isCONSTRUCT = true;
        }
        else
        { // Tem um select e um construct nessa query, entao pega o de menos index.
            if (indexCONSTRUCT < indexSELECT)
            {
                indexOfQuery = indexCONSTRUCT;
                _isCONSTRUCT = true;
            }
            else
            {
                indexOfQuery = indexSELECT;
                _isCONSTRUCT = false;
            }
        }

        querySPARQL = querySPARQL.substring(indexOfQuery);


        String finalQuery = null;
        // Retirar os FROM
        if(_isCONSTRUCT) {
            int indexFROM = querySPARQL.indexOf("FROM");
            int indexWHERE = querySPARQL.indexOf("WHERE");
            String fristPart = querySPARQL.substring(0, indexFROM);
            String secondPart = querySPARQL.substring(indexWHERE);

            //System.out.println("Query a ser executada é (construct): ");
            finalQuery = fristPart + secondPart;
            //System.out.println(finalQuery);
        }
        else
        {
            int indexFROM = querySPARQL.indexOf("FROM");
            String fristPart = querySPARQL.substring(0, indexFROM);
            String middle = querySPARQL.substring(indexFROM);
            int indexWHERE = middle.indexOf("{");
            String secondPart = middle.substring(indexWHERE);

            System.out.println("BasicOperator "+_name+":Query a ser executada é (select): ");
            finalQuery = fristPart + secondPart;
            //System.out.println(finalQuery);
        }

        // Coloca a query final no obj. Já convertida para a sintaxe SPARQL.
        _query.set_query(_query.getQueryPREFIX()+finalQuery, false);

        // Carregar a base estática de RDF caso exista
        _model = ModelFactory.createDefaultModel();

        System.out.println("BasicOperator "+_name+":Reading model into memory...");

        for(int i=0;i<_query.get_staticDatabases().size();i++)
        {
            _model.read(_query.get_staticDatabases().get(i));
        }

        runConsumer(_query);

        return true;
    }









    /**
     * Está função converte um grafo RDF Model do Jena em uma lista de RDF graphs.
     *
     * @param answerTriples: O modelo jena que representa a lista de triplas retornada pela query.
     * @param query: A querry que produziu as triplas
     * @return Retorna: A lista de RDF graphs identificados pela propriedade sioc:id
     */
    private StreamSnipBlocks convertAnswerToTripleBlocks(Model answerTriples, Query query)
    {
        String queryMsgIds = "select distinct ?msgID WHERE { ?msg sioc:id ?msgID  } ";

        /**
         * DEBUG: Just for printing the results - INICIO
         */
        /*System.out.println("BasicOperator "+_name+":Writing results to file...");
        try {
            File outputFile = new File("answer"+_name+"Result"+_msgsSent+".ttl");
            OutputStream outStream = new FileOutputStream(outputFile);
            Writer writer = new OutputStreamWriter(outStream, StandardCharsets.UTF_8);
            answerTriples.write(writer, "TTL");
        }
        catch (Exception e)
        {
            System.out.println("BasicOperator "+_name+":Deu erro ai na resposta da query (construct): "+e);
        }*/
        /**
         * DEBUG: Just for printing the results - FIM
         */

        ResultSet rs1 = QueryExecutionFactory.create( query.getQueryPREFIX()+queryMsgIds, answerTriples ).execSelect();

        ///////////////////////////////////////////////////////////////////////////////////////////
        // STEP 2: Para cada um dos sioc:id eu pego todas as triplas dele crio os RDFQuadruple,
        // usando o System.currentTimeMillis() como timestamp.
        ///////////////////////////////////////////////////////////////////////////////////////////
        StreamSnipBlocks allTriples = new StreamSnipBlocks();

        while(rs1.hasNext())
        {
            QuerySolution postResult = rs1.next();
            //String postID = postResult.get("postID").asResource().toString().replace("\\\\", ""); // asLiteral
            String msgID = postResult.get("msgID").asLiteral().toString(); // asLiteral

            if(_query.getIsDebugModeOn()) {
                System.out.println("msgID = " + msgID);
            }

            // Replace postID number with the postID number to extract.
            String constructContentTemplate = query.getConstructContent();
            //System.out.println("constructContentTemplate = "+constructContentTemplate);

            String temp = constructContentTemplate.replaceFirst("(sioc:id)(\\s*)((\\S+)[^.])", "sioc:id \""+msgID+"\" .");
            String constructContent = temp.replaceFirst("[.][.]", ".");

            constructContent = constructContent.replaceAll("(\\^\\^http://www\\.w3\\.org/2001/XMLSchema#)([^\\\"]+)", "");
            //System.out.println("constructContent = "+constructContent);

            // TODO: Precisa colocar um OPTIONAL clause por tripla! Não é o bloco que é opcional, é cada tripla.
            // Vai ficar lento a query, mas é o jeito.
            String whereContent = query.getWhereBlock(constructContent);
            //System.out.println("whereContent = "+whereContent);

            String queryToGetRdfGraphs = "construct { "+constructContent+" }  where { "+whereContent+" }";

            if(_query.getIsDebugModeOn())
            {
                System.out.println("------------------");
                System.out.println(query.getQueryPREFIX()+queryToGetRdfGraphs);
                System.out.println("------------------");
            }

            Model model2 = QueryExecutionFactory.create( query.getQueryPREFIX()+queryToGetRdfGraphs, answerTriples).execConstruct();

            TriplesBlock postTriplesBlock = new TriplesBlock();
            StmtIterator it = model2.listStatements();

            if(_query.getIsDebugModeOn())
            {
                System.out.println("msgID = "+msgID);
            }

            //System.out.println("it.hasNext() = "+it.hasNext());
            while(it.hasNext())
            {
                Statement stmt = it.nextStatement();

                if(_query.getIsDebugModeOn())
                {
                    System.out.println("stmt = "+stmt.toString());
                }

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



    /************ KAFKA PRODUCERS AND CONSUMERS ************/

    /**
     * TODO: ESSE AQUI PODERIA SER UMA FUNCAO DA CLASS ABSTRATA do publisher do modulo Operator.
     * @param triples
     */
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

        writeJsonOutToFile(jsonStream.toString(), _executionNum);
    }


    /**
     *
     * @param query
     */
    public void runConsumer(Query query)
    {
        String consumerGroupName = "BasicOpConsumer_"+_nodeID+"_"+query.get_queryID();
        String topicName = query.get_inputStreamIDFromAggregator();
        _logger.debug(_name+"Consumer(listening) on topic = {} - ConsumerGroup = {}", topicName, consumerGroupName );
        System.out.println(_name+"Consumer(listening) on topic = "+topicName+" - ConsumerGroup = "+consumerGroupName );

        List<String> topics = new ArrayList<String>();
        topics.add(topicName);
        KafkaConsumer<Integer, String> consumer = ConsumerCreator.createConsumer(topics,
                IOperatorConstants.KAFKA_BROKERS,
                consumerGroupName,
                IOperatorConstants.MAX_POLL_RECORDS,
                IOperatorConstants.OFFSET_RESET_EARLIER); // OFFSET_RESET_EARLIER

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
                final ConsumerRecords<Integer, String> consumerRecords = _consumer.poll(Duration.ofMillis(Long.MAX_VALUE));

                for (ConsumerRecord<Integer, String> record : consumerRecords) {

                    System.out.println("--------------- BasicOperator ---------------");
                    //System.out.println("Record Key " + record.key());
                    //System.out.println("Consumer: Record value " + record.value());
                    _logger.debug("Consumer received: Record value " + record.value());
                    //System.out.println("Record partition " + record.partition());
                    //System.out.println("Record offset " + record.offset());

                    // Todas as queries que tiverem query.get_inputStreamIDFromAggregator() == topicName
                    // essas sao as queries que precisam receber o dado.
                    _logger.debug("Consumer "+_name+" (received) for query = {} - ConsumerGroup = {}", _query.get_queryID().toString(), _consumerName );
                    System.out.println("Consumer "+_name+" (received) for query = "+_query.get_queryID().toString()+" - ConsumerGroup = "+_consumerName );

                    if(_utils.getAttributeFromMsg(record.value(), "isTwitterDB") != null &&
                            _utils.getAttributeFromMsg(record.value(), "isTwitterDB").equals("true"))
                        isTwitterDB = true;
                    else
                        isTwitterDB = false;


                    ArrayList<TriplesBlock> triplesBlock = new ArrayList<TriplesBlock>();
                    if(isTwitterDB) {
                        triplesBlock = _utils.getTriplesBlockFromJSONSnip(record.value());

                        appendData(_query.get_queryID(), triplesBlock);
                    }
                    else
                    {
                        ArrayList<RdfQuadruple> triples = _utils.getTriplesFromJSONSnip(record.value());

                        appendData(_query.get_queryID(), triples);
                    }
                    /*
                        //System.out.println("Number of blocks: "+triplesBlock.size());

                        triples = new ArrayList<Triple>();
                        for(int i=0; i<triplesBlock.size(); i++)
                        {
                            //System.out.println(triplesBlock.get(i).getTriples());
                            _numTriplesIn = _numTriplesIn + triplesBlock.get(i).size();
                            //System.out.println("NumTriples = "+triplesBlock.get(i).getTriples().size());
                            //System.out.println("NumTriples = "+triplesBlock.get(i).size());

                            // O modelo basico de triple é o modelo do CSPARQL, o RDFQuadruple, entao eu tive que converter para
                            // o modelo do Jena, o Triple, para eu poder adicionar no Model jena.
                            for(int k=0;k<triplesBlock.get(i).getTriples().size();k++)
                            {
                                Node subject = NodeFactory.createLiteral(triplesBlock.get(i).getTriples().get(k).getSubject());
                                Node predicate = NodeFactory.createLiteral(triplesBlock.get(i).getTriples().get(k).getPredicate());
                                Node object = NodeFactory.createLiteral(triplesBlock.get(i).getTriples().get(k).getObject());

                                Triple triple = new Triple(subject, predicate, object);
                                triples.add(triple);
                            }
                        }
                    }
                    else
                    {
                        triples = new ArrayList<Triple>();
                        triples = _utils.getSimpleTriplesFromJSONSnip(record.value());
                        _numTriplesIn = _numTriplesIn + triples.size();
                    }*/

                    //appendData(_query.get_queryID(), triplesBlock);
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

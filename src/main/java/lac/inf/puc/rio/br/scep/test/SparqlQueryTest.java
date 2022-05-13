package lac.inf.puc.rio.br.scep.test;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.*;
import junit.framework.TestCase;
import lac.inf.puc.rio.br.scep.database.IQueryDatabase;
import lac.inf.puc.rio.br.scep.database.QueryDatabase;
import lac.inf.puc.rio.br.scep.model.AggInfo;
import lac.inf.puc.rio.br.scep.model.Query;
import lac.inf.puc.rio.br.scep.model.QueryInfo;
import lac.inf.puc.rio.br.scep.model.TriplesBlock;
import lac.inf.puc.rio.br.scep.utils.Utils;
import lac.inf.puc.rio.br.scep.utils.WriterToFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SparqlQueryTest extends TestCase
{
    private static IQueryDatabase _queryDb;
    protected static Utils _utils;
    private static int _queryID = 11;

    private static WriterToFile _writerToFile;


    protected void setUp()
    {

    }

    public void testBasicOperator()
    {

    }

    public void testReadPropertiesFile()
    {
        File jarDir = new File(ClassLoader.getSystemClassLoader().getResource(".").getPath());
        String slash;
        if(Utils.isThisWindows())
            slash = "\\";
        else
            slash = "/";

        String appConfigPath = jarDir.getAbsolutePath() + slash + "nodeexample.properties";

        Properties prop = new Properties();
        // load a properties file
        try {
            prop.load(new FileInputStream(appConfigPath));
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        AggInfo _queriesOfThisAgg = new AggInfo();

        String queriesStr = prop.getProperty("queryIDs").trim();
        String[] nodeQueries = queriesStr.split(",");
        System.out.println("queriesStr = "+queriesStr);
        for(int i=0;i<nodeQueries.length;i++)
        {
            QueryInfo queryInfo = new QueryInfo();
            nodeQueries[i] = nodeQueries[i].trim();
            System.out.println("nodeQueries["+i+"] = "+nodeQueries[i].trim());

            String nodeInfo_inputStr = prop.getProperty("query"+nodeQueries[i]+"_inputTopic").trim();
            System.out.println("nodeInfo_inputStr query"+nodeQueries[i]+" = "+nodeInfo_inputStr);

            String nodeInfo_outputStr = prop.getProperty("query"+nodeQueries[i]+"_outputTopic").trim();
            System.out.println("nodeInfo_outputStr query"+nodeQueries[i]+" = "+nodeInfo_outputStr);

            String nodeInfo_windInfoStr = prop.getProperty("query"+nodeQueries[i]+"_windowInfo").trim();
            System.out.println("nodeInfo_windInfoStr query"+nodeQueries[i]+" = "+nodeInfo_windInfoStr);

            String nodeInfo_inputStreamTopicsStr = prop.getProperty("query"+nodeQueries[i]+"_inputStreamTopics").trim();
            String[] inputs = nodeInfo_inputStreamTopicsStr.split(",");
            ArrayList<String> inputsAList = new ArrayList<String>();
            for (int j=0;j<inputs.length;j++)
            {
                System.out.println("nodeInfo_inputStreamTopicsStr query"+nodeQueries[i]+" = "+inputs[j]);
                inputsAList.add(inputs[j]);
            }

            queryInfo.set_queryID(Integer.valueOf(nodeQueries[i]));
            queryInfo.set_inputStreams(inputsAList);
            queryInfo.set_queryInputTopic(nodeInfo_inputStr);
            queryInfo.set_queryOutputTopic(nodeInfo_outputStr);
            queryInfo.setQueryWindowInfo(nodeInfo_windInfoStr);

            System.out.println("Query"+queryInfo.get_queryID()+ "getWindowType()"+queryInfo.getWindowType());
            System.out.println("Query"+queryInfo.get_queryID()+ "getWindowSize()"+queryInfo.getWindowSize());

            _queriesOfThisAgg.addQueryInfo(queryInfo);
        }
    }

    public static void main(String[] args)
    {
        if(_queryDb == null) {
            //_queryDb = new QueryDatabase();
            _queryDb = QueryDatabase.getInstance();
        }

        _writerToFile = new WriterToFile("TestCode");

        Model model = ModelFactory.createDefaultModel();

        System.out.println("model default size = "+model.size());
        System.out.println("Reading model into memory...");
        model.read("examples/MusicalArtists.rdf");
        System.out.println("Model loaded.");

        System.out.println("Number of statements: "+model.size());

        /**
         * Carregar tweets na KB
         */
        Boolean isThereMoreTweets = true;
        int totalAnalizedTweets = 0;
        int offset = 0;
        int numMsgsPerTime = 1;

        //Model modelTweet = ModelFactory.createDefaultModel();

        ArrayList<List<String>> allJsons = new ArrayList<List<String>>();
        List<String> jsons=null;
        while(isThereMoreTweets) {
            jsons = _writerToFile.readJsonMsgsFromFile(offset, numMsgsPerTime);
            if(jsons == null)
                isThereMoreTweets = false;
            else
                allJsons.add(jsons);

            offset = offset + numMsgsPerTime;
        }

        ArrayList<TriplesBlock> triplesBlock = null;
        triplesBlock = new ArrayList<TriplesBlock>();
        if(allJsons.size() != 0)
            for(int t=0;t<allJsons.size();t++) {
                for (int i = 0; i < allJsons.get(t).size(); i++) {
                    triplesBlock.addAll(_utils.getTriplesBlockFromJSONSnip(allJsons.get(t).get(i)));
                }
            }

        ArrayList<Triple> triples = null;
        if(triplesBlock!= null)
        for(int j=0;j<triplesBlock.size();j++)
        {
            triples = new ArrayList<Triple>();
            for(int i=0; i<triplesBlock.size(); i++)
            {
                for(int k=0;k<triplesBlock.get(i).getTriples().size();k++)
                {
                    Node subject = NodeFactory.createLiteral(triplesBlock.get(i).getTriples().get(k).getSubject());
                    Node predicate = NodeFactory.createLiteral(triplesBlock.get(i).getTriples().get(k).getPredicate());
                    Node object = NodeFactory.createLiteral(triplesBlock.get(i).getTriples().get(k).getObject());

                    Triple triple = new Triple(subject, predicate, object);
                    triples.add(triple);

                    Resource sub = ResourceFactory.createResource(triplesBlock.get(i).getTriples().get(k).getSubject());
                    Property pred = ResourceFactory.createProperty(triplesBlock.get(i).getTriples().get(k).getPredicate());
                    Resource obj = ResourceFactory.createResource(triplesBlock.get(i).getTriples().get(k).getObject());

                    //String obj =  triplesBlock.get(i).getTriples().get(k).getObject();

                    Statement st = model.createStatement(sub,pred,obj);
                    model.add(st);
                }
            }
        }

        if(triples != null)
        for(int k=0;k<triples.size();k++)
        {
            //modelTweet.getGraph().add(triples.get(k));
        }

        try{
            File outputFile = new File("modelTestAll.ttl");
            OutputStream outStream = new FileOutputStream(outputFile);
            Writer writer = new OutputStreamWriter(outStream, StandardCharsets.UTF_8);
            model.write(writer, "TTL");
        }
        catch (Exception e)
        {
            System.out.println("Deu algo errado ai: "+e);
        }
        // TODO: Verificar se oq eu adiciono no model é oq tem no tweet.txt
        // TODO: Depois disso junta os models e ve se ta tudo certo.
        // TODO: Depois d juntar, faz a query


        /**
         * Preparar a query CSPARQL -> SPARQL
         */
        Query query = _queryDb.getQuery(_queryID);
        if(query == null)
        {
            _utils.error("Bad query or queryID do not exists.");
            return;
        }

        System.out.println("query.get_query()="+query.get_query());
        System.out.println("query.getQueryPREFIX()="+query.getQueryPREFIX());
        System.out.println("query.getWindowType()="+query.getWindowType());
        System.out.println("query.get_staticDatabases().get(0)="+query.get_staticDatabases().get(0));

        String querySPARQL = query.get_query();

        int indexCONSTRUCT = querySPARQL.indexOf("CONSTRUCT");
        int indexSELECT = querySPARQL.indexOf("SELECT");
        Boolean isCONSTRUCT = false;

        int indexOfQuery = 0;
        if (indexCONSTRUCT == -1 && indexSELECT == -1)
        { // Query com CONSTRUCT
            _utils.error("ERROR: A query sparql precisa ser um construct ou um select.");
        }
        else if (indexCONSTRUCT == -1)
        {
            indexOfQuery = indexSELECT;
            isCONSTRUCT = false;
        }
        else if (indexSELECT == -1)
        {
            indexOfQuery = indexCONSTRUCT;
            isCONSTRUCT = true;
        }
        else
        { // Tem um select e um construct nessa query, entao pega o de menos index.
            if (indexCONSTRUCT < indexSELECT)
            {
                indexOfQuery = indexCONSTRUCT;
                isCONSTRUCT = true;
            }
            else
            {
                indexOfQuery = indexSELECT;
                isCONSTRUCT = false;
            }
        }

        querySPARQL = querySPARQL.substring(indexOfQuery);


        String finalQuery = null;
        // Retirar os FROM
        if(isCONSTRUCT) {
            int indexFROM = querySPARQL.indexOf("FROM");
            int indexWHERE = querySPARQL.indexOf("WHERE");
            String fristPart = querySPARQL.substring(0, indexFROM);
            String secondPart = querySPARQL.substring(indexWHERE);

            System.out.println("Query a ser executada é (construct): ");
            finalQuery = fristPart + secondPart;
            System.out.println(finalQuery);
        }
        else
        {
            int indexFROM = querySPARQL.indexOf("FROM");
            String fristPart = querySPARQL.substring(0, indexFROM);
            String middle = querySPARQL.substring(indexFROM);
            int indexWHERE = middle.indexOf("{");
            String secondPart = middle.substring(indexWHERE);

            System.out.println("Query a ser executada é (select): ");
            finalQuery = fristPart + secondPart;
            System.out.println(finalQuery);
        }


        if(isCONSTRUCT) {
            System.out.println("Executing query...");

            // https://stackify.com/heres-how-to-calculate-elapsed-time-in-java/
            Instant start = Instant.now();
            Model model2 = QueryExecutionFactory.create( query.getQueryPREFIX() + finalQuery, model ).execConstruct();
            Instant end = Instant.now();
            Duration timeElapsed = Duration.between(start, end);
            System.out.println("Query executed in "+timeElapsed.getSeconds());

            System.out.println("Writing results to file...");
            try {
                File outputFile = new File("queryAnswer.ttl");
                OutputStream outStream = new FileOutputStream(outputFile);
                Writer writer = new OutputStreamWriter(outStream, StandardCharsets.UTF_8);
                model2.write(writer, "TTL");
            }
            catch (Exception e)
            {
                System.out.println("Deu erro ai na resposta da query (construct): "+e);
            }

            /*
            System.out.println("Writing results to stdout...");
            StmtIterator it = model2.listStatements();
            System.out.println("Resposta (CONSTRUCT): ");
            while(it.hasNext()) {
                Statement stmt = it.nextStatement();

                Triple t = stmt.asTriple();
                System.out.println(t);
            }*/
        }
        else
        {
            ResultSet rs1 = QueryExecutionFactory.create(query.getQueryPREFIX() + finalQuery, model).execSelect();

            /*
            try {
                File outputFile = new File("queryAnswer.ttl");
                OutputStream outStream = new FileOutputStream(outputFile);
                Writer writer = new OutputStreamWriter(outStream, StandardCharsets.UTF_8);
                rs1.write(writer, "TTL");
            }
            catch (Exception e)
            {
                System.out.println("Deu erro ai na resposta da query (select): "+e);
            }*/

            System.out.println("Resposta (Select): ");
            while(rs1.hasNext()) {
                QuerySolution postResult = rs1.next();
                System.out.println(postResult);
            }
        }

    }
}

// Total: 169338
// Tweet: 966
// KB:    168376

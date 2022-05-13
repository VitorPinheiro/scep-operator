package lac.inf.puc.rio.br.scep.utils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import lac.inf.puc.rio.br.scep.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import eu.larkc.csparql.cep.api.RdfQuadruple;
import lac.inf.puc.rio.br.scep.model.StreamSnipBlocks;
import lac.inf.puc.rio.br.scep.model.TriplesBlock;

/**
 * @author vitor
 *
 */
public class Utils 
{
	private static Logger logger = LogManager.getLogger(Utils.class);
	
	public Utils()
	{
		logger.debug("Utils created.");
	}
	
	
	
	/**
	 *  Get a json with the format of blocks of triples and convert to to an ArrayList of TriplesBlock
	 * @param jsonSnip
	 * @return
	 */
	public static ArrayList<TriplesBlock> getTriplesBlockFromJSONSnip(String jsonSnip)
	{
		String tripleList = getTriplesJsonFromAtributteMsg(jsonSnip);	    
	    JsonArray arrayTriples = parseJsonArrayIntoString(tripleList);
	    
	    ArrayList<TriplesBlock> triples = new ArrayList<TriplesBlock>();
	    
	    for(int i=0; i< arrayTriples.size(); i++)
	    {
	    	JsonArray blockTriplesArray = parseJsonArrayIntoString(arrayTriples.get(i).toString());
	    	String tripleJson = blockTriplesArray.get(0).toString();
	    	
	    	JsonArray blockTriplesArray2 = parseJsonArrayIntoString(tripleJson);
	    	
	    	TriplesBlock block = new TriplesBlock();
	    	for(int j=0; j< blockTriplesArray2.size(); j++)
	    	{
	    		
	    		String tripleJson2 = blockTriplesArray2.get(j).toString();
	    			    		
	    		RdfQuadruple triple = new RdfQuadruple(getAttributeFromMsg(tripleJson2,  "Subject"), 
						getAttributeFromMsg(tripleJson2,  "Predicate"), 
					 	getAttributeFromMsg(tripleJson2,  "Object"), 
						Long.valueOf(getAttributeFromMsg(tripleJson2,  "Timestamp")));	    		
	    		
	    		
	    		block.addTriple(triple);
	    	}
	    	
	    	triples.add(block);
	    }
	    return triples;
	}

	/**
	 * Metodo utilizado para criar uma lsita de RdfQuadruple. Esse formato é usado pelo CSparql.
	 * @param jsonSnip
	 * @return
	 */
	public static ArrayList<RdfQuadruple> getTriplesFromJSONSnip(String jsonSnip)
	{
		String tripleList = getTriplesJsonFromAtributteMsg(jsonSnip);	    
	    JsonArray arrayTriples = parseJsonArrayIntoString(tripleList);
	    
	    ArrayList<RdfQuadruple> triples = new ArrayList<RdfQuadruple>();
	    for(int i=0; i< arrayTriples.size(); i++)
	    {
	    	String tripleJson = arrayTriples.get(i).toString();
	    	
	    	RdfQuadruple triple = new RdfQuadruple(getAttributeFromMsg(tripleJson,  "Subject"), 
	    									getAttributeFromMsg(tripleJson,  "Predicate"), 
	    								 	getAttributeFromMsg(tripleJson,  "Object"), 
	    									Long.valueOf(getAttributeFromMsg(tripleJson,  "Timestamp")));
	    	
	    	triples.add(triple);
	    }
	    return triples;
	}

	/**
	 * Metodo utilizado para transformar o jsonSnip em uma lista de Triples para o operador BasicOperator
	 * @param jsonSnip
	 * @return
	 */
	public static ArrayList<Triple> getSimpleTriplesFromJSONSnip(String jsonSnip)
	{
		String tripleList = getTriplesJsonFromAtributteMsg(jsonSnip);
		JsonArray arrayTriples = parseJsonArrayIntoString(tripleList);

		ArrayList<Triple> triples = new ArrayList<Triple>();
		for(int i=0; i< arrayTriples.size(); i++)
		{
			String tripleJson = arrayTriples.get(i).toString();

			Node subject = NodeFactory.createLiteral(getAttributeFromMsg(tripleJson,  "Subject"));
			Node predicate = NodeFactory.createLiteral(getAttributeFromMsg(tripleJson,  "Predicate"));
			Node object = NodeFactory.createLiteral(getAttributeFromMsg(tripleJson,  "Object"));

			Triple triple = new Triple(subject, predicate, object);

			triples.add(triple);
		}
		return triples;
	}
	
	
	
	public static String getAttributeFromMsg(String jsonMsgStr, String attribute)
    {
		JsonObject jsonMsg; 
		String ret = null;
		JsonParser parser = new JsonParser();
		try {
		    jsonMsg = parser.parse(jsonMsgStr).getAsJsonObject(); //new Gson().fromJson(jsonMsgStr, JsonObject.class);
	
		    if(jsonMsg.get(attribute) == null)
		    {
		    	//System.out.println("There is no such attriute: "+attribute);
		    	return null;
		    }
	
		    ret = jsonMsg.get(attribute).toString().replaceAll("\"" , "");
		}
		catch (Exception e){
		    //e.printStackTrace();
		    System.out.println("Invalid json format: "+ attribute);
		    return null;
		}
	
		//System.out.println("Valid json format");
		return ret;
    }
	
	/**
     * Returns String of element of Json array of position index
     * @param arraySensorDataObjectsJson
     * @return element of jsonArray as string
     * @author Pumar
     */
    public static JsonArray parseJsonArrayIntoString(String arraySensorDataObjectsJson)
    {
		JsonParser parser = new JsonParser();
	
		JsonArray objectsArray = (JsonArray) parser.parse(arraySensorDataObjectsJson);
	
		return objectsArray;
    }
	
	/**
     * Returns substring of attribute given, used for getting Json Array
     * @param jsonMsgStr A mensagem string no formato json.
     * @return
     */
    public static String getTriplesJsonFromAtributteMsg(String jsonMsgStr)
    {
		JsonObject jsonMsg; 
		String ret = null;
		JsonParser parser = new JsonParser();
		try {
		    jsonMsg = parser.parse(jsonMsgStr).getAsJsonObject();
		    ret = jsonMsg.get("Triples").toString();
		}
		catch (Exception e){
		    //e.printStackTrace();
		    System.out.println("Invalid json format e2"+ "Triples");
		    return null;
		}
		return ret;
    }

	private static boolean isWindows(String OS) {
		return (OS.indexOf("win") >= 0);
	}

	private static boolean isMac(String OS) {
		return (OS.indexOf("mac") >= 0);
	}

	private static boolean isUnix(String OS) {
		return (OS.indexOf("nix") >= 0
				|| OS.indexOf("nux") >= 0
				|| OS.indexOf("aix") > 0);
	}

	private static boolean isSolaris(String OS) {
		return (OS.indexOf("sunos") >= 0);
	}

    public static Boolean isThisWindows()
	{
		String OS = System.getProperty("os.name").toLowerCase();

		Boolean ret = false;
		if (isWindows(OS)) {
			ret = true;
		} else if (isMac(OS)) {
			ret = false;
		} else if (isUnix(OS)) {
			ret = false;
		} else if (isSolaris(OS)) {
			ret = false;
		} else {
			ret = false;
		}

		return ret;
	}

	public static AggInfo getAggInfoFromConfig(String nodeFileName)
	{
		File jarDir = new File(ClassLoader.getSystemClassLoader().getResource(".").getPath());
		String slash;
		if(isThisWindows())
			slash = "\\";
		else
			slash = "/";

		String appConfigPath = jarDir.getAbsolutePath() + slash + nodeFileName;//"node.properties";

		Properties prop = new Properties();

		try {
			prop.load(new FileInputStream(appConfigPath));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		AggInfo _queriesOfThisAgg = new AggInfo();

		String queriesStr = prop.getProperty("queryIDs").trim();
		String[] nodeQueries = queriesStr.split(",");
		//System.out.println("queriesStr = "+queriesStr);
		for(int i=0;i<nodeQueries.length;i++)
		{
			QueryInfo queryInfo = new QueryInfo();
			nodeQueries[i] = nodeQueries[i].trim();
			//System.out.println("nodeQueries["+i+"] = "+nodeQueries[i].trim());

			String nodeInfo_inputStr = prop.getProperty("query"+nodeQueries[i]+"_inputTopic").trim();
			//System.out.println("nodeInfo_inputStr query"+nodeQueries[i]+" = "+nodeInfo_inputStr);

			String nodeInfo_outputStr = prop.getProperty("query"+nodeQueries[i]+"_outputTopic").trim();
			//System.out.println("nodeInfo_outputStr query"+nodeQueries[i]+" = "+nodeInfo_outputStr);

			String nodeInfo_windInfoStr = prop.getProperty("query"+nodeQueries[i]+"_windowInfo").trim();
			//System.out.println("nodeInfo_windInfoStr query"+nodeQueries[i]+" = "+nodeInfo_windInfoStr);

			String nodeInfo_inputStreamTopicsStr = prop.getProperty("query"+nodeQueries[i]+"_inputStreamTopics").trim();
			String[] inputs = nodeInfo_inputStreamTopicsStr.split(",");
			ArrayList<String> inputsAList = new ArrayList<String>();
			for (int j=0;j<inputs.length;j++)
			{
				//System.out.println("nodeInfo_inputStreamTopicsStr query"+nodeQueries[i]+" = "+inputs[j]);
				inputsAList.add(inputs[j]);
			}

			queryInfo.set_queryID(Integer.valueOf(nodeQueries[i]));
			queryInfo.set_inputStreams(inputsAList);
			queryInfo.set_queryInputTopic(nodeInfo_inputStr);
			queryInfo.set_queryOutputTopic(nodeInfo_outputStr);
			queryInfo.setQueryWindowInfo(nodeInfo_windInfoStr);

			_queriesOfThisAgg.addQueryInfo(queryInfo);
		}

		return _queriesOfThisAgg;
	}
	
	public static String getProperty(String nodeFileName, String propertyName)
	{
		//String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();

		File jarDir = new File(ClassLoader.getSystemClassLoader().getResource(".").getPath());
		String slash;
		if(isThisWindows())
			slash = "\\";
		else
			slash = "/";

		String appConfigPath = jarDir.getAbsolutePath() + slash + nodeFileName;//"node.properties";

		Properties appProps = new Properties();

		try {
			appProps.load(new FileInputStream(appConfigPath));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block		
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		String propertyValue = appProps.getProperty(propertyName);
		
		return propertyValue;
	}
	
	
	public static JsonObject streamBlocksToJsonObject(StreamSnipBlocks triplesBlock, String nodeID, String msgNumber, String producer, boolean isRDFgraph)
	{
		if(triplesBlock == null)
		{
			error("triples == null. Must have triples to send.");
			return null;
		}
		
		if(nodeID == null)
		{
			error("nodeID = null. Must have a nodeID");
			return null;
		}
		
		if(msgNumber == null)
		{
			error("msgNumber = null. Must have a msgNumber.");
			return null;
		}
		
		if(producer == null)
		{
			error("producer = null. Must have a producer.");
			return null;
		}
		
		JsonObject jsonObj = new JsonObject();

		jsonObj.addProperty("ID", nodeID+"_"+producer+"_"+msgNumber); // O ID da snip gerada.
		jsonObj.addProperty("Producer", producer.toString()); // O ID da query que gerou essa snip ou o ID da stream inicial
		
		if(isRDFgraph)
			jsonObj.addProperty("isRDFgraph", "true");
		else
			jsonObj.addProperty("isRDFgraph", "false");		
		
		jsonObj.add("Triples", blockToJsonArray(triplesBlock));
		
		return jsonObj;
	}
	
	public static JsonArray blockToJsonArray(StreamSnipBlocks triplesBlock)
	{
		JsonArray array = new JsonArray();
		try 
		{		
			//System.out.println("triplesBlock.getNumberOfBlocks() = "+triplesBlock.getNumberOfBlocks());
			for(int j=0;j<triplesBlock.getNumberOfBlocks();j++)
			{				
			    TriplesBlock block = triplesBlock.getBlockWithOutDeleting(j); 
			    
		    	//JsonObject jsonObj = new JsonObject();
			    JsonArray innerArray = new JsonArray();
		    	
		    	//jsonObj.add("Block"+j, toJsonArray(block.getTriples()));
			    innerArray.add(toJsonArray(block.getTriples()));
		    	array.add(innerArray);
			}
		    
		} catch (Exception e) {
		    error(e.getMessage());
		    return null;
		}
		
		return array;
	}
	
	
	/**
     * 
     * @param triples The triples of the snip.
     * @param nodeID The ID of the node which this snip was produced.
     * @param msgNumber It is only a counter that says how many messages the sender on the nodeID generated.
     * @param producer The query/aggregator/initialStream that produced this snip.
     * @return
     */
	public static JsonObject toJsonObject(List<RdfQuadruple> triples, String nodeID, String msgNumber, String producer, boolean isRDFgraph)
	{
		if(triples == null)
		{
			error("triples == null. Must have triples to send.");
			return null;
		}
		
		if(nodeID == null)
		{
			error("nodeID = null. Must have a nodeID");
			return null;
		}
		
		if(msgNumber == null)
		{
			error("msgNumber = null. Must have a msgNumber.");
			return null;
		}
		
		if(producer == null)
		{
			error("producer = null. Must have a producer.");
			return null;
		}
		
		JsonObject jsonObj = new JsonObject();

		jsonObj.addProperty("ID", nodeID+"_"+producer+"_"+msgNumber); // O ID da snip gerada.
		jsonObj.addProperty("Producer", producer.toString()); // O ID da query que gerou essa snip ou o ID da stream inicial
		
		if(isRDFgraph)
			jsonObj.addProperty("isRDFgraph", "true");
		else
			jsonObj.addProperty("isRDFgraph", "false");
		
		jsonObj.add("Triples", toJsonArray(triples));
		
		return jsonObj;
	}
	
	public static JsonArray toJsonArray(List<RdfQuadruple> triples) 
	{
		try 
		{
		    JsonArray array = new JsonArray();
		    		    
		    for(int i=0; i<triples.size();i++) 
		    {
		    	JsonObject json_object = new JsonObject();
				json_object.addProperty("Subject", triples.get(i).getSubject());
				json_object.addProperty("Predicate", triples.get(i).getPredicate());
				json_object.addProperty("Object", triples.get(i).getObject());
				json_object.addProperty("Timestamp", triples.get(i).getTimestamp());
				
				array.add(json_object);
		    }
		    
		    return array;
		} catch (Exception e) {
		    error(e.getMessage());
		    return null;
		}
	}
	
	public static JsonArray toJsonArray(String[] list)
	{
		try 
		{
		    JsonArray array = new JsonArray();
		    		    
		    for(int i=0; i<list.length;i++) 
		    {
		    	JsonObject json_object = new JsonObject();
				json_object.addProperty("Gateway", list[i]);				
				array.add(json_object);
		    }
		    
		    return array;
		} catch (Exception e) {
		    error(e.getMessage());
		    return null;
		}
	}
	
	public static BufferedReader readFileAsStream(String path) 
	{
        try {
          if (path == "-") {
            return new BufferedReader(
              new InputStreamReader(System.in));
          }
          return new BufferedReader(
            new FileReader(path));
        }
        catch (FileNotFoundException e) 
        {
        	error(String.format("cannot access '%s'", new Object[] { path }));
          
          throw new AssertionError(); 
        }
	}
	
	public static ArrayList<RdfQuadruple> readTriples(BufferedReader input)
    {
    	ArrayList<RdfQuadruple> triples = new ArrayList<RdfQuadruple>();
    	try
        {
          String line;
          while ((line = input.readLine()) != null) 
          { 
            String[] toks = line.split(" +");
            if (toks.length != 4) {
            	error("bad line: " + line);
            }
            
            long time = parseTime(toks[3]);
            if (time < 0L) {
              error("bad time value: " + toks[3]);
            }
            
            
            RdfQuadruple q = new RdfQuadruple(toks[0], toks[1], toks[2], parseTime(toks[3]));
            
            /*RdfQuadruple q = new RdfQuadruple(toks[0], toks[1], toks[2], System.currentTimeMillis());
            try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
            
            //System.out.println("Line added: "+q+" at "+q.getTimestamp() / 1000 + "secs");
            
            triples.add(q);
          }      
          
        } catch (Exception e) {
        	error("read error: " + e.getMessage());
        } 
    	
    	return triples;
    }
	
	private static long parseTime(String str) 
	{
        String[] toks = str.split(":");
        if (toks.length == 1)
          return Long.parseLong(toks[0]);
        if (toks.length == 3) {
          long h = Long.parseLong(toks[0]);
          long m = Long.parseLong(toks[1]);
          long s = Long.parseLong(toks[2]);
          h = h > 0L ? h : 0L;
          m = m > 0L ? m : 0L;
          s = s > 0L ? s : 0L;
          return (h * 3600L + m * 60L + s * 1L) * 1000L;
        }
        return -1L;
	}	
	
	public static Integer parseToIntegerIfPossible(String strNum) {
		Integer num;
		try {
	        num = Integer.parseInt(strNum);
	    } catch (NumberFormatException | NullPointerException nfe) {
	        return null;
	    }
	    return num;
	}
	
	public static void error(String msg) 
	{
        System.err.println("error: " + msg);
        System.exit(1);
    }
}
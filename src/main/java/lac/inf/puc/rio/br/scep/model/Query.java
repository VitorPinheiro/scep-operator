/**
 * 
 */
package lac.inf.puc.rio.br.scep.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import lac.inf.puc.rio.br.scep.utils.Utils;

/**
 * @author vitor
 *
 */
public class Query implements Cloneable
{
	/**
	 * The query itself
	 */
	private String _query;	
	
	/**
	 * The ID os the streams which are input for this query.
	 */
	private ArrayList<String> _inputStreams;
	
	/**
	 * An universal identifier for the query
	 */
	private Integer _queryID;
	
	/**
	 * Pode ser em minutos ou em numero de eventos
	 */
	private WindowType _windowType;
	
	/**
	 * 
	 */
	private Integer _windowSize;
	private Unity _sizeUnity;
	
	private Integer _windowStep;
	private Unity _stepUnity;
	
	private boolean producesTriplesInBlocks;
	
	/**
	 * Esta propriedade é definida como a identificadora de blocos. O cliente que for usar essa funcionalidade de gerar os grafos, precisa
	 * colocar no CONSTRUCT do seu SPARQL que cada grafo RDF produzido é identificado pela pripriedade _blockIdentificator.
	 */
	private static String _blockIdentificator = "sioc:id";
	
	/**
	 * Vai conter o conteudo dentro do CONSTRUCT da query SPARQL.
	 */
	private String _constructContent;
	
	/**
	 * Vai conter o conteudo dentro do WHERE da query SPARQL. 
	 * Essa query é usada para reconstruir a resposta produzida em grafos.
	 */
	private String _whereContent;
	
	private String _queryPREFIX;
	
	public Query()
	{
		producesTriplesInBlocks = false;
		_constructContent = null;
		_whereContent = null;
	}
	
	/**
	 * The static databases that the query needs access.
	 */
	private ArrayList<String> _staticDatabases;
	
	public boolean getProducesTriplesInBlock()
	{
		return producesTriplesInBlocks;
	}
	
	public void setProducesTriplesInBlock(boolean isAnswerInBlock)
	{
		producesTriplesInBlocks = isAnswerInBlock;

		if(isAnswerInBlock) {
			setConstructBlock();
			setWhereBlock(null);
		}
	}
	
	
	public WindowType getWindowType()
	{
		return _windowType;
	}
	
	private void setWindowType(WindowType windowType)
	{
		_windowType = windowType;
	}
	
	public Integer getWindowSize()
	{
		return _windowSize;
	}
	
	private void setWindowSize(Integer windowSize)
	{
		_windowSize = windowSize;
	}
	
	private void setWindowSizeUnity(Unity unity)
	{
		_sizeUnity = unity;
	}
	
	public Unity getWindowSizeUnity()
	{
		return _sizeUnity;
	}
	
	public String getConstructContent()
	{
		return _constructContent;
	}
	
	public void setConstructContent(String constructContent)
	{
		_constructContent = constructContent;		
	}
	
	public String getWhereContent()
	{
		return _whereContent;
	}
	
	public void setWhereContent(String whereContent)
	{
		_whereContent = whereContent;
	}
	
	public void setQueryPREFIX(String queryPREFIX)
	{
		_queryPREFIX = queryPREFIX;
	}
	
	public String getQueryPREFIX()
	{
		return _queryPREFIX;
	}
	
	public Integer getWindowStep()
	{
		return _windowStep;
	}
	
	private void setWindowStep(Integer windowStep)
	{
		_windowStep = windowStep;
	}
	
	private void setWindowStepUnity(Unity unity)
	{
		_stepUnity = unity;
	}
	
	public Unity getWindowStepUnity()
	{
		return _stepUnity;
	}
	
	
	
	public String get_query() {
		return _query;
	}

	/**
	 * Insere a query no formaro CSPARQL. Se o artributo updataMetadata estiver true, ele extrai da query as informações de prefixo e tipo de janela.
	 * @param query
	 * @param updataMetadata
	 */
	public void set_query(String query, Boolean updataMetadata)
	{
		_query = query;
		if(updataMetadata)
		{
			setInfoQuery();
			setPrefixBlock();
		}
	}

	public ArrayList<String> get_inputStreamsIDs() {
		return _inputStreams;
	}

	public void set_inputStreamsIDs(ArrayList<String> inputStreams) {
		_inputStreams = inputStreams;
	}
	
	public void add_inputStreamsByID(String streamID)
	{
		if(_inputStreams == null)
			_inputStreams = new ArrayList<String>();
		
		_inputStreams.add(streamID);
	}

	public String get_inputStreamIDFromAggregator() {
		return "Q"+_queryID+"I";
	}
	
	public String get_outputStreamID() {
		return "Q"+_queryID+"O";
	}

	public Integer get_queryID() {
		return _queryID;
	}

	public void set_queryID(Integer queryID) {
		_queryID = queryID;
	}

	public ArrayList<String> get_staticDatabases() {
		return _staticDatabases;
	}

	public void set_staticDatabases(ArrayList<String> staticDatabases) {
		_staticDatabases = staticDatabases;
	}

	public void add_staticDatabase(String staticDatabaseFolder, String fileName)
	{
		if(Utils.isThisWindows())
		{
			add_staticDatabaseWindows(staticDatabaseFolder, fileName);
		}
		else
		{
			add_staticDatabaseUnix(staticDatabaseFolder, fileName);
		}
	}

	public void add_staticDatabaseWindows(String staticDatabaseFolder, String fileName)
	{
		Path input = Paths.get(staticDatabaseFolder, fileName);

		String rootPath = "";// Thread.currentThread().getContextClassLoader().getResource("").getPath();
		//String appConfigPath = "file://"+rootPath + staticDatabases; // "databases/db.rdf";
		//String appConfigPath = staticDatabases; // "databases/db.rdf";
		
		if(_staticDatabases == null)
		{
			_staticDatabases = new ArrayList<String>();
		}
		_staticDatabases.add(input.toUri().toString());
	}

	
	public void add_staticDatabaseUnix(String staticDatabaseFolder, String fileName)
	{
		//String rootPath = "";// Thread.currentThread().getContextClassLoader().getResource("").getPath();
		//String appConfigPath = "file://"+rootPath + staticDatabaseFolder; // "databases/db.rdf";
		//String appConfigPath = staticDatabases; // "databases/db.rdf";

		Path input = Paths.get(staticDatabaseFolder, fileName);

		if(_staticDatabases == null)
		{
			_staticDatabases = new ArrayList<String>();
		}
		_staticDatabases.add(input.toUri().toString());
	}
	
	private void setWindowRangeSize(String valueWithUnity)
	{
		String timeValue = valueWithUnity.substring(0, valueWithUnity.length()-1);
		
		if(valueWithUnity.contains(Unity.MINUTES.getUnity()))
		{
			setWindowSize(Integer.parseInt(timeValue));
			setWindowSizeUnity(Unity.MINUTES);
		}
		else if(valueWithUnity.contains(Unity.SECONDS.getUnity()))
		{
			setWindowSize(Integer.parseInt(timeValue));
			setWindowSizeUnity(Unity.SECONDS);
		}
		else if(valueWithUnity.contains(Unity.HOURS.getUnity()))
		{
			setWindowSize(Integer.parseInt(timeValue));
			setWindowSizeUnity(Unity.HOURS);
		}
		else if(valueWithUnity.contains(Unity.DAYS.getUnity()))
		{
			setWindowSize(Integer.parseInt(timeValue));
			setWindowSizeUnity(Unity.DAYS);
		}
	}
	
	private void setWindowStepeSize(String valueWithUnity)
	{
		String timeValue = valueWithUnity.substring(0, valueWithUnity.length()-1);

		if(valueWithUnity.contains(Unity.MINUTES.getUnity()))
		{
			setWindowStep(Integer.parseInt(timeValue));
			setWindowStepUnity(Unity.MINUTES);
		}
		else if(valueWithUnity.contains(Unity.SECONDS.getUnity()))
		{
			setWindowStep(Integer.parseInt(timeValue));
			setWindowStepUnity(Unity.SECONDS);
		}
		else if(valueWithUnity.contains(Unity.HOURS.getUnity()))
		{
			setWindowStep(Integer.parseInt(timeValue));
			setWindowStepUnity(Unity.HOURS);
		}
		else if(valueWithUnity.contains(Unity.DAYS.getUnity()))
		{
			setWindowStep(Integer.parseInt(timeValue));
			setWindowStepUnity(Unity.DAYS);
		}
	}
	
	/**
	 * Function that automatically gets the PREFIXs of the query.
	 */
	private void setPrefixBlock()
	{
		int indexBegin = _query.indexOf("PREFIX");
    	
    	if(indexBegin == -1)
    		return;
    	
    	String subString = _query.substring(indexBegin);
    	
    	int[] nums = new int[4];
    	nums[0] = subString.indexOf("SELECT ");
    	nums[1] = subString.indexOf("CONSTRUCT ");
    	nums[2] = subString.indexOf("ASK ");
    	nums[3] = subString.indexOf("DESCRIBE ");   
    	Arrays.sort(nums);
    	
    	String resultPREFIX = "";
    	for (int i = 0; i<nums.length; i++)
    	{
    		if(nums[i] != -1)
    		{
    			resultPREFIX = subString.substring(0, nums[i]);
    			break;
    		}
    	}
    	
    	setQueryPREFIX(resultPREFIX);
	}
	
	/**
	 * Function that automatically gets the WHERE part of the query. 
	 * This will be used to understand the RDF graphs produced by this query. CSPARQL produces the
	 * output in the forms of triples, we need to parse this triples to RDF graphs.
	 */
	private void setWhereBlock(String constructBlock)
	{
		String cb = null;
		if (constructBlock == null)
			cb = _constructContent;
		else
			cb = constructBlock;
		
		String[] statments = cb.split("[.]");
    	
    	for(int i=0; i<statments.length;i++)
    	{
    		statments[i] = statments[i].trim();
    		
    		if(!statments[i].contains(_blockIdentificator))
    			statments[i] = "OPTIONAL{"+statments[i]+"}";
    		else
    			statments[i] = statments[i] + " .";
    	}    	
    	    	
    	String whereContent = String.join("\n", statments);
    	setWhereContent(whereContent);
	}
	
	public String getWhereBlock(String constructblock)
	{
		setWhereBlock(constructblock);
		
    	return _whereContent;
	}
	
	/**
	 * Function that automatically gets the CONSTRUCT part of the query. 
	 * This will be used to understand the RDF graphs produced by this query. CSPARQL produces the
	 * output in the forms of triples, we need to parse this triples to RDF graphs.
	 */
	private void setConstructBlock()
	{
    	int indexBegin = _query.indexOf("CONSTRUCT");
    	
    	if(indexBegin == -1)
    		Utils.error("Query.java: A query de ID ("+_queryID+") não possui o operador CONSTRUCT. Se ela vai gerar RDF graphs, ela precisa ter este operador.");
    	
    	String subString = _query.substring(indexBegin+9);    	
    	int parentesisBegin = subString.indexOf("{");
    	int parentesisEnd = subString.indexOf("}");
    	
    	String constructContent = subString.substring(parentesisBegin+1, parentesisEnd);
    	
    	if( validateConstructContent(constructContent) )
    		setConstructContent(constructContent);
    	else
    		Utils.error("Query.java: A query de ID ("+_queryID+") não possui o operador CONSTRUCT validado. Esta faltando ou tem mais de uma propriedade sioc:id.");
	}
	
	/** 
	 * Verifica se o conteudo da clausula CONSTRUCT está valido. Todo CONSTRUCT produtor de rdf graphs precisa ter exatamente uma tripla
	 * que usa a propriedade sioc:id. Esta propriedade foi escolhida por este sistema para identificar os diferentes rdf graphs.
	 * 
	 * @param constructContent
	 * @return
	 */
	private boolean validateConstructContent(String constructContent)
	{
		int numFristOcc = constructContent.indexOf("sioc:id");
    	int numSecondOcc = constructContent.lastIndexOf("sioc:id");
    	
    	if(numFristOcc == numSecondOcc)
    		return true;
    	else
    		return false;
	}
	
	/**
	 * Function that automatically sets information about the query extracting it from the query text.
	 */
	private void setInfoQuery()
	{
		if(_query == null)
			return;
		
    	int indexBegin = _query.indexOf("[RANGE");    	
    	String subString = _query.substring(indexBegin+7);    	
    	int indexEnd = subString.indexOf("]");    	
    	String infoQuery = subString.substring(0, indexEnd);
    	
    	String[] words = infoQuery.split(" ");

    	if(words[0].equalsIgnoreCase("TRIPLES"))
    	{ // Triples    		
    		setWindowType(WindowType.NUMBER_OF_TRIPLES);
    		setWindowSize(Integer.parseInt(words[1]));
    		setWindowSizeUnity(null); // do not have UNITY    		
    	}
    	else if(words[1].equalsIgnoreCase("TUMBLING"))
    	{ // TIME FIXED
    		setWindowType(WindowType.FIXED_TIME);    		
    		setWindowRangeSize(words[0]);    		
    	}
    	else if(words[1].equalsIgnoreCase("STEP"))
    	{ // SLIDING
    		setWindowType(WindowType.SLIDING_TIME);
    		setWindowRangeSize(words[0]);  
    		setWindowStepeSize(words[2]); 
    	}
	}
	
	public void printInfo()
	{
		System.out.println("Window Type = "+_windowType);
		System.out.println("Window Range size = "+_windowSize);
		if(_windowType != WindowType.NUMBER_OF_TRIPLES)
			System.out.println("Window Range Unity = "+_sizeUnity);
		if(_windowType == WindowType.SLIDING_TIME)
		{
			System.out.println("Window Step size = "+ _windowStep);
			System.out.println("Window Step unity = "+_stepUnity);
		}
		
	}

	public Query getClone() {
		try {
			// call clone in Object.
			return (Query) super.clone();
		} catch (CloneNotSupportedException e) {
			System.out.println ("Query: Cloning not allowed. " );
			return this;
		}
	}
}
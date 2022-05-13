package lac.inf.puc.rio.br.scep;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import lac.inf.puc.rio.br.scep.database.IQueryDatabase;
import lac.inf.puc.rio.br.scep.database.QueryDatabase;
import lac.inf.puc.rio.br.scep.model.Query;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
        
        //readPrefixPartOfSPARQL();
        //generateWherePartOfQueryToConstructRDFgraphs();
        //readConstructPartOfSPARQL();
        getStreamIriFromQuery();
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        assertTrue( true );
    }
    
    public void readPrefixPartOfSPARQL()
    {
    	IQueryDatabase _queryDb;
    	_queryDb = QueryDatabase.getInstance();
		//_queryDb = new QueryDatabase();
    	
    	Query query = _queryDb.getQuery(1);
    	
    	
    	String queryTxt = query.get_query();	
    	int indexBegin = queryTxt.indexOf("PREFIX");
    	
    	if(indexBegin == -1)
    		return;
    	
    	String subString = queryTxt.substring(indexBegin);
    	
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
    	
    	if(resultPREFIX.equals(""))
    		System.out.println("VAZIO");
    	else
    		System.out.println("resultPREFIX = "+resultPREFIX);
    }
    
    public void getStreamIriFromQuery()
    {
    	IQueryDatabase _queryDb;
    	_queryDb = QueryDatabase.getInstance();
		//_queryDb = new QueryDatabase();
    	Query query = _queryDb.getQuery(1);
    	
    	String queryText = query.get_query();
    	int indexBegin = queryText.indexOf("STREAM");
    	
    	if(indexBegin == -1)
    	{
    		System.out.println("Essa query nao tem STREAM");
    		return;
    	}
    	
    	String subString = queryText.substring(indexBegin+6);
    	System.out.println("subString = "+subString);
    	
    	int parentesisBegin = subString.indexOf("<");
    	int parentesisEnd = subString.indexOf(">");
    	
    	String streamIri = subString.substring(parentesisBegin+1, parentesisEnd-1);
    	System.out.println("streamIri = "+streamIri);
    }
    
    public void generateWherePartOfQueryToConstructRDFgraphs()
    {
    	IQueryDatabase _queryDb;
    	_queryDb = QueryDatabase.getInstance();
		//_queryDb = new QueryDatabase();
    	
    	Query query = _queryDb.getQuery(1);
    	String constructPart = query.getConstructContent();
    	
    	System.out.println("constructPart = "+constructPart);
    	
    	String[] statments = constructPart.split("[.]");
    	
    	System.out.println("----------- statments.len = "+statments.length);
    	for(int i=0; i<statments.length;i++)
    	{
    		System.out.println(statments[i].trim());
    		statments[i] = statments[i].trim();
    		if(!statments[i].contains("sioc:id"))
    			statments[i] = "OPTIONAL{"+statments[i]+"}";
    		else
    			statments[i] = statments[i] + " .";
    		//System.out.println(statments[i]);
    	}
    	System.out.println("-----------");
    	    	
    	String whereContent = String.join("\n", statments);
    	
    	System.out.println(whereContent);
    	
    	//String result = "OPTIONAL { "+constructPart+" }";
    	
    	//System.out.println("result = "+result);
    }
    
    public void readConstructPartOfSPARQL()
    {
    	IQueryDatabase _queryDb;
    	_queryDb = QueryDatabase.getInstance();
		//_queryDb = new QueryDatabase();
    	
    	Query query = _queryDb.getQuery(1);
    	
    	
    	String queryTxt = query.get_query();	
    	int indexBegin = queryTxt.indexOf("CONSTRUCT");
    	
    	if(indexBegin == -1)
    	{
    		System.out.println("Essa query nao tem CONSTRUCT");
    		return;
    	}
    	
    	String subString = queryTxt.substring(indexBegin+9);
    	System.out.println("subString = "+subString);
    	
    	int parentesisBegin = subString.indexOf("{");
    	int parentesisEnd = subString.indexOf("}");
    	
    	String constructContent = subString.substring(parentesisBegin+1, parentesisEnd);
    	System.out.println("constructContent = "+constructContent);
    	
    	int numFristOcc = constructContent.indexOf("sioc:id");
    	int numSecondOcc = constructContent.lastIndexOf("sioc:id");
    	
    	if(numFristOcc == numSecondOcc)
    		System.out.println("CONSTRUCT query validated. "+numFristOcc);
    	else
    		System.out.println("CONSTRUCT query not validated. "+numFristOcc+ " / "+numSecondOcc);
    	
    	// Substituir o siod:id por uma string que eu queira    	
    	String newContent = constructContent.replaceFirst("(sioc:id)(\\s*)((\\S+)[^.])", "sioc:id 1234 .");
    	String newContent2 = newContent.replaceFirst("[.][.]", ".");
    	
    	System.out.println("newContent = "+newContent2);
    	
    	
    }
}

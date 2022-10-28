/**
 * 
 */
package lac.inf.puc.rio.br.scep.database;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import lac.inf.puc.rio.br.scep.model.Query;
import lac.inf.puc.rio.br.scep.utils.Utils;

/**
 * @author vitor
 *
 */
public class QueryDatabase implements IQueryDatabase
{
	private final ArrayList<Query> _queries;
	private Utils _utils;
	
	private static QueryDatabase _instance;

	public static QueryDatabase getInstance()
	{
		if(_instance == null)
		{
			_instance = new QueryDatabase();
		}
		
		return _instance;
	}
	
	private QueryDatabase()
	{
		_queries = new ArrayList<Query>();

		
		Query query = new Query();

		/*
		query.set_query(readFileAsString("examples/attendingDoctors.rq"));
		query.add_inputStreamsByID("IS1"); // The associated aggregator uses it to create its consumer.
		query.set_queryID(1);
		query.add_staticDatabase("/Users/vitor/git-repository/KAFKA/scep-operator/examples/consultation.rdf");
//		query.add_staticDatabase("http://dbpedia.org/data/Wendy_Savage.rdf"); // Ja preciso ter todos os dbs que vou usar adicionados na query
		query.setProducesTriplesInBlock(false);
		_queries.add(query);
		*/


		query = new Query();
		query.set_query(readFileAsString("examples/checkDangerousBusTest.rq"), true);
		//query.set_query(readFileAsString("examples/checkDangerousBusTestCS.rq"), true); // CSparql
		query.add_inputStreamsByID("IS1"); // The associated aggregator uses it to create its consumer.
		query.set_queryID(12);
		//query.set_queryToSelectMsgID("select distinct ?msgID WHERE { ?tweet sioc:id ?msgID  } ");

		query.add_staticDatabase("examples/", "beawaros.rdf");
		//query.add_staticDatabase("/opt/scep/scep-operator/target/classes/databases/MusicalArtists.rdf");
		query.setProducesTriplesInBlock(true);
		query.setIsDebugModeOn(false);
		_queries.add(query);

	}
	
	private String readFileAsString(String path) {
	    try {
	      byte[] encoded = Files.readAllBytes(Paths.get(path, new String[0]));
	      return new String(encoded, StandardCharsets.UTF_8);
	    } catch (IOException e) {
	      _utils.error(String.format("cannot access '%s'", new Object[] { path }));
	      
	      throw new AssertionError(); }
	  }

	public Query getQuery(int queryID) 
	{
		for(int i=0;i<_queries.size();i++)
		{
			if(_queries.get(i).get_queryID() == queryID)
				return _queries.get(i);
		}
		
		return null;
	}
	
	public ArrayList<Query> getAllQueries()
	{
		return _queries;
	}
	
	public void addQuery(Query query)
	{
		
	}
}

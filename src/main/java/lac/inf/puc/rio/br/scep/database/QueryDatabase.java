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

		/*
		query = new Query();
		query.set_query(readFileAsString("examples/attendingDoctorsQuery.rq"));
		query.add_inputStreamsByID("Q1O"); // The associated aggregator uses it to create its consumer.
		query.set_queryID(2);
		query.add_staticDatabase("file:///Users/vitor/git-repository/KAFKA/scep-operator/examples/consultation.rdf");
		query.setProducesTriplesInBlock(false);
		_queries.add(query);

		query = new Query();
		query.set_query(readFileAsString("examples/EstimateProfit.rq"));
		query.add_inputStreamsByID("Q1O"); // The associated aggregator uses it to create its consumer.
		query.set_queryID(3);
		query.add_staticDatabase("file:///Users/vitor/git-repository/KAFKA/scep-operator/examples/consultation.rdf");
		query.setProducesTriplesInBlock(false);
		_queries.add(query);

		query = new Query();
		query.set_query(readFileAsString("examples/GenerateReport.rq"));
		query.add_inputStreamsByID("Q2O"); // The associated aggregator uses it to create its consumer.
		query.add_inputStreamsByID("Q3O"); // The associated aggregator uses it to create its consumer.
		query.set_queryID(4);
		query.add_staticDatabase("file:///Users/vitor/git-repository/KAFKA/scep-operator/examples/consultation.rdf");
		query.setProducesTriplesInBlock(false);
		_queries.add(query);

		query = new Query();
		query.set_query(readFileAsString("examples/showAllTriples.rq"));
		query.add_inputStreamsByID("IS1"); // The associated aggregator uses it to create its consumer.
		query.set_queryID(5);
		query.setProducesTriplesInBlock(false);
		_queries.add(query);

		query = new Query();
		query.set_query(readFileAsString("examples/testQuery.rq"));
		query.add_inputStreamsByID("IS1"); // The associated aggregator uses it to create its consumer.
		query.set_queryID(6);
		query.setProducesTriplesInBlock(false);
		_queries.add(query);*/

		/*
		query = new Query();
		query.set_query(readFileAsString("examples/getPeopleCountry.rq"));
		query.add_inputStreamsByID("IS1"); // The associated aggregator uses it to create its consumer.
		query.set_queryID(1); // antiga 7
		//query.add_staticDatabase("/Users/vitor/git-repository/KAFKA/scep-operator/examples/MusicalArtists.rdf");
		query.add_staticDatabase("/opt/scep/scep-operator/target/classes/databases/MusicalArtists.rdf");
		query.setProducesTriplesInBlock(true);
		_queries.add(query);
		*/
/*		query = new Query();
		query.set_query(readFileAsString("examples/getPeopleCountryCS.rq"));
		query.add_inputStreamsByID("IS1"); // The associated aggregator uses it to create its consumer.
		query.set_queryID(1); // antiga 7
		//query.add_staticDatabase("/Users/vitor/git-repository/KAFKA/scep-operator/examples/MusicalArtists.rdf");
		query.add_staticDatabase("/opt/scep/scep-operator/target/classes/databases/MusicalArtists.rdf");
		query.setProducesTriplesInBlock(true);
		_queries.add(query);	*/

/*		query = new Query();
		query.set_query(readFileAsString("examples/getPeople.rq"));
		query.add_inputStreamsByID("IS1"); // The associated aggregator uses it to create its consumer.
		query.set_queryID(1); // antiga 7
		//query.add_staticDatabase("/Users/vitor/git-repository/KAFKA/scep-operator/examples/MusicalArtists.rdf");
		query.add_staticDatabase("/opt/scep/scep-operator/target/classes/databases/MusicalArtists.rdf");
		query.setProducesTriplesInBlock(true);
		_queries.add(query);*/

/*		query = new Query();
		query.set_query(readFileAsString("examples/getPeopleCS.rq"));
		query.add_inputStreamsByID("IS1"); // The associated aggregator uses it to create its consumer.
		query.set_queryID(1); // antiga 7
		//query.add_staticDatabase("/Users/vitor/git-repository/KAFKA/scep-operator/examples/MusicalArtists.rdf");
		query.add_staticDatabase("/opt/scep/scep-operator/target/classes/databases/MusicalArtists.rdf");
		query.setProducesTriplesInBlock(true);
		_queries.add(query);*/


		query = new Query();
		query.set_query(readFileAsString("examples/checkDangerousBusTest.rq"), true);
		query.add_inputStreamsByID("IS1"); // The associated aggregator uses it to create its consumer.
		query.set_queryID(12);
		//query.set_queryToSelectMsgID("select distinct ?msgID WHERE { ?tweet sioc:id ?msgID  } ");

		query.add_staticDatabase("/Users/vitor/git-repository/DSCEP-github/scep-operator/examples/", "beawaros.rdf");
		//query.add_staticDatabase("/opt/scep/scep-operator/target/classes/databases/MusicalArtists.rdf");
		query.setProducesTriplesInBlock(true);
		_queries.add(query);


		/*
		query = new Query();
		query.set_query(readFileAsString("examples/getMusicalArtistsOnly.rq"), true);
		query.add_inputStreamsByID("IS1"); // The associated aggregator uses it to create its consumer.
		query.set_queryID(1); // antiga 7	
		query.add_staticDatabase("/Users/vitor/git-repository/DSCEP/scep-operator/examples/", "MusicalArtists.rdf");
		//query.add_staticDatabase("/opt/scep/scep-operator/target/classes/databases/MusicalArtists.rdf");		
		query.setProducesTriplesInBlock(true);
		_queries.add(query);*/
/*
		query = new Query();
		query.set_query(readFileAsString("examples/getTelevisionShows.rq"), true);
		query.add_inputStreamsByID("IS1"); // The associated aggregator uses it to create its consumer.
		query.set_queryID(2); // antiga 10
		//query.add_staticDatabase("file:///Users/vitor/git-repository/DSCEP/scep-operator/examples/TVShowsFiltered.rdf");
		//query.add_staticDatabase("/opt/scep/scep-operator/target/classes/databases/TVShows.rdf");
		query.add_staticDatabase("/Users/vitor/git-repository/DSCEP/scep-operator/examples/TVShows.rdf");
		query.setProducesTriplesInBlock(true);
		_queries.add(query);


		query = new Query();
		query.set_query(readFileAsString("examples/getMusicalArtistsOnlySPARQL.rq"), true);
		query.add_inputStreamsByID("IS1"); // The associated aggregator uses it to create its consumer.
		query.set_queryID(10); // antiga 7
		query.add_staticDatabase("/Users/vitor/git-repository/DSCEP/scep-operator/examples/MusicalArtists.rdf");
		//query.add_staticDatabase("/opt/scep/scep-operator/target/classes/databases/MusicalArtists.rdf");
		query.setProducesTriplesInBlock(true);
		_queries.add(query);

		query = new Query();
		query.set_query(readFileAsString("examples/getMusicalArtistsOnlySimplified.rq"), true);
		query.add_inputStreamsByID("IS1"); // The associated aggregator uses it to create its consumer.
		query.set_queryID(12); // antiga 7
		query.add_staticDatabase("/Users/vitor/git-repository/DSCEP/scep-operator/examples/MusicalArtists.rdf");
		//query.add_staticDatabase("/opt/scep/scep-operator/target/classes/databases/MusicalArtists.rdf");
		query.setProducesTriplesInBlock(true);
		_queries.add(query);

		query = new Query();
		query.set_query(readFileAsString("examples/getMusicalArtistsOnlySimplified2.rq"), true);
		query.add_inputStreamsByID("IS1"); // The associated aggregator uses it to create its consumer.
		query.set_queryID(13); // antiga 7
		query.add_staticDatabase("/Users/vitor/git-repository/DSCEP/scep-operator/examples/MusicalArtists.rdf");
		//query.add_staticDatabase("/opt/scep/scep-operator/target/classes/databases/MusicalArtists.rdf");
		query.setProducesTriplesInBlock(true);
		_queries.add(query);
*/
		/*query = new Query();
		query.set_query(readFileAsString("examples/getMusicalArtistsOnlySimplified2.rq"), true);
		query.add_inputStreamsByID("IS1"); // The associated aggregator uses it to create its consumer.
		query.set_queryID(14); // antiga 7
		query.add_staticDatabase("/Users/vitor/git-repository/DSCEP/scep-operator/examples/", "MusicalArtists.rdf");
		//query.add_staticDatabase("E:\\gitprojects\\Tese\\scep-operator\\examples", "MusicalArtists.rdf");
		//query.add_staticDatabase("/opt/scep/scep-operator/target/classes/databases/MusicalArtists.rdf");
		query.setProducesTriplesInBlock(true);
		_queries.add(query);*/

		/*query = new Query();
		query.set_query(readFileAsString("examples/getTweetSubject.rq"));
		query.add_inputStreamsByID("IS1"); // The associated aggregator uses it to create its consumer.
		query.set_queryID(1);		
		query.setProducesTriplesInBlock(true);
		_queries.add(query);*/
		
		/*		
		query = new Query();
		query.set_query(readFileAsString("examples/getSentimentArtists.rq"));
		query.add_inputStreamsByID("Q1O"); // The associated aggregator uses it to create its consumer.
		query.set_queryID(2); // antiga 8		
		query.setProducesTriplesInBlock(false);
		_queries.add(query);
		
		query = new Query();
		query.set_query(readFileAsString("examples/getLikesShares.rq"));
		query.add_inputStreamsByID("Q1O"); // The associated aggregator uses it to create its consumer.
		query.set_queryID(3); // antiga 9		
		query.setProducesTriplesInBlock(false);
		_queries.add(query);
		
		query = new Query();
		query.set_query(readFileAsString("examples/getTelevisionShows.rq"));
		query.add_inputStreamsByID("IS1"); // The associated aggregator uses it to create its consumer.
		query.set_queryID(2); // antiga 10
		//query.add_staticDatabase("file:///Users/vitor/git-repository/DSCEP/scep-operator/examples/TVShowsFiltered.rdf");
		//query.add_staticDatabase("/opt/scep/scep-operator/target/classes/databases/TVShows.rdf");
		query.add_staticDatabase("file:///Users/vitor/git-repository/DSCEP/scep-operator/examples/TVShows.rdf");
		query.setProducesTriplesInBlock(true);
		_queries.add(query);
		
		query = new Query();
		query.set_query(readFileAsString("examples/getSentimentTvShows.rq"));
		query.add_inputStreamsByID("Q4O"); // The associated aggregator uses it to create its consumer.
		query.set_queryID(5); 		
		query.setProducesTriplesInBlock(false);
		_queries.add(query);
		
		query = new Query();
		query.set_query(readFileAsString("examples/getLikesSharesTV.rq"));
		query.add_inputStreamsByID("Q4O"); // The associated aggregator uses it to create its consumer.
		query.set_queryID(6); 	
		query.setProducesTriplesInBlock(false);
		_queries.add(query);
		*/		
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

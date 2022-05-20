/**
 * 
 */
package lac.inf.puc.rio.br.scep.utils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author vitor
 *
 */
public class WriterToFile 
{
	private String _filePath;
	private String _speedFileName;
	private String _jsonFileName;
		
	private String _speedFile = "Speed.txt";	
	private String _jsonFile = "jsonOut.json";

	private String _tweetsFile = "Tweets.txt";
	
	public WriterToFile(String callerName)
	{
		_filePath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
		_speedFileName = callerName+_speedFile;
		_jsonFileName = callerName;
		initialize();
	}
	
	private void initialize()
	{
		// Delete speed file
		File speedFile = new File(_filePath+_speedFileName);         
        speedFile.delete();
        
        // Delete jsonOut files        
        File directory = new File(_filePath);
        File[] files = directory.listFiles();
        for (File f : files)
        {
            if (f.getName().endsWith(_jsonFile))
              f.delete();
        }
	}
	
	public void writeToSpeedFileNewLine(String time, int executionNum, int numTriplesIn, int numTriplesOut)
	{
		//System.out.println("Writing to this file = "+_filePath+_speedFileName);
		PrintWriter out = null;
		try
		{
			out = new PrintWriter(new BufferedWriter(new FileWriter(_filePath+_speedFileName, true)));
		    out.println("Execution "+executionNum+": "+time+ " "+numTriplesIn+ " "+numTriplesOut);
		} 
		catch (IOException e) 
		{
			 System.err.println(e);
		}
		finally 
		{
			if (out != null) {
		        out.close();
		    }
		}
	}
	
	public void writeToJsonOutFile(String json, int executionNum)
	{
		String fileName = _jsonFileName+"_ex_"+executionNum+_jsonFile;
		System.out.println("Writing to this file = "+_filePath+fileName);
		PrintWriter out = null;
		try
		{
			out = new PrintWriter(new BufferedWriter(new FileWriter(_filePath+fileName, true)));
		    out.print(json);
		} 
		catch (IOException e) 
		{
			 System.err.println(e);
		}
		finally 
		{
			if (out != null) {
		        out.close();
		    }
		}
	}



	/**
	 * Each json msg has the size delimited on the TweetStreamGenerator.
	 * @param fromMsg
	 * @param numberOfMsgs
	 * @return
	 */
	public List<String> readJsonMsgsFromFile(int fromMsg, int numberOfMsgs)
	{
		// We need to provide file path as the parameter:
		// double backquote is to avoid compiler interpret words
		// like \test as \t (ie. as a escape sequence)
		int msgsRead = 0;
		int lastIndexToRead = numberOfMsgs + fromMsg;

		File file = new File(_filePath+_tweetsFile);

		List<String> ret = new ArrayList<String>();

		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(file));

			String st;
			while ((st = br.readLine()) != null)
			{

				if(msgsRead >= fromMsg)
				{
					//System.out.println(st);
					ret.add(st);
				}

				msgsRead++;

				if(msgsRead >= lastIndexToRead)
					break;
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("(WriterToFile.readJsonMsgsFromFile) Numero de json msgs = "+ret.size());
		if(ret.size() == 0)
			return null;

		return ret;
	}
}

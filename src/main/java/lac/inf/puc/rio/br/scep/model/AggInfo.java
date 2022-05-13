package lac.inf.puc.rio.br.scep.model;

import java.util.ArrayList;
import java.util.HashMap;

public class AggInfo
{
    // QueryID : [queryInputTopic, queryOutputTopic]
    private ArrayList<QueryInfo> _queryInfos;

    public ArrayList<QueryInfo> getQueryInfos()
    {
        return _queryInfos;
    }

    /**
     * Retorna todos os topics que o aggregator deve se inscrever.
     * @return
     */
    public ArrayList<String> getInputStreamsIDs()
    {
        ArrayList<String> ret = new ArrayList<>();

        for(int i=0;i<_queryInfos.size();i++)
        {
            for(int j=0;j<_queryInfos.get(i).get_inputStreams().size();j++) {
                if (!ret.contains(_queryInfos.get(i).get_inputStreams().get(j))) {
                    ret.add(_queryInfos.get(i).get_inputStreams().get(j));
                }
            }
        }

        return ret;
    }

    public void addQueryInfo(QueryInfo queryInfo)
    {
        if(_queryInfos == null)
        {
            _queryInfos = new ArrayList<QueryInfo>();
        }

        _queryInfos.add(queryInfo);
    }
}

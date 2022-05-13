package lac.inf.puc.rio.br.scep.model;

import java.util.ArrayList;

public class QueryInfo
{
    private Integer _queryID;
    private String _queryInputTopic;
    private String _queryOutputTopic;
    private ArrayList<String> _inputStreams;

    private WindowType _windowType;
    private Integer _windowSize;
    private Unity _sizeUnity;

    private Integer _windowStep;
    private Unity _stepUnity;

    public Integer get_queryID() {
        return _queryID;
    }

    public void set_queryID(Integer queryID) {
        _queryID = queryID;
    }

    public String get_queryInputTopic() {
        return _queryInputTopic;
    }

    public void set_queryInputTopic(String queryInputTopic) {
        _queryInputTopic = queryInputTopic;
    }

    public ArrayList<String> get_inputStreams() {
        return _inputStreams;
    }

    public void set_inputStreams(ArrayList<String> inputStreams) {
        _inputStreams = inputStreams;
    }

    public String get_queryOutputTopic() {
        return _queryOutputTopic;
    }

    public void set_queryOutputTopic(String queryOutputTopic) {
        _queryOutputTopic = queryOutputTopic;
    }

    public WindowType getWindowType() {
        return _windowType;
    }

    private void setWindowType(WindowType type)
    {
        _windowType = type;
    }

    private void setWindowSize(Integer windowSize)
    {
        _windowSize = windowSize;
    }

    public Integer getWindowSize()
    {
        return _windowSize;
    }

    private void setWindowSizeUnity(Unity unity)
    {
        _sizeUnity = unity;
    }

    private void setWindowStep(Integer windowStep)
    {
        _windowStep = windowStep;
    }

    private void setWindowStepUnity(Unity unity)
    {
        _stepUnity = unity;
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
     * Function that automatically sets information about the query extracting it from the query text.
     */
    public void setQueryWindowInfo(String windowInfo)
    {
        if(windowInfo == null)
            return;

        int indexBegin = windowInfo.indexOf("[RANGE");
        String subString = windowInfo.substring(indexBegin+7);
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
}

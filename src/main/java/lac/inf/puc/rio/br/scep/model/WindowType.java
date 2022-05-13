/**
 * 
 */
package lac.inf.puc.rio.br.scep.model;

/**
 * @author vitor
 *
 */
public enum WindowType 
{
	NUMBER_OF_TRIPLES("count_window"),
	FIXED_TIME("fixed_time_window"),
	SLIDING_TIME("sliding_window");

	private String _value;

	WindowType(String windowTypeValue){
		_value = windowTypeValue;
	}
	public String getValor(){
		return _value;
	}
	public void setValor(String value) { _value = value; }

	public static WindowType getWindowTypeFromValue(String value)
	{
		if(value.equals("count_window"))
			return NUMBER_OF_TRIPLES;
		else if(value.equals("fixed_time_window"))
			return FIXED_TIME;
		else if(value.equals("sliding_window"))
			return SLIDING_TIME;

		return null;
	}
}

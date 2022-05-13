/**
 * 
 */
package lac.inf.puc.rio.br.scep.model;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import eu.larkc.csparql.cep.api.RdfQuadruple;

/**
 * @author vitor
 * 
 * Essa classe representa um pedaço da stream. Ela é o formato JSON da stream que corre pelo SDDL e MobileHubs.
 * Esta em forma de classe para ficar de acordo com o padrão orientado a objeto e para conter mais informações como o numero
 * total de triplas e o numero total de minutos contidos nessa snip.
 * 
 * A ideia é que se possa juntar mais de um snip neste objeto, quando se unir os snips aumentam o numero de triplas e/ou o numero de minutos
 * contidos na snip. Isso é útil na hora de dedicir se enviamos essa snip para o operator ou não. 
 * 
 * Vai servir também para mander os eventos ordenados por timestamp.
 * 
 * O Stream Snip pode ter dados correspondendes de uma ou mais queries.
 *
 */
public class StreamSnip 
{	
	private List<RdfQuadruple> _triples;
	
	public StreamSnip()
	{
		_triples = Collections.synchronizedList(new ArrayList<RdfQuadruple>());
	}
	
	public void addTriples(List<RdfQuadruple> triples)
	{
		synchronized(_triples)
		{
			_triples.addAll(triples);		
			Collections.sort(_triples, Comparator.comparing(RdfQuadruple::getTimestamp));
		}
	}
	
	public void addTriple(RdfQuadruple triple)
	{
		synchronized(_triples)
		{
			_triples.add(triple);			
			Collections.sort(_triples, Comparator.comparing(RdfQuadruple::getTimestamp));
		}
	}
	
	
	public Integer getNumberOfTriples()
	{
		return _triples.size();
	}
	
	public List<RdfQuadruple> getTriples()
	{
		return _triples;
	}
	
	/**
	 * Esse metodo retorna os primeiros numOfTriples e os deleta da lista.
	 * @param numOfTriples
	 * @return
	 */
	public List<RdfQuadruple> getTriples(int numOfTriples)
	{
		List<RdfQuadruple> retTriples = new ArrayList<RdfQuadruple>();
		
		Iterator<RdfQuadruple> iterator = _triples.iterator();
		int count = 0;
		
		while(iterator.hasNext())
		{
			retTriples.add(iterator.next());
			count++;
			
			if(count == numOfTriples)
				break;
		}
		
		_triples.removeAll(retTriples);
		
		return retTriples;
	}
	
	/**
	 * Quando eu for inserir triplas eu posso ver se ja tenho uma igual a aquela, se tiver eu nao insiro.
	 * Pra ser igual o timestamp pode ser até 100 millesimos de diferença.
	 * 
	 * Que ai assim que uma query produzir resultados de muitas janelas que tem overlap, eu só envio a janela pro
	 * StreamRouting e todas as queries que quiserem essa janela vao receber no seu snip. E em todos os snips vai ter a 
	 * deduplicacao garantindo (ou quase) que nao tenha dado duplicado.
	 * 
	 * Um snip é montado em blocos, de acordo com o que é recebido pelo input. cada bloco é resultado de uma query diferente. 
	 * É importante que sempre que adicionar triplas em um snip elas sejam ordenadas por timestamp. Que ai na hora de
	 * pegar uma janela do snip ela ja vai ta corretamente ordenada e com os inputs misturados.
	 * @param snip
	 */
	public void addSnip(StreamSnip snip)
	{		
		addTriples(snip.getTriples());
	}
	
	public void clearSnip()
	{
		synchronized(_triples)
		{
			_triples.clear();
		}
	}
	
	/**
	 * Return the window size in milliseconds.
	 * @return
	 */
	public Long getWindowTimeSize()
	{
		int lastIndex;
		Long windowSize;
		
		synchronized(_triples)
		{
			lastIndex = _triples.size() -1;
			windowSize = _triples.get(lastIndex).getTimestamp() - _triples.get(0).getTimestamp();
		}
		
		return windowSize;
	}
}

package org.msrg.publiy.utils;

public class LimittedSizeList<T extends ListElement> {

	private final T[] _elements;
	private final int _size;
	private int _count;
	
	public LimittedSizeList(T[] elements){
		_size = elements.length;
		_elements = elements;
	}
	
	public void append(T t){
		synchronized (_elements){
			if ( t == null )
				throw new NullPointerException();
			
			_elements[_count++%_size] = t;
		}
	}
	
	public T getElement(int i){
		synchronized (_elements){
			return _elements[i];
		}
	}
	
	public T getLastElement(){
		synchronized(_elements){
			if ( _count == 0 )
				return null;
			
			else
				return _elements[(_count-1)%_size];
		}
	}
	
	public int getSize(){
		return _size;
	}
	
	public int getCount(){
		return _count;
	}
	
	public ElementTotal<LimittedSizeList<T>> getElementsTotalButLast(int j){
		synchronized (_elements){
			return getElementsTotalPrivately(j, false);
		}
	}
	
	public ElementTotal<LimittedSizeList<T>> getElementsTotal(int j){
		synchronized (_elements){
			return getElementsTotalPrivately(j, true);
		}
	}
	
	private ElementTotal<LimittedSizeList<T>> getElementsTotalPrivately(int j, boolean includeLast){
		double sum = 0;
		int i=0;
		int k=0;
		long latest=-1, earliest=-1;
		for ( ; i<_size && i<_count ; i++ ){
			if ( !includeLast )
				if ( i == (_count-1)%_size )
					continue;
			
			k++;
			ListElement element = _elements[i];
			long elementTime = element.getElementTime();
			if ( elementTime > latest )
				latest = elementTime;
			if ( elementTime < earliest )
				earliest = elementTime;
			
			sum += element.getValue(j);
		}
		
		if ( k==0 )
			return null;
		
		ElementTotal<LimittedSizeList<T>> tot = new ElementTotal<LimittedSizeList<T>>(this, earliest, latest, sum, k);
		return tot;
	}

	public String toString(){
		String str = "";

		synchronized(_elements){
			for ( int i=1 ; i<=_size && i<=_count ; i++ )
				str = _elements[(_count-i)%_size].toString() + ((i==1)?"":",") + str;
		}
		
		return "{" + str + "}";
	}
	
	public String toStringShort(){
		String str = "";
		if ( _count > 0 ){
			int valuesCount = _elements[0].getValuesCount();
			for ( int i=0; i<valuesCount ; i++ )
				str += getElementsTotal(i) + ((i==valuesCount-1)?"":",");
		}
		
		return "[" + str + "]";
	}

	public String toStringLong() {
		return toStringShort() + toString();
	}
	
}

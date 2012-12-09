package org.msrg.publiy.publishSubscribe;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.msrg.publiy.utils.SystemTime;

public class Publication implements Serializable {
	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = 1431408801428980990L;
	public HashMap<String, List<Integer>> _predicates;
	public HashMap<String, List<String>> _stringPredicates;
	public long _serialID;
	public boolean _noSourceRoute = false;
	
	public Publication () {
		_predicates = new HashMap<String, List<Integer>>();
		_stringPredicates = new HashMap<String, List<String>>();
		_serialID = SystemTime.currentTimeMillis();
	}
	
	public Publication addStringPredicate(String att, String value) {
		List<String> values = _stringPredicates.get(att);
		if(values == null) {
			values = new LinkedList<String>();
			_stringPredicates.put(att, values);
		}
		
		values.add(value);
		return this;
	}
	
	public Publication addPredicate(String att, int value) {
		List<Integer> values = _predicates.get(att);
		if(values == null) {
			values = new LinkedList<Integer>();
			_predicates.put(att, values);
		}
		
		values.add(value);
		
		return this;
	}
	
	public List<String> getStringValue(String attr) {
		return _stringPredicates.get(attr);
	}
	
	public List<Integer> getValue(String att) {
		return _predicates.get(att);
	}
	
	@Override
	public String toString() {
		return "Pub("+	": " + _predicates + _stringPredicates + ")";
	}
	
	public String encode() {
		return encode(this);
	}
	
	public static String encode(Publication publication) {
		if(publication == null)
			return "NULL";
		
		StringWriter writer = new StringWriter();
		
		SortedSet<String> sortedAttributeSet = new TreeSet<String>(publication._predicates.keySet());
		for(String attr : sortedAttributeSet) {
			List<Integer> values = publication._predicates.get(attr);
			for(Integer value : values)
				writer.append("\t" + attr + "\t" + value);
		}

		SortedSet<String> sortedStringAttributeSet = new TreeSet<String>(publication._stringPredicates.keySet());
		for(String attr : sortedStringAttributeSet) {
			List<String> values = publication._stringPredicates.get(attr);
			for(String value : values)
				writer.append("\t" + attr + "\t" + value);
		}
		
		return writer.toString();
	}
	
	public static Publication decode(String str) {
		if(str.equals("NULL"))
			return null;
		
		StringTokenizer strT = new StringTokenizer(str);
		Publication publication = new Publication();
		while(strT.hasMoreTokens()) {
			String attr = strT.nextToken();
			String value = strT.nextToken();
			try{
				int intValue = new Integer(value).intValue();
				publication.addPredicate(attr, intValue);
			} catch (Exception ex) {
				publication.addStringPredicate(attr, value);
			}
		}
		return publication;
	}
	
	public Publication getClone() {
		Publication newPublicationt = new Publication();
		return duplicate(newPublicationt);
	}
	
	protected Publication duplicate(Publication publication) {
		publication._predicates = new HashMap<String, List<Integer>>();
		publication._serialID = _serialID;
		publication._noSourceRoute = _noSourceRoute;
		for(Entry<String, List<Integer>> entries : _predicates.entrySet()) {
			String attribute = entries.getKey();
			List<Integer> values = new LinkedList<Integer>(entries.getValue());
			publication._predicates.put(attribute, values);
		}
		for(Entry<String, List<String>> entries : _stringPredicates.entrySet()) {
			String attribute = entries.getKey();
			List<String> values = new LinkedList<String>(entries.getValue());
			publication._stringPredicates.put(attribute, values);
		}
		return publication;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		
		if(!this.getClass().isAssignableFrom(obj.getClass()))
			return false;
		
		Publication pubObj = (Publication) obj;
		return pubObj._predicates.equals(_predicates) &&
			pubObj._stringPredicates.equals(_stringPredicates);
	}

	public List<Integer> getPredicate(String attr) {
		return _predicates.get(attr);
	}
	
	public List<String> getStringPredicate(String attr) {
		return _stringPredicates.get(attr);
	}

	public HashMap<String, List<String>> getStringPredicates() {
		return _stringPredicates;
	}
	
	public HashMap<String, List<Integer>> getPredicates() {
		return _predicates;
	}
}
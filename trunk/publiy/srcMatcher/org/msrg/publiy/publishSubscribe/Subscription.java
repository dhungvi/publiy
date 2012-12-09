package org.msrg.publiy.publishSubscribe;

import java.io.BufferedReader;

import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.msrg.publiy.publishSubscribe.matching.MatchingEngine;
import org.msrg.publiy.publishSubscribe.matching.MatchingEngineImp;
import org.msrg.publiy.publishSubscribe.matching.SimplePredicate;
import org.msrg.publiy.publishSubscribe.matching.SimpleStringPredicate;


public class Subscription implements Serializable {
	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = 1469807858098702492L;
	public final LinkedList<SimplePredicate> _predicates;
	public final LinkedList<SimpleStringPredicate> _stringPredicates;

	public Subscription(){
		_predicates = new LinkedList<SimplePredicate>();
		_stringPredicates = new LinkedList<SimpleStringPredicate>();
	}
	
	public Subscription addPredicate(SimplePredicate sp){
		_predicates.add(sp);
		
		return this;
	}

	public Subscription addStringPredicate(SimpleStringPredicate sp){
		_stringPredicates.add(sp);
		
		return this;
	}

	public String toString(){
		return "Sub(" + _predicates + _stringPredicates + ")";
	}
	
	public boolean hasStringPredicate(String attr, char operator, String strValue) {
		for(SimpleStringPredicate mySp : _stringPredicates)
			if(attr.equals(mySp._attribute))
				if(strValue.equals(mySp._strValue))
					if(operator == mySp._operator)
						return true;
		
		return false;
	}
	
	public boolean hasPredicate(String attr, char operator, int intValue) {
		for(SimplePredicate mySp : _predicates)
			if(attr.equals(mySp._attribute))
				if(intValue == mySp._intValue)
					if(operator == mySp._operator)
						return true;
		
		return false;
	}
	
	public boolean hasPredicate(SimplePredicate sp) {
		return hasPredicate(sp._attribute, sp._operator, sp._intValue);
	}
	
	public boolean hasStringPredicate(SimpleStringPredicate ssp) {
		return hasStringPredicate(ssp._attribute, ssp._operator, ssp._strValue);
	}
	
	public static String encode(Subscription subscription){
		StringWriter writer = new StringWriter();
		for(SimplePredicate sp : subscription._predicates) {
			String attr = sp._attribute;
			char op = sp._operator;
			int intValue = sp._intValue;
			writer.append("\t" + attr + "\t" + op + "\t" + intValue + " , ");
		}
		
		for(SimpleStringPredicate ssp : subscription._stringPredicates) {
			String attr = ssp._attribute;
			char op = ssp._operator;
			String strValue = ssp._strValue;
			writer.append("\t" + attr + "\t" + op + "\t" + strValue + " , ");
		}
		
		return writer.toString();
	}
	
	static Pattern SimplePredicatePattern =
		Pattern.compile(SimplePredicate.SimplePredicatePatternStr);
	public static synchronized Subscription decodeRegex(String str) {
		Subscription subscription = new Subscription();

		String[] predStrs = str.split(",");
		for (String onePredStr : predStrs) {
			Matcher matcher = SimplePredicatePattern.matcher(onePredStr);
			if(!matcher.matches())
				throw new IllegalStateException(onePredStr);
			if(matcher.groupCount()!=3)
				throw new IllegalStateException(onePredStr);
			
			String attr = matcher.group(1);
			String op = matcher.group(2);
			if (op.length()!=1)
				throw new IllegalStateException("Subscription does not match pattern: " + onePredStr);
			String valueStr = matcher.group(3);
			
			try{
				SimplePredicate sp =
					SimplePredicate.buildSimplePredicate(attr, op.charAt(0), new Integer(valueStr));
				subscription.addPredicate(sp);
			}catch(Exception ex) {
				SimpleStringPredicate ssp =
					SimpleStringPredicate.buildSimpleStringPredicate(attr, op.charAt(0), valueStr);
				subscription.addStringPredicate(ssp);
			}
		}
		
		return subscription;
	}
	
	@Deprecated
	public static Subscription decode_old(String str){
		char[] charStr = str.toCharArray();
		for ( int i=0 ; i<charStr.length ; i++ )
			if ( charStr[i] == ',' )
				charStr[i] = ' ';
		str = new String(charStr);

		StringTokenizer strT = new StringTokenizer(str);
		Subscription subscription = new Subscription();
		while ( strT.hasMoreTokens() )
		{
			String attr = strT.nextToken();
			char op = strT.nextToken().charAt(0);
			String value = strT.nextToken();
			int intValue = new Integer(value).intValue();
			SimplePredicate sp = SimplePredicate.buildSimplePredicate(attr, op, intValue);
			subscription.addPredicate(sp);
		}
		return subscription;
	}

	static Pattern _SUBSCRIPTION_PATTERN = Pattern.compile("\\s*(Sub\\(\\[)?([^\\]\\)]*)(\\]\\))?");
	static Pattern _SUB_PATTERN = Pattern.compile("\\s*([\\w\\-\\.\\:]+)\\s*([=%><~])\\s*/?([\\w\\-\\.\\:]+)\\s*");
	public static Subscription decode(String str){
		synchronized(_SUBSCRIPTION_PATTERN)
		{
			Matcher matcher = _SUBSCRIPTION_PATTERN.matcher(str);
			if(matcher.matches()) {
				if(matcher.groupCount() == 3)
					str = matcher.group(2);
				else if(matcher.groupCount() != 1)
					throw new IllegalArgumentException();
			}  else {
				throw new IllegalArgumentException();
			}
			
			Subscription subscription = new Subscription();
			String[] predicateStrs = str.split(",");
			for(String predicateStr : predicateStrs) {
				predicateStr = predicateStr.trim();
				if(predicateStr.length() == 0)
					continue;
				
				Matcher subMatcher = _SUB_PATTERN.matcher(predicateStr);
				if(!subMatcher.matches())
					throw new IllegalArgumentException(predicateStr);
				
				if(subMatcher.groupCount() != 3)
					throw new IllegalArgumentException();
				
				String attr = subMatcher.group(1);
				char op = subMatcher.group(2).charAt(0);
				String value = subMatcher.group(3);
				try{
					SimplePredicate sp = SimplePredicate.buildSimplePredicate(attr, op, new Integer(value).intValue());
					subscription.addPredicate(sp);
				} catch(Exception x) {
					SimpleStringPredicate ssp =
						SimpleStringPredicate.buildSimpleStringPredicate(attr, op, value);
					subscription.addStringPredicate(ssp);
				}
			}
			
			return subscription;
		}
	}
	
	public Subscription getClone(){
		Subscription newSubscription = new Subscription();
		Iterator<SimplePredicate> it = _predicates.iterator();
		while (it.hasNext() )
			newSubscription.addPredicate(it.next());
		return newSubscription;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		
		if(!this.getClass().isAssignableFrom(obj.getClass()))
			return false;

		Subscription subObj = (Subscription) obj;
		
		if(_predicates.size() != subObj._predicates.size())
			return false;
		
		if(_stringPredicates.size() != subObj._stringPredicates.size())
			return false;

		for(SimpleStringPredicate ssp : _stringPredicates) {
			boolean foundEqual = false;
			for(SimpleStringPredicate sspObj : subObj._stringPredicates) {
				if(sspObj.equals(ssp)) {
					foundEqual = true;
					break;
				}
			}
			
			if(!foundEqual)
				return false;
		}
		
		for(SimplePredicate sp : _predicates) {
			boolean foundEqual = false;
			for(SimplePredicate spObj : subObj._predicates) {
				if(spObj.equals(sp)) {
					foundEqual = true;
					break;
				}
			}
			
			if(!foundEqual)
				return false;
		}
		
		return true;
	}
	
	public boolean equalsExactly(Subscription subscription) {
		if(_predicates.size() != subscription._predicates.size())
			return false;
		
		if(_stringPredicates.size() != subscription._stringPredicates.size())
			return false;

		for(SimpleStringPredicate ssp : _stringPredicates)
			if(!subscription.hasStringPredicate(ssp))
				return false;

		for(SimplePredicate sp : _predicates)
			if(!subscription.hasPredicate(sp))
				return false;
		
		return true;
	}

	
	public static Subscription[] readSubscriptionsFromFile(String filename){
		String line = null;
		List<Subscription> subsList = new LinkedList<Subscription>();
		
		try{
			FileReader fileReader = new FileReader(filename);
			BufferedReader bufferedFileReader = new BufferedReader(fileReader);
			do{
				line = bufferedFileReader.readLine();
				if ( line == null )
					break;
				Subscription subscription = Subscription.decode(line);
				subsList.add(subscription);
			}while ( line.charAt(0) != '#');
			
			bufferedFileReader.close();
		}catch(IOException iox){}
		
		return subsList.toArray(new Subscription[0]);
	}
	
	public static void main(String[] argv) {
        String pubStr = "	ATTR8          18    ATTR9         19";
        Publication publication = Publication.decode(pubStr);
        
        Subscription subscription = Subscription.decode("Sub([ATTR8=18, ATTR9=19])");
        MatchingEngine me = new MatchingEngineImp();
        System.out.println(me.match(subscription, publication));
	}
}

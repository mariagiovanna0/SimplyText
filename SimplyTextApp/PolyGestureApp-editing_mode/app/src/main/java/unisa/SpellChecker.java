package unisa;

import java.util.ArrayList;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.UnderlineSpan;

public class SpellChecker {
	private ArrayList<String> dictionary;
	
	public void initDictionary(String text){
//		Log.i("ciao", text);
		dictionary = new ArrayList<String>();

		String allTextWords[] = text.split("([.,!?:;'\"-]|\\s|\\r|\\n)+");
		for(int i=0; i<allTextWords.length; i++){
			String token = allTextWords[i];
			if (!dictionary.contains(token)){
//				Log.i("ciao", token+" added");
				dictionary.add(token);
			}
		}

//		Collections.sort(dictionary);
	}
	
	
//	public void initDictionary(String text){
//		dictionary = new ArrayList<String>();
//		StringTokenizer st = new StringTokenizer(text);
//		while(st.hasMoreTokens()){
//			String token = st.nextToken();
//			if (!dictionary.contains(token))
//				dictionary.add(token);
//		}
//
////		Collections.sort(dictionary);
//	}
	
	public boolean dictionaryLookup(String word){
		return dictionary.contains(word);
	}
	
	public SpannableString underlineErrors(CharSequence cs){
		String text = cs.toString();
        SpannableString wordToSpan = new SpannableString(text);
        
        int fromIndex = 0;
        String words[] = text.split("([.,!?:;'\"-]|\\s)+");
		for(int i=0; i<words.length; i++){
			String token = words[i];
			int start = text.indexOf(token, fromIndex);
			int end = start+token.length();

			if (!dictionaryLookup(token)){
		        wordToSpan.setSpan(new UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			fromIndex = end;
		}

        return wordToSpan;
	}
	
	
//	public SpannableString underlineErrors(CharSequence cs){
//		String text = cs.toString();
//        SpannableString wordToSpan = new SpannableString(text);
//        StringTokenizer st = new StringTokenizer(text," ",true);
//        int start = 0;
//        int end = 0;
//		while(st.hasMoreTokens()){
//			String token = st.nextToken();
//			end += token.length();
//			if (token.trim().length()>0 && !dictionaryLookup(token)){
//		        wordToSpan.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//			}
//			start = end;
//		}
//
//        return wordToSpan;
//	}
	
	public static void main(String args[]){
		String finalText = "one two three four five";
		String initialText = "one two thXree four five";
		SpellChecker sc = new SpellChecker();
		sc.initDictionary(finalText);
		System.out.println(sc.underlineErrors(initialText));
	}
}
 
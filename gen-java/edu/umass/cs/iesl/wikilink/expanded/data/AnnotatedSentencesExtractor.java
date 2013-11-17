package edu.umass.cs.iesl.wikilink.expanded.data;

import java.util.TreeMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

// Given a set of mentions and a text representing the text from a HTML file, 
// this class returns the sentences from the text that contain at least two anchors. 
// The returned sentences will be annotated with the mentions (wikipedia link + freebaseId).
// For example: 
// 		text = "Barack Obama met with Putin.";
//		mentions = ("Barack Obama":(wikipedia/Barak_Obrama; freebaseid= 920))
//				   ("Putin":(wikipedia/Vladimir_Putin; freebaseid= 230))
// returns: "[Barack Obama]<#wikipedia/Barack_Obama;920#> met with [Putin]<#wikipedia/Putin;230#>."
public class AnnotatedSentencesExtractor {
	static int number_null_freebase_ids = 0;
	static int total_number_anchors = 0;
	
	static public Vector<String> extractSentences(String text, TreeMap<String, Mention> mentions) {
		Vector<String> rez = new Vector<String>();
		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < text.length(); ++i) {
			char c = text.charAt(i);
			if (c == '\n' || c == '.' || c == '!' || c == '?') {
				if (c != '\n') sb.append(c);
				String annotated_sentence = insertMentions(sb.toString(), mentions);
				sb = new StringBuilder();				
				if (annotated_sentence != null) {
					rez.add(annotated_sentence);
				}
			} else {
				sb.append(c);
			}
		}		
		return rez;
	}

	// Given the input string, it inserts the mentions in it near the corresponding substrings. 
	static private String insertMentions(String text, TreeMap<String, Mention> mentions) {
		String rez = text;
		int nr_anchors = 0;
		int nr_null_freebase_ids = 0;
		
		for (Mention m : mentions.values()) {
			int lastIndex = 0;
			while(lastIndex != -1){
				lastIndex = rez.indexOf(m.anchor_text, lastIndex);
				if( lastIndex != -1){
					int f = rez.indexOf("[",lastIndex), l = rez.indexOf("#>",lastIndex);
					if (l != -1 && (f == -1 || f > l)) { // We are inside an anchor already
						lastIndex += m.anchor_text.length();
					} else {
						StringBuilder sb = new StringBuilder();
						sb.append(rez.substring(0, lastIndex));
						sb.append("[");
						sb.append(rez.substring(lastIndex, lastIndex + m.anchor_text.length()));
						String t = "]<#" + m.wiki_url.substring(7) + ";" + m.freebase_id + "#>";
						sb.append(t);
						sb.append(rez.substring(lastIndex + m.anchor_text.length()));
						rez = sb.toString();
						nr_anchors ++;
						if (m.freebase_id == null) nr_null_freebase_ids++;

						lastIndex += m.anchor_text.length() + 1 + t.length();
					}
				}
			}
		}
		
		// Keep just sentences with at least 2 wikipedia references (anchors).
		if (nr_anchors < 2) {
			return null;
		}
		
		number_null_freebase_ids += nr_null_freebase_ids;
		total_number_anchors += nr_anchors;
		return rez;
	}
}

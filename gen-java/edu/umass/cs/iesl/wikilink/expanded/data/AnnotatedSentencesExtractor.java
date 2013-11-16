package edu.umass.cs.iesl.wikilink.expanded.data;

import java.util.HashMap;
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
// returns: "Barack Obama <#wikipedia/Barack_Obama;920#> met with Putin <#wikipedia/Putin;230#>."
public class AnnotatedSentencesExtractor {
	static public Vector<String> extractSentences(String text, HashMap<String, Mention> mentions) {
		Vector<String> rez = new Vector<String>();
		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < text.length(); ++i) {
			char c = text.charAt(i);
			if (c == '\n' || c == '.' || c == '!' || c == '?') {
				if (c != '\n') sb.append(c);
				if (NumberMentions(sb.toString(), mentions) >= 2) {
					rez.add(insertMentions(sb.toString(), mentions));
				}
				sb = new StringBuilder();
			} else {
				sb.append(c);
			}
		}		
		return rez;
	}
	
	static private int NumberMentions(String text, HashMap<String, Mention> mentions) {
		int num_mentions = 0;
		for (Mention m : mentions.values()) {
			int lastIndex = 0;
			while(lastIndex != -1){
				lastIndex = text.indexOf(m.anchor_text,lastIndex);
			    if( lastIndex != -1){
			    	num_mentions ++;
			    	lastIndex += m.anchor_text.length();
			    }
			}
		}
		return num_mentions;
	}
	
	
	// Given the input string, it inserts the mentions in it near the corresponding substrings. 
	static private String insertMentions(String text, HashMap<String, Mention> mentions) {
		String rez = text;
		for (Mention m : mentions.values()) {
			int lastIndex = 0;
			while(lastIndex != -1){
				lastIndex = rez.indexOf(m.anchor_text, lastIndex);
			    if( lastIndex != -1){
			    	StringBuilder sb = new StringBuilder();
			    	sb.append(rez.substring(0, lastIndex));
			    	sb.append("[");
			    	sb.append(rez.substring(lastIndex, lastIndex + m.anchor_text.length()));
			    	String t = "]<#" + m.wiki_url.substring(7) + ";" + m.freebase_id + "#>";
					sb.append(t);
					sb.append(rez.substring(lastIndex + m.anchor_text.length()));
			    	rez = sb.toString();
		
			    	lastIndex += m.anchor_text.length() + 1 + t.length();
			    }
			}
		}
		return rez;
	}
}

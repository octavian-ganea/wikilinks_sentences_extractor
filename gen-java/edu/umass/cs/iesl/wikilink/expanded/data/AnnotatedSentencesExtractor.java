package edu.umass.cs.iesl.wikilink.expanded.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TreeMap;
import java.util.Vector;

import org.htmlparser.parserapplications.StringExtractor;
import org.htmlparser.util.ParserException;


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


	// Main function for complete sentence extraction given a Thrift stream as input.
	static public void parseHTMLandExtractSentences(ThriftReader thriftIn) throws ParserException, IOException {
		int pages_counter = 1;
		
		while (thriftIn.hasNext()) {
			WikiLinkItem i = ((WikiLinkItem)thriftIn.read());		  
			if (i.content.dom != null) {
				// Use an in-memory file system to solve this API issue with StringExtractor that
				// doesn't allow input as a string HTML file content, but just the HTML file path.
				File temp = new File("/dev/shm/htmlparser.tmp");
				BufferedWriter out = new BufferedWriter(new FileWriter(temp));

				out.write(HTMLHackCleaner.replaceSpecialSymbols(i.content.dom));
				out.close();		

				StringExtractor se = new StringExtractor("/dev/shm/htmlparser.tmp");
				String all_paragraphs = se.extractStrings(false);

				temp.delete();

				// Remove duplicates in mentions:
				TreeMap<String, Mention> mentions_hashtable = new TreeMap<String,Mention>(new Utils.StringComp());
				for (Mention m : i.mentions) {
					//System.out.println(m.anchor_text + " " + m.wiki_url + " @@@@");
					if (!mentions_hashtable.containsKey(m.anchor_text)) {
						mentions_hashtable.put(m.anchor_text, m);
					}
				}

				// Vector with sentences containing at least two wikipedia hyperlinks; annotated with their
				// freebase ids.
				Vector<String> proper_sentences =
					AnnotatedSentencesExtractor.extractSentences(all_paragraphs, mentions_hashtable);
				for (String s : proper_sentences) System.out.println(">>>>>\n" + s);
				if (proper_sentences.size() > 0) {
					System.out.println("------- Finished page " + pages_counter + " ----");
					System.out.print("------ Stats so far: nr anchors = ");
					System.out.print(AnnotatedSentencesExtractor.total_number_anchors);
					System.out.print(" ; nr null freebase ids = ");
					System.out.println(AnnotatedSentencesExtractor.number_null_freebase_ids + " ----");
				}
			}
			pages_counter++;
		}		
	}
}

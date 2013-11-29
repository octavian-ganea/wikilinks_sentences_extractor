package edu.umass.cs.iesl.wikilink.expanded.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import org.htmlparser.parserapplications.StringExtractor;
import org.htmlparser.util.ParserException;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;


// This class parses each WikiLinkItem Thrift object and outputs the set of mentions from the page
// together with a set of sentences from the HTML file. The sentences are obtaining after applying an
// HTML parser to extract the text from the page and after delimiting them by '.', '?', '!' or '\n'
// or by using the Stanford Core NLP parser.
public class SimpleSentencesExtractor {
	static public Vector<String> extractSentences(String text) {
		Vector<String> rez = new Vector<String>();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < text.length(); ++i) {
			char c = text.charAt(i);
			if (c == '\n' || c == '.' || c == '!' || c == '?') {
				if (c != '\n') sb.append(c);
				String sentence = sb.toString();
				sb = new StringBuilder();

				int k = 0;
				while (k < sentence.length() && Character.isSpaceChar(sentence.charAt(k))) k++;
				if (k + 2 >= sentence.length()) continue;
				sentence = sentence.substring(k);
				
				// Just sentences with at least two words and ending in . ? or !
				int j = sentence.length() - 1;
				while (j > 0 && Character.isSpaceChar(sentence.charAt(j))) j--;
				char last = sentence.charAt(j);
				if (j > 0 && (last == '.' || last == '!' || last == '?') && sentence.contains(" ")) {
					rez.add(sentence);
				}
			} else {
				sb.append(c);
			}
		}		
		return rez;
	}

	static public Vector<String> extractSentencesWithStanfordNLP(String text, StanfordCoreNLP pipeline) {
		Vector<String> rez = new Vector<String>();
		StringTokenizer st = new StringTokenizer(text, "\n");
		while (st.hasMoreTokens()) {
			String par = st.nextToken();
			int dot = par.lastIndexOf('.');
			int exclm = par.lastIndexOf('!');
			int interog = par.lastIndexOf('?');
			int x = Math.max(dot, exclm);
			x = Math.max(x, interog);
			if (x == -1) continue;
			par = par.substring(0, x+1);
			if (par.contains(" ")) {
			    Annotation document = new Annotation(par);			    
			    pipeline.annotate(document);
			    List<CoreMap> sentences = document.get(SentencesAnnotation.class);

			    for(CoreMap sentence: sentences) {
			    	StringBuilder sb = new StringBuilder("");	
			    	for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
			    		String word = token.get(TextAnnotation.class);
			    		sb.append(word + " ");
			    	}
				    rez.add(sb.toString());
			    }
			}			
		}
		return rez;
	}
	
	
	
	
	// Main function for complete sentence extraction given a Thrift stream as input.
	static public void parseHTMLandExtractSentences(ThriftReader thriftIn) throws ParserException, IOException, InterruptedException {
	    Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		
		int pages_counter = 1;
		int total_number_sentences_so_far = 0;
		while (thriftIn.hasNext()) {
			WikiLinkItem i = ((WikiLinkItem)thriftIn.read());

			//if (pages_counter % 1000 == 0) System.out.println(pages_counter);
			
			/*
			if (i.content.dom != null && pages_counter > 79548) { // TODO: DELETE THIS 
				System.out.println(pages_counter); // ######       // TODO: DELETE THIS LINE
			*/	
			if (i.content.dom != null) {				
				// Use an in-memory file system to solve this API issue with StringExtractor that
				// doesn't allow input as a string HTML file content, but just the HTML file path.
				File temp = new File("/dev/shm/htmlparser.tmp" + pages_counter);
				BufferedWriter out = new BufferedWriter(new FileWriter(temp));

				out.write(HTMLHackCleaner.replaceSpecialSymbols(i.content.dom));
				out.close();		

				StringExtractor se = new StringExtractor("/dev/shm/htmlparser.tmp" + pages_counter);
				String all_paragraphs = se.extractStrings(false);
				temp.delete();
				
				// Vector with sentences containing at least two wikipedia hyperlinks; annotated with their
				// freebase ids.
				Vector<String> proper_sentences =
					SimpleSentencesExtractor.extractSentencesWithStanfordNLP(all_paragraphs, pipeline);
				
				if (proper_sentences.size() > 0) {
					total_number_sentences_so_far += proper_sentences.size();
					System.out.println("-------------------------------------");
					System.out.println("--- Page num: " + pages_counter);
					System.out.println("--- Docid: " + i.doc_id);
					System.out.println("--- URL: " + i.url);
					System.out.println("--- Num sentences: " + proper_sentences.size());
					System.out.println("--- Total num sentences so far: " + total_number_sentences_so_far);
					System.out.println("--- Mentions:");
					for (Mention m : i.mentions) {
						System.out.println(m.wiki_url + "; " + Utils.convertGUIDtoMID(m.freebase_id) + " --> " + m.anchor_text);
					}
					System.out.println("--- Sentences:");
					for (String s : proper_sentences) System.out.println(s);
				}	
			}
			pages_counter++;
		}		
	}
}

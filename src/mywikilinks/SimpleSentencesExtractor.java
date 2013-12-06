package mywikilinks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import org.htmlparser.parserapplications.StringExtractor;
import org.htmlparser.util.ParserException;

import scala.collection.mutable.HashSet;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.WordToSentenceProcessor;
import edu.stanford.nlp.util.CoreMap;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.*;

import edu.umass.cs.iesl.wikilink.expanded.process.CleanDOM;


import org.jsoup.Jsoup;
import org.jsoup.nodes.*;



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

	static public Vector<String> extractSentencesWithStanfordNLP(String text) {
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
			    PTBTokenizer ptbt = new PTBTokenizer(
			    		new StringReader(par), new CoreLabelTokenFactory(), "ptb3Escaping=false");

			    List<List<CoreLabel>> sents = (new WordToSentenceProcessor()).process(ptbt.tokenize());
			    for (List<CoreLabel> sent : sents) {
			    	StringBuilder sb = new StringBuilder("");
			    	for (CoreLabel w : sent) sb.append(w + " ");
			    	rez.add(sb.toString());
			    }				
			}			
		}
		return rez;
	}
	
	
	static public String extractTextFromHTMLUsingStringExtractor(String html) throws Exception {
		// Use an in-memory file system to solve this API issue with StringExtractor that
		// doesn't allow input as a string HTML file content, but just the HTML file path.
		double x = Math.random();
		File temp = new File("/dev/shm/htmlparser.tmp" + x);
		BufferedWriter out = new BufferedWriter(new FileWriter(temp));

		out.write(html);
		out.close();		
		StringExtractor se = new StringExtractor("/dev/shm/htmlparser.tmp" + x);
		String all_text = se.extractStrings(false);
		temp.delete();
		return all_text;		
	}
	
	// Main function for complete sentence extraction given a Thrift stream as input.
	static public void parseHTMLandExtractSentences(ThriftReader thriftIn) throws IOException, Exception {
		int pages_counter = 1;
		int total_number_sentences_so_far = 0;
		
		while (thriftIn.hasNext()) {
			WikiLinkItem i = ((WikiLinkItem)thriftIn.read());
			/*
			if (i.content.dom != null && pages_counter > 79548) { // TODO: DELETE THIS 
				System.out.println(pages_counter); // ######       // TODO: DELETE THIS LINE
			*/	
			if (i.content.dom != null) {	

				HashMap<String, Vector<Integer>> hm_index = new HashMap<String,Vector<Integer>>();
				for (Mention m : i.mentions) {
					int startWikiInURL = m.wiki_url.indexOf("wikipedia.org");
					try {
						String partOfURL = "";
						try {
							partOfURL = URLDecoder.decode(m.wiki_url.substring(startWikiInURL), "UTF-8");
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
						String key = partOfURL +";;"+m.anchor_text;

						hm_index.put(key, new Vector<Integer>());
					} catch (java.lang.IllegalArgumentException e) {
				    }
				}

				int index = 0;
				for (Mention m : i.mentions) {
					int startWikiInURL = m.wiki_url.indexOf("wikipedia.org");
					
					try {
						String partOfURL = URLDecoder.decode(m.wiki_url.substring(startWikiInURL), "UTF-8");
						String key = partOfURL +";;"+m.anchor_text;
					
						Vector<Integer> v = hm_index.get(key);
						v.add(index);
						hm_index.put(key, v);					
					} catch (java.lang.IllegalArgumentException e) {
				    } catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					
					index++;
				}

				//try {
				// This is how it is done in
				// https://code.google.com/p/wiki-link/source/browse/process/src/main/scala/edu/umass/cs/iesl/wikilink/expanded/process/Runner.scala:110				
				String cleanDom = "";
				try {
					cleanDom = Charset.defaultCharset().decode(ByteBuffer.wrap(i.content.dom.getBytes())).toString();
				} catch (java.lang.OutOfMemoryError e) {
					e.printStackTrace();
					System.err.print("CAUGHT OUT OF MEM ERROR HERE: " + cleanDom.length());
				}
				
				//String cleanDom = CleanDOM.apply(i.content.dom).get();
				Document doc = Jsoup.parse(HTMLHackCleaner.replaceSpecialSymbols(cleanDom));

				Iterator<Element> it = doc.select("a").iterator();
				HashSet<Integer> visited_mentions = new HashSet<Integer>();

				while (it.hasNext()) {
					Element link = it.next();
					String linkHref = link.attr("href");
					String linkText = link.text().trim();
					int startWikiInURL = linkHref.indexOf("wikipedia.org");
					if (startWikiInURL == -1) continue;

					String url = linkHref.substring(startWikiInURL);
					String partOfURL = "";
				    try {
						partOfURL = URLDecoder.decode(url, "UTF-8");
				    } catch (java.lang.IllegalArgumentException e) {
				    }
					
					String key = partOfURL +";;"+linkText;
					
					if (!hm_index.containsKey(key)) continue;
					for (Integer j : hm_index.get(key)) {
						if (!visited_mentions.contains(j)) {
							visited_mentions.add(j);
							link.append("[[[end " + j + "]]]");							
							link.before("[[[start " + j + "]]]");							
							break;
						}
					}					
				}								

				// Use Wikilinks code to extract text from the HTML.				
				String all_text ="";
				 all_text = extractTextFromHTMLUsingStringExtractor(doc.outerHtml());
				 
				/*
				 ////// This is their parser, but it consumes ~ 20GB of RAM and has a lot of warnings 
				 ////// for quite many HTMLs. Seems to do the same thing as the parser I used above. 
				all_text = KeepEverythingExtractor.INSTANCE.getText(doc.outerHtml());
				
				 /////// Vector with sentences containing at least two wikipedia hyperlinks; annotated with their
				 /////// freebase ids. 
				 /////// Use Stanford NLP framework to extract sentences from the text.
				
				Vector<String> proper_sentences =
					SimpleSentencesExtractor.extractSentencesWithStanfordNLP(all_text);
				*/
				
				if (all_text.length() > 0) {
				//	total_number_sentences_so_far += proper_sentences.size();
					System.out.println("-------------------------------------");
					System.out.println("--- Page num: " + pages_counter);
					System.out.println("--- Docid: " + i.doc_id);
					System.out.println("--- URL: " + i.url);
				//	System.out.println("--- Num sentences: " + proper_sentences.size());
				//	System.out.println("--- Total num sentences so far: " + total_number_sentences_so_far);
					System.out.println("--- Mentions:");
					
					int index_mention = 0;
					for (Mention m : i.mentions) {
						System.out.println(m.wiki_url + "; " + Utils.convertGUIDtoMID(m.freebase_id) + " --> " + m.anchor_text);
						index_mention ++;
					}
				//	System.out.println("--- Sentences:");
				//	for (String s : proper_sentences) System.out.println(s);
					System.out.println("--- All text:");
					System.out.println(all_text);
				}
			}
			pages_counter++;
		}		
	}
}

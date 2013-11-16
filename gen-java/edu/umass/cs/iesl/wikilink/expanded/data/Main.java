package edu.umass.cs.iesl.wikilink.expanded.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Vector;

import org.apache.thrift.TBase;
import org.htmlparser.parserapplications.StringExtractor;

import org.apache.commons.lang3.StringEscapeUtils;

public class Main {
	public static void main(String[] args) throws Exception {
		// Create the reader
		ThriftReader thriftIn = new ThriftReader(new File(args[0]), new ThriftReader.TBaseCreator() {
			@Override
			public TBase create() {
				return new WikiLinkItem();
			}
		});

		thriftIn.open();

		int pages_counter = 1;
		while (thriftIn.hasNext()) {
			WikiLinkItem i = ((WikiLinkItem)thriftIn.read());		  

			if (i.content.dom != null) {
				// Use an in-memory file system to solve this API issue with StringExtractor that doesn't allow 
				// input as a string HTML file content, but just the HTML file path.
				File temp = new File("/dev/shm/htmlparser.tmp");
				BufferedWriter out = new BufferedWriter(new FileWriter(temp));

				out.write(HTMLHackCleaner.replaceSpecialSymbols(i.content.dom));
				out.close();		

				StringExtractor se = new StringExtractor("/dev/shm/htmlparser.tmp");
				String all_paragraphs = se.extractStrings(false);

				if (!temp.delete()) {
					System.err.println("Cannot delete temporary file " + temp.getAbsolutePath());
				}

				// Remove duplicates in mentions:
				HashMap<String, Mention> mentions_hashtable = new HashMap<String,Mention>();
				for (Mention m : i.mentions) {
					if (!mentions_hashtable.containsKey(m.anchor_text)) {
						mentions_hashtable.put(m.anchor_text, m);
					}
				}

				// Vector with sentences containing at least two wikipedia hyperlinks; annotated with their freebase ids.
				Vector<String> proper_sentences = AnnotatedSentencesExtractor.extractSentences(all_paragraphs, mentions_hashtable);
				for (String s : proper_sentences) System.out.println(">>>>> " + s);
				if (proper_sentences.size() > 0) {
					System.out.println("------------- Page " + pages_counter + " --------------");
				}
			}

			pages_counter++;
		}

		thriftIn.close();
	}
}

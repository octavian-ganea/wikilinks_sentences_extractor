package edu.umass.cs.iesl.wikilink.expanded.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.htmlparser.util.ParserException;

class Posting implements Comparable {
	String anchor;
	Integer count;
	
	public Posting(String a, Integer i) {
		anchor = a; count = i;
	}
	@Override
	public int compareTo(Object o) {
		Posting x = (Posting) o;
		if (count != x.count) return -count + x.count;
		return anchor.compareTo(x.anchor);
	}
}

public class AnchorsInvertedIndex {
	// Create in memory index from one shard of data.
	public static HashMap<String, TreeMap<String, Integer>> createInMemoryIndex(
			ThriftReader thriftIn)  throws ParserException, IOException {		
		int pages_counter = 1;
		
		// Inverted index with: entity --> List[name, freq]
		HashMap<String, String> wiki_freebase_map_ids = new HashMap<String, String>();
		HashMap<String, TreeMap<String, Integer>> inverted_index = 
			new HashMap<String, TreeMap<String, Integer>>();
		
		int nr_entries = 0;
		while (thriftIn.hasNext()) {
			if (pages_counter % 1000 == 0)
				System.out.println("Finished page : " + pages_counter);
			
			WikiLinkItem i = ((WikiLinkItem)thriftIn.read());		  

			HashSet<String> seen_anchors = new HashSet<String>();
			for (Mention m : i.mentions) {
				if (seen_anchors.contains(m.anchor_text)) continue;
				seen_anchors.add(m.anchor_text);	
				
				String key = m.wiki_url.substring(m.wiki_url.lastIndexOf('/'));
				wiki_freebase_map_ids.put(key, Utils.convertGUIDtoMID(m.freebase_id));
				if (!inverted_index.containsKey(key)) {
					inverted_index.put(key, new TreeMap<String, Integer>());
				}
				TreeMap<String, Integer> posting_list = inverted_index.get(key);
				if (posting_list.containsKey(m.anchor_text)) {
					posting_list.put(m.anchor_text, posting_list.get(m.anchor_text) + 1);
				} else {
					nr_entries++;
					posting_list.put(m.anchor_text, 1);					
				}
			}
			pages_counter++;
		}
		System.out.println("########: " + nr_entries);
		return inverted_index;
	}
	
	
	// Outputs (wikiurl+freebaseID , anchor text) pairs. After all data will be processed,
	// another function will build the actual index.
	// Output files: index_shards/term_docids/%d
	public static void outputTermDocidPairs(
			ThriftReader thriftIn)  throws ParserException, IOException {		
		Vector<PrintWriter> files = new Vector<PrintWriter>();
		for (int i = 0; i < 26; ++i) {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("index_shards/term_docids/" + i, true)));
			files.add(out);
		}
		
		int pages_counter = 1;		
		while (thriftIn.hasNext()) {
			WikiLinkItem i = ((WikiLinkItem)thriftIn.read());		  

			HashSet<String> seen_anchors = new HashSet<String>();
			for (Mention m : i.mentions) {
				if (seen_anchors.contains(m.anchor_text)) continue;
				seen_anchors.add(m.anchor_text);	
				
				String key_wiki = m.wiki_url.substring(m.wiki_url.lastIndexOf('/'));
				String key_freebase = Utils.convertGUIDtoMID(m.freebase_id);
				char out_fop = 'a';
				if (key_wiki.length() >= 2) out_fop = key_wiki.charAt(1);
				if (Character.isUpperCase(out_fop)) {
					out_fop = Character.toLowerCase(out_fop);
				}
				PrintWriter out = files.elementAt( (2600 + (int)out_fop - 'a') % 26);
				out.println(key_wiki);
				out.println(key_freebase);
				out.println(m.anchor_text);
				out.println("--");
			}
			pages_counter++;
		}
		
		for (int i = 0; i < 26; ++i) {
			files.elementAt(i).flush();
			files.elementAt(i).close();
		}		
	}

	// Creates a distributed inverted-index from Term-Docid pairs and writes it
	// in files index_shards/starting-letter.shard
	// Output files: index_shards/letter.shard
	public static void createDistributedIndexFromTermDocidPairs() throws IOException {
		PrintWriter logs = new PrintWriter(new BufferedWriter(new FileWriter("index_shards/logs.txt")));

		for (int i = 0; i < 26; ++i) {
			char letter = (char)(((int)'A') + i);
			logs.println("Processing : " + letter);
			logs.flush();
			BufferedReader in = new BufferedReader(new FileReader("index_shards/term_docids/" + i));

			HashMap<String, TreeMap<String, Integer>> inverted_index = 
				new HashMap<String, TreeMap<String, Integer>>();
			HashMap<String,String> wiki_freebase_map = new HashMap<String,String>();
			
			try {
				
				int line_nr = 0;
		        String line = in.readLine();
		        String key_wiki = null;
		        String key_freebase = null;
		        String anchor = null;
		        while (line != null) {
		        	if (line_nr % 4 != 3 && line.length() <= 2 && line.startsWith("--")) {
		        		logs.println("Error on line " +line_nr + " in file " + letter);
		        	}
		        	if (line_nr % 4 == 0) {
		        		key_wiki = line;
		        	} else if (line_nr % 4 == 1) {
		        		if (!line.startsWith("null")) key_freebase = line;
		        	} else if (line_nr % 4 == 2) {
		        		anchor = line;
		        	} else if (line_nr % 4 == 3) {
						if (!inverted_index.containsKey(key_wiki)) {
							inverted_index.put(key_wiki, new TreeMap<String, Integer>());
						}		        		
						TreeMap<String, Integer> posting_list = inverted_index.get(key_wiki);
						if (posting_list.containsKey(anchor)) {
							posting_list.put(anchor, posting_list.get(anchor) + 1);
						} else {
							posting_list.put(anchor, 1);					
						}
						if (key_freebase != null && !wiki_freebase_map.containsKey(key_wiki)) {
							wiki_freebase_map.put(key_wiki, key_freebase);
						}
						
		        		key_wiki = null;
		        		key_freebase = null;
		        		anchor = null;
		        	}
		        	 
		        	line_nr++;
		            line = in.readLine();
		        }
		    } finally {
		        in.close();
				PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("index_shards/" + letter + ".shard")));
				for (String key_wiki : inverted_index.keySet()) {
					String key_freebase = null;
					if (wiki_freebase_map.containsKey(key_wiki)) 
						key_freebase = wiki_freebase_map.get(key_wiki);
					out.print("wiki" + key_wiki + ";freeb_id:" + key_freebase + " ---> ");
					
					TreeMap<String, Integer> posting_list = inverted_index.get(key_wiki);
					List<Posting> v = new Vector<Posting>();
					for (String anchor : posting_list.keySet()) {
						Posting p = new Posting(anchor, posting_list.get(anchor));
						v.add(p);
					}
					Collections.sort(v);
					for (Posting p : v) {
						out.print("(" + p.anchor + "," + p.count + "), ");
					}
					out.println();
				}
				out.flush();
		        out.close();
		    }
		}
		logs.close();
	}
}

package edu.umass.cs.iesl.wikilink.expanded.data;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import org.htmlparser.util.ParserException;

public class AnchorsInvertedIndex {
	public static HashMap<String, TreeMap<String, Integer>> createIndex(
			ThriftReader thriftIn)  throws ParserException, IOException {
		int pages_counter = 1;
		
		// Inverted index with: entity --> List[name, freq]
		HashMap<String, String> wiki_freebase_map_ids = new HashMap<String, String>();
		HashMap<String, TreeMap<String, Integer>> inverted_index = 
			new HashMap<String, TreeMap<String, Integer>>();
		while (thriftIn.hasNext()) {
			if (pages_counter % 1000 == 0)
				System.out.println("Finished page : " + pages_counter);
			
			WikiLinkItem i = ((WikiLinkItem)thriftIn.read());		  

			/* -------------------- INDEX construction ------------------- */
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
					posting_list.put(m.anchor_text, 1);					
				}
			}

			if (pages_counter == 90000) {
				for (String s : inverted_index.keySet()) {
					boolean print = false;
					for (String k : inverted_index.get(s).keySet()) {
						if (inverted_index.get(s).get(k) > 5) {
							if (!print) {
								System.out.print(s + "--->");
								print = true;
							}
							System.out.print("(" + k + ":" + inverted_index.get(s).get(k) + ") ;");
						}
					}
					if (print)	System.out.println();
				}
				System.out.println(inverted_index.size());
			}
			/* -------------------- INDEX finished ------------------- */
			pages_counter++;
		}
		return inverted_index;
	}
}

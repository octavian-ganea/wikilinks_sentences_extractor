package mywikilinks;

import java.io.File;
import java.net.URLDecoder;

import org.apache.thrift.TBase;

public class Main {

	public static void create_index_main(String[] args) throws Exception {
		//AnchorsInvertedIndex.createDistributedIndexFromTermDocidPairs();
	}
	
	public static void main(String[] args) throws Exception {
		// Create the reader
		ThriftReader thriftIn = new ThriftReader(new File(args[0]), new ThriftReader.TBaseCreator() {
			@Override
			public TBase create() {
				return new WikiLinkItem();
			}
		}); 
		thriftIn.open();
		
		/* HTML parser and sentence (with annotations) extractor */
		//AnnotatedSentencesExtractor.parseHTMLandExtractSentences(thriftIn);
		
		/* Looks just at the mentions and outputs (wiki_url, freebase_id, anchor_text) */
		//AnchorsInvertedIndex.outputTermDocidPairs(thriftIn);		

		/* HTML parser and sentence (without annotations) extractor */
		SimpleSentencesExtractor.parseHTMLandExtractSentences(thriftIn);
		
		thriftIn.close();
	}
}

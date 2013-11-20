package edu.umass.cs.iesl.wikilink.expanded.data;

import java.io.File;
import org.apache.thrift.TBase;


public class Main {

	public static void main(String[] args) throws Exception {
		AnchorsInvertedIndex.createDistributedIndexFromTermDocidPairs();
	}
	
	public static void corpus_download_main(String[] args) throws Exception {
		// Create the reader
		ThriftReader thriftIn = new ThriftReader(new File(args[0]), new ThriftReader.TBaseCreator() {
			@Override
			public TBase create() {
				return new WikiLinkItem();
			}
		}); 
		thriftIn.open();
	
		/* HTML parser and sentence extractor */
		//AnnotatedSentencesExtractor.parseHTMLandExtractSentences(thriftIn);
		
		//AnchorsInvertedIndex.outputTermDocidPairs(thriftIn);		
		thriftIn.close();
	}
}

package edu.umass.cs.iesl.wikilink.expanded.data;

import org.apache.commons.lang3.StringEscapeUtils;

public class HTMLHackCleaner {

	/* HACK to replace HTML special chars */
	public static String replaceSpecialSymbols(String html) {
		StringBuilder sb = new StringBuilder();
		for (int j = 0; j < html.length(); ++j) {
			char c = html.charAt(j);
			if (c >= (char)8216 && c <= (char)8218) sb.append('\'');
			else if (c >= (char)8211 && c <= (char)8212) sb.append('-');
			else if (c >= (char)8220 && c <= (char)8221) sb.append('"');
			else if (c == (char)8230) sb.append("...");
			else if (c == (char)171) sb.append("<<");
			else if (c == (char)187) sb.append(">>");
			else if (c <= (char)127) sb.append(c); // Some weird chars will be stopped at this point.
		}

		// This doesn't work with the above if statements.
		return sb.toString();
	}
}

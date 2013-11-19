package edu.umass.cs.iesl.wikilink.expanded.data;

import java.util.Comparator;

public class Utils {
	
	// According to http://wiki.freebase.com/wiki/Guid
	public static String convertGUIDtoMID(String s) {
		if (s == null) return null;
		String conv = "0123456789bcdfghjklmnpqrstvwxyz_";
		String left = s.substring(s.indexOf("9202a8c04000641f8") + "9202a8c04000641f8".length());
		StringBuilder sbinary = new StringBuilder();
		for (char c : left.toCharArray()) {
			String to_app = Integer.toBinaryString(Integer.parseInt(""+c, 16));
			String to_app_with_padding = String.format("%4s", to_app).replace(' ', '0');
			sbinary.append(to_app_with_padding); 
		}
		String rez = "";
		int index = 0;
		int nr = 1;
		for (char c : sbinary.toString().toCharArray()) {
			index = (index << 1);
			index |= ((int)(c - '0'));
			nr++;
			if (nr > 5) {
				if (index > 0 || rez != "") rez += conv.charAt(index);
				nr = 1;
				index = 0;
			}
		}
		rez = "/m/0" + rez;
		return rez;
	}

	// Comparator used for mentions_hashtable.
	static class StringComp implements Comparator<String> {
		@Override
		public int compare(String o1, String o2) {
			// Try to match longer anchor texts first. For example: "University of Oklahoma" will be replaced
			// with its anchor before it will be "Oklahoma".
			if (o1.length() - o2.length() != 0) return o2.length() - o1.length();
			return o2.compareTo(o1);
		}
	}
	
}

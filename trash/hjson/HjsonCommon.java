package modmake.util.hjson;

import arc.func.Prov;
import modmake.util.Tools;

import java.util.StringJoiner;

import static java.lang.Double.isFinite;
import static rhino.ScriptRuntime.toNumber;

public class HjsonCommon {
	public Number tryParseNumber(String text, boolean stopAtNext) {

		// try to parse a number

		double        number;
		StringBuilder string       = new StringBuilder();
		int           leadingZeros = 0;
		boolean       testLeading  = true;
		int[]         at           = {0};
		Character[]   ch           = {'.'};
		Prov<Character> next = () -> {
			try {
				ch[0] = text.charAt(at[0]);
			} catch (Exception e) {
				ch[0] = null;
			}
			at[0]++;
			return ch[0];
		};

		next.get();
		if (ch[0] == '-') {
			string.append("-");
			next.get();
		}
		while (ch[0] >= '0' && ch[0] <= '9') {
			if (testLeading) {
				if (ch[0] == '0') leadingZeros++;
				else testLeading = false;
			}
			string.append(ch[0]);
			next.get();
		}
		if (testLeading) leadingZeros--; // single 0 is allowed
		if (ch[0] == '.') {
			string.append('.');
			while (next.get() != null && ch[0] >= '0' && ch[0] <= '9')
				string.append(ch[0]);
		}
		if (ch[0] == 'e' || ch[0] == 'E') {
			string.append(ch[0]);
			next.get();
			if (ch[0] == '-' || ch[0] == '+') {
				string.append(ch[0]);
				next.get();
			}
			while (ch[0] >= '0' && ch[0] <= '9') {
				string.append(ch[0]);
				next.get();
			}
		}

		// skip white/to (newline)
		while (ch[0] != null && ch[0] <= ' ') next.get();

		if (stopAtNext) {
			// end scan if we find a punctuator character like ,}] or a comment
			if (ch[0] == ',' || ch[0] == '}' || ch[0] == ']' ||
					ch[0] == '#' || ch[0] == '/' && (text.charAt(at[0]) == '/' || text.charAt(at[0]) == '*')) ch[0] = 0;
		}

		number = toNumber(string);
		if (ch[0] != null || leadingZeros != 0 || !isFinite(number)) return Double.NaN;
		else return number;
	}

	public Comment createComment(MyString value, Comment comment) {
		// if (Object.defineProperty) Object.defineProperty(value, "__COMMENTS__", { enumerable: false, writable: true });
		return value.__COMMENTS__ = Tools.or(comment, new Comment());
	}

	public void removeComment(MyString value) {
		value.__COMMENTS__ = new Comment(null);
	}

	public Comment getComment(MyString value) {
		return value.__COMMENTS__;
	}

	public String forceComment(String text) {
		if (text.isBlank()) return "";
		var    array = text.split("\\n");
		String str;
		int    i, j, len;
		for (j = 0; j < array.length; j++) {
			str = array[j];
			len = str.length();
			for (i = 0; i < len; i++) {
				var c = str.charAt(i);
				if (c == '#') break;
				else if (c == '/' && (str.charAt(i + 1) == '/' || str.charAt(i + 1) == '*')) {
					if (str.charAt(i + 1) == '*') j = array.length; // assume /**/ covers whole block, bail out
					break;
				} else if (c > ' ') {
					array[j] = "# " + str;
					break;
				}
			}
		}
		StringJoiner joiner = new StringJoiner("\\n");
		for (var s : array) {
			joiner.add(s);
		}
		return joiner.toString();
	}


	static class Comment {
		public String value = null;

		public Comment() {}

		public Comment(String value) {
			this.value = value;
		}
	}

	static class MyString {
		public Comment __COMMENTS__;
		public String  string;
	}
}

package modmake.util.hjson;

import org.hjson.HjsonOptions;
import rhino.ScriptRuntime;

import java.util.regex.Pattern;

public class HjsonDsf {
	/* Hjson https://hjson.github.io */
	public static void loadDsf(col, type) {

		if (Object.prototype.toString.apply(col) != = '[object Array]') {
			if (col) throw new IllegalArgumentException("dsf option must contain an array!");
			else return nopDsf;
		} else if (col.length == 0) return nopDsf;

		var dsf = [];
		function isFunction (f) { return {}.toString.call(f) == = '[object Function]'; }

		col.forEach(function(x) {
			if (!x.name || !isFunction(x.parse) || !isFunction(x.stringify))
				throw new Error("extension does not match the DSF interface");
			dsf.push(function() {
				try {
					if (type == "parse") {
						return x.parse.apply(null, arguments);
					} else if (type == "stringify") {
						var res = x.stringify.apply(null, arguments);
						// check result
						if (res != = undefined && (typeof res != = "string" ||
																											 res.length == = 0 ||
																																			 res[0] == = '"' ||
            [].some.call(res, function(c) {return isInvalidDsfChar(c);})))
						throw new Error("value may not be empty, start with a quote or contain a punctuator character except colon: " + res);
						return res;
					} else throw new Error("Invalid type");
				} catch (e) {
					throw new Error("DSF-" + x.name + " failed; " + e.message);
				}
			});
		});

		return runDsf.bind(null, dsf);
	}

	function runDsf(dsf, value) {
		if (dsf) {
			for (var i = 0; i < dsf.length; i++) {
				var res = dsf[i] (value);
				if (res != null) return res;
			}
		}
	}

	// function nopDsf(/*value*/) {
	// }

	public static boolean isInvalidDsfChar(char c) {
		return c == '{' || c == '}' || c == '[' || c == ']' || c == ',';
	}


	static class Math extends Dsf<Double> {
		public Math() {
			super("Math", "");
		}

		@Override
		public Double parse(String value) {
			return ScriptRuntime.toNumber(value);
		}

		@Override
		public String stringify(Double value) {
			if (value == null) return "null";
			return ScriptRuntime.toString((double) value);
		}
	}


	static class Hex extends Dsf<Integer> {
		Pattern pattern = Pattern.compile("0x[\\dA-Fa-f]+");

		public Hex() {
			super("Hex", "parse hexadecimal numbers prefixed with 0x");
		}

		@Override
		public Integer parse(String value) {
			if (pattern.matcher(value).find()) {
				return Integer.parseInt(value, 16);
			} else {
				return null;
			}
		}

		@Override
		public String stringify(Integer value) {
			return null;
		}
	}

	static abstract class Dsf<T> {
		public final String description, name;
		public HjsonOptions options;

		public Dsf(String name, String description) {
			this.name = name;
			this.description = description;
		}

		public abstract T parse(String value);

		public abstract String stringify(T value);
	}

}

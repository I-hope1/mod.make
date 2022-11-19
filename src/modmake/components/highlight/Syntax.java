package modmake.components.highlight;

import arc.graphics.Color;
import arc.struct.IntMap;
import mindustry.graphics.Pal;
import modmake.components.TextAreaTable;
import modmake.components.TextAreaTable.MyTextArea;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.COMMENTS;

public class Syntax {
	static final Pattern
			whiteSpaceP = Pattern.compile("(\\s+)"),
			stringP = Pattern.compile("(([\"'`]).*?(?<!\\\\)\\2)", COMMENTS),
			operatCharP = Pattern.compile("([~|,+=*/\\-<>!]+)", COMMENTS),
			bracketsP = Pattern.compile("([\\[{()}\\]]+)", COMMENTS),
			others = Pattern.compile("([\\s\\S])")
					// ,whitespace = Pattern.compile("(\\s+)")
					;

	static final Color
			stringC = Color.valueOf("#ce9178"),
			keywordC = Color.valueOf("#569cd6"),
			numberC = Color.valueOf("#b5cea8"),
			commentC = Color.valueOf("#6a9955"),
			bracketsC = Color.valueOf("#ffd704"),
			operatCharC = Pal.accentBack,
			functionsC = Color.sky,//Color.valueOf("#ae81ff")
			objectsC = Color.valueOf("#66d9ef");
	/*public static class Node {
	 *//* true: 1, false: 0 *//*
		public boolean has;
		public Node parent;
		public Node left;
		public Node right;

		public Node(boolean has, Node parent, Node left, Node right) {
			this.has = has;
			this.parent = parent;
			this.left = left;
			this.right = right;
		}

		static Node currentNode = null;

		static Node node(boolean has, Node parent, Node left, Node right) {
			Node node = new Node(has, parent, left, right);
			currentNode = node;
			return node;
		}

		static Node node(boolean has, Node left, Node right) {
			return node(has, currentNode, left, right);
		}

	}*/

	public final TextAreaTable areaTable;
	public DrawTask[] taskArr;
	public DrawTask task = null;
	Color defaultColor = Color.white;
	char c;
	char lastChar;
	int len;

	public Syntax(TextAreaTable table) {
		areaTable = table;
		area = areaTable.getArea();
	}


	public boolean isWordBreak(char c) {
		return !((48 <= c && c <= 57) || (65 <= c && c <= 90) || (97 <= c && c <= 122) || (19968 <= c && c <= 40869));
	}

	public void drawDefText(int start, int max) {
		area.font.setColor(defaultColor);
		area.drawMultiText(displayText, start, max);
	}


	public MyTextArea area;
	public String displayText;


	public void highlightingDraw(String displayText) {
		this.displayText = displayText;
		task = null;
		// String result;
		for (DrawTask drawTask : taskArr) {
			drawTask.reset();
		}
		int lastIndex = 0;
		len = displayText.length();
		lastChar = '\n';
		out:
		for (int i = 0; i < len; i++, lastChar = c) {
			c = displayText.charAt(i);

			if (task == null) {
				for (DrawTask drawTask : taskArr) {
					if (drawTask.draw(i)) {
						if (drawTask.isFinished()) {
							drawTask.drawText(i);
							drawTask.reset();
							lastIndex = i + 1;
							continue out;
						}
						task = drawTask;
						break;
					}
					drawTask.reset();
				}
			} else if (task.draw(i)) {
				if (task.isFinished()) {
					task.drawText(i);
					task.reset();
					lastIndex = i + 1;
					task = null;
				}
			} else {
				task = null;
			}
			if (task == null) {
				if (lastIndex < i + 1) {
					drawDefText(lastIndex, i + 1);
					lastIndex = i + 1;
				}
			}
		}
		if (task != null && task.crazy) {
			task.drawText(len - 1);
		} else if (lastIndex < len) {
			drawDefText(lastIndex, len);
		}
	}

	public abstract class DrawTask {
		final Color color;
		boolean crazy;
		int lastIndex;

		public DrawTask(Color color, boolean crazy) {
			this.color = color;
			this.crazy = crazy;
		}

		public DrawTask(Color color) {
			this(color, false);
		}


		void reset() {
			lastIndex = -1;
		}

		abstract boolean isFinished();

		abstract boolean draw(int i);

		public void drawText(int i) {
			if (lastIndex == -1) return;
			area.font.setColor(color);
			area.drawMultiText(displayText, lastIndex, i + 1);
		}

		public boolean nextIs(int i, char c) {
			return i + 1 < len && c == displayText.charAt(i + 1);
		}
	}

	public class DrawWord extends DrawTask {
		IntMap<?> total;
		IntMap<?> current;

		DrawWord(IntMap<?> total, Color color) {
			super(color);
			this.total = current = total;
		}

		@Override
		void reset() {
			super.reset();
			current = total;
		}

		@Override
		boolean isFinished() {
			return current == total;
		}

		boolean draw(int i) {
			if (!current.containsKey(c)) return false;

			if (lastIndex == -1) lastIndex = i;
			current = (IntMap<?>) current.get(c);
			if (current.size == 0) {
				if (i + 1 < len && !isWordBreak(displayText.charAt(i + 1))) return false;
				// result = displayText.substring(lastIndex, i);
				current = total;
				// Log.info("ok");
			}
			return true;
		}

	}

	public class DrawComment extends DrawTask {

		public DrawComment(Color color) {
			super(color, true);
		}

		@Override
		void reset() {
			super.reset();
			body = false;
			finished = false;
		}

		@Override
		boolean isFinished() {
			return finished;
		}

		private boolean finished, body, multi;

		@Override
		boolean draw(int i) {
			if (body) {
				if (multi ? lastChar == '*' && c == '/' : c == '\n' || i + 1 >= len) {
					finished = true;
				}
				return true;
			}
			if (c == '/' && i + 1 < len) {
				char next = displayText.charAt(i + 1);
				if (next == '*' || next == '/') {
					lastIndex = i;
					multi = next == '*';
					body = true;
					return true;
				}
			}
			return false;
		}
	}

	public class DrawString extends DrawTask {

		public DrawString(Color color) {
			super(color, true);
		}

		@Override
		void reset() {
			super.reset();
			leftQuote = rightQuote = false;
		}

		@Override
		boolean isFinished() {
			return rightQuote;
		}

		boolean leftQuote, rightQuote;
		char quote;

		@Override
		boolean draw(int i) {
			if (!leftQuote) {
				if (c == '\'' || c == '"' || c == '`') {
					quote = c;
					leftQuote = true;
					lastIndex = i;
					return true;
				} else {
					return false;
				}
			}
			if (quote == c || (c == '\n' && quote != '`')) {
				rightQuote = true;
				leftQuote = false;
				return true;
			}
			return true;
		}
	}

	public class DrawNumber extends DrawTask {
		public DrawNumber(Color color) {
			super(color);
		}

		@Override
		void reset() {
			super.reset();
			hasSign = false;
			hasE = false;
			hasPoint = false;
			finished = false;
			crazy = false;
			// signIndex = -1;
		}

		@Override
		boolean isFinished() {
			return finished;
		}

		boolean finished = false;
		boolean hasSign, hasE, hasPoint;
		// int signIndex;

		boolean isNumber(char c) {
			return 48 <= c && c <= 57;
		}

		@Override
		boolean draw(int i) {
			if (!hasSign && (c == '+' || c == '-')) {
				if (!hasE) lastIndex = i;
				hasSign = true;
				// signIndex = i;
				return true;
			}
			if (!hasE && c == '.') {
				if (hasPoint) {
					finished = true;
					return true;
				}
				hasPoint = true;
				if (!hasSign) {
					hasSign = true;
					lastIndex = i;
				}
				return true;
			}
			if (!hasE && (c == 'E' || c == 'e')) {
				hasE = true;
				hasSign = false;
				return true;
			}
			if (isNumber(c)) {
				if (!hasE && !isWordBreak(lastChar) && !hasSign) {
					return false;
				}
				crazy = true;
				if (!hasSign && !hasE) {
					hasSign = true;
					lastIndex = i;
				}
				if (i + 1 >= len || isWordBreak(displayText.charAt(i + 1))) {
					finished = true;
				}
				return true;
			}

			return false;
		}
	}

	public class DrawSymbol extends DrawTask {
		final IntMap<Object> symbols;

		public DrawSymbol(IntMap<Object> map, Color color) {
			super(color);
			symbols = map;
		}

		@Override
		void reset() {
			super.reset();
		}

		@Override
		boolean isFinished() {
			return true;
		}

		@Override
		boolean draw(int i) {
			if (symbols.containsKey(c)) {
				lastIndex = i;
				return true;
			}
			return false;
		}
	}
}

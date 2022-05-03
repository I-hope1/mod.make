package modmake.ui;

import arc.files.Fi;
import arc.func.Boolf;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Log;
import rhino.JavaAdapter;
import rhino.NativeJavaObject;
import rhino.ScriptableObject;

import java.util.ArrayList;

public class ImgEditor {
	public static final float[] brushSizes = new float[]{1.0F, 1.5F, 2.0F, 3.0F, 4.0F, 5.0F, 9.0F, 15.0F, 20.0F};
	public StringMap tags = new StringMap();
	public final Stack stack = new Stack();
	private boolean loading;
	public float brushSize = 1.0f;
	public Color drawColor;
	public Fi currentFi = null;
	private static Seq<TileData> currentOp = new Seq<>();
	private Tiles tiles;

	public ImgEditor() {
		tiles = new Tiles(32, 32);
		drawColor = Color.black;
	}

	public boolean isLoading() {
		return loading;
	}

	public void beginEdit(int width, int height) {
		currentFi = null;
		reset();
		loading = true;
		tiles = new Tiles(width, height);

		loading = false;
	}

	public void beginEdit(ImgEditorDialog.Img img) {
		currentFi = img.file;
		reset();
		loading = true;
		tags.putAll(img.tags);

		loading = false;
		tiles = new Tiles(img.pixmap);
	}

	public void beginEdit(Pixmap pixmap) {
		currentFi = null;
		reset();
		tiles = new Tiles(pixmap);
	}

	public void load(Runnable r) {
		loading = true;
		r.run();
		loading = false;
	}

	private void reset() {
		clearOp();
		brushSize = 1.0F;
		drawColor = Color.clear;
		tags = new StringMap();
	}

	public Tile tile(int x, int y) {
		return tiles.getn(x, y);
	}

	public int width() {
		return tiles.tiles.length;
	}

	public int height() {
		return tiles.tiles[0].length;
	}


	public void drawCircle(int x, int y, Cons<Tile> drawer) {
		int clamped = (int) brushSize;

		for (int rx = -clamped; rx <= clamped; ++rx) {
			for (int ry = -clamped; ry <= clamped; ++ry) {
				if (Mathf.within((float) rx, (float) ry, brushSize - 0.5F + 1.0E-4F)) {
					int wx = x + rx;
					int wy = y + ry;
					if (wx >= 0 && wy >= 0 && wx < width() && wy < height()) {
						drawer.get(tile(wx, wy));
					}
				}
			}
		}

	}

	public void drawSquare(int x, int y, Cons<Tile> drawer) {
		int clamped = (int) brushSize;

		for (int rx = -clamped; rx <= clamped; ++rx) {
			for (int ry = -clamped; ry <= clamped; ++ry) {
				int wx = x + rx;
				int wy = y + ry;
				if (wx >= 0 && wy >= 0 && wx < width() && wy < height()) {
					drawer.get(tile(wx, wy));
				}
			}
		}

	}

	public void resize(int width, int height) {
		stack.clear();
		clearOp();
		Tiles previous = tiles;
		int offsetX = (width() - width) / 2;
		int offsetY = (height() - height) / 2;
		loading = true;
		Tiles tiles = this.tiles.resize(width, height);

		loading = false;
	}

	public void clearOp() {
		if (currentOp != null) {
			currentOp.clear();
		}
	}

	public void undo() {
		stack.undo();
	}

	public void redo() {
		stack.redo();
	}

	public boolean canUndo() {
		return stack.canUndo();
	}

	public boolean canRedo() {
		return stack.canRedo();
	}

	public void flushOp() {
		if (!currentOp.isEmpty()) {
			stack.add(currentOp.copy());
			currentOp.clear();
		}
	}

	public static void addTileOp(TileData t) {
		currentOp.add(t);
	}

	public Tiles tiles() {
		return tiles;
	}

	public void drawBlocksReplace(int x, int y) {
		drawBlocks(x, y, (tile) -> tile.get() != Color.clear);
	}

	public void drawBlocks(int x, int y, boolean square, Boolf<Tile> tester) {
		Cons<Tile> drawer = tile -> {
			if (tester.get(tile)) {
				tile.set(drawColor);
			}
		};
		if (square) {
			drawSquare(x, y, drawer);
		} else {
			drawCircle(x, y, drawer);
		}
	}

	public void drawBlocks(int x, int y, Boolf<Tile> tester) {
		drawBlocks(x, y, false, tester);
	}

	public void drawBlocks(int x, int y) {
		drawBlocks(x, y, false, tile -> tile.get() != drawColor);
	}

	public class Tiles {
		public Tile[][] tiles;
		public Pixmap pixmap;
		public int w, h;

		public Tiles(Pixmap pixmap) {
			this.pixmap = pixmap;
			resize(pixmap.width, pixmap.height);
		}

		public Tiles(int w, int h) {
			this(new Pixmap(w, h));
		}

		public Tiles resize(int w, int h) {
			this.w = w;
			this.h = h;
			tiles = new Tile[w][h];
			for (int i = 0; i < w; i++) {
				for (int j = 0; j < h; j++) {
					if (tiles[i][j] == null) {
						tiles[i][j] = new Tile(pixmap, i, j);
					}
				}
			}
			return this;
		}

		public boolean in(int x, int y) {
			return x >= 0 && x < w && y >= 0 && y < h;
		}

		public Tile getn(int x, int y) {
			if (in(x, y)) return tiles[x][y];
			throw new IllegalArgumentException(x + "," + y + "不在" + w + "," + h + "里");
		}

		public void each(Cons<Tile> cons) {
			for (Tile[] ts : tiles) {
				for (Tile t : ts) {
					cons.get(t);
				}
			}
		}
	}
	public static class TileData {
		public Color color;
		public int x, y;
		public TileData(Tile t){
			this.color = t.get();
			this.x = t.x;
			this.y = t.y;
		}
	}

	public static class Tile {
		public Pixmap pixmap;
		public int x, y;

		public Tile(Pixmap pixmap, int x, int y) {
			this.x = x;
			this.y = y;
			this.pixmap = pixmap;
		}

		public void set(Color c) {
			if (get().equals(c)) return;
			addTileOp(new TileData(this));
			pixmap.set(x, pixmap.height - y - 1, c);
		}

		public Color get() {
			return new Color(pixmap.get(x, pixmap.height - y - 1));
		}

	}

	public class Stack {
		ArrayList<Seq<TileData>> list = new ArrayList<>();
		int cur = 0;

		public void add(Seq<TileData> t) {
			if (cur < list.size()) {
				for (int i = cur; i < list.size(); i++) {
					list.remove(i);
				}
			}
			list.add(t);
			cur++;
		}

		public void clear() {
			cur = 0;
			list.clear();
		}

		public void undo() {
			if (canUndo()) {
				Seq<TileData> ts = list.remove(--cur);
				Pixmap p = tiles.pixmap;
				ts.each(t -> {
					p.set(t.x, p.height - t.y - 1, t.color);
				});
				ts.clear();
			}
		}

		public boolean canUndo() {
			return cur > 0 && cur <= list.size();
		}

		public void redo() {
			if (canRedo()) {
				Seq<TileData> ts = list.remove(++cur);
				ts.each(t -> {
					Pixmap p = tile(t.x, t.y).pixmap;
					p.set(t.x, p.height - t.y - 1, t.color);
				});
			}
		}

		public boolean canRedo() {
			return cur >= 0 && cur < list.size();
		}
	}
}
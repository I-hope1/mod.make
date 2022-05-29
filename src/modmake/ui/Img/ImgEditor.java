package modmake.ui.img;

import arc.files.Fi;
import arc.func.Boolf;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.math.Mathf;
import arc.struct.Seq;

import java.util.ArrayList;

import static mindustry.Vars.ui;
import static modmake.IntUI.imgDialog;
import static modmake.ui.img.ImgEditorDialog.Img;

public class ImgEditor {
	public static final float[] brushSizes = {1f, 1.5f, 2f, 3f, 4f, 5f, 9f, 15f, 20f};
	public final Stack stack = new Stack();
	private boolean loading;
	public float brushSize = 1.0f;
	public Color drawColor;
	public Fi currentFi = null;
	private static final Seq<TileData> currentOp = new Seq<>();
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

	public void beginEdit(Img img) {
		currentFi = img.file;
		reset();
		loading = true;
		tiles = new Tiles(img.pixmap);
		loading = false;
	}

	public void beginEdit(Fi fi) {
		Img img;
		if (fi.exists()) {
			img = new Img(fi);
		} else {
			img = new Img(new Pixmap(32, 32));
			img.file = fi;
		}
		try {
			beginEdit(img);
		} catch (Exception e) {
			ui.showException(e);
		}
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
		imgDialog.view.reset();
	}

	public void save() {
		try {
			new Img(tiles().pixmap).toFile(currentFi);
			ui.showInfoFade("@editor.saved");
		} catch (Exception e) {
			ui.showException(e);
		}
	}

	public Tile tile(int x, int y) {
		return tiles.getn(x, y);
	}

	public Pixmap pixmap() {
		return tiles.pixmap;
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
		Pixmap previous = tiles.pixmap;
		int offsetX = (width - width()) / 2;
		int offsetY = (height - height()) / 2;
		loading = true;
		tiles.resize(width, height);
		tiles.pixmap.draw(previous, offsetX, offsetY);

		loading = false;
	}

	public void clearOp() {
		currentOp.clear();
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

	public boolean flushOp() {
		if (!currentOp.isEmpty()) {
			stack.addUndo(currentOp.copy());
			stack.list2.clear();
			currentOp.clear();
			return true;
		}
		return false;
	}

	public static void addTileOp(TileData t) {
		currentOp.add(t);
	}

	public Tiles tiles() {
		return tiles;
	}

	public void drawBlocksReplace(int x, int y) {
		drawBlocks(x, y, (tile) -> tile.color() != Color.clear);
	}

	public void drawBlocks(int x, int y, boolean square, Boolf<Tile> tester) {
		Cons<Tile> drawer = tile -> {
			if (tester.get(tile)) {
				tile.color(drawColor);
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
		drawBlocks(x, y, false, tile -> tile.color() != drawColor);
	}

	public static class Tiles {
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
			if (pixmap == null || pixmap.width != w || pixmap.height != h) pixmap = new Pixmap(w, h);
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
			return null;
//			throw new IllegalArgumentException(x + "," + y + "不在" + w + "," + h + "里");
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

		public TileData(Tile t) {
			this.color = t.color();
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

		public void color(Color c) {
			if (color().equals(c)) return;
			addTileOp(new TileData(this));
			pixmap.setRaw(x, pixmap.height - y - 1, c.rgba());
		}

		public Color color() {
			return new Color(pixmap.get(x, pixmap.height - y - 1));
		}

	}

	public class Stack {
		protected ArrayList<Seq<TileData>> list1 = new ArrayList<>();
		protected ArrayList<Seq<TileData>> list2 = new ArrayList<>();

		public void addUndo(Seq<TileData> seq) {
			list1.add(seq);
		}

		public void addRedo(Seq<TileData> seq) {
			list2.add(seq);
		}

		public void clear() {
			list1.clear();
			list2.clear();
		}

		public void undo() {
			if (canUndo()) {
				var seq = list1.remove(list1.size() - 1);
				addRedo(setPixmap(seq));
			}
		}

		public boolean canUndo() {
			return list1.size() > 0;
		}

		public void redo() {
			if (canRedo()) {
				var seq = list2.remove(list2.size() - 1);
				addUndo(setPixmap(seq));
			}
		}

		public Seq<TileData> setPixmap(Seq<TileData> seq) {
			Pixmap p = tiles.pixmap;
			var seq2 = new Seq<TileData>();
			seq.each(t -> {
				seq2.add(new TileData(tile(t.x, t.y)));
				p.set(t.x, p.height - t.y - 1, t.color);
			});
			seq.clear();
			return seq2;
		}

		public boolean canRedo() {
			return list2.size() > 0;
		}
	}
}
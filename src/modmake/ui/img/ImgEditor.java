package modmake.ui.img;

import arc.files.Fi;
import arc.func.Boolf;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.math.Mathf;
import modmake.IntVars;
import modmake.util.img.MyPixmapIO;
import modmake.util.img.Stack;

import static mindustry.Vars.ui;
import static modmake.IntUI.*;
import static modmake.components.DataHandle.settings;
import static modmake.ui.img.ImgEditorDialog.Img;

public class ImgEditor {
	public static final float[] brushSizes = {1f, 1.5f, 2f, 3f, 4f, 5f, 9f, 15f, 20f, 30f};
	public final Stack stack = new Stack(this);
	public float brushSize = 1.0f;
	public Color drawColor;
	public Fi currentFi = null;
	private Tiles tiles;

	public ImgEditor() {
		tiles = new Tiles(32, 32);
		drawColor = Color.black;
	}

	public void beginEdit(int width, int height) {
		currentFi = null;
		reset();
//		if (tiles != null) tiles.pixmap.dispose();
		tiles = new Tiles(width, height);
	}

	public void beginEdit(Img img) {
		currentFi = img.file;
		reset();
//		if (tiles != null) tiles.pixmap.dispose();
		tiles = new Tiles(img.pixmap);
	}

	public void beginEdit(Fi fi) {
		Img img;
		if (fi.exists()) {
			img = new Img(fi);
		} else {
			img = new Img(new MyPixmap(32, 32));
			img.file = fi;
		}
		try {
			beginEdit(img);
		} catch (Exception e) {
			ui.showException(e);
		}
	}

	public void beginEdit(MyPixmap pixmap) {
		currentFi = null;
		reset();
		tiles = new Tiles(pixmap);
	}

	public void load(Runnable r) {
		r.run();
	}

	private void reset() {
		clearOp();
		brushSize = 1.0f;
		drawColor = Color.clear;
		imgDialog.view.reset();
	}

	public void save() {
		try {
			MyPixmapIO.write(pixmap(), currentFi);
			ui.showInfoFade("@editor.saved");
		} catch (Exception e) {
			ui.showException(e);
		}
	}

	public Tile tile(int x, int y) {
		return tiles.get(x, y);
	}

	public Tile tileRaw(int x, int y) {
		return tiles.getRaw(x, y);
	}

	public MyPixmap pixmap() {
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
				if (Mathf.within((float) rx, (float) ry, brushSize - 0.5f + 0.0004F)) {
					int wx = x + rx;
					int wy = y + ry;
					if (wx >= 0 && wy >= 0 && wx < width() && wy < height()) {
						drawer.get(tileRaw(wx, wy));
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
					drawer.get(tileRaw(wx, wy));
				}
			}
		}

	}

	public void resize(int width, int height) {
		stack.clear();
		clearOp();
//		Pixmap pixmap = Pixmaps.resize(pixmap(), width, height);
		MyPixmap pixmap = new MyPixmap(width, height);
        pixmap.draw(pixmap(), width / 2 - pixmap().width / 2, height / 2 - pixmap().height / 2);
//		pixmap().dispose();
		// TODO dispose
		tiles = new Tiles(pixmap);
		/*MyPixmap previous = tiles.pixmap;
		int offsetX = (width - width()) / 2;
		int offsetY = (height - height()) / 2;
		loading = true;
		tiles.resize(width, height);
		tiles.pixmap.draw(previous, offsetX, offsetY);*/

		view.rebuildCont();

	}

	public void clearOp() {
		stack.tmp = null;
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
//		if (!currentOp.isEmpty()) {
		if (stack.tmp != null) {
//			stack.addUndo(currentOp.copy());
			stack.addUndo(stack.tmp);
			stack.tmp = null;
			stack.redoes.clear();
			/*currentOp.each(t -> {
			});*/
//			view.rebuildCont();
			if (settings.getBool("auto_save_image")) save();
		}
	}

	public Tiles tiles() {
		return tiles;
	}

	public void drawBlocksReplace(int x, int y) {
		drawBlocks(x, y, tile -> tile.colorRgba() != Color.clearRgba);
	}

	public void drawBlocks(int x, int y, boolean square, Boolf<Tile> tester) {
		Cons<Tile> drawer = tile -> {
			if (tester.get(tile)) {
				tile.color(drawColor);
			}
		};
		IntVars.async(null, () -> {
			if (square) {
				drawSquare(x, y, drawer);
			} else {
				drawCircle(x, y, drawer);
			}
		}, () -> {}, false);
	}

	public void drawBlocks(int x, int y, Boolf<Tile> tester) {
		drawBlocks(x, y, false, tester);
	}

	public void drawBlocks(int x, int y) {
		drawBlocks(x, y, false, tile -> tile.colorRgba() != drawColor.rgba());
	}

	public int clampX(int x) {
		return Mathf.clamp(x, 0, width() - 1);
	}
	public int clampY(int y) {
		return Mathf.clamp(y, 0, height() - 1);
	}

	public static class Tiles {
		public Tile[][] tiles;
		public MyPixmap pixmap;
		public int w, h;

		public Tiles(MyPixmap pixmap) {
			this(pixmap, pixmap.width, pixmap.height);
		}

		public Tiles(MyPixmap pixmap, int w, int h) {
			this.pixmap = pixmap;
			this.w = w;
			this.h = h;

			tiles = new Tile[w][h];
			for (int i = 0; i < w; i++) {
				for (int j = 0; j < h; j++) {
					tiles[i][j] = new Tile(pixmap, i, j);
				}
			}
		}

		public Tiles(int w, int h) {
			this(new MyPixmap(w, h), w, h);
		}

		/*public void resize(int w, int h) {
			this.w = w;
			this.h = h;
			tiles = new Tile[w][h];
			if (pixmap == null || pixmap.width != w || pixmap.height != h) pixmap = new MyPixmap(w, h);
			for (int i = 0; i < w; i++) {
				for (int j = 0; j < h; j++) {
					if (tiles[i][j] == null) {
						tiles[i][j] = new Tile(pixmap, i, j);
					}
				}
			}
		}*/

		public boolean in(int x, int y) {
			return x >= 0 && x < w && y >= 0 && y < h;
		}

		public Tile get(int x, int y) {
			if (in(x, y)) return tiles[x][y];
			return null;
//			throw new IllegalArgumentException(x + "," + y + "不在" + w + "," + h + "里");
		}

		public Tile getRaw(int x, int y) {
			return tiles[x][y];
		}

		public void each(Cons<Tile> cons) {
			for (Tile[] ts : tiles) {
				for (Tile t : ts) {
					cons.get(t);
				}
			}
		}
	}

	public static class Tile {
		public MyPixmap pixmap;
		public int x, y;

		public Tile(MyPixmap pixmap, int x, int y) {
			this.x = x;
			this.y = y;
			this.pixmap = pixmap;
		}

		public void color(Color c) {
//			if (colorRgba() == c.rgba()) return;
			color(c.rgba());
			/*addTileOp(new TileData(this));
			pixmap.setRaw(x, pixmap.height - y - 1, c.rgba());*/
		}

		public void color(int color) {
//			addTileOp(new TileData(this));
			pixmap.setRaw(x, pixmap.height - y - 1, color);
//			Pixmaps.drawPixel(view.cont.texture, x, pixmap.height - y - 1, color);
		}

		public int colorRgba() {
			return pixmap.getRaw(x, pixmap.height - y - 1);
		}

		public Color color() {
			return new Color(colorRgba());
		}

	}

	public static class MyPixmap extends Pixmap {

		public MyPixmap(int width, int height) {
			super(width, height);
		}

		public MyPixmap(Fi file) {
			super(file);
		}

		@Override
		public void setRaw(int x, int y, int color) {
			if (color == getRaw(x, y)) return;
//			addTileOp(new TileData(color, x, y));
			if (imgEditor.stack.tmp == null) {
				var out = new Pixmap(width, height);
				out.draw(this);
				imgEditor.stack.tmp = out.pixels;
			}
			super.setRaw(x, y, color);
		}

		@Override
		public void set(int x, int y, int color) {
			if (in(x, y)) {
				setRaw(x, y, color);
			}
		}

		@Override
		public void fill(int color) {
			super.fill(color);
		}
	}
}
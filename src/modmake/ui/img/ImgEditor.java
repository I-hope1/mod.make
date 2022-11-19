package modmake.ui.img;

import arc.files.Fi;
import arc.func.Boolf;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.math.Mathf;
import arc.util.Log;
import modmake.util.img.MyPixmapIO;
import modmake.util.img.Stack;

import java.nio.ByteBuffer;

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

	public void reset() {
		clearOp();
		brushSize = 1.0f;
		drawColor = Color.clear;
		imgEditor.stack.clear();
		imgDialog.view.reset();
	}

	public void save() {
		try {
			MyPixmapIO.write(pixmap(), currentFi);
			if (ui != null) ui.showInfoFade("@editor.saved");
		} catch (Exception e) {
			if (ui != null) ui.showException(e);
			else Log.err(e);
		}
	}

	{
		// 关闭游戏时自动保存
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (settings.getBool("auto_save_image") && stack.tmp != null) save();
		}));
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

	public void drawCircle(int x, int y, int color) {
		drawCircle(x, y, color, __ -> true);
	}

	public void drawCircle(int x, int y, int color, Boolf<Integer> tester) {
		int clamped = (int) brushSize;
		int h = height() - 1;
		for (int rx = -clamped; rx <= clamped; rx++) {
			for (int ry = -clamped; ry <= clamped; ry++) {
				if (Mathf.within(rx, ry, brushSize - 0.5f + 0.0001f)) {
					int wx = x + rx, wy = y + ry;

					if (wx < 0 || wy < 0 || wx >= width() || wy >= height() ||
							!tester.get(tiles.pixmap.getRaw(wx, h - wy))) continue;

					tile(wx, wy).color(color);
					//					tiles.pixmap.setRaw(wx, h - wy, color);
				}
			}
		}
	}

	public void drawSquare(int x, int y, int color, Boolf<Integer> tester) {
		int h = imgEditor.height() - 1;
		int clamped = (int) Math.ceil(imgEditor.brushSize);
		int rx1 = x - clamped, rx2 = x + clamped;
		int ry1 = h - y - clamped, ry2 = h - y + clamped;
		rx1 = imgEditor.clampX(rx1);
		rx2 = imgEditor.clampX(rx2);
		ry1 = imgEditor.clampY(ry1);
		ry2 = imgEditor.clampY(ry2);

		for (int rx = rx1; rx <= rx2; rx++) {
			for (int ry = ry1; ry <= ry2; ry++) {
				if (tester.get(tiles.pixmap.getRaw(rx, ry))) {
					tiles.pixmap.setRaw(rx, ry, color);
				}
			}
		}

	}


	public void drawBlocksReplace(int x, int y) {
		drawBlocks(x, y, color -> color != Color.clearRgba);
	}

	public void drawBlocks(int x, int y, boolean square, Boolf<Integer> tester) {

		if (square) {
			drawSquare(x, y, drawColor.rgba(), tester);
		} else {
			drawCircle(x, y, drawColor.rgba(), tester);
		}
	}

	public void drawBlocks(int x, int y, Boolf<Integer> tester) {
		drawBlocks(x, y, false, tester);
	}

	public void drawBlocks(int x, int y) {
		int toColor = drawColor.rgba();
		drawBlocks(x, y, false, color -> color != toColor);
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

		public MyPixmap(ByteBuffer buffer, int width, int height) {
			super(buffer, width, height);
		}


		public void cache() {
			if (imgEditor.stack.tmp == null) {
				imgEditor.stack.tmp = copy().pixels;
			}
		}

		@Override
		public void setRaw(int x, int y, int color) {
			if (color == getRaw(x, y)) return;
			//			addTileOp(new TileData(color, x, y));
			cache();
			super.setRaw(x, y, color);
		}

		@Override
		public void set(int x, int y, int color) {
			if (in(x, y)) {
				cache();
				pixels.putInt((x + y * width) * 4, color);
			}
		}

		@Override
		public void fill(int color) {
			cache();
			super.fill(color);
		}

		@Override
		public String toString() {
			return pixels.toString();
		}
	}
}
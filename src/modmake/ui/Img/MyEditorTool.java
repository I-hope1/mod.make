package modmake.ui.img;

import arc.func.Boolf;
import arc.func.Cons;
import arc.graphics.Color;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Bresenham2;
import arc.math.geom.Point2;
import arc.struct.IntSeq;
import arc.util.Structs;

import static modmake.IntUI.imgEditor;
import static modmake.IntUI.view;
import static modmake.ui.img.ImgEditor.Tile;

public enum MyEditorTool {
	zoom(KeyCode.v),
	pick(KeyCode.i) {
		public void touched(int x, int y) {
			if (Structs.inBounds(x, y, imgEditor.width(), imgEditor.height())) {
				imgEditor.drawColor = imgEditor.tile(x, y).color();
			}
		}
	},
	line(KeyCode.l, "orthogonal") {
		{
			edit = true;
		}

		public void touchedLine(int x1, int y1, int x2, int y2) {
			if (this.mode == 0) {
				if (Math.abs(x2 - x1) > Math.abs(y2 - y1)) {
					y2 = y1;
				} else {
					x2 = x1;
				}
			}

			Bresenham2.line(x1, y1, x2, y2, (x, y) -> {
				if (!imgEditor.tiles().in(x, y)) return;
				imgEditor.drawBlocks(x, y);
			});
		}
	},
	pencil(KeyCode.b, "replace", "square") {
		{
			edit = true;
			draggable = true;
		}

		public void touched(int x, int y) {
			if (!imgEditor.tiles().in(x, y)) return;

//			Pixmap pix = imgEditor.pixmap();
			if (this.mode == -1) {
				imgEditor.drawBlocks(x, y);
			} else if (this.mode == 0) {
				imgEditor.drawBlocksReplace(x, y);
				imgEditor.tile(x, y).color(imgEditor.drawColor);
			} else if (this.mode == 1) {
				imgEditor.drawBlocks(x, y, true, tile -> true);
				imgEditor.tile(x, y).color(imgEditor.drawColor);
			}

		}
	},
	eraser(KeyCode.e) {
		{
			edit = true;
			draggable = true;
		}

		public void touched(int x, int y) {
			imgEditor.drawCircle(x, y, tile -> {
				tile.color(Color.clear);
			});
		}
	},
	fill(KeyCode.g, "replaceall") {
		IntSeq stack;

		{
			edit = true;
			stack = new IntSeq();
		}

		public void touched(int x, int y) {
			if (Structs.inBounds(x, y, imgEditor.width(), imgEditor.height())) {
				Tile tile = imgEditor.tile(x, y);
				Color destx = tile.color();
				if (destx.equals(imgEditor.drawColor)) {
					return;
				}
				this.fill(x, y, mode == 0, t -> t.color().equals(destx),
						t -> t.color(imgEditor.drawColor));
			}
		}

		void fill(int x, int y, boolean replace, Boolf<Tile> tester, Cons<Tile> filler) {
			int width = imgEditor.width();
			int height = imgEditor.height();
			int x1;
			int cy;
			if (replace) {
				for (x1 = 0; x1 < width; ++x1) {
					for (cy = 0; cy < height; ++cy) {
						Tile tile = imgEditor.tile(x1, cy);
						if (tester.get(tile)) {
							filler.get(tile);
						}
					}
				}
			} else {
				this.stack.clear();
				this.stack.add(Point2.pack(x, y));

				try {
					while (this.stack.size > 0 && this.stack.size < width * height) {
						cy = this.stack.pop();
						int xx = Point2.x(cy);
						int yx = Point2.y(cy);

						x1 = xx;
						while (x1 >= 0 && tester.get(imgEditor.tile(x1, yx))) {
							--x1;
						}

						++x1;
						boolean spanAbove = false;

						for (boolean spanBelow = false; x1 < width && tester.get(imgEditor.tile(x1, yx)); ++x1) {
							filler.get(imgEditor.tile(x1, yx));
							if (!spanAbove && yx > 0 && tester.get(imgEditor.tile(x1, yx - 1))) {
								this.stack.add(Point2.pack(x1, yx - 1));
								spanAbove = true;
							} else if (spanAbove && !tester.get(imgEditor.tile(x1, yx - 1))) {
								spanAbove = false;
							}

							if (!spanBelow && yx < height - 1 && tester.get(imgEditor.tile(x1, yx + 1))) {
								this.stack.add(Point2.pack(x1, yx + 1));
								spanBelow = true;
							} else if (spanBelow && yx < height - 1 && !tester.get(imgEditor.tile(x1, yx + 1))) {
								spanBelow = false;
							}
						}
					}

					this.stack.clear();
				} catch (OutOfMemoryError var12) {
					this.stack = null;
					System.gc();
					var12.printStackTrace();
					this.stack = new IntSeq();
				}
			}
		}
	},
	spray(KeyCode.r, "replace") {
		final double chance = 0.012D;

		{
			edit = true;
			draggable = true;
		}

		public void touched(int x, int y) {
			if (this.mode == -1) {
				imgEditor.drawCircle(x, y, tile -> {
					if (Mathf.chance(0.012D)) {
						tile.color(imgEditor.drawColor);
					}

				});
			} else if (this.mode == 0) {
				imgEditor.drawBlocks(x, y, tile -> Mathf.chance(0.012D) && tile.color() != Color.clear);
			} else {
				imgEditor.drawBlocks(x, y, tile -> Mathf.chance(0.012D));
			}

		}
	},
	select(KeyCode.f, "move") {
		{
			edit = true;
		}


		public void selected(int startX, int startY, int toX, int toY) {
			if (mode != -1) return;
			int x = Math.max(0, Math.min(startX, toX));
			toX = Math.min(Math.max(startX, toX), imgEditor.width());
			startX = x;
			int y = Math.max(0, Math.min(startY, toY));
			toY = Math.min(Math.max(startY, toY), imgEditor.height());
			startY = y;
			view.select.clear();
			for (x = startX; x <= toX; x++) {
				for (y = startY; y <= toY; y++) {
					view.select.add(imgEditor.tile(x, y));
				}
			}
			mode = 0;
//			ui.showInfo("" + view.select.all);
		}

		public void drag(int offsetX, int offsetY) {
			if (mode != 0) return;
			view.select.each(tile -> {
				tile.x += offsetX;
				tile.y += offsetY;
			});
		}
	};

	public static final MyEditorTool[] all = values();
	public final String[] altModes;
	public KeyCode key;
	public int mode;
	public boolean edit;
	public boolean draggable;

	MyEditorTool() {
		mode = -1;
		altModes = new String[]{};
	}

	MyEditorTool(KeyCode code) {
		this();
		key = code;
	}

	MyEditorTool(String... altModes) {
		mode = -1;
		key = KeyCode.unset;
		this.altModes = altModes;
	}

	MyEditorTool(KeyCode code, String... altModes) {
		this.mode = -1;
		this.key = code;
		this.altModes = altModes;
	}

	public void touched(int x, int y) {
	}

	public void touchedLine(int x1, int y1, int x2, int y2) {
	}

	// for select
	public void selected(int x1, int y1, int x2, int y2) {}

	public void drag(int offsetX, int offsetY) {}
}

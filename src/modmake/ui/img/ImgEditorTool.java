package modmake.ui.img;

import arc.func.Boolf;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Bresenham2;
import arc.math.geom.Point2;
import arc.struct.IntSeq;
import arc.util.Log;

import static modmake.IntUI.imgEditor;
import static modmake.IntUI.view;
import static modmake.ui.img.ImgEditor.Tile;

public enum ImgEditorTool {
	zoom(KeyCode.v),
	pick(KeyCode.i) {
		public void touched(int x, int y) {
			if (imgEditor.tiles().in(x, y)) {
				imgEditor.drawColor = imgEditor.tileRaw(x, y).color();
			}
		}
	},
	line(KeyCode.l, "orthogonal") {
		{
			edit = true;
		}

		public void touchedLine(int x1, int y1, int x2, int y2) {
			/*x1 = imgEditor.clampX(x1);
			x2 = imgEditor.clampX(x2);
			y1 = imgEditor.clampY(y1);
			y2 = imgEditor.clampY(y2);*/
			if (mode == 0) {
				if (Math.abs(x2 - x1) > Math.abs(y2 - y1)) {
					y2 = y1;
				} else {
					x2 = x1;
				}
			}

			Bresenham2.line(x1, y1, x2, y2, (x, y) -> {
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
			int h = imgEditor.height() - 1;
			if (this.mode == -1) {
				imgEditor.drawBlocks(x, y);
				/*int clamped = (int) imgEditor.brushSize;
				imgEditor.pixmap().fillCircle(x, h - y, clamped, imgEditor.drawColor.rgba());*/
			} else if (this.mode == 0) {
				imgEditor.drawBlocksReplace(x, y);
				//				imgEditor.tile(x, y).color(imgEditor.drawColor);
			} else if (this.mode == 1) {
				int clamped = (int) Math.ceil(imgEditor.brushSize);
				int rx1     = x - clamped, rx2 = x + clamped;
				int ry1     = h - y - clamped, ry2 = h - y + clamped;
				rx1 = imgEditor.clampX(rx1);
				rx2 = imgEditor.clampX(rx2);
				ry1 = imgEditor.clampY(ry1);
				ry2 = imgEditor.clampY(ry2);
				Pixmap pixmap = imgEditor.pixmap();
				int    color  = imgEditor.drawColor.rgba();
				for (int rx = rx1; rx <= rx2; rx++) {
					for (int ry = ry1; ry <= ry2; ry++) {
						pixmap.setRaw(rx, ry, color);
					}
				}
				//				imgEditor.pixmap().fillRect(x - clamped, h - y - clamped, clamped * 2, clamped * 2, imgEditor.drawColor.rgba());
				//				imgEditor.drawBlocks(x, y, true, tile -> true);
				//				imgEditor.tile(x, y).color(imgEditor.drawColor);
			}

		}
	},
	eraser(KeyCode.e) {
		{
			edit = true;
			draggable = true;
		}

		public void touched(int x, int y) {
			imgEditor.drawCircle(x, y, Color.clearRgba);

			//			y = imgEditor.height() - 1 - y;
			//			imgEditor.pixmap().fillCircle(x, y, (int) imgEditor.brushSize, Color.clearRgba);
		}
	},
	fill(KeyCode.g, "replaceall", "fillall") {
		IntSeq stack;

		{
			edit = true;
			stack = new IntSeq();
		}

		public void touched(int x, int y) {
			if (imgEditor.tiles().in(x, y)) {
				if (mode == 1) {
					imgEditor.pixmap().fill(imgEditor.drawColor.rgba());
					return;
				}
				int destx = imgEditor.tileRaw(x, y).colorRgba();
				if (destx == imgEditor.drawColor.rgba()) {
					return;
				}
				fill(x, y, mode == 0, t -> t.colorRgba() == destx,
				 t -> t.color(imgEditor.drawColor));
			}
		}

		void fill(int x, int y, boolean replace, Boolf<Tile> tester, Cons<Tile> filler) {
			int width = imgEditor.width(), height = imgEditor.height();

			if (replace) {
				//just do it on everything
				imgEditor.tiles().each(tile -> {
					if (tester.get(tile)) {
						filler.get(tile);
					}
				});

			} else {
				//perform flood fill
				int x1;

				stack.clear();
				stack.add(Point2.pack(x, y));

				try {
					while (stack.size > 0 && stack.size < width * height) {
						int popped = stack.pop();
						x = Point2.x(popped);
						y = Point2.y(popped);

						x1 = x;
						while (x1 >= 0 && tester.get(imgEditor.tileRaw(x1, y))) x1--;
						x1++;
						boolean spanAbove = false, spanBelow = false;
						while (x1 < width && tester.get(imgEditor.tileRaw(x1, y))) {
							filler.get(imgEditor.tileRaw(x1, y));

							if (!spanAbove && y > 0 && tester.get(imgEditor.tileRaw(x1, y - 1))) {
								stack.add(Point2.pack(x1, y - 1));
								spanAbove = true;
							} else if (spanAbove && !tester.get(imgEditor.tileRaw(x1, y - 1))) {
								spanAbove = false;
							}

							if (!spanBelow && y < height - 1 && tester.get(imgEditor.tileRaw(x1, y + 1))) {
								stack.add(Point2.pack(x1, y + 1));
								spanBelow = true;
							} else if (spanBelow && y < height - 1 && !tester.get(imgEditor.tileRaw(x1, y + 1))) {
								spanBelow = false;
							}
							x1++;
						}
					}
					stack.clear();
				} catch (OutOfMemoryError e) {
					//hack
					stack = null;
					System.gc();
					Log.err(e);
					stack = new IntSeq();
				}
			}
		}
	},

	spray(KeyCode.r, "replace") {
		final double chance = 0.012;

		{
			edit = true;
			draggable = true;
		}

		public void touched(int x, int y) {
			if (this.mode == -1) {
				imgEditor.drawCircle(x, y, imgEditor.drawColor.rgba(), __ -> Mathf.chance(chance));
			} else if (this.mode == 0) {
				imgEditor.drawBlocks(x, y, color -> Mathf.chance(chance) && color != Color.clearRgba);
			} else {
				imgEditor.drawBlocks(x, y, color -> Mathf.chance(chance));
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
			view.select.init(startX, startY, toX, toY);
			mode = 0;
			//			ui.showInfo("" + view.select.all);
		}

		public void drag(int offsetX, int offsetY) {
			if (mode != 0) return;
			view.select.offsetX += offsetX;
			view.select.offsetY += offsetY;
		}
	};

	public static final ImgEditorTool[] all = values();
	public final        String[]        altModes;
	public              KeyCode         key;
	public              int             mode;
	public              boolean         edit;
	public              boolean         draggable;

	ImgEditorTool() {
		mode = -1;
		altModes = new String[]{};
	}

	ImgEditorTool(KeyCode code) {
		this();
		key = code;
	}

	ImgEditorTool(KeyCode code, String... altModes) {
		this.mode = -1;
		this.key = code;
		this.altModes = altModes;
	}

	public void touched(int x, int y) {
	}

	public void touchedLine(int x1, int y1, int x2, int y2) {
	}

	// for select
	public void selected(int x1, int y1, int x2, int y2) {
	}

	public void drag(int offsetX, int offsetY) {}
}

package modmake.ui.img;

import arc.Core;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.Mathf;
import arc.math.geom.*;
import arc.scene.Element;
import arc.scene.event.*;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Scl;
import arc.util.*;
import mindustry.graphics.Pal;
import mindustry.input.Binding;
import mindustry.ui.GridImage;

import static mindustry.Vars.mobile;
import static modmake.IntUI.*;

public class ImgView extends Element implements GestureDetector.GestureListener {
	ImgEditorTool tool = ImgEditorTool.pencil;
	private float offsetx, offsety;
	private       float     zoom           = 1f;
	public        boolean   settingsChange = false;
	private       boolean   grid           = false;
	private final GridImage image          = new GridImage(0, 0);
	private final Vec2      vec            = new Vec2();
	private final Rect      rect           = new Rect();
	private final Vec2[][]  brushPolygons  = new Vec2[ImgEditor.brushSizes.length][0];

	public static boolean showTransparentCanvas = false;
	boolean drawing;
	int     lastx, lasty;
	int startx, starty;
	float mousex, mousey;
	ImgEditorTool lastTool;
	public Select        select = new Select();
	public TextureRegion cont, background;

	public ImgView() {

		for (int i = 0, len = ImgEditor.brushSizes.length; i < len; i++) {
			float size = ImgEditor.brushSizes[i];
			float mod  = size % 1f;
			brushPolygons[i] = Geometry.pixelCircle(size, (index, x, y) -> Mathf.dst(x, y, index - mod, index - mod) <= size - 0.5f);
		}

		Core.input.getInputProcessors().insert(0, new GestureDetector(20, 0.5f, 2, 0.15f, this));
		this.touchable = Touchable.enabled;

		Point2 firstTouch = new Point2();

		addListener(new InputListener() {
			@Override
			public boolean mouseMoved(InputEvent event, float x, float y) {
				mousex = x;
				mousey = y;
				requestScroll();

				return false;
			}

			@Override
			public void enter(InputEvent event, float x, float y, int pointer, Element fromActor) {
				requestScroll();
			}

			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
				if (pointer != 0) {
					return false;
				}

				if (!mobile && button != KeyCode.mouseLeft && button != KeyCode.mouseMiddle && button != KeyCode.mouseRight) {
					return true;
				}

				if (button == KeyCode.mouseRight) {
					lastTool = tool;
					tool = ImgEditorTool.eraser;
				}

				if (button == KeyCode.mouseMiddle) {
					lastTool = tool;
					tool = ImgEditorTool.zoom;
				}

				mousex = x;
				mousey = y;

				Point2 p = project(x, y);
				lastx = p.x;
				lasty = p.y;
				startx = p.x;
				starty = p.y;
				tool.touched(p.x, p.y);
				firstTouch.set(p);

				rebuildCont();
				drawing = true;
				return true;
			}

			@Override
			public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
				if (!mobile && button != KeyCode.mouseLeft && button != KeyCode.mouseMiddle && button != KeyCode.mouseRight) {
					return;
				}

				drawing = false;

				Point2 p = project(x, y);

				if (tool == ImgEditorTool.line) {
					tool.touchedLine(startx, starty, p.x, p.y);
				}
				if (tool == ImgEditorTool.select) {
					tool.selected(startx, starty, p.x, p.y);
				}

				if (tool.edit) {
					imgEditor.flushOp();
				}

				if ((button == KeyCode.mouseMiddle || button == KeyCode.mouseRight) && lastTool != null) {
					tool = lastTool;
					lastTool = null;
				}

				rebuildCont();
			}

			@Override
			public void touchDragged(InputEvent event, float x, float y, int pointer) {
				mousex = x;
				mousey = y;

				Point2 p = project(x, y);


				if (drawing && tool.draggable && !(p.x == lastx && p.y == lasty)) {
					Bresenham2.line(lastx, lasty, p.x, p.y, (cx, cy) -> tool.touched(cx, cy));
				}

				if (tool == ImgEditorTool.select) {
					tool.drag(p.x - lastx, p.y - lasty);
				}

				if (tool == ImgEditorTool.line && tool.mode == 1) {
					if (Math.abs(p.x - firstTouch.x) > Math.abs(p.y - firstTouch.y)) {
						lastx = p.x;
						lasty = firstTouch.y;
					} else {
						lastx = firstTouch.x;
						lasty = p.y;
					}
				} else {
					lastx = p.x;
					lasty = p.y;
				}

				rebuildCont();
			}
		});
	}

	public ImgEditorTool getTool() {
		return tool;
	}

	public void setTool(ImgEditorTool tool) {
		this.tool = tool;
	}

	public boolean isGrid() {
		return grid;
	}

	public void setGrid(boolean grid) {
		this.grid = grid;
	}

	public void center() {
		offsetx = offsety = 0;
	}

	public void reset() {
		center();
		tool = ImgEditorTool.pencil;
		select.clear();
		if (cont != null) cont.texture.dispose();
		if (background != null) background.texture.dispose();
		cont = null;
		background = null;
		System.gc();
	}

	@Override
	public void act(float delta) {
		super.act(delta);

		if (Core.scene.getKeyboardFocus() == null || !(Core.scene.getKeyboardFocus() instanceof TextField) && !Core.input.keyDown(KeyCode.controlLeft)) {
			float ax = Core.input.axis(Binding.move_x);
			float ay = Core.input.axis(Binding.move_y);
			offsetx -= ax * 15f / zoom;
			offsety -= ay * 15f / zoom;
		}

		if (Core.input.keyTap(KeyCode.shiftLeft)) {
			lastTool = tool;
			tool = ImgEditorTool.pick;
		}

		if (Core.input.keyRelease(KeyCode.shiftLeft) && lastTool != null) {
			tool = lastTool;
			lastTool = null;
		}

		if (Core.scene.getScrollFocus() != this) return;

		zoom += Core.input.axis(Binding.zoom) / 10f * zoom;
		clampZoom();
	}

	private void clampZoom() {
		zoom = Mathf.clamp(zoom, 0.6f, Math.max(imgEditor.width(), imgEditor.height()) / 10f);
	}

	public Point2 project(float x, float y) {
		float ratio     = 1f / ((float) imgEditor.width() / imgEditor.height());
		float size      = Math.min(width, height);
		float sclwidth  = size * zoom;
		float sclheight = size * zoom * ratio;
		x = (x - getWidth() / 2 + sclwidth / 2 - offsetx * zoom) / sclwidth * imgEditor.width();
		y = (y - getHeight() / 2 + sclheight / 2 - offsety * zoom) / sclheight * imgEditor.height();

		return Tmp.p1.set((int) x, (int) y);
	}

	public Vec2 unproject(int x, int y) {
		float ratio     = 1f / ((float) imgEditor.width() / imgEditor.height());
		float size      = Math.min(width, height);
		float sclwidth  = size * zoom;
		float sclheight = size * zoom * ratio;
		float px        = (float) x / imgEditor.width() * sclwidth + offsetx * zoom - sclwidth / 2 + getWidth() / 2;
		float py = (float) y / imgEditor.height() * sclheight
							 + offsety * zoom - sclheight / 2 + getHeight() / 2;
		return vec.set(px, py);
	}

	@Override
	public void draw() {
		float ratio     = 1f / ((float) imgEditor.width() / imgEditor.height());
		float size      = Math.min(width, height);
		float sclwidth  = size * zoom;
		float sclheight = size * zoom * ratio;
		float centerx   = x + width / 2 + offsetx * zoom;
		float centery   = y + height / 2 + offsety * zoom;

		image.setImageSize(imgEditor.width(), imgEditor.height());

		if (!ScissorStack.push(rect.set(x + Core.scene.marginLeft, y + Core.scene.marginBottom, width, height))) {
			return;
		}

		Draw.color(Pal.remove);
		Lines.stroke(2f);
		Vec2 leftBottom = new Vec2(centerx - sclwidth / 2, centery - sclheight / 2);
		Lines.rect(leftBottom.x - 1, leftBottom.y - 1, sclwidth + 2, sclheight + 2);

		float unit = sclwidth / imgEditor.width();
		/*float myUnit = unit * 16f / zoom;
		for (float x = 0; x < sclwidth; x += myUnit) {
			for (int y = 0; y < sclheight; y += myUnit) {
				float x3 = leftBottom.x + x;
				float y3 = leftBottom.y + y;
				float x1 = x3 + myUnit / 4f, x2 = x3 + myUnit * .75f;
				float y1 = y3 + myUnit / 4f, y2 = y3 + myUnit * .75f;
				float _unit = myUnit / 2f;
				Draw.color(Color.gray);
				Fill.rect(x1, y1, _unit, _unit);
				Fill.rect(x2, y2, _unit, _unit);
				Draw.color(Color.lightGray);
				Fill.rect(x1, y2, _unit, _unit);
				Fill.rect(x2, y1, _unit, _unit);
			}
		}*/
		/*if (currentOp.isEmpty() || settingsChange) {
			settingsChange = false;
			buffer.dispose();
			buffer.bind();*/
		Draw.color();
		if (showTransparentCanvas) {
			if (background == null) rebuildBackground();
			if (background != null) Draw.rect(background, centerx, centery, sclwidth, sclheight);
		}
		if (cont == null) {
			cont = new TextureRegion(new Texture(imgEditor.width(), imgEditor.height()));
			rebuildCont();
		}
		//		if (cont.texture == null) {
		//		rebuildCont();
		//		}
		Draw.rect(cont, centerx, centery, sclwidth, sclheight);

		// 选择渲染
		if (select.any()) {
			/*TmpTile first = select.all.first(), last = select.all.peek();
			float firstX = leftBottom.x + (first.x) * unit, firstY = leftBottom.y + (first.y) * unit;
			float lastX = leftBottom.x + (last.x) * unit, lastY = leftBottom.y + (last.y) * unit;
			Draw.color(Color.gray.cpy().a(0.7f));
			Fill.rect(firstX, firstY, lastX - firstX, lastY - firstY);*/
			TextureRegion region    = select.textureRegion;
			float         width     = unit * region.width;
			float         height    = unit * region.height;
			float         bx        = leftBottom.x + unit * select.offsetX;
			float         by        = leftBottom.y + unit * select.offsetY;
			float         offset    = Math.max(width, height) / (float) Math.sqrt(zoom) / 15f;
			float         minX      = bx - offset / 2f - 1, maxX = bx + width + offset / 2f + 1;
			float         minY      = by - offset / 2f - 1, maxY = by + height + offset / 2f + 1;
			int           divisions = Math.max(region.width, region.height) * 2;
			Lines.stroke(offset);
			Draw.color(Color.gray);
			Lines.dashLine(minX, minY, minX, maxY, divisions);
			Lines.dashLine(maxX, minY, maxX, maxY, divisions);
			Lines.dashLine(minX, minY, maxX, minY, divisions);
			Lines.dashLine(minX, maxY, maxX, maxY, divisions);
			Lines.stroke(offset / 2);
			Draw.color(Pal.accent);
			Lines.dashLine(minX, minY, minX, maxY, divisions);
			Lines.dashLine(maxX, minY, maxX, maxY, divisions);
			Lines.dashLine(minX, minY, maxX, minY, divisions);
			Lines.dashLine(minX, maxY, maxX, maxY, divisions);
			//			Lines.stroke(unit / 4f);
			//			Lines.dashLine(bx, by, by + unit * (width + 0.6f), by + unit * (height + 0.6f),
			//					(width + height) / 3);
			Draw.color();

			Draw.rect(region, leftBottom.x + unit * select.offsetX + width / 2f,
			 leftBottom.y + unit * select.offsetY + height / 2f,
			 width, height);
		}

		Draw.reset();

		if (grid) {
			Draw.color(Color.gray);
			image.setBounds(centerx - sclwidth / 2, centery - sclheight / 2, sclwidth, sclheight);
			image.draw();

			Lines.stroke(3f);
			Draw.color(Pal.accent);
			Lines.line(centerx - sclwidth / 2f, centery, centerx + sclwidth / 2f, centery);
			Lines.line(centerx, centery - sclheight / 2f, centerx, centery + sclheight / 2f);

			Draw.reset();
		}

		int index = 0;
		for (int i = 0; i < ImgEditor.brushSizes.length; i++) {
			if (imgEditor.brushSize == ImgEditor.brushSizes[i]) {
				index = i;
				break;
			}
		}

		float scaling = zoom * Math.min(width, height) / imgEditor.width();

		Draw.color(Pal.accent);
		Lines.stroke(Scl.scl(2f));

		//		if (tool != MyEditorTool.fill) {
		if (tool == ImgEditorTool.line && drawing) {
			Vec2  v1 = unproject(startx, starty).add(x, y);
			float sx = v1.x, sy = v1.y;
			Vec2  v2 = unproject(lastx, lasty).add(x, y);

			Lines.poly(brushPolygons[index], sx, sy, scaling);
			Lines.poly(brushPolygons[index], v2.x, v2.y, scaling);
		}

		if ((tool.edit || tool == ImgEditorTool.line && !drawing) && (!mobile || drawing)) {
			Point2 p = project(mousex, mousey);
			Vec2   v = unproject(p.x, p.y).add(x, y);

			//pencil square outline
			if (tool == ImgEditorTool.pencil && tool.mode == 1) {
				Lines.square(v.x + scaling / 2f, v.y + scaling / 2f, scaling * (imgEditor.brushSize + 0.5f));
			} else if (tool == ImgEditorTool.select && tool.mode == -1 && drawing) {
				Vec2  v1 = unproject(startx, starty).add(x, y).cpy();
				Vec2  v2 = unproject(p.x, p.y).add(x, y);
				float x1, x2, y1, y2;
				if (v2.x > v1.x) {
					x1 = v1.x;
					x2 = v2.x;
				} else {
					x1 = v2.x;
					x2 = v1.x;
				}
				if (v2.y > v1.y) {
					y1 = v1.y;
					y2 = v2.y;
				} else {
					y1 = v2.y;
					y2 = v1.y;
				}
				x2 += unit;
				y2 += unit;
				Lines.rect(x1, y1, x2 - x1, y2 - y1);
			} else if (tool == ImgEditorTool.fill || tool == ImgEditorTool.select) {
				//				Lines.stroke(3);
				Lines.square(v.x + scaling / 2, v.y + scaling / 2, scaling / 2f);
			} else {
				Lines.poly(brushPolygons[index], v.x, v.y, scaling);
			}
		}
		/*} else {
			if (tool.edit && (!mobile || drawing)) {
				Point2 p = project(mousex, mousey);
				Vec2 v = unproject(p.x, p.y).add(x, y);
				float offset = scaling / 2f;
				Lines.square(
						v.x + scaling / 2f + offset,
						v.y + scaling / 2f + offset,
						scaling);
			}
		}*/

		Draw.color(Pal.accent);
		Lines.stroke(Scl.scl(3f));
		Lines.rect(x, y, width, height);
		Draw.reset();

		ScissorStack.pop();
	}

	public void rebuildCont() {
		//		var pixmap = imgEditor.pixmap().flipY();
		/*int w = imgEditor.width(), h = imgEditor.height();
		var pixmap = new Pixmap(w, h);
		imgEditor.tiles().each(tile -> {
			int x = tile.x, y = h - tile.y - 1;
			Color color = new Color(tile.color().rgba());
			pixmap.setRaw(x, y, color.rgba());
		});*/
		//		if (cont.texture != null) cont.texture.dispose();
		// 		cont = new TextureRegion(new Texture(imgEditor.pixmap()));
		if (mobile) {
			Pixmap pixmap = new Pixmap(imgEditor.pixmap().pixels, imgEditor.width(), imgEditor.height());
			cont.texture.draw(pixmap);
			pixmap.dispose();
		} else {
			cont.texture.draw(imgEditor.pixmap());
		}
		/*new BaseDialog("") {{
			cont.image(new TextureRegion(new Texture(imgEditor.pixmap()))).grow();
			addCloseButton();
		}}.show();*/
		//		cont.flip(false, true);
	}

	public void rebuildBackground() {
		int w         = imgEditor.width() * 2, h = imgEditor.height() * 2;
		var pixmap    = new Pixmap(w, h);
		int lightGray = Color.lightGray.rgba();
		int gray      = Color.gray.rgba();
		//		if (w % 2 == 1) w--;
		for (int x = 0; x <= w; x += 2) {
			for (int y = 0; y <= h; y += 2) {
				pixmap.set(x, y, lightGray);
				pixmap.set(x + 1, y, gray);
				pixmap.set(x, y + 1, gray);
				pixmap.set(x + 1, y + 1, lightGray);
			}
		}
		background = new TextureRegion(new Texture(pixmap));
	}

	public boolean quiet() {
		return Core.scene == null || Core.scene.getKeyboardFocus() == null
					 || !Core.scene.getKeyboardFocus().isDescendantOf(imgDialog)
					 || !imgDialog.isShown() || tool != ImgEditorTool.zoom ||
					 Core.scene.hit(Core.input.mouse().x, Core.input.mouse().y, true) != this;
	}

	@Override
	public boolean pan(float x, float y, float deltaX, float deltaY) {
		if (quiet()) return false;
		offsetx += deltaX / zoom;
		offsety += deltaY / zoom;
		return false;
	}

	@Override
	public boolean zoom(float initialDistance, float distance) {
		if (quiet()) return false;
		float nzoom = distance - initialDistance;
		zoom += nzoom / 10000f / Scl.scl(1f) * zoom;
		clampZoom();
		return false;
	}

	@Override
	public boolean pinch(Vec2 initialPointer1, Vec2 initialPointer2, Vec2 pointer1, Vec2 pointer2) {
		return false;
	}

	@Override
	public void pinchStop() {
	}

	public static class Select {

		public boolean coverTransparent = false, cut = false, multi = false;
		public int offsetX, offsetY;
		public Pixmap        pixmap;
		public TextureRegion textureRegion = new TextureRegion();

		public void cover() {
			if (pixmap == null) return;
			pixmap.each((x, y) -> {
				var tile = imgEditor.tile(x + offsetX, y + offsetY);
				if (tile != null && (coverTransparent || !pixmap.empty(x, y))) {
					tile.color(pixmap.getRaw(x, y));
				}
			});
			if (!multi) {
				clear();
				ImgEditorTool.select.mode = -1;
			}
			imgEditor.flushOp();
			view.rebuildCont();
		}

		public void init(int startX, int startY, int toX, int toY) {
			startX = imgEditor.clampX(startX);
			startY = imgEditor.clampY(startY);
			toX = imgEditor.clampX(toX);
			toY = imgEditor.clampY(toY);

			offsetX = startX;
			offsetY = startY;
			int width = Math.abs(toX - startX + 1), height = Math.abs(toY - startY + 1);
			pixmap = new Pixmap(width, height);
			//			toClear.clear();
			pixmap.each((x, y) -> {
				var tile = imgEditor.tileRaw(x + offsetX, y + offsetY);
				if (tile != null && tile.colorRgba() != 0) {
					pixmap.setRaw(x, y, tile.colorRgba());
					if (cut) tile.color(Color.clearRgba);
				}
			});
			//			if (cut) toClear.each(t -> t.color(Color.clearRgba));
			textureRegion.set(new Texture(pixmap));
			textureRegion.flip(false, true);
		}

		public boolean any() {
			return textureRegion.texture != null && pixmap != null;
		}

		public void clear() {
			//			Log.info("clearSelect");
			if (pixmap != null) pixmap.dispose();
			if (textureRegion.texture != null) textureRegion.texture.dispose();
			pixmap = null;
			textureRegion.texture = null;
		}

		public void flipX() {
			if (pixmap == null) return;
			pixmap = pixmap.flipX();
			//			textureRegion.texture.draw(pixmap);
			textureRegion.flip(true, false);
		}

		public void flipY() {
			if (pixmap == null) return;
			pixmap = pixmap.flipY();
			textureRegion.flip(false, true);
		}

		public void rotate() {
			if (pixmap == null) return;

			pixmap = Pixmaps.rotate(pixmap, 90);
			textureRegion.set(new Texture(pixmap));
			textureRegion.flip(false, true);
			//			cpy = null;
		}
	}
}

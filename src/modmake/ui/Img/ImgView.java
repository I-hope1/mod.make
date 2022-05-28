package modmake.ui.img;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.ScissorStack;
import arc.input.GestureDetector;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.*;
import arc.scene.Element;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Scl;
import arc.struct.Seq;
import arc.util.Tmp;
import mindustry.editor.MapEditor;
import mindustry.graphics.Pal;
import mindustry.input.Binding;
import mindustry.ui.GridImage;

import static mindustry.Vars.mobile;
import static mindustry.Vars.ui;
import static modmake.IntUI.imgDialog;
import static modmake.IntUI.imgEditor;
import static modmake.components.dataHandle.settings;

public class ImgView extends Element implements GestureDetector.GestureListener {
	MyEditorTool tool = MyEditorTool.pencil;
	private float offsetx, offsety;
	private float zoom = 1f;
	private boolean grid = false;
	private GridImage image = new GridImage(0, 0);
	private Vec2 vec = new Vec2();
	private Rect rect = new Rect();
	private Vec2[][] brushPolygons = new Vec2[MapEditor.brushSizes.length][0];

	boolean drawing;
	int lastx, lasty;
	int startx, starty;
	float mousex, mousey;
	MyEditorTool lastTool;

	public final Seq<TmpTile> allSelected = new Seq<>();

	public void cover() {
		allSelected.each(TmpTile::cover);
		allSelected.clear();
	}

	static class TmpTile {
		public int x, y;
		final Color color;

		public TmpTile(ImgEditor.Tile tile) {
			x = tile.x;
			y = tile.y;
			color = tile.color();
		}

		public void cover() {
			imgEditor.tile(x, y).color(color);
		}
	}


	public ImgView() {

		for (int i = 0; i < MapEditor.brushSizes.length; i++) {
			float size = MapEditor.brushSizes[i];
			float mod = size % 1f;
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
					tool = MyEditorTool.eraser;
				}

				if (button == KeyCode.mouseMiddle) {
					lastTool = tool;
					tool = MyEditorTool.zoom;
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

				if (tool == MyEditorTool.line) {
					tool.touchedLine(startx, starty, p.x, p.y);
				}
				if (tool == MyEditorTool.select) {
					tool.selected(startx, starty, p.x, p.y);
				}

				if (tool.edit && imgEditor.flushOp() && settings.getBool("auto_save_image")) {
					imgEditor.save();
				}

				if ((button == KeyCode.mouseMiddle || button == KeyCode.mouseRight) && lastTool != null) {
					tool = lastTool;
					lastTool = null;
				}

			}

			@Override
			public void touchDragged(InputEvent event, float x, float y, int pointer) {
				mousex = x;
				mousey = y;

				Point2 p = project(x, y);

				if (drawing && tool.draggable && !(p.x == lastx && p.y == lasty)) {
					ui.editor.resetSaved();
					Bresenham2.line(lastx, lasty, p.x, p.y, (cx, cy) -> tool.touched(cx, cy));
				}

				if (tool == MyEditorTool.line && tool.mode == 1) {
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
			}
		});
	}

	public MyEditorTool getTool() {
		return tool;
	}

	public void setTool(MyEditorTool tool) {
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
			tool = MyEditorTool.pick;
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
		zoom = Mathf.clamp(zoom, 0.2f, 20f);
	}

	public Point2 project(float x, float y) {
		float ratio = 1f / ((float) imgEditor.width() / imgEditor.height());
		float size = Math.min(width, height);
		float sclwidth = size * zoom;
		float sclheight = size * zoom * ratio;
		x = (x - getWidth() / 2 + sclwidth / 2 - offsetx * zoom) / sclwidth * imgEditor.width();
		y = (y - getHeight() / 2 + sclheight / 2 - offsety * zoom) / sclheight * imgEditor.height();

		return Tmp.p1.set((int) x, (int) y);
	}

	public Vec2 unproject(int x, int y) {
		float ratio = 1f / ((float) imgEditor.width() / imgEditor.height());
		float size = Math.min(width, height);
		float sclwidth = size * zoom;
		float sclheight = size * zoom * ratio;
		float px = ((float) x / imgEditor.width()) * sclwidth + offsetx * zoom - sclwidth / 2 + getWidth() / 2;
		float py = ((float) (y) / imgEditor.height()) * sclheight
				+ offsety * zoom - sclheight / 2 + getHeight() / 2;
		return vec.set(px, py);
	}

	@Override
	public void draw() {
		float ratio = 1f / ((float) imgEditor.width() / imgEditor.height());
		float size = Math.min(width, height);
		float sclwidth = size * zoom;
		float sclheight = size * zoom * ratio;
		float centerx = x + width / 2 + offsetx * zoom;
		float centery = y + height / 2 + offsety * zoom;

		image.setImageSize(imgEditor.width(), imgEditor.height());

		if (!ScissorStack.push(rect.set(x + Core.scene.marginLeft, y + Core.scene.marginBottom, width, height))) {
			return;
		}

		Draw.color(Pal.remove);
		Lines.stroke(2f);
		Lines.rect(centerx - sclwidth / 2 - 1, centery - sclheight / 2 - 1, sclwidth + 2, sclheight + 2);

		float unit = sclwidth / imgEditor.width();
		Vec2 leftBottom = new Vec2(centerx - sclwidth / 2, centery - sclheight / 2);
		imgEditor.tiles().each(tile -> {
			float x = leftBottom.x + tile.x * unit;
			float y = leftBottom.y + tile.y * unit;
			Color color = tile.color();
			if (color.a < 1) {
				float x1 = x + unit * .25f, x2 = x + unit * .75f;
				float y1 = y + unit * .25f, y2 = y + unit * .75f;
				float _unit = unit / 2f;
				Draw.color(Color.gray);
				Fill.rect(x1, y1, _unit, _unit);
				Fill.rect(x2, y2, _unit, _unit);
				Draw.color(Color.lightGray);
				Fill.rect(x1, y2, _unit, _unit);
				Fill.rect(x2, y1, _unit, _unit);
			}
			Draw.color(tile.color());
			Fill.rect(x + unit / 2f, y + unit / 2f, unit, unit);
		});

		if (allSelected.any()) {
			TmpTile first = allSelected.first(), last = allSelected.peek();
			float firstX = leftBottom.x + (first.x) * unit, firstY = leftBottom.y + (first.y) * unit;
			float lastX = leftBottom.x + (last.x) * unit, lastY = leftBottom.y + (last.y) * unit;
			Draw.color(Color.gray.cpy().a(0.7f));
			Fill.rect(firstX, firstY, lastX - firstX, lastY - firstY);
			allSelected.each(tile -> {
				float x = leftBottom.x + tile.x * unit;
				float y = leftBottom.y + tile.y * unit;
				Color color = tile.color;
				if (color.a < 1) {
					float x1 = x + unit * .25f, x2 = x + unit * .75f;
					float y1 = y + unit * .25f, y2 = y + unit * .75f;
					float _unit = unit / 2f;
					Draw.color(Color.gray);
					Fill.rect(x1, y1, _unit, _unit);
					Fill.rect(x2, y2, _unit, _unit);
					Draw.color(Color.lightGray);
					Fill.rect(x1, y2, _unit, _unit);
					Fill.rect(x2, y1, _unit, _unit);
				}
				Draw.color(tile.color);
				Fill.rect(x + unit / 2f, y + unit / 2f, unit, unit);
			});
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

		if (tool != MyEditorTool.fill) {
			if (tool == MyEditorTool.line && drawing) {
				Vec2 v1 = unproject(startx, starty).add(x, y);
				float sx = v1.x, sy = v1.y;
				Vec2 v2 = unproject(lastx, lasty).add(x, y);

				Lines.poly(brushPolygons[index], sx, sy, scaling);
				Lines.poly(brushPolygons[index], v2.x, v2.y, scaling);
			}


			if ((tool.edit || (tool == MyEditorTool.line && !drawing)) && (!mobile || drawing)) {
				Point2 p = project(mousex, mousey);
				Vec2 v = unproject(p.x, p.y).add(x, y).cpy();

				//pencil square outline
				if (tool == MyEditorTool.pencil && tool.mode == 1) {
					Lines.square(v.x + scaling / 2f, v.y + scaling / 2f, scaling * imgEditor.brushSize);
				} else if (tool == MyEditorTool.select && tool.mode == -1 && drawing) {
//					Vec2 v1 = unproject(startx, starty);
					Vec2 v1 = unproject(startx, starty).add(x, y);
					float sx = v1.x, sy = v1.y;
					Lines.rect(sx, sy, v.x - sx, v.y - sy);
				} else {
					Lines.poly(brushPolygons[index], v.x, v.y, scaling);
				}
			}
		} else {
			if (tool.edit && (!mobile || drawing)) {
				Point2 p = project(mousex, mousey);
				Vec2 v = unproject(p.x, p.y).add(x, y);
				float offset = scaling / 2f;
				Lines.square(
						v.x + scaling / 2f + offset,
						v.y + scaling / 2f + offset,
						scaling);
			}
		}

		Draw.color(Pal.accent);
		Lines.stroke(Scl.scl(3f));
		Lines.rect(x, y, width, height);
		Draw.reset();

		ScissorStack.pop();
	}

	public boolean active() {
		return Core.scene != null && Core.scene.getKeyboardFocus() != null
				&& Core.scene.getKeyboardFocus().isDescendantOf(imgDialog)
				&& imgDialog.isShown() && tool == MyEditorTool.zoom &&
				Core.scene.hit(Core.input.mouse().x, Core.input.mouse().y, true) == this;
	}

	@Override
	public boolean pan(float x, float y, float deltaX, float deltaY) {
		if (!active()) return false;
		offsetx += deltaX / zoom;
		offsety += deltaY / zoom;
		return false;
	}

	@Override
	public boolean zoom(float initialDistance, float distance) {
		if (!active()) return false;
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
}

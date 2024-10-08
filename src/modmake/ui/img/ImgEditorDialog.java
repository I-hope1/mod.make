package modmake.ui.img;

import arc.Core;
import arc.files.Fi;
import arc.func.Cons;
import arc.graphics.*;
import arc.input.KeyCode;
import arc.math.geom.Vec2;
import arc.scene.actions.Actions;
import arc.scene.event.Touchable;
import arc.scene.ui.*;
import arc.scene.ui.ImageButton.ImageButtonStyle;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.util.*;
import mindustry.editor.MapEditor;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import modmake.IntUI;
import modmake.components.MyMod;
import modmake.ui.MyStyles;
import modmake.ui.img.ImgEditor.MyPixmap;
import modmake.util.img.MyPixmapIO;

import java.lang.reflect.*;

import static mindustry.Vars.*;
import static modmake.IntUI.*;

public class ImgEditorDialog extends Dialog {
	public              ImgView         view;
	private             ImgResizeDialog resizeDialog;
	private             ScrollPane      pane;
	private final       BaseDialog      menu;
	private             Table           colorSelection;
	public              Runnable        hiddenRun    = null;
	//currently never read
	private             boolean         shownWithImg = false;
	public static final float           ToolSize     = 58f;

	public ImgEditorDialog() {
		super("");

		background(Styles.black);

		view = new ImgView();

		menu = new BaseDialog("@menu");
		menu.addCloseButton();

		float swidth = 180f;

		menu.cont.table(t -> {
			t.defaults().size(swidth, 60f).padBottom(5).padRight(5).padLeft(5);

			t.button("@save", Icon.save, this::save);

			t.button("@editor.resize", Icon.resize, () -> {
				resizeDialog.show();
				menu.hide();
			});

			t.row();

			t.button("@editor.import", Icon.download, () ->
			 platform.showFileChooser(true, "png", file -> ui.loadAnd(() -> {
				 try {
					 imgEditor.beginEdit(new Img(file));
				 } catch (Exception e) {
					 ui.showException(e);
				 }
			 }))
			);

			t.button("@editor.export", Icon.upload, () -> platform.export(imgEditor.currentFi.nameWithoutExtension(), "png", file -> {
				new Img(imgEditor.pixmap()).toFile(file);
			}));

			t.row();

			t.button("@settings", Icon.settings, () -> IntUI.settingsDialog.show());

			t.row();

			t.button("@quit", Icon.exit, () -> {
				tryExit();
				menu.hide();
			});
			t.button("@save.quit", Icon.exit, () -> {
				save();
				hide();
				menu.hide();
			});
		}).row();


		resizeDialog = new ImgResizeDialog((width, height) -> {
			if (!(imgEditor.width() == width && imgEditor.height() == height)) {
				ui.loadAnd(() -> {
					view.select.cover();
					imgEditor.resize(width, height);
					view.reset();
				});
			}
		});

		setFillParent(true);

		clearChildren();
		margin(0);

		update(() -> {
			if (Core.scene.getKeyboardFocus() instanceof Dialog && Core.scene.getKeyboardFocus() != this) {
				return;
			}

			if (Core.scene != null && Core.scene.getKeyboardFocus() == this) {
				doInput();
			}
		});

		shown(() -> {

			if (!Core.settings.getBool("landscape")) platform.beginForceLandscape();
			imgEditor.clearOp();
			imgEditor.stack.clear();
			Core.scene.setScrollFocus(view);
			if (!shownWithImg) {
				imgEditor.beginEdit(32, 32);
			}
			shownWithImg = false;

			Time.runTask(10f, platform::updateRPC);
		});

		hidden(() -> {
			if (hiddenRun != null) hiddenRun.run();
			imgEditor.reset();
			Time.runTask(0, () -> {
				if (imgEditor.pixmap() != null) imgEditor.pixmap().dispose();
			});
			platform.updateRPC();
			if (!Core.settings.getBool("landscape")) platform.endForceLandscape();
		});

		shown(this::build);
	}


	public @Nullable
	void save() {
		boolean isEditor = state.rules.editor;
		state.rules.editor = false;
		Fi fi = imgEditor.currentFi;

		if (imgEditor.currentFi == null) {
			platform.export("@saveimage", "png", f -> {
				imgEditor.currentFi = f;
				imgEditor.save();
				ui.showInfoFade("@editor.saved");
			});
		} else {
			new Img(imgEditor.tiles().pixmap).toFile(fi);
			ui.showInfoFade("@editor.saved");
		}

		menu.hide();
		state.rules.editor = isEditor;
	}

	public Dialog show(MyMod mod) {
		this.mod = mod;
		return super.show(Core.scene, Actions.sequence(Actions.alpha(1f)));
	}

	@Override
	public Dialog show() {
		throw new RuntimeException();
	}

	MyMod mod;
	@Override
	public void hide() {

		ifl:
		if (imgEditor.currentFi != null) {
			Fi fi = imgEditor.currentFi;
			if (!fi.exists()) break ifl;
			if (fi.parent().name().equals("sprites")) {
				mod.sprites1.put(fi.nameWithoutExtension(), new Pixmap(fi));
			} else if (fi.parent().name().equals("sprites-override")) {
				mod.sprites2.put(fi.nameWithoutExtension(), new Pixmap(fi));
			}
		}
		super.hide(Actions.sequence(Actions.alpha(0f)));
	}

	public void beginEditImg(MyMod mod, Fi file) {
		ui.loadAnd(() -> {
			try {
				shownWithImg = true;
				imgEditor.beginEdit(file);
				show(mod);
			} catch (Exception e) {
				Log.err(e);
				ui.showException("@editor.errorload", e);
			}
		});
	}

	public boolean hasPane() {
		return Core.scene.getScrollFocus() == pane || Core.scene.getKeyboardFocus() != this;
	}

	public void build() {
		clearChildren();
		table(cont -> {
			cont.left();

			cont.pane(MyStyles.nonePane, mid -> {
				 mid.top();

				 Table tools = new Table().top();

				 ButtonGroup<ImageButton> group     = new ButtonGroup<>();
				 Table[]                  lastTable = {null};

				 Cons<ImgEditorTool> addTool = tool -> {

					 ImageButton button = new ImageButton(
						(Icon.icons.containsKey(tool.name()) ? Icon.icons : icons).get(tool.name()), Styles.clearTogglei);
					 button.clicked(() -> {
						 view.setTool(tool);
						 if (lastTable[0] != null) {
							 lastTable[0].remove();
						 }
					 });
					 button.update(() -> button.setChecked(view.getTool() == tool));
					 group.add(button);

					 if (tool.altModes.length > 0) {
						 button.clicked(l -> {
							 if (!mobile) {
								 //desktop: rightclick
								 l.setButton(KeyCode.mouseRight);
							 }
						 }, e -> {
							 //need to double tap
							 if (mobile && e.getTapCount() < 2) {
								 return;
							 }

							 if (lastTable[0] != null) {
								 lastTable[0].remove();
							 }

							 Table table = new Table(Styles.black9);
							 table.defaults().size(300f, 70f);

							 for (int i = 0; i < tool.altModes.length; i++) {
								 int    mode = i;
								 String name = tool.altModes[i];

								 table.button(b -> {
									 b.left();
									 b.marginLeft(6);
									 b.setStyle(MyStyles.clearTogglet);
									 b.add(Core.bundle.get("toolmode." + name)).left();
									 b.row();
									 b.add(Core.bundle.get("toolmode." + name + ".description")).color(Color.lightGray).left();
								 }, () -> {
									 tool.mode = tool.mode == mode ? -1 : mode;
									 table.remove();
								 }).update(b -> b.setChecked(tool.mode == mode));
								 table.row();
							 }

							 table.update(() -> {
								 Vec2 v = button.localToStageCoordinates(Tmp.v1.setZero());
								 table.setPosition(v.x, v.y, Align.topLeft);
								 if (!isShown()) {
									 table.remove();
									 lastTable[0] = null;
								 }
							 });

							 table.pack();
							 table.act(Core.graphics.getDeltaTime());

							 addChild(table);
							 lastTable[0] = table;
						 });
					 }


					 Label mode = new Label("");
					 mode.setColor(Pal.remove);
					 mode.update(() -> mode.setText(tool.mode == -1 ? "" : "M" + (tool.mode + 1) + " "));
					 mode.setAlignment(Align.bottomRight, Align.bottomRight);
					 mode.touchable = Touchable.disabled;

					 tools.stack(button, mode);
				 };

				 tools.defaults().size(ToolSize);

				 tools.button(Icon.menu, Styles.cleari, menu::show);

				 ImageButton grid = tools.button(Icon.grid, Styles.clearTogglei, () -> view.setGrid(!view.isGrid())).get();

				 addTool.get(ImgEditorTool.zoom);

				 tools.row();

				 ImageButton undo = tools.button(Icon.undo, Styles.cleari, imgEditor::undo).get();
				 ImageButton redo = tools.button(Icon.redo, Styles.cleari, imgEditor::redo).get();

				 addTool.get(ImgEditorTool.pick);

				 tools.row();

				 undo.setDisabled(() -> !imgEditor.canUndo());
				 redo.setDisabled(() -> !imgEditor.canRedo());

				 undo.update(() -> undo.getImage().setColor(undo.isDisabled() ? Color.gray : Color.white));
				 redo.update(() -> redo.getImage().setColor(redo.isDisabled() ? Color.gray : Color.white));
				 grid.update(() -> grid.setChecked(view.isGrid()));

				 addTool.get(ImgEditorTool.line);
				 addTool.get(ImgEditorTool.pencil);
				 addTool.get(ImgEditorTool.eraser);

				 tools.row();

				 addTool.get(ImgEditorTool.fill);
				 addTool.get(ImgEditorTool.spray);
				 addTool.get(ImgEditorTool.select);

				 //				ImageButton rotate = tools.button(Icon.right, Styles.cleari, () -> imgEditor.rotation = (imgEditor.rotation + 1) % 4).get();
				 //				rotate.getImage().update(() -> {
				 //					rotate.getImage().setRotation(90);
				 //					rotate.getImage().setOrigin(Align.center);
				 //				});

				 tools.row();

				 mid.add(tools).top().padBottom(-6);

				 mid.row();

				 mid.table(Tex.underline, t -> {
					 Slider slider = new Slider(0, MapEditor.brushSizes.length - 1, 1, false);
					 slider.moved(f -> imgEditor.brushSize = MapEditor.brushSizes[(int) f]);
					 for (int j = 0; j < MapEditor.brushSizes.length; j++) {
						 if (MapEditor.brushSizes[j] == imgEditor.brushSize) {
							 slider.setValue(j);
						 }
					 }

					 String prefix = Core.bundle.get("editor.brush") + ": ";
					 var    label  = new Label(() -> prefix + imgEditor.brushSize);
					 label.setAlignment(Align.center);
					 label.touchable = Touchable.disabled;

					 t.top().stack(slider, label).width(ToolSize * 3f - 20).padTop(4f);
					 t.row();
				 }).padTop(5).growX().top();

				 mid.row();

				 mid.table(t -> {
					 t.defaults().growX();
					 t.button("@editor.center", Icon.move, Styles.cleart, view::center).margin(6).row();
					 t.check("显示透明画布", ImgView.showTransparentCanvas, b -> {
						 ImgView.showTransparentCanvas = b;
					 });
				 }).growX().top();

				 mid.row();

				 mid.table(Tex.underline, t -> {
					 t.table(Tex.underlineWhite, t1 -> t1.add("选择")).growX().row();
					 t.table(Tex.pane, this::buildSelectTable).growX();
				 }).growX().top();

				 mid.row();
			 }).margin(0).left().growY()
			 .get().setScrollingDisabled(false, true);


			cont.table(t -> t.add(view).grow()).grow();

			cont.table(this::addBlockSelection).right().growY();

		}).grow();
	}

	public void buildSelectTable(Table table) {
		table.left().defaults().left();
		table.button("放置", view.select::cover).disabled(__ -> !view.select.any()).growX().row();
		table.check("剪切", view.select.cut, b -> view.select.cut = b).row();
		table.check("覆盖透明", view.select.coverTransparent, b -> {
			view.select.coverTransparent = b;
		}).row();
		table.check("复用", view.select.multi, b -> {
			view.select.multi = b;
		}).row();

		ImageButtonStyle style = MyStyles.clearPartiali;
		table.table(t1 -> {
			t1.defaults().size(ToolSize - 12f);
			t1.button(Icon.flipX, style, view.select::flipX).disabled(b -> !view.select.any());
			t1.button(Icon.flipY, style, view.select::flipY).disabled(b -> !view.select.any());
			t1.button(Icon.rotate, style, view.select::rotate).disabled(b -> !view.select.any());
		}).growX();
	}

	public void doInput() {

		if (Core.input.ctrl()) {
			//alt mode select
			for (int i = 0; i < view.getTool().altModes.length; i++) {
				if (i + 1 < KeyCode.numbers.length && Core.input.keyTap(KeyCode.numbers[i + 1])) {
					view.getTool().mode = i;
				}
			}
		} else {
			for (ImgEditorTool tool : ImgEditorTool.all) {
				if (Core.input.keyTap(tool.key)) {
					view.setTool(tool);
					break;
				}
			}
		}

		if (Core.input.keyTap(KeyCode.escape) || Core.input.keyTap(KeyCode.back)) {
			if (!menu.isShown()) {
				menu.show();
			}
		}

		//ctrl keys (undo, redo, save)
		if (Core.input.ctrl()) {
			if (Core.input.keyTap(KeyCode.z)) {
				imgEditor.undo();
			}

			//more undocumented features, fantastic
			if (Core.input.keyTap(KeyCode.t)) {
				imgEditor.flushOp();
			}

			if (Core.input.keyTap(KeyCode.y)) {
				imgEditor.redo();
			}

			if (Core.input.keyTap(KeyCode.s)) {
				save();
			}

			if (Core.input.keyTap(KeyCode.g)) {
				view.setGrid(!view.isGrid());
			}
		}
	}

	private void tryExit() {
		ui.showConfirm("@confirm", "@editor.unsaved", this::hide);
	}

	private void addBlockSelection(Table cont) {
		colorSelection = new Table();
		pane = new ScrollPane(colorSelection);
		pane.setFadeScrollBars(false);
		pane.setOverscroll(true, false);
		pane.exited(() -> {
			if (pane.hasScroll()) {
				Core.scene.setScrollFocus(view);
			}
		});

		cont.table(search -> {
			search.image(Icon.zoom).padRight(8);
			search.field("", this::rebuildColorSelection)
			 .name("imgEditor/search").maxTextLength(maxNameLength).get().setMessageText("@players.search");
		}).pad(-2);
		cont.row();
		cont.table(Tex.underline, extra -> {
			extra.left();
			Image img;
			extra.add(img = new Image(Tex.whiteui, imgEditor.drawColor)).size(42).padRight(4f);
			img.update(() -> img.setColor(imgEditor.drawColor));
			extra.label(() -> imgEditor.drawColor + "").grow();
		}).growX().left().get().clicked(() -> ui.picker.show(imgEditor.drawColor,
		 c -> imgEditor.drawColor = c.cpy()));
		cont.row();
		cont.add(pane).expandY().growX().top().left();

		rebuildColorSelection("");
	}


	public static ObjectMap<String, Color> Colors = new ObjectMap<>(),
	 Pals                                         = new ObjectMap<>();

	private void rebuildColorSelection(String searchText) {
		colorSelection.clear();

		if (Colors.isEmpty()) getColors();
		final int[] i          = {0};
		Table       colorTable = new Table();
		colorSelection.add(colorTable).row();

		Colors.each((k, c) -> {
			if (!searchText.isEmpty() && !k.toLowerCase().contains(searchText.toLowerCase())
			) return;

			ImageButton button = new ImageButton(Tex.whiteui, Styles.clearTogglei);
			button.getStyle().imageUp = MyStyles.whiteui.tint(c);
			button.clicked(() -> imgEditor.drawColor = c);
			button.resizeImage(8 * 4f);
			button.update(() -> button.setChecked(imgEditor.drawColor == c));
			colorTable.add(button).size(50f).tooltip(k);

			if (++i[0] % 4 == 0) {
				colorTable.row();
			}
		});
		colorSelection.image().color(Color.lightGray).growX().row();

		if (Pals.isEmpty()) getPals();
		final int[] j        = {0};
		Table       palTable = new Table();
		colorSelection.add(palTable).row();

		Pals.each((k, c) -> {
			if (!searchText.isEmpty() && !k.toLowerCase().contains(searchText.toLowerCase())
			) return;

			ImageButton button = new ImageButton(Tex.whiteui, Styles.clearTogglei);
			button.getStyle().imageUp = MyStyles.whiteui.tint(c);
			button.clicked(() -> imgEditor.drawColor = c);
			button.resizeImage(8 * 4f);
			button.update(() -> button.setChecked(imgEditor.drawColor.rgba() == c.rgba()));
			palTable.add(button).size(50f).tooltip(k);

			if (++j[0] % 4 == 0) {
				palTable.row();
			}
		});

		if (i[0] + j[0] == 0) {
			colorSelection.add("@none").color(Color.lightGray).padLeft(80).padTop(10);
		}
	}

	public static void getColors() {
		Field[] fields = Color.class.getFields();
		AccessibleObject.setAccessible(fields, true);
		Color color;
		for (Field field : fields) {
			try {
				color = (Color) field.get(null);
			} catch (Exception ignored) {
				continue;
			}
			Colors.put(field.getName(), color);
		}
	}

	public static void getPals() {
		Field[] fields = Pal.class.getFields();
		AccessibleObject.setAccessible(fields, true);
		for (Field field : fields) {
			try {
				Pals.put(field.getName(), (Color) field.get(null));
			} catch (Exception e) {
				Log.err(e);
			}
		}
	}

	public static class Img {
		public Fi       file;
		public MyPixmap pixmap;

		public Img(Fi fi) {
			file = fi;
			pixmap = new MyPixmap(fi);
		}

		public Img(MyPixmap pixmap) {
			this.pixmap = pixmap;
		}

		public void toFile(Fi fi) {
			MyPixmapIO.write(pixmap, fi);
			//			PixmapIO.writeApix(fi, pixmap);
		}
	}
}

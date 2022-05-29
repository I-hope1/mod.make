package modmake.ui.img;

import arc.Core;
import arc.files.Fi;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.input.KeyCode;
import arc.math.geom.Vec2;
import arc.scene.actions.Actions;
import arc.scene.event.Touchable;
import arc.scene.ui.*;
import arc.scene.ui.layout.Table;
import arc.util.*;
import mindustry.editor.MapEditor;
import mindustry.game.Rules;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import modmake.IntUI;
import modmake.ui.styles;

import java.lang.reflect.Field;

import static mindustry.Vars.*;
import static modmake.IntUI.icons;
import static modmake.IntUI.imgEditor;

public class ImgEditorDialog extends Dialog {
	public ImgView view;
	private ImgResizeDialog resizeDialog;
	private ScrollPane pane;
	private final BaseDialog menu;
	private Table blockSelection;
	public Runnable hiddenRun = null;
	private boolean saved = false; //currently never read
	private boolean shownWithImg = false;

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
		}).row();

		menu.cont.button("@quit", Icon.exit, () -> {
			tryExit();
			menu.hide();
		}).size(swidth * 2f + 10, 60f);

		resizeDialog = new ImgResizeDialog((x, y) -> {
			if (!(imgEditor.width() == x && imgEditor.height() == y)) {
				ui.loadAnd(() -> {
					imgEditor.resize(x, y);
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

			saved = true;
			if (!Core.settings.getBool("landscape")) platform.beginForceLandscape();
			imgEditor.clearOp();
			imgEditor.stack.clear();
			Core.scene.setScrollFocus(view);
			if (!shownWithImg) {
				//clear units, rules and other unnecessary stuff
				logic.reset();
				state.rules = new Rules();
				imgEditor.beginEdit(32, 32);
			}
			shownWithImg = false;

			Time.runTask(10f, platform::updateRPC);

			view.select.clear();
		});

		hidden(() -> {
			if (hiddenRun != null) hiddenRun.run();
			imgEditor.clearOp();
			imgEditor.stack.clear();
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
			platform.export("保存", "png", f -> {
				imgEditor.currentFi = f;
				imgEditor.save();
				ui.showInfoFade("@editor.saved");
			});
		} else {
			new Img(imgEditor.tiles().pixmap).toFile(fi);
			ui.showInfoFade("@editor.saved");
		}

		menu.hide();
		saved = true;
		state.rules.editor = isEditor;
	}

	/**
	 * Called when a built-in map save is attempted.
	 */
	protected void handleSaveBuiltin(Img img) {
		ui.showErrorMessage("@editor.save.overwrite");
	}

	@Override
	public Dialog show() {
		return super.show(Core.scene, Actions.sequence(Actions.alpha(1f)));
	}

	@Override
	public void hide() {
		super.hide(Actions.sequence(Actions.alpha(0f)));
	}

	public void beginEditImg(Fi file) {
		ui.loadAnd(() -> {
			try {
				shownWithImg = true;
				imgEditor.beginEdit(file);
				show();
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
		float size = 58f;

		clearChildren();
		table(cont -> {
			cont.left();

			cont.table(mid -> {
				mid.top();

				Table tools = new Table().top();

				ButtonGroup<ImageButton> group = new ButtonGroup<>();
				Table[] lastTable = {null};

				Cons<MyEditorTool> addTool = tool -> {

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
								int mode = i;
								String name = tool.altModes[i];

								table.button(b -> {
									b.left();
									b.marginLeft(6);
									b.setStyle(Styles.clearTogglet);
									b.add(Core.bundle.get("toolmode." + name)).left();
									b.row();
									b.add(Core.bundle.get("toolmode." + name + ".description")).color(Color.lightGray).left();
								}, () -> {
									tool.mode = (tool.mode == mode ? -1 : mode);
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

				tools.defaults().size(size);

				tools.button(Icon.menu, Styles.cleari, menu::show);

				ImageButton grid = tools.button(Icon.grid, Styles.clearTogglei, () -> view.setGrid(!view.isGrid())).get();

				addTool.get(MyEditorTool.zoom);

				tools.row();

				ImageButton undo = tools.button(Icon.undo, Styles.cleari, imgEditor::undo).get();
				ImageButton redo = tools.button(Icon.redo, Styles.cleari, imgEditor::redo).get();

				addTool.get(MyEditorTool.pick);

				tools.row();

				undo.setDisabled(() -> !imgEditor.canUndo());
				redo.setDisabled(() -> !imgEditor.canRedo());

				undo.update(() -> undo.getImage().setColor(undo.isDisabled() ? Color.gray : Color.white));
				redo.update(() -> redo.getImage().setColor(redo.isDisabled() ? Color.gray : Color.white));
				grid.update(() -> grid.setChecked(view.isGrid()));

				addTool.get(MyEditorTool.line);
				addTool.get(MyEditorTool.pencil);
				addTool.get(MyEditorTool.eraser);

				tools.row();

				addTool.get(MyEditorTool.fill);
				addTool.get(MyEditorTool.spray);
				addTool.get(MyEditorTool.select);

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

					var label = new Label("@editor.brush");
					label.setAlignment(Align.center);
					label.touchable = Touchable.disabled;

					t.top().stack(slider, label).width(size * 3f - 20).padTop(4f);
					t.row();
				}).padTop(5).growX().top();

				mid.row();

				mid.table(Tex.underline, t -> {
					t.table(Tex.underlineWhite, t1 -> t1.add("选择")).growX().row();
					t.table(Tex.pane, select -> {
						select.defaults().width(size * 3f);
						select.button("放置", view.select::cover).disabled(__ -> !view.select.any()).row();
						select.check("剪切", view.select.cut, b -> view.select.cut = b).row();
						select.check("选择透明", view.select.selectTransparent, b -> view.select.selectTransparent = b);
					});
				}).growX().top();

				mid.row();

				mid.table(t -> {
					t.defaults().growX().margin(9f);
					t.button("@editor.center", Icon.move, Styles.cleart, view::center).row();
					t.check("显示透明画布", ImgView.showTransparentCanvas, b -> ImgView.showTransparentCanvas = b);
				}).growX().top();

				mid.row();
			}).margin(0).left().growY();


			cont.table(t -> t.add(view).grow()).grow();

			cont.table(this::addBlockSelection).right().growY();

		}).grow();
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
			for (MyEditorTool tool : MyEditorTool.all) {
				if (Core.input.keyTap(tool.key)) {
					view.setTool(tool);
					break;
				}
			}
		}

		if (Core.input.keyTap(KeyCode.escape)) {
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
		blockSelection = new Table();
		pane = new ScrollPane(blockSelection);
		pane.setFadeScrollBars(false);
		pane.setOverscroll(true, false);
		pane.exited(() -> {
			if (pane.hasScroll()) {
				Core.scene.setScrollFocus(view);
			}
		});

		cont.table(search -> {
			search.image(Icon.zoom).padRight(8);
			search.field("", this::rebuildBlockSelection)
					.name("imgEditor/search").maxTextLength(maxNameLength).get().setMessageText("@players.search");
		}).pad(-2);
		cont.row();
		cont.table(Tex.underline, extra -> {
			extra.left();
			Image img;
			extra.add(img = new Image(Tex.whiteui, imgEditor.drawColor)).size(42);
			img.update(() -> img.setColor(imgEditor.drawColor));
			extra.label(() -> imgEditor.drawColor + "").growX().growY();
		}).growX().left().get().clicked(() -> ui.picker.show(imgEditor.drawColor,
				c -> imgEditor.drawColor = c.cpy()));
		cont.row();
		cont.add(pane).expandY().growX().top().left();

		rebuildBlockSelection("");
	}

	private void rebuildBlockSelection(String searchText) {
		blockSelection.clear();

		int i = 0;

		Table colorTable = new Table();
		blockSelection.add(colorTable).row();
		Field[] fields = Color.class.getDeclaredFields();
		for (Field field : fields) {
			field.setAccessible(true);
			Object color = null;
			try {
				color = field.get(null);
			} catch (Exception ignored) {}
			;
			if (!(color instanceof Color)) continue;
			Color c = (Color) color;

			if ((!searchText.isEmpty() && !field.getName().toLowerCase().contains(searchText.toLowerCase()))
			) continue;

			ImageButton button = new ImageButton(Tex.whiteui, Styles.clearTogglei);
			button.getStyle().imageUp = styles.whiteui.tint(c);
			button.clicked(() -> imgEditor.drawColor = c);
			button.resizeImage(8 * 4f);
			button.update(() -> button.setChecked(imgEditor.drawColor == c));
			colorTable.add(button).size(50f).tooltip(field.getName());

			if (++i % 4 == 0) {
				colorTable.row();
			}
		}

		blockSelection.image().color(Color.lightGray).growX().row();

		int j = 0;
		Table palTable = new Table();
		blockSelection.add(palTable).row();
		fields = Pal.class.getDeclaredFields();
		for (Field field : fields) {
			field.setAccessible(true);
			Object color = null;
			try {
				color = field.get(null);
			} catch (Exception e) {
				Log.err(e);
			}
			if (!(color instanceof Color)) continue;
			Color c = (Color) color;

			if ((!searchText.isEmpty() && !field.getName().toLowerCase().contains(searchText.toLowerCase()))
			) continue;

			ImageButton button = new ImageButton(Tex.whiteui, Styles.clearTogglei);
			button.getStyle().imageUp = styles.whiteui.tint(c);
			button.clicked(() -> imgEditor.drawColor = c);
			button.resizeImage(8 * 4f);
			button.update(() -> button.setChecked(imgEditor.drawColor == c));
			palTable.add(button).size(50f).tooltip(field.getName());

			if (++j % 4 == 0) {
				palTable.row();
			}
		}

		if (i + j == 0) {
			blockSelection.add("@none").color(Color.lightGray).padLeft(80f).padTop(10f);
		}
	}

	public static class Img {
		public Fi file;
		public Pixmap pixmap;

		public Img(Fi fi) {
			file = fi;
			pixmap = new Pixmap(fi);
		}

		public Img(Pixmap pixmap) {
			this.pixmap = pixmap;
		}

		public void toFile(Fi fi) {
			fi.writePng(pixmap);
		}
	}
}

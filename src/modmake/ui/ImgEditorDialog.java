package modmake.ui;

import arc.Core;
import arc.files.Fi;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.input.KeyCode;
import arc.math.geom.Vec2;
import arc.scene.actions.Actions;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.style.Drawable;
import arc.scene.ui.*;
import arc.scene.ui.layout.Table;
import arc.struct.StringMap;
import arc.util.*;
import mindustry.core.GameState.State;
import mindustry.editor.*;
import mindustry.game.Rules;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.io.MapIO;
import mindustry.maps.Map;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import modmake.IntUI;

import java.lang.reflect.Field;

import static mindustry.Vars.*;
import static modmake.IntUI.imgDialog;
import static modmake.IntUI.imgEditor;

public class ImgEditorDialog extends Dialog {
	public ImgView view;
	private ImgInfoDialog infoDialog;
	private MapLoadDialog loadDialog;
	private ImgResizeDialog resizeDialog;
	private ScrollPane pane;
	private BaseDialog menu;
	private Table blockSelection;
	public Runnable hiddenRun = null;
	private boolean saved = false; //currently never read
	private boolean shownWithImg = false;

	public ImgEditorDialog() {
		super("");

		background(Styles.black);

		view = new ImgView();
		infoDialog = new ImgInfoDialog();

		menu = new BaseDialog("@menu");
		menu.addCloseButton();

		float swidth = 180f;

		menu.cont.table(t -> {
			t.defaults().size(swidth, 60f).padBottom(5).padRight(5).padLeft(5);

			t.button("@editor.savemap", Icon.save, this::save);

			t.button("图像信息", Icon.pencil, () -> {
				infoDialog.show();
				menu.hide();
			});

			t.row();

			t.button("@editor.resize", Icon.resize, () -> {
				resizeDialog.show();
				menu.hide();
			});

			t.button("@editor.import", Icon.download, () ->
					platform.showFileChooser(true, "png", file -> ui.loadAnd(() -> {
						try {
							imgEditor.beginEdit(new Img(file));
						} catch (Exception e) {
							ui.showException(e);
						}
					}))
			).row();

			t.button("@editor.export", Icon.upload, () -> platform.export(imgEditor.tags.get("name", "unknown"), "png", file -> {
				new Img(imgEditor.tiles().pixmap).toFile(file);
			}));
		});

		menu.cont.row();

		menu.cont.row();

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

		loadDialog = new MapLoadDialog(map -> ui.loadAnd(() -> {
			try {
				//imgEditor.beginEdit(map);
			} catch (Exception e) {
				ui.showException("@editor.errorload", e);
				Log.err(e);
			}
		}));

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


	public @Nullable void save() {
		boolean isEditor = state.rules.editor;
		state.rules.editor = false;
		String name = imgEditor.tags.get("name", "").trim();
		imgEditor.tags.remove("width");
		imgEditor.tags.remove("height");

		Img[] returned = {null};

		if (imgEditor.currentFi == null) {
			platform.export("保存", "png", f -> {
				imgEditor.currentFi = f;
				imgEditor.tags.put("name", f.nameWithoutExtension());
				returned[0] = new Img(imgEditor.tiles().pixmap);
				returned[0].toFile(imgEditor.currentFi);
				ui.showInfoFade("@editor.saved");
			});
		} else if (name.isEmpty()) {
			infoDialog.show();
			Core.app.post(() -> ui.showErrorMessage("@editor.save.noname"));
		} else {
			returned[0] = new Img(imgEditor.tiles().pixmap);
			returned[0].toFile(imgEditor.currentFi);
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
				imgEditor.beginEdit(new Img(file));
				show();
			} catch (Exception e) {
				Log.err(e);
				ui.showException("@editor.errorload", e);
			}
		});
	}

	public ImgView getView() {
		return view;
	}

	public void resetSaved() {
		saved = false;
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

				Cons<EditorTool> addTool = tool -> {

					ImageButton button = new ImageButton(ui.getIcon(tool.name()), Styles.clearTogglei);
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

				tools.defaults().size(size, size);

				tools.button(Icon.menu, Styles.cleari, menu::show);

				ImageButton grid = tools.button(Icon.grid, Styles.clearTogglei, () -> view.setGrid(!view.isGrid())).get();

				addTool.get(EditorTool.zoom);

				tools.row();

				ImageButton undo = tools.button(Icon.undo, Styles.cleari, imgEditor::undo).get();
				ImageButton redo = tools.button(Icon.redo, Styles.cleari, imgEditor::redo).get();

				addTool.get(EditorTool.pick);

				tools.row();

				undo.setDisabled(() -> !imgEditor.canUndo());
				redo.setDisabled(() -> !imgEditor.canRedo());

				undo.update(() -> undo.getImage().setColor(undo.isDisabled() ? Color.gray : Color.white));
				redo.update(() -> redo.getImage().setColor(redo.isDisabled() ? Color.gray : Color.white));
				grid.update(() -> grid.setChecked(view.isGrid()));

				addTool.get(EditorTool.line);
				addTool.get(EditorTool.pencil);
				addTool.get(EditorTool.eraser);

				tools.row();

				addTool.get(EditorTool.fill);
				addTool.get(EditorTool.spray);

//				ImageButton rotate = tools.button(Icon.right, Styles.cleari, () -> imgEditor.rotation = (imgEditor.rotation + 1) % 4).get();
//				rotate.getImage().update(() -> {
//					rotate.getImage().setRotation(90);
//					rotate.getImage().setOrigin(Align.center);
//				});

				tools.row();

				tools.table(Tex.underline, t -> t.add("@editor.teams"))
						.colspan(3).height(40).width(size * 3f + 3f).padBottom(3);

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

				if (!mobile) {
					mid.table(t -> {
						t.button("@editor.center", Icon.move, Styles.cleart, view::center).growX().margin(9f);
					}).growX().top();
				}

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
			for (EditorTool tool : EditorTool.all) {
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
		cont.add(pane).expandY().top().left();

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

	public class Img {
		public Fi file;
		public Pixmap pixmap;

		public Img(Fi fi) {
			file = fi;
			tags.put("name", fi.nameWithoutExtension());
			pixmap = new Pixmap(fi);
		}

		public Img(Pixmap pixmap) {
			this.pixmap = pixmap;
		}

		public void toFile(Fi fi) {
			fi.writePng(pixmap);
		}

		public StringMap tags = new StringMap();
	}
}
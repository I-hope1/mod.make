package modmake.ui.dialog;

import arc.Core;
import arc.files.Fi;
import arc.func.Cons;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.Table;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.Jval;
import arc.util.serialization.Jval.Jformat;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.ctype.*;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.ui.*;
import mindustry.ui.dialogs.BaseDialog;
import modmake.*;
import modmake.components.*;
import modmake.ui.MyStyles;
import modmake.util.MyReflect;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.regex.Pattern;

import static mindustry.Vars.ui;
import static modmake.IntUI.*;
import static modmake.components.DataHandle.*;
import static modmake.util.Tools.*;
import static modmake.util.load.ContentSeq.cTypeMap;

public class ModDialog extends BaseDialog {
	public MyMod currentMod;
	public static ContentLoader tmpLoader = new ContentLoader();

	/**
	 * 默认为false，在hide之前使用，无需手动改回来。
	 */
	public boolean disabledHidden = false;
	public ModDialog() {
		super("");
		title.setStyle(lstyle);

		hidden(() -> {
			if (disabledHidden) {
				disabledHidden = false;
				return;
			}
			currentMod = null;
			modsDialog.show();
		});
	}

	public static Label.LabelStyle lstyle = new Label.LabelStyle(Fonts.def, Color.white);

	// 语言
	public ObjectMap<String, String> bundles;


	Table desc;
	float w = !Core.graphics.isPortrait() ? 520 : Vars.mobile ? 410 : 440;

	public void load() {
		jsonDialog.load();

		try {
			bundles = MyReflect.getValue(ui.language, "displayNames");
			//			Log.info(bundles);
		} catch (Throwable e) {
			bundles = new ObjectMap<>();
			Log.err(e);
		}

		addCloseButton();

		desc = new Table();
		desc.center();
		desc.defaults().padTop(10).left();

		cont.pane(desc).fillX().fillY().get().setScrollingDisabled(true, false);
	}

	Table getContentTable(MyMod mod) {

		var t = new Table();
		t.center();
		t.defaults().padTop(10).left();
		var contentRoot = mod.root.child("content");

		var cont = new Table();
		cont.defaults().padTop(10).left();
		var body = new Table();
		body.top().left();
		body.defaults().padTop(2).top().left();
		cont.pane(p -> p.add(body).left().growX()).growX()
				.with(pane -> pane.setScrollingDisabled(true, false)).row();

		final Fi[] selectedContent = new Fi[1];
		final boolean[] displayContentSprite = new boolean[1];

		var ref = new Object() {
			Cons<Fi> setup = null;
		};
		ref.setup = content -> {
			selectedContent[0] = content;
			body.clearChildren();

			displayContentSprite[0] = settings.getBool("display-content-sprite");

			if (content.equals(contentRoot)) {
				var cTypes = cTypeMap;
				cTypes.keys().toSeq().each(type -> {
					var f = content.child(type);
					if (f.exists() && !f.isDirectory()) {
						f.deleteDirectory();
					}
					body.button(b -> {
						b.left();
						b.add(types.get(f.name(), f::name));
					}, Styles.defaultb, () -> ref.setup.get(f)).growX().pad(2).padLeft(4).left().row();
				});
				return;
			}

			body.add("@content.info").row();
			var table = new Table();
			final boolean[] ok = {false};
			String[] selfText = {""};

			IntUI.searchTable(table, (p, text) -> {
				selfText[0] = text;

				if (ok[0]) {
					p.getChildren().each(Element::change);
					return;
				}
				ok[0] = true;
				p.clearChildren();
				Seq<Fi> all = content.findAll();
				for (var i = 0; i < all.size; i++) {
					final Fi[] json = {all.get(i)};
					if (!json[0].extEquals("hjson") && !json[0].extEquals("json")) continue;
					var btn = new Button(Styles.defaultb);
					btn.defaults().growX().pad(2).padLeft(4).minWidth(w - 10).left();

					// 包装btn，以控制显示隐藏
					var btnTable = new Table();
					boolean[] shown = {true};
					btnTable.changed(() -> {
						boolean canShow = true;
						Pattern pattern = Pattern.compile(selfText[0], Pattern.CASE_INSENSITIVE);
						try {
							if (!Objects.equals(selfText[0], "") && !pattern.matcher(json[0].nameWithoutExtension()).find())
								canShow = false;
						} catch (Exception e) {canShow = false;}

						if (canShow) {
							if (!shown[0]) {
								shown[0] = true;
								btnTable.add(btn);
							}
						} else {
							shown[0] = false;
							btn.remove();
						}
					});
					btnTable.add(btn);
					p.add(btnTable).growX().left().row();

					btn.left();
					Runnable _setup = () -> {
						btn.clearChildren();
						if (displayContentSprite[0]) {
							var image = btn.image(IntVars.find(mod, json[0].nameWithoutExtension())).size(32).padRight(6).left().get();
							image.clicked(contentSpriteDialog::show);
							if (!Vars.mobile) image.addListener(new HandCursorListener());
						}
						btn.add(json[0].name());
					};
					_setup.run();
					//						p.add(btn).growX().left().row();
					IntUI.longPress(btn, 600, longPress -> {
						if (longPress) {
							ui.showConfirm("@confirm",
									Core.bundle.format("confirm.remove", json[0].nameWithoutExtension()),
									() -> {
										json[0].delete();
										ref.setup.get(selectedContent[0]);
									}
							);
						} else {
							if (!json[0].exists()) {
								ui.showException(new NullPointerException("file(" + json[0] + ")不存在"));
								ref.setup.get(selectedContent[0]);
								return;
							}
							disabledHidden = true;
							hide();
							jsonDialog.show(json[0], mod);
							var listener = new VisibilityListener() {
								@Override
								public boolean hidden() {

									json[0] = jsonDialog.file;
									_setup.run();
									show();
									jsonDialog.removeListener(this);
									return false;
								}
							};
							jsonDialog.addListener(listener);
						}
					});
				}
				//				}, () -> {});
			});
			body.add(table).growX().maxHeight(Core.graphics.getHeight()).row();

			// buttons
			body.table(buttons -> {
				buttons.defaults().growX();
				buttons.button("@back", Icon.left, () -> ref.setup.get(contentRoot)).growX();
				buttons.button("@add", Icon.add, () -> {
					new Dialog("") {{
						var name = new TextField();

						cont.table(t -> {
							t.add("@name");
							t.add(name).growX();
						}).growX().row();
						var table = new Table();
						Seq<Jval> values = new Seq<>();
						final int[] selected = {0};
						final int[] j = {0};
						boolean[] ok = {false};
						Seq<Button> btns = new Seq<>();
						ObjectMap<String, Jval> map = or(framework.get(content.name()), ObjectMap::new);
						int cols = 2;
						map.each((key, value) -> {
							if (!ok[0]) {
								int k = j[0];
								btns.add(table.button("@mod.blank-template", MyStyles.clearTogglet, () -> {
									if (selected[0] != -1) btns.get(selected[0]).setChecked(false);
									btns.get(selected[0] = k).setChecked(true);
								}).size(150, 64).get());
								values.add(Jval.valueOf(""));
								if (++j[0] % cols == 0) table.row();
							}
							ok[0] = true;

							int k = j[0];
							btns.add(table.button(key, MyStyles.clearTogglet, () -> {
								if (selected[0] != -1) btns.get(selected[0]).setChecked(false);
								btns.get(selected[0] = k).setChecked(true);
							}).size(150, 64).get());
							values.add(value);
							if (++j[0] % cols == 0) table.row();
						});
						var children = table.getChildren();
						children.get(selected[0]).fireClick();
						table.defaults().width(300);
						cont.pane(table).width(300).height(300).row();
						final UnlockableContent[] selectUnlockContent = {null};
						Button[] __btn = {null};
						__btn[0] = cont.button("@get-from-instance", Styles.flatTogglet, () -> {
							IntUI.showSelectImageTable(__btn[0], Vars.content.getBy(ContentType.valueOf(cTypeMap.get(content.name()))), () -> selectUnlockContent[0], c -> {
								selectUnlockContent[0] = c;
								if (selected[0] != -1) btns.get(selected[0]).setChecked(false);
								selected[0] = -1;
							}, 42, 32, Vars.mobile ? 6 : 10, true);
						}).checked(__ -> selected[0] == -1).growX().height(45).get();

						buttons.button("@back", this::hide).size(150, 64);
						buttons.button("@ok", () -> {
							Fi file = content.child(name.getText() + ".hjson");

							if (selected[0] != -1) {
								file.writeString(values.get(selected[0]).toString(Jval.Jformat.hjson));
							} else {
								ContentLoader lastLoader = Vars.content;
								try {
									Class<?> cls = selectUnlockContent[0].getClass();
									Vars.content = tmpLoader;
									try {
										Constructor<?> constructor = cls.getDeclaredConstructor(String.class);
										constructor.setAccessible(true);
										Content ins = (Content) constructor.newInstance(Time.nanos() + "");

										file.writeString(copyJval(ins, selectUnlockContent[0]).toString(Jformat.hjson));
									} catch (NoSuchMethodException e) {
										file.writeString(copyJval(selectUnlockContent[0], selectUnlockContent[0]).toString(Jformat.hjson));
									}
								} catch (Throwable e) {
									ui.showException("Failed to fetch", e);
									return;
								} finally {
									Vars.content = lastLoader;
								}
							}
							// dialog.hide();
							ref.setup.get(selectedContent[0]);
							hide();
						}).size(150, 64);
						closeOnBack();

						show();
					}};
				}).growX().disabled(__ -> framework.get(content.name()) == null).row();
			}).growX();

		};
		ref.setup.get(contentRoot);

		t.add(cont).growX().width(w).row();

		Fi spritesDirectory1 = mod.root.child("sprites");
		Fi spritesDirectory2 = mod.root.child("sprites-override");

		t.image().color(Pal.accent).growX().row();
		t.button("@view.sprite1", () -> {
			spriteDialog.hiddenRun = () -> {
				ref.setup.get(selectedContent[0]);
				//				mod.loadSprites();
			};
			spriteDialog.setup(spritesDirectory1);
		}).growX().row();
		t.button("@view.sprite1", () -> {
			spriteDialog.hiddenRun = () -> ref.setup.get(selectedContent[0]);
			spriteDialog.setup(spritesDirectory2);
		}).growX().row();
		t.button("@mod.sprite.load", mod::loadSprites).growX().row();

		return t;
	}


	public ModDialog show(MyMod mod) {
		currentMod = mod;
		if ("打开项目加载一次".equals(settings.get("auto_load_sprites"))) mod.loadSprites();

		var meta = mod.meta;
		var displayName = "" + mod.displayName();
		title.setText(displayName);

		desc.clearChildren();

		if (meta.size == 0) {
			desc.add("@error", Color.red);
			show();
			return this;
		}

		if (!(mod.logo() + "").equals("error") && settings.getBool("display_mod_logo")) {
			desc.image(mod.logo()).row();
		}


		desc.add("@editor.name", Color.gray).padRight(10).padTop(0).row();
		desc.add(displayName).growX().wrap().padTop(2).row();

		if (meta.has("author")) {
			desc.add("@editor.author", Color.gray).padRight(10).row();
			desc.add("" + meta.getString("author", "???")).growX().wrap().padTop(2).row();
		}
		if (meta.has("version")) {
			desc.add("@editor.version", Color.gray).padRight(10).row();
			desc.add("" + meta.getString("version", "???")).growX().wrap().padTop(2).row();
		}
		if (meta.has("description")) {
			desc.add("@editor.description").padRight(10).color(Color.gray).top().row();
			desc.add("" + meta.getString("description", "???")).growX().wrap().padTop(2).row();
		}

		Seq<Color> colors = Seq.with(Color.gold, Color.pink, Color.sky);
		Seq<String> names = new Seq<>();
		Seq.with("editor.content", "bundles", "scripts").each(str -> names.add(Core.bundle.get(str, str)));
		//		names.replace(str -> Core.bundle.get(str, str));
		Seq<Table> tables = Seq.with(
				/* content */
				getContentTable(mod),
				/* bundles */
				new Table(MyStyles.whiteui.tint(1, .8f, 1, .8f), t -> {
					t.add("@default").padLeft(4).growX().left();
					t.button(Icon.pencil, MyStyles.clearTransi, () -> {
						disabledHidden = true;
						hide();
						editor.edit(mod.root.child("bundles").child("bundle.properties"), mod);
						var listener = new VisibilityListener() {
							@Override
							public boolean hidden() {
								show();
								editor.removeListener(this);
								return false;
							}
						};
						editor.addListener(listener);
					}).size(42).pad(10).row();
					for (Locale k : Vars.locales) {
						t.add(bundles.get(k + "", () -> k + "")).padLeft(4).growX().left();
						t.button(Icon.pencil, MyStyles.clearTransi, () ->
								editor.edit(mod.root.child("bundles").child("bundle_" + k + ".properties"), mod)
						).size(42).pad(10).row();
						// if (Core.graphics.getWidth() > Core.graphics.getHeight() && i % 2 == 1) t.row();
					}
				}),
				/* scripts */
				new Table(MyStyles.whiteui.tint(.7f, .7f, 1, .8f), t -> {
					t.add("未完成");
				})
		);

		desc.row();

		desc.add(new IntTab(-1, names, colors, tables).build()).growX().minWidth(w);

		show();
		return this;
	}
}

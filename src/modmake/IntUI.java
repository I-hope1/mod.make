package modmake;

import arc.Core;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.TextureRegion;
import arc.input.KeyCode;
import arc.math.Interp;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.actions.Actions;
import arc.scene.event.ChangeListener.ChangeEvent;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.Table;
import arc.struct.*;
import arc.util.*;
import arc.util.Timer.Task;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.type.*;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Block;
import modmake.components.DataHandle;
import modmake.components.Window.DisposableWindow;
import modmake.components.input.area.TextAreaTable;
import modmake.ui.*;
import modmake.ui.dialog.*;
import modmake.ui.img.*;
import modmake.util.Tools;
import modmake.util.tools.Search;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static mindustry.Vars.*;
import static modmake.IntVars.mod;
import static modmake.util.BuildContent.*;
import static modmake.util.tools.Tools.*;

public class IntUI {
	public static ObjectMap<String, TextureRegionDrawable> icons = new ObjectMap<>();

	// 加载icons
	static {
		mod.root.child("icons").findAll(f -> f.extEquals("png")).each(f -> {
			icons.put(f.nameWithoutExtension(), new TextureRegionDrawable(new TextureRegion(new Texture(f))));
		});
	}

	public static Frag             frag           = new Frag();
	public static ImgEditor        imgEditor      = new ImgEditor();
	public static ImgEditorDialog  imgDialog      = new ImgEditorDialog();
	public static ImgView          view           = imgDialog.view;
	public static MySettingsDialog settingsDialog = new MySettingsDialog();

	public static ModsDialog modsDialog = new ModsDialog();
	// public static ModMetaDialog modMetaDialog = new ModMetaDialog();
	// public static JsonDialog jsonDialog = new JsonDialog();
	// public static ContentSpriteDialog contentSpriteDialog = new ContentSpriteDialog();
	public static Editor     editor     = new Editor();
	// public static ModDialog modDialog = new ModDialog();

	public static NameDialog nameDialog = new NameDialog();


	/**
	 * Argument format:
	 * <br>0) button name
	 * <br>1) description
	 * <br>2) icon name
	 * <br>3) listener
	 */
	public static void createDialog(String title, Object... arguments) {
		BaseDialog dialog = new BaseDialog(title);

		float h = 90f;

		dialog.cont.defaults().size(360f, h).padBottom(5).padRight(5).padLeft(5);

		for (int i = 0; i < arguments.length; i += 4) {
			String   name        = (String) arguments[i];
			String   description = (String) arguments[i + 1];
			Drawable icon        = arguments[i + 2] != null ? (Drawable) arguments[i + 2] : Tex.clear;
			Runnable listenable  = (Runnable) arguments[i + 3];

			TextButton button = dialog.cont.button(name, () -> {
				listenable.run();
				dialog.hide();
			}).left().margin(0).get();

			button.clearChildren();
			button.image(icon).padLeft(10);
			button.table(t -> {
				t.add(name).growX().wrap();
				t.row();
				t.add(description).color(Color.gray).growX().wrap();
			}).growX().pad(10f).padLeft(5);

			button.row();

			dialog.cont.row();
		}

		dialog.addCloseButton();
		dialog.show();
	}

	public static void showTextArea(TextField text) {
		BaseDialog dialog = new BaseDialog("");
		dialog.title.remove();
		var areaTable = new TextAreaTable(unpackString(text.getText()));
		// areaTable.syntax = new Syntax(areaTable);
		dialog.cont.add(areaTable).grow();
		var area = areaTable.getArea();

		dialog.addCloseListener();
		dialog.buttons.defaults().growX();
		dialog.buttons.button("@back", Icon.left, dialog::hide).grow();

		dialog.buttons.button("@edit", Icon.edit, () -> new Dialog("") {{
			addCloseButton();
			table(Tex.button, t -> {
				var style = Styles.cleart;
				t.defaults().size(280, 60).left();
				t.row();
				t.button("@schematic.copy.import", Icon.download, style, () -> {
					dialog.hide();
					area.setText(Core.app.getClipboardText().replace("\r", "\n"));
				}).marginLeft(12);
				t.row();
				t.button("@schematic.copy", Icon.copy, style, () -> {
					dialog.hide();
					Core.app.setClipboardText(area.getText()
					 .replaceAll("\r", "\n"));
				}).marginLeft(12);
			});
			closeOnBack();
			show();
		}}).grow();
		dialog.buttons.button("@ok", Icon.ok, () -> {
			dialog.hide();
			text.setText(packString(area.getText()));
			text.fire(new ChangeEvent());
		}).grow();

		dialog.show();
	}


	public static <T extends Element> T doubleClick(T elem, Runnable click, Runnable dclick) {
		elem.addListener(new ClickListener() {
			final Task clickTask = new Task() {
				@Override
				public void run() {
					click.run();
				}
			};

			@Override
			public void clicked(InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				if (clickTask.isScheduled()) {
					dclick.run();
					clickTask.cancel();
				} else {
					Timer.schedule(clickTask, 0.25f);
				}
			}
		});

		return elem;
	}

	public static void searchTable(Table table, Cons2<Table, String> fun) {
		Table[] p = {null};
		table.table(t -> {
			t.image(Icon.zoom);
			TextField text = new TextField();
			t.add(text).growX();
			text.changed(() -> {
				fun.get(p[0], text.getText());
			});
			/* 自动聚焦到搜索框 */
			if (Core.app.isDesktop()) {
				Core.scene.setKeyboardFocus(text);
			}
		}).padRight(8).growX().fill().top().row();

		var pane = table.top().pane(p1 -> fun.get(p1.top(), "")).pad(0).top().get();
		pane.setScrollingDisabled(true, false);

		p[0] = (Table) pane.getWidget();

		table.pack();
	}

	/**
	 * 弹出一个小窗，自己设置内容
	 *
	 * @param button     用于定位弹窗的位置
	 * @param f          (p, hide, text)
	 *                   p 是Table，你可以添加元素
	 *                   hide 是一个函数，调用就会关闭弹窗
	 *                   text 如果 searchable 为 true ，则启用。用于返回用户在搜索框输入的文本
	 * @param searchable 可选，启用后会添加一个搜索框
	 */
	public static <T extends Button> Table
	showSelectTable(T button, Cons3<Table, Runnable, String> f, boolean searchable) {
		if (button == null) {
			throw new NullPointerException("button cannot be null");
		}
		Table t = new Table(Tex.button) {
			public float getPrefHeight() {
				return Math.min(super.getPrefHeight(), Core.graphics.getHeight());
			}

			public float getPrefWidth() {
				return Math.min(super.getPrefWidth(), Core.graphics.getWidth());
			}
		};
		Element hitter = new Element();
		Runnable hide = () -> {
			hitter.remove();
			t.actions(Actions.fadeOut(0.3f, Interp.fade), Actions.remove());
		};
		hitter.clicked(hide);
		hitter.fillParent = true;

		Core.scene.add(hitter);
		Core.scene.add(t);

		t.update(() -> {
			if (button.parent == null || !button.isDescendantOf(Core.scene.root)) {
				Core.app.post(hide);
				return;
			}

			button.localToStageCoordinates(Tmp.v1.set(button.getWidth() / 2f, button.getHeight() / 2f));
			t.setPosition(Tmp.v1.x, Tmp.v1.y, Align.center);
			if (t.getWidth() > Core.scene.getWidth())
				t.setWidth(Core.graphics.getWidth());
			if (t.getHeight() > Core.scene.getHeight())
				t.setHeight(Core.graphics.getHeight());
			t.keepInStage();
			t.invalidateHierarchy();
			t.pack();
		});
		t.actions(Actions.alpha(0), Actions.fadeIn(0.3f, Interp.fade));

		Table p = new Table();
		p.top();
		if (searchable) {
			t.table(top -> {
				top.image(Icon.zoom);
				TextField text = new TextField();
				top.add(text).growX();
				text.changed(() -> f.get(p, hide, text.getText()));
				// /* 自动聚焦到搜索框 */
				// text.fireClick();
			}).padRight(8f).growX().fill().top().row();
		}
		f.get(p, hide, "");

		ScrollPane pane = new ScrollPane(p);
		t.top().add(pane).pad(0f).top();
		pane.setScrollingDisabled(true, false);

		t.pack();

		return t;
	}

	public static <T extends Button, E> Table
	showSelectListTable(T button, Seq<E> list, Prov<String> holder,
	                    Cons<E> cons, int width, int height, Boolean searchable) {
		if (list == null) throw new IllegalArgumentException("list cannot be null");
		return showSelectTable(button, (p, hide, text) -> {
			p.clearChildren();

			Pattern pattern = compileRegExp(text);
			if (pattern == null) return;
			list.each(item -> text.isEmpty() || testP(pattern, "" + item)
			                  || testP(pattern, DataHandle.types.get("" + item, () -> "" + item)),
			 item -> {
				 p.button(DataHandle.types.get("" + item, () -> "" + item), Styles.cleart, () -> {
					 cons.get(item);
					 hide.run();
				 }).size(width, height).disabled(Objects.equals(holder.get(), "" + item)).row();
			 });
		}, searchable);
	}


	/**
	 * 弹出一个可以选择内容的窗口（类似物品液体源的选择）
	 * （需要提供图标）
	 *
	 * @param items     用于展示可选的内容
	 * @param icons     可选内容的图标
	 * @param holder    选中的内容，null就没有选中任何
	 * @param size      每个内容的元素大小
	 * @param imageSize 每个内容的图标大小
	 * @param cons      选中内容就会调用
	 * @param cols      一行的元素数量
	 */
	public static <T extends Button, T1> Table
	showSelectImageTableWithIcons(T button, Seq<T1> items, Seq<? extends Drawable> icons,
	                              Prov<T1> holder, Cons<T1> cons,
	                              float size, float imageSize, int cols, boolean searchable) {
		return showSelectTable(button, (Table p, Runnable hide, String text) -> {
			p.clearChildren();
			p.left();
			ButtonGroup<ImageButton> group = new ButtonGroup<>();
			group.setMinCheckCount(0);
			p.defaults().size(size);

			int c = 0;

			Pattern pattern = compileRegExp(text);
			if (pattern == null) return;

			for (int i = 0; i < items.size; i++) {
				T1 item = items.get(i);
				// 过滤不满足条件的
				if (!Objects.equals(text, "") && !(item instanceof String && pattern.matcher("" + item).find())
				    && !Tools.<UnlockableContent, Boolean>as(item,
				 unlock -> pattern.matcher(unlock.name).find() ||
				           pattern.matcher(unlock.localizedName).find(), false)) {
					continue;
				}

				ImageButton btn = p.button(Tex.whiteui, MyStyles.clearToggleTransi, imageSize, () -> {
					cons.get(item);
					hide.run();
				}).size(size).get();
				//				if (!Vars.mobile)
				btn.addListener(new Tooltip(t -> t.background(Tex.button)
				 .add(item + "")));
				btn.getStyle().imageUp = icons.get(i);
				btn.update(() -> {
					T1 hold = holder.get();
					btn.setChecked(item instanceof UnlockableContent && hold instanceof UnlockableContent ? hold.equals(item) : (hold + "").equals(item + ""));
				});

				if (++c % cols == 0) {
					p.row();
				}
			}
		}, searchable);
	}

	/**
	 * 弹出一个可以选择内容的窗口（无需你提供图标）
	 */
	public static <T extends Button, T1 extends UnlockableContent> Table
	showSelectImageTable(T button, Seq<T1> items, Prov<T1> holder, Cons<T1> cons,
	                     float size, int imageSize, int cols, boolean searchable) {
		Seq<Drawable> icons = new Seq<>();
		items.each(item -> icons.add(new TextureRegionDrawable(item.uiIcon)));
		return showSelectImageTableWithIcons(button, items, icons, holder, cons, size, imageSize, cols,
		 searchable);
	}

	/**
	 * 弹出一个可以选择内容的窗口（需你提供图标构造器）
	 */
	public static <T extends Button, T1> Table
	showSelectImageTableWithFunc(T button, Seq<T1> items, Prov<T1> holder, Cons<T1> cons,
	                             float size, int imageSize, int cols, Func<T1, Drawable> func, boolean searchable) {
		Seq<Drawable> icons = new Seq<>();
		items.each(item -> {
			icons.add(func.get(item));
		});
		return showSelectImageTableWithIcons(button, items, icons, holder, cons, size, imageSize, cols, searchable);
	}

	public static <T extends Button> void allContentSelection(T btn, Seq<UnlockableContent> all,
	                                                          Prov<UnlockableContent> holder,
	                                                          Cons<UnlockableContent> cons,
	                                                          float size, int imageSize, boolean searchable) {
		Table[] tableArr = {new Table(), new Table(), new Table(),
		                    new Table(), new Table(), new Table()};
		int length = tableArr.length;
		showSelectTable(btn, (p, hide, v) -> {
			p.clearChildren();
			p.image(Tex.whiteui, Pal.accent).growX().height(3).pad(4).row();

			var cont = p.table().get();

			Pattern reg;
			try {
				reg = Pattern.compile(v, Pattern.CASE_INSENSITIVE);
			} catch (Exception e) {
				return;
			}
			var cols = mobile ? 6 : 10;
			// 清空
			for (Table table1 : tableArr) table1.clearChildren();
			Content current = holder.get();

			// 遍历所有tech
			for (var content : all) {
				if (!reg.matcher(content.name).find() && !reg.matcher(content.localizedName).find()) continue;

				var index = content instanceof Item ? 0
				 : content instanceof Liquid ? 1
				 : content instanceof Block ? 2
				 : content instanceof UnitType ? 3
				 : content instanceof SectorPreset ? 4
				 : 5;
				var table1 = tableArr[index];
				ImageButton button = table1.button(new TextureRegionDrawable(content.uiIcon),
				 MyStyles.clearToggleTransi, imageSize, () -> {
					 cons.get(content);
					 hide.run();
				 }).size(size).get();
				if (current != null) button.setChecked(current == content);

				//						if (!Vars.mobile)
				button.addListener(new Tooltip(tool -> tool.background(Tex.button)
				 .add(content.localizedName)));

				if (table1.getChildren().size % cols == 0) {
					table1.row();
				}
			}

			for (int j = 0; j < length; j++) {
				Table table1 = tableArr[j];
				cont.add(table1).growX().left().row();
				if (table1.getChildren().size != 0 && j < length - 2) {
					cont.image(Tex.whiteui, Pal.accent).growX().height(3).pad(4).row();
				}
			}
		}, true);
	}

	public static <T extends UnlockableContent> Prov<String> selectionWithField(Table table, Seq<T> items,
	                                                                            String current, int size, int imageSize,
	                                                                            int cols, boolean searchable) {
		var field = new TextField(current);
		table.add(field).fillX();
		var btn = table.button(Icon.pencilSmall, MyStyles.clearFulli, () -> { }).size(40).padLeft(-1).get();
		btn.clicked(() -> showSelectImageTable(btn, items,
		 () -> content.getByName(ContentType.item, field.getText()),
		 item -> field.setText(item.name), size, imageSize,
		 cols, searchable)
		);

		return field::getText;
	}

	public static BaseDialog test(Element el) {
		return new BaseDialog("test") {{
			cont.pane(p -> p.add(el)).grow();
			addCloseButton();
			show();
		}};
	}


	/**
	 * 长按事件
	 * @param <T>      the type parameter
	 * @param elem     被添加侦听器的元素
	 * @param duration 需要长按的事件（单位毫秒[ms]，600ms=0.6s）
	 * @param boolc0   {@link Boolc#get(boolean b)}形参{@code b}为是否长按
	 * @return the t
	 */
	public static <T extends Element> T
	longPress(T elem, final long duration, final Boolc boolc0) {
		Boolc boolc = b -> Tools.runLoggedException(() -> boolc0.get(b));
		elem.addCaptureListener(new LongPressListener(boolc, duration));
		return elem;
	}
	public static <T extends Element> T
	longPress(T elem, final Boolc boolc) {
		return longPress(elem, 600, boolc);
	}
	/**
	 * 长按事件
	 * @param <T>      the type parameter
	 * @param elem     被添加侦听器的元素
	 * @param duration 需要长按的事件（单位毫秒[ms]，600ms=0.6s）
	 * @param run      长按时调用
	 * @return the t
	 */
	public static <T extends Element> T
	longPress0(T elem, long duration, Runnable run) {
		return longPress(elem, duration, b -> {
			if (b) run.run();
		});
	}


	public static <T extends Element> T
	rightClick(T elem, Runnable run) {
		elem.addListener(new ClickListener(KeyCode.mouseRight) {
			public void clicked(InputEvent event, float x, float y) {
				run.run();
			}
		});
		return elem;
	}

	/**
	 * long press for mobile
	 * r-click for desktop
	 */
	public static <T extends Element> T
	longPressOrRclick(T element, Consumer<T> run) {
		return mobile ? longPress(element, 600, b -> {
			if (b) run.accept(element);
		}) : rightClick(element, () -> run.accept(element));
	}

	public static void
	addShowMenuListener(Element elem, MenuList... list) {
		longPressOrRclick(elem, __ -> {
			showSelectTableRB(Core.input.mouse().cpy(), (p, hide, ___) -> {
				for (MenuList menu : list) {
					menu.button = p.button(
					 menu.name,
					 Tools.or(menu.icon, Styles.none),
					 Styles.flatt,
					 () -> {
						 menu.run.run();
						 hide.run();
					 }).size(120, 42).get();
					p.row();
				}
			}, false);
		});
	}

	public static final float DEF_DURATION = 0.2f;
	/**
	 * 在鼠标右下弹出一个小窗，自己设置内容
	 *
	 * @param vec2       用于定位弹窗的位置
	 * @param f          (p, hide, text)
	 *                   p 是Table，你可以添加元素
	 *                   hide 是一个函数，调用就会关闭弹窗
	 *                   text 如果 @param 为 true ，则启用。用于返回用户在搜索框输入的文本
	 * @param searchable 可选，启用后会添加一个搜索框
	 */
	public static Table
	showSelectTableRB(Vec2 vec2, Cons3<Table, Runnable, String> f,
	                  boolean searchable) {
		Table t = new Table(Tex.pane) {
			public float getPrefHeight() {
				return Math.min(super.getPrefHeight(), (float) Core.graphics.getHeight());
			}

			public float getPrefWidth() {
				return Math.min(super.getPrefWidth(), (float) Core.graphics.getWidth());
			}
		};
		Element hitter = new Element();
		Runnable hide = () -> {
			hitter.remove();
			t.actions(Actions.fadeOut(DEF_DURATION, Interp.fade), Actions.remove());
		};
		hitter.clicked(hide);
		hitter.fillParent = true;
		Core.scene.add(hitter);
		Core.scene.add(t);
		t.update(() -> {
			Tmp.v1.set(vec2);
			t.setPosition(Tmp.v1.x, Tmp.v1.y, Align.topLeft);
			if (t.getWidth() > Core.scene.getWidth()) {
				t.setWidth((float) Core.graphics.getWidth());
			}

			if (t.getHeight() > Core.scene.getHeight()) {
				t.setHeight((float) Core.graphics.getHeight());
			}

			t.keepInStage();
			t.invalidateHierarchy();
			t.pack();
		});
		t.actions(Actions.alpha(0f), Actions.fadeIn(DEF_DURATION, Interp.fade));
		Table p = new Table();
		p.top();
		if (searchable) {
			new Search((cont, text) -> {
				f.get(cont, hide, text);
			}).build(t, p);
		}

		f.get(p, hide, "");
		ScrollPane pane = new ScrollPane(p);
		t.top().add(pane).pad(0.0f).top();
		pane.setScrollingDisabled(true, false);
		t.pack();
		return t;
	}


	public static ConfirmWindow showConfirm(String text, Runnable confirmed) {
		return showConfirm("@confirm", text, null, confirmed);
	}

	public static ConfirmWindow showConfirm(String title, String text, Runnable confirmed) {
		return showConfirm(title, text, null, confirmed);
	}

	public static ConfirmWindow showConfirm(String title, String text, Boolp hide, Runnable confirmed) {
		ConfirmWindow window = new ConfirmWindow(title, 0, 100, false, false);
		// window.hidden(() -> Window.all.remove(window));
		window.cont.add(text).width(mobile ? 400f : 500f).wrap().pad(4f).get().setAlignment(Align.center, Align.center);
		window.buttons.defaults().size(200f, 54f).pad(2f);
		window.setFillParent(false);
		window.buttons.button("@cancel", Icon.cancel, window::hide);
		window.buttons.button("@ok", Icon.ok, () -> {
			window.hide();
			confirmed.run();
		});
		if (hide != null) {
			window.update(() -> {
				if (hide.get()) {
					window.hide();
				}
			});
		}
		window.keyDown(KeyCode.enter, () -> {
			window.hide();
			confirmed.run();
		});
		window.keyDown(KeyCode.escape, window::hide);
		window.keyDown(KeyCode.back, window::hide);
		window.show();
		return window;
	}

	public static class ConfirmWindow extends DisposableWindow {
		public ConfirmWindow(String title, float minWidth, float minHeight, boolean full, boolean noButtons) {
			super(title, minWidth, minHeight, full, noButtons);
		}

		public void setCenter(Vec2 vec2) {
			setPosition(vec2.x - getPrefWidth() / 2f, vec2.y - getPrefHeight() / 2f);
		}
	}

	public static class MenuList {
		public Drawable icon;
		public String   name;
		public Runnable run;
		public Button   button;

		public MenuList(Drawable icon, String name, Runnable run) {
			this.icon = icon;
			this.name = name;
			this.run = run;
		}
	}

	public static class ConfirmList extends MenuList {
		public ConfirmList(Drawable icon, String name, String text, Runnable run) {
			super(icon, name, () -> {
				showConfirm(text, run).setPosition(Core.input.mouse());
			});
		}
	}

	static final Vec2 last = new Vec2();
	public static class LongPressListener extends ClickListener {
		final long  duration;
		final Boolc boolc;
		public LongPressListener(Boolc boolc0, long duration0) {
			boolc = boolc0;
			duration = duration0;
			task = new Task() {
				public void run() {
					if (pressed && Core.input.mouse().dst(last) < 10) {
						longPress = true;
						boolc.get(true);
					}
				}
			};
		}
		boolean longPress;
		final Task task;
		public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
			if (event.stopped) return false;
			longPress = false;
			if (super.touchDown(event, x, y, pointer, button)) {
				last.set(Core.input.mouse());
				task.cancel();
				Timer.schedule(task, duration / 1000f);
				return true;
			}
			return false;
		}
		public void clicked(InputEvent event, float x, float y) {
			// super.clicked(event, x, y);
			if (longPress) return;
			if (task.isScheduled() && pressed) boolc.get(false);
			task.cancel();
		}
	}
}

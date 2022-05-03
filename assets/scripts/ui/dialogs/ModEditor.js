
const findClass = require('func/findClass')
const IntStyles = findClass('ui.styles');
const IntFunc = require('func/index');
const IntSettings = require('content/settings');
const JsonDialog = require('ui/dialogs/JsonDialog');
const Editor = require('ui/dialogs/Editor');

const { framework, types: typeIni } = findClass("components.dataHandle");
// 语言
ModEditor.bundles = null;

let dialog, desc;
let w = !Core.graphics.isPortrait() ? 540 : Vars.mobile ? 410 : 440;
ModEditor.load = function () {
	JsonDialog.load()

	let field = IntFunc.toClass(LanguageDialog).getDeclaredField("displayNames")
	field.setAccessible(true)
	this.bundles = field.get(Vars.ui.language)

	dialog = new BaseDialog('');
	dialog.addCloseButton();

	desc = new Table;
	desc.center();
	desc.defaults().padTop(10).left();

	dialog.cont.pane(desc).fillX().fillY().get().setScrollingDisabled(true, false);
}

function getContentTable(mod) {

	let t = new Table()
	t.center();
	t.defaults().padTop(10).left();
	let contentRoot = mod.file.child('content');

	let cont = new Table();
	cont.defaults().padTop(10).left();
	let body = new Table();
	body.top().left();
	body.defaults().padTop(2).top().left();
	cont.pane(cons(p => p.add(body).left().growX())).growX().with(p => p.setScrollingDisabled(true, false)).row();

	let selectedContent
	let displayContentSprite;

	let setup = content => {
		selectedContent = content
		body.clearChildren();

		displayContentSprite = IntSettings.getValue("base", "display-content-sprite");

		if (content == contentRoot) {
			let cTypes = Editor.ContentTypes
			cTypes.keys().toSeq().each(cons(type => {
				let f = content.child(type)
				if (f.exists() && !f.isDirectory()) {
					f.deleteDirectory()
				}
				body.button(cons(b => {
					b.left()
					b.add(typeIni.get(f.name()) || f.name());
				}), Styles.defaultb, () => setup(f)).growX().pad(2).padLeft(4).left().row();
			}))
			return
		}

		body.add('$content.info').row();
		let table = new Table()
		var ok = false, selfText = '';
		IntFunc.searchTable(table, (p, text) => {
			selfText = text;

			if (ok) {
				p.cells.forEach(e => {
					let elem = e.get()
					elem.change();
				})
				return;
			}
			ok = true;
			p.clearChildren()
			let generator = function* () {
				let count = 0
				let all = content.findAll();
				for (let i = 0; i < all.size; i++) {
					let json = all.get(i)
					if (!/^h?json$/.test(json.extension())) continue
					if (count++ > 10) yield;
					let btn = new Button(Styles.defaultb);
					btn.defaults().growX().minWidth(400).pad(2).padLeft(4).left();
					let btnTable = new Table();
					let shown = true
					btnTable.changed(() => {
						let canShow = true;
						try {
							if (selfText != '' && !RegExp(selfText, 'i').test(json.nameWithoutExtension())) canShow = false
						} catch (e) { canShow = false }

						if (canShow) {
							if (!shown) {
								shown = true;
								btnTable.add(btn);
							}
						} else {
							shown = false
							btn.remove()
						}
					})
					btnTable.add(btn);
					p.add(btnTable).growX().row();

					btn.left()
					let table = new Table();
					table.left()
					let _setup = () => {
						table.clearChildren()
						if (displayContentSprite) {
							let image = table.image(IntFunc.find(mod, json.nameWithoutExtension())).size(32).padRight(6).left().get();
							if (!Vars.mobile) image.addListener(new HandCursorListener());
						}

						table.add(json.name()).top();
					};
					_setup();
					btn.add(table).growX().left().get()
					IntFunc.longPress(btn, 600, longPress => {
						if (longPress) {
							Vars.ui.showConfirm('$confirm',
								Core.bundle.format('confirm.remove', json.nameWithoutExtension()),
								run(() => {
									json.delete();
									setup(selectedContent);
								})
							);
						} else {
							if (!json.exists()) {
								IntFunc.showException(Error("file(" + json + ")不存在"))
								setup(selectedContent)
								return
							}
							JsonDialog.show(json, mod);
							let listener = extend(VisibilityListener, {
								hidden: () => {
									json = JsonDialog.file
									_setup()
									JsonDialog.ui.removeListener(listener)
									return false;
								}
							})
							JsonDialog.ui.addListener(listener);
						}
					});

				}
			}
			let g = generator()
			IntFunc.async("加载content", g, () => { });
		})
		body.add(table).growX().maxHeight(Core.graphics.height).row()

		// buttons
		body.table(cons(t => {
			t.defaults().growX()
			t.button("$back", Icon.left, () => setup(contentRoot)).growX();
			t.button('$add', Icon.add, () => {
				let ui = new Dialog('');
				let name = new TextField;
				ui.cont.table(cons(t => {
					t.add('$name')
					t.add(name).growX();
				})).growX().row();
				let table = new Table, values = [],
					selected = 0, type = {};
				let ok = false, j = 0;
				let map = framework.get(type.value = content.name()) || new ObjectMap()
				map.each(cons2((key, value) => {
					if (!ok) {
						let k = j;
						table.button("空白模板", Styles.clearTogglet, () => {
							children.get(selected).setChecked(false)
							children.get(selected = k).setChecked(true)
						}).size(150, 64)
						values.push('')
						if (j++ % 2) table.row()
					}
					ok = true

					let k = j
					table.button(key, Styles.clearTogglet, () => {
						children.get(selected).setChecked(false)
						children.get(selected = k).setChecked(true)
					}).size(150, 64)
					values.push(value)
					if (j++ % 2) table.row()
				}))
				let children = table.children
				children.get(selected).fireClick()
				table.defaults().width(300)
				ui.cont.pane(table).width(300).height(300)

				ui.buttons.button('$back', () => ui.hide()).size(150, 64);
				ui.buttons.button('$ok', run(() => {
					let file = content.child(type.value).child(name.getText() + '.hjson');
					file.writeString(values[selected]);
					// dialog.hide();
					setup(selectedContent)
					ui.hide();
				})).size(150, 64);
				ui.closeOnBack()

				ui.show();
			}).growX().disabled(boolf(() => framework.get(content.name()) == null)).row();
		})).growX()

	}
	setup(contentRoot);

	t.add(cont).growX().width(w).row();

	const ImgEditor = findClass('IntUI').imgDialog;
	let spritesDirectory = mod.file.child('sprites');
	let showSprites;
	t.button('查看图片库', showSprites = () => {

		let ui = new BaseDialog('图片库');

		let cont = new Table(cons(t => {
			t.top();
			let all = mod.spritesFi();
			if (all != null) {
				all.walk(cons(f => {
					try {
						buildImage(t, f);
					} catch(err) {
						Log.err(err);
					}
				}))
			}
		}));
		function buildImage(t, file) {
			if (file.extension() != 'png') return;
			t.table(cons(t => {
				t.left();

				let label = new Label(prov(() => file.nameWithoutExtension()))
				let field = new TextField();
				field.addListener(extend(InputListener, {
					keyUp(event, key) {
						if (key == "Enter") {
							let toFile;
							try {
								toFile = file.sibling(field.getText() + ".png") 
							} catch(e) {
								Vars.ui.showErrorMessage("文件名称不合法")
								return
							}
							file.moveTo(toFile)
							file = toFile
							cell.setElement(label)
						}
					}
				}));
				let cell = t.add(label);
				label.clicked(() => {
					field.setText(label.getText())
					cell.setElement(field)
				});
				t.button("", Icon.trash, Styles.cleart, () => t.remove() && file.delete())
				t.row();
				t.image().color(Color.gray).minWidth(440).row();
				t.image(new TextureRegion(new Texture(file))).size(96)
					.get().clicked(() => ImgEditor.beginEditImg(file));
			})).padTop(10).left().row();
		}
		ui.cont.pane(cont).fillX().fillY();
		ui.addCloseButton();
		ImgEditor.hiddenRun = () => {
			ui.hide();
			showSprites();
		};
		ui.buttons.button('$add', Icon.add, () => IntFunc.createDialog("添加图片",
			"新建图片", "默认32*32", Icon.add, run(() => ImgEditor.show()),
			"导入图片", "仅限png", Icon.download, run(() => IntFunc.selectFile(true, 'import file to add sprite', 'png', cons(f => {
				let toFile = spritesDirectory.child(f.name());
				function go() {
					try {
						buildImage(cont, f);
					} catch(err) {
						Vars.ui.showErrorMessage('文件可能损坏')
						return
					}
					f.copyTo(toFile);
				}
				if (toFile.exists()) Vars.ui.showConfirm('$confirm', '是否要覆盖', run(go));
				else go();
			})))
		)).size(210, 64);

		ui.hidden(run(() => setup(selectedContent)));

		ui.show();
	}).growX();
	return t;
}


ModEditor.current = null
function ModEditor(mod) {
	let meta = mod.meta;
	let displayName = '' + mod.displayName();
	dialog.title.setText(displayName)

	desc.clearChildren()

	if (meta.size == 0) {
		desc.add('$error', Color.red);
		return dialog.show();
	}

	if (mod.logo() != 'error' && IntSettings.getValue("base", "display_mod_logo")) desc.image(mod.logo()).row();


	desc.add('$editor.name', Color.gray).padRight(10).padTop(0).row();
	desc.add(displayName).growX().wrap().padTop(2).row();

	if (meta.has('author')) {
		desc.add('$editor.author', Color.gray).padRight(10).row();
		desc.add('' + meta.getString('author', "???")).growX().wrap().padTop(2).row();
	}
	if (meta.has('version')) {
		desc.add('$editor.version', Color.gray).padRight(10).row();
		desc.add('' + meta.getString('version', "???")).growX().wrap().padTop(2).row();
	}
	if (meta.has('description')) {
		desc.add('$editor.description').padRight(10).color(Color.gray).top().row();
		desc.add('' + meta.getString('description', "???")).growX().wrap().padTop(2).row();
	}


	let head = desc.table().fillX().get();

	let selected;
	let colors = [Color.gold, Color.pink, Color.sky];
	let names = ['editor.content', 'bundles', 'scripts'];
	let tables = [
		/* content */
		getContentTable(mod),
		/* bundles */
		new Table(Tex.whiteui.tint(1, .8, 1, .8), cons(t => {
			t.add("$default").padLeft(4).growX().left();
			t.button(Icon.pencil, Styles.clearTransi, () =>
				Editor.edit(mod.file.child("bundles").child("bundle.properties"), mod)
			).size(42).pad(10).row();
			Vars.locales.forEach(k => {
				t.add(ModEditor.bundles.get(k + "") || k + "").padLeft(4).growX().left();
				t.button(Icon.pencil, Styles.clearTransi, () =>
					Editor.edit(mod.file.child("bundles").child("bundle_" + k + ".properties"), mod)
				).size(42).pad(10).row();
				// if (Core.graphics.getWidth() > Core.graphics.getHeight() && i % 2 == 1) t.row();
			});
		})),
		/* scripts */
		new Table(Tex.whiteui.tint(.7, .7, 1, .8), cons(t => {
			t.add('未完成')
		}))
	];

	desc.row();
	let cell = desc.add().growX(), transitional = false;

	tables.forEach((table, i) => {
		head.button(cons(b => {
			b.add('$' + names[i], colors[i]).padRight(10 + 5).growY().row();

			let image = b.image().size(w / 3 - 1, 4).growX().get();
			b.update(() => image.setColor(selected == i ? colors[i] : Color.gray));
		}), IntStyles.clearb, () => {
			if (selected == i || transitional) return;
			if (selected != null) {
				let t = tables[selected]
				t.actions(Actions.fadeOut(0.2, Interp.fade), Actions.remove())
				transitional = true
				head.update(() => {
					if (t.hasActions()) return
					transitional = false
					head.update(null);
					selected = i;
					cell.setElement(table);
					table.actions(Actions.sequence(Actions.alpha(0), Actions.fadeIn(0.3, Interp.fade)))
				})
			} else {
				cell.setElement(table);
				selected = i;
			}
		}).size(w / 3, 60)
	});

	head.children.get(0).fireClick();

	return dialog.show();
}
module.exports = ModEditor
const IntStyles = require('scene/styles');
const IntFunc = require('func/index');
const JsonDialog = require('scene/ui/dialogs/JsonDialog');
const IntModsDialog = require('scene/ui/dialogs/ModsDialog');
const Editor = require('scene/ui/dialogs/Editor');

// 语言
const bundles = [
	'bundle',
	'bundle_be',
	'bundle_cs',
	'bundle_da',
	'bundle_de',
	'bundle_es',
	'bundle_et',
	'bundle_eu',
	'bundle_fi',
	'bundle_fil',
	'bundle_fr',
	'bundle_fr_BE',
	'bundle_hu',
	'bundle_in_ID',
	'bundle_it',
	'bundle_ja',
	'bundle_ko',
	'bundle_lt',
	'bundle_nl',
	'bundle_nl_BE',
	'bundle_pl',
	'bundle_pt_BR',
	'bundle_pt_PT',
	'bundle_ro',
	'bundle_ru',
	'bundle_sv',
	'bundle_th',
	'bundle_tk',
	'bundle_tr',
	'bundle_uk_UA',
	'bundle_zh_CN',
	'bundle_zh_TW'
];

const framework = {};
(() => {
	const dir = IntFunc.mod.root.child("framework")
	dir.list().forEach(fi => {
		let arr = []
		fi.findAll().each(cons(f => {
			arr.push(f.nameWithoutExtension(), f.readString())
		}))
		framework[fi.name()] = arr
	})
})()


let dialog, desc;
let w = Core.graphics.getWidth() > Core.graphics.getHeight() ? 540 : 440;
exports.load = function () {
	JsonDialog.load()

	dialog = new BaseDialog('');
	dialog.addCloseButton();

	desc = new Table;
	desc.center();
	desc.defaults().padTop(10).left();

	dialog.cont.pane(desc).fillX().fillY().get().setScrollingDisabled(true, false);;

	dialog.addCloseListener();
}

exports.current = null
exports.constructor = function (mod) {
	let meta = mod.meta;
	let displayName = '' + mod.displayName();
	dialog.title.setText(displayName)

	desc.clearChildren()

	if (meta.size == 0) {
		desc.add('$error', Color.red);
		return dialog.show();
	}

	let logo = mod.logo();
	if (logo != 'error') desc.add(logo).row();

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
		Table(Styles.none, cons(t => {
			t.center();
			t.defaults().padTop(10).left();
			let contentDir = mod.file.child('content');
			let selectedContent
			let cont = new Table();
			cont.defaults().padTop(10).left();
			let body = new Table();
			body.top().left();
			body.defaults().padTop(2).top().left();
			cont.pane(cons(p => p.add(body).left().grow().get().left())).fillX().minWidth(450).row();

			let setup = content => {
				selectedContent = content
				body.clearChildren();

				if (content == contentDir) {
					let cTypes = Editor.ContentTypes
					cTypes.keys().toSeq().each(cons(type => {
						let f = content.child(type)
						if (f.exists() && !f.isDirectory()) {
							f.deleteDirectory()
						}
						body.button(cons(b => {
							b.left()
							b.add(f.name());
						}), Styles.defaultb, () => setup(f)).fillX().minWidth(400).pad(2).padLeft(4).left().row();
					}))
					return
				}
				IntFunc.searchTable(body, (p, text) => {
					p.clearChildren()
					content.findAll().each(boolf(c => {
						try {
							if (!/^h?json$/.test(c.extension())) return false;
							return text == '' || RegExp(text, 'i').test(c.nameWithoutExtension())
						} catch (e) { return false }
					}), cons(json => {
						p.button(cons(b => {
							b.left()
							b.table(cons(t => {
								t.left()
								let image = t.image(IntFunc.find(mod, json.nameWithoutExtension())).size(32).padRight(6).left().get();
								if (!Vars.mobile) image.addListener(new HandCursorListener());

								t.add(json.name()).top();
							})).growX().left().get()
							IntFunc.longPress(b, 600, longPress => {
								if (longPress) {
									Vars.ui.showConfirm('$confirm',
										Core.bundle.format('confirm.remove', json.nameWithoutExtension()),
										run(() => {
											json.delete();
											setup(selectedContent);
										})
									);
								}
								else {
									JsonDialog.constructor(json, mod);
								}
							});

							let mod_name = meta.has("name") ? meta.getString("name").toLowerCase().replace(' ', '-') : modName
							let _mod = Vars.mods.getMod(mod_name)
							let clazz = Vars.mods.getClass()
							let field = clazz.getDeclaredField('parser')
							field.setAccessible(true)
							let parser = field.get(Vars.mods)
							// this.current = Vars.content.getLastAdded();
							/* b.table(cons(t => {
								t.button(Icon.add, Styles.clearTransi, () => {
									try {
										//this binds the content but does not load it entirely
										let name = json.parent().name()
										let ctype = Seq([ContentType]).get(0).getField(name[name.length - 1] != "s" ? name : name.substring(0, name.length - 1)).get(null)
										let loader = parser.parse(_mod, json.nameWithoutExtension(), json.readString(), json, ctype)
										loader.init()
										loader.load()
										if (loader instanceof Block) {
											loader.loadIcon()
											loader.buildVisibility = BuildVisibility.shown
										}
										dialog.hide()
										IntModsDialog.ui.hide()
										IntModsDialog.ui.shown(() => {
											loader = null
											let c = this.current
											if (c != null && Vars.content.getLastAdded() == c) {
												Vars.content.removeLast()
												if (this.current instanceof Block) {
													this.current.buildVisibility = BuildVisibility.hidden
													this.current = null
												}
											}
											Time.run(1, () => dialog.show())
											IntModsDialog.ui.shown(() => { })
										})
									} catch (e) {
										if (this.current != Vars.content.getLastAdded() && Vars.content.getLastAdded() != null) {
											Log.err(e)
										}
										Vars.ui.showErrorMessage(e);
										this.current = null
									}
								});
							})) */
						}), Styles.defaultb, run(() => { })).fillX().minWidth(400).pad(2).padLeft(4).left().row();

					}))

				})
				body.row()
				body.table(cons(t => {
					t.defaults().fillX()
					t.button("$back", Icon.left, () => setup(contentDir)).fillX().minWidth(200);
					t.button('$add', Icon.add, run(() => {
						let ui = new Dialog('');
						let name = new TextField;
						ui.cont.table(cons(t => {
							t.add('$name')
							t.add(name).fillX();
						})).fillX().row();
						let table = new Table, values = [],
							selected = 0, type = {};
						let ok = false, j = 0;
						let arr = framework[type.value = content.name()] || []
						for (let i = 0; i < arr.length; i += 2) {
							if (type.value != content.name()) continue
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
							table.button(arr[i], Styles.clearTogglet, () => {
								children.get(selected).setChecked(false)
								children.get(selected = k).setChecked(true)
							}).size(150, 64)
							values.push(arr[i + 1])
							if (j++ % 2) table.row()
						}
						let children = table.children
						children.get(selected).fireClick()
						table.defaults().width(300)
						ui.cont.pane(table).width(300).height(400)

						ui.buttons.button('$back', () => ui.hide()).size(150, 64);
						ui.buttons.button('$ok', run(() => {
							let file = content.child(type.value).child(name.getText() + '.json');
							file.writeString(values[selected]);
							// dialog.hide();
							setup(selectedContent)
							ui.hide();
						})).size(150, 64);

						ui.show();
					})).fillX().minWidth(200).disabled(boolf(() => framework[content.name()] == null)).row();
				})).fillX().growX()

			}
			setup(contentDir);

			t.add('$content.info').row();
			t.add(cont).growX().width(w).row();

			let spritesDirectory = mod.file.child('sprites');
			t.button('查看图片库', run(() => {
				let ui = new BaseDialog('图片库');

				let cont = new Table(cons(t => {
					t.top();
					let all = mod.spritesAll();
					for (let f of all) {
						buildImage(t, f);
					}
				}));
				function buildImage(t, file) {
					if (file.extension() != 'png') return;
					t.table(cons(t => {
						t.left();
						let field = t.field(file.nameWithoutExtension(),
							cons(text => {
								let toFile = file.parent().child(text + '.png');
								file.moveTo(toFile);
								file = toFile;
							})).growX().left().get();
						t.row();
						t.image().color(Color.gray).minWidth(440).row();
						t.image(TextureRegion(Texture(file))).size(96);
					})).padTop(10).left().row();
				}
				ui.cont.pane(cont).fillX().fillY();
				ui.addCloseButton();
				ui.buttons.button('$add', Icon.add, run(() =>
					IntFunc.selectFile(true, 'import file to add sprite', 'png', cons(f => {
						let toFile = spritesDirectory.child(f.name());
						function go() {
							f.copyTo(toFile);
							buildImage(cont, f);
						}
						if (toFile.exists()) Vars.ui.showConfirm('$confirm', '是否要覆盖', run(go));
						else go();
					}))
				)).size(90, 64);

				ui.hidden(run(() => setup(selectedContent)));

				ui.show();
			})).width(w - 20);

		})),
		/* bundles */
		Table(Tex.whiteui.tint(1, .8, 1, .8), cons(t => {
			bundles.forEach(v => {
				t.add(Core.bundle.get('bundle.' + v, v)).padLeft(4).width(400).left();
				t.button(Icon.pencil, Styles.clearTransi, run(() =>
					Editor.edit(mod.file.child('bundles').child(v + '.properties'), mod)
				)).growX().right().pad(10).row();
				// if (Core.graphics.getWidth() > Core.graphics.getHeight() && i % 2 == 1) t.row();
			});
		})),
		/* scripts */
		Table(Tex.whiteui.tint(.7, .7, 1, .8), cons(t => {
			t.add('未完成')
			return
			let scripts = mod.file.child('scripts');
			let main = scripts.child('main.js');
			main.exists() || main.writeString('');
			let cont = new Table;

			let all = scripts.findAll().toArray();
			let buttons = [];

			let buildButton = (cont, i, f) => cont.button(cons(b => {
				b.top().left();
				b.margin(12);
				b.defaults().left().top();
				b.table(cons(title => {
					title.left();
					title.image(Core.atlas.find(modName + '-js.file', Tex.clear)).size(64).padTop(8).padLeft(-8).padRight(8);
					title.add(f.nameWithoutExtension(), f.name() == 'main.js' ? Color.gold : Color.white).wrap().width(170).growX().left()/* .get().clicked(run(() => {

					})); */
					title.add().growX().left();
				}));
				b.table(cons(right => {
					right.right();
					right.button(Icon.trash, Styles.clearPartiali, run(() => Vars.ui.showConfirm('$confirm', Core.bundle.format('confirm.remove', f.name()), run(() => {
						f.delete();
						buttons.splice(i, 1).clear();
					})))).size(50);
				})).grow();
			}), IntStyles.clearb, run(() => Editor.edit(f, mod))).width(w - 20).get()

			for (let i = 0; i < all.length; i++) {
				buttons.push(buildButton(cont, i, all[i]));
				cont.row();
			}

			cont.table(cons(t => {
				t.button('$add', Icon.add, run(() => {
					let dialog = new Dialog('$add');
					dialog.cont.add('$fileName');
					let name = dialog.cont.add(new TextField('')).get();
					dialog.cont.row();
					let table = dialog.buttons;
					table.button('$back', Icon.left, run(() => dialog.hide()));
					table.button('$ok', Icon.ok, run(() => {
						if (name.getText() == 'main') return Vars.ui.showErrorMessage('文件名不能为[orange]main[]。');
						let toFile = scripts.child(name.getText() + '.js');
						function go() {
							toFile.writeString('');
							dialog.hide();
							build(cont, buttons.length - 1, toFile);
						}
						if (toFile.exists()) {
							Vars.ui.showConfirm('覆盖', '同名文件已存在\n是否要覆盖', run(() => go()));
						} else go();
					}));
					dialog.show();
				})).size(120, 64);
				// t.button('导入插件', Icon.download, run(() => {})).fillX();
				t.button('test', run(() => {
					// let o = Vars.mods.scripts.runConsole(main.readString());
					// if(o != null) Vars.ui.showInfo('' + o);
					Vars.mods.scripts.run(Vars.mods.locateMod(modName), main.readString());
				})).size(120, 64);
			})).name('buttons').fillX();

			t.add(cont);
		}))
	];

	desc.row();
	let cont = desc.table().fillX().get(), transitional = false;

	tables.forEach((table, i) => {
		head.button(cons(b => {
			b.add('$' + names[i], colors[i]).padRight(10 + 5).growY().row();

			let image = b.image().size(w / 3 - 1, 4).growX().get();
			b.update(run(() => image.setColor(selected == i ? colors[i] : Color.gray)));
		}), IntStyles.clearb, run(() => {
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
					cont.add(table);
					table.actions(Actions.sequence(Actions.alpha(0), Actions.fadeIn(0.3, Interp.fade)))
				})
			} else {
				cont.add(table);
				selected = i;
			}
		})).size(w / 3, 60);
	});
	head.children.get(0).fireClick();

	return dialog.show();
}
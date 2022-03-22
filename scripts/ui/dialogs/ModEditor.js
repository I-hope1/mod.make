const IntStyles = require('ui/styles');
const IntFunc = require('func/index');
const IntSettings = require('content/settings');
const JsonDialog = require('ui/dialogs/JsonDialog');
const Editor = require('ui/dialogs/Editor');
const { caches: { framework } } = require('func/IniHandle')

// 语言
exports.bundles = null;


let dialog, desc;
let w = Core.graphics.getWidth() > Core.graphics.getHeight() ? 540 : 440;
exports.load = function () {
	JsonDialog.load()

	let field = IntFunc.toClass(LanguageDialog).getDeclaredField("displayNames")
	field.setAccessible(true)
	this.bundles = field.get(Vars.ui.language)

	dialog = new BaseDialog('');
	dialog.addCloseButton();

	desc = new Table;
	desc.center();
	desc.defaults().padTop(10).left();

	dialog.cont.pane(desc).fillX().fillY().get().setScrollingDisabled(true, false);;
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
		new Table(Styles.none, cons(t => {
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
					content.walk(cons(json => {
						try {
							if (!/^h?json$/.test(json.extension())) return;
							if (text != '' && !RegExp(text, 'i').test(json.nameWithoutExtension())) return
						} catch (e) { return }
						p.button(cons(b => {
							b.left()
							b.table(cons(t => {
								t.left()
								if (IntSettings.getValue("base", "display-content-sprite")) {
									let image = t.image(IntFunc.find(mod, json.nameWithoutExtension())).size(32).padRight(6).left().get();
									if (!Vars.mobile) image.addListener(new HandCursorListener());
								}

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
						let map = framework[type.value = content.name()] || new Map()
						map.forEach((value, key) => {
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
						})
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
					let all = mod.spritesFi();
					if (all != null) {
						all.walk(cons(f => {
							buildImage(t, f);
						}))
					}
				}));
				function buildImage(t, file) {
					if (file.extension() != 'png') return;
					t.table(cons(t => {
						t.left();

						t.add(file.nameWithoutExtension())
						/* t.field(file.nameWithoutExtension(),
							cons(text => {
								let toFile = file.parent().child(text + '.png');
								file.moveTo(toFile);
								file = toFile;
							})).growX().left().get(); */
						t.row();
						t.image().color(Color.gray).minWidth(440).row();
						t.image(new TextureRegion(new Texture(file))).size(96);
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
				)).size(210, 64);

				ui.hidden(run(() => setup(selectedContent)));

				ui.show();
			})).width(w - 20);

		})),
		/* bundles */
		new Table(Tex.whiteui.tint(1, .8, 1, .8), cons(t => {
			t.add("$default").padLeft(4).width(400).left();
			t.button(Icon.pencil, Styles.clearTransi, () =>
				Editor.edit(mod.file.child("bundles").child("bundle.properties"), mod)
			).growX().right().pad(10).row();
			Vars.locales.forEach(k => {
				t.add(exports.bundles.get(k + "") || k + "").padLeft(4).width(400).left();
				t.button(Icon.pencil, Styles.clearTransi, () =>
					Editor.edit(mod.file.child("bundles").child("bundle_" + k + ".properties"), mod)
				).growX().right().pad(10).row();
				// if (Core.graphics.getWidth() > Core.graphics.getHeight() && i % 2 == 1) t.row();
			});
		})),
		/* scripts */
		new Table(Tex.whiteui.tint(.7, .7, 1, .8), cons(t => {
			t.add('未完成')
			return
			// let scripts = mod.file.child('scripts');
			// let main = scripts.child('main.js');
			// main.exists() || main.writeString('');
			// let cont = new Table;

			// let all = scripts.findAll().toArray();
			// let buttons = [];

			// let buildButton = (cont, i, f) => cont.button(cons(b => {
			// 	b.top().left();
			// 	b.margin(12);
			// 	b.defaults().left().top();
			// 	b.table(cons(title => {
			// 		title.left();
			// 		title.image(Core.atlas.find(modName + '-js.file', Tex.clear)).size(64).padTop(8).padLeft(-8).padRight(8);
			// 		title.add(f.nameWithoutExtension(), f.name() == 'main.js' ? Color.gold : Color.white).wrap().width(170).growX().left()/* .get().clicked(run(() => {

			// 		})); */
			// 		title.add().growX().left();
			// 	}));
			// 	b.table(cons(right => {
			// 		right.right();
			// 		right.button(Icon.trash, Styles.clearPartiali, run(() => Vars.ui.showConfirm('$confirm', Core.bundle.format('confirm.remove', f.name()), run(() => {
			// 			f.delete();
			// 			buttons.splice(i, 1).clear();
			// 		})))).size(50);
			// 	})).grow();
			// }), IntStyles.clearb, run(() => Editor.edit(f, mod))).width(w - 20).get()

			// for (let i = 0; i < all.length; i++) {
			// 	buttons.push(buildButton(cont, i, all[i]));
			// 	cont.row();
			// }

			// cont.table(cons(t => {
			// 	t.button('$add', Icon.add, run(() => {
			// 		let dialog = new Dialog('$add');
			// 		dialog.cont.add('$fileName');
			// 		let name = dialog.cont.add(new TextField('')).get();
			// 		dialog.cont.row();
			// 		let table = dialog.buttons;
			// 		table.button('$back', Icon.left, run(() => dialog.hide()));
			// 		table.button('$ok', Icon.ok, run(() => {
			// 			if (name.getText() == 'main') return Vars.ui.showErrorMessage('文件名不能为[orange]main[]。');
			// 			let toFile = scripts.child(name.getText() + '.js');
			// 			function go() {
			// 				toFile.writeString('');
			// 				dialog.hide();
			// 				build(cont, buttons.length - 1, toFile);
			// 			}
			// 			if (toFile.exists()) {
			// 				Vars.ui.showConfirm('覆盖', '同名文件已存在\n是否要覆盖', run(() => go()));
			// 			} else go();
			// 		}));
			// 		dialog.show();
			// 	})).size(120, 64);
			// 	// t.button('导入插件', Icon.download, run(() => {})).fillX();
			// 	t.button('test', run(() => {
			// 		// let o = Vars.mods.scripts.runConsole(main.readString());
			// 		// if(o != null) Vars.ui.showInfo('' + o);
			// 		Vars.mods.scripts.run(Vars.mods.locateMod(modName), main.readString());
			// 	})).size(120, 64);
			// })).name('buttons').fillX();

			// t.add(cont);
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
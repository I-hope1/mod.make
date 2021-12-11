
const IntStyles = require('scene/styles');
const IntFunc = require('func/index')
const scripts = Vars.mods.scripts

this[modName + '_main'] = this

const forIn = IntFunc.forIn;
const testElement = table => {
	let ui = new Dialog('');
	ui.cont.pane(cons(p => p.add(table))).size(Math.min(Core.graphics.getWidth(), Core.graphics.getHeight()) - 100).grow().row();
	ui.cont.button('$back', Icon.left, () => ui.hide()).size(200, 60);
	return ui.show();
};
const testEffect = str => {
	let dialog = testTable(extend(Table, {
		draw() {
			try {
				eval(str);
			} catch (e) {
				Vars.ui.showErrorMessage(e);
				this.remove();
				dialog.hide();
			}
		}
	}));
	return dialog;
};

var useable = require('testFi').useable;
exports.cont = {
	name: Core.bundle.get('test.name', 'test'),
	log: '', message: '', 'while': false, wrap: false, // scope: false,
	record: useable ? Vars.dataDirectory.child('mods(I hope...)').child('historical record') : null,
	show(table, buttons) {
		let cont = new Table();
		let w = Core.graphics.getWidth() > Core.graphics.getHeight() ? 540 : 440, h = Core.graphics.getHeight() * .4;

		const text = cont.add(new TextArea(this.message)).size(w, 390).get();
		cont.row();

		cont.button('$ok', () => {
			this.message = text.getText().replace(/\r/g, '\n');
			this.evalMessage();
			if (this.record) {
				let arr = this.record.list();
				arr.sort((a, b) => a.name() - b.name())
				/* 限制30个 */
				for (let i = 0; i < arr.length - 29; i++) {
					arr[i].deleteDirectory();
				}
				let d = this.record.child(Time.millis());
				d.child('message.txt').writeString(this.message);
				d.child('log.txt').writeString(this.log);
			}
		}).row();

		cont.table(Tex.button, cons(t => t.pane(cons(p => p.label(() => this.log))).size(w, 390)));

		table.add(cont).row();

		table.pane(cons(p => {
			p.button('', Icon.star, Styles.cleart, () => Vars.dataDirectory.child('mods(I hope...)').child('bookmarks').child(Vars.dataDirectory.child('mods(I hope...)').child('bookmarks').list().length + '-' + Time.millis() + '.txt').writeString(this.message));
			p.button(cons(b => b.label(() => this.while ? '$while' : '$default')), Styles.defaultb, () => this.while = !this.while).size(100, 55);
			p.button(cons(b => b.label(() => this.wrap ? '严格' : '非严格')), Styles.defaultb, () => this.wrap = !this.wrap).size(100, 55);
			// p.button(cons(b => b.label(() => this.scope ? 'new scope' : 'default')), Styles.defaultb, run(() => this.scope = !this.scope)).size(100, 55);

			if (useable) {
				p.button('$hitoricalRecord', () => {
					let dialog = new BaseDialog('$hitoricalRecord');
					dialog.cont.pane(cons(p => {
						let list = this.record.list();
						let _this = this;
						/* 按从新到旧排序 */
						list.sort((a, b) => b.name() - a.name()).forEach((f, i) => {
							let f = list[i];
							p.table(Tex.button, cons(t => {
								let btn = t.left().button(cons(b => {
									b.pane(cons(c => c.add(f.child('message.txt').readString()).left())).fillY().fillX().left();
								}), IntStyles.clearb, () => { }).height(70).minWidth(400).growX().fillX().left().get()
								IntFunc.longPress(btn, 600, longPress => {
									if (longPress) {
										let ui = new Dialog('');
										ui.cont.pane(cons(p => {
											p.add(i.child('message.txt').readString()).row();
											p.image().height(3).fillX().row();
											p.add(f.child('log.txt').readString());
										})).size(400).row();
										ui.cont.button(Icon.trash, () => {
											ui.hide();
											f.delete();
										}).row();
										ui.cont.button('$ok', () => ui.hide()).fillX().height(60);
										ui.show();
									} else {
										_this.message = f.child('message.txt').readString();
										_this.log = f.child('log.txt').readString();
										_this.setup();
										dialog.hide();
									}
								});
								t.button('', Icon.trash, Styles.cleart, () => f.deleteDirectory() && p.children.get(i).remove()).fill().right();
							})).width(w).row();
						});
					})).fillX().fillY();
					dialog.addCloseButton();
					dialog.show();
				}).size(100, 55);
				p.button('$bookmark', () => {
					let mark = Vars.dataDirectory.child('mods(I hope...)').child('bookmarks');
					let dialog = new BaseDialog('$bookmark');
					dialog.cont.pane(cons(p => {
						let list = mark.list();
						let _this = this;
						list.forEach((f, i) => {
							p.table(Tex.button, cons(t => {
								let btn = t.left().button(cons(b => {
									b.pane(cons(c => c.add(f.readString()))).left().fillY().fillX().left();
								}), IntStyles.clearb, () => { }).height(70).minWidth(400).growX().left().fillX().get();
								IntFunc.longPress(btn, 600, longPress => {
									if (longPress) {
										let ui = new Dialog('');
										ui.cont.pane(cons(p => {
											p.add(f.readString()).row();
										})).size(400).row();
										ui.cont.button(Icon.trash, () => {
											ui.hide();
											f.delete();
										}).row();
										ui.cont.button('$ok', () => ui.hide()).fillX().height(60);
										ui.show();
									} else {
										_this.message = f.readString();
										_this.setup();
										dialog.hide();
									}
								});
								t.button('', Icon.trash, Styles.cleart, () => f.delete() && p.children.get(i).remove()).fill().right();
							})).width(w).row();
						});
					})).fillX().fillY();
					dialog.addCloseButton();
					dialog.show();
				}).size(100, 55);
			}
		})).height(60).fillX();

		buttons.button('$back', Icon.left, () => this.ui.hide()).size(210, 64);


		let dialog = new BaseDialog('$edit');

		dialog.cont.pane(cons(p => {
			p.margin(10);
			p.table(Tex.button, cons(t => {
				let style = Styles.cleart;
				t.defaults().size(280, 60).left();

				t.row();
				t.button("@schematic.copy.import", Icon.download, style, () => {
					dialog.hide();
					text.setText(Core.app.getClipboardText());
				}).marginLeft(12);

				t.row();
				t.button("@schematic.copy", Icon.copy, style, () => {
					dialog.hide();
					Core.app.setClipboardText(text.getText().replace(/\r/g, '\n'));
				}).marginLeft(12);
			}));
		}));

		dialog.addCloseButton();

		buttons.button('$edit', Icon.edit, () => {
			dialog.show();
		}).size(210, 64);
	},
	setup() {
		this.ui.cont.clear();
		this.ui.buttons.clear();

		this.ui.cont.pane(cons(p => this.show(p, this.ui.buttons))).fillX().fillY();
	},
	buildConfiguration(table) {
		this.setup();
		this.ui.show();

		table.update(() => {
			if (this.while && this.message != '') this.evalMessage();
		});
	},
	evalMessage() {
		let def = this.message
		def = this.wrap ? '(function(){"use strict";' + def + '\n})();' : def
		this.log = scripts.runConsole(def);
		// try{
		// let print = text => log(this.name, text);
		// this.log = '' + eval(this.message);
		// }catch(e){
		// let str = e.message.replace(/\([^]*/g, '');
		// let arr = str.split(' '), arr2 = [];
		// for(let i in arr){
		// try{
		// if(/number|string/i.test(typeof eval(arr[i]))) arr2.push(arr.splice(i, 1));
		// }catch(e){continue;};
		// }
		// let str2 = arr.join('-').replace(/\:/g, '~');
		// this.log = '[red][' + Core.bundle.get(e.name, e.name) + '][gray]: [white]' + (!Core.bundle.has(str2) ? str : arr2.length ? Core.bundle.format(str2, eval('' + arr2)) : Core.bundle.get(str2)) + '[#ccccff](#' + e.lineNumber + ')[]';
		// }
	},
	load() {
		this.ui = new BaseDialog(this.name);
		this.ui.addCloseListener();
		
		scripts.runConsole(('const forIn = ' + forIn).replace(/\n/g, '') + (';const testElement = ' + testElement).replace(/\n/g, ''));
	},
	read(stream, revision) {
		this.super$read(stream, revision);

		this.message = stream.str();
		this.log = stream.str();
		this.while = !!stream.b();
	},
	write(stream) {
		this.super$write(stream);

		stream.str('' + this.message);
		stream.str('' + this.log);
		stream.b(+this.while);
	}
};

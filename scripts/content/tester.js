
const IntStyles = require('styles');

const forEach = (obj, str) => {
	var jg = [];
	for(var e in obj){
		jg.push(obj[e]);
	}
	return str == null ? jg : jg.join(str);
};
const forIn = (obj, str) => {
	var jg = [];
	for(var e in obj){
		jg.push(e + ': ' + obj[e]);
	}
	return str == null ? jg : jg.join(str);
};
const testTable = table => {
	let ui = new Dialog('');
	ui.cont.pane(cons(p => p.add(table))).size(Math.min(Core.graphics.getWidth(), Core.graphics.getHeight()) - 100).grow().row();
	ui.cont.button('$back', Icon.left, run(() => ui.hide())).size(200, 60);
	return ui.show();
};
const testEffect = str => {
	let dialog = testTable(extend(Table, {
		draw(){
			try{
				eval(str);
			}catch(e){
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
	log:'', message:'', 'while':false, strict:false,
	record: useable ? Vars.dataDirectory.child('mods(I hope...)').child('historical record') : null,
	show(table, buttons){
		let cont = new Table();
		let w = Core.graphics.getWidth() > Core.graphics.getHeight() ? 540 : 440, h = Core.graphics.getHeight() * .4;

		const text = cont.add(new TextArea(this.message)).size(w, 390).get();
		cont.row();

		cont.button('$ok', run(() => {
			this.message = text.getText().replace(/\r/g, '\n');
			this.evalMessage();
			if (this.record) {
				let d = this.record.child(Time.millis());
				d.child('message.txt').writeString(this.message);
				d.child('log.txt').writeString(this.log);
				let arr = this.record.list();
				arr.splice(0, arr.slice(0, -30).length).forEach(f => f.deleteDirectory()); /* 限制30个 */
			}
		})).row();

		cont.table(Tex.button, cons(t => t.pane(cons(p => p.label(() => this.log))).size(w, 390)));

		table.add(cont).row();

		table.pane(cons(p => {
			p.button('', Icon.star, Styles.cleart, run(() => Vars.dataDirectory.child('mods(I hope...)').child('bookmarks').child(Vars.dataDirectory.child('mods(I hope...)').child('bookmarks').list().length + '-' + Time.millis() + '.txt').writeString(this.message)));
			p.button(cons(b => b.label(() => this.while ? '$while' : '$default')), Styles.defaultb, run(() => this.while = !this.while)).size(100, 55);
			p.button(cons(b => b.label(() => this.strict ? '严格' : '非严格')), Styles.defaultb, run(() => this.strict = !this.strict)).size(100, 55);

			if (useable) {
				p.button('$hitoricalRecord', run(() => {
					let dialog = new BaseDialog('$hitoricalRecord');
					dialog.cont.pane(cons(p => {
						let list = this.record.list();
						let _this = this;
						list.slice().reverse() /* 按从新到旧排序 */ .forEach((e, j) => {
							let i = j, f = e;
							p.table(Tex.button, cons(t => {
								t.left().button(cons(b => {
									b.pane(cons(c => c.add(f.child('message.txt').readString()).left())).fillY().fillX().left();
								}), IntStyles[1].cont, run(() => {})).height(70).minWidth(400).growX().fillX().left().get().addListener(extend(ClickListener, {
									clicked(event, x, y){
										if(this.visualPressedTime - this.lastTapTime > 700){
											let ui = new Dialog('');
											ui.cont.pane(cons(p => {
												p.add(i.child('message.txt').readString()).row();
												p.image().height(3).fillX().row();
												p.add(f.child('log.txt').readString());
											})).size(400).row();
											ui.cont.button(Icon.trash, run(() => {
												ui.hide();
												f.delete();
											})).row();
											ui.cont.button('$ok', run(() => ui.hide())).fillX().height(60);
											ui.show();
										}else{
											_this.message = f.child('message.txt').readString();
											_this.log = f.child('log.txt').readString();
											_this.setup();
											dialog.hide();
										}
									}
								}));
								t.button('', Icon.trash, Styles.cleart, run(() => f.deleteDirectory() && p.children.get(i).remove())).fill().right();
							})).width(w).row();
						});
					})).fillX().fillY();
					dialog.addCloseButton();
					dialog.show();
				})).size(100, 55);
				p.button('$bookmark', run(() => {
					let mark = Vars.dataDirectory.child('mods(I hope...)').child('bookmarks');
					let dialog = new BaseDialog('$bookmark');
					dialog.cont.pane(cons(p => {
						let list = mark.list();
						let _this = this;
						list.forEach((e, j) => {
							let i = j, f = e;
							p.table(Tex.button, cons(t => {
								t.left().button(cons(b => {
									b.pane(cons(c => c.add(f.readString()))).left().fillY().fillX().left();
								}), IntStyles[1].cont, run(() => {})).height(70).minWidth(400).growX().left().fillX().get().addListener(extend(ClickListener, {
									clicked(event, x, y){
										if(this.visualPressedTime - this.lastTapTime > 700){
											let ui = new Dialog('');
											ui.cont.pane(cons(p => {
												p.add(f.readString()).row();
											})).size(400).row();
											ui.cont.button(Icon.trash, run(() => {
												ui.hide();
												f.delete();
											})).row();
											ui.cont.button('$ok', run(() => ui.hide())).fillX().height(60);
											ui.show();
										}else{
											_this.message = f.readString();
											_this.setup();
											dialog.hide();
										}
									}
								}));
								t.button('', Icon.trash, Styles.cleart, run(() => f.delete() && p.children.get(i).remove())).fill().right();
							})).width(w).row();
						});
					})).fillX().fillY();
					dialog.addCloseButton();
					dialog.show();
				})).size(100, 55);
			}
		})).height(60).fillX();

		buttons.button('$back', Icon.left, run(() => this.ui.hide())).size(210, 64);


		let dialog = new BaseDialog('$edit');

		dialog.cont.pane(cons(p => {
			p.margin(10);
			p.table(Tex.button, cons(t => {
				let style = Styles.cleart;
				t.defaults().size(280, 60).left();

				t.row();
				t.button("@schematic.copy.import", Icon.download, style, run(() => {
					dialog.hide();
					text.setText(Core.app.getClipboardText());
				})).marginLeft(12);

				t.row();
				t.button("@schematic.copy", Icon.copy, style, run(() => {
					dialog.hide();
					Core.app.setClipboardText(text.getText().replace(/\r/g, '\n'));
				})).marginLeft(12);
			}));
		}));

		dialog.addCloseButton();

		buttons.button('$edit', Icon.edit, run(() => {
			dialog.show();
		})).size(210, 64);
	},
	setup(){
		this.ui.cont.clear();
		this.ui.buttons.clear();

		this.ui.cont.pane(cons(p => this.show(p, this.ui.buttons))).fillX().fillY();
	},
	buildConfiguration(table){
		this.ui = new BaseDialog(this.name);
		this.setup();
		this.ui.show();

		table.update(run(() => {
			if(this.while && this.message != '') this.evalMessage();
		}));
	},
	evalMessage(){
		let def = 'function forIn(obj, str){var jg = [];for(var e in obj){jg.push(e + ": " + obj[e])};return str == null ? jg : jg.join(str);};' + this.message;
		this.log = Vars.mods.scripts.runConsole(this.strict ? '(function(){"use strict";' + def + '\n})();' : def);
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
	read(stream, revision){
		this.super$read(stream, revision);

		this.message = stream.str();
		this.log = stream.str();
		this.while = !!stream.b();
	},
	write(stream){
		this.super$write(stream);

		stream.str('' + this.message);
		stream.str('' + this.log);
		stream.b(+this.while);
	}
};

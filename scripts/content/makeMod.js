
const IntStyles = require('styles');
const IntFunc = require('func/index');
const buildContent = require('func/buildContent').method;

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

const dataDirectory = Vars.dataDirectory.child('mods(I hope...)');

// const Contents = Vars.content.blocks().toArray().concat(Vars.content.liquids().toArray()).concat(Vars.content.items());
/* 所有的type */
const types = {
	'blocks': [
		['Accelerator', 'LaunchPad'],
		['BaseTurret', 'ReloadTurret', 'Turret', 'ItemTurret', 'LiquidTurret', 'LaserTurret', 'PowerTurret', 'TractorBeamTurret', 'PointDefenseTurret'],
		['Wall', 'Thruster', 'ShockMine', 'MendProjector', 'ForceProjector', 'OverdriveProjector'],
		['Converyor', 'ArmoredConveryor', 'BufferedItemBridge', 'ChaindBuilding', 'StackConveryor'],
		['ItemSource', 'LiquidSource', 'PowerSource'],
		['Door', 'OreBlock', 'Floor'],
		['StorageBlock', 'Unloader', 'CoreBlock'],
		['UnitBlock', 'UnitFactory', 'Reconstructor'],
		['AtttibuteSmelter', 'GenericCrafter', 'GenericSmelter'],
		['Battery', 'PowerGenerator', 'SolarGenerator', 'SingleTypeGenerator', 'ImpactReactor', 'NuclearReactor',  'PowerNode'],
	],
	'items': [],
	'liquids': [],
};
for (let k in types) {
	types[k] = types[k].toString().split(',');
	types[k].unshift(k.split('').map((s, i) => i == 0 ? s.toUpperCase() : i == k.length - 1 ? '' : s).join(''));
}

// 查找图片
function find(mod, name) {
	let error = Core.atlas.find('error');
	let all = mod.spritesAll();
	return all.length != 0 && (() => {
		for (var f of all) {
			if (f.name() == name + '.png') return new TextureRegion(new Texture(f));
		}
	})() || error;
}

// 暂时没用
function TextField_JS(text, i, arr, t) {
	arr.splice(i, 0, extend(TextField, {
		index: +i,
		toString() {
			return this.text;
		},
		/*paste(content, fireChangeEvent){
			if(content == null) return;
			let buffer = [];
			let textLength = this.text.length;
			if(this.hasSelection) textLength -= Math.abs(this.cursor - this.selectionStart);
			let data = style.font.getData();
			let field = this;
			let content = content.split(/\r|\n/);
			for(let i in content){
				if(i == 0) continue;
				TextField_JS(content[i], index + i, arr);
			}
			content = content[0];
			if(hasSelection) cursor = delete(fireChangeEvent);
			if(fireChangeEvent)
				this.changeText(this.text, this.insert(this.cursor, content, this.text));
			else
				this.text = this.insert(this.cursor, content, this.text);
			this.updateDisplayText();
			this.cursor += content.length;
		}*/
	}));
	arr[i].addListener(extend(InputListener, {
		enter(event, x, y, pointer, fromActor) {
			TextField_JS('', this.index + 1, arr, t);
			arr.forEach((e, i) => i > this.index ? e.index++ : 0);
		}
	}));
	let style = new TextField.TextFieldStyle(Styles.defaultField);
	//style.messageFontColor = style.fontColor = style.focusedFontColor = style.disabledFontColor = Color.black;
	style.font = Fonts.def;
	style.cursor = Tex.whiteui.tint(1, 1, 1, .7);
	//style.background = Tex.whiteui.tint(1, 1, 1, 1);
	//style.focusedBackground = Tex.whiteui.tint(.1, .1, .1, .7);
	style.selection = Tex.whiteui.tint(.3, .3, 1, .7);
	arr[i].setStyle(style);
	arr[i].setText(text);
	arr[i].setWidth(Core.graphics.getWidth() * .8);
	t.addChildAt(i, arr[i]);
}



exports.cont = {
	name: Core.bundle.get('makeMod.localizedName', 'makeMod'), needFi: true,

	scripts: {
		build() {
			let table = this.table = extend(Table, {
				main: this,
			});
			table.table(Tex.whiteui, cons(t => {
				let main = table.main;
				if (main == null) return null;
				t.color.set(main.color);
				t.margin(6);
				t.add('$' + main.type).style(Styles.outlineLabel).color(Color.white).padRight(8);
				t.image().height(1).growX().color(Color.gray);
				t.button(Icon.copy, Styles.logici, run(() => t.parent.add(main.build()))).padRight(6);
				t.button(Icon.cancel, Styles.logici, run(() => main.remove()));
				t.row();
				t.table(Styles.black, cons(t => {
					t.left();
					t.marginLeft(4);
					t.marginBottom(7);
					main.buildChildren(t);
				})).width(Core.graphics.getWidth() * .6).pad(4).padTop(3).left().growX();
			})).width(Core.graphics.getWidth() * .7).pad(4).padTop(2).left().growX();
		},

		defined:function(){
			this.build.call(this);
			this.type = 'defined';
			this.color = Color.sky;
			this.name = 'result';
			this.value = '"value"';
			this.toString = function() {
				return 'let ' + this.name + ' = ' + this.value + ';';
			};
			this.buildChildren = function(table) {
				table.add('$name');
				let _name = table.add(new TextField(this.name)).get();
				table.add(' = ');
				table.add('$value');
				let _value = table.add(new TextField(this.value)).get();
				table.update(run(() => {
					this.name = _name.getText();
					this.value = _value.getText();
				}));
			};
			this.remove = function() {
				if (this.table != null) this.table.remove();
			}
		}
	},

	mods: [],
	modsPath: Vars.dataDirectory.child('mods(I hope...)'),
	importMod(file) {
		try {
			file.copyTo(dataDirectory.child(file.name()));
			this.setup(this.ui);
		} catch (e) {
			let err = '[red][' + Core.bundle.get(e.name, e.name) + '][]' + e.message;
			Vars.ui.showErrorMessage(err);
		}
	},
	editor(file, mod) {
		Vars.ui.loadfrag.show();

		file.exists() || file.writeString('');

		let dialog = new BaseDialog('代码编辑器');
		let cont = dialog.cont;
		cont.top().defaults().padTop(0).top();
		let w = Core.graphics.getWidth() * .8;
		let result = {};
		let fileName = file.extension() != 'properties' ? cont.table(cons(t => t.add('$fileName'))).get().add(
			new TextField('' + file.nameWithoutExtension())).get() : null;

		cont.row();
		cont.pane(cons(p => {
			// 编辑代码
			switch (file.extension()) {
				case 'json': case 'hjson': {
					let obj = result.value = IntFunc.HjsonParse(file.readString());
					let parentname = file.parent().name();
					let parenttype = parentname[0].toUpperCase() + parentname.slice(1, -1);
					obj.type = obj.type != null ? obj.type :
						types[parentname] != null && types[parentname][0] != null ? types[parentname][0] : 'none';

					// type接口
					p.table(Tex.clear, cons(t => {
						t.add('$type').padRight(2);
						let button = new Button(IntStyles[1].cont);
						t.add(button).size(190, 40);
						button.label(() => '$' + obj.type.toLowerCase()).center().grow().row();
						button.image().color(Color.gray).fillX();
						button.clicked(run(() => IntFunc.showSelectTable(button, (p, hide, v) => {
							let arr = ['none'];
							for (let k in types) {
								arr = arr.concat(types[k])
							}

							p.clearChildren()
							let reg = RegExp('' + v, 'i')
							for (let type of arr) {
								if (!reg.test(type)) continue;
								let t = type;
								p.button(Core.bundle.get(t.toLowerCase(), t), Styles.cleart, run(() => {
									obj.type = t;
									hide.run();
								})).pad(5).size(200, 65).disabled(obj.type == t).row();
							}
						}, true)));
					})).fillX().row();

					// 添加content属性的函数
					let tablesChange = (tables, table, index, remove, name, value) => {
						let ts = tables,
							i = index,
							k = name,
							v = value;
						if (!remove) {
							ts.splice(i, 0, table.table(
								i % 3 == 0 ? Tex.whiteui.tint(0, 1, 1, .7) :
								i % 3 == 1 ? Tex.whiteui.tint(1, 1, 0, .7) :
								Tex.whiteui.tint(1, 0, 1, .7), cons(t => {
								// 不行，不能定义变量
								/* // 让函数拥有变量
								eval(('' + IntFunc.buildContent).replace(/function\s*\(\)\s*\{([^]+)\}/, '$1')); */

								buildContent(obj, [ts, t, i, k ,v]);

								t.button('', Icon.trash, Styles.cleart, run(() => tablesChange(ts, t, i, true, k, v)));
								if (Core.bundle.has(k + '.help')) t.add('// ' + Core.bundle.get(k + '.help')).padLeft(2);
							})).fillX().margin(3).marginLeft(6).marginRight(6).left().get());
						}
						else { // remove为true时执行
							delete obj[k];
							ts.splice(i, 1)[0].remove();
						}
						table.row()
					}

					let table = new Table(Tex.whiteui.tint(.4, .4, .4, .9)), tables = [];
					p.table(Tex.button, cons(t => {
						t.add(table).fillX().pad(4).row();

						let content;
						// 添加接口
						let btn = t.button('$add', Icon.add, run(() => IntFunc.showSelectTable(
							btn, (p, hide, v) => {
								p.left().top().defaults().left().top();
								p.clearChildren();
								if (obj.type == 'none') {
									let name = p.table(cons(t => 
										t.add('$name').growX().left().row()
									)).get().add(new TextField)
										.width(300).get();
									p.row();
									let value = p.table(cons(t => t.add(
												'$value').growX().left()
											.row())).get().add(
											new TextField)
										.width(300).get();
									p.row();
									p.button('$ok', Styles.cleart, run(() => {
										tableChange(tables,
											tables.length, false, name
											.getText(), value
											.getText());
										hide.run()
									})).fillX();
									return;
								}
								let cont = eval(obj.type);
								while (content == null) {
									try {
										// 创建一个临时类
										content = new JavaAdapter(cont, {},
											'unused_' + obj.type);
									} catch (e) {
										Vars.content.removeLast();
									}
								}
								let reg = RegExp(v, 'i');
								let arr = IntFunc.forIn(content, (k, obj) => typeof obj[k] != 'function' &&
									!/^(id|self|unlocked|stats|bars|inlineDescription|delegee|details|minfo|cicon|cacheLayer|region|iconId|uiIcon|fullIcon)$/
									.test(k) && (reg.test(k) || reg.test(Core.bundle.get('content.' + k))), (k, obj) => {
									p.button(Core.bundle.get('content.' + k, k), Styles.cleart, run(() => {
										tablesChange(
											tables, table,
											tables.length,
											false,
											k, obj[k]
										);
										hide.run();
									})).size(Core.graphics.getWidth() * .2, 45)
										.disabled(result.value[k] != null).row();
								});
							}, obj.type != 'none'))).fillX().growX().get();
						t.row();
						t.table(cons(t => {
							// 研究
							var k = 'research';
							t.add(Core.bundle.get(k, k));
							t.add(':');
							
							let techs = TechTree.all.toArray();

							let btn = t.button(obj[k] != '' && obj[k] != null ? obj[k] : '$none', Styles.cleart,
								run(() => IntFunc.showSelectTable(btn, (p, hide, v) => {
									p.clearChildren();
									p.button('$none', Styles.cleart, run(() => {
										obj[k] = '';
										btn.setText('$none');
									}));

									let cont = p.table().get();
									let tableArr = [new Table, new Table, new Table, new Table, new Table, new Table];
									
									let reg = RegExp(v, 'i'), i = 0;

									for (let tech of techs) {
										let t = tech.content;
										if (!reg.test(t.name) && !reg.test(t.localizedName)) continue;

										let table = tableArr[
											t instanceof Item ? 0 :
											t instanceof Liquid ? 1 :
											t instanceof Block ? 2 :
											t instanceof UnitType ? 3 :
											t instanceof SectorPreset ? 4 :
											5
										];
										let button = table.button(Tex.whiteui, Styles.clearToggleTransi, 32, () => {
											btn.setText(obj[k] = t.name);
											hide.run()
										}).size(40).get();
										button.getStyle().imageUp = new TextureRegionDrawable(t.uiIcon);
										button.update(() => button.setChecked(obj[k] == t.name));

										if (table.children.size % (Vars.mobile ? 6 : 10) == 0) {
											table.row();
										}
									}
									for (let i = 0; i < tableArr.length; i++) {
										let table = tableArr[i];
										cont.add(table).growX().left().row();
										if (table.children.size != 0 && i < tableArr.length - 2) {
											cont.image(Tex.whiteui, Pal.accent)
												.growX().height(3).pad(4).row();
										}
									}
								}, true)
							)).size(150, 60).get();
						})).fillX();
					})).fillX().row();
					for (var k in obj) {
						if (k == 'type' || k == 'research') continue;
						tablesChange(tables, table, tables.length, false, k, obj[k]);
					}
				}
				break;
				case 'properties': {
					let str = file.readString().split('\n');
					let obj = result.value = {
						from: [],
						to: []
					};
					if (str.join('') == '') str.length = 0;
					let cont = p.table(Tex.whiteui.tint(.5, .6, .1, .8)).padLeft(3).get();
					let fun = (from, to) => {
						let table = cont.table(Tex.button, cons(t => {
							obj.from.push(t.add(new TextField(from)).width(200).get());
							t.add(' = ', Color.gray);
							obj.to.push(t.add(new TextField(to)).width(200).get());
						})).get();
						let index = obj.from.length - 1;
						IntFunc.doubleClick(table, [run(() => {
							Vars.ui.showConfirm('$confirm', 
								Core.bundle.format('confirm.remove', obj.from[index].getText()),
								run(() => {
									table.remove();
									obj.from.splice(index, 1);
									obj.to.splice(index, 1);
								})
							);
						}), run(() => {})]);
						cont.row();
					}
					str.forEach(e => {
						let arr = e.split(/\s*=\s*/);
						fun(arr[0], arr[1]);
					});
					p.row();
					p.button('$add', Icon.add, run(() => fun('', '')));
				}
				break;
				case 'js': {
					p.top().defaults().top().grow();
	
					let cont = p.table().get();
					p.row();
					let btn = p.button('$add', Icon.add, run(() => IntFunc.showSelectTable(btn, (p, hide) => {
						let cols = 2, c = 0;
						let scripts = this.scripts;
						p.left();
						Object.keys(scripts).forEach(k => {
							if (k == 'build') return;

							p.button('$scripts.' + k, Styles.cleart, run(() => {
								cont.add((new scripts[k]).build()).row();
							})).size(440 / 2, 60);
							if (++c % cols == 0) p.row();
						});
					}))).size(220, 75).bottom().get();
					p.row();
				}
				break;
				default:
					let area = result.value = p.area(file.readString(), cons(t => {

					})).size(Math.min(Core.graphics.getWidth(), Core.graphics.getHeight()) - 200)
						.get();
					/*area.changed(run(() => {
						nums
					}));*/
					//area.clicked(run(() => IntFunc.showTextArea(result.value)));
			}
		})).fillX().height(Core.graphics.getHeight() - (Vars.mobile ? 400 : 200)).grow().row();

		dialog.buttons.button('$back', Icon.left, run(() => dialog.hide())).size(220, 70);

		dialog.buttons.button('$ok', Icon.ok, run(() => {
			try {
				// 编译代码
				switch (file.extension()) {
					case 'json':
					case 'hjson':
						var arr = [], obj = result.value;
						for (let k in obj) {
							if (obj[k] == null || obj[k] == '') continue;
							if (k == 'type') {
								if (!/(none|item|liquid|unit)\b/i.test(obj[k])){
									arr.push('"type": "' + obj[k] + '"');
								}
								continue;
							}
							if (k == 'research') {
								arr.push('"research": "' + obj[k] + '"');
								continue;
							}
							arr.push('"' + k + '": ' + obj[k]);
						}
	
						file = mod.file.child('content').child(obj.type == 'none' ? 'blocks' :
							Object.keys(types).map(e => types[e].indexOf(obj.type) != -1 ? e : '').join(''))
							.child(file.name());
						file.writeString('{\n\t' + arr.join(',\n\t') + '\n}');
	
						/* let strs = [], obj = result.value;
						for(let k in obj){
							strs.push('\t"' + k + '": "' + obj[k] + '"');
						}
						file.writeString('{\n' + strs.join(',\n') + '\n}'); */
						break;
					case 'properties': 
						var str = [],
							obj = result.value;
						obj.from.forEach((e, i) => {
							str.push(e.getText() + ' = ' + obj.to[i].getText());
						});
						file.writeString(str.join('\n'));
						break;
					default:
						file.writeString(result.value.getText().replace(/\r/g, '\n'));
				}
			} catch(e) {
				Vars.ui.showErrorMessage(e);
			}
			if (fileName) {
				let toFile = file.parent().child(fileName.getText() + '.' + file.extension());
				file.moveTo(toFile);
			}
			dialog.hide();
		})).size(220, 70);

		dialog.show();
		Vars.ui.loadfrag.hide();
		return dialog;
	},
	editModJson(file) {
		let obj = IntFunc.HjsonParse(file.exists() ? file.readString() : null);
		let isNull = obj == null;
		obj = isNull ? {} : obj;
		let ui = new Dialog(isNull ? '$mod.create' : '$edit');
		let arr = ['name', 'displayName', 'description', 'author', 'version', 'main'],
			cont = new Table();
		cont.add('$mod.fileName');
		let filename = obj.fileName = cont.add(new TextField(!isNull ? file.parent().nameWithoutExtension() : '')).get();
		filename.changed(run(() => {
			ok.setDisabled(filename.getText().replace(/\s/g, '') == '');
		}));
		cont.row();
		cont.add('$minGameVersion');
		let mingameversion = obj.minGameVersion = cont.add(new TextField(!isNull && obj.minGameVersion ? obj.minGameVersion : '105')).get();
		mingameversion.changed(run(() => {
			ok.setDisabled((mingameversion.getText() | 0) < 105 || (mingameversion.getText() | 0) > Version.build);
		}));
		cont.row();
		for (let i of arr) {
			cont.add('$' + i);
			let field = new TextField(!isNull && obj[i] != null ? obj[i].replace(/\n|\r/g, '\\n') : '');
			field.clicked(run(function() {
				// 如果长按时间大于800毫秒
				if (Time.millis() - this.visualPressedTime - this.visualPressedDuration * 1000 > 800)
					IntFunc.showTextArea(field);
			}));
			obj[i] = cont.add(field).get();
			cont.row();
		}
		ui.cont.add(cont).row();
		let func = mod => {
			delete obj.fileName;
			if (!isNull) file.moveTo(mod);
			let strArr = [];
			for (let k in obj) {
				if (obj[k].getText() == '') continue;
				strArr.push('\t"' + k + '": "' + obj[k].getText().replace(/\n|\r/g, '\\n') + '"');
			}
			mod.child(isNull ? 'mod.json' : 'mod.' + file.extension()).writeString('{\n' +
				strArr.join(',\n') + ',\n}');
			this.setup(this.ui);
			ui.hide();
		}
		let buttons = new Table;
		let w = Core.graphics.getWidth(),
			h = Core.graphics.getHeight();
		buttons.button('$back', Icon.left, run(() => ui.hide())).size(Math.max(w, h) * 0.1, Math.min(w, h) *
			0.1);
		let ok = buttons.button('$ok', Icon.ok, run(() => {
			if ((obj.minGameVersion.getText() | 0) < 105) {
				return Vars.ui.showErrorMessage(Core.bundle.get('minGameVersion') + Core.bundle.get(
					'cannot-be-less-than') + '105.');
			}
			let mod = dataDirectory.child(obj.fileName.getText());
			if (mod.path() != file.parent().path() && mod.exists()) {
				Vars.ui.showConfirm('覆盖', '同名文件已存在\n是否要覆盖', run(() => {
					mod.deleteDirectory();
					func(mod);
				}));
			}
			else func(mod);
		})).size(Math.max(w, h) * 0.1, Math.min(w, h) * 0.1).get();
		ui.cont.add(buttons);
		ui.show();
	},

	newMod(file) {
		let meta = IntFunc.HjsonParse(
			file.child('mod.json').exists() ? file.child('mod.json').readString() :
			file.child('mod.hjson').exists() ? file.child('mod.hjson').readString() : null);
		if (meta == null) return null;
		return {
			'file': file,
			'meta': meta,
			spritesAll() {
				return this.file.child('sprites').exists() ? this.file.child('sprites').findAll().toArray() : [];
			},
			displayName() {
				return this.meta.displayName != null ? this.meta.displayName : this.meta.name;
			},
			logo() {
				try {
					return TextureRegion(Texture(this.file.child('sprites-override').child('logo.png')));
				} catch (err) {
					return Core.atlas.find('error');
				}
			}
		};
	},
	
	showMod(mod) {
		Vars.ui.loadfrag.show();
		this._showMod(mod);
		Time.run(1, run(() => Vars.ui.loadfrag.hide()));
	},
	_showMod(mod) {
		let meta = mod.meta;
		let displayName = mod.displayName();
		let w = Core.graphics.getWidth() > Core.graphics.getHeight() ? 540 : 440;
		let dialog = new BaseDialog(displayName);

		dialog.addCloseButton();
		dialog.cont.pane(cons(desc => {
			desc.center();
			desc.defaults().padTop(10).left();

			if (meta == {}) {
				desc.add('$error', Color.red);
				return;
			}

			let logo = mod.logo();
			if (logo != 'error') desc.add(logo).row();

			desc.add('$editor.name', Color.gray).padRight(10).padTop(0).row();
			desc.add(displayName).growX().wrap().padTop(2).row();

			if (meta.author != null) {
				desc.add('$editor.author', Color.gray).padRight(10).row();
				desc.add(meta.author).growX().wrap().padTop(2).row();
			}
			if (meta.version != null) {
				desc.add('$editor.version', Color.gray).padRight(10).row();
				desc.add(meta.version).growX().wrap().padTop(2).row();
			}
			if (meta.description != null) {
				desc.add('$editor.description').padRight(10).color(Color.gray).top().row();
				desc.add(meta.description).growX().wrap().padTop(2).row();
			}

			
			let head = desc.table().fillX().get();
			
			let selected = -1;
			let colors = [Color.gold, Color.pink, Color.sky];
			let names = [['editor.content'], ['bundles'], ['scripts']];
			let tables = [
				/* content */
				Table(Styles.none, cons(t => {
					t.center();
					t.defaults().padTop(10).left();
					let color = Color.valueOf('#ccccff');
					let content = mod.file.child('content');
					let files = content.findAll().toArray();
					let cont = new Table();
					cont.defaults().padTop(10).left();

					let filter = '';
					let setup = () => {
						cont.clearChildren();
						let body = new Table();
						body.top().left();
						body.defaults().padTop(2).top().left();
						cont.pane(cons(p => p.add(body).left().grow().get().left())).fillX().minWidth(450).row();
						let files = filter instanceof RegExp ? content.findAll().toArray().map(e => filter.test(e) ? e : null) :
						 filter instanceof Cons ? content.findAll().toArray().map(e => filter.get(e)) :
						 filter == '' ? content.findAll().toArray() : content.child('' + filter).findAll().toArray();

						for (let i = 0, len = files.length; i < len; i++) {
							let json = files[i];
							if (json != null && !/h?json/.test(json.extension())) continue;

							body.button(cons(b => {
								b.left()
								let image = b.image(find(mod, json.nameWithoutExtension())).size(32).padRight(6).left().get();
								if (!Vars.mobile) image.addListener(new HandCursorListener());
										
								b.add(json.name()).top();
								let _this = this;
								b.addListener(extend(ClickListener, {
									clicked(a, b, c) {
										if (Time.millis() - this.visualPressedTime - this.visualPressedDuration * 1000 > 800)
											Vars.ui.showConfirm('$confirm',
												Core.bundle.format('confirm.remove', json.nameWithoutExtension()),
												run(() => {
													json.delete();
													setup();
												})
											);
										else {
											let _dialog = _this.showJson(json, mod);
											if (_dialog != null) _dialog.hidden(run(() => setup()));
										}
									}
								}));
							}), Styles.defaultb, run(() => {})).fillX().minWidth(400).pad(2).padLeft(4).left().row();
						}
					}
					setup();

					t.add('$content.info').row();
					t.add(cont).growX().width(w).row();
					t.button('$add', Icon.add, run(() => {
						let ui = new Dialog('');
						let name = ui.cont.table(cons(t => t.add('$name'))).get().add(TextField()).width(150).get();
						ui.buttons.button('$back', run(() => ui.hide())).size(90, 45);
						ui.buttons.button('$ok', run(() => {
							let file = content.child('blocks').child(name.getText() + '.json');
							file.writeString('');
							// dialog.hide();
							ui.hide();
							this.editor(file, mod).hidden(run(() => {
								setup();
							}));
						})).size(90, 45);
						ui.show();
					})).width(w - 20).row();

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

						ui.hidden(run(() => setup()));

						ui.show();
					})).width(w - 20);

				})),
				/* bundles */
				Table(Tex.whiteui.tint(1, .8, 1, .8), cons(t => {
					bundles.forEach((v, i) => {
						t.add('$bundle.' + v).width(400).left();
						t.button(Icon.pencil, Styles.clearTransi, run(() =>
							this.editor(mod.file.child('bundles').child(v + '.properties'), mod)
						)).growX().right().pad(10).row();
						// if (Core.graphics.getWidth() > Core.graphics.getHeight() && i % 2 == 1) t.row();
					});
				})),
				/* scripts */
				Table(Tex.whiteui.tint(.7, .7, 1, .8), cons(t => {
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
							right.button(Icon.trash, Styles.clearPartiali, run(() => Vars.ui.showConfirm('$confirm',Core.bundle.format('confirm.remove', f.name()), run(() => {
								f.delete();
								buttons.splice(i, 1).clear();
							})))).size(50);
						})).grow();
					}), IntStyles[1].cont, run(() => this.editor(f, mod))).width(w - 20).get()

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
							table.button('$ok',Icon.ok, run(() => {
								if (name.getText() == 'main') return Vars.ui.showErrorMessage('文件名不能为[orange]main[]。');
								let toFile = scripts.child(name.getText() +'.js');
								function go() {
									toFile.writeString('');
									dialog.hide();
									build(cont, buttons.length - 1, toFile);
								}
								if (toFile.exists()) {
									Vars.ui.showConfirm('覆盖','同名文件已存在\n是否要覆盖',run(() => go()));
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
			let cont = desc.table().fillX().get();

			tables.forEach((table, i) => {
				head.button(cons(b => {
					b.add('$' + names[i][0], colors[i]).padRight(10 + 5).growY().row();
					
					let image = b.image().size(w / 3 - 1, 4).growX().get();
					b.update(run(() => image.setColor(selected == i ? colors[i] : Color.gray)));
				}), IntStyles[1].cont, run(() => {
					if (selected == i) return;
					selected = i;
					cont.clearChildren();
					cont.add(table);
				})).size(w / 3, 60);
			});
			head.children.get(0).fireClick();
		})).fillX().fillY().get().setScrollingDisabled(true, false);

		dialog.show();
	},
	showJson(json, mod) {
		if (!/hjson|json/.test(json.extension())) return;

		let dialog = new Dialog(json.name() != null ? json.name() : '');
		let w = Core.graphics.getWidth(),
			h = Core.graphics.getHeight();

		bw = w > h ? 550 : 450;
		bh = w > h ? 64 : 70;

		let getText = () => json.readString().replace(/\r/g, '\n').replace(/\t/g, '  ').replace(/\[\]/g, '[ ]')
		let label = new Label(getText());

		dialog.cont.table(cons(p => {
			p.center();
			p.defaults().padTop(10).left();
			p.add('$editor.sourceCode', Color.gray).padRight(10).padTop(0).row();
			p.table(cons(t => {
				t.right();
				t.button(Icon.download, Styles.clearPartiali, run(() => Vars.ui
					.showConfirm(
						'粘贴', '是否要粘贴', run(() => {
							json.writeString(Core.app.getClipboardText());
							dialog.hide();
							this.showJson(json, mod);
						}))));
				t.button(Icon.copy, Styles.clearPartiali, run(() => {
					Core.app.setClipboardText(json.readString());
				}));
				/* t.button('复制为js', Icon.upload, run(() => {
					let obj = eval(json.readString());
					Core.app.setClipboardText(obj);
				})); */
			})).growX().right().row();
			p.pane(cons(p => p.left().add(label))).width(bw).height(h / 3);
		})).size(bw, h / 2).grow().row();

		dialog.buttons.button('$back', Icon.left, Styles.defaultt, run(() => {
			dialog.hide();
		})).size(bw / 2, bh);
		dialog.buttons.button('$edit', Icon.edit, Styles.defaultt, run(() => {
			this.editor(json, mod).hidden(run(() => label.setText(getText())));
		})).size(bw / 2, bh);

		dialog.show();
		return dialog;
	},
	setup(dialog) {
		Vars.ui.loadfrag.show();

		dialog.cont.clear();
		dialog.buttons.clear();

		this.mods = dataDirectory.list();

		dialog.cont.add('$mod.advise').top().row();
		dialog.cont.table(Styles.none, cons(t => t.pane(cons(p => {
			if (this.mods.length == 0) {
				p.table(Styles.black6, cons(t => t.add('$mods.none'))).height(80);
				return;
			}

			let h = 110, w = Vars.mobile ? (Core.graphics.getWidth() > Core.graphics.getHeight() ? 50 : 0) + 440 : 524;

			this.mods.forEach(file => {
				let mod = this.newMod(file);
				if (mod == null) return;

				p.button(cons(b => {
					b.top().left();
					b.margin(12);
					b.defaults().left().top();

					b.table(cons(title => {
						title.left();

						let image = extend(BorderImage, {});
						if (mod.file.child('icon.png').exists()) {
							image.setDrawable(
								TextureRegion(Texture(mod.file.child('icon.png')))
							);
						} else {
							image.setDrawable(Tex.nomap);
						}
						image.border(Pal.accent);
						title.add(image).size(h - 8).padTop(-8).padLeft(-8).padRight(8);

						title.table(cons(text => {
							text.add('[accent]' + /*Strings.stripColors*/mod.displayName() + '\n[lightgray]v' +
								mod.meta.version).wrap().width(300).growX().left();

						})).top().growX();

						title.add().growX().left();
					}));
					b.table(cons(right => {
						right.right();
						right.button(Icon.edit, Styles.clearPartiali, run(() => 
							this.editModJson(mod.file.child('mod.json').exists() ? mod.file.child('mod.json') : mod.file.child('mod.hjson'))
						)).size(50);
						right.button(Icon.trash, Styles.clearPartiali, run(() => 
							Vars.ui.showConfirm('$confirm', '$mod.remove.confirm', run(() => {
								dataDirectory.list()[i].deleteDirectory();
								this.setup(this.ui);
							}))
						)).size(50).row();
						right.button(Icon.upload, Styles.clearPartiali,run(() => {
							let file = Vars.modDirectory;
							function upload(){
								try {
									new ZipFi(mod.file).copyTo(file);
								} catch(e) {
									mod.file.copyTo(file);
								}
							}
							if (file.child(mod.file.name()).exists())
								Vars.ui.showConfirm('覆盖', '同名文件已存在\n是否要覆盖', run(upload));
							else upload();
						})).size(50);
						right.button(Icon.link, Styles.clearPartiali, run(() => Core.app.openFolder(mod.file.absolutePath()))).size(50);
					})).growX().right().padRight(-8).padTop(-8);
				}), IntStyles[1].cont, run(() => {
					this.showMod(mod);
				})).size(w, h).growX().pad(4).row();
			});
		})))).fillX().fillY().row();

		let style = Styles.defaultt,
			margin = 12,
			buttons = dialog.buttons;

		buttons.button('$back', Icon.left, style, run(() => dialog.hide())).margin(margin).size(210, 60);
		buttons.button('$mod.add', Icon.add, style, run(() => {
			let dialog = new BaseDialog('$mod.add'),
				bstyle = Styles.cleart;

			dialog.cont.table(Tex.button, cons(t => {
				t.defaults().left().size(300, 70);
				t.margin(12);

				t.button('$mod.import.file', Icon.file, bstyle, run(() => {
					dialog.hide();

					Vars.platform.showMultiFileChooser(file => {
						try {
							let obj = IntFunc.HjsonParse(file.readString());
							
							dataDirectory.child(file.name()).child('mod.json').writeString(
								obj['mod.json'] != null ? obj['mod.json'] :
								obj['mod.hjson'] != null ? obj['mod.hjson'] : '{}'
							);

							function reload(arr) {
								for (let k in arr) {
									let content = obj[arr[k]];
									for (let type in content) {
										let contentType = content[type];
										for (let json in contentType) {
											dataDirectory.child(file.name()).child(k).child(type)
											.child(json + '.json').writeString(contentType[json]);
										}
									}
								}
							}
							reload(['content', 'sprites',
								'sprites-override',
								'bundles',
								'scripts', 'sounds'
							]);
							this.importMod(file);
						} catch (e) {
							Vars.ui.showErrorMessage(e);
						}
					}, 'json', 'hjson');
				})).margin(12).marginBottom(2).row();
				t.button('$mod.add', Icon.add, bstyle, run(() => {
					this.editModJson(dataDirectory.child('/tmp/').child('mod.hjson'));
				})).margin(12).marginBottom(2);
			}));
			dialog.addCloseButton();
			dialog.show();
		})).margin(margin).size(210, 64).row();

		if (!Vars.mobile) buttons.button('$mods.openfolder', Icon.link, style, run(() => { 
			Core.app.openFolder(dataDirectory.absolutePath())
		})).margin(margin).size(210, 64);
		buttons.button('$quit', Icon.exit, style, run(() => Core.app.exit())).margin(margin).size(210, 64);

		Vars.ui.loadfrag.hide();
	},
	load() {
		this.ui = new BaseDialog(this.name);


	},
	buildConfiguration(table) {
		this.setup(this.ui);
		this.ui.show();
	}
};

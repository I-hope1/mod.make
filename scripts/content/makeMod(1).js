
const IntStyles = require('styles');
const IntFunc = require('func/index');
const sprites = {},
	ModJsonList = {};
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
	types[k].unshift(k.split('').map((s, i) => i == 0 ? s.toUpperCase() : i == k.length - 1 ? '' : s).join(''));
	types[k] = types[k].toString().split(',');
}

// 查找图片
function find(mod, name) {
	let error = Core.atlas.find('error');
	if (!mod.exists()) return error;
	let all = sprites[mod.name()];
	return all != null && all.length != 0 && all.map(e => e.name()).indexOf(name + '.png') != -1 ? new TextureRegion(
		new Texture(all[index])) : error;
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
	name: Core.bundle.get('makeMod.localizedName', 'makeMod'),
	// 暂时没用
	/* scripts: {
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
			return table;
		},
		defined() {
			let obj = new JavaAdapter(this, {
				type: 'defined',
				color: Color.sky,
				name: 'result',
				value: '"value"',
				toString() {
					return 'let ' + this.name + ' = ' + this.value + ';';
				}
			});
			obj.buildChildren = function(table) {
					table.add('$name');
					let _name = table.add(new TextField(this.name)).get();
					table.add(' = ');
					table.add('$value');
					let _value = table.add(new TextField(this.value)).get();
					table.update(run(() => {
						this.name = _name.getText();
						this.value = _value.getText();
					}));
				},
				obj.remove = function() {
					if (this.table != null) this.table.remove();
				}
			return obj;
		}
	}, */
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
				case 'json':
				case 'hjson': {
					let obj = result.value = IntFunc.HjsonParse(file.readString());
					let parentname = file.parent().name();
					let parenttype = parentname[0].toUpperCase() + parentname.slice(1, -1);
					obj.type = obj.type != null ? obj.type : types[parentname] != null && types[
						parentname][0] != null ? types[parentname][0] : 'none';

					// type接口
					p.table(Tex.clear, cons(t => {
						t.add('$type').padRight(2);
						let button = new Button(IntStyles[1].cont);
						t.add(button).size(200, 40);
						button.label(() => '$' + obj.type.toLowerCase()).grow().row();
						button.image().color(Color.gray).fillX();
						button.clicked(run(() => IntFunc.showSelectTable(button, (p, hide, v) => {
							let arr = [];
							for (let k in types) {
								types[k].forEach(e => arr.push(e));
							}
							arr.push('none');
							for (let type of arr) {
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
							ts.splice(i, 0, table.table(i % 3 == 0 ? Tex.whiteui.tint(0, 1, 1, .7) : i % 3 == 1 ? Tex.whiteui.tint(1, 1, 0, .7) : Tex.whiteui.tint(1, 0, 1, .7), cons(t => {
								// 不行，不能定义变量
								/* // 让函数拥有变量
								eval(('' + IntFunc.buildContent).replace(/function\s*\(\)\s*\{([^]+)\}/, '$1')); */

								t.left().add('"' + Core.bundle.get('content.' + k, k) + '"').padLeft(2);
								t.add(': ');

								IntFunc.buildContent(obj, [ts, t, i, k ,v]);

								t.button('', Icon.trash, Styles.cleart, run(() => tablesChange(ts, t, i, true, k, v)));
								if (Core.bundle.has(k + '.help')) t.add('// ' + Core.bundle.get(k + '.help')).padLeft(2);
							})).fillX().left().get());
						}
						else { // remove为true是时执行
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
									let name = p.table(cons(t => t.add(
												'$name')
											.growX().left().row())).get()
										.add(
											new TextField).width(300).get();
									p.row();
									let value = p.table(cons(t => t.add(
												'$value').growX().left()
											.row())).get().add(
											new TextField)
										.width(300).get();
									p.row();
									p.button('$ok', Styles.cleart, run(() => {
										tableChange(tables,
											tables
											.length, false, name
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
								let arr = IntFunc.forIn(content, 
									(k, obj) =>
									typeof obj[k] != 'function' && !
									/id|self|unlocked|stats|bars|inlineDescription|delegee|details|minfo|cicon|cacheLayer|region|iconId/
									.test(k) && (reg.test(k) || reg
										.test(
											Core.bundle.get('content.' +
												k)
										)), (k, obj) => {
										p.button(Core.bundle.get(
													'content.' + k, k),
												Styles.cleart, run(
													() => {
														tablesChange(
															tables,
															table,
															tables
															.length,
															false,
															k, obj[k]);
														hide.run()
													})).size(Core
												.graphics
												.getWidth() * .2, 45)
											.disabled(result.value[k] !=
												null).row();
									});
							}, obj.type != 'none'))).fillX().growX().get();
						t.row();
						t.table(cons(t => {
							// 研究
							var k = 'research';
							t.add(Core.bundle.get(k, k));
							t.add(':');
							
							let techs = TechTree.all.toArray();

							let btn = t.button(obj[k] != '' && obj[k] != null ? obj[k] : '$none', Styles.cleart, run(
									() => IntFunc.showSelectTable(btn, (p, hide,
										v) => {
										p.clearChildren();
										p.button('$none', Styles
											.cleart,
											run(() => {
												obj[k] = '';
												btn.setText(
													'$none'
												);
											}));
										let reg = RegExp(v, 'i');
										for (let tech of techs) {
											let t = tech.content;
											if (reg.test(t.name) ||
												reg
												.test(t
													.localizedName))
												p.button(t
													.localizedName,
													Styles.cleart,
													run(() => {
														btn.setText(obj[k] = t.name.replace(modName + '-', ''));
														hide.run();
													})).size(200, 45).row();
										}
									}, true))).size(100, 60).get();
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
					let cont = p.table(Tex.whiteui.tint(.5, .6, .1, .8)).get();
					let fun = (from, to) => {
						let table = cont.table(Tex.button, cons(t => {
							obj.from.push(t.add(new TextField(from)).width(200).get());
							t.add(' = ', Color.gray);
							obj.to.push(t.add(new TextField(to)).width(200).get());
						})).get();
						let index = obj.from.length - 1;
						this.doubleClick(table, [run(() => {
							Vars.ui.showConfirm('$confirm', Core.bundle.format(
									'confirm.remove', obj.from.splice(index, 1)
								),
								run(() => table.remove()));
						}), run(() => {})]);
						cont.row();
					}
					str.forEach(e => {
						let arr = e.split(' = ');
						fun(arr.splice(0, 1), arr.join(' = '));
					});
					p.row();
					p.button('$add', Icon.add, run(() => fun('', '')));
				}
				break;
				case 'js': {
					p.top().defaults().top().grow();
	
					let cont = p.table().get();
					p.row();
					let btn = p.button('$add', Icon.add, run(() => {
						IntFunc.showSelectTable(btn, (p, hide) => {
							for (let _k in this.scripts) {
								if (_k == 'build') continue;
								let k = _k;
								p.button('$' + k, Styles.cleart, run(() => {
										let build = this.scripts[k]();
										cont.add(build.build()).row();
									})).size(Core.graphics.getWidth() * .2, 45)
									.row();
							}
						});
					})).size(220, 75).bottom().get();
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
		})).size(w, Core.graphics.getHeight() - 400).grow().row();

		dialog.buttons.button('$back', Icon.left, run(() => dialog.hide())).size(220, 70);

		dialog.buttons.button('$ok', Icon.ok, run(() => {
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

					file = mod.child('content').child(obj.type == 'none' ? 'blocks' :
						Object
						.keys(types).map(e => types[e].indexOf(obj.type) != -1 ? e :
							'')
						.join('')).child(file.name());
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
			if (fileName) {
				let toFile = file.parent().child(fileName.getText() + '.' + file.extension());
				file.moveTo(toFile);
				this.showJson(toFile, mod);
			}
			dialog.hide();
		})).size(220, 70);

		dialog.show();
		Vars.ui.loadfrag.hide();
	},
	editModJson(file) {
		let obj = IntFunc.HjsonParse(file.exists() ? file.readString() : null);
		let isNull = obj == null;
		obj = isNull ? {} : obj;
		let ui = new Dialog(isNull ? '$mod.create' : '$edit');
		let arr = ['name', 'displayName', 'description', 'author', 'version', 'main'],
			cont = new Table();
		cont.add('$mod.fileName');
		let filename = obj.fileName = cont.add(new TextField(!isNull ? file.parent().nameWithoutExtension() :
				''))
			.get();
		filename.changed(run(() => {
			ok.setDisabled(filename.getText().replace(/\s/g, '') == '');
		}));
		cont.row();
		cont.add('$minGameVersion');
		let mingameversion = obj.minGameVersion = cont.add(new TextField(!isNull && obj.minGameVersion ? obj
			.minGameVersion : '105')).get();
		mingameversion.changed(run(() => {
			ok.setDisabled((mingameversion.getText() | 0) < 105 || (mingameversion.getText() | 0) >
				Version.build);
		}));
		cont.row();
		for (let i of arr) {
			cont.add('$' + i);
			let field = new TextField(!isNull && obj[i] != null ? obj[i].replace(/\n|\r/g, '\\n') : '');
			field.clicked(run(function() {
				// 如果长按时间大于800毫秒
				if (Time.millis() - this.visualPressedTime - this.visualPressedDuration * 1000 >
					800)
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
			mod.child(isNull ? 'mod.json' : 'mod.' + file.extension()).writeString('{\n' + strArr.join(
					',\n') +
				',\n}');
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
				return;
			}
			func(mod);
		})).size(Math.max(w, h) * 0.1, Math.min(w, h) * 0.1).get();
		ui.cont.add(buttons);
		ui.show();
	},
	
	showMod(mod) {
		Vars.ui.loadfrag.show();
		this._showMod(mod);
		Time.run(2, run(() => Vars.ui.loadfrag.hide()));
	},
	_showMod(mod) {
		let json = ModJsonList[mod.name()];
		let sprites = mod.child('sprites').exists() ? mod.child('sprites') : null;
		let displayName = json.displayName != null ? json.displayName : json.name;
		let w = 420;
		let dialog = new BaseDialog(displayName);

		dialog.addCloseButton();
		dialog.cont.pane(cons(desc => {
			desc.center();
			desc.defaults().padTop(10).left();

			if (json == {}) {
				desc.add('$error', Color.red);
				return;
			}

			desc.add('$editor.name', Color.gray).padRight(10).padTop(0).row();
			desc.add(displayName).growX().wrap().padTop(2).row();

			if (json.author != null) {
				desc.add('$editor.author', Color.gray).padRight(10).row();
				desc.add(json.author).growX().wrap().padTop(2).row();
			}
			if (json.version != null) {
				desc.add('$editor.version', Color.gray).padRight(10).row();
				desc.add(json.version).growX().wrap().padTop(2).row();
			}
			if (json.description != null) {
				desc.add('$editor.description').padRight(10).color(Color.gray).top().row();
				desc.add(json.description).growX().wrap().padTop(2).row();
			}

			var value = 'content'; // 用于tab栏切换
			desc.table(cons(t => {
				t.button(cons(b => {
						b.add('$editor.content', Color.gold).padRight(10 + 5)
							.growY().row();
						let image = b.image().size(w / 3 - 1, 4).growX().get();
						b.update(run(() => image.setColor(value ==
							'content' ?
							Color.gold : Color.gray)));
					}), IntStyles[1].cont, run(() => _setup('content'))).size(w / 3, 60)
					.growY();
				t.button(cons(b => {
					b.add('$bundles', Color.pink).padRight(10 + 5).growY()
						.row();
					let image = b.image().size(w / 3 - 1, 4).growX().get();
					b.update(run(() => image.setColor(value ==
						'bundles' ?
						Color.pink : Color.gray)));
				}), IntStyles[1].cont, run(() => _setup('bundles'))).size(w / 3, 60);
				t.button(cons(b => {
					b.add('$scripts', Color.sky).padRight(10 + 5).growY()
						.row();
					let image = b.image().size(w / 3 - 1, 4).growX().get();
					b.update(run(() => image.setColor(value ==
						'scripts' ?
						Color.sky : Color.gray)));
				}), IntStyles[1].cont, run(() => _setup('scripts'))).size(w / 3, 60);
			})).fillX().row();

			let cont = desc.add(new Table).fillX().get();
			let _setup = str => {
				value = str;
				cont.clearChildren();
				switch (str) {
					case 'content':
						cont.table(Styles.none, cons(t => {
							t.center();
							t.defaults().padTop(10).left();
							let color = Color.valueOf('#ccccff');
							let content = mod.child('content');
							let files = content.findAll().toArray();
							let cont = new Table();
							cont.defaults().padTop(10).left();
							let setup = str => {
								cont.clearChildren();
								let body = new Table();
								body.top().left();
								body.defaults().padTop(2).top().left();
								cont.pane(cons(p => p.add(body).left().grow()
									.get()
									.left())).fillX().minWidth(450).row();
								let files = str instanceof RegExp ? content
									.findAll().toArray().map(e => str.test(e) ?
										e :
										null) : str instanceof Cons ? content
									.findAll().toArray().map(e => str(e) ? e :
										null) : str == '' ? content.findAll()
									.toArray() : content.child(str).findAll()
									.toArray();
								for (let i = 0, len = files.length; i <
									len; i++) {
									let json = files[i];
									if (json != null && json.extension() !=
										'json' && json.extension() != 'hjson')
										continue;

									body.button(cons(b => {
											b.left()
											b.image(find(mod, json
													.nameWithoutExtension()
												)).size(32).padRight(6)
												.left();
											if (!Vars.mobile) image
												.addListener(
													new HandCursorListener()
												);
											b.add(json.name()).top();
											let clicked = () => {
												this.showJson(json,
													mod);
												dialog.hide();
											}
											b.addListener(extend(
												ClickListener, {
													clicked(a,
														b,
														c) {
														if (Time
															.millis() -
															this
															.visualPressedTime -
															this
															.visualPressedDuration *
															1000 >
															800)
															Vars
															.ui
															.showConfirm(
																'$confirm',
																Core
																.bundle
																.format(
																	'confirm.remove',
																	json
																	.nameWithoutExtension()
																),
																run(() => {
																	json
																		.delete();
																	setup
																		(
																			str
																		);
																})
															);
														else
															clicked();
													}
												}));
										}), Styles.defaultb, run(() => {}))
										.fillX()
										.minWidth(400).pad(2).left().row();
								}
							}

							t.add('$content.info').row();
							t.add(cont).grow().size(w).row();
							t.button('$add', Icon.add, run(() => {
								let ui = new Dialog('');
								let name = ui.cont.table(cons(t => t
										.add(
											'$name'))).get().add(
										TextField())
									.get();
								ui.buttons.table(cons(t => t.button(
										'$back',
										run(() => ui.hide())))).get()
									.button('$ok', run(() => {
										let file = content
											.child(
												'blocks').child(
												name
												.getText() +
												'.json'
											);
										file.writeString('');
										dialog.hide();
										ui.hide();
										this.editor(file, mod);
									}));
								ui.show();
							})).fillX().row();
							t.button('查看图片库', run(() => {
								let ui = new BaseDialog('图片库');
								ui.cont.pane(cons(p => {
									let all = sprites !=
										null ?
										sprites.findAll()
										.toArray() : [];
									for (let f of all) {
										if (f.extension() !=
											'png') return;
										p.add('' + f
											.nameWithoutExtension()
										).row();
										p.image().color(
												Color
												.gray)
											.fillX()
											.row();
										p.image(TextureRegion(
												Texture(
													f)))
											.row();
									}
								})).fillX().fillY().row();
								ui.addCloseButton();
								ui.buttons.button('$add', Icon.add, run(
									() => {})).width(90);
								ui.show();
							})).fillX();
							setup('');
						})).width(w - 10).row();
						break;
					case 'bundles':
						cont.table(Tex.whiteui.tint(1, .8, 1, .8), cons(t => {
							for (let _k in bundles) {
								let k = _k;
								t.add('$bundle.' + bundles[k]).left();
								t.button(Icon.pencil, Styles.clearTransi, run(() =>
										this
										.editor(mod.child('bundles').child(
											bundles[
												k] + '.properties'), mod))).right()
									.pad(10)
									.row();
							}
						})).minWidth(400).fillX().row();
						break;
					case 'scripts':
						cont.table(Tex.whiteui.tint(.7, .7, 1, .8), cons(t => {
							let scripts = mod.child('scripts');
							let main = scripts.child('main.js');
							if (!main.exists()) main.writeString('');
							let cont = new Table;

							let all = scripts.findAll().toArray();
							let buttons = [];
							
							function buildButton(cont, i, f) {
								let name = f.nameWithoutExtension();
								return cont.button(cons(b => {
									b.top().left();
									b.margin(12);
									b.defaults().left().top();
									b.table(cons(title => {
										title.left();
										title.image(Core.atlas.find(modName + '-js.file', Tex.clear)).size(102).padTop(8).padLeft(-8).padRight(8);
										title.add(f.name()).wrap().width(170).growX().left()/* .get().clicked(run(() => {
											
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
								}), IntStyles[1].cont, run(() => this.editor(f, mod))).fillX().get()
							}
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
								})).fillX();
								// t.button('导入插件', Icon.download, run(() => {})).fillX();
								t.button('test', run(() => {
									// let o = Vars.mods.scripts.runConsole(main.readString());
									// if(o != null) Vars.ui.showInfo('' + o);
									Vars.mods.scripts.run(Vars.mods.locateMod(mod.name()), main.readString());
								})).fillX();
							})).name('buttons').minWidth(200).fillX();

							t.add(cont);
						})).fillX().height(Core.graphics.getHeight() * .8);
				}
			}

			_setup(value);
		})).width(w + 30).fillY();

		dialog.show();
	},
	showJson(json, mod) {
		if (!/hjson|json/.test(json.extension())) return;

		let dialog = new Dialog(json.name() != null ? json.name() : '');
		let w = Core.graphics.getWidth(),
			h = Core.graphics.getHeight();
		let max = Math.min(w, h) - 200;

		dialog.cont.pane(cons(p => {
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
			p.add(json.readString().replace(/\r/g, '\n').replace(/\t/g, '  ').replace(/\[\]/g, '[ ]'));
		})).size(max).grow().row();

		dialog.buttons.button('$back', Icon.left, Styles.defaultt, run(() => {
			this.showMod(mod);
			dialog.hide();
		})).size(max * 0.5, Math.min(w, h) * 0.1);
		dialog.buttons.button('$edit', Icon.edit, Styles.defaultt, run(() => {
			this.editor(json, mod);
			dialog.hide();
		})).size(max * 0.5, Math.min(w, h) * 0.1);

		dialog.show();
	},
	setup(dialog) {
		Vars.ui.loadfrag.show();

		for (let key in sprites) {
			delete sprites[key];
		}

		dialog.cont.clear();
		dialog.buttons.clear();

		this.mods = dataDirectory.list();

		dialog.cont.add('$mod.advise').row();
		dialog.cont.table(Styles.none, cons(t => t.pane(cons(p => {
			if (this.mods.length == 0) {
				p.table(Styles.black6, cons(t => t.add('$mods.none'))).height(80);
				return;
			}
			for (let i in this.mods) {
				let mod = this.mods[i];
				let json = IntFunc.HjsonParse(mod.child('mod.json').exists() ? mod.child(
						'mod.json')
					.readString() : mod.child('mod.hjson').exists() ? mod.child(
						'mod.hjson')
					.readString() : null);
				if (json == null) continue;
				let displayName = json.displayName != null ? json.displayName : json.name;
				ModJsonList[mod.name()] = json;
				sprites[mod.name()] = mod.child('sprites').exists() ? mod.child('sprites')
					.findAll().toArray() : null;

				p.button(cons(b => {
					b.top().left();
					b.margin(12);
					b.defaults().left().top();

					b.table(cons(title => {
						title.left();
						let image = extend(BorderImage, {});
						if (mod.child('icon.png').exists()) {
							image.setDrawable(new TextureRegion(
								new Texture(mod.child(
									'icon.png'))));
						} else {
							image.setDrawable(Tex.clear);
						}
						image.border(Pal.accent);
						title.add(image).size(102).padTop(-8)
							.padLeft(-
								8).padRight(8);
						title.add(displayName + '\n[lightgray]v' +
								json
								.version).wrap().width(170).growX()
							.left();
						title.add().growX().left();
					}));
					b.table(cons(right => {
						right.right().top();
						right.defaults().right().top();
						right.button(Icon.edit, Styles
							.clearPartiali,
							run(() => this.editModJson(mod
								.child(
									'mod.json').exists() ?
								mod.child('mod.json') :
								mod.child('mod.hjson')))).size(
							50);
						right.button(Icon.trash, Styles
							.clearPartiali,
							run(() => Vars.ui.showConfirm(
								'$confirm',
								'$mod.remove.confirm', run(
									() => {
										dataDirectory
											.list()[i]
											.deleteDirectory();
										this.setup(this.ui);
									})))).size(50).row();
						right.button(Icon.upload, Styles
							.clearPartiali,
							run(() => {
								let file = Vars
									.modDirectory;
								if (file.child(mod.name())
									.exists()) Vars.ui
									.showConfirm('覆盖',
										'同名文件已存在\n是否要覆盖',
										run(
											() => mod
											.copyTo(
												file))
									);
								else mod.copyTo(file);
							})).size(50);
						right.button(Icon.link, Styles
							.clearPartiali,
							run(() => Core.app.openFolder(mod
								.absolutePath()))).size(50);
					})).grow();
				}), IntStyles[1].cont, run(() => {
					this.showMod(mod);
				})).size(Core.graphics.getWidth() * .8, 120).row();
			}
		})))).grow().size(Core.graphics.getWidth() * .81, Core.graphics.getHeight() * .7).row();

		let style = Styles.defaultt,
			margin = 12,
			buttons = dialog.buttons;

		buttons.button('$back', Icon.left, style, run(() => dialog.hide())).margin(margin).size(Core.graphics
			.getWidth() * .4, 60);
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
							let obj = IntFunc.HjsonParse(file
								.readString());
							dataDirectory.child(file.name()).child(
								'mod.json').writeString(obj[
								'mod.json'] != null ? obj[
								'mod.json'] : obj[
								'mod.hjson'] != null ? obj[
								'mod.hjson'] : {});

							function reload(arr) {
								for (let k in arr) {
									let content = obj[arr[k]];
									for (let type in content) {
										let contentType = content[
											type];
										for (let json in
												contentType)
											dataDirectory.child(file
												.name()).child(k)
											.child(
												type).child(json +
												'.json')
											.writeString(
												contentType[json]);
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
					this.editModJson(dataDirectory.child('/tmp/').child(
						'mod.hjson'));
				})).margin(12).marginBottom(2);
			}));
			dialog.addCloseButton();
			dialog.show();
		})).margin(margin).size(Core.graphics.getWidth() * .4, 60).row();
		buttons.button('$mods.openfolder', Icon.link, style, run(() => Core.app.openFolder(dataDirectory
			.absolutePath()))).margin(margin).size(Core.graphics.getWidth() * .4, 60);
		buttons.button('$quit', Icon.exit, style, run(() => Core.app.exit())).margin(margin).size(Core.graphics
			.getWidth() * .4, 60);

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


const findClass = require('func/findClass')
const mod_Name = modName
/*
function BaseSetting(objName, key, name, defaultValue, info) {
	object['obj_' + objName][key] = this;

	this.name = name;
	this.parent = object['obj_' + objName];
	let v = settings.get(mod_Name + key);
	this.value = v == null ? defaultValue : v;
	this.build = () => new Label("???");
	this.info = info;
}
function _CheckBox(objName, key, name, defaultValue, info) {
	BaseSetting.call(this, objName, key, name, defaultValue, info);
	this.value = this.value == "true";

	let elem;
	this.build = function () {
		elem = new CheckBox(this.name);
		elem.changed(() => this.set(elem.isChecked()))
		elem.setChecked(this.value)
		elem.update(() => elem.setDisabled(!!this.parent.disabled))
		elem.setOrigin(Align.left)
		return elem;
	}
	this.set = function (value) {
		this.value = value;
		settings.set(mod_Name + key, value);
		elem.setChecked(value);
	}
}
function _Slider(objName, key, name, defaultValue, min, max, step, sp, info) {
	BaseSetting.call(this, objName, key, name, defaultValue, info);
	let v = +this.value;
	this.value = isNaN(v) ? defaultValue : v;

	let elem;
	this.build = function () {
		let content = new Table()

		let slider = elem = new Slider(min, max, step, false);
		slider.setValue(this.value);

		let label = new Label("", Styles.outlineLabel);
		content.add(this.name, Styles.outlineLabel).left().growX().wrap();
		content.add(label).padLeft(10).right();
		content.margin(3, 20, 3, 20);
		content.touchable = Touchable.disabled;

		slider.changed(() => {
			this.set(slider.getValue());
			label.setText("" + sp(slider.getValue()));
		});
		slider.change();

		let t = new Table()
		t.defaults().growX()
		t.stack(slider, content).width(360).left().padTop(4).growX();

		return t;
	}
	this.set = function (value) {
		this.value = value;
		settings.set(mod_Name + key, value);
		elem.setValue(value);
	}
}*/

let object = {
	name: 'settings',
	buildConfiguration() {
		findClass('IntUI').settingsDialog.show();
	}
}
/*
new _CheckBox("base", "display-content-sprite", "显示content图片", true)
new _CheckBox("base", "display_mod_logo", "显示mod的logo", false)
new _CheckBox("base", "not_show_again", "不再显示更新日志", false)

new _CheckBox("editor", "auto_fold_code", "自动折叠代码", false)
new _CheckBox("editor", "display_deprecated", "显示禁用类(字段)", false, "需要重启")
new _CheckBox("editor", "point_out_unknown_field", "指出未知字段", false)
new _CheckBox("editor", "colorful_table", "多彩样式", false)

new _CheckBox("base", "auto_load_mod", "自动加载mod", false, "清除所有content，加载此mod\n[red](不会导入到游戏的mods文件夹，实验性)")
new _CheckBox("loadMod", "load_sprites", "加载图集", false, "未完成(存在bug)")
new _CheckBox("loadMod", "display_exception", "显示报错", true)*/
// new _Slider("loadMod", "compiling_times_per_second", "每秒最多编译次数", 1000, 500, 10000, 10, v => v + "/次");

this[modName + "-contArr"].push(object)
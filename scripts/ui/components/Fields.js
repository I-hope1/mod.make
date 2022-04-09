
const buildContent = require('func/buildContent')
const IntSettings = require("content/settings");
const { MyObject, MyArray } = require('func/constructor')

let backgrounds;
exports.load = function () {
	backgrounds = [
		Tex.whiteui.tint(.6, .8, .6, .6),
		Tex.whiteui.tint(.8, .6, .8, .6),
		Tex.whiteui.tint(.8, .8, .6, .6),
		Tex.whiteui.tint(.6, .8, .8, .6),
	]
}
exports.colorfulTable = (i, _cons) => {
	let bg;
	if (IntSettings.getValue("editor", "colorful_table")) {
		bg = backgrounds[++i % 4]
	} else {
		bg = Tex.pane;
	}
	return new Table(bg, _cons)
}

exports.json = function (fields, i, key) {
	return this.colorfulTable(i, table => {
		table.left().defaults().left();
		buildContent.build(fields.type, fields, table, key, fields.map.get(key))
	})
}

exports.constructor = function (value, type, table) {
	if (value == null) throw Error("'value' can't be null");

	if (value instanceof MyArray || value instanceof MyObject) {
		this.map = value
	} else throw new TypeError("'" + value + "' is not MyArray or MyObject")

	this.table = table
	Object.defineProperty(this, 'type', { get: () => type.get() })
	let i = 0;
	this.add = function (table, key, value) {
		if (value != null && !this.map.has(key)) {
			this.map.put(key, value)
		}
		let t = table || exports.json(this, i++, key)
		this.table.add(t).fillX().row()
	}
	this.remove = function (item, key) {
		this.map.remove(key)
		if (item != null) item.remove()
		else Log.err("can't remove key: " + key)
	}
}
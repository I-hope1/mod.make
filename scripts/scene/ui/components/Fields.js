
const buildContent = require('func/buildContent')
const IntCons = require('func/constructor')

exports.colorfulTable = (i, cons) => {
	return new Table(i % 3 == 0 ? Tex.whiteui.tint(0, 1, 1, .7) :
		i % 3 == 1 ? Tex.whiteui.tint(1, 1, 0, .7) :
			Tex.whiteui.tint(1, 0, 1, .7), cons)
}

exports.json = function (fields, i, key) {
	return this.colorfulTable(i, table => {
		// 不行，不能定义变量
		/* // 让函数拥有变量
		eval(('' + IntFunc.buildContent).replace(/function\s*\(\)\s*\{([^]+)\}/, '$1')); */

		table.left().defaults().left();
		// try {
		buildContent.build(fields.type, fields, table, key, fields.map.get(key));
		/* } catch (e) {
			Vars.ui.showErrorMessage(e)
			return
		} */
	})
}

exports.constructor = function (map, type, table) {
	if (map == null) throw Error("'map' cannot be null")
	if (!(map instanceof IntCons.Array || map instanceof IntCons.Object)) {
		if (map instanceof Array) map = new IntCons.Array(map)
		else map = new IntCons.Object(map)
	}
	this.map = map, this.table = table
	Object.defineProperty(this, 'type', { get: () => type.get() })
	let i = 0;
	this.add = function (table, key, value) {
		if (value != null && this.map.get(key) == null) {
			this.map.put(key, value)
		}
		let t = table || exports.json(this, i++, key)
		this.table.add(t).fillX().row()
	}
	this.remove = function (item, key) {
		this.map.remove(key)
		if (item != null) item.remove()
		else Log.info(key)
	}/* 
	this.init = function(){
		
	} */
}
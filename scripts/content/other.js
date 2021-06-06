
var useable = require('testFi').useable;
exports.cont = {
	name:'more', show: false,

	all: ['lastlog', '单位生成', 'showcrashes', 'select'].map(str => {
		try {
			var cont = require('content/others/' + str).cont;
			return !cont.needFi || useable ? cont : null;
		} catch (e) {
			Log.err('' + e);
		}
		return null;
	}),

	load(){
		let t = this.ui = new Table;
		this.all.forEach(cont => {
			if (cont == null) return;
			cont.load();
			t.button(cont.name, run(() => cont.click && cont.click())).size(120, 40).disabled(new Boolf({get:() => !!cont.disabled})).row();
		})
	},
	buildConfiguration(table){
		this.show = true//!this.show;
		// this.btn.setChecked(this.show);
		if (this.show) table.add(this.ui);
	}
}
exports.cont = {
	name: 'lastlog', needFi: true,
	load(){
		this.ui = new BaseDialog(this.name);
		this.ui.cont.clear();
		this.ui.cont.pane(cons(p => {
			p.label(() => Vars.dataDirectory.child('last_log.txt').readString());
		})).fillX().fillY().row();
		this.ui.addCloseButton();
	},
	click(table){
		this.ui.show();
	}
};
elements['lastlog'] = {
	load(){
		this.ui = new BaseDialog('last_log');
		this.ui.addCloseButton();
	},
	buildConfiguration(table){
		this.ui.cont.clear();
		this.ui.cont.pane(cons(p => {
			p.add(Vars.dataDirectory.child('last_log.txt').readString());
		})).fillX().fillY().row();
		this.ui.show();
	}
};
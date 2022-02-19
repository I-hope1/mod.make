
exports.cont = {
	name: 'settings', tables: {},
	load() {
		let dialog = this.ui = new BaseDialog("设置");
		dialog.addCloseButton();
	},
	buildConfiguration() {
		this.ui.show();
	}
}
exports.clearb = exports.nonePane = {};
Events.on(ClientLoadEvent, () => {
	exports.nonePane = new ScrollPane.ScrollPaneStyle;

	exports.clearb = new Button.ButtonStyle(Styles.defaultb);
	exports.clearb.up = Styles.none;
	exports.clearb.down = exports.clearb.over = Styles.flatOver;
})
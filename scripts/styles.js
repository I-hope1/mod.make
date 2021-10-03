exports[0] = {};
exports[1] = {};
Events.on(ClientLoadEvent, e => {
	exports[0].cont = new ScrollPane.ScrollPaneStyle(Styles.defaultPane);
	exports[0].cont.vScroll = exports[0].cont.vScrollKnob = exports[0].cont.corner = Styles.none;

	exports[1].cont = new Button.ButtonStyle(Styles.defaultb);
	exports[1].cont.up = Styles.black5;
	exports[1].cont.down = exports[1].cont.over = Styles.flatOver;
});

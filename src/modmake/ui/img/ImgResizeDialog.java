package modmake.ui.img;

import arc.Core;
import arc.func.Intc2;
import arc.math.Mathf;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import mindustry.ui.dialogs.BaseDialog;

import static modmake.IntUI.*;

public class ImgResizeDialog extends BaseDialog {
    // just advice
	public static int minSize = 1, maxSize = 1000;

    int width, height;

    public ImgResizeDialog(Intc2 cons){
        super("重设大小");

        closeOnBack();
        shown(() -> {
            cont.clear();
            width = imgEditor.width();
            height = imgEditor.height();

            Table table = new Table();

            for(boolean w : Mathf.booleans){
                table.add(w ? "@width" : "@height").padRight(8f);
                table.defaults().height(60f).padTop(8);

                table.field((w ? width : height) + "", TextField.TextFieldFilter.digitsOnly, value -> {
                    int val = Integer.parseInt(value);
                    if(w) width = val; else height = val;
                }).valid(value -> {
                    if (!Strings.canParsePositiveInt(value)) return true;
                    int i = Integer.parseInt(value);
                    return i <= maxSize && i >= minSize;
                }).maxTextLength(4);

                table.row();
            }
            cont.row();
            cont.add(table);

        });

        hidden(() -> {
            Core.scene.setKeyboardFocus(imgDialog);
        });

        buttons.defaults().size(200f, 50f);
        buttons.button("@cancel", this::hide);
        buttons.button("@ok", () -> {
            cons.get(width, height);
            hide();
        });
    }
}

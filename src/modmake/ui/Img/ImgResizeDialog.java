package modmake.ui.Img;

import arc.func.Intc2;
import arc.math.Mathf;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import mindustry.ui.dialogs.BaseDialog;

import static modmake.IntUI.imgEditor;

public class ImgResizeDialog extends BaseDialog {
	public static int minSize = 10, maxSize = 32 * 16, increment = 50;

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
                }).valid(value -> Strings.canParsePositiveInt(value) && Integer.parseInt(value) <= maxSize && Integer.parseInt(value) >= minSize).maxTextLength(3);

                table.row();
            }
            cont.row();
            cont.add(table);

        });

        buttons.defaults().size(200f, 50f);
        buttons.button("@cancel", this::hide);
        buttons.button("@ok", () -> {
            cons.get(width, height);
            hide();
        });
    }
}

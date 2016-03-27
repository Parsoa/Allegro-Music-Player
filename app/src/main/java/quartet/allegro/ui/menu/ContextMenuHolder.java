package quartet.allegro.ui.menu;

import android.view.View;

import java.util.List;

/**
 * Created by akbar on 7/3/15.
 */
public interface ContextMenuHolder {
    void showContextMenu(List<String> items, MenuItemCallback callback, int x, int y);
    void showContextMenu(List<String> items, MenuItemCallback callback, int y);
    void showContextMenu(List<String> items, MenuItemCallback callback);

}

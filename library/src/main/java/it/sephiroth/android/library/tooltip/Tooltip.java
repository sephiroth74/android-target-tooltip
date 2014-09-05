package it.sephiroth.android.library.tooltip;

/**
 * Created by alessandro on 04/09/14.
 */
public interface Tooltip {
	void show();

	void hide(boolean remove);

	void setOffsetX(int x);

	void setOffsetY(int y);

	void offsetTo(int x, int y);

	boolean isAttached();

	boolean isShown();
}

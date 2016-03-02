
**Version 1.3.12**
* Added custom font support

**Version 1.3.9**
* Changed the close policy creation. Now there's a builder for it. See the changes to the Builder `closePolicy` method.
* Added static methods `remove` and `removeAll` in the main Tooltip class.
* Added `android:layout_margins` as property of the `TooltipOverlay` style.

**Version 1.3.8**

* Removed useless TooltipManager, now just use Tooltip.make(..)
* Added `floatingAnimation` parameter for the builder. This will enable a nice floating animation on the tooltip.
* Added `ttlm_elevation` attribute for the TextView. Enable the view elevation on android-21

**Version 1.3.7**

* Added overlay pulse touch.
* Tooltips now follow correctly the target anchor

**Version 1.3.0**

* Added `android:textAppearance` in the TooltipLayout style
* Now tooltip follows the target view layout changes.

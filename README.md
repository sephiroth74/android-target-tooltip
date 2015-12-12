Android Tooltip
======================

Create Toast like tooltips, but targets can be specified, plus custom properties and features

Master: ![travis-ci](https://travis-ci.org/sephiroth74/android-target-tooltip.svg?branch=master)

Installation
===

	compile 'it.sephiroth.android.library.targettooltip:target-tooltip-library:+'
	

Usage
===

	TooltipManager manager = new TooltipManager(this);
	manager
		.show(
			new TooltipManager.Builder(101)
			.anchor(aView, TooltipManager.Gravity.BOTTOM)
			.closePolicy(TooltipManager.ClosePolicy.TouchOutside, 3000)
			.activateDelay(800)
			.showDelay(300)
			.text(R.string.hello_world)
			.maxWidth(500)
			.withArrow(true)
			.withOverlay(true)
			.build()
		);

See the inner [Builder][1] class for the complete set of options

Customization
===

Tooltip style can be customized in your style object:

    <!-- default style -->
    <declare-styleable name="TooltipLayout">
        <attr name="ttlm_padding" format="dimension" />
        <attr name="ttlm_strokeColor" format="color" />
        <attr name="ttlm_backgroundColor" format="color" />
        <attr name="ttlm_strokeWeight" format="dimension" />
        <attr name="ttlm_cornerRadius" format="dimension" />
        <attr name="ttlm_arrowRatio" format="float" />
        <attr name="android:textAppearance" />
        <attr name="ttlm_overlayStyle" format="reference" />
    </declare-styleable>


And this is the style for the overlay touch:

    <declare-styleable name="TooltipOverlay">
        <attr name="android:color" />
        <attr name="android:alpha" />
        <attr name="ttlm_repeatCount" format="integer" />
        <attr name="ttlm_duration" format="integer" />
    </declare-styleable>
	
then pass the style in the Builder method **withStyleId(int resId)**

Screenshots
===
![Screen shot](screenshots/image01.png)

[1]: https://github.com/sephiroth74/android-target-tooltip/blob/master/library/src/main/java/it/sephiroth/android/library/tooltip/TooltipManager.java#L169

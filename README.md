Android Tooltip
======================

Create Toast like tooltips, physical targets can be specified, or even points on screen.
Many additional features and customizations. Just look at the samples Activities.

[![Build Status](https://travis-ci.org/sephiroth74/android-target-tooltip.svg?branch=master)](https://travis-ci.org/sephiroth74/android-target-tooltip)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/it.sephiroth.android.library.targettooltip/target-tooltip-library/badge.svg)](https://maven-badges.herokuapp.com/maven-central/it.sephiroth.android.library.targettooltip/target-tooltip-library)

Installation
===

	compile 'it.sephiroth.android.library.targettooltip:target-tooltip-library:1.3.15'


Usage
===

	Tooltip.make(this,
			new Builder(101)
			.anchor(aView, Gravity.BOTTOM)
			.closePolicy(new ClosePolicy()
			    .insidePolicy(true, false)
			    .outsidePolicy(true, false), 3000)
			.activateDelay(800)
			.showDelay(300)
			.text(R.string.hello_world)
			.maxWidth(500)
			.withArrow(true)
			.withOverlay(true)
			.typeface(mYourCustomFont)
			.floatingAnimation(AnimationBuilder.DEFAULT)
			.build()
		).show();

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
        <attr name="ttlm_elevation" format="dimension" />

        <!-- font file path inside your assets folder -->
        <attr name="ttlm_font" format="string" />

        <!-- textview text gravity -->
        <attr name="android:gravity" />
    </declare-styleable>


And this is the style for the overlay touch:

    <declare-styleable name="TooltipOverlay">
        <attr name="android:color" />
        <attr name="android:alpha" />
        <attr name="ttlm_repeatCount" format="integer" />
        <attr name="ttlm_duration" format="integer" />
        <attr name="android:layout_margin" />
    </declare-styleable>

then pass the style in the Builder method **withStyleId(int resId)**


Video
===
[![IMAGE ALT TEXT HERE](http://img.youtube.com/vi/QitX9bnsnP4/0.jpg)](http://www.youtube.com/watch?v=QitX9bnsnP4)


Screenshots
===
![Screen shot](screenshots/image01.png)


[1]: https://github.com/sephiroth74/android-target-tooltip/blob/master/library/src/main/java/it/sephiroth/android/library/tooltip/Tooltip.java#L1471

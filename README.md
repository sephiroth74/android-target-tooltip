Android Tooltip
======================

Create Toast like tooltips, but targets can be specified, plus custom properties and features

Installation (Using [JitPack](https://jitpack.io/#Epsiloni/android-target-tooltip/1.5))
===
**Step 1**. Add the JitPack repository to your build file.  
Add this in your `build.gradle` at the end of repositories:

	repositories {
		// ...
		maven { url "https://jitpack.io" }
	}

**Step 2**. Add the dependency in the form

	dependencies {
		        compile 'com.github.Epsiloni:android-target-tooltip:1.5'
	}

Usage
===

	TooltipManager.getInstance()
		.create(MainActivity.TOOLTIP_EDITORIAL_1)
		.anchor(aView, TooltipManager.Gravity.BOTTOM)
		.closePolicy(TooltipManager.ClosePolicy.TouchOutside, 3000)
		.activateDelay(800)
		.text("Something to display in the tooltip...")
		.maxWidth(500)
		.show();

See the inner [Builder][1] class for the complete set of options

Screenshots
===
With Tooltip arrow:
![With arrow](pics/arrow.png)

Without Tooltip arrow:
![Without arrow](pics/noarrow.png)

[1]: https://github.com/Epsiloni/android-target-tooltip/blob/master/library/src/main/java/it/sephiroth/android/library/tooltip/TooltipManager.java#L169

package app.aaps.wear.watchfaces.utils

import app.aaps.shared.impl.weardata.JsonKeyValues

/**
 * Whether a view that the zip marks `visible` is drawn in the current mode.
 *
 * The `showIn` key lets a design show one thing while the watch is awake and another in always-on.
 * The seconds already work like that, but only the seconds: a date put where the seconds are would
 * sit on top of them while the watch is awake. With `showIn` the date can say "ambient" and the
 * seconds stay alone.
 *
 * Pure on purpose, like [secondVisibility]: the answer depends only on its inputs, so it can be asked
 * on every frame and every mode change without drifting.
 *
 * @param showIn the view's `showIn` value, or null / empty / unknown for the default, which is always
 * @param interactive whether the watch is awake
 */
fun shownInMode(showIn: String?, interactive: Boolean): Boolean = when (showIn) {
    JsonKeyValues.INTERACTIVE.key -> interactive
    JsonKeyValues.AMBIENT.key     -> !interactive
    else                          -> true
}

/**
 * Whether the design uses `showIn` at all, from the values its views declare.
 *
 * A face without it looks the same awake and in always-on, so the render path may keep its cached
 * layers across a mode change. A face with it must redraw them - see `CwfCachePolicy`.
 */
fun dependsOnMode(showInValues: Collection<String?>): Boolean =
    showInValues.any { it == JsonKeyValues.INTERACTIVE.key || it == JsonKeyValues.AMBIENT.key }

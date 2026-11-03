package app.photowiget.widget

import app.photowiget.data.WidgetSpan

// Class names are the launcher's handle on placed widgets and must never change.
class PhotoWidget3x2 : PhotoWidgetProvider()
class PhotoWidget2x3 : PhotoWidgetProvider()
class PhotoWidget4x2 : PhotoWidgetProvider()
class PhotoWidget3x3 : PhotoWidgetProvider()
class PhotoWidget4x3 : PhotoWidgetProvider()

fun WidgetSpan.providerClass(): Class<out PhotoWidgetProvider> = when (this) {
    WidgetSpan.Cells2x2 -> PhotoWidgetProvider::class.java
    WidgetSpan.Cells3x2 -> PhotoWidget3x2::class.java
    WidgetSpan.Cells2x3 -> PhotoWidget2x3::class.java
    WidgetSpan.Cells4x2 -> PhotoWidget4x2::class.java
    WidgetSpan.Cells3x3 -> PhotoWidget3x3::class.java
    WidgetSpan.Cells4x3 -> PhotoWidget4x3::class.java
}

val allProviderClasses: List<Class<out PhotoWidgetProvider>> = WidgetSpan.entries.map { it.providerClass() }

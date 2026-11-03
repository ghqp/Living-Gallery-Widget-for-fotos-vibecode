package app.photowiget.data

/** Home screen cell sizes a widget can be pinned with. */
enum class WidgetSpan(val columns: Int, val rows: Int) {
    Cells2x2(2, 2),
    Cells3x2(3, 2),
    Cells2x3(2, 3),
    Cells3x3(3, 3),
    Cells4x2(4, 2),
    Cells4x3(4, 3),
    ;

    val minWidthDp: Int get() = 70 * columns - 30

    val minHeightDp: Int get() = 70 * rows - 30

    val label: String get() = "$columns×$rows"
}

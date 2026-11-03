package app.photowiget.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WidgetDesign(
    val id: String,
    val name: String,
    val photos: List<PhotoItem> = emptyList(),
    val look: WidgetLook = WidgetLook(),
    val behavior: WidgetBehavior = WidgetBehavior(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    companion object {
        const val MAX_PHOTOS = 24
        const val MAX_NAME_LENGTH = 40
    }
}

@Serializable
data class PhotoItem(
    val file: String,
    val crop: PhotoCrop = PhotoCrop(),
)

/**
 * Visible part of a photo inside the square frame: [zoom] 1 shows the largest centered square,
 * [centerX] and [centerY] are the window center as fractions of the photo size.
 */
@Serializable
data class PhotoCrop(
    val zoom: Float = 1f,
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
)

@Serializable
data class WidgetLook(
    val frame: FrameShape = FrameShape.Square,
    val cornerRadius: Int = DEFAULT_CORNER_RADIUS,
    val borderWidth: Int = 0,
    val borderTint: BorderTint = BorderTint.White,
    val tone: ColorTone = ColorTone.Original,
) {
    companion object {
        const val DEFAULT_CORNER_RADIUS = 12
        const val MAX_CORNER_RADIUS = 48
        const val CORNER_RADIUS_STEP = 4
        const val MAX_BORDER_WIDTH = 10
    }
}

@Serializable
data class WidgetBehavior(
    val interval: RotationInterval = RotationInterval.Off,
    val shuffle: Boolean = false,
    val tapAction: TapAction = TapAction.NextPhoto,
)

@Serializable
enum class FrameShape(val hasCorners: Boolean) {
    @SerialName("circle")
    Circle(false),

    @SerialName("square")
    Square(true),

    @SerialName("rounded")
    Rounded(false),

    @SerialName("arch")
    Arch(true),

    @SerialName("cookie")
    Cookie(false),

    @SerialName("clover")
    Clover(false),
}

@Serializable
enum class ColorTone {
    @SerialName("original")
    Original,

    @SerialName("mono")
    Mono,

    @SerialName("vivid")
    Vivid,

    @SerialName("dramatic")
    Dramatic,

    @SerialName("warm")
    Warm,

    @SerialName("cool")
    Cool,

    @SerialName("sepia")
    Sepia,

    @SerialName("fade")
    Fade,

    @SerialName("noir")
    Noir,
}

@Serializable
enum class BorderTint(val argb: Int?) {
    @SerialName("auto")
    Auto(null),

    @SerialName("white")
    White(0xFFFFFFFF.toInt()),

    @SerialName("black")
    Black(0xFF111318.toInt()),

    @SerialName("sky")
    Sky(0xFF9ECAFF.toInt()),

    @SerialName("rose")
    Rose(0xFFFFB1C8.toInt()),

    @SerialName("sand")
    Sand(0xFFF1D8A8.toInt()),

    @SerialName("mint")
    Mint(0xFFA9E5CB.toInt()),
}

@Serializable
enum class RotationInterval(val minutes: Long) {
    @SerialName("off")
    Off(0),

    @SerialName("15m")
    Minutes15(15),

    @SerialName("30m")
    Minutes30(30),

    @SerialName("1h")
    Hour1(60),

    @SerialName("3h")
    Hours3(180),

    @SerialName("6h")
    Hours6(360),

    @SerialName("12h")
    Hours12(720),

    @SerialName("1d")
    Day1(1440),
}

@Serializable
enum class TapAction {
    @SerialName("next")
    NextPhoto,

    @SerialName("open")
    OpenApp,
}

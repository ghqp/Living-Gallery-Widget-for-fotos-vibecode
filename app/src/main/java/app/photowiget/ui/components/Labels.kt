package app.photowiget.ui.components

import androidx.annotation.StringRes
import app.photowiget.R
import app.photowiget.data.model.BorderTint
import app.photowiget.data.model.ColorTone
import app.photowiget.data.model.FrameShape
import app.photowiget.data.model.RotationInterval

@get:StringRes
val FrameShape.labelRes: Int
    get() = when (this) {
        FrameShape.Circle -> R.string.frame_circle
        FrameShape.Square -> R.string.frame_square
        FrameShape.Rounded -> R.string.frame_rounded
        FrameShape.Arch -> R.string.frame_arch
        FrameShape.Cookie -> R.string.frame_cookie
        FrameShape.Clover -> R.string.frame_clover
    }

@get:StringRes
val ColorTone.labelRes: Int
    get() = when (this) {
        ColorTone.Original -> R.string.tone_original
        ColorTone.Mono -> R.string.tone_mono
        ColorTone.Vivid -> R.string.tone_vivid
        ColorTone.Dramatic -> R.string.tone_dramatic
        ColorTone.Warm -> R.string.tone_warm
        ColorTone.Cool -> R.string.tone_cool
        ColorTone.Sepia -> R.string.tone_sepia
        ColorTone.Fade -> R.string.tone_fade
        ColorTone.Noir -> R.string.tone_noir
    }

@get:StringRes
val BorderTint.labelRes: Int
    get() = when (this) {
        BorderTint.Auto -> R.string.tint_auto
        BorderTint.White -> R.string.tint_white
        BorderTint.Black -> R.string.tint_black
        BorderTint.Sky -> R.string.tint_sky
        BorderTint.Rose -> R.string.tint_rose
        BorderTint.Sand -> R.string.tint_sand
        BorderTint.Mint -> R.string.tint_mint
    }

@get:StringRes
val RotationInterval.labelRes: Int
    get() = when (this) {
        RotationInterval.Off -> R.string.interval_off
        RotationInterval.Minutes15 -> R.string.interval_15m
        RotationInterval.Minutes30 -> R.string.interval_30m
        RotationInterval.Hour1 -> R.string.interval_1h
        RotationInterval.Hours3 -> R.string.interval_3h
        RotationInterval.Hours6 -> R.string.interval_6h
        RotationInterval.Hours12 -> R.string.interval_12h
        RotationInterval.Day1 -> R.string.interval_1d
    }

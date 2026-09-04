package com.zik.music.audio.spatial

interface MotionModel {
    fun positionAt(timeSeconds: Double): Vector3
}

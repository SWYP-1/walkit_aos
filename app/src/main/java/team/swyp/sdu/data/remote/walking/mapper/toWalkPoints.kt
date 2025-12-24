package team.swyp.sdu.data.remote.walking.mapper

import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.data.remote.walking.dto.WalkPoint

fun List<LocationPoint>.toWalkPoints(): List<WalkPoint> =
    map {
        WalkPoint(
            latitude = it.latitude,
            longitude = it.longitude,
            timestampMillis = it.timestamp
        )
    }

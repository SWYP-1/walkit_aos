package swyp.team.walkit.data.remote.walking.mapper

import swyp.team.walkit.data.model.LocationPoint
import swyp.team.walkit.data.remote.walking.dto.WalkPoint

fun List<LocationPoint>.toWalkPoints(): List<WalkPoint> =
    map {
        WalkPoint(
            latitude = it.latitude,
            longitude = it.longitude,
            timestampMillis = it.timestamp
        )
    }

package team.swyp.sdu.data.api

import com.google.gson.annotations.SerializedName

/**
 * Pokemon data model from PokéAPI
 * https://pokeapi.co/api/v2/pokemon/{name}
 */
data class Pokemon(
    val id: Int,
    val name: String,
    val height: Int,
    val weight: Int,
    @SerializedName("base_experience")
    val baseExperience: Int,
    val sprites: Sprites,
    val types: List<Type>,
    val stats: List<Stat>,
) {
    data class Sprites(
        @SerializedName("front_default")
        val frontDefault: String?,
        @SerializedName("back_default")
        val backDefault: String?,
    )

    data class Type(
        val slot: Int,
        val type: TypeDetail,
    ) {
        data class TypeDetail(
            val name: String,
            val url: String,
        )
    }

    data class Stat(
        @SerializedName("base_stat")
        val baseStat: Int,
        val stat: StatDetail,
    ) {
        data class StatDetail(
            val name: String,
            val url: String,
        )
    }
}

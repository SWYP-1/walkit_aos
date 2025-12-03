package team.swyp.sdu.data.api

import retrofit2.http.GET
import retrofit2.http.Path

data class Post(
    val userId: Int,
    val id: Int,
    val title: String,
    val body: String,
)

interface ApiService {
    // JSONPlaceholder 무료 테스트 API 사용
    // 모든 포스트 가져오기
    @GET("posts")
    suspend fun getPosts(): List<Post>

    // 특정 포스트 가져오기
    @GET("posts/{id}")
    suspend fun getPost(
        @Path("id") id: Int,
    ): Post
}

/**
 * PokéAPI Service
 * https://pokeapi.co/api/v2/
 */
interface PokemonApiService {
    // 포켓몬 정보 가져오기 (이름으로)
    @GET("pokemon/{name}")
    suspend fun getPokemon(
        @Path("name") name: String,
    ): Pokemon
}

package team.swyp.sdu.domain.service

/**
 * 이미지 다운로드를 위한 인터페이스
 */
interface ImageDownloader {

    /**
     * 이미지 URL에서 PNG 이미지를 다운로드하여 ByteArray로 반환
     * @param imageUrl 다운로드할 PNG 이미지의 URL
     * @return 이미지의 ByteArray
     * @throws IllegalStateException 다운로드 실패 시
     * @throws IllegalArgumentException 잘못된 응답 시
     */
    suspend fun downloadPngImage(imageUrl: String): ByteArray
}


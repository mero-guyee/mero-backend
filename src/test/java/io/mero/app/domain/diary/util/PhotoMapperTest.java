package io.mero.app.domain.diary.util;

import io.mero.app.domain.diary.entity.Photo;
import io.mero.app.domain.diary.entity.UploadStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PhotoMapperTest {

    @Test
    @DisplayName("URL 리스트를 Photo 엔티티로 변환 - 기본")
    void URL_리스트를_Photo_엔티티로_변환_기본() {
        // given
        List<String> urls = List.of(
                "https://example.com/photo1.jpg",
                "https://example.com/photo2.png"
        );

        // when
        List<Photo> photos = PhotoMapper.fromUrls(urls, null);

        // then
        assertThat(photos).hasSize(2);
        assertThat(photos.get(0).getImageUrl()).isEqualTo("https://example.com/photo1.jpg");
        assertThat(photos.get(0).getFileName()).isEqualTo("photo1.jpg");
        assertThat(photos.get(0).getMimeType()).isEqualTo("image/jpeg");
        assertThat(photos.get(0).getOrderIndex()).isEqualTo(0);
        assertThat(photos.get(0).getUploadStatus()).isEqualTo(UploadStatus.COMPLETED);

        assertThat(photos.get(1).getOrderIndex()).isEqualTo(1);
        assertThat(photos.get(1).getMimeType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("빈 URL 리스트 처리")
    void 빈_URL_리스트_처리() {
        // when
        List<Photo> photos = PhotoMapper.fromUrls(null, null);

        // then
        assertThat(photos).isEmpty();
    }

    @Test
    @DisplayName("파일명 추출 실패 시 null 처리")
    void 파일명_추출_실패_시_null_처리() {
        // given
        List<String> urls = List.of("https://example.com/");

        // when
        List<Photo> photos = PhotoMapper.fromUrls(urls, null);

        // then
        assertThat(photos).hasSize(1);
        assertThat(photos.get(0).getFileName()).isNull();
    }

    @Test
    @DisplayName("Photo 엔티티를 URL 리스트로 변환")
    void Photo_엔티티를_URL_리스트로_변환() {
        // given
        List<Photo> photos = List.of(
                Photo.builder()
                        .imageUrl("url1")
                        .orderIndex(0)
                        .build(),
                Photo.builder()
                        .imageUrl("url2")
                        .orderIndex(1)
                        .build()
        );

        // when
        List<String> urls = PhotoMapper.toUrls(photos);

        // then
        assertThat(urls).containsExactly("url1", "url2");
    }

    @Test
    @DisplayName("다양한 이미지 확장자의 MIME 타입 추출")
    void 다양한_이미지_확장자의_MIME_타입_추출() {
        // given
        List<String> urls = List.of(
                "https://example.com/photo.jpg",
                "https://example.com/photo.jpeg",
                "https://example.com/photo.png",
                "https://example.com/photo.gif",
                "https://example.com/photo.webp"
        );

        // when
        List<Photo> photos = PhotoMapper.fromUrls(urls, null);

        // then
        assertThat(photos.get(0).getMimeType()).isEqualTo("image/jpeg");
        assertThat(photos.get(1).getMimeType()).isEqualTo("image/jpeg");
        assertThat(photos.get(2).getMimeType()).isEqualTo("image/png");
        assertThat(photos.get(3).getMimeType()).isEqualTo("image/gif");
        assertThat(photos.get(4).getMimeType()).isEqualTo("image/webp");
    }

    @Test
    @DisplayName("알 수 없는 확장자의 경우 MIME 타입은 null")
    void 알_수_없는_확장자의_경우_MIME_타입은_null() {
        // given
        List<String> urls = List.of("https://example.com/photo.unknown");

        // when
        List<Photo> photos = PhotoMapper.fromUrls(urls, null);

        // then
        assertThat(photos.get(0).getMimeType()).isNull();
    }

    @Test
    @DisplayName("orderIndex는 리스트 순서대로 부여됨")
    void orderIndex는_리스트_순서대로_부여됨() {
        // given
        List<String> urls = List.of("url1", "url2", "url3", "url4");

        // when
        List<Photo> photos = PhotoMapper.fromUrls(urls, null);

        // then
        assertThat(photos.get(0).getOrderIndex()).isEqualTo(0);
        assertThat(photos.get(1).getOrderIndex()).isEqualTo(1);
        assertThat(photos.get(2).getOrderIndex()).isEqualTo(2);
        assertThat(photos.get(3).getOrderIndex()).isEqualTo(3);
    }
}

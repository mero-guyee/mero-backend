package io.mero.app.domain.footprint.service;

import io.mero.app.domain.expense.entity.Expense;
import io.mero.app.domain.expense.repository.ExpenseRepository;
import io.mero.app.domain.footprint.dto.FootprintCreateRequest;
import io.mero.app.domain.footprint.dto.FootprintDetailResponse;
import io.mero.app.domain.footprint.dto.FootprintResponse;
import io.mero.app.domain.footprint.dto.FootprintUpdateRequest;
import io.mero.app.domain.footprint.dto.PhotoResponse;
import io.mero.app.domain.footprint.entity.Footprint;
import io.mero.app.domain.footprint.entity.Photo;
import io.mero.app.domain.footprint.repository.FootprintRepository;
import io.mero.app.domain.footprint.repository.PhotoRepository;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.trip.repository.TripRepository;
import io.mero.app.domain.user.entity.User;
import io.mero.app.global.dto.StorageUploadResult;
import io.mero.app.global.enums.ImageMimeType;
import io.mero.app.global.exception.ForbiddenException;
import io.mero.app.global.exception.NotFoundException;
import io.mero.app.global.service.StorageService;
import io.mero.app.global.util.MessageUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FootprintServiceTest {

    @Mock
    private FootprintRepository footprintRepository;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private MessageUtil messageUtil;

    @InjectMocks
    private FootprintService footprintService;

    // ===== 발자취 생성 =====

    @Test
    @DisplayName("발자취 생성 성공")
    void 발자취_생성_성공() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user);

        FootprintCreateRequest request = new FootprintCreateRequest(
                "client-id-1",
                "도쿄 첫째날",
                "신주쿠를 걸었다",
                LocalDate.of(2026, 4, 1),
                null,
                Collections.emptyList(),
                Collections.emptyList()
        );

        Footprint footprint = createFootprint(1L, trip, request.getClientId(), request.getContent(), request.getDate());

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(footprintRepository.findByClientIdAndTripId(request.getClientId(), tripId)).willReturn(Optional.empty());
        given(footprintRepository.save(any(Footprint.class))).willReturn(footprint);

        // when
        FootprintResponse response = footprintService.createFootprint(userId, tripId, request);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getClientId()).isEqualTo("client-id-1");
        assertThat(response.getDate()).isEqualTo(LocalDate.of(2026, 4, 1));

        verify(footprintRepository).findByClientIdAndTripId(request.getClientId(), tripId);
        verify(footprintRepository).save(any(Footprint.class));
    }

    @Test
    @DisplayName("발자취 생성 성공 - 중복 clientId (멱등성)")
    void 발자취_생성_성공_중복_clientId() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user);

        FootprintCreateRequest request = new FootprintCreateRequest(
                "client-id-1",
                "도쿄 첫째날",
                "신주쿠를 걸었다",
                LocalDate.of(2026, 4, 1),
                null,
                Collections.emptyList(),
                Collections.emptyList()
        );

        Footprint existing = createFootprint(1L, trip, request.getClientId(), request.getContent(), request.getDate());

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(footprintRepository.findByClientIdAndTripId(request.getClientId(), tripId)).willReturn(Optional.of(existing));

        // when
        FootprintResponse response = footprintService.createFootprint(userId, tripId, request);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        verify(footprintRepository, never()).save(any(Footprint.class));
    }

    @Test
    @DisplayName("발자취 생성 실패 - 권한 없음")
    void 발자취_생성_실패_권한_없음() {
        // given
        Long userId = 1L;
        Long otherUserId = 2L;
        Long tripId = 1L;

        User otherUser = createUser(otherUserId);
        Trip trip = createTrip(tripId, otherUser);

        FootprintCreateRequest request = new FootprintCreateRequest(
                "client-id-1", "제목", "내용", LocalDate.of(2026, 4, 1), null, null, null
        );

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(messageUtil.getMessage("error.forbidden")).willReturn("접근 권한이 없습니다");

        // when & then
        assertThatThrownBy(() -> footprintService.createFootprint(userId, tripId, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("접근 권한이 없습니다");
    }

    @Test
    @DisplayName("발자취 생성 실패 - 여행 없음")
    void 발자취_생성_실패_여행_없음() {
        // given
        Long userId = 1L;
        Long tripId = 999L;

        FootprintCreateRequest request = new FootprintCreateRequest(
                "client-id-1", "제목", "내용", LocalDate.of(2026, 4, 1), null, null, null
        );

        given(tripRepository.findById(tripId)).willReturn(Optional.empty());
        given(messageUtil.getMessage("error.trip.notFound")).willReturn("여행을 찾을 수 없습니다");

        // when & then
        assertThatThrownBy(() -> footprintService.createFootprint(userId, tripId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("여행을 찾을 수 없습니다");
    }

    // ===== 발자취 목록 조회 =====

    @Test
    @DisplayName("발자취 목록 조회 성공")
    void 발자취_목록_조회_성공() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user);

        List<Footprint> footprints = List.of(
                createFootprint(1L, trip, "client-id-1", "첫째날", LocalDate.of(2026, 4, 2)),
                createFootprint(2L, trip, "client-id-2", "둘째날", LocalDate.of(2026, 4, 1))
        );

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(footprintRepository.findByTripIdOrderByDateDesc(tripId)).willReturn(footprints);

        // when
        List<FootprintResponse> responses = footprintService.getFootprints(userId, tripId);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo(1L);
        assertThat(responses.get(1).getId()).isEqualTo(2L);

        verify(footprintRepository).findByTripIdOrderByDateDesc(tripId);
    }

    // ===== 발자취 상세 조회 =====

    @Test
    @DisplayName("발자취 상세 조회 성공")
    void 발자취_상세_조회_성공() {
        // given
        Long userId = 1L;
        Long tripId = 1L;
        Long footprintId = 1L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user);
        Footprint footprint = createFootprint(footprintId, trip, "client-id-1", "신주쿠를 걸었다", LocalDate.of(2026, 4, 1));

        given(footprintRepository.findById(footprintId)).willReturn(Optional.of(footprint));
        given(expenseRepository.findByFootprint(footprint)).willReturn(Collections.emptyList());

        // when
        FootprintDetailResponse response = footprintService.getFootprint(userId, tripId, footprintId);

        // then
        assertThat(response.getId()).isEqualTo(footprintId);
        assertThat(response.getTripId()).isEqualTo(tripId);
        assertThat(response.getExpenses()).isEmpty();

        verify(expenseRepository).findByFootprint(footprint);
    }

    @Test
    @DisplayName("발자취 상세 조회 실패 - 권한 없음")
    void 발자취_상세_조회_실패_권한_없음() {
        // given
        Long userId = 1L;
        Long otherUserId = 2L;
        Long tripId = 1L;
        Long footprintId = 1L;

        User otherUser = createUser(otherUserId);
        Trip trip = createTrip(tripId, otherUser);
        Footprint footprint = createFootprint(footprintId, trip, "client-id-1", "내용", LocalDate.of(2026, 4, 1));

        given(footprintRepository.findById(footprintId)).willReturn(Optional.of(footprint));
        given(messageUtil.getMessage("error.forbidden")).willReturn("접근 권한이 없습니다");

        // when & then
        assertThatThrownBy(() -> footprintService.getFootprint(userId, tripId, footprintId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("접근 권한이 없습니다");
    }

    @Test
    @DisplayName("발자취 상세 조회 실패 - tripId 불일치")
    void 발자취_상세_조회_실패_tripId_불일치() {
        // given
        Long userId = 1L;
        Long tripId = 1L;
        Long otherTripId = 2L;
        Long footprintId = 1L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user);
        Footprint footprint = createFootprint(footprintId, trip, "client-id-1", "내용", LocalDate.of(2026, 4, 1));

        given(footprintRepository.findById(footprintId)).willReturn(Optional.of(footprint));
        given(messageUtil.getMessage("error.forbidden")).willReturn("접근 권한이 없습니다");

        // when & then
        assertThatThrownBy(() -> footprintService.getFootprint(userId, otherTripId, footprintId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("접근 권한이 없습니다");
    }

    // ===== 발자취 수정 =====

    @Test
    @DisplayName("발자취 수정 성공")
    void 발자취_수정_성공() {
        // given
        Long userId = 1L;
        Long tripId = 1L;
        Long footprintId = 1L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user);
        Footprint footprint = createFootprint(footprintId, trip, "client-id-1", "원본 내용", LocalDate.of(2026, 4, 1));

        FootprintUpdateRequest request = new FootprintUpdateRequest(
                "수정된 제목",
                "수정된 내용",
                LocalDate.of(2026, 4, 2),
                null,
                Collections.emptyList(),
                Collections.emptyList()
        );

        given(footprintRepository.findById(footprintId)).willReturn(Optional.of(footprint));

        // when
        FootprintResponse response = footprintService.updateFootprint(userId, tripId, footprintId, request);

        // then
        assertThat(response.getContent()).isEqualTo("수정된 내용");
        assertThat(response.getDate()).isEqualTo(LocalDate.of(2026, 4, 2));
    }

    @Test
    @DisplayName("발자취 수정 실패 - 권한 없음")
    void 발자취_수정_실패_권한_없음() {
        // given
        Long userId = 1L;
        Long otherUserId = 2L;
        Long tripId = 1L;
        Long footprintId = 1L;

        User otherUser = createUser(otherUserId);
        Trip trip = createTrip(tripId, otherUser);
        Footprint footprint = createFootprint(footprintId, trip, "client-id-1", "내용", LocalDate.of(2026, 4, 1));

        FootprintUpdateRequest request = new FootprintUpdateRequest(
                "제목", "내용", LocalDate.of(2026, 4, 1), null, null, null
        );

        given(footprintRepository.findById(footprintId)).willReturn(Optional.of(footprint));
        given(messageUtil.getMessage("error.forbidden")).willReturn("접근 권한이 없습니다");

        // when & then
        assertThatThrownBy(() -> footprintService.updateFootprint(userId, tripId, footprintId, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("접근 권한이 없습니다");
    }

    // ===== 발자취 삭제 =====

    @Test
    @DisplayName("발자취 삭제 성공")
    void 발자취_삭제_성공() {
        // given
        Long userId = 1L;
        Long tripId = 1L;
        Long footprintId = 1L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user);
        Footprint footprint = createFootprint(footprintId, trip, "client-id-1", "내용", LocalDate.of(2026, 4, 1));

        given(footprintRepository.findById(footprintId)).willReturn(Optional.of(footprint));

        // when
        footprintService.deleteFootprint(userId, tripId, footprintId);

        // then
        assertThat(footprint.isDeleted()).isTrue();
        verify(footprintRepository, never()).delete(any());
    }

    @Test
    @DisplayName("발자취 삭제 시 연결된 Expense의 footprint 참조가 해제된다")
    void 발자취_삭제_시_연결된_Expense_unlink() {
        // given
        Long userId = 1L;
        Long tripId = 1L;
        Long footprintId = 1L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user);
        Footprint footprint = createFootprint(footprintId, trip, "client-id-1", "내용", LocalDate.of(2026, 4, 1));

        Expense expense1 = Expense.builder().trip(trip).footprint(footprint).build();
        Expense expense2 = Expense.builder().trip(trip).footprint(footprint).build();

        given(footprintRepository.findById(footprintId)).willReturn(Optional.of(footprint));
        given(expenseRepository.findByFootprint(footprint)).willReturn(List.of(expense1, expense2));

        // when
        footprintService.deleteFootprint(userId, tripId, footprintId);

        // then
        assertThat(expense1.getFootprint()).isNull();
        assertThat(expense2.getFootprint()).isNull();
        assertThat(footprint.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("발자취 삭제 실패 - 권한 없음")
    void 발자취_삭제_실패_권한_없음() {
        // given
        Long userId = 1L;
        Long otherUserId = 2L;
        Long tripId = 1L;
        Long footprintId = 1L;

        User otherUser = createUser(otherUserId);
        Trip trip = createTrip(tripId, otherUser);
        Footprint footprint = createFootprint(footprintId, trip, "client-id-1", "내용", LocalDate.of(2026, 4, 1));

        given(footprintRepository.findById(footprintId)).willReturn(Optional.of(footprint));
        given(messageUtil.getMessage("error.forbidden")).willReturn("접근 권한이 없습니다");

        // when & then
        assertThatThrownBy(() -> footprintService.deleteFootprint(userId, tripId, footprintId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("접근 권한이 없습니다");

        verify(footprintRepository, never()).delete(any());
    }

    // ===== 사진 업로드 =====

    @Test
    @DisplayName("사진 업로드 성공")
    void 사진_업로드_성공() {
        // given
        Long userId = 1L;
        Long tripId = 1L;
        Long footprintId = 1L;
        String clientId = "client-photo-1";

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user);
        Footprint footprint = createFootprint(footprintId, trip, "client-id-1", "내용", LocalDate.of(2026, 4, 1));

        MockMultipartFile photo = new MockMultipartFile(
                "photo", "photo1.jpg", "image/jpeg", "photo content".getBytes()
        );

        List<StorageUploadResult> uploadResults = List.of(
                new StorageUploadResult(
                        "users/1/trips/1/footprints/1/photo1.jpg",
                        "https://example.com/photo1.jpg",
                        "photo1.jpg",
                        13L,
                        "image/jpeg"
                )
        );

        Photo savedPhoto = Photo.builder()
                .footprint(footprint)
                .clientId(clientId)
                .s3Key("users/1/trips/1/footprints/1/photo1.jpg")
                .s3Url("https://example.com/photo1.jpg")
                .originalFilename("photo1.jpg")
                .fileSize(13L)
                .mimeType(ImageMimeType.JPEG)
                .orderIndex(0)
                .build();

        given(footprintRepository.findById(footprintId)).willReturn(Optional.of(footprint));
        given(photoRepository.findByClientId(clientId)).willReturn(Optional.empty());
        given(storageService.uploadFootprintPhotos(eq(userId), eq(tripId), eq(footprintId), anyList()))
                .willReturn(uploadResults);
        given(photoRepository.save(any(Photo.class))).willReturn(savedPhoto);

        // when
        PhotoResponse response = footprintService.uploadPhoto(userId, tripId, footprintId, clientId, photo);

        // then
        assertThat(response.getS3Url()).isEqualTo("https://example.com/photo1.jpg");

        verify(storageService).uploadFootprintPhotos(eq(userId), eq(tripId), eq(footprintId), anyList());
        verify(photoRepository).save(any(Photo.class));
    }

    @Test
    @DisplayName("사진 업로드 멱등성 - 동일 clientId 재요청 시 storage 업로드 스킵")
    void 사진_업로드_멱등성() {
        // given
        Long userId = 1L;
        Long tripId = 1L;
        Long footprintId = 1L;
        String clientId = "client-photo-1";

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user);
        Footprint footprint = createFootprint(footprintId, trip, "client-id-1", "내용", LocalDate.of(2026, 4, 1));

        MockMultipartFile photo = new MockMultipartFile(
                "photo", "photo1.jpg", "image/jpeg", "photo content".getBytes()
        );

        Photo existing = Photo.builder()
                .footprint(footprint)
                .clientId(clientId)
                .s3Key("existing-key")
                .s3Url("https://example.com/existing.jpg")
                .originalFilename("photo1.jpg")
                .fileSize(13L)
                .mimeType(ImageMimeType.JPEG)
                .orderIndex(0)
                .build();

        given(footprintRepository.findById(footprintId)).willReturn(Optional.of(footprint));
        given(photoRepository.findByClientId(clientId)).willReturn(Optional.of(existing));

        // when
        PhotoResponse response = footprintService.uploadPhoto(userId, tripId, footprintId, clientId, photo);

        // then
        assertThat(response.getS3Url()).isEqualTo("https://example.com/existing.jpg");
        verify(storageService, never()).uploadFootprintPhotos(any(), any(), any(), anyList());
        verify(photoRepository, never()).save(any(Photo.class));
    }

    @Test
    @DisplayName("사진 업로드 실패 - 권한 없음")
    void 사진_업로드_실패_권한_없음() {
        // given
        Long userId = 1L;
        Long otherUserId = 2L;
        Long tripId = 1L;
        Long footprintId = 1L;
        String clientId = "client-photo-1";

        User otherUser = createUser(otherUserId);
        Trip trip = createTrip(tripId, otherUser);
        Footprint footprint = createFootprint(footprintId, trip, "client-id-1", "내용", LocalDate.of(2026, 4, 1));

        MockMultipartFile photo = new MockMultipartFile(
                "photo", "photo1.jpg", "image/jpeg", "photo content".getBytes()
        );

        given(footprintRepository.findById(footprintId)).willReturn(Optional.of(footprint));
        given(messageUtil.getMessage("error.forbidden")).willReturn("접근 권한이 없습니다");

        // when & then
        assertThatThrownBy(() -> footprintService.uploadPhoto(userId, tripId, footprintId, clientId, photo))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("접근 권한이 없습니다");

        verify(storageService, never()).uploadFootprintPhotos(any(), any(), any(), anyList());
    }

    // ===== 사진 삭제 =====

    @Test
    @DisplayName("사진 삭제 성공")
    void 사진_삭제_성공() {
        // given
        Long userId = 1L;
        Long tripId = 1L;
        Long footprintId = 1L;
        Long photoId = 1L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user);
        Footprint footprint = createFootprint(footprintId, trip, "client-id-1", "내용", LocalDate.of(2026, 4, 1));

        Photo photo = Photo.builder()
                .footprint(footprint)
                .s3Key("users/1/trips/1/footprints/1/photo1.jpg")
                .s3Url("https://example.com/photo1.jpg")
                .originalFilename("photo1.jpg")
                .fileSize(13L)
                .mimeType(ImageMimeType.JPEG)
                .orderIndex(0)
                .build();

        footprint.getPhotos().add(photo);

        given(footprintRepository.findById(footprintId)).willReturn(Optional.of(footprint));
        given(photoRepository.findById(photoId)).willReturn(Optional.of(photo));

        // when
        footprintService.deletePhoto(userId, tripId, footprintId, photoId);

        // then
        verify(photoRepository).delete(photo);
    }

    @Test
    @DisplayName("사진 삭제 실패 - 사진 없음")
    void 사진_삭제_실패_사진_없음() {
        // given
        Long userId = 1L;
        Long tripId = 1L;
        Long footprintId = 1L;
        Long photoId = 999L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user);
        Footprint footprint = createFootprint(footprintId, trip, "client-id-1", "내용", LocalDate.of(2026, 4, 1));

        given(footprintRepository.findById(footprintId)).willReturn(Optional.of(footprint));
        given(photoRepository.findById(photoId)).willReturn(Optional.empty());
        given(messageUtil.getMessage("error.photo.notFound")).willReturn("사진을 찾을 수 없습니다");

        // when & then
        assertThatThrownBy(() -> footprintService.deletePhoto(userId, tripId, footprintId, photoId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("사진을 찾을 수 없습니다");
    }

    // ===== Helper methods =====

    private User createUser(Long userId) {
        return User.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("password")
                .nickname("테스트유저")
                .build();
    }

    private Trip createTrip(Long tripId, User user) {
        return Trip.builder()
                .id(tripId)
                .user(user)
                .title("도쿄 여행")
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2026, 4, 7))
                .build();
    }

    private Footprint createFootprint(Long footprintId, Trip trip, String clientId, String content, LocalDate date) {
        return Footprint.builder()
                .id(footprintId)
                .trip(trip)
                .clientId(clientId)
                .content(content)
                .date(date)
                .photoUrls(new ArrayList<>())
                .build();
    }
}

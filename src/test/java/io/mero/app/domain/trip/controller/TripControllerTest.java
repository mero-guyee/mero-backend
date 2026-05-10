package io.mero.app.domain.trip.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mero.app.domain.trip.dto.TripCreateRequest;
import io.mero.app.domain.trip.dto.TripDetailResponse;
import io.mero.app.domain.trip.dto.TripResponse;
import io.mero.app.domain.trip.dto.TripUpdateRequest;
import io.mero.app.domain.trip.service.TripService;
import io.mero.app.global.jwt.JwtAuthenticationFilter;
import io.mero.app.global.util.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(
        controllers = TripController.class,
        excludeFilters = {@ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TripService tripService;

    private MockedStatic<SecurityUtil> securityUtil;

    @BeforeEach
    void setUp() {
        securityUtil = mockStatic(SecurityUtil.class);
        securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        securityUtil.close();
    }

    @Test
    @DisplayName("여행 생성 성공 - 이미지 없음")
    void 여행_생성_성공_이미지_없음() throws Exception {
        // given
        String clientId = "test-client-id-1";
        TripCreateRequest request = new TripCreateRequest(
                clientId,
                "남미 여행",
                LocalDate.of(2026, 3, 11),
                LocalDate.of(2026, 5, 15),
                List.of("브라질", "아르헨티나", "페루")
        );

        TripResponse response = new TripResponse(
                1L,
                clientId,
                "남미 여행",
                LocalDate.of(2026, 3, 11),
                LocalDate.of(2026, 5, 15),
                List.of("브라질", "아르헨티나", "페루"),
                null,
                LocalDateTime.now()
        );

        given(tripService.createTrip(anyLong(), any(TripCreateRequest.class), any())).willReturn(response);

        MockMultipartFile dataPart = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );

        // when & then
        mockMvc.perform(multipart("/api/trips")
                        .file(dataPart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.getId()))
                .andExpect(jsonPath("$.clientId").value(clientId))
                .andExpect(jsonPath("$.title").value(response.getTitle()))
                .andExpect(jsonPath("$.startDate").value(response.getStartDate().toString()))
                .andExpect(jsonPath("$.endDate").value(response.getEndDate().toString()))
                .andExpect(jsonPath("$.countries").isArray())
                .andExpect(jsonPath("$.countries[0]").value("브라질"))
                .andExpect(jsonPath("$.imageUrl").doesNotExist());
    }

    @Test
    @DisplayName("여행 생성 성공 - 이미지 포함")
    void 여행_생성_성공_이미지_포함() throws Exception {
        // given
        String clientId = "test-client-id-2";
        TripCreateRequest request = new TripCreateRequest(
                clientId,
                "남미 여행",
                LocalDate.of(2026, 3, 11),
                LocalDate.of(2026, 5, 15),
                List.of("브라질", "아르헨티나", "페루")
        );

        String imageUrl = "https://test.supabase.co/storage/v1/object/public/test-images/users/1/trips/images/uuid_test.jpg";

        TripResponse response = new TripResponse(
                1L,
                clientId,
                "남미 여행",
                LocalDate.of(2026, 3, 11),
                LocalDate.of(2026, 5, 15),
                List.of("브라질", "아르헨티나", "페루"),
                imageUrl,
                LocalDateTime.now()
        );

        given(tripService.createTrip(anyLong(), any(TripCreateRequest.class), any(MultipartFile.class))).willReturn(response);

        MockMultipartFile dataPart = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );

        MockMultipartFile imagePart = new MockMultipartFile(
                "image",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        // when & then
        mockMvc.perform(multipart("/api/trips")
                        .file(dataPart)
                        .file(imagePart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.getId()))
                .andExpect(jsonPath("$.title").value(response.getTitle()))
                .andExpect(jsonPath("$.imageUrl").value(imageUrl));
    }

    @Test
    @DisplayName("여행 목록 조회 성공")
    void 여행_목록_조회_성공() throws Exception {
        // given
        List<TripResponse> tripResponses = List.of(
                new TripResponse(
                        1L, "client-id-1", "남미 여행",
                        LocalDate.of(2026, 3, 11),
                        LocalDate.of(2026, 5, 15),
                        List.of("브라질", "아르헨티나", "페루"),
                        null,
                        LocalDateTime.now()
                ),
                new TripResponse(
                        2L, "client-id-2", "일본 여행",
                        LocalDate.of(2026, 8, 10),
                        LocalDate.of(2026, 10, 16),
                        List.of("도쿄", "오사카", "교토"),
                        null,
                        LocalDateTime.now()
                )
        );

        given(tripService.getTrips(anyLong())).willReturn(tripResponses);

        // when & then
        mockMvc.perform(get("/api/trips"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(tripResponses.size()))
                .andExpect(jsonPath("$[0].title").value(tripResponses.get(0).getTitle()))
                .andExpect(jsonPath("$[1].title").value(tripResponses.get(1).getTitle()));
    }

    @Test
    @DisplayName("여행 상세 조회 성공")
    void 여행_상세_조회_성공() throws Exception {
        // given
        TripDetailResponse response = new TripDetailResponse(
                1L,
                "남미 여행",
                LocalDate.of(2026, 3, 11),
                LocalDate.of(2026, 5, 15),
                List.of("브라질", "아르헨티나", "페루"),
                null,
                LocalDateTime.now(),
                Collections.emptyList()
        );

        given(tripService.getTrip(anyLong(), eq(1L))).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/trips/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("남미 여행"));
    }

    @Test
    @DisplayName("여행 수정 성공")
    void 여행_수정_성공() throws Exception {
        // given
        TripUpdateRequest request = new TripUpdateRequest(
                "남미 여행 수정",
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 5, 16),
                List.of("아르헨티나", "페루", "볼리비아")
        );

        TripResponse response = new TripResponse(
                1L,
                "client-id-1",
                "남미 여행 수정",
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 5, 16),
                List.of("아르헨티나", "페루", "볼리비아"),
                null,
                LocalDateTime.now()
        );

        given(tripService.updateTrip(anyLong(), eq(1L), any(TripUpdateRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(put("/api/trips/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("남미 여행 수정"))
                .andExpect(jsonPath("$.startDate").value("2026-03-10"))
                .andExpect(jsonPath("$.endDate").value("2026-05-16"));
    }

    @Test
    @DisplayName("여행 삭제 성공")
    void 여행_삭제_성공() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/trips/1"))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("여행 이미지 업로드 성공")
    void 여행_이미지_업로드_성공() throws Exception {
        // given
        String newImageUrl = "https://test.supabase.co/storage/v1/object/public/test-images/users/1/trips/images/uuid_new.jpg";

        TripResponse response = new TripResponse(
                1L,
                "client-id-1",
                "남미 여행",
                LocalDate.of(2026, 3, 11),
                LocalDate.of(2026, 5, 15),
                List.of("브라질", "아르헨티나", "페루"),
                newImageUrl,
                LocalDateTime.now()
        );

        given(tripService.updateTripImage(anyLong(), eq(1L), any(MultipartFile.class))).willReturn(response);

        MockMultipartFile imagePart = new MockMultipartFile(
                "image",
                "new_image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "new test image content".getBytes()
        );

        // when & then
        mockMvc.perform(multipart("/api/trips/1/image")
                        .file(imagePart))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value(newImageUrl));

        verify(tripService).updateTripImage(eq(1L), eq(1L), any(MultipartFile.class));
    }

    @Test
    @DisplayName("여행 이미지 삭제 성공")
    void 여행_이미지_삭제_성공() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/trips/1/image"))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(tripService).deleteTripImage(1L, 1L);
    }
}

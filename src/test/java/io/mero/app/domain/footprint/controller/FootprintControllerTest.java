package io.mero.app.domain.footprint.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mero.app.domain.footprint.dto.FootprintCreateRequest;
import io.mero.app.domain.footprint.dto.FootprintDetailResponse;
import io.mero.app.domain.footprint.dto.FootprintResponse;
import io.mero.app.domain.footprint.dto.FootprintUpdateRequest;
import io.mero.app.domain.footprint.dto.LocationResponse;
import io.mero.app.domain.footprint.dto.PhotoResponse;
import io.mero.app.domain.footprint.service.FootprintService;
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

import java.time.LocalDate;
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
        controllers = FootprintController.class,
        excludeFilters = {@ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class FootprintControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FootprintService footprintService;

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
    @DisplayName("발자취 생성 성공")
    void 발자취_생성_성공() throws Exception {
        // given
        FootprintCreateRequest request = new FootprintCreateRequest(
                "client-id-1",
                "도쿄 첫째날",
                "신주쿠를 걸었다",
                LocalDate.of(2026, 4, 1),
                null,
                Collections.emptyList(),
                Collections.emptyList()
        );

        FootprintResponse response = new FootprintResponse(
                1L,
                "client-id-1",
                1L,
                "신주쿠를 걸었다",
                LocalDate.of(2026, 4, 1),
                null,
                Collections.emptyList(),
                null
        );

        given(footprintService.createFootprint(anyLong(), eq(1L), any(FootprintCreateRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/trips/1/footprints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.clientId").value("client-id-1"))
                .andExpect(jsonPath("$.tripId").value(1L))
                .andExpect(jsonPath("$.date").value("2026-04-01"));
    }

    @Test
    @DisplayName("발자취 목록 조회 성공")
    void 발자취_목록_조회_성공() throws Exception {
        // given
        List<FootprintResponse> responses = List.of(
                new FootprintResponse(1L, "client-id-1", 1L, "첫째날", LocalDate.of(2026, 4, 1), null, Collections.emptyList(), null),
                new FootprintResponse(2L, "client-id-2", 1L, "둘째날", LocalDate.of(2026, 4, 2), null, Collections.emptyList(), null)
        );

        given(footprintService.getFootprints(anyLong(), eq(1L))).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/trips/1/footprints"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));
    }

    @Test
    @DisplayName("발자취 상세 조회 성공")
    void 발자취_상세_조회_성공() throws Exception {
        // given
        FootprintDetailResponse response = new FootprintDetailResponse(
                1L,
                1L,
                "신주쿠를 걸었다",
                LocalDate.of(2026, 4, 1),
                Collections.emptyList(),
                null,
                List.of(PhotoResponse.builder()
                        .id(1L)
                        .clientId("client-photo-1")
                        .s3Url("https://example.com/photo1.jpg")
                        .build()),
                Collections.emptyList()
        );

        given(footprintService.getFootprint(anyLong(), eq(1L), eq(1L))).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/trips/1/footprints/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.tripId").value(1L))
                .andExpect(jsonPath("$.photos").isArray())
                .andExpect(jsonPath("$.photos[0].id").value(1L))
                .andExpect(jsonPath("$.photos[0].clientId").value("client-photo-1"));
    }

    @Test
    @DisplayName("발자취 수정 성공")
    void 발자취_수정_성공() throws Exception {
        // given
        FootprintUpdateRequest request = new FootprintUpdateRequest(
                "수정된 제목",
                "수정된 내용",
                LocalDate.of(2026, 4, 2),
                null,
                Collections.emptyList(),
                Collections.emptyList()
        );

        FootprintResponse response = new FootprintResponse(
                1L,
                "client-id-1",
                1L,
                "수정된 내용",
                LocalDate.of(2026, 4, 2),
                null,
                Collections.emptyList(),
                null
        );

        given(footprintService.updateFootprint(anyLong(), eq(1L), eq(1L), any(FootprintUpdateRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(put("/api/trips/1/footprints/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("수정된 내용"))
                .andExpect(jsonPath("$.date").value("2026-04-02"));
    }

    @Test
    @DisplayName("발자취 삭제 성공")
    void 발자취_삭제_성공() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/trips/1/footprints/1"))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(footprintService).deleteFootprint(1L, 1L, 1L);
    }

    @Test
    @DisplayName("발자취 사진 업로드 성공")
    void 발자취_사진_업로드_성공() throws Exception {
        // given
        PhotoResponse response = PhotoResponse.builder()
                .id(1L)
                .s3Url("https://example.com/photo1.jpg")
                .originalFilename("photo1.jpg")
                .orderIndex(0)
                .build();

        given(footprintService.uploadPhoto(anyLong(), eq(1L), eq(1L), eq("client-photo-1"), any()))
                .willReturn(response);

        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "photo1.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "photo content".getBytes()
        );

        // when & then
        mockMvc.perform(multipart("/api/trips/1/footprints/1/photos/client-photo-1")
                        .file(photo))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.s3Url").value("https://example.com/photo1.jpg"));
    }

    @Test
    @DisplayName("발자취 사진 삭제 성공")
    void 발자취_사진_삭제_성공() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/trips/1/footprints/1/photos/1"))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(footprintService).deletePhoto(1L, 1L, 1L, 1L);
    }
}

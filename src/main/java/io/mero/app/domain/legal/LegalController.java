package io.mero.app.domain.legal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Tag(name = "Legal", description = "법적 문서 (이용약관, 개인정보처리방침, 위치정보 이용약관)")
@Controller
public class LegalController {

    @Operation(summary = "이용약관", description = "서비스 이용약관 페이지를 반환합니다.",
            responses = @ApiResponse(responseCode = "200", description = "HTML 페이지 반환"))
    @GetMapping("/terms")
    public String terms() {
        return "forward:/terms.html";
    }

    @Operation(summary = "개인정보처리방침", description = "개인정보처리방침 페이지를 반환합니다.",
            responses = @ApiResponse(responseCode = "200", description = "HTML 페이지 반환"))
    @GetMapping("/privacy")
    public String privacy() {
        return "forward:/privacy.html";
    }

    @Operation(summary = "위치정보 이용약관", description = "위치정보 이용약관 페이지를 반환합니다.",
            responses = @ApiResponse(responseCode = "200", description = "HTML 페이지 반환"))
    @GetMapping("/location")
    public String location() {
        return "forward:/location.html";
    }
}

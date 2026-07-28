package io.mero.app.domain.user.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginResponseSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("첫 가입 여부는 isNewUser 키 하나로만 직렬화된다")
    void 첫_가입_여부는_키_하나로만_나간다() throws Exception {
        // given
        LoginResponse response = new LoginResponse(
                1L, "a@example.com", null, null, "access", "refresh", true);

        // when
        String json = objectMapper.writeValueAsString(response);

        // then - Lombok이 만든 isNewUser() getter 때문에 newUser 키가 함께 나가면 안 된다
        assertThat(json).contains("\"isNewUser\":true");
        assertThat(json).doesNotContain("newUser\":");
        assertThat(objectMapper.readTree(json).has("newUser")).isFalse();
    }
}

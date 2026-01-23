package io.mero.app.domain.trip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TripMemoUpdateRequest {

    @NotBlank(message = "{tripMemo.title.notBlank}")
    @Size(max = 200, message = "{tripMemo.title.size}")
    private String title;

    @NotBlank(message = "{tripMemo.content.notBlank}")
    @Size(max = 5000, message = "{tripMemo.content.size}")
    private String content;
}

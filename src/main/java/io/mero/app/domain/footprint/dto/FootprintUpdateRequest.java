package io.mero.app.domain.footprint.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FootprintUpdateRequest {

    @Size(max = 200, message = "{footprint.title.size}")
    private String title;

    @Size(max = 5000, message = "{footprint.content.size}")
    private String content;

    @NotNull(message = "{footprint.date.notNull}")
    private LocalDate date;

    @Valid
    private List<LocationRequest> locations;

    private List<String> photoUrls;

}

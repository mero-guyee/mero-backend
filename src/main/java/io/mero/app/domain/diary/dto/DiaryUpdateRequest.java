package io.mero.app.domain.diary.dto;

import io.mero.app.global.embedded.Location;
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
public class DiaryUpdateRequest {

    @Size(max = 200, message = "{diary.title.size}")
    private String title;

    @Size(max = 5000, message = "{diary.content.size}")
    private String content;

    @NotNull(message = "{diary.date.notNull}")
    private LocalDate date;

    private Location location;
    private List<String> photoUrls;

}

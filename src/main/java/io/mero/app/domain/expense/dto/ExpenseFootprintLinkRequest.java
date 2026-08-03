package io.mero.app.domain.expense.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseFootprintLinkRequest {

    @NotNull(message = "{expense.footprintId.notNull}")
    private Long footprintId;
}

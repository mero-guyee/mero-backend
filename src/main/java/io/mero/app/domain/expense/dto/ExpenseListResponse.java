package io.mero.app.domain.expense.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ExpenseListResponse {
    private List<ExpenseResponse> expenses;
    private List<CurrencyUsageDto> currencyUsages;
}

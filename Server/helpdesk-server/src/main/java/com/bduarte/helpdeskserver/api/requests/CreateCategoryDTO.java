package com.bduarte.helpdeskserver.api.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCategoryDTO {
    @NotBlank(message = "Precisa informar uma categoria")
    private String categoryName;

    @Positive(message = "O tempo da resposta em horas precisa ser maior que zero")
    private Integer responseTimeInHoursDefault;
}

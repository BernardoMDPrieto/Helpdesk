package com.bduarte.helpdeskserver.api.requests;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCategoryDTO {
    private String categoryName;

    @Positive(message = "O tempo da resposta em horas precisa ser maior que zero")
    private Integer responseTimeInHoursDefault;

}

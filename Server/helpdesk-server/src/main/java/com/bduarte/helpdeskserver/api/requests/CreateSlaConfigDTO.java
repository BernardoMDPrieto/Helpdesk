package com.bduarte.helpdeskserver.api.requests;

import com.bduarte.helpdeskserver.enums.Priority;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateSlaConfigDTO {
    @NotNull(message = "Precisa informar uma categoria")
    private Long categoryId;

    @NotNull(message = "Precisa informar uma prioridade")
    private Priority priority;

    @NotNull(message = "Precisa informar o tempo de resposta em horas")
    @Positive(message = "O tempo de resposta em horas precisa ser maior que zero")
    private Integer responseTimeInHours;

    @NotNull(message = "Precisa informar o tempo de resolução em horas")
    @Positive(message = "O tempo de resolução em horas precisa ser maior que zero")
    private Integer resolutionTimeInHours;
}

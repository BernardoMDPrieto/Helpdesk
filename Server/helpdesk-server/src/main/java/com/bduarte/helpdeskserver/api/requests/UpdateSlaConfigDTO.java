package com.bduarte.helpdeskserver.api.requests;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateSlaConfigDTO {
    @Positive(message = "O tempo de resposta em horas precisa ser maior que zero")
    private Integer responseTimeInHours;

    @Positive(message = "O tempo de resolução em horas precisa ser maior que zero")
    private Integer resolutionTimeInHours;
}

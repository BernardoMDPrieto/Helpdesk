package com.bduarte.helpdeskserver.api.requests;

import com.bduarte.helpdeskserver.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateTicketDTO {
    @NotBlank(message = "Precisa informar um título")
    private String title;

    @NotBlank(message = "Precisa informar uma descrição")
    private String description;

    @NotNull(message = "Precisa informar uma categoria")
    private Long categoryId;

    @NotNull(message = "Precisa informar uma prioridade")
    private Priority priority;
}

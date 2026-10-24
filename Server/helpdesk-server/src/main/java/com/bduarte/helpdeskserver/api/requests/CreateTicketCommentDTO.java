package com.bduarte.helpdeskserver.api.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateTicketCommentDTO {
    @NotBlank(message = "Precisa informar o conteúdo do comentário")
    private String content;

    // Only admins and agents may send true; omitted means a public comment
    private Boolean internal;
}

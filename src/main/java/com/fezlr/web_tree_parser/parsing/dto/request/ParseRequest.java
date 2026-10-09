package com.fezlr.web_tree_parser.parsing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ParseRequest {
    @NotBlank(message = "Please enter a set")
    @Size(max = 100000, message = "Input must be 100000 as maximum")
    private String input;
}

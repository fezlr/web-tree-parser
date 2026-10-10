package com.fezlr.web_tree_parser.parsing.dto.request;

import com.fezlr.web_tree_parser.parsing.constant.ParsingLimits;
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
    @Size(max = ParsingLimits.MAX_INPUT_LENGTH, message = "Input must be " + ParsingLimits.MAX_INPUT_LENGTH + " characters as maximum")
    private String input;
}

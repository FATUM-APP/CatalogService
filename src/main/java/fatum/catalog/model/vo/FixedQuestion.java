package fatum.catalog.model.vo;


import fatum.catalog.exception.CatalogErrorCode;
import fatum.catalog.exception.FatumServiceException;
import fatum.catalog.model.constant.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Locale;
import java.util.Set;

public record FixedQuestion(
        @NotBlank(message = "A Question is required")
        @Size(max = 50, message = "A question cannot be longer than 50 characters")
        String question,
        @NotNull
        QuestionType type,
        Set<String> options
) {

    public FixedQuestion {
        String normalized = question.trim().toLowerCase(Locale.ROOT);
        question = normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
        if (options == null) options = Set.of();
        if ((type == QuestionType.SINGLE_CHOICE || type == QuestionType.MULTIPLE_CHOICE)
                && options.size() < 2) {
            throw new FatumServiceException(CatalogErrorCode.QUESTION_WITHOUT_OPTIONS);
        }
    }

}

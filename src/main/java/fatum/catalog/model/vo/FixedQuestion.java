package fatum.catalog.model.vo;

import com.fasterxml.jackson.annotation.JsonCreator;
import fatum.catalog.exception.FatumServiceException;
import fatum.catalog.model.constant.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class FixedQuestion {

    @NotBlank(message = "A Question is required")
    @Size(max = 50, message = "A question cannot be longer than 50 characters")
    private final String _question;

    private final String question;

    private final QuestionType type;

    private final Set<String> options;

    @JsonCreator
    public FixedQuestion(String _question, QuestionType type, Set<String> options) {
        this._question = Objects.requireNonNull(_question, "_question");
        this.question = normalizeQuestion(this._question);
        this.type = Objects.requireNonNull(type, "type");
        this.options = options;

        if ((type == QuestionType.MULTIPLE_CHOICE || type == QuestionType.SINGLE_CHOICE)
                && (options == null || options.isEmpty() || options.size() < 2)) {
            throw new FatumServiceException(FatumServiceException.QUESTION_WITHOUT_OPTIONS);
        }
    }

    public FixedQuestion(String _question, QuestionType type) {
        this(_question, type, null);
    }

    public String getQuestion() {
        return question;
    }

    private static String normalizeQuestion(String rawQuestion) {
        String normalized = rawQuestion.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return normalized;
        }
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
    }

    public QuestionType getType() {
        return type;
    }

    public Set<String> getOptions() {
        return options;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FixedQuestion that)) {
            return false;
        }
        return _question.equals(that._question)
                && type == that.type
                && Objects.equals(options, that.options);
    }

    @Override
    public int hashCode() {
        int result = _question.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + (options != null ? options.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FixedQuestion(_question=" + _question
                + ", type=" + type
                + ", options=" + options + ")";
    }
}

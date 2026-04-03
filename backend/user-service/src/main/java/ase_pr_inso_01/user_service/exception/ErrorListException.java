package ase_pr_inso_01.user_service.exception;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Common super class for exceptions that report field-specific errors.
 */
public abstract class ErrorListException extends Exception {

  private final Map<String, String> errors;
  private final String messageSummary;
  private final String errorListDescriptor;

  protected ErrorListException(String errorListDescriptor, String messageSummary, Map<String, String> errors) {
    super(messageSummary);
    this.errorListDescriptor = errorListDescriptor;
    this.messageSummary = messageSummary;
    this.errors = errors;
  }

  @Override
  public String getMessage() {
    // We convert the map to a string like "email: invalid, password: short" for logs
    String formattedErrors = errors.entrySet().stream()
            .map(entry -> entry.getKey() + ": " + entry.getValue())
            .collect(Collectors.joining(", "));

    return "%s. %s: %s."
            .formatted(messageSummary, errorListDescriptor, formattedErrors);
  }

  public String summary() {
    return messageSummary;
  }

  public Map<String, String> getErrors() {
    return Collections.unmodifiableMap(errors);
  }
}
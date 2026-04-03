package ase_pr_inso_01.user_service.exception;

import java.util.Map;

/**
 * Exception that signals, that data,
 * that came from outside the backend, conflicts with the current state of the system.
 * The data violates some constraint on relationships
 * (rather than an invariant).
 * Contains a list of all conflict checks that failed when validating the piece of data in question.
 */
public class ConflictException extends ErrorListException {
  public ConflictException(String messageSummary, Map<String, String> errors) {
    super("", messageSummary, errors);
  }

  public ConflictException(String summary, String field, String error) {
    super("Conflict issues", summary, Map.of(field, error));
  }
}

package ase_pr_inso_01.user_service.exception;
import java.util.List;
import java.util.Map;

public class ValidationException extends ErrorListException{
  private Map<String, String> errors;
  public ValidationException(String messageSummary, Map<String, String> errors) {
    super("Validation issues", messageSummary, errors);
  }
  public ValidationException(String summary, String field, String error) {
    super("Validation issues", summary, Map.of(field, error));
  }

  public Map<String, String> getValidationIssues() {
    return errors;
  }
}

package ase_pr_inso_01.farm_service.exception;
import java.util.List;

public class ValidationException extends ErrorListException{
    public ValidationException(String messageSummary, List<String> errors) {
        super("", messageSummary, errors);
    }
}

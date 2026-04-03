package ase_pr_inso_01.user_service.exception;

public class NotFoundException  extends RuntimeException{
  public NotFoundException() {
  }

  public NotFoundException(String message) {
    super(message);
  }

  public NotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public NotFoundException(Exception e) {
    super(e);
  }

}

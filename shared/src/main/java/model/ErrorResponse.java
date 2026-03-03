package model;

public class ErrorResponse {
    private String message;

    public ErrorResponse(String error) {
        this.message = "Error: " + error;
    }

    public String getMessage() {
        return message;
    }
}

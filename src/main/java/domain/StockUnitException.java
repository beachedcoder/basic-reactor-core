package domain;

public class StockUnitException extends Exception {
    public StockUnitException(String message, Throwable error) {
        super(message,error);
    }
}

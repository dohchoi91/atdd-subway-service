package nextstep.subway.station.exeption;

public class DuplicateStationException extends RuntimeException {
    public static final String MESSAGE = "중복된 역 입니다";

    public DuplicateStationException() {
        super(MESSAGE);
    }
}

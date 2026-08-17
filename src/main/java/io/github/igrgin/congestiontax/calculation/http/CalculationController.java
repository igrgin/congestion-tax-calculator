package io.github.igrgin.congestiontax.calculation.http;

import io.github.igrgin.congestiontax.calculation.CalculationService;
import io.github.igrgin.congestiontax.calculation.http.dto.CalculationRequest;
import io.github.igrgin.congestiontax.calculation.http.dto.CalculationResponse;
import io.github.igrgin.congestiontax.calculation.http.exception.InvalidPassageCountException;
import io.github.igrgin.congestiontax.calculation.http.exception.InvalidPassageTimestampException;
import io.github.igrgin.congestiontax.calculation.http.exception.InvalidTimeZoneException;
import io.github.igrgin.congestiontax.calculation.model.CalculationCommand;
import io.github.igrgin.congestiontax.domain.calculation.Passage;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CalculationController {

    private static final DateTimeFormatter PASSAGE_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);

    private final CalculationService calculationService;

    @PostMapping("/api/v1/cities/{cityCode}" + "/congestion-tax/calculations")
    public CalculationResponse calculate(
            @PathVariable String cityCode, @Valid @RequestBody CalculationRequest request) {
        if (request.passages().size() != 1) {
            throw new InvalidPassageCountException(request.passages().size());
        }

        var timeZone = parseTimeZone(request.timeZone());

        var passages = IntStream.range(0, request.passages().size())
                .mapToObj(index -> parsePassage(request.passages().get(index), index, timeZone))
                .toList();

        var calculatedTax =
                calculationService.calculate(new CalculationCommand(cityCode, request.vehicleType(), passages));

        return CalculationResponse.from(calculatedTax);
    }

    private static ZoneId parseTimeZone(String timeZone) {
        if (!ZoneId.getAvailableZoneIds().contains(timeZone)) {
            throw new InvalidTimeZoneException();
        }

        return ZoneId.of(timeZone);
    }

    private static Passage parsePassage(String timestamp, int passageIndex, ZoneId timeZone) {
        try {
            var cityDateTime = LocalDateTime.parse(timestamp, PASSAGE_TIMESTAMP_FORMAT);
            var occurredAt = cityDateTime.atZone(timeZone).toInstant();

            return new Passage(occurredAt, cityDateTime);
        } catch (DateTimeParseException exception) {
            throw new InvalidPassageTimestampException(passageIndex, exception);
        }
    }
}

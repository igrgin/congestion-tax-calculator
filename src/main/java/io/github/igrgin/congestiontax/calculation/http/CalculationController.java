package io.github.igrgin.congestiontax.calculation.http;

import io.github.igrgin.congestiontax.calculation.CalculationService;
import io.github.igrgin.congestiontax.calculation.http.dto.CalculationRequest;
import io.github.igrgin.congestiontax.calculation.http.dto.CalculationResponse;
import io.github.igrgin.congestiontax.calculation.http.exception.InvalidPassageCountException;
import io.github.igrgin.congestiontax.calculation.http.exception.InvalidPassageTimestampException;
import io.github.igrgin.congestiontax.calculation.model.CalculationCommand;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
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

        var passageCityDateTimes = IntStream.range(0, request.passages().size())
                .mapToObj(index -> parsePassageCityDateTime(request.passages().get(index), index))
                .toList();

        var calculatedTax = calculationService.calculate(
                new CalculationCommand(cityCode, request.vehicleType(), passageCityDateTimes));

        return CalculationResponse.from(calculatedTax);
    }

    private static LocalDateTime parsePassageCityDateTime(String timestamp, int passageIndex) {
        try {
            return LocalDateTime.parse(timestamp, PASSAGE_TIMESTAMP_FORMAT);
        } catch (DateTimeParseException exception) {
            throw new InvalidPassageTimestampException(passageIndex, exception);
        }
    }
}

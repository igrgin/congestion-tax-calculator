package io.github.igrgin.congestiontax.calculation.http;

import io.github.igrgin.congestiontax.calculation.CalculationCommand;
import io.github.igrgin.congestiontax.calculation.CalculationService;
import io.github.igrgin.congestiontax.calculation.http.dto.CalculationRequest;
import io.github.igrgin.congestiontax.calculation.http.dto.CalculationResponse;
import io.github.igrgin.congestiontax.calculation.http.exception.InvalidPassageCountException;
import io.github.igrgin.congestiontax.calculation.http.exception.InvalidPassageTimestampException;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CalculationController {

    private final CalculationService calculationService;

    @PostMapping("/api/v1/cities/{cityCode}" + "/congestion-tax/calculations")
    public CalculationResponse calculate(
            @PathVariable String cityCode, @Valid @RequestBody CalculationRequest request) {
        if (request.passages().size() != 1) {
            throw new InvalidPassageCountException(request.passages().size());
        }

        var passageInstants = IntStream.range(0, request.passages().size())
                .mapToObj(index -> parsePassage(request.passages().get(index), index))
                .toList();

        var calculatedTax =
                calculationService.calculate(new CalculationCommand(cityCode, request.vehicleType(), passageInstants));

        return CalculationResponse.from(calculatedTax);
    }

    private static Instant parsePassage(String passage, int passageIndex) {
        try {
            return OffsetDateTime.parse(passage).toInstant();
        } catch (DateTimeParseException exception) {
            throw new InvalidPassageTimestampException(passageIndex, exception);
        }
    }
}

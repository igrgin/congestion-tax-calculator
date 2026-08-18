package io.github.igrgin.congestiontax.calculation.http;

import io.github.igrgin.congestiontax.calculation.CalculationService;
import io.github.igrgin.congestiontax.calculation.http.dto.CalculationRequest;
import io.github.igrgin.congestiontax.calculation.http.dto.CalculationResponse;
import io.github.igrgin.congestiontax.calculation.model.CalculationCommand;
import jakarta.validation.Valid;
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
        var command = new CalculationCommand(cityCode, request.vehicleType(), request.passages());
        var calculatedTax = calculationService.calculate(command);

        return CalculationResponse.from(calculatedTax);
    }
}

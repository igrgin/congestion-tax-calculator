package io.github.igrgin.congestiontax.citylocaltime.persistence;

import io.github.igrgin.congestiontax.citylocaltime.CityLocalTimeService;
import io.github.igrgin.congestiontax.citylocaltime.exception.UnknownCityException;
import io.github.igrgin.congestiontax.domain.calculation.LocalizedPassage;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CityLocalTimeServiceImpl implements CityLocalTimeService {

    private final CityRepository cityRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LocalizedPassage> localize(String cityCode, List<Instant> passageInstants) {
        var cityEntity = cityRepository.findByCode(cityCode).orElseThrow(() -> new UnknownCityException(cityCode));

        var cityTimeZone = cityEntity.toZoneId();

        return passageInstants.stream()
                .map(passageInstant ->
                        new LocalizedPassage(passageInstant, LocalDateTime.ofInstant(passageInstant, cityTimeZone)))
                .toList();
    }
}

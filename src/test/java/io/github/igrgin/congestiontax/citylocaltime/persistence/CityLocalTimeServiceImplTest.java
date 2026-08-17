package io.github.igrgin.congestiontax.citylocaltime.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import io.github.igrgin.congestiontax.citylocaltime.CityLocalTimeService;
import io.github.igrgin.congestiontax.citylocaltime.exception.UnknownCityException;
import io.github.igrgin.congestiontax.domain.calculation.LocalizedPassage;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CityLocalTimeServiceImplTest {

    @Mock
    private CityRepository cityRepository;

    private CityLocalTimeService cityLocalTimeService;

    @BeforeEach
    void setUp() {
        cityLocalTimeService = new CityLocalTimeServiceImpl(cityRepository);
    }

    @Test
    void convertsPassageInstantToCityLocalTime() {
        var cityEntity = new CityEntity("gothenburg", "Gothenburg", "Europe/Stockholm");
        var passageInstant = Instant.parse("2013-02-08T05:20:27Z");

        given(cityRepository.findByCode("gothenburg")).willReturn(Optional.of(cityEntity));

        var result = cityLocalTimeService.localize("gothenburg", List.of(passageInstant));

        assertThat(result)
                .containsExactly(new LocalizedPassage(passageInstant, LocalDateTime.of(2013, 2, 8, 6, 20, 27)));
    }

    @Test
    void rejectsUnknownCity() {
        var passageInstants = List.of(Instant.parse("2013-02-08T05:20:27Z"));

        given(cityRepository.findByCode("unknown")).willReturn(Optional.empty());

        assertThatThrownBy(() -> cityLocalTimeService.localize("unknown", passageInstants))
                .isInstanceOf(UnknownCityException.class);
    }
}

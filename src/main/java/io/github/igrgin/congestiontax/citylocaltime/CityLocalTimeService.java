package io.github.igrgin.congestiontax.citylocaltime;

import io.github.igrgin.congestiontax.domain.calculation.LocalizedPassage;
import java.time.Instant;
import java.util.List;

public interface CityLocalTimeService {

    public List<LocalizedPassage> localize(String cityCode, List<Instant> passageInstants);
}

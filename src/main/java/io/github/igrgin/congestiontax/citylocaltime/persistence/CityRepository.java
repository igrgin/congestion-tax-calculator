package io.github.igrgin.congestiontax.citylocaltime.persistence;

import java.util.Optional;
import org.springframework.data.repository.Repository;

interface CityRepository extends Repository<CityEntity, Long> {

    public Optional<CityEntity> findByCode(String code);
}

package io.github.igrgin.congestiontax.taxrule.persistence;

import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface CityRepository extends Repository<CityEntity, Long> {

    Optional<CityEntity> findByCode(String code);
}

package io.github.igrgin.congestiontax.taxrule.persistence;

import java.util.Optional;
import org.springframework.data.repository.Repository;

interface VehicleTypeRepository extends Repository<VehicleTypeEntity, String> {

    public Optional<VehicleTypeEntity> findByCode(String code);
}

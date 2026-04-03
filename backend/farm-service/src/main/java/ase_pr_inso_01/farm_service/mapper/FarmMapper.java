package ase_pr_inso_01.farm_service.mapper;

import java.util.Arrays;
import java.util.Map;

import ase_pr_inso_01.farm_service.controller.dto.farm.FarmCreateDto;
import ase_pr_inso_01.farm_service.controller.dto.farm.FarmDetailsDto;
import ase_pr_inso_01.farm_service.controller.dto.farm.FieldCreateDto;
import ase_pr_inso_01.farm_service.controller.dto.farm.FieldDetailsDto;
import ase_pr_inso_01.farm_service.models.Farm;
import ase_pr_inso_01.farm_service.models.Field;
import org.springframework.stereotype.Component;

@Component
public class FarmMapper {
    public Farm farmCreateDtoToFarm(FarmCreateDto dto, String userId) {
        Farm farm = new Farm();

        farm.setName(dto.name());
        farm.setLatitude(dto.latitude());
        farm.setLongitude(dto.longitude());
        farm.setSoilType(dto.soilType());
        farm.setUserId(userId);

        Field[] fields = Arrays.stream(dto.fields())
                .map(this::fieldCreateDtoToField)
                .toArray(Field[]::new);
        farm.setFields(fields);

        return farm;
    }

    public FarmDetailsDto farmToFarmDetailsDto(Farm farm, Map<String, Double> factors) {
        FieldDetailsDto[] fieldDetailsDtos = null;

        fieldDetailsDtos = Arrays.stream(farm.getFields())
                .map(this::fieldToFieldDetailsDto)
                .toArray(FieldDetailsDto[]::new);

        return new FarmDetailsDto(
                farm.getId(),
                farm.getName(),
                farm.getLatitude(),
                farm.getLongitude(),
                farm.getSoilType(),
                fieldDetailsDtos,
                farm.getRecommendations(),
                farm.getUserId(),
                factors
        );
    }

    public Field fieldCreateDtoToField(FieldCreateDto dto) { //TODO: Possibly extract to separate mapper (Fields mapper)
        Field field = new Field();
        field.setId(dto.id());
        field.setStatus(dto.status());
        return field;
    }

    private FieldDetailsDto fieldToFieldDetailsDto(Field field) {
        return new FieldDetailsDto(
                field.getId(),
                field.getStatus(),
                field.getSeedType(),
                field.getPlantedDate(),
                field.getHarvestDate(),
                field.getGrowthStage()
        );
    }
}

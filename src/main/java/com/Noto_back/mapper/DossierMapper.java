package com.Noto_back.mapper;

import com.Noto_back.dto.DossierResponse;
import com.Noto_back.model.Dossier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DossierMapper {

    @Mapping(target = "parentId", expression = "java(dossier.getParent() != null ? dossier.getParent().getId() : null)")
    DossierResponse toResponse(Dossier dossier);
}

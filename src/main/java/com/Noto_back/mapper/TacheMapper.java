package com.Noto_back.mapper;

import com.Noto_back.dto.TacheResponse;
import com.Noto_back.model.Tache;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TacheMapper {
    TacheResponse toResponse(Tache tache);
}

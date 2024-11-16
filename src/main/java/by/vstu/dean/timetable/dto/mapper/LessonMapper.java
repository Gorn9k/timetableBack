package by.vstu.dean.timetable.dto.mapper;

import by.vstu.dean.core.models.mapper.BaseMapperInterface;
import by.vstu.dean.core.utils.ReflectionUtils;
import by.vstu.dean.timetable.dto.LessonDTO;
import by.vstu.dean.timetable.models.LessonModel;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface LessonMapper extends BaseMapperInterface<LessonDTO, LessonModel> {

    @Override
    default LessonDTO toDto(LessonModel entity) {
        return entity == null ? null : (LessonDTO) ReflectionUtils.mapObject(entity, new LessonModel(), false, false);
    }

}
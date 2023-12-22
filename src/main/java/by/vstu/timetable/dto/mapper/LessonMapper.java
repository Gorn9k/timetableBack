package by.vstu.timetable.dto.mapper;

import by.vstu.dean.dto.future.BaseMapperInterface;
import by.vstu.timetable.dto.LessonDTO;
import by.vstu.timetable.models.LessonModel;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface LessonMapper extends BaseMapperInterface<LessonDTO, LessonModel> {

}
package by.vstu.timetable.dto.mapper.impl;

import by.vstu.timetable.dto.LessonDTO;
import by.vstu.timetable.dto.mapper.LessonMapper;
import by.vstu.timetable.models.LessonModel;
import by.vstu.timetable.repo.LessonModelRepository;
import by.vstu.timetable.repo.rest.ClassroomRepo;
import by.vstu.timetable.repo.rest.DisciplinesRepo;
import by.vstu.timetable.repo.rest.GroupsRepo;
import by.vstu.timetable.repo.rest.TeacherRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LessonMapperImpl implements LessonMapper {

    @Autowired
    private GroupsRepo groupsRepo;
    @Autowired
    private DisciplinesRepo disciplinesRepo;
    @Autowired
    private TeacherRepo teacherRepo;
    @Autowired
    private ClassroomRepo classroomRepo;

    @Override
    public LessonModel toEntity(LessonDTO dto) {

        if(dto == null)
            return null;

        LessonModel lessonModel = new LessonModel();

        lessonModel.setId(dto.getId());
        lessonModel.setSourceId(dto.getSourceId());
        lessonModel.setUpdated(dto.getUpdated());

        lessonModel.setLessonNumber(dto.getLessonNumber());
        lessonModel.setDay(dto.getDay());
        lessonModel.setLessonType(dto.getLessonType());
        lessonModel.setWeekType(dto.getWeekType());
        lessonModel.setRoomId(dto.getClassroom().getId());
        lessonModel.setSubGroup(dto.getSubGroup());
        lessonModel.setGroupId(dto.getGroup().getId());
        lessonModel.setDisciplineId(dto.getDiscipline().getId());
        lessonModel.setTeacherId(dto.getTeacher().getId());
        lessonModel.setStartDate(dto.getStartDate());
        lessonModel.setEndDate(dto.getEndDate());


        return lessonModel;
    }

    @Override
    public LessonDTO toDto(LessonModel entity) {
        if(entity == null)
            return null;

        LessonDTO lessonDTO = new LessonDTO();

        lessonDTO.setId(entity.getId());
        lessonDTO.setSourceId(entity.getSourceId());
        lessonDTO.setUpdated(entity.getUpdated());

        lessonDTO.setLessonNumber(entity.getLessonNumber());
        lessonDTO.setDay(entity.getDay());
        lessonDTO.setLessonType(entity.getLessonType());
        lessonDTO.setWeekType(entity.getWeekType());
        lessonDTO.setClassroom(this.classroomRepo.getSingle(entity.getRoomId()));
        lessonDTO.setSubGroup(entity.getSubGroup());
        lessonDTO.setGroup(this.groupsRepo.getSingle(entity.getGroupId()));
        lessonDTO.setDiscipline(this.disciplinesRepo.getSingle(entity.getDisciplineId()));
        lessonDTO.setTeacher(this.teacherRepo.getSingle(entity.getTeacherId()));
        lessonDTO.setStartDate(entity.getStartDate());
        lessonDTO.setEndDate(entity.getEndDate());

        return lessonDTO;
    }

    @Override
    public LessonModel partialUpdate(LessonDTO dto, LessonModel entity) {
        return null;
    }

    @Override
    public List<LessonDTO> toDto(List<LessonModel> all) {
        return all.stream().map(this::toDto).toList();
    }

    @Override
    public List<LessonModel> toEntity(List<LessonDTO> all) {
        return all.stream().map(this::toEntity).toList();
    }
}

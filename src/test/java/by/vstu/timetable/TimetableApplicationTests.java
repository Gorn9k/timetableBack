package by.vstu.timetable;

import by.vstu.dean.future.models.students.GroupModel;
import by.vstu.timetable.enums.ESubGroup;
import by.vstu.timetable.models.StreamModel;
import by.vstu.timetable.repo.rest.DisciplinesRepo;
import by.vstu.timetable.repo.rest.GroupsRepo;
import by.vstu.timetable.repo.rest.TeacherRepo;
import by.vstu.timetable.services.LessonService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootTest
class TimetableApplicationTests {

	@Autowired
	private TeacherRepo teacherRepo;
	@Autowired
	private LessonService lessonService;
	@Autowired
	private GroupsRepo groupsRepo;
	@Autowired
	private DisciplinesRepo disciplinesRepo;

	@Test
	void contextLoads() {

		List<StreamModel> streams = new ArrayList<>();

		lessonService.getAll().stream().filter(p -> p.getSubGroup().equals(ESubGroup.ALL)).forEach(lesson -> {


			streams.stream().filter(p -> p.getStreamCourse() == groupsRepo.getSingle(lesson.getGroupId()).getYearStart().shortValue() && p.getDisciplineId() == lesson.getDisciplineId() && p.getTeacherId().equals(lesson.getTeacherId()) && p.getRoomId() == lesson.getRoomId() && lesson.getDay() == p.getDay() && lesson.getLessonNumber() == p.getLessonNumber() && lesson.getLessonType().equals(p.getLessonType())).forEach(stream -> {
//				System.out.printf("%s %s %s\n", stream.getDisciplineId(), lesson.getGroupId(), stream.getGroups());
				stream.add(lesson.getGroupId());
			});




//			if(streams.stream().noneMatch(p -> p.getStreamCourse() == groupsRepo.getSingle(lesson.getGroupId()).getYearStart().shortValue() && p.getDisciplineId() == lesson.getDisciplineId() && p.getTeacherId().equals(lesson.getTeacherId()) && p.getRoomId() == lesson.getRoomId() && lesson.getDay() == p.getDay() && lesson.getLessonNumber() == p.getLessonNumber() && p.getLessonType().equals(lesson.getLessonType()))) {
//				StreamModel streamModel = new StreamModel();
//				streamModel.setLessonType(lesson.getLessonType());
//				streamModel.setDisciplineId(lesson.getDisciplineId());
//				streamModel.setDay(lesson.getDay());
//				streamModel.setTeacherId(lesson.getTeacherId());
//				streamModel.setRoomId(lesson.getRoomId());
//				GroupModel group = groupsRepo.getSingle(lesson.getGroupId());
//				streamModel.setStreamCourse(group.getYearStart().shortValue());
//				streamModel.setLessonNumber(lesson.getLessonNumber());
//				streamModel.setGroups(new HashSet<>(Collections.singletonList(lesson.getGroupId())));
//				streams.add(streamModel);
//			}



		});

		streams.stream().filter(p -> p.getGroups().size() > 1).forEach(stream -> {

			StringBuilder groups = new StringBuilder();

			stream.getGroups().forEach(g -> {
				groups.append(this.groupsRepo.getSingle(g).getName()).append(" ");
			});
			System.out.println("-----------------------------------------------");
			System.out.println(this.disciplinesRepo.getSingle(stream.getDisciplineId()).getName());
			System.out.println(stream.getDay());
			System.out.println(stream.getLessonNumber());
			System.out.println(groups);
			System.out.println("-----------------------------------------------");
		});
	}

}

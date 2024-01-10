package by.vstu.timetable.repo.rest;

import by.vstu.dean.future.models.lessons.TeacherModel;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

@Repository
public class TeacherRepo extends DeanApiRepositoryBase<TeacherModel> {

    public TeacherRepo() {
        super("teachers/", TeacherModel.class);
    }

    @Override
    @Cacheable(value = "teachers", key = "#id")
    public TeacherModel getSingle(Long id) {
        return super.getSingle(id);
    }

}

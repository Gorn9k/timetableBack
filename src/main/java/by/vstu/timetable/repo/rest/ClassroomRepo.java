package by.vstu.timetable.repo.rest;

import by.vstu.dean.future.models.rooms.ClassroomModel;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

@Repository
public class ClassroomRepo extends DeanApiRepositoryBase<ClassroomModel> {

    public ClassroomRepo() {
        super("classes/", ClassroomModel.class);
    }

    @Override
    @Cacheable(value = "classes", key = "#id")
    public ClassroomModel getSingle(Long id) {
        return super.getSingle(id);
    }

}

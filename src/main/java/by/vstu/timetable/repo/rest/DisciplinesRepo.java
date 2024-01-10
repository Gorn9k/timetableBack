package by.vstu.timetable.repo.rest;

import by.vstu.dean.future.models.lessons.DisciplineModel;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

@Repository
public class DisciplinesRepo extends DeanApiRepositoryBase<DisciplineModel> {

    protected DisciplinesRepo() {
        super("disciplines/", DisciplineModel.class);
    }

    @Override
    @Cacheable(value = "disciplines", key = "#id")
    public DisciplineModel getSingle(Long id) {
        return super.getSingle(id);
    }

}

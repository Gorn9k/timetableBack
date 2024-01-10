package by.vstu.timetable.repo.rest;

import by.vstu.dean.future.models.lessons.DepartmentModel;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

@Repository
public class DepartmentsRepo extends DeanApiRepositoryBase<DepartmentModel> {

    protected DepartmentsRepo() {
        super("departments/", DepartmentModel.class);
    }

    @Override
    @Cacheable(value = "departments", key = "#id")
    public DepartmentModel getSingle(Long id) {
        return super.getSingle(id);
    }

}

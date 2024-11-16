package by.vstu.dean.timetable.repo.rest;

import by.vstu.dean.core.requests.TokenRequest;
import by.vstu.dean.support.models.DepartmentModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Objects;

@Repository
public class DepartmentsRepo extends DeanApiRepositoryBase<DepartmentModel> {

    private static final Logger log = LoggerFactory.getLogger(DepartmentsRepo.class);

    protected DepartmentsRepo(TokenRequest tokenRequest) {
        super("departments/", DepartmentModel.class, tokenRequest);
    }

    @Override
    @Cacheable(value = "departments")
    public DepartmentModel getSingle(Long id) {
        DepartmentModel departmentModel = super.getSingle(id);
        if (!Objects.equals(id, departmentModel.getId()))
            log.warn("id={};departmentId={}", id, departmentModel.getId());
        return departmentModel;
    }

}

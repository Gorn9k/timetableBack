package by.vstu.dean.timetable.repo.rest;


import by.vstu.dean.core.requests.TokenRequest;
import by.vstu.dean.support.models.TeacherModel;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

@Repository
public class TeacherRepo extends DeanApiRepositoryBase<TeacherModel> {

    private static final Logger log = LoggerFactory.getLogger(TeacherRepo.class);

    private List<TeacherModel> allModels;

    public TeacherRepo(TokenRequest tokenRequest) {
        super("teachers/", TeacherModel.class, tokenRequest);
    }

    @Override
    @Cacheable(value = "teachers")
    public TeacherModel getSingle(Long id) {
        if (allModels == null || allModels.isEmpty())
            allModels = this.getAll();//super.rsql("status==ACTIVE");

        TeacherModel teacherModel = allModels.stream().filter(t -> t.getId().equals(id)).findAny().orElse(null);
        if (teacherModel == null)
            log.warn("teacher not found for id={}", id);
        if (teacherModel != null && !Objects.equals(id, teacherModel.getId()))
            log.warn("id={};teacherId={}", id, teacherModel.getId());
        return teacherModel;
    }

    @PostConstruct
    @Scheduled(initialDelayString = "PT5M",fixedDelayString = "PT5M")
    public void getAllActive() {
        this.allModels = this.getAll();//super.rsql("status==ACTIVE");
    }

}

package by.vstu.dean.timetable.repo.rest;


import by.vstu.dean.core.requests.TokenRequest;
import by.vstu.dean.support.models.ClassroomModel;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Objects;

@Repository
public class ClassroomRepo extends DeanApiRepositoryBase<ClassroomModel> {

    private static final Logger log = LoggerFactory.getLogger(ClassroomRepo.class);

    private List<ClassroomModel> allModels;

    public ClassroomRepo(TokenRequest tokenRequest) {
        super("classes/", ClassroomModel.class, tokenRequest);
    }

    @Override
    @Cacheable(value = "classes")
    public ClassroomModel getSingle(Long id) {
        if (allModels == null || allModels.isEmpty())
            allModels = this.getAll(); //super.rsql("status==ACTIVE");

//        ClassroomModel classroomModel = super.getSingle(id);
        ClassroomModel classroomModel = allModels.stream().filter(c -> c.getId().equals(id)).findAny().orElse(null);
        if (classroomModel == null)
            log.warn("Classroom not found for id={}", id);
        if (classroomModel != null && !Objects.equals(id, classroomModel.getId()))
            log.warn("id={};classroomId={}", id, classroomModel.getId());
        return classroomModel;
    }

    @PostConstruct
    @Scheduled(initialDelayString = "PT5M",fixedDelayString = "PT5M")
    public void getAllActive() {
        this.allModels = this.getAll();//super.rsql("status==ACTIVE");
    }

}

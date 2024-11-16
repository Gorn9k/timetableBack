package by.vstu.dean.timetable.repo.rest;


import by.vstu.dean.core.requests.TokenRequest;
import by.vstu.dean.support.models.DisciplineModel;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Objects;

@Repository
public class DisciplinesRepo extends DeanApiRepositoryBase<DisciplineModel> {

    private static final Logger log = LoggerFactory.getLogger(DisciplinesRepo.class);

    private List<DisciplineModel> allModels;

    protected DisciplinesRepo(TokenRequest tokenRequest) {
        super("disciplines/", DisciplineModel.class, tokenRequest);
    }

    @Override
    @Cacheable(value = "disciplines")
    public DisciplineModel getSingle(Long id) {
        if (allModels == null || allModels.isEmpty())
            allModels = this.getAll(); //super.rsql("status==ACTIVE");

        DisciplineModel disciplineModel = allModels.stream().filter(d -> d.getId().equals(id)).findAny().orElse(null);
        if (disciplineModel == null)
            log.warn("Discipline not found for id={}", id);
        if (disciplineModel != null && !Objects.equals(id, disciplineModel.getId()))
            log.warn("id={};disciplineId={}", id, disciplineModel.getId());
        return disciplineModel;
    }

    @PostConstruct
    @Scheduled(initialDelayString = "PT5M",fixedDelayString = "PT5M")
    public void getAllActive() {
        this.allModels = this.getAll();//super.rsql("status==ACTIVE");
    }

}

package by.vstu.dean.timetable.repo.rest;

import by.vstu.dean.core.requests.TokenRequest;
import by.vstu.dean.support.models.GroupModel;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Objects;

@Repository
public class GroupsRepo extends DeanApiRepositoryBase<GroupModel> {

    private static final Logger log = LoggerFactory.getLogger(GroupsRepo.class);

    private List<GroupModel> allModels;

    public GroupsRepo(TokenRequest tokenRequest) {
        super("groups/", GroupModel.class, tokenRequest);
    }

    @Override
    @Cacheable(value = "groups")
    public GroupModel getSingle(Long id) {
        if (allModels == null || allModels.isEmpty())
            allModels = this.getAll();//super.rsql("status==ACTIVE");

        GroupModel groupModel = allModels.stream().filter(g -> g.getId().equals(id)).findAny().orElse(null);
        if (groupModel == null)
            log.warn("Group not found for id={}", id);
        if (groupModel != null && !Objects.equals(id, groupModel.getId()))
            log.warn("id={};groupId={}", id, groupModel.getId());
        return groupModel;
    }

    @PostConstruct
    @Scheduled(initialDelayString = "PT5M",fixedDelayString = "PT5M")
    public void getAllActive() {
        this.allModels = this.getAll();//super.rsql("status==ACTIVE");
    }
}

package by.vstu.timetable.repo.rest;

import by.vstu.dean.future.models.students.GroupModel;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

@Repository
public class GroupsRepo extends DeanApiRepositoryBase<GroupModel> {
    public GroupsRepo() {
        super("groups/", GroupModel.class);
    }

    @Override
    @Cacheable(value = "groups", key = "#id")
    public GroupModel getSingle(Long id) {
        return super.getSingle(id);
    }
}

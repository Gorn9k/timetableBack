package by.vstu.timetable.repo.rest;

import by.vstu.dean.future.DBBaseModel;
import by.vstu.dean.requests.TokenRequest;
import by.vstu.dean.requests.repo.ApiRepositoryBase;
import org.springframework.data.repository.NoRepositoryBean;


@NoRepositoryBean
public abstract class DeanApiRepositoryBase<O extends DBBaseModel> extends ApiRepositoryBase<O> {

    protected DeanApiRepositoryBase(String endpoint, Class<O> target) {
        super("http://192.168.11.252:18076/api", endpoint, "", new TokenRequest("http://192.168.11.252:8888/token?grant_type=password", "admin@gmail.com", "admin", "DEAN_RSQL", "jBN5qHywPBjh").getToken(), target);
    }

}

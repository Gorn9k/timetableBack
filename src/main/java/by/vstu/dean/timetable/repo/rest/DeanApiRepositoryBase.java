package by.vstu.dean.timetable.repo.rest;


import by.vstu.dean.core.models.DBBaseModel;
import by.vstu.dean.core.requests.TokenRequest;
import by.vstu.dean.core.requests.repo.ApiRepositoryBase;
import com.google.gson.GsonBuilder;
import jakarta.annotation.PostConstruct;
import org.modelmapper.TypeToken;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;


@NoRepositoryBean
@EnableScheduling
public abstract class DeanApiRepositoryBase<O extends DBBaseModel> extends ApiRepositoryBase<O> {

    protected String authUrl = "http://192.168.11.252:8888/token?grant_type=password";
    protected String clientId = "DEAN_RSQL";
    protected String clientSecret = "jBN5qHywPBjh";
    protected String login = "admin@gmail.com";
    protected String password = "admin";
    protected final TokenRequest tokenRequest;

    protected DeanApiRepositoryBase(String endpoint, Class<O> target, TokenRequest tokenRequest) {
        super("http://192.168.11.252:18076/api", endpoint, "", target);
        this.tokenRequest = tokenRequest;
    }

    @PostConstruct
    public void init() {
        this.setTokenRequest(this.tokenRequest);
    }

//    @Scheduled(initialDelayString = "PT20H", fixedRateString = "PT20H")
//    public void updateToken() {
//
//        System.out.println(LocalDateTime.now().toString() + " === ОБНОВЛЕНИЕ ТОКЕНА");
//
//        BaseRequest<String> request = new BaseRequest<String>(authUrl)
//                .setMediaType(MediaType.APPLICATION_FORM_URLENCODED);
//
//        if (this.clientId != null && this.password != null) {
//            request.setAuthData(clientId, clientSecret);
//        }
//
//        request.setAuthHeaders();
//
//        String json = request.run(String.format("username=%s&password=%s", this.login, this.password));
//
//        TokenModel tokenModel = new GsonBuilder()
//                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeJsonAdapter()).create()
//                .fromJson(json, new TypeToken<TokenModel>() {
//                }.getType());
//
//        System.setProperty("accessToken", tokenModel.getAccessToken());
//
//        Date exp = new Date(System.currentTimeMillis() + Long.parseLong(tokenModel.getExpiresIn()) * 1000);
//        LocalDateTime expires = exp.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
//
//        System.setProperty("expires_at", expires.toString());
//        this.setToken(System.getProperty("accessToken"));
//    }

}

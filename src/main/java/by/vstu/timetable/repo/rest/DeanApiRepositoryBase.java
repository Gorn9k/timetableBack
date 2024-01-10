package by.vstu.timetable.repo.rest;

import by.vstu.dean.adapters.json.LocalDateTimeJsonAdapter;
import by.vstu.dean.auth.models.TokenModel;
import by.vstu.dean.future.DBBaseModel;
import by.vstu.dean.requests.BaseRequest;
import by.vstu.dean.requests.TokenRequest;
import by.vstu.dean.requests.repo.ApiRepositoryBase;
import com.google.gson.GsonBuilder;
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

    protected DeanApiRepositoryBase(String endpoint, Class<O> target) {
        super("http://192.168.11.252:18076/api", endpoint, "", new TokenRequest("http://192.168.11.252:8888/token?grant_type=password", "admin@gmail.com", "admin", "DEAN_RSQL", "jBN5qHywPBjh").getToken(), target);
    }

    @Scheduled(initialDelayString = "PT20H", fixedRateString = "PT20H")
//    @Scheduled(initialDelayString = "PT20H", fixedRateString = "P1D")
    public void updateToken() {

        System.out.println(LocalDateTime.now().toString() + " === ОБНОВЛЕНИЕ ТОКЕНА");

        BaseRequest<String> request = new BaseRequest<String>(authUrl)
                .setMediaType(MediaType.APPLICATION_FORM_URLENCODED);

        // Если предоставлены данные клиента, добавляем их в запрос.
        if (this.clientId != null && this.password != null) {
            request.setAuthData(clientId, clientSecret);
        }

        // Устанавливаем заголовки для аутентификации.
        request.setAuthHeaders();

        // Выполняем POST-запрос для получения токена аутентификации.
        String json = request.run(String.format("username=%s&password=%s", this.login, this.password));

        // Десериализуем полученный JSON-ответ, преобразовывая его в объект TokenModel.
        TokenModel tokenModel = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeJsonAdapter()).create()
                .fromJson(json, new TypeToken<TokenModel>() {
                }.getType());


        // Сохраняем полученный токен и его срок действия в системных свойствах.
        System.setProperty("accessToken", tokenModel.getAccessToken());

        Date exp = new Date(System.currentTimeMillis() + Long.parseLong(tokenModel.getExpiresIn()) * 1000);
        LocalDateTime expires = exp.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

        System.setProperty("expires_at", expires.toString());

        System.out.println("New token: " + System.getProperty("accessToken"));
        System.out.println("New expires_at: " + System.getProperty("expires_at"));
        this.setToken(System.getProperty("accessToken"));
        System.out.println("New request token: " + this.dgetToken().toString());
    }

}

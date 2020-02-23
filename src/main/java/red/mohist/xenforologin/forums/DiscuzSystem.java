package red.mohist.xenforologin.forums;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.http.util.EntityUtils;
import org.bukkit.entity.Player;
import red.mohist.xenforologin.XenforoLogin;
import red.mohist.xenforologin.enums.ResultType;
import red.mohist.xenforologin.interfaces.ForumSystem;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getLogger;

@SuppressWarnings("unused")
public class DiscuzSystem implements ForumSystem {

    private final String url;
    private final String key;

    public DiscuzSystem(String url, String key) {
        this.url = url;
        this.key = key;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Nonnull
    @Override
    public ResultType register(Player player, String password, String email) {
        return ResultType.NO_USER;
    }


    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public ResultType login(Player player, String password) {
        try {
            ResponseHandler<String> responseHandler = response -> {
                int status = response.getStatusLine().getStatusCode();
                if (status == 200) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else if (status == 404) {
                    XenforoLogin.instance.getLogger().warning(XenforoLogin.instance.langFile("errors.url", ImmutableMap.of(
                            "url", url)));
                }
                return null;
            };

            String result = Request.Post(url + "?action=login")
                    .bodyForm(Form.form().add("login", player.getName())
                            .add("password", password).build())
                    .addHeader("UC-Api-Key", key)
                    .execute().handleResponse(responseHandler);


            if (result == null) {
                return ResultType.SERVER_ERROR;
            }
            JsonParser parse = new JsonParser();
            JsonObject json = parse.parse(result).getAsJsonObject();
            if (json == null) {
                return ResultType.SERVER_ERROR;
            }
            if (json.get("success") != null && json.get("success").getAsBoolean()) {
                json.get("user").getAsJsonObject().get("username").getAsString();
                if (json.get("user").getAsJsonObject().get("username").getAsString().equals(player.getName())) {
                    return ResultType.OK;
                } else {
                    return ResultType.ERROR_NAME.inheritedObject(ImmutableMap.of(
                            "correct", json.getAsJsonObject("exact").get("username").getAsString()));
                }
            } else {
                JsonArray errors = json.get("errors").getAsJsonArray();
                if (errors.size() > 0) {
                    if (errors.get(0).getAsJsonObject().get("code").getAsString().equals("incorrect_password")) {
                        return ResultType.PASSWORD_INCORRECT;
                    } else if (errors.get(0).getAsJsonObject().get("code").getAsString().equals("requested_user_x_not_found")) {
                        return ResultType.NO_USER;
                    } else {
                        if (errors.get(0).getAsJsonObject().get("code").getAsString().equals("UC_key_error")) {
                            XenforoLogin.instance.getLogger().warning(XenforoLogin.instance.langFile("errors.key", ImmutableMap.of(
                                    "key", key)));
                            return ResultType.SERVER_ERROR;
                        }
                        return ResultType.UNKNOWN.inheritedObject(ImmutableMap.of(
                                "code", errors.get(0).getAsJsonObject().get("code").getAsString(),
                                "message", errors.get(0).getAsJsonObject().get("message").getAsString()));
                    }
                } else {
                    return ResultType.SERVER_ERROR;
                }
            }
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Error while checking player " + player.getName() + " data", e);
            return ResultType.SERVER_ERROR;
        }
    }

    @Nonnull
    @Override
    public ResultType join(Player player) {
        return join(player.getName());
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public ResultType join(String name) {
        ResponseHandler<String> responseHandler = response -> {
            int status = response.getStatusLine().getStatusCode();
            if (status == 200) {
                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            } else if (status == 404) {
                getLogger().warning(XenforoLogin.instance.langFile("errors.url", ImmutableMap.of(
                        "url", url)));
            }
            return null;
        };
        String result;
        try {
            result = Request.Post(url + "?action=join")
                    .bodyForm(Form.form().add("login", name)
                            .build())
                    .addHeader("UC-Api-Key", key)
                    .execute().handleResponse(responseHandler);
        } catch (IOException e) {
            e.printStackTrace();
            return ResultType.SERVER_ERROR;
        }
        if (result == null) {
            new ClientProtocolException("Unexpected response: null").printStackTrace();
            return ResultType.SERVER_ERROR;
        }
        JsonParser parse = new JsonParser();
        JsonObject json = parse.parse(result).getAsJsonObject();
        if (json == null) {
            new ClientProtocolException("Unexpected json: null").printStackTrace();
            return ResultType.SERVER_ERROR;
        }
        if (json.get("exact") != null && json.get("exact").isJsonNull()) {
            return ResultType.NO_USER;
        }
        if (json.get("errors") != null && json.get("errors").getAsJsonArray().get(0).getAsJsonObject().get("code").getAsString().equals("UC_key_error")) {
            XenforoLogin.instance.getLogger().warning(XenforoLogin.instance.langFile("errors.key", ImmutableMap.of(
                    "key", key)));
            return ResultType.SERVER_ERROR;
        }
        if (!json.getAsJsonObject("exact").get("username").getAsString().equals(name)) {
            return ResultType.ERROR_NAME.inheritedObject(ImmutableMap.of(
                    "correct", json.getAsJsonObject("exact").get("username").getAsString()));
        }
        return ResultType.OK;
    }
}

/*
 * Copyright 2020 Mohist-Community
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package red.mohist.sodionauth.yggdrasilserver.controller;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.netty.handler.codec.http.*;
import red.mohist.sodionauth.core.enums.ResultType;
import red.mohist.sodionauth.core.utils.Helper;
import red.mohist.sodionauth.yggdrasilserver.modules.RefreshRespone;
import red.mohist.sodionauth.yggdrasilserver.provider.UserProvider;

import java.sql.SQLException;

public class ValidateController implements Controller {
    @Override
    public Object handle(JsonElement content, FullHttpRequest request) throws SQLException {
        JsonObject post=content.getAsJsonObject();
        String accessToken = post.get("accessToken").getAsString();
        String clientToken = post.get("clientToken").getAsString();
        if(UserProvider.instance.verifyToken(clientToken,accessToken)){
            return new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.NO_CONTENT);
        }else{
            return new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK);
        }
    }
}
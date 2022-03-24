/*
 *    Copyright (c) 2021, VRAI Labs and/or its affiliates. All rights reserved.
 *
 *    This software is licensed under the Apache License, Version 2.0 (the
 *    "License") as published by the Apache Software Foundation.
 *
 *    You may not use this file except in compliance with the License. You may
 *    obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 */

package io.supertokens.test.session.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.supertokens.ProcessState;
import io.supertokens.test.TestingProcessManager;
import io.supertokens.test.Utils;
import io.supertokens.test.httpRequest.HttpRequestForTesting;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

public class RefreshSessionAPITest2_13 {
    @Rule
    public TestRule watchman = Utils.getOnFailure();

    @AfterClass
    public static void afterTesting() {
        Utils.afterTesting();
    }

    @Before
    public void beforeEach() {
        Utils.reset();
    }

    @Test
    public void successOutputWithGrantPayload() throws Exception {
        String[] args = { "../" };

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        String userId = "userId";
        JsonObject userDataInJWT = new JsonObject();
        userDataInJWT.addProperty("key", "value");
        JsonObject userDataInDatabase = new JsonObject();
        userDataInDatabase.addProperty("key", "value");
        JsonObject grantPayload = Utils.getExampleGrantPayload();

        JsonObject request = new JsonObject();
        request.addProperty("userId", userId);
        request.add("userDataInJWT", userDataInJWT);
        request.add("userDataInDatabase", userDataInDatabase);
        request.add("grants", grantPayload);
        request.addProperty("enableAntiCsrf", false);

        JsonObject sessionInfo = HttpRequestForTesting.sendJsonPOSTRequest(process.getProcess(), "",
                "http://localhost:3567/recipe/session", request, 1000, 1000, null, Utils.getCdiVersion2_13ForTests(),
                "session");
        assertEquals(sessionInfo.get("status").getAsString(), "OK");

        JsonObject sessionRefreshBody = new JsonObject();

        sessionRefreshBody.addProperty("refreshToken",
                sessionInfo.get("refreshToken").getAsJsonObject().get("token").getAsString());
        sessionRefreshBody.addProperty("enableAntiCsrf", false);

        JsonObject sessionRefreshResponse = HttpRequestForTesting.sendJsonPOSTRequest(process.getProcess(), "",
                "http://localhost:3567/recipe/session/refresh", sessionRefreshBody, 1000, 1000, null,
                Utils.getCdiVersion2_13ForTests(), "session");

        checkRefreshSessionResponse(sessionRefreshResponse, process, userId, userDataInJWT, grantPayload, false);
        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }

    @Test
    public void successOutputWithV2AccessToken() throws Exception {
        String[] args = { "../" };

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        String userId = "userId";
        JsonObject userDataInJWT = new JsonObject();
        userDataInJWT.addProperty("key", "value");
        JsonObject userDataInDatabase = new JsonObject();
        userDataInDatabase.addProperty("key", "value");

        JsonObject request = new JsonObject();
        request.addProperty("userId", userId);
        request.add("userDataInJWT", userDataInJWT);
        request.add("userDataInDatabase", userDataInDatabase);
        request.addProperty("enableAntiCsrf", false);

        JsonObject sessionInfo = HttpRequestForTesting.sendJsonPOSTRequest(process.getProcess(), "",
                "http://localhost:3567/recipe/session", request, 1000, 1000, null, Utils.getCdiVersion2_12ForTests(),
                "session");
        assertEquals(sessionInfo.get("status").getAsString(), "OK");

        JsonObject sessionRefreshBody = new JsonObject();

        sessionRefreshBody.addProperty("refreshToken",
                sessionInfo.get("refreshToken").getAsJsonObject().get("token").getAsString());
        sessionRefreshBody.addProperty("enableAntiCsrf", false);

        JsonObject sessionRefreshResponse = HttpRequestForTesting.sendJsonPOSTRequest(process.getProcess(), "",
                "http://localhost:3567/recipe/session/refresh", sessionRefreshBody, 1000, 1000, null,
                Utils.getCdiVersion2_13ForTests(), "session");

        checkRefreshSessionResponse(sessionRefreshResponse, process, userId, userDataInJWT, new JsonObject(), false);
        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }

    private static void checkRefreshSessionResponse(JsonObject response, TestingProcessManager.TestingProcess process,
            String userId, JsonObject userDataInJWT, JsonObject grantPayload, boolean hasAntiCsrf) {

        assertNotNull(response.get("session").getAsJsonObject().get("handle").getAsString());
        assertEquals(response.get("session").getAsJsonObject().get("userId").getAsString(), userId);
        assertEquals(response.get("session").getAsJsonObject().get("userDataInJWT").getAsJsonObject().toString(),
                userDataInJWT.toString());
        System.out.println(response.get("session"));
        if (grantPayload != null) {
            assertEquals(response.get("session").getAsJsonObject().get("grants").getAsJsonObject().toString(),
                    grantPayload.toString());
        } else {
            assertEquals(response.get("session").getAsJsonObject().get("grants").getAsJsonObject().toString(),
                    new JsonObject().toString());
        }
        assertEquals(response.get("session").getAsJsonObject().entrySet().size(), 4);

        assertTrue(response.get("accessToken").getAsJsonObject().has("token"));
        assertTrue(response.get("accessToken").getAsJsonObject().has("expiry"));
        assertTrue(response.get("accessToken").getAsJsonObject().has("createdTime"));
        assertEquals(response.get("accessToken").getAsJsonObject().entrySet().size(), 3);

        String token = response.get("accessToken").getAsJsonObject().get("token").getAsString();
        String[] splittedToken = token.split("\\.");
        JsonObject payload = (JsonObject) new JsonParser()
                .parse(io.supertokens.utils.Utils.convertFromBase64(splittedToken[1]));
        assertTrue(payload.has("userData"));
        if (grantPayload != null) {
            assertTrue(payload.has("grants"));
        } else {
            assertTrue(!payload.has("grants"));
        }

        assertTrue(response.get("refreshToken").getAsJsonObject().has("token"));
        assertTrue(response.get("refreshToken").getAsJsonObject().has("expiry"));
        assertTrue(response.get("refreshToken").getAsJsonObject().has("createdTime"));
        assertEquals(response.get("refreshToken").getAsJsonObject().entrySet().size(), 3);

        assertTrue(response.get("idRefreshToken").getAsJsonObject().has("token"));
        assertTrue(response.get("idRefreshToken").getAsJsonObject().has("expiry"));
        assertTrue(response.get("idRefreshToken").getAsJsonObject().has("createdTime"));

        assertEquals(response.get("idRefreshToken").getAsJsonObject().entrySet().size(), 3);

        assertEquals(response.has("antiCsrfToken"), hasAntiCsrf);

        assertEquals(response.entrySet().size(), hasAntiCsrf ? 6 : 5);
    }

}

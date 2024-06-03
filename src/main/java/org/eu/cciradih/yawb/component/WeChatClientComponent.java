package org.eu.cciradih.yawb.component;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.eu.cciradih.yawb.data.WeChatSendMsgTransfer;
import org.eu.cciradih.yawb.data.WeChatTransfer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeChatClientComponent {
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    public final String EXTSPAM = "Go8FCIkFEokFCggwMDAwMDAwMRAGGvAESySibk50w5Wb3uTl2c2h64jVVrV7gNs06GFlWplHQbY/5FfiO++1yH4ykC" +
            "yNPWKXmco+wfQzK5R98D3so7rJ5LmGFvBLjGceleySrc3SOf2Pc1gVehzJgODeS0lDL3/I/0S2SSE98YgKleq6Uqx6ndTy9yaL9qFxJL7eiA/R" +
            "3SEfTaW1SBoSITIu+EEkXff+Pv8NHOk7N57rcGk1w0ZzRrQDkXTOXFN2iHYIzAAZPIOY45Lsh+A4slpgnDiaOvRtlQYCt97nmPLuTipOJ8Qc5p" +
            "M7ZsOsAPPrCQL7nK0I7aPrFDF0q4ziUUKettzW8MrAaiVfmbD1/VkmLNVqqZVvBCtRblXb5FHmtS8FxnqCzYP4WFvz3T0TcrOqwLX1M/DQvcHa" +
            "GGw0B0y4bZMs7lVScGBFxMj3vbFi2SRKbKhaitxHfYHAOAa0X7/MSS0RNAjdwoyGHeOepXOKY+h3iHeqCvgOH6LOifdHf/1aaZNwSkGotYnYSc" +
            "W8Yx63LnSwba7+hESrtPa/huRmB9KWvMCKbDThL/nne14hnL277EDCSocPu3rOSYjuB9gKSOdVmWsj9Dxb/iZIe+S6AiG29Esm+/eUacSba0k8" +
            "wn5HhHg9d4tIcixrxveflc8vi2/wNQGVFNsGO6tB5WF0xf/plngOvQ1/ivGV/C1Qpdhzznh0ExAVJ6dwzNg7qIEBaw+BzTJTUuRcPk92Sn6QDn" +
            "2Pu3mpONaEumacjW4w6ipPnPw+g2TfywJjeEcpSZaP4Q3YV5HG8D6UjWA4GSkBKculWpdCMadx0usMomsSS/74QgpYqcPkmamB4nVv1JxczYIT" +
            "IqItIKjD35IGKAUwAA==";

    @SneakyThrows
    public WeChatTransfer getJsLogin() {
        long time = new Date().getTime();
        String redirectUri = "https://wx.qq.com/cgi-bin/mmwebwx-bin/webwxnewloginpage?mod=desktop";
        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("https")
                .host("login.wx.qq.com")
                .addPathSegment("jslogin")
                .addQueryParameter("appid", "wx782c26e4c19acffb")
                .addQueryParameter("fun", "new")
                .addQueryParameter("lang", "zh_CN")
                .addQueryParameter("redirect_uri", redirectUri)
                .addQueryParameter("_", String.valueOf(time))
                .build();

        return getWeChatTransfer(httpUrl);
    }

    public String getQrCodeUri(String uuid) {
        return new HttpUrl.Builder()
                .scheme("https")
                .host("login.wx.qq.com")
                .addPathSegment("qrcode")
                .addPathSegment(uuid)
                .build()
                .toString();
    }

    @SneakyThrows
    public WeChatTransfer getLoginUri(String uuid) {
        long time = new Date().getTime();
        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("https")
                .host("login.wx.qq.com")
                .addPathSegments("cgi-bin/mmwebwx-bin/login")
                .addQueryParameter("loginicon", "true")
                .addQueryParameter("r", String.valueOf(time))
                .addQueryParameter("tip", "0")
                .addQueryParameter("uuid", uuid)
                .addQueryParameter("_", String.valueOf(time))
                .build();

        return getWeChatTransfer(httpUrl);
    }

    private WeChatTransfer getWeChatTransfer(HttpUrl httpUrl) throws IOException {
        Request request = new Request.Builder()
                .get()
                .url(httpUrl)
                .build();

        try (Response response = this.okHttpClient.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new RuntimeException();
            }
            return this.parseStringResponse(responseBody.string());
        }
    }

    @SneakyThrows
    public WeChatTransfer getWebWxNewLoginPage(String redirectUri) {
        HttpUrl httpUrl = HttpUrl.get(redirectUri).newBuilder()
                .addQueryParameter("fun", "new")
                .build();

        Request request = new Request.Builder()
                .get()
                .url(httpUrl)
                .header("extspam", EXTSPAM)
                .header("version", "2.0.0")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        try (Response response = this.okHttpClient.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new RuntimeException();
            }
            WeChatTransfer weChatTransfer = this.parseXmlResponse(responseBody.string());
            weChatTransfer.setHost(httpUrl.host());

            long time = new Date().getTime();
            String deviceId = "e" + String.valueOf(time).repeat(2).substring(0, 15);
            weChatTransfer.setDeviceId(deviceId);
            return weChatTransfer;
        }
    }

    @SneakyThrows
    public WeChatTransfer postWebWxInit(WeChatTransfer weChatTransfer) {
        long time = new Date().getTime();
        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("https")
                .host(weChatTransfer.getHost())
                .addPathSegments("cgi-bin/mmwebwx-bin/webwxinit")
                .addQueryParameter("_", String.valueOf(time))
                .build();

        WeChatTransfer baseRequest = new WeChatTransfer();
        baseRequest.setBaseRequest(weChatTransfer);
        String content = this.objectMapper.writeValueAsString(baseRequest);
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(content, mediaType);

        return getWeChatTransfer(httpUrl, requestBody);
    }

    @SneakyThrows
    public WeChatTransfer postWebWxGetContact(WeChatTransfer weChatTransfer) {
        long time = new Date().getTime();
        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("https")
                .host(weChatTransfer.getHost())
                .addPathSegments("cgi-bin/mmwebwx-bin/webwxgetcontact")
                .addQueryParameter("pass_ticket", weChatTransfer.getPassTicket())
                .addQueryParameter("rr", String.valueOf(time))
                .addQueryParameter("skey", weChatTransfer.getSKey())
                .build();

        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create("{}", mediaType);

        return getWeChatTransfer(httpUrl, requestBody);
    }

    private WeChatTransfer getWeChatTransfer(HttpUrl httpUrl, RequestBody requestBody) throws IOException {
        Request request = new Request.Builder()
                .post(requestBody)
                .url(httpUrl)
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        try (Response response = this.okHttpClient.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new RuntimeException();
            }
            return this.objectMapper.readValue(responseBody.string(), WeChatTransfer.class);
        }
    }

    @SneakyThrows
    public WeChatTransfer getSyncCheck(WeChatTransfer weChatTransfer) {
        long time = new Date().getTime();
        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("https")
                .host(weChatTransfer.getHost())
                .addPathSegments("cgi-bin/mmwebwx-bin/synccheck")
                .addQueryParameter("deviceid", weChatTransfer.getDeviceId())
                .addQueryParameter("r", String.valueOf(time))
                .addQueryParameter("sid", weChatTransfer.getWxSid())
                .addQueryParameter("skey", weChatTransfer.getSKey())
                .addQueryParameter("synckey", weChatTransfer.getSyncKey().toString())
                .addQueryParameter("uin", weChatTransfer.getWxUin())
                .addQueryParameter("_", String.valueOf(time))
                .build();

        return getWeChatTransfer(httpUrl);
    }

    @SneakyThrows
    public WeChatTransfer postWebWxSync(WeChatTransfer weChatTransfer) {
        long time = new Date().getTime();
        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("https")
                .host(weChatTransfer.getHost())
                .addPathSegments("cgi-bin/mmwebwx-bin/webwxsync")
                .addQueryParameter("pass_ticket", weChatTransfer.getPassTicket())
                .addQueryParameter("rr", String.valueOf(time))
                .addQueryParameter("sid", weChatTransfer.getWxSid())
                .addQueryParameter("skey", weChatTransfer.getSKey())
                .build();

        WeChatTransfer baseRequest = new WeChatTransfer();
        baseRequest.setBaseRequest(weChatTransfer);
        baseRequest.setSyncKey(weChatTransfer.getSyncKey());
        baseRequest.setCheckSyncKey(weChatTransfer.getSyncKey());
        String content = this.objectMapper.writeValueAsString(baseRequest);
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(content, mediaType);

        Request request = new Request.Builder()
                .post(requestBody)
                .url(httpUrl)
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        try (Response response = this.okHttpClient.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new RuntimeException();
            }
            WeChatTransfer webWxSync = this.objectMapper.readValue(responseBody.string(), WeChatTransfer.class);
            weChatTransfer.setSyncKey(webWxSync.getSyncKey());
            weChatTransfer.setCheckSyncKey(webWxSync.getSyncKey());
            return webWxSync;
        }
    }

    @SneakyThrows
    public void postWebWxSendMsg(WeChatTransfer weChatTransfer) {
        long time = new Date().getTime() / 10000 * 10000;
        long random = new SecureRandom().nextLong(1000, 9999);
        String id = String.valueOf(time + random);
        WeChatSendMsgTransfer weChatSendMsgTransfer = WeChatSendMsgTransfer.builder()
                .localId(id)
                .clientMsgId(id)
                .fromUserName(weChatTransfer.getUser().getUserName())
                .toUserName(weChatTransfer.getToUserName())
                .type(1)
                .content(weChatTransfer.getContent())
                .build();
        WeChatTransfer baseRequest = new WeChatTransfer();
        baseRequest.setBaseRequest(weChatTransfer);
        baseRequest.setMsg(weChatSendMsgTransfer);
        baseRequest.setScene(0);
        String content = this.objectMapper.writeValueAsString(baseRequest);
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(content, mediaType);

        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("https")
                .host(weChatTransfer.getHost())
                .addPathSegments("cgi-bin/mmwebwx-bin/webwxsendmsg")
                .addQueryParameter("pass_ticket", weChatTransfer.getPassTicket())
                .addQueryParameter("lang", "zh_CN")
                .build();

        Request request = new Request.Builder()
                .post(requestBody)
                .url(httpUrl)
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        try (Response response = this.okHttpClient.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new RuntimeException();
            }
        }
    }

    @SneakyThrows
    private WeChatTransfer parseStringResponse(String response) {
        Map<String, String> responseMap = new HashMap<>();
        String[] kvsStrings = response.split(";", 2);
        Arrays.stream(kvsStrings).forEach(kvsString -> {
            String[] kvStrings = kvsString.split("=", 2);
            if (kvStrings.length > 1) {
                responseMap.put(kvStrings[0].trim(), kvStrings[1].trim());
            }
        });

        WeChatTransfer weChatTransfer = new WeChatTransfer();
        if (StringUtils.hasText(responseMap.get("window.QRLogin.code"))) {
            weChatTransfer.setCode(responseMap.get("window.QRLogin.code"));
        }
        if (StringUtils.hasText(responseMap.get("window.QRLogin.uuid"))) {
            String uuid = responseMap.get("window.QRLogin.uuid");
            uuid = uuid.substring(1, uuid.lastIndexOf("\""));
            weChatTransfer.setUuid(uuid);
        }
        if (StringUtils.hasText(responseMap.get("window.code"))) {
            weChatTransfer.setCode(responseMap.get("window.code"));
        }
        if (StringUtils.hasText(responseMap.get("window.userAvatar"))) {
            String userAvatar = responseMap.get("window.userAvatar");
            userAvatar = userAvatar.substring(1, userAvatar.lastIndexOf("'"));
            weChatTransfer.setUserAvatar(userAvatar);
        }
        if (StringUtils.hasText(responseMap.get("window.redirect_uri"))) {
            String redirectUri = responseMap.get("window.redirect_uri");
            redirectUri = redirectUri.substring(+1, redirectUri.lastIndexOf("\""));
            weChatTransfer.setRedirectUri(redirectUri);
        }
        if (StringUtils.hasText(responseMap.get("window.synccheck"))) {
            String syncCheckString = responseMap.get("window.synccheck");
            return this.objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
                    .readValue(syncCheckString, WeChatTransfer.class);
        }
        return weChatTransfer;
    }

    @SneakyThrows
    private WeChatTransfer parseXmlResponse(String response) {
        WeChatTransfer weChatTransfer = new WeChatTransfer();
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        Document document = documentBuilder.parse(byteArrayInputStream);

        NodeList errorList = document.getElementsByTagName("error");
        if (errorList.getLength() > 0) {
            Element error = (Element) errorList.item(0);
            NodeList sKeyList = error.getElementsByTagName("skey");
            if (sKeyList.getLength() > 0) {
                String sKey = sKeyList.item(0).getTextContent();
                weChatTransfer.setSKey(sKey);
            }
            NodeList wxSidList = error.getElementsByTagName("wxsid");
            if (wxSidList.getLength() > 0) {
                String wxSid = wxSidList.item(0).getTextContent();
                weChatTransfer.setWxSid(wxSid);
            }
            NodeList wxUinList = error.getElementsByTagName("wxuin");
            if (wxUinList.getLength() > 0) {
                String wxUin = wxUinList.item(0).getTextContent();
                weChatTransfer.setWxUin(wxUin);
            }
            NodeList passTicketList = error.getElementsByTagName("pass_ticket");
            if (passTicketList.getLength() > 0) {
                String passTicket = passTicketList.item(0).getTextContent();
                weChatTransfer.setPassTicket(passTicket);
            }
        }
        return weChatTransfer;
    }
}

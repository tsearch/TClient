package de.tsearch.tclient;

import de.tsearch.tclient.http.respone.LoginResponse;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class TClientInstance {
    private final Config config;

    protected Map<String, String> standardHeader;

    public TClientInstance(Config config) {
        this.config = config;
        if (config.isLoginCredentials()) {
            this.reLogin();
        } else {
            this.regenerateStandardHeader();
        }
    }

    /**
     * Try to login with credentials in config.
     *
     * @return if login is successful
     * @throws RuntimeException when login credentials is missing
     */
    public synchronized boolean reLogin() {
        if (this.config.isLoginCredentials()) {
            HttpResponse<LoginResponse> response = Unirest.post("https://id.twitch.tv/oauth2/token")
                    .queryString("client_id", this.config.getClientId())
                    .queryString("client_secret", this.config.getClientSecret())
                    .queryString("grant_type", "client_credentials")
                    .asObject(LoginResponse.class);

            if (response.isSuccess()) {
                LoginResponse responseBody = response.getBody();
                this.config.setAccessToken(responseBody.getAccessToken());
                this.config.setValidUntil(Instant.now().plus(responseBody.getExpiresIn(), ChronoUnit.SECONDS));
                this.regenerateStandardHeader();
                return true;
            } else {
                return false;
            }
        } else {
            throw new RuntimeException("Cannot login without login credentials!");
        }
    }

    /**
     * ReLogin if access token is expired
     */
    protected synchronized void reLoginIfNecessary() {
        if (this.config.getValidUntil().isBefore(Instant.now())) {
            reLogin();
        }
    }

    private void regenerateStandardHeader() {
        this.standardHeader = Map.of("Authorization", "Bearer " + this.config.getAccessToken(),
                "Client-ID", this.config.getClientId(),
                "Accept", "*/*",
                "User-Agent", "TSearch");
    }
}
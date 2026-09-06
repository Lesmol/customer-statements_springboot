package com.lvmp.customerstatements_springboot.config;

import com.lvmp.customerstatements_springboot.client.UserClient;
import com.lvmp.customerstatements_springboot.config.properties.UserServiceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class UserClientConfig {
    private final UserServiceProperties properties;

    @Bean
    public UserClient userClient() {
        RestClient restClient = RestClient.builder()
                .baseUrl(properties.uri())
                .requestFactory(buildRequestFactory(
                        properties.connectTimeout(),
                        properties.readTimeout())
                )
                .build();

        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(UserClient.class);
    }

    private JdkClientHttpRequestFactory buildRequestFactory(Duration connectTimeout, Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                        .connectTimeout(connectTimeout)
                        .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        return requestFactory;
    }
}

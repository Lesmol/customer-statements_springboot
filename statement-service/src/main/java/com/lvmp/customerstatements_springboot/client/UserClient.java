package com.lvmp.customerstatements_springboot.client;

import com.lvmp.customerstatements_springboot.model.response.UserView;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.UUID;

@HttpExchange("/internal/v1/users")
public interface UserClient {

    @GetExchange("/by-email/{email}")
    UserView getByEmail(@PathVariable String email);

    @GetExchange("/{id}")
    UserView getById(@PathVariable UUID id);
}

package com.softpro.ATH.Controller;

import com.softpro.ATH.Dto.ConsumerDto;
import com.softpro.ATH.Dto.ConsumerRegisterRequest;
import com.softpro.ATH.Service.ConsumerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/consumers")
public class ConsumerController {

    private final ConsumerService consumerService;

    public ConsumerController(ConsumerService consumerService) {
        this.consumerService = consumerService;
    }

    @PostMapping("/register")
    public ResponseEntity<ConsumerDto> register(
            @RequestBody ConsumerRegisterRequest request
    ) {

        ConsumerDto consumer = consumerService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(consumer);
    }
}
package com.softpro.ATH.Service;

import com.softpro.ATH.Dto.ConsumerDto;
import com.softpro.ATH.Dto.ConsumerRegisterRequest;
import com.softpro.ATH.Model.Consumer;
import com.softpro.ATH.Repository.ConsumerRepo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ConsumerService {

    private final ConsumerRepo consumerRepo;

    public ConsumerService(ConsumerRepo consumerRepo) {
        this.consumerRepo = consumerRepo;
    }

    public ConsumerDto register(ConsumerRegisterRequest request) {

        if (consumerRepo.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Consumer email already exists");
        }

        Consumer consumer = new Consumer();

        consumer.setName(request.getName());
        consumer.setEmail(request.getEmail());

        /*
         * TEMPORARY:
         * We are not connecting Consumer authentication yet.
         *
         * ATH currently does not contain Spring Security.
         * Authentication will be integrated later using the
         * existing ATH authentication architecture after we
         * inspect it carefully.
         */
        consumer.setPassword(request.getPassword());

        consumer.setContactNo(request.getContactNo());
        consumer.setAddress(request.getAddress());
        consumer.setCity(request.getCity());
        consumer.setState(request.getState());
        consumer.setPincode(request.getPincode());
        consumer.setLatitude(request.getLatitude());
        consumer.setLongitude(request.getLongitude());

        consumer.setStatus("ACTIVE");
        consumer.setRegDate(LocalDateTime.now());

        Consumer saved = consumerRepo.save(consumer);

        return convertToDto(saved);
    }

    private ConsumerDto convertToDto(Consumer consumer) {

        ConsumerDto dto = new ConsumerDto();

        dto.setId(consumer.getId());
        dto.setName(consumer.getName());
        dto.setEmail(consumer.getEmail());
        dto.setContactNo(consumer.getContactNo());
        dto.setAddress(consumer.getAddress());
        dto.setCity(consumer.getCity());
        dto.setState(consumer.getState());
        dto.setPincode(consumer.getPincode());
        dto.setLatitude(consumer.getLatitude());
        dto.setLongitude(consumer.getLongitude());
        dto.setStatus(consumer.getStatus());

        return dto;
    }
}
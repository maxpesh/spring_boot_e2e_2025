package com.example.service.adoptions;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.DirectChannelSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannelSpec;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@Configuration
class IntegrationConfiguration {
    static final String ADOPTIONS_CHANNEL_NAME = "outboundAdoptionsMessageChannel";
    static final String ADOPTIONS_NAME = "adoptions";

    @Bean
    Queue adoptionQueue() {
        return QueueBuilder.durable(ADOPTIONS_NAME).build();
    }

    @Bean
    Exchange adoptionsExchange() {
        return ExchangeBuilder.directExchange(ADOPTIONS_NAME).build();
    }

    @Bean
    Binding adoptionsBinding(Queue adoptionsQueue, Exchange adoptionsExchange) {
        return BindingBuilder.bind(adoptionsQueue).to(adoptionsExchange).with(ADOPTIONS_NAME).noargs();
    }

    @Bean(ADOPTIONS_CHANNEL_NAME)
    MessageChannelSpec<DirectChannelSpec, DirectChannel> outboundAdoptionsMessageChanel() {
        return MessageChannels.direct();
    }

    @Bean
    IntegrationFlow outboundAdoptionsIntegrationFlow(@Qualifier(ADOPTIONS_CHANNEL_NAME) MessageChannel msgChannel,
                                                     AmqpTemplate amqpTemplate) {
        return IntegrationFlow
                .from(msgChannel)
                .handle(Amqp.outboundAdapter(amqpTemplate)
                        .routingKey(ADOPTIONS_NAME)
                        .exchangeName(ADOPTIONS_NAME))
                .get();
    }
}

@Controller
class DogAdoptiognGrapgqlController {
    private final DogAdoptionService dogAdoptionService;

    DogAdoptiognGrapgqlController(DogAdoptionService dogAdoptionService) {
        this.dogAdoptionService = dogAdoptionService;
    }

    @QueryMapping
    Collection<Dog> dogs() {
        return dogAdoptionService.dogs();
    }
}

@Controller
@ResponseBody
class DogAdoptionHttpController {
    private final DogAdoptionService dogAdoptionService;

    DogAdoptionHttpController(DogAdoptionService dogAdoptionService) {
        this.dogAdoptionService = dogAdoptionService;
    }

    @GetMapping("/dogs")
    Collection<Dog> dogs() {
        return dogAdoptionService.dogs();
    }


    @GetMapping("/assistant")
    String assistant(@RequestParam String question) {
        return dogAdoptionService.assistant(question);
    }

    @PostMapping("/dogs/{dogId}/adoptions")
    void adopt(@PathVariable int dogId, @RequestParam String owner) {
        dogAdoptionService.adopt(dogId, owner);
    }
}

@Service
@Transactional
class DogAdoptionService {
    private final DogRepository dogRepo;
    private final ApplicationEventPublisher eventPublisher;

    DogAdoptionService(DogRepository dogRepo, ApplicationEventPublisher eventPublisher) {
        this.dogRepo = dogRepo;
        this.eventPublisher = eventPublisher;
    }

    void adopt(int dogId, String owner) {
        dogRepo.findById(dogId).ifPresent(dog -> {
            Dog updated = dogRepo.save(new Dog(dogId, dog.name(), dog.description(), owner));
            eventPublisher.publishEvent(new DogAdoptionEvent(dogId));
            System.out.println("updated " + updated);
        });
    }

    Collection<Dog> dogs() {
        return dogRepo.findAll();
    }

    String assistant(String question) {
        return null;
    }
}

record Dog(@Id int id, String name, String description, String owner) {
}

@Repository
interface DogRepository extends ListCrudRepository<Dog, Integer> {
}
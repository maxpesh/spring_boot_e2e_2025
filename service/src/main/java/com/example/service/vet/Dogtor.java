package com.example.service.vet;

import com.example.service.adoptions.DogAdoptionEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
class Dogtor {
    @ApplicationModuleListener
    void checkup(DogAdoptionEvent dogAdoptionEvent) throws InterruptedException {
        TimeUnit.SECONDS.sleep(5);
        System.out.println("checking up on " + dogAdoptionEvent);
    }
}

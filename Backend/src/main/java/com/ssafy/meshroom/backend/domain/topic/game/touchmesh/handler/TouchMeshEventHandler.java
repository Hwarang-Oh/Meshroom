package com.ssafy.meshroom.backend.domain.topic.game.touchmesh.handler;

import com.ssafy.meshroom.backend.domain.topic.game.touchmesh.dto.TouchDto;
import com.ssafy.meshroom.backend.domain.topic.game.touchmesh.service.KafkaTouchMeshProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class TouchMeshEventHandler {

    @Autowired
    private KafkaTouchMeshProducer kafkaTouchMeshProducer;

    @MessageMapping("/game/touch")
    public void handleTouchEvent(TouchDto touchEvent) {
        System.out.println(touchEvent.toString());
        kafkaTouchMeshProducer.sendEvent(touchEvent);


    }

}

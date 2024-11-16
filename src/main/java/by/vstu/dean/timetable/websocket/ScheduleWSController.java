package by.vstu.dean.timetable.websocket;

import by.vstu.dean.core.anotations.WebSocketTopic;
import by.vstu.dean.core.websocket.WSControllerManager;
import by.vstu.dean.core.websocket.controllers.BaseWSController;
import by.vstu.dean.core.websocket.enums.EPacketType;
import by.vstu.dean.core.websocket.packets.BaseWSPacket;
import by.vstu.dean.timetable.dto.LessonDTO;
import by.vstu.dean.timetable.dto.RoomDTO;
import by.vstu.dean.timetable.dto.mapper.LessonMapper;
import by.vstu.dean.timetable.models.LessonModel;
import by.vstu.dean.timetable.repo.LessonModelRepository;
import by.vstu.dean.timetable.services.LessonService;
import by.vstu.dean.timetable.services.common.RoomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@Controller
@MessageMapping("/schedule/")
@WebSocketTopic(value = "/topic/schedule/", dtoClass = LessonDTO.class, modelClass = LessonModel.class)
@Slf4j
public class ScheduleWSController extends BaseWSController<LessonDTO, LessonModel, LessonMapper, LessonModelRepository, LessonService> {

    private final RoomService roomService;

    public ScheduleWSController(SimpMessagingTemplate messagingTemplate, LessonService service, LessonMapper mapper, WSControllerManager controllerManager, RoomService roomService) {
        super(messagingTemplate, service, mapper, controllerManager);
        this.roomService = roomService;
    }

    @Override
    protected void sendUpdate(StompHeaderAccessor accessor, Object data, EPacketType packetType) {

        if(data instanceof LessonModel) {
            this.mapLesson(data).ifPresent(room -> super.sendUpdate(accessor, room, packetType));
            return;
        }
        log.info("Sending raw data to WS {}", data);
        super.sendUpdate(accessor, data, packetType);
    }

    private Optional<RoomDTO> mapLesson(Object data) {
        if(!(data instanceof LessonModel lessonModel))
            return Optional.empty();
        return Optional.of(this.roomService.convertToDto(this.mapper.toDto(lessonModel)));
    }

//    @Override
//    public void onUpdate(Object data) {
//        this.mapLesson(data).ifPresent(room -> this.sendUpdate(null, room, EPacketType.UPDATE));
//    }
//
//    @Override
//    public void onCreate(Object data) {
//        this.mapLesson(data).ifPresent(room -> this.sendUpdate(null, room, EPacketType.CREATE));
//    }
//
//    @Override
//    public void onDelete(Object data) {
//        this.mapLesson(data).ifPresent(room -> this.sendUpdate(null, room, EPacketType.DELETE));
//    }
}

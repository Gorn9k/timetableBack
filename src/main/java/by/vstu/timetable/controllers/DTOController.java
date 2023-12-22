package by.vstu.timetable.controllers;

import by.vstu.dean.controllers.common.BaseController;
import by.vstu.dean.dto.BaseDTO;
import by.vstu.dean.services.BaseService;

public abstract class DTOController<D extends BaseDTO> extends BaseController {

    public DTOController(BaseService service) {
        super(service);
    }
}

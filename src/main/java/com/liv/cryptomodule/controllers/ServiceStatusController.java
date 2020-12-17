package com.liv.cryptomodule.controllers;

import com.liv.cryptomodule.util.SQLDatabaseConnection;
import com.liv.cryptomodule.dto.ServiceStatusDTO;
import com.liv.cryptomodule.dto.UserServicesDTO;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(value = "/services")
public class ServiceStatusController {

    @PostMapping("/addServiceStatus")
    public void addServiceStatus(@RequestBody ServiceStatusDTO serviceStatus) {
        SQLDatabaseConnection.addServiceStatus(serviceStatus);
    }
    @PostMapping("/getUserServices")
    public ArrayList<ServiceStatusDTO> getUserServices(@RequestBody UserServicesDTO user) {
        return SQLDatabaseConnection.getUserServices(user);
    }
}

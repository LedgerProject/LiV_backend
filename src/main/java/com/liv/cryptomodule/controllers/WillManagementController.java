package com.liv.cryptomodule.controllers;

import com.liv.cryptomodule.dto.CreateWillDTO;
import com.liv.cryptomodule.dto.KYC;
import com.liv.cryptomodule.dto.WillBasicDTO;
import com.liv.cryptomodule.util.SQLDatabaseConnection;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(value = "/wills")
public class WillManagementController {

    @PostMapping("/create-will")
    public int createWill(@RequestBody CreateWillDTO will) throws IOException {
        return SQLDatabaseConnection.createWill(will);
    }
    @GetMapping("/get-will-requests")
    public ArrayList<WillBasicDTO> getWills() throws IOException {
        return SQLDatabaseConnection.getWills();
    }
    @GetMapping("/approve/{willId:.+}")
    public void approveWill(@PathVariable String willId) throws IOException {
        SQLDatabaseConnection.approveWill(willId);
    }
    @GetMapping("/reject/{willId:.+}")
    public void rejectWill(@PathVariable String willId) throws IOException {
        SQLDatabaseConnection.rejectWill(willId);
    }
    @GetMapping("/get-will-details/{willId:.+}")
    public KYC getWillDetails(@PathVariable String willId) throws IOException {
        return SQLDatabaseConnection.getWillDetails(willId);
    }
}
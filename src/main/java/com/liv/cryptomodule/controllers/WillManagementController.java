package com.liv.cryptomodule.controllers;

import com.liv.cryptomodule.dto.CreateWillDTO;
import com.liv.cryptomodule.dto.PageAndFilterDTO;
import com.liv.cryptomodule.dto.WillRequestDTO;
import com.liv.cryptomodule.dto.WillRequestIdDTO;
import com.liv.cryptomodule.exception.WrongPageOrderException;
import com.liv.cryptomodule.util.SQLDatabaseConnection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(value = "/will-requests")
public class WillManagementController {

    @PostMapping(value = "/create", consumes = {MediaType.APPLICATION_JSON_VALUE,
            MediaType.MULTIPART_FORM_DATA_VALUE})
    public int createWill(@RequestPart(value = "will", required = true) CreateWillDTO will, @RequestPart(value = "file", required = true) MultipartFile file) throws IOException {
        return SQLDatabaseConnection.createWill(will, file);
    }

    @GetMapping(value = "/")
    public ArrayList<WillRequestDTO> getWills(@RequestBody(required = false) PageAndFilterDTO pageAndFilterDTO) throws IOException, WrongPageOrderException {
        return SQLDatabaseConnection.getWillRequests(pageAndFilterDTO);
    }

    @PostMapping("/approve")
    public void approveWill(@RequestBody List<WillRequestIdDTO> willIdList) throws IOException {
        SQLDatabaseConnection.approveWill(willIdList);
    }

    @PostMapping("/reject")
    public void rejectWill(@RequestBody List<WillRequestIdDTO> willIdList) throws IOException {
        SQLDatabaseConnection.rejectWill(willIdList);
    }

    @GetMapping("/{willId:.+}")
    public WillRequestDTO getWillDetails(@PathVariable String willId) throws IOException {
        return SQLDatabaseConnection.getWillRequestDetails(willId);
    }

    // TODO: 15.02.2021 /notify get id will

    @GetMapping("/notify/{willRequest:.+}")
    public void sendTestEmail(@PathVariable String willRequest) throws IOException, IllegalStateException {
        SQLDatabaseConnection.sendNotification(willRequest);
        /*try {
            EmailService.sendEmail(willRequest, "kostyanich7@gmail.com",
                    "TestSubject", MailContentBuilder.generateMailContent(
                            new EmailPayload("Test Mail",
                                    "kostyanich7@gmail.com", //noreply что-то там)
                                    "Test",// -
                                    "Testovskij",// -
                                    "kostiantyn.nechvolod@gmail.com",
                                    "Konstantin",
                                    "Nechvolod",
                                    "http://18.192.22.193:8080/ipfs/QmaNxbQNrJdLzzd8CKRutBjMZ6GXRjvuPepLuNSsfdeJRJ")
                    ));
        } catch (NullPointerException e) {
            throw new IllegalStateException("JSON payload structure is incorrect");
        }*/
    }
}
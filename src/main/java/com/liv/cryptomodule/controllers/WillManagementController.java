package com.liv.cryptomodule.controllers;

import com.liv.cryptomodule.dto.*;
import com.liv.cryptomodule.exception.IPFSException;
import com.liv.cryptomodule.exception.UserNotFoundException;
import com.liv.cryptomodule.exception.WrongPageOrderException;
import com.liv.cryptomodule.util.SQLDatabaseConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(value = "/will-requests")
public class WillManagementController {

    @PostMapping(value = "/create", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<String> createWill(@RequestPart(value = "sender_id", required = true) String senderId, @RequestPart(value = "recipient_email", required = true) String recipientEmail, @RequestPart(value = "file", required = true) MultipartFile file) throws IOException, UserNotFoundException, SQLException, ClassNotFoundException {
        try {
            SQLDatabaseConnection.createWill(senderId, recipientEmail, file);
            return new ResponseEntity<>("Will created", HttpStatus.OK);
        } catch (IPFSException e) {
            e.printStackTrace();
            return new ResponseEntity<>("Could not connect to IPFS node", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (NoSuchAlgorithmException | SignatureException e) {
            return new ResponseEntity<>("Could not sign the request", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (InvalidKeyException e) {
            return new ResponseEntity<>("Signature key is invalid", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value = "/")
    public ArrayList<WillRequestResponseDTO> getWills(
            @RequestParam(value = "pageOrder", required = false) String pageOrder,
            @RequestParam(value = "pageLimit", required = false) Integer pageLimit,
            @RequestParam(value = "ownerId", required = false) String ownerId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "recipientId", required = false) String recipientId) throws IOException, WrongPageOrderException {

        PageDTO pageDTO = null;
        FilterDTO filterDTO = null;
        PageAndFilterDTO pageAndFilterDTO = null;

        if(pageOrder != null || pageLimit != null){
            pageDTO = new PageDTO(
                    pageOrder,
                    pageLimit
            );
        }

        if(ownerId != null || status != null || recipientId != null){
            filterDTO = new FilterDTO(
                    ownerId,
                    status,
                    recipientId
            );
        }

        if(pageDTO != null || filterDTO != null){
            pageAndFilterDTO = new PageAndFilterDTO(
                    pageDTO,
                    filterDTO
            );
        }


        return SQLDatabaseConnection.getWillRequests(
                pageAndFilterDTO
        );
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
    public WillRequestResponseDTO getWillDetails(@PathVariable String willId) throws IOException {
        return SQLDatabaseConnection.getWillRequestDetails(willId);
    }

    // TODO: 15.02.2021 /notify get id will

    @GetMapping("/notify/{willRequest:.+}")
    public void confirmDeath(@PathVariable String willRequest) throws IOException {
        SQLDatabaseConnection.confirmDeath(willRequest);
    }
    @GetMapping("/release/{willRequest:.+}")
    public void releaseWill(@PathVariable String willRequest) throws IOException, IllegalStateException {
        SQLDatabaseConnection.sendNotification(willRequest);
    }
    @DeleteMapping("/delete/{willRequest:.+}")
    public void deleteWill(@PathVariable String willRequest) throws IOException, IllegalStateException {
        SQLDatabaseConnection.deleteWill(willRequest);
    }
}
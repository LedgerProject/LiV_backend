package com.liv.cryptomodule.dto

data class UserModelDTO(
        val email: String,
        val first_name: String,
        val middle_name: String,
        val last_name: String,
        val address: String,
        val passport_number: String
) {
    constructor(email: String, kycDTO: KycDTO) : this(
            email = email,
            first_name = kycDTO.firstName,
            middle_name = kycDTO.middleName,
            last_name = kycDTO.lastName,
            address = kycDTO.address,
            passport_number = kycDTO.passportID
    )
}
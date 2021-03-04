package com.liv.cryptomodule.dto

data class UserModelDTO(
        var id: String = "",
        var email: String = "",
        var did: String? = "",
        var firstName: String = "",
        var middleName: String = "",
        var lastName: String = "",
        var address: String = "",
        var passportNumber: String = ""
) {
    constructor(id: String, email: String, kycDTO: KycDTO) : this(
            id = id,
            email = email,
            firstName = kycDTO.firstName,
            middleName = kycDTO.middleName,
            lastName = kycDTO.lastName,
            address = kycDTO.address,
            passportNumber = kycDTO.passportID
    )
}
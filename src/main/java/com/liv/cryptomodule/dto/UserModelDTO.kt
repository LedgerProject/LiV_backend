package com.liv.cryptomodule.dto

data class UserModelDTO(
        var id: String = "",
        var email: String = "",
        var did: String? = "",
        var firstName: String = "",
        var lastName: String = "",
        var secondName: String = "",
        var address: String = "",
        var nif: String = "",
        var birthday: String = ""
) {
    constructor(id: String, email: String, kycDTO: KycDTO) : this(
            id = id,
            email = email,
            firstName = kycDTO.firstName,
            lastName = kycDTO.lastName,
            secondName = kycDTO.secondName,
            address = kycDTO.address,
            nif = kycDTO.nif,
            birthday = kycDTO.birthday
    )
}
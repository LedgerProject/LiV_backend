package com.liv.cryptomodule.dto

data class WillRequestResponseDTO(
        var id: String = "",
        var statusId: String = "",
        var documentHash: String = "",
        var documentLink: String = "",
        var creator: UserModelDTO? = null,
        var recipient: UserModelDTO? = null
)
package com.liv.cryptomodule.util

import com.liv.cryptomodule.exception.UserNotFoundException
import java.io.IOException
import java.sql.SQLException

@Throws(IOException::class, UserNotFoundException::class, SQLException::class, ClassNotFoundException::class)
fun SQLDatabaseConnection.createWillKt():Int {
    // no way to use inner methods like saveDocumentToIpfs, executeUpdateToDB without exposing.
    // no way to refactor this explicit method and use as drop in replacement inside of SQLDatabaseConnection
    return -1
}
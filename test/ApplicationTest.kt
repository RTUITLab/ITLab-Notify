package ru.rtuitlab

import io.ktor.http.*
import kotlin.test.*
import io.ktor.server.testing.*
import module

class ApplicationTest {
    @Test
    fun testRoot() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("Server is online", response.content)
            }
        }
    }
}
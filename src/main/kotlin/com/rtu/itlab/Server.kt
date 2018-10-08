package com.rtu.itlab

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.rtu.itlab.database.DB
import com.rtu.itlab.responses.*
import com.rtu.itlab.utils.getProp
import io.ktor.application.*
import io.ktor.features.ContentNegotiation
import io.ktor.gson.*
import io.ktor.request.receiveStream
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.*
import java.io.InputStreamReader

fun Application.main() {
/*
    val transportClient = HttpTransportClient.getInstance()
    val vk = VkApiClient(transportClient)*/
    val properties = getProp()
    val db = DB("1230",null,null)

    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
        }
    }

    routing {

//        post("/") {
//
//            val tmp: JsonObject? = Gson().
//                    fromJson(InputStreamReader(call.receiveStream(),"UTF-8"), JsonObject::class.java)
//
//            val type: String? = tmp?.get("type")?.asString
//
//            val body: String? = tmp?.getAsJsonObject("object")?.get("text")?.asString
//
//            val user_id = tmp?.getAsJsonObject("object")?.get("from_id")?.asInt
//
//            val actor = GroupActor(properties.getProperty("group.id").toInt(), properties.getProperty("group.accessToken"))
//
//            if (type.equals("message_new")) {
//                vk.messages()
//                        .send(actor)
//                        .userId(user_id)
//                        .message("DmRomanov Server: Привет! $body")
//                        .execute()
//                call.respond("ok")
//            } else {
//                if (type.equals("confirmation")) call.respondText(properties.getProperty("server.response"))
//                else {
//                    call.respondText("ok")
//                }
//            }
//        }

        get("/") { call.respondText { "It's OK, just Wrong" } }

        post("/person/add"){
            val tmp: JsonObject? = Gson().fromJson(call.receiveText(), JsonObject::class.java)
            db.addPerson(tmp!!)
            call.respond("ok")
        }

        get("/persons/get"){
            //val tmp: JsonObject? = Gson().fromJson(InputStreamReader(call.receiveStream(),"UTF-8"), JsonObject::class.java)
            db.getAllPersons()
            call.respond("ok")
        }

        post("person/update/value"){
            //val tmp: JsonObject? = Gson().fromJson(InputStreamReader(call.receiveStream(),"UTF-8"), JsonObject::class.java)
            db.getAllPersons()
            call.respond("ok")
        }


        post("/"){
            val tmp: JsonObject? = Gson().fromJson(InputStreamReader(call.receiveStream(),"UTF-8"), JsonObject::class.java)
            if (tmp?.get("type")?.asString.equals("confirmation")) call.respondText { properties.getProperty("server.response") }
            else call.respond("ok")
        }

        post("/bot/equipment/added"){
            val tmp: JsonObject? = Gson().fromJson(InputStreamReader(call.receiveStream(),"UTF-8"), JsonObject::class.java)
            BotEquipmentAdded(tmp).send()
            call.respond("ok")
        }

        post("/bot/event/change"){
            val tmp: JsonObject? = Gson().fromJson(InputStreamReader(call.receiveStream(),"UTF-8"), JsonObject::class.java)
            BotEventChange(tmp).send()
            call.respond("ok")
        }

        post("/bot/event/deleted"){
            val tmp: JsonObject? = Gson().fromJson(InputStreamReader(call.receiveStream(),"UTF-8"), JsonObject::class.java)
            BotEventDeleted(tmp).send()
            call.respond("ok")
        }

        post("/bot/event/reminder"){
            val tmp: JsonObject? = Gson().fromJson(InputStreamReader(call.receiveStream(),"UTF-8"), JsonObject::class.java)
            BotEventReminder(tmp).send()
            call.respond("ok")
        }

        post("/bot/newevent"){
            val tmp: JsonObject? = Gson().fromJson(InputStreamReader(call.receiveStream(),"UTF-8"), JsonObject::class.java)
            notifyAboutNewEvent(tmp)
            call.respond("ok")
        }

        post("/bot/event/invite"){
            val tmp: JsonObject? = Gson().fromJson(InputStreamReader(call.receiveStream(),"UTF-8"), JsonObject::class.java)
            notifyAboutEventInvite(tmp)
            call.respond("ok")
        }

        post("/bot/event/confirm"){
            val tmp: JsonObject? = Gson().fromJson(InputStreamReader(call.receiveStream(),"UTF-8"), JsonObject::class.java)
            notifyAboutEventConfirm(tmp)
            call.respond("ok")
        }

        post("/bot/event/freeplace"){
            val tmp: JsonObject? = Gson().fromJson(InputStreamReader(call.receiveStream(),"UTF-8"), JsonObject::class.java)
            notifyAboutEventFreePlace(tmp)
            call.respond("ok")
        }

        post("/bot/event/deletefromevent"){
            val tmp: JsonObject? = Gson().fromJson(InputStreamReader(call.receiveStream(),"UTF-8"), JsonObject::class.java)
            notifyAboutDeleteFromEvent(tmp)
            call.respond("ok")
        }



    }
}

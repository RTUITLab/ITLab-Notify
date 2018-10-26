package com.rtu.itlab.responses

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.gson.responseObject
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.rtu.itlab.database.DBClient
import com.rtu.itlab.utils.ServerResponseJson
import com.rtu.itlab.utils.UserCard
import com.vk.api.sdk.client.actors.GroupActor

class GetVkToken(tmp: JsonObject?, db: DBClient) : ResponseHandler(db){
    private val vkId = tmp!!.getAsJsonObject("object").get("from_id").asInt
    private val token: String = tmp!!.getAsJsonObject("object").get("text").asString
    //private val actor = GroupActor(config.getInt("group.id"), config.getString("group.accessToken"))

    override fun send() {
        if (token.startsWith("L:")) {
            Fuel.post(config.getString("apiserver.host") + "/api/account/property/vk")
                    .body(Gson().toJson(UserCard(token.substringAfter("L:"), vkId)))
                    .header("Content-Type" to "application/json", "Authorization" to config.getString("apiserver.accessToken"))
                    .responseObject<ServerResponseJson> {_, _, result ->
                        //println("${result.get()} ${response.responseMessage}")
                        //println("${result.component1()} HERE 2")
                        //println("${result.toString()} HERE 3")
                        when {
                            result.get().statusCode==1 -> {
                                vk.messages()
                                        .send(actor)
                                        .userId(vkId)
                                        .message("Поздравляем, ваша учетня запись прикреплена")
                                        .execute()
                                //result.get().data.vkId = vkId
                                db!!.addPerson(result.get().data.copy(vkId = vkId.toString()))

                            }
                            result.get().statusCode==26 -> vk.messages()
                                    .send(actor)
                                    .userId(vkId)
                                    .message("Проверьте правильность написания кода")
                                    .execute()
                            else -> vk.messages()
                                    .send(actor)
                                    .userId(vkId)
                                    .message("Что-то пошло не так")
                                    .execute()
                        }
                    }
        } else {
            vk.messages()
                    .send(actor)
                    .userId(vkId)
                    .message("Если вы пытались прислать код для верификация, то вы сделали это как-то не так\nЯ не веду бесед с незнакомцами\nПроверьте правильность написания кода")
                    .execute()
        }
    }
}

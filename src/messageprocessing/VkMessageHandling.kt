package messageprocessing

import bot.keyboard.getKeyboardForCurrentPerson
import bot.keyboard.keyboard
import com.google.gson.JsonObject
import bot.BotCommands
import database.HibernateUtil
import database.schema.UserSettings
import emailsender.*
import org.slf4j.LoggerFactory
import messageprocessing.responses.event.Event
import utils.Config
import workwithapi.RequestsToServerApi

class VKMessageHandling(private val requestsToServerApi: RequestsToServerApi) : Handler() {

    private val logger = LoggerFactory.getLogger(this.javaClass.name)

    private var email: String? = null

    private var id: String? = null

    private var keyboard = "{\"buttons\":[],\"one_time\":true}"


    override fun sendEmail(destinationEmails: Set<String>, event: Event?) {
        val html = HtmlEmail()

        when (val apiUrl = Config().loadPath("apiserver.host")) {
            null -> logger.error("There is no path 'apiserver.host' in the config file")
            else -> html.run {
                changeDescription(
                    "<p>Ваш аккаунт сайта $apiUrl был привязан к системе уведомлений</p>" +
                            "<p>Управлять(отключать или включать) уведомлениями вы можете путем общения с ботом RTU IT Lab</p>"
                )
                changeUrl(apiUrl)
            }
        }

        html.changeTitle("Уведомление об успешном подключении системы уведомлений")

        val emailConfiguration = loadConfigurationsForEmail()

        if (emailConfiguration.email != null && emailConfiguration.password != null &&
            emailConfiguration.subject != null && emailConfiguration.port != null &&
            emailConfiguration.host != null
        ) {

            if (html.getHtmlString().isNotBlank()) {

                sendMail(
                    UserMail(emailConfiguration.email, emailConfiguration.password),
                    MailMessage(
                        emailConfiguration.subject,
                        html.getHtmlString().replace("Перейти к событию", "Перейти на сайт")
                    ),
                    HostMail(emailConfiguration.port, emailConfiguration.host),
                    destinationEmails
                )
            } else {
                logger.error("Html is empty or blank. Can't send message to users!")
            }

        } else {
            logger.error("Check mail.email, mail.password, mail.subject, mail.port, mail.host in config file")
        }
    }

    override fun sendVk(message: String?, vkId: String?) {
        if (!message.isNullOrEmpty() && !vkId.isNullOrEmpty()) {
            vk.messages()
                .send(actor)
                .userId(vkId.toInt())
                .message(message)
                .keyboard(keyboard)
                .execute()

        }
    }

    private fun getKeyboardJson(vkId: String, databaseConnection: HibernateUtil): String {
        var result = ""

        val keyboardClass = getKeyboardForCurrentPerson(requestsToServerApi, vkId, databaseConnection)

        if (keyboardClass.lines.size > 0)
            result = keyboardClass.getKeyboardJson().toString()

        return if (result.isNotBlank())
            result
        else
            "{\"buttons\":[],\"one_time\":true}"
    }

    override fun process(inputJson: JsonObject?, databaseConnection: HibernateUtil) {

        val vkId = inputJson?.getAsJsonObject("object")?.get("from_id")?.asString
        val messageText = inputJson?.getAsJsonObject("object")?.get("text")?.asString

        var userModel = if (!vkId.isNullOrEmpty())
            requestsToServerApi.getUserModelByVkId(vkId)
        else
            null

        val message = if (userModel != null && !messageText.isNullOrEmpty() && !vkId.isNullOrEmpty()) {
            id = userModel.id
            email = userModel.email
            if (databaseConnection.isUserInDatabase(id!!)) {
                keyboard = getKeyboardJson(vkId, databaseConnection)

                if (messageText.startsWith("L:"))
                    "Вы уже авторизованы в этом сервисе &#10084;"
                else {
                    val msg = when (BotCommands.getEnumClassByCommandText(messageText)) {

                        BotCommands.UnSubscribeEmail -> unSubscribe("email", databaseConnection)

                        BotCommands.UnSubscribeVk -> unSubscribe("vk", databaseConnection)

                        BotCommands.SubscribeEmail -> subscribe("email", databaseConnection)

                        BotCommands.SubscribeVk -> subscribe("vk", databaseConnection)

                        BotCommands.DeleteFromNotifyCenter -> deleteFromNotify(databaseConnection)

                        BotCommands.Help -> {
                            var result = "Комманды, которые я знаю: \n"
                            BotCommands.values().forEach {
                                if (it.commandText != "/help")
                                    result += it.commandText + "\n"
                            }
                            result
                        }

                        null ->
                            "Я вас не понимаю, то, что я понимаю вы можете узнать написав комманду \"/help\""

                    }
                    keyboard = getKeyboardJson(vkId, databaseConnection)
                    msg
                }
            } else {
                val res = databaseConnection.addEntity(
                    UserSettings(
                        id,
                        vkNotification = true,
                        emailNotification = true
                    )
                )

                if (res) {
                    keyboard = getKeyboardJson(vkId, databaseConnection)
                    "Вы добавлены в базу данных рассылки &#128519;"
                } else {
                    keyboard = "{\"buttons\":[],\"one_time\":true}"
                    "Ранее вы уже добавляли vk id на сайт, но произошла ошибка " +
                            "с добавлением вас в базу данных &#128546;"
                }
            }
        } else if (!messageText.isNullOrEmpty()) {
            if (messageText.startsWith("L:")) {
                userModel = requestsToServerApi.sendTokenToServerForAccess(messageText, vkId!!)

                if (userModel != null) {
                    id = userModel.id
                    email = userModel.email
                }

                if (id != null && email != null) {
                    val res = databaseConnection.addEntity(
                        UserSettings(
                            id,
                            vkNotification = true,
                            emailNotification = true
                        )
                    )
                    if (res) {
                        keyboard = getKeyboardJson(vkId, databaseConnection)
                        sendEmail(setOf(email!!))
                        "Поздравляем!, вы авторизовались в этом сервисе &#128293;&#128293;&#128293;"
                    } else {
                        keyboard = "{\"buttons\":[],\"one_time\":true}"
                        "Произошла ошибка во время добавления вас в базу данных &#128546;"
                    }
                } else {
                    keyboard = "{\"buttons\":[],\"one_time\":true}"
                    "Произошла ошибка во время вашей авторизации. Возможно не верный код авторизации &#128546;"
                }
            } else {
                keyboard = "{\"buttons\":[],\"one_time\":true}"
                "Я не понимаю вас. Возможно, для начала вам нужно " +
                        "авторизоваться в сервисе"
            }
        } else {
            null
        }
        sendVk(message, vkId)
    }

    private fun deleteFromNotify(databaseConnection: HibernateUtil): String {
        return if (!id.isNullOrBlank() &&
            databaseConnection.deleteEntities(id!!, UserSettings())
        ) {
            "Ваши эксклюзивные данные были удалины из базы данных данного сервиса"
        } else {
            "Произошла ошибка отвязки вашего аккаунта от данного сервиса"
        }
    }

    private fun unSubscribe(typeNotice: String, databaseConnection: HibernateUtil): String {

        val personInfo = if (!id.isNullOrBlank())
            databaseConnection.getEntityById(id!!, UserSettings())
        else
            null

        return if (personInfo != null) {
            val result = when (typeNotice) {
                "vk" -> {
                    val newPersonInfo = personInfo.copy(vkNotification = false)
                    databaseConnection.updateEntity(newPersonInfo)
                }
                "email" -> {
                    val newPersonInfo = personInfo.copy(emailNotification = false)
                    databaseConnection.updateEntity(newPersonInfo)
                }
                else -> false
            }
            if (result)
                "Вы успешно отписаны от $typeNotice рассылки!"
            else
                "Произошла ошибка при обновлении ваших данных при отписке от $typeNotice рассылки"
        } else
            "Произошла ошибка при получении ваших данных из базы данных"

    }

    private fun subscribe(typeNotice: String, databaseConnection: HibernateUtil): String {

        val personInfo = if (!id.isNullOrBlank())
            databaseConnection.getEntityById(id!!, UserSettings())
        else
            null

        return if (personInfo != null) {
            val result = when (typeNotice) {
                "vk" -> {
                    val newPersonInfo = personInfo.copy(vkNotification = true)
                    databaseConnection.updateEntity(newPersonInfo)
                }
                "email" -> {
                    val newPersonInfo = personInfo.copy(emailNotification = true)
                    databaseConnection.updateEntity(newPersonInfo)
                }
                else -> false
            }
            if (result)
                "Вы успешно подписаны на $typeNotice рассылки!"
            else
                "Произошла ошибка при обновлении ваших данных при отписке от $typeNotice рассылки"
        } else
            "Произошла ошибка при получении ваших данных из базы данных"
    }

}
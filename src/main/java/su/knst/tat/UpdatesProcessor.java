package su.knst.tat;

import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import su.knst.tat.event.chat.*;
import su.knst.tat.event.global.NewPollEvent;
import su.knst.tat.event.user.ChosenInlineResultEvent;
import su.knst.tat.event.user.InlineQueryEvent;
import su.knst.tat.event.user.PreCheckoutQueryEvent;
import su.knst.tat.event.user.ShippingQueryEvent;
import su.knst.tat.handlers.ChatHandler;
import su.knst.tat.handlers.GlobalHandler;
import su.knst.tat.handlers.UserHandler;

import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

public class UpdatesProcessor implements UpdatesListener {
    protected HashMap<Long, ChatHandler> chats = new HashMap<>();
    protected HashMap<Long, UserHandler> users = new HashMap<>();
    protected GlobalHandler globalHandler;

    protected Function<Long, ? extends ChatHandler> chatHandlerGenerator;
    protected Function<Long, ? extends UserHandler> userHandlerGenerator;

    protected Function<Update, Boolean> updateValidator;

    public UpdatesProcessor(GlobalHandler globalHandler,
                            Function<Long, ? extends ChatHandler> chatHandlerGenerator,
                            Function<Long, ? extends UserHandler> userHandlerGenerator) {
        this.globalHandler = globalHandler;
        this.chatHandlerGenerator = chatHandlerGenerator;
        this.userHandlerGenerator = userHandlerGenerator;

        globalHandler.start();
    }

    protected ChatHandler getOrCreateChatHandler(long chatId) {
        if (!chats.containsKey(chatId)) {
            ChatHandler handler = chatHandlerGenerator.apply(chatId);
            handler.start();

            chats.put(chatId, handler);

            return handler;
        }

        return chats.get(chatId);
    }

    protected UserHandler getOrCreateUserHandler(long userId) {
        if (!users.containsKey(userId)) {
            UserHandler handler = userHandlerGenerator.apply(userId);

            users.put(userId, handler);

            return handler;
        }

        return users.get(userId);
    }

    public void setUpdateValidator(Function<Update, Boolean> updateValidator) {
        this.updateValidator = updateValidator;
    }

    @Override
    public int process(List<Update> updates) {
        int lastUpdateId = CONFIRMED_UPDATES_NONE;

        for (Update update : updates) {
            if (updateValidator != null && !updateValidator.apply(update))
                continue;

            int updateId = update.updateId();

            try {
                if (update.message() != null) {
                    long chatId = update.message().chat().id();
                    long userId = update.message().from().id();

                    getOrCreateChatHandler(chatId).getEventHandler().fire(new NewMessageEvent(updateId, userId, chatId, update.message()));
                }

                if (update.editedMessage() != null) {
                    long chatId = update.editedMessage().chat().id();
                    long userId = update.editedMessage().from().id();

                    getOrCreateChatHandler(chatId).getEventHandler().fire(new EditedMessageEvent(updateId, userId, chatId, update.editedMessage()));
                }

                if (update.channelPost() != null) {
                    long chatId = update.channelPost().chat().id();
                    long userId = update.channelPost().from().id();

                    getOrCreateChatHandler(chatId).getEventHandler().fire(new NewChannelPostEvent(updateId, userId, chatId, update.channelPost()));
                }

                if (update.editedChannelPost() != null) {
                    long chatId = update.editedChannelPost().chat().id();
                    long userId = update.editedChannelPost().from().id();

                    getOrCreateChatHandler(chatId).getEventHandler().fire(new EditedChannelPostEvent(updateId, userId, chatId, update.editedChannelPost()));
                }

                if (update.callbackQuery() != null) {
                    long chatId = update.callbackQuery().message().chat().id();
                    long userId = update.callbackQuery().from().id();

                    getOrCreateChatHandler(chatId).getEventHandler().fire(new CallbackQueryEvent(updateId, userId, chatId, update.callbackQuery()));
                }

                if (update.pollAnswer() != null) {
                    long chatId = update.pollAnswer().voterChat().id();
                    long userId = update.pollAnswer().user().id();

                    getOrCreateChatHandler(chatId).getEventHandler().fire(new PollAnswerEvent(updateId, userId, chatId, update.pollAnswer()));
                }

                if (update.myChatMember() != null) {
                    long chatId = update.myChatMember().chat().id();
                    long userId = update.myChatMember().from().id();

                    getOrCreateChatHandler(chatId).getEventHandler().fire(new UpdatedBotEvent(updateId, userId, chatId, update.myChatMember()));
                }

                if (update.chatMember() != null) {
                    long chatId = update.chatMember().chat().id();
                    long userId = update.chatMember().from().id();

                    getOrCreateChatHandler(chatId).getEventHandler().fire(new UpdatedChatMemberEvent(updateId, userId, chatId, update.chatMember()));
                }

                if (update.chatJoinRequest() != null) {
                    long chatId = update.chatJoinRequest().chat().id();
                    long userId = update.chatJoinRequest().from().id();

                    getOrCreateChatHandler(chatId).getEventHandler().fire(new ChatJoinRequestEvent(updateId, userId, chatId, update.chatJoinRequest()));
                }

                if (update.inlineQuery() != null) {
                    long userId = update.inlineQuery().from().id();

                    getOrCreateUserHandler(userId).getEventHandler().fire(new InlineQueryEvent(updateId, userId, update.inlineQuery()));
                }

                if (update.chosenInlineResult() != null) {
                    long userId = update.chosenInlineResult().from().id();

                    getOrCreateUserHandler(userId).getEventHandler().fire(new ChosenInlineResultEvent(updateId, userId, update.chosenInlineResult()));
                }

                if (update.shippingQuery() != null) {
                    long userId = update.shippingQuery().from().id();

                    getOrCreateUserHandler(userId).getEventHandler().fire(new ShippingQueryEvent(updateId, userId, update.shippingQuery()));
                }

                if (update.preCheckoutQuery() != null) {
                    long userId = update.preCheckoutQuery().from().id();

                    getOrCreateUserHandler(userId).getEventHandler().fire(new PreCheckoutQueryEvent(updateId, userId, update.preCheckoutQuery()));
                }

                if (update.poll() != null)
                    globalHandler.getEventHandler().fire(new NewPollEvent(updateId, update.poll()));

            }catch (Exception e) {
                e.printStackTrace();
            }

            lastUpdateId = update.updateId();
        }

        return lastUpdateId;
    }
}

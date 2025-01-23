package com.yourorg.javadoc_generator_ai.ai.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Component
public class AiClientImpl implements AiClient {

    private ChatClient client;

    public AiClientImpl(ChatModel model) {
        client = ChatClient.builder(model).build();
    }


    public String callApi(String systemPrompt, String input) {
        //  TODO-07: Use the client object to call the API.
        //  .prompt() creates a prompt to pass to the Model.class
        //  .user() sets the "user" message. Pass the input String parameter.
        //  .call() invokes the model.  It returns a CallResponse.
        //  .content() is a simple means of extracting String content from the response.
        //  Have the method return the content of the response.
        return client.prompt().system(systemPrompt).user(input).call().content();
    }
}

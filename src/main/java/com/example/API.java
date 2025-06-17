package com.example;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import org.json.JSONObject;

public class API {
	// Based on the deepseek API guide
	public static String askAI(String url, String model, String role, String content) {
//        if (apiKey == null) {
//        	return null;
//        }
        try {
            String type = "application/json";
            URL u = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", type);
            // Typically used with hugging face or deepseek API, unncessary for ai.hackclub.com
//            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            
            /*Example object result:
             * {
			  "messages": [
			    {
			      "role": "system",
			      "content": "You are a helpful assistant."
			    },
			    {
			      "role": "user",
			      "content": "Please debug my code, there is an error: \npublic class Main {\n\tpublic static void main(String[] args) {\n\n\t}\n}\n"
			    }
			  ],
			  "model": "llama-3.3-70b-versatile"
			}*/
            // Create the JSON body using JSONObject and JSONArray
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);
            
            org.json.JSONArray messages = new org.json.JSONArray();
            JSONObject sysMsg = new JSONObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", "You are a helpful assistant.");
            messages.put(sysMsg);

            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", "Please debug my code, there is an error: \n" + content);
            messages.put(userMsg);

            requestBody.put("messages", messages);
            
            System.out.println(requestBody.toString());
            // Write the JSON body to the output stream (taken from online)
            try (java.io.OutputStream os = conn.getOutputStream()) {
                // Convert the request body to byte form (in UTF-8 encoding)
                byte[] input = requestBody.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                // Normally, this would write text to the output of the program, but, since we
                // set os to the output stream of the connection, it will send it to the URL
                os.write(input, 0, input.length);
            }

            // Read the responses status code
            int status = conn.getResponseCode();
            // Create an input stream to read the response but set to the error form if the
            // status is not OK
            java.io.InputStream is = (status < 400) ? conn.getInputStream() : conn.getErrorStream();
            // Create a scanner to read the input stream (is)
            try (Scanner scanner = new Scanner(is, "UTF-8")) {
                // Use a delimiter of \\A to ensure it selects all of the text (rather than
                // splitting by " ")
                // hasNext() checks if there is any remaining content to read (because usually
                // it reads word by word) but in this case it basically just checks if the
                // string is empty or not because if nothing is given then it will return an
                // Exception of NoSuchElementException
                String response = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                System.out.println(response);
                
                // Parse JSON by going directly to the "choices" key
                JSONObject resultObj = new JSONObject(response);
                JSONObject choices = (JSONObject) resultObj.getJSONArray("choices").get(0);
                String textResult = "You are using " + model + ". \nChatGPT is given the role: " + role + "\n" + choices.getJSONObject("message").getString("content");
                // Replace escaped new line characters so it actually renders new lines correctly instead of showing \n on the screen and not a new line
                conn.disconnect();
                return (textResult.replace("\\n", "\n").replace("\\\"", "\""));
            }
        } catch (Exception e) {
            System.out.println("Failed to connect to the API");
            e.printStackTrace();
        }
        return "";
    }
}

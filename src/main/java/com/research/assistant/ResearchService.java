package com.research.assistant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
@Service
public class ResearchService {
    @Value("${gemini.api.url}")
  private String geminiApiUrl;
    @Value("${gemini.api.key}")
  private String geminiApiKey;

   private final WebClient webClient;
   private final ObjectMapper objectMapper;

//    public ResearchService(WebClient.Builder  webClientBuilder, ObjectMapper objectMapper) {
//        this.objectMapper = objectMapper;
//        this.webClient = WebClient.builder().build();
//    }

public ResearchService() {
    // Create both objects manually, bypassing Spring bean requirements
    this.objectMapper = new ObjectMapper();
    this.webClient = WebClient.builder().build();
}
    public String processContent(ResearchRequest request) {
        // Build the prompt
        String prompt =  buildPrompt(request);
        //Query the AI Model API
        Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                     Map.of("parts", new Object[]{
                             Map.of("text",prompt)
                     })
                }

        );
    String response = webClient.post()
            .uri(geminiApiUrl + geminiApiKey)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .block();
        //Parse the response

        //Return response
        return extractTextFromResponse(response);

    }

    private String extractTextFromResponse(String response) {
        try {
            GeminiResponse geminiResponse = objectMapper.readValue(response, GeminiResponse.class);
            if (geminiResponse.getCandidates() != null && !geminiResponse.getCandidates().isEmpty()) {
                GeminiResponse.Candidate firstCandidate = geminiResponse.getCandidates().get(0);

                if (firstCandidate.getContent() != null &&
                        firstCandidate.getContent().getParts() != null &&
                        !firstCandidate.getContent().getParts().isEmpty()) {

                    return firstCandidate.getContent().getParts().get(0).getText();
                }
            }
      return "No Content found in response";
        }
        catch (Exception e){

            return "Error Parsing: "+ e.getMessage();
        }
    }

    private String buildPrompt(ResearchRequest request){
        StringBuilder prompt = new StringBuilder();
        switch (request.getOperation()){
            case "Summarize":
                prompt.append("Summarize the contents of your research in few sentences\n\n.");
                break;
            case "Suggest":
                prompt.append("Based on the Following Content:  a clear alternative suggestion for the following Content ");
                break;
            default:
                throw new IllegalArgumentException("Unknown Operation"+ request.getOperation());

        }
        prompt.append(request.getContent());
        return prompt.toString();
    }

}

package com.zzy.ksongfloat;

import com.zzy.ksongfloat.ai.AiModelFetcher;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AiModelFetcherTest {
    @Test
    public void parseModels_extractsIds() throws Exception {
        String json = "{\"data\":[{\"id\":\"deepseek-chat\"},{\"id\":\"deepseek-reasoner\"}]}";
        List<String> models = AiModelFetcher.parseModels(json);
        assertEquals(2, models.size());
        assertEquals("deepseek-chat", models.get(0));
    }

    @Test
    public void pickDefaultModel_prefersDeepseekChat() {
        List<String> models = Arrays.asList("gpt-4o-mini", "deepseek-chat");
        assertEquals("deepseek-chat", AiModelFetcher.pickDefaultModel(models, ""));
    }
}

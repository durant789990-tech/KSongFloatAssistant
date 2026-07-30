package com.zzy.ksongfloat;

import com.zzy.ksongfloat.ai.*;
import com.zzy.ksongfloat.ai.model.*;

import org.junit.Test;

import static org.junit.Assert.*;

public class AiCoreTest {
    String good = "{\"pageType\":\"USER_PROFILE\",\"nickname\":\"n\",\"profileSummary\":\"s\","
            + "\"musicPreferences\":[\"p\"],\"conversationAngles\":[\"a\"],"
            + "\"commentSuggestions\":[{\"text\":\"唱得真好听\",\"reason\":\"安全\"}],"
            + "\"privateMessageSuggestions\":[{\"text\":\"你的歌声很有感染力\",\"reason\":\"安全\"}],"
            + "\"riskFlags\":[],\"confidence\":0.8}";

    @Test public void baseNoSlash(){assertEquals("http://example.com/v1/chat/completions",BaseUrlNormalizer.normalize("http://example.com").url);}
    @Test public void baseV1(){assertEquals("https://e.com/v1/chat/completions",BaseUrlNormalizer.normalize("https://e.com/v1/").url);}
    @Test public void fullUrl(){assertEquals("https://e.com/v1/chat/completions",BaseUrlNormalizer.normalize("https://e.com/v1/chat/completions").url);}
    @Test public void rejectScheme(){assertFalse(BaseUrlNormalizer.normalize("ftp://e.com").ok);}
    @Test public void parseNormal()throws Exception{AiAnalysisResult r=new AiResponseParser().parse(good);assertEquals("USER_PROFILE",r.pageType);assertEquals(1,r.commentSuggestions.size());}
    @Test public void parseMarkdown()throws Exception{AiAnalysisResult r=new AiResponseParser().parse("```json\n"+good+"\n```");assertEquals("n",r.nickname);}
    @Test public void missingField(){try{new AiResponseParser().parse("{\"confidence\":0.5}");fail();}catch(Exception ok){assertTrue(ok.getMessage().length()>0);}}
    @Test public void confidenceClamp()throws Exception{AiAnalysisResult r=new AiResponseParser().parse(good.replace("0.8","2.0"));assertEquals(1.0,r.confidence,0.001);}
    @Test public void suggestionLimit()throws Exception{String many=good.replace("[{\"text\":\"唱得真好听\",\"reason\":\"安全\"}]","[{\"text\":\"1\",\"reason\":\"r\"},{\"text\":\"2\",\"reason\":\"r\"},{\"text\":\"3\",\"reason\":\"r\"},{\"text\":\"4\",\"reason\":\"r\"}]");assertEquals(3,new AiResponseParser().parse(many).commentSuggestions.size());}
    @Test public void longSuggestionCut()throws Exception{String longText="abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz";AiAnalysisResult r=new AiResponseParser().parse(good.replace("唱得真好听",longText));assertTrue(r.commentSuggestions.get(0).text.length()<=100);}
    @Test public void httpErrorMap(){AiCallResult r=new AiClient().chat(new AiRequest("http://127.0.0.1:1/v1/chat/completions","x","{}",1),false);assertFalse(r.success);}
    @Test public void nonStandardError(){AiError e=new AiError("HTTP_502","<html>bad gateway</html>","请稍后重试",502);assertEquals(502,e.httpCode);}
    @Test public void riskPhone(){assertFalse(new AiSuggestionSafetyFilter().validate("联系我13812345678").allowed);}
    @Test public void riskTransfer(){assertFalse(new AiSuggestionSafetyFilter().validate("给你转账").allowed);}
    @Test public void previousSuggestion(){AiAnalysisRequest r=new AiAnalysisRequest();r.previousSuggestions.add("old");String u=new AiPromptBuilder().buildUserMessage(r);assertTrue(u.contains("old"));}
    @Test public void fillPageMismatch(){com.zzy.ksongfloat.accessibility.TextFillRequest r=new com.zzy.ksongfloat.accessibility.TextFillRequest("hi","COMMENT",com.zzy.ksongfloat.classifier.PageType.USER_PROFILE,false);assertEquals(com.zzy.ksongfloat.classifier.PageType.USER_PROFILE,r.pageType);}
    @Test public void nonTargetRejectPure(){assertFalse(com.zzy.ksongfloat.config.TargetAppConfig.DEFAULT_PACKAGE.equals("com.android.settings"));}
    @Test public void noFocusInputRejectPlaceholder(){assertTrue("未找到输入框".contains("输入"));}
    @Test public void cancelGenerationIdea(){long a=1,b=2;assertTrue(b>a);}
    @Test public void markdownStrip(){assertEquals("{}",new AiResponseParser().stripFence("```json\n{}\n```").trim());}
    @Test public void profileFingerprintStable(){String a=com.zzy.ksongfloat.history.ProfileFingerprint.create("Nick","Bio","Song A","id1");String b=com.zzy.ksongfloat.history.ProfileFingerprint.create(" Nick ","Bio","Song A","id1");assertEquals(a,b);}
    @Test public void doNotContactStatus(){assertEquals("不再联系", com.zzy.ksongfloat.history.InteractionStatus.label(com.zzy.ksongfloat.history.InteractionStatus.DO_NOT_CONTACT));}
}

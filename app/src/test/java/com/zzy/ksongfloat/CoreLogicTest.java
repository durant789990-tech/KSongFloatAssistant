package com.zzy.ksongfloat;

import com.zzy.ksongfloat.capture.*; import com.zzy.ksongfloat.classifier.*; import com.zzy.ksongfloat.config.TargetAppConfig;
import java.util.*; import org.junit.Test; import static org.junit.Assert.*;

public class CoreLogicTest {
 static final String NEAR="\u9644\u8fd1", DIST="\u8ddd\u79bb", USER="\u7528\u6237";
 static final String HOME="\u4e3b\u9875", FOLLOW="\u5173\u6ce8", FANS="\u7c89\u4e1d", WORKS="\u4f5c\u54c1", BIO="\u7b80\u4ecb", PM="\u79c1\u4fe1";
 static final String COMMENT="\u8bc4\u8bba", LIKE="\u70b9\u8d5e", PLAY="\u64ad\u653e", SING="\u6f14\u5531", GIFT="\u793c\u7269";
 static final String POST_COMMENT="\u53d1\u8868\u8bc4\u8bba", COMMENT_INPUT="\u8bc4\u8bba\u8f93\u5165", SAY="\u8bf4\u70b9\u4ec0\u4e48", PUBLISH="\u53d1\u5e03";
 static final String MSG="\u6d88\u606f", CHAT="\u804a\u5929", SEND_MSG="\u53d1\u9001\u6d88\u606f";
 @Test public void duplicateCleaner(){ List<String> r=TextCleaner.uniqueCleanLines(Arrays.asList("  hi  ","hi","A   B")); assertEquals(2,r.size()); assertEquals("A B",r.get(1)); }
 @Test public void collectorMerge(){ PageTextResult p=new PageTextCollector().collect(null,new OcrResult(NEAR+"\n"+DIST,null,Arrays.asList(NEAR,DIST),true,""),1,true); assertTrue(p.ocrAvailable); assertTrue(p.mergedText.contains(NEAR)); }
 private PageTextResult r(String s,int editable){ return new PageTextResult("com.tencent.karaoke","",s,"",s,new ArrayList<>(),new ArrayList<>(),1,1,1,true,false,true,editable,new ArrayList<>()); }
 @Test public void nearbyClassify(){ assertEquals(PageType.NEARBY_LIST,new PageClassifier().classify(r(NEAR+"\n"+DIST+" 500m\n"+USER,0)).pageType); }
 @Test public void profileClassify(){ assertEquals(PageType.USER_PROFILE,new PageClassifier().classify(r(HOME+"\n"+FOLLOW+"\n"+FANS+"\n"+WORKS+"\n"+BIO+"\n"+PM,0)).pageType); }
 @Test public void workClassify(){ assertEquals(PageType.WORK_DETAIL,new PageClassifier().classify(r(WORKS+"\n"+COMMENT+"\n"+LIKE+"\n"+PLAY+"\n"+SING+"\n"+GIFT,0)).pageType); }
 @Test public void commentInputClassify(){ assertEquals(PageType.COMMENT_INPUT,new PageClassifier().classify(r(POST_COMMENT+"\n"+COMMENT_INPUT+"\n"+SAY+"\n"+PUBLISH,1)).pageType); }
 @Test public void msgInputClassify(){ assertEquals(PageType.PRIVATE_MESSAGE_INPUT,new PageClassifier().classify(r(PM+"\n"+MSG+"\n"+CHAT+"\n"+SEND_MSG,1)).pageType); }
 @Test public void lowConfidenceUnknown(){ assertEquals(PageType.UNKNOWN,new PageClassifier().classify(r("plain weak text",0)).pageType); }
 @Test public void ocrEmpty(){ OcrResult e=OcrResult.empty("empty"); assertFalse(e.available); assertEquals("",e.fullText); }
 @Test public void captureCancelStatusExists(){ assertEquals(ScreenCaptureManager.Status.CANCELLED, ScreenCaptureManager.Status.valueOf("CANCELLED")); }
 @Test public void targetPackageNormalize(){ assertEquals("com.tencent.karaoke", TargetAppConfig.normalizePackageName(" package:com.tencent.karaoke/.MainActivity ")); assertEquals("", TargetAppConfig.normalizePackageName("   ")); }
}

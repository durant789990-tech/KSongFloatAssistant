package com.zzy.ksongfloat.classifier;

import com.zzy.ksongfloat.capture.PageTextResult;
import java.util.*;

public class PageClassifier {
    private static final String S_QM="\u5168\u6c11";
    private static final String S_KG="k\u6b4c";
    private static final String S_NEAR="\u9644\u8fd1";
    private static final String S_DISTANCE="\u8ddd\u79bb";
    private static final String S_USER="\u7528\u6237";
    private static final String S_HOME="\u4e3b\u9875";
    private static final String S_FOLLOW="\u5173\u6ce8";
    private static final String S_FANS="\u7c89\u4e1d";
    private static final String S_WORKS="\u4f5c\u54c1";
    private static final String S_BIO="\u7b80\u4ecb";
    private static final String S_PM="\u79c1\u4fe1";
    private static final String S_COMMENT="\u8bc4\u8bba";
    private static final String S_LIKE="\u70b9\u8d5e";
    private static final String S_PLAY="\u64ad\u653e";
    private static final String S_GIFT="\u793c\u7269";
    private static final String S_SING="\u6f14\u5531";
    private static final String S_POST_COMMENT="\u53d1\u8868\u8bc4\u8bba";
    private static final String S_COMMENT_INPUT="\u8bc4\u8bba\u8f93\u5165";
    private static final String S_SAY="\u8bf4\u70b9\u4ec0\u4e48";
    private static final String S_PUBLISH="\u53d1\u5e03";
    private static final String S_MESSAGE="\u6d88\u606f";
    private static final String S_CHAT="\u804a\u5929";
    private static final String S_SEND_MSG="\u53d1\u9001\u6d88\u606f";

    public PageClassificationResult classify(PageTextResult r){
        List<String>warn=new ArrayList<>(); if(r==null) return unknown(warn,"no page text");
        String all=(r.packageName+"\n"+r.windowTitle+"\n"+r.mergedText+"\n"+r.resourceIds+"\n"+r.contentDescriptions).toLowerCase(Locale.ROOT);
        if(!(all.contains("karaoke")||all.contains(S_QM)||all.contains(S_KG)||all.contains("kg"))) warn.add("front package/text not clearly ksong");
        Candidate nearby=c(PageType.NEARBY_LIST), profile=c(PageType.USER_PROFILE), work=c(PageType.WORK_DETAIL), comment=c(PageType.COMMENT_INPUT), msg=c(PageType.PRIVATE_MESSAGE_INPUT);
        score(nearby,all,S_NEAR,.25); score(nearby,all,S_DISTANCE,.25); score(nearby,all,"km",.15); score(nearby,all,S_USER,.15);
        score(profile,all,S_HOME,.25); score(profile,all,S_FOLLOW,.2); score(profile,all,S_FANS,.2); score(profile,all,S_WORKS,.2); score(profile,all,S_BIO,.15); score(profile,all,S_PM,.15);
        score(work,all,S_COMMENT,.18); score(work,all,S_LIKE,.18); score(work,all,S_PLAY,.18); score(work,all,S_WORKS,.18); score(work,all,S_GIFT,.12); score(work,all,S_SING,.2);
        score(comment,all,S_POST_COMMENT,.35); score(comment,all,S_COMMENT_INPUT,.35); score(comment,all,S_SAY,.25); score(comment,all,S_PUBLISH,.15); if(r.editableCount>0 && all.contains(S_COMMENT)) add(comment,"editable","comment editable",.25);
        score(msg,all,S_PM,.28); score(msg,all,S_MESSAGE,.22); score(msg,all,S_CHAT,.25); score(msg,all,S_SEND_MSG,.3); if(r.editableCount>0 && (all.contains(S_PM)||all.contains(S_MESSAGE)||all.contains(S_CHAT))) add(msg,"editable","message editable",.25);
        List<Candidate> cs=Arrays.asList(nearby,profile,work,comment,msg); Candidate best=nearby; for(Candidate x:cs) if(x.score>best.score) best=x;
        if(best.evidence.size()<2) return unknownWith(warn,best.evidence,"not enough evidence");
        double conf=Math.min(1.0,best.score); if(conf<0.6) return unknownWith(warn,best.evidence,"confidence below 0.6");
        return new PageClassificationResult(best.type,conf,best.evidence,warn,detectNickname(r),detectSongs(r));
    }
    private void score(Candidate c,String all,String kw,double w){ if(kw!=null&&!kw.isEmpty()&&all.contains(kw.toLowerCase(Locale.ROOT))) add(c,"keyword",kw,w); }
    private void add(Candidate c,String src,String text,double w){ c.score+=w; c.evidence.add(new PageEvidence(src,text,w)); }
    private Candidate c(PageType t){ return new Candidate(t); }
    private static class Candidate{ PageType type; double score; List<PageEvidence> evidence=new ArrayList<>(); Candidate(PageType t){type=t;} }
    private PageClassificationResult unknown(List<String>w,String reason){ w.add(reason); return new PageClassificationResult(PageType.UNKNOWN,0,new ArrayList<>(),w,"",new ArrayList<>()); }
    private PageClassificationResult unknownWith(List<String>w,List<PageEvidence>e,String reason){ w.add(reason); return new PageClassificationResult(PageType.UNKNOWN,0.4,e,w,"",new ArrayList<>()); }
    private String detectNickname(PageTextResult r){ String[] ls=r.mergedText.split("\\n"); for(String l:ls){ if(l.length()>1&&l.length()<18&&!l.contains(S_FOLLOW)&&!l.contains(S_FANS)&&!l.contains(S_COMMENT)) return l; } return ""; }
    private List<String> detectSongs(PageTextResult r){ List<String> out=new ArrayList<>(); for(String l:r.mergedText.split("\\n")){ if((l.contains("\u300a")&&l.contains("\u300b"))||l.contains(S_SING)){ if(out.size()<5) out.add(l); } } return out; }
}

package com.zzy.ksongfloat.accessibility;

import android.view.accessibility.AccessibilityNodeInfo;
import com.zzy.ksongfloat.capture.TextCleaner;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AccessibilityTextExtractor {
    private static final int MAX_DEPTH=10, MAX_NODES=220;
    private int visited, editableCount;
    private final Set<String> texts=new LinkedHashSet<>(), descs=new LinkedHashSet<>(), ids=new LinkedHashSet<>(), edits=new LinkedHashSet<>(), clicks=new LinkedHashSet<>(), all=new LinkedHashSet<>();
    public AccessibilitySnapshot extract(String pkg, String title, AccessibilityNodeInfo root, boolean isKsong) {
        visited=0; editableCount=0; texts.clear(); descs.clear(); ids.clear(); edits.clear(); clicks.clear(); all.clear();
        if(root!=null) walk(root,0);
        if(!isKsong) return AccessibilitySnapshot.unavailable(pkg);
        return new AccessibilitySnapshot(pkg,title,System.currentTimeMillis(),new ArrayList<>(texts),new ArrayList<>(descs),new ArrayList<>(ids),new ArrayList<>(edits),new ArrayList<>(clicks),new ArrayList<>(all),editableCount,true);
    }
    private void walk(AccessibilityNodeInfo n,int depth){
        if(n==null||depth>MAX_DEPTH||visited++>MAX_NODES) return;
        String text=TextCleaner.cleanLine(cs(n.getText())); String desc=TextCleaner.cleanLine(cs(n.getContentDescription())); String id=TextCleaner.cleanLine(n.getViewIdResourceName());
        if(!text.isEmpty()){texts.add(text); all.add(text);} if(!desc.isEmpty()){descs.add(desc); all.add(desc);} if(!id.isEmpty()) ids.add(id);
        if(n.isEditable()){ editableCount++; if(!text.isEmpty()) edits.add(text); }
        if(n.isClickable() && !text.isEmpty()) clicks.add(text);
        int c=Math.min(n.getChildCount(),40);
        for(int i=0;i<c;i++){ AccessibilityNodeInfo child=null; try{ child=n.getChild(i); walk(child,depth+1); } finally { try{ if(child!=null) child.recycle(); }catch(Exception ignored){} } }
    }
    private String cs(CharSequence c){ return c==null?"":c.toString(); }
}

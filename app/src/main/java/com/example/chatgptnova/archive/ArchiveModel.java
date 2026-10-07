package com.example.chatgptnova.archive;

import com.google.gson.Gson;
import java.util.*;

/** Owns exported mapping nodes, including roots, branches and uninterpreted metadata. */
public final class ArchiveModel {
    public static final Gson JSON=new com.google.gson.GsonBuilder().setObjectToNumberStrategy(com.google.gson.ToNumberPolicy.BIG_DECIMAL).create();
    private ArchiveModel() {}
    @SuppressWarnings("unchecked") public static Map<String,Object> object(Object value) {
        return value instanceof Map ? (Map<String,Object>)value : Collections.emptyMap();
    }
    public static String string(Object value) { return value instanceof String ? (String)value : ""; }
    public static Double number(Object value) {
        if(!(value instanceof Number))return null;
        double d=((Number)value).doubleValue();return Double.isFinite(d)&&Math.abs(d)<253402300800d ? d : null;
    }
    public static final class Node {
        public final String key,id,parent,role,channel,contentType,status,raw;
        public final Double created;
        public final boolean hasMessage;
        public final Map<String,Object> data;
        public Node(String key,Map<String,Object> data) {
            this.key=key;this.data=data;raw=JSON.toJson(data);
            parent=string(data.get("parent"));Map<String,Object> m=object(data.get("message"));hasMessage=data.get("message") instanceof Map;
            id=string(m.get("id"));role=string(object(m.get("author")).get("role"));channel=string(m.get("channel"));
            contentType=string(object(m.get("content")).get("content_type"));status=string(m.get("status"));created=number(m.get("create_time"));
        }
        public String text() {
            Map<String,Object> c=object(object(data.get("message")).get("content"));
            Object p=c.get("parts");StringBuilder out=new StringBuilder();
            if(p instanceof List)for(Object part:(List<?>)p) {
                if(out.length()>0)out.append('\n');
                if(part instanceof String)out.append(part);
                else out.append("[非文本内容 / attachment metadata 已保存在本地档案]");
            }
            else if(c.get("text") instanceof String)out.append(c.get("text"));
            if(out.length()==0&&!c.isEmpty())out.append("[非文本或空内容；原始 metadata 已保留]");
            return out.toString();
        }
    }
    public static final class Conversation {
        public final String id,title,currentNode,header;
        public final Double created,updated;
        public final LinkedHashMap<String,Node> nodes;
        public Conversation(Map<String,Object> data) throws ArchiveError {
            id=string(data.get("conversation_id")).isEmpty()?string(data.get("id")):string(data.get("conversation_id"));
            title=string(data.get("title")).isEmpty()?"无标题会话":string(data.get("title"));
            currentNode=string(data.get("current_node"));created=number(data.get("create_time"));updated=number(data.get("update_time"));
            if(!(data.get("mapping") instanceof Map))throw new ArchiveError("A08_SCHEMA_UNSUPPORTED");
            Map<String,Object> h=new LinkedHashMap<>(data);h.remove("mapping");header=JSON.toJson(h);nodes=new LinkedHashMap<>();
            Map<String,Object> mapping=object(data.get("mapping"));
            if(mapping.size()>10000)throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
            for(Map.Entry<String,Object> e:mapping.entrySet()) {
                if(e.getKey().isEmpty()||!(e.getValue() instanceof Map))throw new ArchiveError("A08_SCHEMA_UNSUPPORTED");
                nodes.put(e.getKey(),new Node(e.getKey(),object(e.getValue())));
            }
        }
        public int messageCount(){int n=0;for(Node v:nodes.values())if(v.hasMessage)n++;return n;}
    }
}

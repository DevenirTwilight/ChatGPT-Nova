package com.example.chatgptnova.archive;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import com.example.chatgptnova.archive.ArchiveModel.*;

/** Synthetic compatible exports only. No claim about real OpenAI export completeness. */
public class ArchiveCoreTest {
    static Map<String,Object> node(String id,String parent,String role,Object parts) {
        Map<String,Object> m=new LinkedHashMap<>();m.put("id",id);m.put("author",Map.of("role",role));m.put("content",Map.of("content_type","text","parts",parts));m.put("create_time",2);m.put("channel","commentary");m.put("future",Map.of("unknown",true));
        Map<String,Object> n=new LinkedHashMap<>();n.put("parent",parent);n.put("message",m);return n;
    }
    static Map<String,Object> data(String id) {
        Map<String,Object> d=new LinkedHashMap<>();d.put("conversation_id",id);d.put("title","中 français English 😀");d.put("update_time",10);d.put("current_node","a");
        Map<String,Object> mapping=new LinkedHashMap<>();mapping.put("a",node("a","u","assistant",List.of("answer")));mapping.put("u",node("u","","user",List.of("question")));d.put("mapping",mapping);return d;
    }
    static File json(String content)throws IOException {File f=File.createTempFile("nova-synthetic-",".json");f.deleteOnExit();try(FileOutputStream out=new FileOutputStream(f)){out.write(content.getBytes(StandardCharsets.UTF_8));}return f;}
    static File zip(Map<String,String> entries)throws IOException {File f=File.createTempFile("nova-synthetic-",".zip");f.deleteOnExit();try(ZipOutputStream out=new ZipOutputStream(new FileOutputStream(f))){for(Map.Entry<String,String> e:entries.entrySet()){out.putNextEntry(new ZipEntry(e.getKey()));out.write(e.getValue().getBytes(StandardCharsets.UTF_8));out.closeEntry();}}return f;}
    static List<Conversation> parse(File f,boolean zip)throws Exception {List<Conversation> out=new ArrayList<>();new ArchiveImporter(new ArchiveImporter.Control()).read(f,zip,out::add);return out;}
    static String encode(Object x){return ArchiveModel.JSON.toJson(x);}
    static void error(String code,File f,boolean zip)throws Exception {try{parse(f,zip);fail("expected "+code);}catch(ArchiveError e){assertEquals(code,e.code);assertEquals(code,e.getMessage());}}
    @Test public void zipDiscoverySplitAndUnrelated()throws Exception {
        Map<String,String> e=new LinkedHashMap<>();e.put("other.txt","irrelevant");e.put("folder/conversations_2.json",encode(List.of(data("2"))));e.put("conversations.json",encode(List.of(data("1"))));e.put("conversations-3.json",encode(List.of(data("3"))));assertEquals(3,parse(zip(e),true).size());
    }
    @Test public void noData()throws Exception {error("A03_NO_CONVERSATIONS_DATA",zip(Map.of("chat.html","hello")),true);}
    @Test public void invalidZip()throws Exception {error("A02_INVALID_ZIP",json("invalid"),true);}
    @Test public void unsafePaths()throws Exception {for(String path:List.of("../conversations.json","/conversations.json","C:/conversations.json","folder\\conversations.json","x/../conversations.json"))error("A02_INVALID_ZIP",zip(Map.of(path,"[]")),true);}
    @Test public void duplicateCanonicalEntries()throws Exception {error("A02_INVALID_ZIP",zip(Map.of("conversations.json","[]","./conversations.json","[]")),true);}
    @Test public void malformedAndDuplicateKeys()throws Exception {error("A04_JSON_PARSE_FAILED",json("[{broken]"),false);error("A04_JSON_PARSE_FAILED",json("{\"mapping\":{},\"mapping\":{}}"),false);error("A04_JSON_PARSE_FAILED",json("[] trailing"),false);}
    @Test public void strictJson()throws Exception {error("A04_JSON_PARSE_FAILED",json("[/* comment */]"),false);}
    @Test public void schema()throws Exception {error("A08_SCHEMA_UNSUPPORTED",json("[1]"),false);error("A08_SCHEMA_UNSUPPORTED",json("[{\"mapping\":null}]"),false);}
    @Test public void arrayWrapperSingle()throws Exception {for(Object v:List.of(List.of(data("1")),Map.of("conversations",List.of(data("1")),"future",true),data("1")))assertEquals(1,parse(json(encode(v)),false).size());}
    @Test public void unknownFieldsPreserved()throws Exception {Map<String,Object>d=data("1");d.put("future",Map.of("new","value"));Conversation c=parse(json(encode(d)),false).get(0);assertTrue(c.header.contains("value"));assertTrue(c.nodes.get("a").raw.contains("unknown"));}
    @Test public void currentBranchNotObjectOrder()throws Exception {Conversation c=new Conversation(data("1"));ArchiveTree.Selection s=ArchiveTree.select(c,false);assertEquals("u",s.messages.get(0).key);assertEquals("a",s.messages.get(1).key);assertEquals("current-branch",s.scope);}
    @Test public void editedAndRegeneratedBranches()throws Exception {Map<String,Object>d=data("1");ArchiveModel.object(d.get("mapping")).put("b",node("b","u","assistant",List.of("regenerated")));Conversation c=new Conversation(d);assertEquals(2,ArchiveTree.select(c,false).messages.size());assertEquals(3,ArchiveTree.select(c,true).messages.size());d.remove("current_node");assertEquals("all-nodes-safe-order",ArchiveTree.select(new Conversation(d),false).scope);}
    @Test public void orphan()throws Exception {Map<String,Object>d=data("1");ArchiveModel.object(ArchiveModel.object(d.get("mapping")).get("u")).put("parent","missing");ArchiveTree.Selection s=ArchiveTree.select(new Conversation(d),false);assertFalse(s.warnings.isEmpty());assertEquals(2,s.messages.size());}
    @Test public void cycle()throws Exception {Map<String,Object>d=data("1");ArchiveModel.object(ArchiveModel.object(d.get("mapping")).get("u")).put("parent","a");ArchiveTree.Selection s=ArchiveTree.select(new Conversation(d),false);assertEquals("all-nodes-safe-order",s.scope);assertFalse(s.warnings.isEmpty());}
    @Test public void noCurrentUniqueLeaf()throws Exception {Map<String,Object>d=data("1");d.remove("current_node");assertEquals("current-branch",ArchiveTree.select(new Conversation(d),false).scope);}
    @Test public void nullRootsUnknownRolesNonText()throws Exception {Map<String,Object>d=data("1");Map<String,Object>m=ArchiveModel.object(d.get("mapping"));Map<String,Object> root=new LinkedHashMap<>();root.put("message",null);root.put("parent",null);m.put("root",root);m.put("tool",node("tool","a","tool",List.of(Map.of("asset_pointer","private"))));m.put("developer",node("developer","tool","developer",List.of()));Conversation c=new Conversation(d);assertEquals(4,c.messageCount());assertTrue(c.nodes.get("tool").text().contains("非文本"));assertTrue(c.nodes.get("tool").raw.contains("asset_pointer"));}
    @Test public void duplicateAndUpdateMerge()throws Exception {Conversation c=new Conversation(data("1"));ArchiveMerge same=new ArchiveMerge(c,new Conversation(data("1")));assertEquals(0,same.added);assertEquals(2,same.skipped);Map<String,Object>d=data("1");Map<String,Object>m=ArchiveModel.object(d.get("mapping"));m.put("b",node("b","a","assistant",List.of("answer")));ArchiveMerge p=new ArchiveMerge(c,new Conversation(d));assertEquals(1,p.added);assertEquals(3,p.merged.messageCount());}
    @Test public void identicalTextDifferentIdsNotMerged()throws Exception {Map<String,Object>d=data("1");ArchiveModel.object(d.get("mapping")).put("b",node("b","u","assistant",List.of("answer")));assertEquals(3,new Conversation(d).messageCount());}
    @Test public void olderImportPreservesNewer()throws Exception {Conversation c=new Conversation(data("1"));Map<String,Object>d=data("1");d.put("update_time",1);d.put("title","old");ArchiveModel.object(d.get("mapping")).put("a",node("a","u","assistant",List.of("older answer")));ArchiveMerge p=new ArchiveMerge(c,new Conversation(d));assertEquals(c.title,p.merged.title);assertEquals("answer",p.merged.nodes.get("a").text());}
    @Test public void missingIdentityNotGuessed()throws Exception {Map<String,Object>d=data("");Conversation c=new Conversation(d);assertTrue(c.id.isEmpty());assertEquals(2,new ArchiveMerge(null,c).added);}
    @Test public void cancellation()throws Exception {ArchiveImporter.Control ctl=new ArchiveImporter.Control();ctl.cancelled.set(true);try{new ArchiveImporter(ctl).read(json(encode(List.of(data("1")))),false,c->{});fail();}catch(ArchiveError e){assertEquals("A07_IMPORT_CANCELLED",e.code);}}
    @Test public void earlyStringAndDepthLimits()throws Exception {error("A05_ARCHIVE_TOO_LARGE",json("[\""+"x".repeat(524289)+"\"]"),false);error("A05_ARCHIVE_TOO_LARGE",json("[".repeat(65)+"]".repeat(65)),false);}
    @Test public void compressionBomb()throws Exception {error("A05_ARCHIVE_TOO_LARGE",zip(Map.of("conversations.json","x".repeat(2*1024*1024))),true);}
    @Test public void invalidUtf8()throws Exception {File f=json("[]");try(FileOutputStream out=new FileOutputStream(f)){out.write(new byte[]{'[','"',(byte)0xc3,'"',']'});}error("A04_JSON_PARSE_FAILED",f,false);}
    @Test public void roundTripRawIdentity()throws Exception {Conversation c=new Conversation(data("1"));Map<String,Object>d=ArchiveModel.object(ArchiveModel.JSON.fromJson(encode(data("1")),Map.class));assertEquals(2,new ArchiveMerge(new Conversation(d),c).skipped);}
    @Test public void timeLimit()throws Exception {ArchiveImporter.Control ctl=new ArchiveImporter.Control(System.nanoTime()-301_000_000_000L);try{ctl.check();fail();}catch(ArchiveError e){assertEquals("A05_ARCHIVE_TOO_LARGE",e.code);}}
    @Test public void hugeConversationIterativeTree()throws Exception {Map<String,Object>d=data("large");Map<String,Object>m=new LinkedHashMap<>();for(int i=0;i<9999;i++)m.put("n"+i,node("n"+i,i==0?"":"n"+(i-1),"assistant",List.of("x")));d.put("mapping",m);d.put("current_node","n9998");assertEquals(9999,ArchiveTree.select(new Conversation(d),false).messages.size());}
    @Test public void generatedScaleAndLongCode()throws Exception {for(int count:new int[]{1,100,1000}){List<Object>d=new ArrayList<>();for(int i=0;i<count;i++)d.add(data("c"+i));assertEquals(count,parse(json(encode(d)),false).size());}Map<String,Object>d=data("long");ArchiveModel.object(d.get("mapping")).put("a",node("a","u","assistant",List.of("```java\n"+"code 😀 中 français\n".repeat(10000)+"```\n|a|b|\n|-|-|\n|1|2|\n$E=mc^2$")));assertTrue(parse(json(encode(List.of(d))),false).get(0).nodes.get("a").text().contains("$E=mc^2$"));}
}
